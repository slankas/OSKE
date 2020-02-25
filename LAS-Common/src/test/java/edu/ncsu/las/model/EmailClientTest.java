package edu.ncsu.las.model;

/**
 * 
 *
 */
public class EmailClientTest {


	@org.testng.annotations.Test
	public void testSimpleSend()  {
		try {
			EmailConfiguration ec  = new EmailConfiguration("smtp.mail.yahoo.com", 25, "", ""); //this will fail with the current configuration
			EmailClient client = new EmailClient(ec);
			client.sendMessage("user@domain.com", "Test message", "Testing if we can send an through port 25...");
			System.out.println("sent message");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
