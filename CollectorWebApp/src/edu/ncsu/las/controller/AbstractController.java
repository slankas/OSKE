package edu.ncsu.las.controller;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.UserAgreement;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.util.DateUtilities;


@Controller
public abstract class AbstractController {
	protected static Logger logger = Logger.getLogger("controller");	
	
/*	@ExceptionHandler(ValidationException.class)
	public void handleApplicationExceptions(Throwable exception, HttpServletResponse response) {

		if (exception instanceof ValidationException) {
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
			} catch (IOException e) {
				logger.warn("UserController.handleApplicationExceptions: unable to send error - " + exception);
				e.printStackTrace();
			}
		}
	}
*/
	
	@ExceptionHandler(Throwable.class)
	public String handleAuthenticationExceptions(Throwable exception,  HttpServletRequest request) {
		if (exception instanceof OAuthException) {
			return "google";
		}
		if (exception instanceof LocalAuthenticationException) {
			request.getSession(true).setAttribute("allowPasswordReset", Boolean.TRUE);
			return "loginForm";
		}
		else if (exception instanceof LDAPAuthenticationException) {
			request.getSession(true).setAttribute("allowPasswordReset", Boolean.FALSE);
			return "loginForm";
		}
		else if (exception instanceof AuthenticationException) {
			return "authenticationError";
		}
		else if (exception instanceof UserAgreementException){
			request.setAttribute("message", exception.getMessage());
			return "userAgreement";
		}
		else if (exception instanceof UserAgreementReviewException){
			return "userAgreementReview";
		}
		else if (exception instanceof OfflineException){
			return "offline";
		}
		else {
			logger.log(Level.SEVERE, "Exception: ", exception);
			return "applicationError";
		}
	}	
	/*
	@ExceptionHandler(Exception.class)
	public String handleApplicationExceptions(Throwable exception) {
		logger.log(Level.SEVERE, "Exception: ", exception);
		return "applicationError";
	}
	*/
	
	public static class ValidationException extends Exception {
		private static final long serialVersionUID = 1L;
		public ValidationException(String message) {
			super(message);
		}
	}

	public static class AuthenticationException extends Exception {
		private static final long serialVersionUID = 1L;
		public AuthenticationException(String message) {
			super(message);
		}
	}
	

	public static class OAuthException extends AuthenticationException {
		private static final long serialVersionUID = 1L;
		public OAuthException(String message) {
			super(message);
		}
	}
	
	public static class LocalAuthenticationException extends AuthenticationException {
		private static final long serialVersionUID = 1L;
		public LocalAuthenticationException(String message) {
			super(message);
		}
	}

	public static class LDAPAuthenticationException extends AuthenticationException {
		private static final long serialVersionUID = 1L;
		public LDAPAuthenticationException(String message) {
			super(message);
		}
	}
	
	
	/**
	 * This exception is thrown when a domain is offline.
	 * 
	 * 
	 */
	public static class OfflineException extends Exception {
		private static final long serialVersionUID = 1L;
		public OfflineException(String message) {
			super(message);
		}
	}
	
	
	
	public static class UserAgreementReviewException extends Exception {
		private static final long serialVersionUID = 1L;
		public UserAgreementReviewException(String message) {
			super(message);
		}
	}

	
	
	
	/**
	 * A UserAgreementException is thrown when the user is validated, but he/she
	 * does not have an approved user agreement in place.
	 * 
	 *
	 */
	public static class UserAgreementException extends Exception {
		private static final long serialVersionUID = 1L;
		public UserAgreementException(String message) {
			super(message);
		}
	}
	
	public static String getInternalUniqueSessionID(HttpServletRequest request) {
		HttpSession httpSession = request.getSession();
		
		synchronized (httpSession) {   //synchronizing to prevent multiple concurrent requests from the same user getting different session IDs
			if (httpSession.getAttribute("uuid") == null) {
				httpSession.setAttribute("uuid", UUID.randomUUID().toString());
			}
		}
		return httpSession.getAttribute("uuid").toString();
	}
	
	public static String getRemoteAddress(HttpServletRequest request) {
		String header = request.getHeader("x-real-ip");
		if (header == null) {
			header = request.getRemoteAddr();
		}
		return header;
	}
	
	public void sendMessageToUser(HttpServletRequest request, String message) {
		if (request.getAttribute("messages") == null) {
			request.setAttribute("messages", new java.util.ArrayList<String>());
		}
		if (request.getAttribute("messages") instanceof java.util.ArrayList<?>) {
			@SuppressWarnings("unchecked")
			java.util.ArrayList<String> messages = (java.util.ArrayList<String>) request.getAttribute("messages");
			messages.add(message);
		}
		else {
			logger.log(Level.WARNING, "Invalid type for messages: "+ request.getAttribute("messages").getClass().getName());
		}
		
	}
	
	public void validateUserAndSetPageAttributes(HttpServletRequest request) throws AuthenticationException, UserAgreementException, UserAgreementReviewException {
		 User u = edu.ncsu.las.webapp.Authentication.getUser(request);
		 
		 if (u == null) {
			 if (Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_METHOD).equalsIgnoreCase("oauth2")) {
				 throw new OAuthException("no user defined");
			 }
			 else if (Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_METHOD).equalsIgnoreCase("local")) {
				 throw new LocalAuthenticationException("no user defined");
			 }
			 else if (Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_METHOD).equalsIgnoreCase("ldap")) {
				 throw new LDAPAuthenticationException("no user defined");
			 }			 
			 else {
				 throw new AuthenticationException("No such user exists"); 
			 }
		 }
		 request.setAttribute("givenUserId", u.getEmailID());
		 request.setAttribute("givenName", u.getName());
		 request.setAttribute("userRole", u);

		 if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_REQUIRE_USERAGREEMENT)) {
			 if (u.hasActiveAndApprovedAgreement() == false) {
				 request.setAttribute("noHeaderButtons", true);
				 
				 UserAgreement ua = UserAgreement.getMostRecentUserAgreement(u.getEmailID());
				 if (ua == null) {
					 throw new UserAgreementException("You must complete a user agreement form and have it approved to access OpenKE.");
				 }
				 switch(ua.getStatus()) {
				 case UserAgreement.STATE_EXPIRED:  
				 case UserAgreement.STATE_APPROVED: throw new UserAgreementException("Your user agreement has expired.  You need to submit another form to access OpenKE.");
				 case UserAgreement.STATE_DENIED:   throw new UserAgreementException("Your user agreement has denied.  You need to submit another form to access OpenKE.");
				 case UserAgreement.STATE_REVIEW:   throw new UserAgreementReviewException("agreement under review");
				 case UserAgreement.STATE_REVOKED:  throw new UserAgreementException("Your user agreement was revoked.  You need to submit another form to access OpenKE.");
				 case UserAgreement.STATE_REWORK:   throw new UserAgreementException("Your user agreement was sent back for changes.  You will need to submit the form again to access OpenKE.");
				 default: throw new UserAgreementException("Invalid status: "+ua.getStatus());
				 }
			 }
			 else {
				 HttpSession httpSession = request.getSession();
				 // if the user's agreement will expire, alert them, but only do so once per session..
				 if (httpSession.getAttribute("userAgreementExpirationCheck") == null) {
					 if (u.getActiveAndApprovedUserAgreement().willExpire(30)) {
						 UserAgreement possibleNew = UserAgreement.getMostRecentUserAgreement(u.getEmailID());
						 // make sure the user hasn't already submitted a new one ...
						 if ( possibleNew != null && !(possibleNew.getExpirationTimestamp().after(u.getActiveAndApprovedUserAgreement().getExpirationTimestamp()) && possibleNew.getStatus().equals(UserAgreement.STATE_REVIEW))) {
							 request.setAttribute("userAgreementExpiration", DateUtilities.getDateTimeISODateTimeFormat(u.getActiveAndApprovedUserAgreement().getExpirationTimestamp().toInstant()));
							 httpSession.setAttribute("userAgreementExpirationCheck", Boolean.TRUE);
						 }
					 }
				 }	 
			 }
			 
		 }
	}
	
	public void validateUserWithDomainAndSetPageAttributes(HttpServletRequest request,String domain) throws AuthenticationException, UserAgreementException, UserAgreementReviewException,OfflineException {
		this.validateUserAndSetPageAttributes(request);

		User u = edu.ncsu.las.webapp.Authentication.getUser(request);  // we know u is valid from the previous method call which throws an exception if not ...
		 
		Domain domainObj = Collector.getTheCollecter().getDomain(domain);
		if (u.hasDomainAccess(domain) == false || domainObj == null) {
			throw new AuthenticationException("No access to domain: "+domain);			 
		}
		
		if (domainObj.isOffline()) {
			request.setAttribute("message", "Domain offline: "+domain);
			throw new OfflineException("Domain offline: "+domain);	
		}

		request.setAttribute("domain", domain);
	}

	public String getCheckAuthorization(HttpServletRequest request, RoleType requiredRole, String domain, String successPage, String failurePage) {
		if (edu.ncsu.las.webapp.Authentication.getUser(request).hasAccess(domain, requiredRole) == false) {
			return failurePage;
		}
		else {
			return successPage;
		}
	}	
	public void instrumentAPI(String eventDescription, JSONObject eventDetails, long startTime, Long endTime, HttpServletRequest httpRequest, String domainStr)  {
		JSONObject dataObject    = InstrumentationController.createRequestDataObject(eventDescription, startTime, endTime, eventDetails);
        JSONObject contentObject = InstrumentationController.createEventObject(httpRequest, domainStr, 	edu.ncsu.las.webapp.Authentication.getUser(httpRequest), dataObject);
        Instrumentation.sendEvent(contentObject, domainStr);
	}
	
	public void instrumentAPI(String eventDescription, JSONObject eventDetails, long startTime, Long endTime, HttpServletRequest httpRequest, String userID, String domainStr)  {
		JSONObject dataObject    = InstrumentationController.createRequestDataObject(eventDescription, startTime, endTime, eventDetails);
        JSONObject contentObject = InstrumentationController.createEventObject(httpRequest, domainStr, 	userID, dataObject);
        Instrumentation.sendEvent(contentObject, domainStr);
	}	
	
}
