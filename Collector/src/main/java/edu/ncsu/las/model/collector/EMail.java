package edu.ncsu.las.model.collector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.util.DateUtilities;

/**
 * Contains common methods for extracting and converting email messages and formats
 * 
 * 
 */
public class EMail {
	static Logger logger = Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	public static class Attachment {
		public String fileName;
		public String contentType;
		public byte[] content;
		public int size;
		
		private Attachment() {}
		
		public static Attachment createFrom(MimeBodyPart part, boolean loadAttachment) throws MessagingException, IOException {
			Attachment a = new Attachment();
			
	    	a.fileName    = part.getFileName();
	    	a.contentType = part.getContentType();
	    	
	    	if (loadAttachment) {
		    	ByteArrayOutputStream output = new ByteArrayOutputStream();    	 
		    	InputStream input = part.getInputStream();
		    	 
		    	byte[] buffer = new byte[4096];
		    	 
		    	int byteRead;
		    	while ((byteRead = input.read(buffer)) != -1) {
		    	    output.write(buffer, 0, byteRead);
		    	}
		    	input.close();
		    	output.close();
		    	a.content = output.toByteArray();
		    	a.size = a.content.length;
	    	}
	    	else {
	    		a.size = part.getSize();
	    	}
			return a;
		}
		
		public JSONObject toJSON() {
			return new JSONObject().put("fileName", fileName).put("contentType", contentType).put("size", size);
		}
	}
	
	
	private static Pattern lessThanGreaterBoundary = Pattern.compile("<(.+?)>");
	
	public static JSONObject processAddressToParts(String address, String type) {
		JSONObject result = new JSONObject().put("type", type);
		
		Matcher m = lessThanGreaterBoundary.matcher(address);
		if (m.find()) {
			result.put("email", m.group(1));
			String name = address.substring(0,address.indexOf("<")).trim();
			if (name.startsWith("\"")) { name = name.substring(1); }
			if (name.endsWith("\"")) { name = name.substring(0, name.length()-1); }
			name = name.trim();
			result.put("name",name);			
		}
		else {
			result.put("email",address);
		}
		return result;
	}
	
	
	private static void addAddressesToArray(Address[] addressees, String type, JSONArray addressArray) {
		if (addressees == null) { return ; }
		for (Address a: addressees) {
			addressArray.put(processAddressToParts(a.toString(), type));
		}
	}
	
	public static String extractText(Message mesg) throws MessagingException, IOException {
		if (mesg.getContentType().contains("multipart")) {  // concatenate the text / html sections to extract the text
			return extractText((Multipart) mesg.getContent());
		}
		else {
			return mesg.getContent().toString();
		}
	}
	
	private static String extractText(Multipart multiPart) throws MessagingException, IOException {
		StringBuilder sb = new StringBuilder();
						
		for (int i = 0; i < multiPart.getCount(); i++) {
			MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
			if (part.getContentType().contains("multipart")) {
				sb.append(extractText((Multipart) part.getContent()));
				sb.append("\n");
			}
		    else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) == false) { 
		    	if (part.getContentType().toLowerCase().contains("text")) {
		    		String text = TikaUtilities.extractText(part.getContent().toString().getBytes());
		    		sb.append(text);
		    		sb.append("\n");
		    	} 
		    }
		}	
		return sb.toString();
	}

	public static String extractHTML(Message mesg) throws MessagingException, IOException {
		if (mesg.getContentType().contains("multipart")) { 
			return extractHTML((Multipart) mesg.getContent());
		}
		else {
			return mesg.getContent().toString();
		}
	}
	
	public static String extractHTML(Multipart multiPart) throws MessagingException, IOException {
		StringBuilder sb = new StringBuilder();

			
		for (int i = 0; i < multiPart.getCount(); i++) {
			MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
			if (part.getContentType().contains("multipart")) {
				sb.append(extractHTML((Multipart) part.getContent()));
				sb.append("\n");
			}
			if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) == false) {    	
			   	if (part.getContentType().toLowerCase().contains("text/html")) {
			   		String text = part.getContent().toString();
			   		sb.append(text);
			   		sb.append("\n");
			   	} 
			}	
		}
		return sb.toString();
	}	
	
	public static JSONObject convertToJSONObject(Message mesg) throws MessagingException, IOException {
		JSONObject result = new JSONObject();
		
		JSONArray headers = new JSONArray();
		Enumeration<Header> headerEnumeration = mesg.getAllHeaders();
		while (headerEnumeration.hasMoreElements()) {
			Header h = headerEnumeration.nextElement();
			JSONObject headerObj = new JSONObject().put("name", h.getName()).put("value", h.getValue());
			headers.put(headerObj);
		}
		result.put("headers", headers);
		
		JSONArray addressees = new JSONArray();
		addAddressesToArray(mesg.getRecipients(Message.RecipientType.TO), "to", addressees);
		addAddressesToArray(mesg.getRecipients(Message.RecipientType.BCC), "bcc", addressees);
		addAddressesToArray(mesg.getRecipients(Message.RecipientType.CC), "cc", addressees);
		addAddressesToArray(mesg.getFrom(), "from", addressees);
		addAddressesToArray(mesg.getReplyTo(), "replyTo", addressees);
		result.put("addressees", addressees);
			
		
		result.put("subject",mesg.getSubject());
		result.put("disposition", mesg.getDisposition());
		result.put("contentType ",mesg.getContentType());
		result.put("size", mesg.getSize());
		
		//Assign the last seen message ID at that value
		String[] messageIDs = mesg.getHeader("Message-ID");
		for (String messageID: messageIDs) {
			result.put("messageID", messageID);
		}
		
		result.put("sentDate", DateUtilities.getDateTimeISODateTimeFormat(mesg.getSentDate().toInstant()));
		result.put("receivedDate", DateUtilities.getDateTimeISODateTimeFormat(mesg.getReceivedDate().toInstant()));
		
		
		if (mesg.getContentType().contains("multipart")) {
			JSONArray multiparts = new JSONArray();
			
			Multipart multiPart = (Multipart) mesg.getContent();
			 
			for (int i = 0; i < multiPart.getCount(); i++) {
			    MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
			    
			    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			    	Attachment a = Attachment.createFrom(part, false);
			    	multiparts.put(a.toJSON());
			    }
			    else { 
			    	JSONObject partObj = new JSONObject();
			    	partObj.put("contentType", part.getContentType())
			    	       .put("encoding", part.getEncoding())
			    	       .put("size", part.getSize());
			    	multiparts.put(partObj);
			    }
			}	
			result.put("multiparts", multiparts);
		}
		
		
		return result;
	}
	

	
	
}
