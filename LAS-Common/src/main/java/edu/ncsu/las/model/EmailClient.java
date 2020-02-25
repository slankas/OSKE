package edu.ncsu.las.model;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.logging.Logger;


/**
 * Used to send out emails. 
 * 
 */

public class EmailClient {
	private static final Logger logger =Logger.getLogger(EmailClient.class.getName());
	
	private Properties _mailServerProperties;
	private Session _mailSession;
	
	private EmailConfiguration _configuration;
	
	public EmailClient(EmailConfiguration configuration) {
		_configuration = configuration;
		
		_mailServerProperties = System.getProperties();
		_mailServerProperties.put("mail.smtp.port", configuration.getPort());
		_mailServerProperties.put("mail.smtp.auth", "true");
		_mailServerProperties.put("mail.smtp.starttls.enable", "true");
		_mailSession = Session.getDefaultInstance(_mailServerProperties, null);
		

	}

	public void sendMessage(List<String> toUsers, List<String> ccUsers, List<String> bccUsers, String subject, String emailBody) throws AddressException, MessagingException {
		MimeMessage mailMessage  = new MimeMessage(_mailSession);

		for (String id: toUsers)  { mailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(id)); }
		for (String id: ccUsers)  { mailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(id)); }
		for (String id: bccUsers) { mailMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(id)); }
		
		this.sendMessage(mailMessage, subject, emailBody);
	}

	public void sendMessage(String toEmailID, String subject, String emailBody) throws AddressException, MessagingException {
		MimeMessage mailMessage  = new MimeMessage(_mailSession);

		mailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmailID));
		this.sendMessage(mailMessage, subject, emailBody);
	}
	
	private void sendMessage(MimeMessage mailMessage, String subject, String emailBody) throws AddressException, MessagingException {
		mailMessage.setSubject(subject);
		mailMessage.setFrom(new InternetAddress(_configuration.getUser()));
		mailMessage.setContent(emailBody, "text/html");
		
		final Transport transport = _mailSession.getTransport("smtp");
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				transport.connect("smtp.gmail.com", _configuration.getUser(), _configuration.getPassword());
				transport.sendMessage(mailMessage, mailMessage.getAllRecipients());
				transport.close();
			}
			catch (Exception ex) {
				logger.log(Level.WARNING, "Email Client - send Message, unable to send", ex);
			}
		});		
		
	}
}
