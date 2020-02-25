package edu.ncsu.las.rest.collector;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.controller.InstrumentationController;
import edu.ncsu.las.controller.AbstractController.AuthenticationException;
import edu.ncsu.las.controller.AbstractController.OfflineException;
import edu.ncsu.las.controller.AbstractController.UserAgreementException;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;


@Controller
public abstract class AbstractRESTController {
	protected static Logger logger = Logger.getLogger("controller");	
	
	@ExceptionHandler(ValidationException.class)
	public void handleApplicationValidationExceptions(ValidationException exception, HttpServletResponse response) {
		logger.log(Level.FINE, "Exeption: ",exception);
		try {
			response.sendError(exception.getStatusCode(), exception.getMessage());
		} catch (IOException e) {
			logger.log(Level.FINE, "Unable to send error message");
		}
	}
	
	@ExceptionHandler(AuthenticationException.class)
	public void handleAuthenticationException(AuthenticationException exception, HttpServletResponse response){
		logger.log(Level.FINE, "Exeption: ",exception);
		try {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
		} catch (IOException e) {
			logger.log(Level.FINE, "Unable to send error message");
		}
	}

	@ExceptionHandler(UserAgreementException.class)
	public void handleUserAgreementException(UserAgreementException exception, HttpServletResponse response){
		logger.log(Level.FINE, "Exeption: ",exception);
		try {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
		} catch (IOException e) {
			logger.log(Level.FINE, "Unable to send error message");
		}
	}
	
	
	@ExceptionHandler(OfflineException.class)
	public void handleApplicationExceptions(OfflineException exception, HttpServletResponse response) {
		logger.log(Level.SEVERE, "Exeption: ",exception);
		try {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, exception.getMessage());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to send error message");
		}
	}		
	
	@ExceptionHandler(Exception.class)
	public void handleApplicationExceptions(Throwable exception, HttpServletResponse response) {
		logger.log(Level.SEVERE, "Exeption: ",exception);
		try {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to send error message");
		}
	}	
	
	public User getUser(HttpServletRequest request) {
		return edu.ncsu.las.webapp.Authentication.getUser(request);
	}
	
	public void validateAuthorization(HttpServletRequest request, String domain, RoleType requiredRole) throws ValidationException {
		this.validateAuthorization(request, domain, requiredRole, true);
	}
	
	public void validateAuthorization(HttpServletRequest request, String domain, RoleType requiredRole, boolean checkOffline) throws ValidationException {
		Domain domainObj = Collector.getTheCollecter().getDomain(domain);
		User u = this.getUser(request);
		if (u == null || u.hasAccess(domain, requiredRole) == false || domainObj == null) {
			throw new ValidationException("Insufficient access", HttpServletResponse.SC_FORBIDDEN);
		}
		
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_REQUIRE_USERAGREEMENT) &&  !u.hasActiveAndApprovedAgreement()) {
			throw new ValidationException("User does not have a current access agreement",HttpServletResponse.SC_FORBIDDEN);
		}
		
		if (checkOffline && domainObj.isOffline()) {
			request.setAttribute("message", "Domain offline: "+domain);
			throw new ValidationException("Domain offline: "+domain, HttpServletResponse.SC_SERVICE_UNAVAILABLE);	
		}
	}	
	
	
	public void validateAnyAuthorization(HttpServletRequest request) throws ValidationException {
		User u = this.getUser(request);
		if (u == null || u.hasAnyActiveAccess() == false) {
			throw new ValidationException("Insufficient access", HttpServletResponse.SC_FORBIDDEN);
		}
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_REQUIRE_USERAGREEMENT) &&  !u.hasActiveAndApprovedAgreement()) {
			throw new ValidationException("User does not have a current access agreement",HttpServletResponse.SC_FORBIDDEN);
		}		
		
	}
	
	public String getEmailAddress(HttpServletRequest request) {
		User u = this.getUser(request);
		return u.getEmailID();
	}
	
	public String getInternalUniqueSessionID(HttpServletRequest request) {
		HttpSession httpSession = request.getSession();
		
		synchronized (httpSession) {   //synchronizing to prevent multiple concurrent requests from the same user getting different session IDs
			if (httpSession.getAttribute("uuid") == null) {
				httpSession.setAttribute("uuid", UUID.randomUUID().toString());
			}
		}
		return httpSession.getAttribute("uuid").toString();
	}
	
	public String getRemoteAddress(HttpServletRequest request) {
		String header = request.getHeader("x-real-ip");
		if (header == null) {
			header = request.getRemoteAddr();
		}
		return header;
	}
	
	public void instrumentAPI(String eventDescription, JSONObject eventDetails, long startTime, Long endTime, HttpServletRequest httpRequest, String domainStr) {
		JSONObject dataObject    = InstrumentationController.createRequestDataObject(eventDescription, startTime, endTime, eventDetails);
        JSONObject contentObject = InstrumentationController.createEventObject(httpRequest, domainStr, this.getUser(httpRequest), dataObject);
        Instrumentation.sendEvent(contentObject, domainStr);
	}
	
}
