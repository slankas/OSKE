package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.EMail;
import edu.ncsu.las.model.collector.EMail.Attachment;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.source.util.SourceCrawler;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.HTMLUtilities;
import edu.ncsu.las.util.StringUtilities;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

import org.json.JSONArray;
import org.json.JSONObject;



/**
 * EmailSourceHandler provides a mechanism to gather data through an email box access via the IMAP protoocal.
 * 
 */
public class EmailSourceHandler extends WebSourceHandler  {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{ 'webCrawler': {}, 'excludeFilter' : '.*(\\\\.(css|js))$' }");
	
	private static final String SOURCE_HANDLER_NAME = "email";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Email Source Handler";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent (skipping AbstractSearch as it is abstract) to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("email", new SourceParameter("email", "JSON Object containing email source specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("email.host", new SourceParameter("email.host", "Name for the IMAP server.  This server must support imaps (imap over TLS) on port 993.",true,"imap.gmail.com",false,SourceParameterType.STRING,false,true));
		    put("email.username", new SourceParameter("email.username", "email account / user name.",true,"las_opensource@ncsu.edu",false,SourceParameterType.STRING,false,true));
		    put("email.password", new SourceParameter("email.password", "password for the account.",true,"password",false,SourceParameterType.STRING,true, true));

		    put("email.inputFolder", new SourceParameter("email.inputFolder", "Folder to scan for incoming email messeages to process",true,"INBOX",false,SourceParameterType.STRING,false,true));
		    put("email.processedFolder", new SourceParameter("email.processedFolder", "Folder to place processed messages.  Will be created if it doesn't already exist.",true,"opeke_processed",false,SourceParameterType.STRING,false,true));

		    put("email.processAttachments", new SourceParameter("email.processAttachments", "if set to true, attachments will be processed.  If false, they are silently ignored.",true,"true",false,SourceParameterType.BOOLEAN,false,true));
		    put("email.followLinks", new SourceParameter("email.followLinks", "if set to true, links will be followed using a webcrawler.  (The links form the seeds of the webcrawler).  If false, they are silently ignored.",true,"true",false,SourceParameterType.BOOLEAN,false,true));
		    put("email.followSubscriptionMgmtLinks", new SourceParameter("email.followSubscriptionMgmtLinks", "if set to true and followLinks is true, then any link with the URL or text containing \"subscrib\", \"manage\", \"preference\", or \"opt\" will be followed.  If false, links with those names will not be followed.  Defaults to false",false,"false",false,SourceParameterType.BOOLEAN,false,true));
	    
		    put("email.subjectRegex", new SourceParameter("email.subjectRegex", "If set, then all subjects must match this regex to be processed.  If not set, all subjects are processed.",false,".*",false,SourceParameterType.REGEX,false,false));
		    put("email.fromRegex", new SourceParameter("email.fromRegex", "If set, then one of the form addresses (because from address are a header, multiple address may exist even if there is typically just one) must match this regex to be processed.  If not set, all from address are processed.  If both subject and from regex are not set, all emails will be processed by this handler.",false,".*",false,SourceParameterType.REGEX,false,false));

			put("email.sinceTimestamp",  new SourceParameter("email.sinceTimestamp", "Keeps track of the timestamp when the last job started.  Only messages since that time will be processed.  This field is automatically created/updated each time the corresponding job finishes. Must be in ISO 8601 format of YYYY-MM-DDTHH:mm:SSZ", false,"2017-10-11T12:30:01Z",false,SourceParameterType.STRING_TIMESTAMP,false,false));
		
		}};
	
	@Override
	public boolean supportsDomainDiscovery() {
		return false;
	}	
		
		
	@Override
	public JSONObject getHandlerDefaultConfiguration() {
		return SOURCE_HANDLER_CONFIGURATION;
	}

	@Override
	public String getSourceHandlerName() {
		return SOURCE_HANDLER_NAME;
	}
	
	@Override
	public String getSourceHandlerDisplayName() {
		return SOURCE_HANDLER_DISPLAY_NAME;
	}
		
	@Override
	public String getDescription() {
		return "Provides an interface to use emails in a imap server account as a data source.";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.NOT_APPLICABLE;
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}
	
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();

		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue,configuration));
		return errors;
	}		
		
	/**
	 * Checks whether or not the current message should  processed based upon the subject and/or from sender.
	 * 
	 *  if subject pattern and from pattern are both not set, then process the message.
	 *  
	 * @param m
	 * @return
	 * @throws MessagingException 
	 */
	private boolean shouldProcessMessage(Message m) throws MessagingException {
		if (_subjectPattern == null && _fromPattern == null) { return true; }
		
		if (_subjectPattern != null) {
			String subject = m.getSubject();
			
			if (_subjectPattern.matcher(subject).find() == false) {
				srcLogger.log(Level.FINER, "subject regex failed: "+subject);
				return false;
			}			
		}
		
		if (_fromPattern != null) {
			String fromAddresses = StringUtilities.join(m.getFrom(), ";");
			
			if (_fromPattern.matcher(fromAddresses).find() == false) {
				srcLogger.log(Level.FINER, "from regex failed: "+fromAddresses);
				return false;
			}
			
		}

		
		return true;
	}
	private Pattern _subjectPattern = null;
	private Pattern _fromPattern    = null;
	
	
	
	
	
	public void processAttachments(Document messageDoc, JSONObject messageObj, Message mesg) throws MessagingException, IOException {
		boolean updateSourceDocument = false;
		
		if (mesg.getContentType().contains("multipart")) {
			JSONArray multiparts = messageObj.getJSONArray("multiparts");
			
			Multipart multiPart = (Multipart) mesg.getContent();
			for (int i = 0; i < multiPart.getCount(); i++) {
			    MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);			    
			    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			    	Attachment a = Attachment.createFrom(part, true);
	                srcLogger.log(Level.INFO, "mail message processing attachment - "+ a.fileName);
	                
	                
	        		Document attachmentDoc = new Document(messageDoc, a.content, MimeType.getMimeType(a.fileName, a.content), mesg.getReceivedDate().getTime());
	        		attachmentDoc.setType("attachment");
	        		multiparts.getJSONObject(i).put("openkeID", attachmentDoc.getUUID());
	        		updateSourceDocument = true;
	        		
	        		this.getDocumentRouter().processPage(attachmentDoc,"");
			    }
			}
		}
		
		if (updateSourceDocument) {
			messageDoc.updatesourceDocument(messageObj);
		}
	}		
	
	
	
	
	private static final String[] SUBSCRIPTION_KEYWORDS = { "subscrib","manage", "preference", "opt"  };
	private static final String[] EMPTY_KEYWORDS = new String[0];
	
	
	public void process() {
		JSONObject emailConfig = this.getJobConfiguration().getJSONObject("email");
		
		if (emailConfig.has("subjectRegex")) {
			String subjectRegex = emailConfig.getString("subjectRegex").trim();
			if (subjectRegex.length() > 0) {
				_subjectPattern = Pattern.compile(subjectRegex);
			}
		}
		
		if (emailConfig.has("fromRegex")) {
			String fromRegex = emailConfig.getString("fromRegex").trim();
			if (fromRegex.length() > 0) {
				_fromPattern = Pattern.compile(fromRegex);
			}
		}		
		
		String[] keywords = SUBSCRIPTION_KEYWORDS;
		if (emailConfig.has("followSubscriptionMgmtLinks") && emailConfig.getBoolean("followSubscriptionMgmtLinks")) {
			keywords = EMPTY_KEYWORDS;
		}
		
		ZonedDateTime startJobTime = ZonedDateTime.now();
		
		String lastProcessed = "1900-01-01T00:00:00Z";
		if (emailConfig.has("sinceTimestamp")) {
			lastProcessed = emailConfig.getString("sinceTimestamp");
		}
		Instant lastProcessedTime = DateUtilities.getFromString(lastProcessed).toInstant();
		
		String userName = emailConfig.getString("username");
		String host     = emailConfig.getString("host");
		String encryptedPassword = emailConfig.getString("password");
		String decryptedPassword = Collector.getTheCollecter().decryptValue(encryptedPassword);
		
		String inputFolderStr     = emailConfig.getString("inputFolder");
		String processedFolderStr = emailConfig.getString("processedFolder");
		
		Properties mailProps = new Properties();
		mailProps.setProperty("mail.imap.starttls.enable", "true");
	
		try {
			// Connect to the server
			Session session = Session.getDefaultInstance(mailProps, null);
			Store store = session.getStore("imaps");
			store.connect(host, userName, decryptedPassword);
			
			// open the inbox folder
			Folder inboxFolder = store.getFolder(inputFolderStr);
			inboxFolder.open(Folder.READ_WRITE);
			
			// Get the processed folder
			Folder processedFolder = store.getFolder(processedFolderStr);
			if (processedFolder.exists() == false ) {
				processedFolder.create(Folder.HOLDS_MESSAGES);
			}
			
			int numMessages = inboxFolder.getMessageCount();
			int currentMessage = numMessages;
			while (currentMessage > 0) {
				Message[] messages = inboxFolder.getMessages(currentMessage, currentMessage);
				currentMessage--; // ensure next loop processes the next messages
				
				if (messages.length != 1) {
					srcLogger.log(Level.WARNING, "bad length");
					continue;
				}
				Message message = messages[0];
				if (message.getReceivedDate().toInstant().isBefore(lastProcessedTime)) {
					srcLogger.log(Level.INFO, "Found message with received date prior to our last processed time, stop processing emails");
					break;
				}
				if (this.shouldProcessMessage(message) == false) {	continue;}
				
				
				//System.out.println(JavaxMailMesssageHandler.convertToJSONObject(message).toString(4));

				// create the document and route
				try {
					JSONObject messageObject = EMail.convertToJSONObject(message);
					Document currentDocument = new edu.ncsu.las.document.Document(messageObject, MimeType.JAVAX_MAILMESSAGE,"email", this.getJobConfiguration(), this.getJob().getSummary(),"",   host, this.getDomainInstanceName(),messageObject.optString("messageID", ""), this.getJobHistory());
					currentDocument.setURL("mail://"+messageObject.optString("messageID", ""));
					String text = EMail.extractText(message);
					currentDocument.setExtractedTextFromTika(text);
					currentDocument.setExtractedText(text,true);
						
					String html = EMail.extractHTML(message);
					
					Set<String> outgoingLinks = HTMLUtilities.getAllLinks(html, "file://localhost/",keywords);
					currentDocument.setOutgoingURLs(outgoingLinks);
					for(String url: outgoingLinks) {
						this.addURLToCrawl(url, 0);
					}
					
					if (emailConfig.getBoolean("processAttachments")) {
						this.processAttachments(currentDocument,messageObject,message);
					}
					
					
					this.getDocumentRouter().processPage(currentDocument,"");
						
				} 
				catch (MessagingException e) {
					srcLogger.log(Level.SEVERE, "Unable to process javax Message in document: " + e);
				} 
				catch (IOException e) {
					srcLogger.log(Level.SEVERE, "Unable to process javax Message in document: " + e);
				}
					
				// move the processed document to the processed folder
				inboxFolder.copyMessages(messages, processedFolder);
				Flags deleted = new Flags(Flags.Flag.DELETED);
				inboxFolder.setFlags(messages, deleted, true);
				inboxFolder.expunge();
			}
			
			try {
				inboxFolder.close();
				if (processedFolder.isOpen()) { processedFolder.close(); }
			} catch (MessagingException e) {
				srcLogger.log(Level.WARNING, "Unable to folders after processing email job: "+this.getJob().getName(),e);
				return;
			}
			
			
			//update the last processing timestamp of email to be when we started this job
			JSONObject fullConfiguration = this.getJobConfiguration();
			JSONObject emailConfiguration = fullConfiguration.getJSONObject("email");
			emailConfiguration.put("sinceTimestamp", DateUtilities.getDateTimeISODateTimeFormat(startJobTime.toInstant()));
			String id = Configuration.getConfigurationProperty(this.getDomainInstanceName(),ConfigurationType.COLLECTOR_ID);
			this.getJob().updateConfiguration(fullConfiguration,id);
		}
		catch (javax.mail.AuthenticationFailedException afe) {
			srcLogger.log(Level.WARNING, "Unable to process email source job, authentication failed: "+this.getJob().getName());
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process email source job, authentication failed");
			this.setJobHistoryStatus(JobHistoryStatus.ERRORED);	
			return;			
		} catch (NoSuchProviderException e) {
			srcLogger.log(Level.SEVERE, "Unable to process email source job - no imaps provider");
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process email source job - no imaps provider");
			this.setJobHistoryStatus(JobHistoryStatus.ERRORED);	
			return;
		} 
		catch (MessagingException e) {
			srcLogger.log(Level.WARNING, "Unable to process email job: "+this.getJob().getName(),e);
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process email job, exception: "+e);
			this.setJobHistoryStatus(JobHistoryStatus.ERRORED);	
			return;
		}
		
		// now crawl any links we found.  WebSourceHandler will update our job status appropriately...
		if (emailConfig.getBoolean("followLinks")) {
			this.processInternal(_urlsToCrawl);
		}
		else {
			this.processInternal(new HashSet<String>()); // we need process internal to up job states, so start, but don't crawl
		}
		
	}	


	private HashSet<String> _urlsToCrawl = new HashSet<String>();
	public void addURLToCrawl(String url, int depth) {
		
		SourceCrawler sc = new SourceCrawler(this);
				
		WebURL wu = new WebURL();
		wu.setURL(url);
		wu.setDepth((short)depth);
		if (sc.shouldVisit(null, wu)) {
			srcLogger.log(Level.INFO, "Added URL to crawl: "+url);
			_urlsToCrawl.add(url);
		}
		else {
			srcLogger.log(Level.INFO, "Not adding url as shouldVisit criteria was not met to a source handler: "+url);
		}
	}	
	
	/**
	 * Tests the passed in configuration and returns the results as a JSON object.
	 * For the email handler, this ensures that can connect to the email server.
	 * 
	 * @param configution
	 * @return
	 */
	public JSONObject testAuthentication(JSONObject configuration) {
		
		try {
			JSONObject emailConfig = configuration.getJSONObject("email");
					
			String userName = emailConfig.getString("username");
			String host     = emailConfig.getString("host");
			String password = emailConfig.getString("password");
			if (password.startsWith("{AES}")) {
				password = Collector.getTheCollecter().decryptValue(password);
			}
			
			Properties props = new Properties();

			props.setProperty("mail.imap.starttls.enable", "true");
			// Connect to the server
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imaps");
			store.connect(host, userName, password);
			
			JSONArray folders = new JSONArray();
			Folder defaultFolder = store.getDefaultFolder();
			for (Folder child: defaultFolder.list()) {
				folders.put(child.getFullName());
			}

			JSONObject result = new JSONObject().put("status", "success")
                                                .put("topLevelFolders", folders);
			return result;
		}
		catch (MessagingException e) {
			JSONObject result = new JSONObject().put("status", "failed")
					                            .put("message", e.toString());
			return result;
		}

		
	}	
			
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);		
		
		//EmailSourceHandler ddgh = new EmailSourceHandler();

	}
	
}
