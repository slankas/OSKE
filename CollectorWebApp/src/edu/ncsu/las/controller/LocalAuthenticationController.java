package edu.ncsu.las.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.EmailClient;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.UserPassword;
import edu.ncsu.las.util.StringValidation;


/**
 * Handles requests for local authentication
 */
@Controller
@RequestMapping("/system/localAuth")
public class LocalAuthenticationController extends AbstractController {
	private static Logger logger = Logger.getLogger(LocalAuthenticationController.class.getName());

	
	/**
	 * User requests a password reset
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/resetPassword", method = RequestMethod.POST)
	public String requestPasswordReset(HttpServletRequest request, @RequestParam(value = "email", required = true) String email) throws Exception {
		logger.log(Level.FINEST, "local auth controller: requestPasswordReset");
		
		if (StringValidation.isValidEmailAddress(email) == false) {
			request.setAttribute("error", "Invalid email address");
			return "loginForm";
		}
		
		String temporaryToken = UserPassword.requestTemporaryAccessToken(email);
		if (temporaryToken != null) {
			String encrypytedEmail = Collector.getTheCollecter().encryptValue(email).substring(5); //remove the leading "{aes}"
			String referrer = request.getHeader("Referer");
			String href = referrer.substring(0,referrer.indexOf(request.getContextPath())) +request.getContextPath() + "/system/localAuth/authenticateTempToken?email="+URLEncoder.encode(encrypytedEmail, "UTF-8")+"&token="+temporaryToken;
			
			
			StringBuilder body = new StringBuilder("Click on the link below to reset your password:<p>&nbsp;");
			body.append("<br><a href=\""+href+"\">Reset OpenKE Password</a>");
			
			
			EmailClient ec = Collector.getTheCollecter().getEmailClient();
			String title = "OpenKE Password Reset";
			
			ec.sendMessage(email, title, body.toString());					
		}
		this.instrumentAPI("authentication.resetPassword", new JSONObject(), System.currentTimeMillis(), null, request, email, Domain.DOMAIN_SYSTEM); 
		
		
		// be "coy" to the user as to whether or not the account token succeeeded or not.  they'll get an email if it does.
		request.setAttribute("error", "If you have an account, you will receive an email to reset your password.");	
		return "loginForm";
	}	

	/**
	 * User attempts to authenticate using a temporary access token so they can perform a
	 * password reset.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/authenticateTempToken", method = RequestMethod.GET)
	public String authenticateViaTemporaryAccessToken(HttpServletRequest request, 
			                                          @RequestParam(value = "email", required = true) String encryptedEmail,
			                                          @RequestParam(value = "token", required = true) String accessToken) throws Exception {
		logger.log(Level.FINEST, "local auth controller: authenticate via token");
		
		String emailID = Collector.getTheCollecter().decryptValue("{AES}" +encryptedEmail);
		String result = UserPassword.authenticateUserViaTemporaryToken(emailID, accessToken);
		this.instrumentAPI("authentication.authenticateTempToken", new JSONObject().put("result",result), System.currentTimeMillis(), null, request, emailID, Domain.DOMAIN_SYSTEM); 
		if (result.equals(UserPassword.RESULT_MUST_CHANGE_PASSWORD)) {
			User u = User.findUser(emailID.toLowerCase());
			if (u != null) {
				request.getSession(true).setAttribute("user", u);
				request.getSession().setAttribute("mustChangePassword", Boolean.TRUE);
				

				
				return "passwordChangeForm"; 
			}
			else {
				request.setAttribute("error", "Unable to locate user record");
			}
		}
		else {
			request.setAttribute("error", "Unable to authorize token");
		}
		
		return "loginForm";
	}	
	
	/**
	 * User attempts to authenticate normally
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/authenticate", method = RequestMethod.POST)
	public String authenticate(HttpServletRequest request, 
			                                          @RequestParam(value = "email", required = true) String email,
			                                          @RequestParam(value = "password", required = true) String password) throws Exception {
		logger.log(Level.FINEST, "local auth controller: authenticate loginForm");
		if (StringValidation.isValidEmailAddress(email) == false) {
			request.setAttribute("error", "Invalid email address");
			return "loginForm";
		}
		
		String result = UserPassword.authenticateUser(email, password);
		this.instrumentAPI("authentication.authenticate", new JSONObject().put("result",result), System.currentTimeMillis(), null, request, email, Domain.DOMAIN_SYSTEM); 

		if (result.equals(UserPassword.RESULT_SUCCESS) || result.equals(UserPassword.RESULT_MUST_CHANGE_PASSWORD) || result.equals(UserPassword.RESULT_PASSWORD_AGE)) {
			User u = User.findUser(email.toLowerCase());
			if (u != null) {
				request.getSession(true).setAttribute("user", u);
				this.validateUserAndSetPageAttributes(request);
				if (result.equals(UserPassword.RESULT_MUST_CHANGE_PASSWORD) || result.equals(UserPassword.RESULT_PASSWORD_AGE)) {
					request.getSession().setAttribute("mustChangePassword", Boolean.TRUE);
					return "passwordChangeForm"; 
				}
				else {
					return "index";
				}
			}
			else {
				request.setAttribute("error", "Unable to locate user record");
			}
		}
		
		else {
			request.setAttribute("error", result);
		}
		
		return "loginForm";
	}		
	
	
	@RequestMapping(value = "/changePassword", method = RequestMethod.POST)
	public String changePassword(HttpServletRequest request, 
			                     @RequestParam(value = "newPassword", required = true) String newPassword,
			                     @RequestParam(value = "verifyPassword", required = true) String verifyPassword) throws Exception {
		logger.log(Level.FINEST, "local auth controller: authenticate loginForm");
		
		this.validateUserAndSetPageAttributes(request);
		User u = edu.ncsu.las.webapp.Authentication.getUser(request);
		this.instrumentAPI("authentication.changePassword", new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM); 

		//
		
		if (newPassword.equals(verifyPassword)) {
			java.util.List<String> errors = UserPassword.validateAccount(u.getEmailID(), newPassword);
			if (errors.size() == 0) {
				if (UserPassword.changePassword(u.getEmailID(),newPassword)) {
					request.getSession().setAttribute("mustChangePassword", null);
					return "index";
				}
				else {
					request.setAttribute("error", "Unable to change password.");
					return "passwordChangeForm";
				}
			}
			else {
				if (errors.size() == 1) {
					request.setAttribute("error", errors.get(0));
				}
				else {
					StringBuilder sb = new StringBuilder("<ul>");
					for (String s: errors) {
						sb.append("<li>");
						sb.append(s);
					}
					sb.append("</ul>");
					request.setAttribute("error",sb.toString());
				}
				return "passwordChangeForm";
			}
		}
		else {
			request.setAttribute("error","Entered passwords do not match.");
			return "passwordChangeForm"; 
		}
	}		
	
	
	/**
	 * User attempts to authenticate through binding to an LDAP server
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/authenticateLDAP", method = RequestMethod.POST)
	public String authenticateLDAP(HttpServletRequest request, 
			                                          @RequestParam(value = "email", required = true) String email,
			                                          @RequestParam(value = "password", required = true) String password) throws Exception {
		logger.log(Level.FINEST, "local auth controller: authenticate loginForm");
		
		if (StringValidation.isValidEmailAddress(email) == false) {
			request.setAttribute("error", "Invalid email address");
			return "loginForm";
		}
		
		String id = email.substring(0, email.indexOf("@"));
		String result = UserPassword.authenticateUserLDAP(id, password);
		this.instrumentAPI("authentication.authenticateLDAP", new JSONObject().put("result",result), System.currentTimeMillis(), null, request, email, Domain.DOMAIN_SYSTEM); 
		if (result.equals(UserPassword.RESULT_SUCCESS)) {
			User u = User.findUser(email.toLowerCase());
			if (u != null) {
				request.getSession(true).setAttribute("user", u);
				this.validateUserAndSetPageAttributes(request);
				return "index";
			}
			else {
				request.setAttribute("error", "Unable to locate user record");
			}
		}
		else {
			request.setAttribute("error", result);
		}
		
		return "loginForm";
	}		
	
	
}

