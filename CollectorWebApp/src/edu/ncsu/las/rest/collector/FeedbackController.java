package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.EmailClient;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.RoleType;


/**
 * Handles requests for the Job.
 * 
 */
@RequestMapping(value = "rest/{domain}/feedback")
@Controller
public class FeedbackController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	
	/**
	 * Allows the user to submit feedback, which is emailed to the administrators for the current domain 
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 * @throws MessagingException 
	 * @throws AddressException 
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody byte[] submitFeedback(HttpServletRequest request, @RequestBody String feedback, @PathVariable("domain") String domainStr) throws ValidationException, IOException, AddressException, MessagingException {
		logger.log(Level.INFO, "FeedbackController: submitFeedback : " + feedback);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		JSONObject feedbackJSON = new JSONObject(feedback);
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.FeedbackController.submitFeedback", feedbackJSON, System.currentTimeMillis(), null, request,domainStr);

		
		String userEmailAddress = this.getEmailAddress(request);

		StringBuilder body = new StringBuilder("OpenKE Feedback Submitted<br>");
		body.append("<br>Subject: " + feedbackJSON.optString("subject"));
		body.append("<br>User: " + this.getUser(request).getName());
		body.append("<br>User Email: "+userEmailAddress);
		body.append("<br>Domain: "+domainStr);
		body.append("<br>Comments:<br>");
		body.append(feedbackJSON.optString("comments"));
		
		String administrators[] = User.getAdministratorEmails(domainStr);
		EmailClient ec = Collector.getTheCollecter().getEmailClient();
		String title = "OpenKE Feedback ("+domainStr+"): " + feedbackJSON.getString("subject");
		ec.sendMessage(Arrays.asList(administrators),new java.util.ArrayList<String>(),new java.util.ArrayList<String>(), title, body.toString());				
				
		feedbackJSON.put("status", "sent");
		return feedbackJSON.toString().getBytes("UTF-8");
	}
	
}