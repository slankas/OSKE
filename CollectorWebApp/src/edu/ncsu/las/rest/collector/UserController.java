package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
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
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.UserPassword;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.util.StringValidation;
import edu.ncsu.las.webapp.Authentication;

/**
 * Handles requests to view / create / change users.
 * For the most part, only domain administrators can access these records.
 * Users can view their only records 
 */
@RequestMapping(value = "rest/{domain}/user/")
@Controller
public class UserController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	/**
	 * validates and creates a new user as appropriate
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String createUser(HttpServletRequest request, @RequestBody String userStr,@PathVariable("domain") String domainStr) throws IOException, ValidationException, Exception {
		logger.log(Level.FINER, "UserController - adduser");
		this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR);
		
		JSONObject userJSON = new JSONObject(userStr);
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.createUser", userJSON,System.currentTimeMillis(),null, request,domainStr);
		
		if (domainStr.equals(Domain.DOMAIN_SYSTEM)) {
			domainStr = userJSON.getString("domain");
			if (Collector.getTheCollecter().getDomain(domainStr) == null) {
				throw new ValidationException("Attempted to setup user for an invalid domain.");
			}
		}

		//System.out.println(userJSON.toString(4));
		
		String emailID = userJSON.getString("emailID");
		String name    = "";
		RoleType role  = RoleType.getEnum(userJSON.getString("role"));
		
		//TODO: need logic that role is valid
		
		Integer index1 = emailID.indexOf('(');
		Integer index2 = emailID.indexOf(')');
		if (index1 > 0 && index2 > 0) {
			name    = emailID.substring(0,index1).trim().toLowerCase();
			emailID = emailID.substring(index1 + 1, index2).trim();
		}
		else {
			JSONObject result = new JSONObject().put("status", "error").put("message", "User must be in the format of Name (email address)");
			return result.toString();
		}
		if (StringValidation.isValidEmailAddress(emailID) == false) {
			JSONObject result = new JSONObject().put("status", "error").put("message", "Email address is not valid.");
			return result.toString();
		}
		
		logger.log(Level.FINER, "email: " + emailID);
		logger.log(Level.FINER, "name: " + name);
		
		if (domainStr.equals(Domain.DOMAIN_SYSTEM) && !(role == RoleType.ADMINISTRATOR || role == RoleType.ADJUDICATOR)) {
			throw new ValidationException("Only administrators and adjudicators may be added to the system domain.");
		}

		if (name.length() == 0 || emailID.length() == 0) {
			JSONObject result = new JSONObject().put("status", "error").put("message", "User must be in the format of Name (email address)");
			return result.toString();
		}


		User u = User.findUser(emailID);
		if (u != null && u.hasDomainAndRole(domainStr,role)) {
			JSONObject result = new JSONObject().put("status", "error").put("message", "User and role already exist");
			return result.toString();
		}
		
		User newUser = new User(emailID);
		newUser.setName(name);
		
		User.RoleDomainAccess rda = newUser.new RoleDomainAccess(role, domainStr, "active", Timestamp.from(Instant.now()), this.getUser(request).getEmailID());
		newUser.createUser(rda);
		newUser.addAccess(rda);
		
		if (u == null && Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_METHOD).equalsIgnoreCase("local")) {
			//we are using local authentication and just created a new record.  Create a password account and contact the user
			String temporaryToken = UserPassword.requestTemporaryAccessToken(emailID);
			if (temporaryToken != null) {
				String encrypytedEmail = Collector.getTheCollecter().encryptValue(emailID).substring(5); //remove the leading "{aes}"
				String referrer = request.getHeader("Referer");
				String href = referrer.substring(0,referrer.indexOf(request.getContextPath())) + request.getContextPath() + "/system/localAuth/authenticateTempToken?email="+URLEncoder.encode(encrypytedEmail, "UTF-8")+"&token="+temporaryToken;
				StringBuilder body = new StringBuilder("An OpenKE account has been created for you.<p>Click on the link below to set your password:<p>&nbsp;");
				body.append("<br><a href=\""+href+"\">Reset OpenKE Password</a>");
				
				EmailClient ec = Collector.getTheCollecter().getEmailClient();
				String title = "OpenKE Account";
				
				ec.sendMessage(emailID, title, body.toString());					
			}			
		}
		
		logger.log(Level.FINEST,	"addUser: " + newUser);
		
		sendUserActionNoticeEmail(domainStr,newUser,"create", this.getEmailAddress(request));
					
		JSONObject result = new JSONObject().put("status", "success");
		result.put("user", newUser.toJSON());
		return result.toString();


	}

	/*
	 * Returns all users for a particular domain
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getAllUsersForDomain(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr)	throws IOException, ValidationException {
		logger.log(Level.FINER,	"UserController: received request to refresh User Table");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ADMINISTRATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.getAllUsersForDomain", new JSONObject(),System.currentTimeMillis(),null, httpRequest,domainStr);

		JSONObject result = new JSONObject();
		JSONArray users = new JSONArray();
			
		for (User u: User.getUsers(domainStr)) {
			users.put(u.toJSON());		
		}
		result.put("users", users);
		return result.toString();		

	}

	/**
	 * Returns a specific user.  Admins can see the complete records of any users.  A user can view their own records.
	 * 
	 */
	@RequestMapping(value = "{emailid:.+}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String retreiveSpecificUser(HttpServletRequest request, @PathVariable("emailid") String emailID,HttpServletRequest httpRequest,@PathVariable("domain") String domainStr)	throws  IOException, ValidationException {
		logger.log(Level.FINER,	"UserController: received request to get user");
		this.validateAnyAuthorization(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.retreiveSpecificUser", new JSONObject().put("userID", emailID),System.currentTimeMillis(),null, httpRequest,domainStr);

		User currentUser = this.getUser(httpRequest);
		if (currentUser.getEmailID().equalsIgnoreCase(emailID) == false) {
			this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR);
		}

		User user = User.findUser(emailID);
		return user.toJSON().toString();
	}

	
	/**
	 * 
	 * @param emailid note: this is passed as base64 encoding.  We actually get the email id from the request body
	 * @param userStr
	 * @param request
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@RequestMapping(value = "{emailid:.+}", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody String updateUser(@PathVariable("emailid") String emailid, @RequestBody String userStr, HttpServletRequest httpRequest,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		JSONObject userJSON = new JSONObject(userStr);
		logger.log(Level.FINER, "Edit user: "+ userJSON.toString());
		this.validateAuthorization(httpRequest, domainStr, RoleType.ADMINISTRATOR, false);
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.updateUser", userJSON,System.currentTimeMillis(),null, httpRequest,domainStr);

		String emailID   = userJSON.getString("emailID");
		RoleType role    = RoleType.getEnum(userJSON.getString("role"));
		RoleType newRole = userJSON.has("newRole") ? RoleType.getEnum(userJSON.getString("newRole")) : null;
		String newStatus = userJSON.has("newStatus") ? userJSON.getString("newStatus") : null;
		
		// if we are in the system domain, we allow all those administrators to change roles/status in any domain - provided they also administer that domain 
		/* Code not needed: the client side code passes the actual domain in the request header.
		if (domainStr.equals(Domain.DOMAIN_SYSTEM)) {
			domainStr        = userJSON.getString("domain");
			this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR,false);
		}*/
		
		boolean result= false;
		
		User u = User.findUser(emailID, role,domainStr);
		if (u == null) {
			throw new ValidationException("User not found: "+emailID, 404);
		}
		
		if (newRole != null) {
			if (domainStr.equals(Domain.DOMAIN_SYSTEM) && !(newRole == RoleType.ADMINISTRATOR || newRole == RoleType.ADJUDICATOR )) {
				throw new ValidationException("Only administrators and adjudicators may be added to the system domain.");
			}
			
			User.RoleDomainAccess rda = u.getFirstAccess();
			
			result = rda.updateRole(newRole,domainStr,this.getUser(httpRequest).getEmailID());
			sendUserActionNoticeEmail(domainStr,u,"role change", this.getEmailAddress(httpRequest));
		}

		if (newStatus != null) {
			User.RoleDomainAccess rda = u.getFirstAccess();

			result = rda.updateStatus(newStatus,domainStr,this.getUser(httpRequest).getEmailID());
			sendUserActionNoticeEmail(domainStr,u,"status change", this.getEmailAddress(httpRequest));
		}
		
		return new JSONObject().put("result", result).toString();
	}

	
	
	//TODO: These two probably need to move to a new location
	
	/**
	 * Authenticate a user via Google OAuth2
	 * @throws GeneralSecurityException 
	 * 
	 */
	@RequestMapping(value = "authenticate", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String authenticateGoogleToken(HttpServletRequest request, HttpServletRequest httpRequest,@PathVariable("domain") String domainStr, @RequestBody String userStr)	throws  IOException, ValidationException, GeneralSecurityException {
		logger.log(Level.FINER,	"UserController: received request to authenticate user via Google OAuth2");
		
		JSONObject reqObj = new JSONObject(userStr);
		String googleToken = reqObj.getString("token");
		User user = Authentication.getUser(request, googleToken);
		if (user !=null) {
			this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.authenticateGoogleToken", new JSONObject().put("status", "success"),System.currentTimeMillis(),null, httpRequest,domainStr);

			return user.toJSON().toString();
		}
		else {
			this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.authenticateGoogleToken", new JSONObject().put("status", "failure"),System.currentTimeMillis(),null, httpRequest,domainStr);

			return "{ \"error\": \"No such user in the system.\"}";
		}
	}	
	
	/**
	 * Used to destroy the current user's session - This will force the authorization to be re-established
	 * (necessary when any roles/domains are changed for the current user). 
	 * 
	 * If this needs to be done automatically, then we'd also need to store the user's sessions by their userID
	 * as well.
	 * 
	 * @param httpRequest
	 * @return Empty json object
	 * @throws IOException
	 */
	@RequestMapping(value = "session",  method = RequestMethod.DELETE, headers = "Accept=application/json")
	public @ResponseBody String deleteUserSession(HttpServletRequest httpRequest) throws IOException {
		logger.log(Level.FINER,"user Controller: logout");
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.deleteUserSession", new JSONObject(),System.currentTimeMillis(),null, httpRequest,Domain.DOMAIN_SYSTEM);

		httpRequest.getSession(true).invalidate();
		
		return "{}";
	}		
	
	@RequestMapping(value = "session/setHomePage/{newHome}",  method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String setUserHomePageSession(HttpServletRequest httpRequest, @PathVariable("newHome") String newHome) throws IOException, ValidationException {
		logger.log(Level.FINER,"user Controller: set user home page session");

		if (newHome == null  || !(newHome.equals("analyst") || newHome.equals("collector"))) {
			throw new ValidationException("Invalid home: must be analyst or collector");
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserController.setUserHomePageSession", new JSONObject().put("homepage", newHome),System.currentTimeMillis(),null, httpRequest,Domain.DOMAIN_SYSTEM);
		
		httpRequest.getSession(true).setAttribute("home", newHome);
		
		return new JSONObject().put("status", "success").toString();
	}		
	
	
	
	private void sendUserActionNoticeEmail(String domain, User u, String action, String userID) {

		try {
			String adjudicators[] = User.getAdjudicatorEmails(domain);	
			if (adjudicators.length > 0) {
				EmailClient ec = Collector.getTheCollecter().getEmailClient();
				String title = Collector.getTheCollecter().getDomain(domain).getFullName() +" - user "+action+": "+u.getName();
				ec.sendMessage(Arrays.asList(adjudicators),new java.util.ArrayList<String>(),new java.util.ArrayList<String>(), title, userID+" performed:<br>"+ u.toJSON().toString());
			}
			else {
				logger.log(Level.WARNING, "No adjudicators exist for the domain, no user email notifications sent: " + domain);
			}
		} catch (MessagingException e) {
			logger.log(Level.WARNING, " unable to send user notification email: " + e);
		}
	}	
}