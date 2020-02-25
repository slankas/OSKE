package edu.ncsu.las.source;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.EMail;

public class EmailSourceHandlerTest {

	public static void main(String[] args) throws IOException {
		Properties props = new Properties();

		props.setProperty("mail.imap.starttls.enable", "true");
		
		String host = "imap.gmail.com";
		String username = "";
		String password = "";
		String provider = "imaps";

		try {
			// Connect to the server
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore(provider);
			store.connect(host, username, password);

			
			//listAllFolders(store.getDefaultFolder(),"");
			
			// open the inbox folder
			Folder inbox = store.getFolder("INBOX");
			System.out.println(inbox.getClass().getName());
			
			inbox.open(Folder.READ_ONLY);
			

			// get a list of javamail messages as an array of messages
			int numMessages = inbox.getMessageCount();
			
			System.out.println("Num messages in inbox: "+numMessages);
			Message[] messages = inbox.getMessages(Math.max(1, numMessages-100),numMessages);

			for (int i = 0; i < messages.length; i++) {
				printMessage(messages[i]);
			}
			
			inbox.close(false);
			store.close();
		} catch (NoSuchProviderException nspe) {
			System.err.println("invalid provider name");
		} catch (MessagingException me) {
			System.err.println("messaging exception");
			me.printStackTrace();
		}
	}




	
	public static void listAllFolders(Folder currentFolder,String depth) throws MessagingException {
		System.out.println(depth+currentFolder);
		Folder children[] = currentFolder.list();
		for (Folder child: children) {
			listAllFolders(child, depth+"  ");
		}
	}
	
	public static void printMessage(Message m) throws MessagingException, IOException {
		JSONObject messageObject = EMail.convertToJSONObject(m);
		System.out.println(messageObject.toString(4));
		
		return;
	}
	

}
