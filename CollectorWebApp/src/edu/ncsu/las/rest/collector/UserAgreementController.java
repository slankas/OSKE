package edu.ncsu.las.rest.collector;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.UserAgreement;
import edu.ncsu.las.model.collector.UserAgreementText;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.util.DateUtilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Handles requests for UserAgreements
 * 
 */
@RequestMapping(value = "rest/{domain}/userAgreement")
@Controller
public class UserAgreementController extends AbstractRESTController {

    private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

    /**
     * Returns the latest UserAgreement form that a user needs to complete and sign
     *
     * @throws ValidationException
     * @throws IOException
     */
    @RequestMapping(value= "/text",method = RequestMethod.GET,headers = "Accept=application/json")
    public @ResponseBody
    String getLatestUserAgreementForm(HttpServletRequest request, @PathVariable("domain") String domainStr) throws ValidationException, IOException {
        logger.log(Level.FINER, "UserAgreementText controller: getting latest user agreement text/form ");
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserAgreementController.getLatestUserAgreementForm", new JSONObject(),System.currentTimeMillis(),null, request,domainStr);

        try {
        	UserAgreementText uat = UserAgreementText.getLatestUserAgreementForm();
        	JSONObject result = new JSONObject().put("status","success").put("form",uat.toJSON());
        	return result.toString();
        }
        catch (Exception e) {
        	logger.log(Level.SEVERE, "Unable to retrieve latest user agreement form: "+e.toString());
        	JSONObject result = new JSONObject().put("status", "error").put("message", "Unable to retrieve user agreement form");
        	return result.toString();
        }
    }

    /**
     * Returns a specific version of a UserAgreement form
     *
     * @throws ValidationException
     * @throws IOException
     */
    @RequestMapping(value= "/text/{versionNumber}",method = RequestMethod.GET,headers = "Accept=application/json")
    public @ResponseBody
    String getSpecificUserAgreementForm(HttpServletRequest request, @PathVariable("domain") String domainStr, @PathVariable("versionNumber") int versionNumber) throws ValidationException, IOException {
        logger.log(Level.FINER, "UserAgreementText controller: get specific user agreement text/form: "+versionNumber);
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserAgreementController.getSpecificUserAgreementForm", new JSONObject().put("versionNumbeR", versionNumber),System.currentTimeMillis(),null, request,domainStr);

        try {
        	UserAgreementText uat = UserAgreementText.getUserAgreementForm(versionNumber);
        	JSONObject result = new JSONObject().put("status","success").put("form",uat.toJSON());
        	return result.toString();
        }
        catch (Exception e) {
        	logger.log(Level.SEVERE, "Unable to retrieve latest user agreement form: "+e.toString());
        	JSONObject result = new JSONObject().put("status", "error").put("message", "Unable to retrieve user agreement form");
        	return result.toString();
        }
    }
    
    /**
     * Returns the most recent user agreement records for all users.
     *
     * @throws ValidationException
     * @throws IOException
     */
    @RequestMapping(value= "/user",method = RequestMethod.GET,headers = "Accept=application/json")
    public @ResponseBody String getLatestUserAgreementsForAllUsers(HttpServletRequest request,  @PathVariable("domain") String domainStr, @RequestParam(value="complete", required = false, defaultValue="false") boolean completeFlag) throws ValidationException, IOException {
        logger.log(Level.FINER, "UserAgreementText controller: getting latest user agreements of all users.");
        this.validateAuthorization(request, domainStr, RoleType.ADJUDICATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserAgreementController.getLatestUserAgreementsForAllUsers", new JSONObject().put("completeFlag", completeFlag),System.currentTimeMillis(),null, request,domainStr);
        
        org.json.JSONObject result = new org.json.JSONObject();
        JSONArray allUserAgreement = new JSONArray();
        
        List<UserAgreement> userAgreements;
        if (completeFlag) {
        	userAgreements = UserAgreement.getAllUserAgreements();
        }
        else {
        	userAgreements = UserAgreement.getLatestAgreementsForAllUsers();
        }
        
        for( UserAgreement ua: userAgreements) {
            allUserAgreement.put(ua.toJSON());
        }
        result.put("agreements",allUserAgreement);

        return result.toString();
    }
    
    
    /**
     * User retrieves their most recent user agreement. Can only be called by the current user to get his/her own record
     * Or they get their entire history with "complete=true" 
     * 
     * @throws ValidationException
     * @throws IOException
     */

    @RequestMapping(value = "/user/{emailid:[0-9a-zA-Z-_\\.@]+}", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
    public @ResponseBody String getMostRecentAgreementForUser(HttpServletRequest request, HttpServletResponse response, @PathVariable("domain") String domainStr,@PathVariable("emailid") String emailID,
    		                                                  @RequestParam(value="complete", required = false, defaultValue="false")  boolean complete) throws ValidationException, IOException {
        logger.log(Level.FINER, "UserAgreementText Controller: getting user's personal records");
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserAgreementController.getMostRecentAgreementForUser", new JSONObject(),System.currentTimeMillis(),null, request,domainStr);
        
        //don't need the authorization check - only care about the user ID for this check  this.validateAnyAuthorization(request);
        User u = this.getUser(request);

        if (u.getEmailID().equalsIgnoreCase(emailID) == false) {
        	logger.log(Level.WARNING, "passed user ID didn't match logged in user's id: ("+emailID+","+u.getEmailID()+")");
        	throw new ValidationException("Insufficient access", HttpServletResponse.SC_FORBIDDEN);
        }
        
        if (complete == false) {
	        UserAgreement ua = UserAgreement.getMostRecentUserAgreement(emailID);
	        if (ua != null) {
	        	JSONObject result = new JSONObject().put("status", "success").put("agreement", ua.toJSON());
	        	return result.toString();        	
	        }
	        else {
	        	JSONObject result = new JSONObject().put("status", "error").put("message", "no user agreement available");
	        	return result.toString();
	        }
        }
        else {
        	List<UserAgreement> agreements = UserAgreement.getAllUserAgreementsForUser(emailID);
            org.json.JSONObject result = new org.json.JSONObject();
            JSONArray allUserAgreement = new JSONArray();
            
            for( UserAgreement ua: agreements) {
                allUserAgreement.put(ua.toJSON());
            }
            result.put("agreements",allUserAgreement);
            result.put("status", "success");

            return result.toString();
        }
    }
    
    /**
     * User submits a new user agreement.
     * 
     * @throws ValidationException
     * @throws IOException
     */

    @RequestMapping(value = "/user/{emailid:.+}", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public @ResponseBody String userSignsUserAgreement(HttpServletRequest request, HttpServletResponse response, @RequestBody String data, @PathVariable("domain") String domainStr,@PathVariable("emailid") String emailID)	throws ValidationException, IOException {
        logger.log(Level.INFO, "UserAgreementText Controller: inserting user details in user_agreement table");

        User u = this.getUser(request);
        UserAgreementText uat = UserAgreementText.getLatestUserAgreementForm();  // actual form itself
        JSONObject userAgreementObject = new JSONObject(data);                   // what the user has uploaded
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserAgreementController.userSignsUserAgreement", userAgreementObject,System.currentTimeMillis(),null, request,domainStr);

        if (emailID.equals(u.getEmailID()) == false ) {
        	JSONObject result = new JSONObject().put("status", "error").put("message", "User ID not correct.");
        	return result.toString();
        }
        
        
        Instant now = ZonedDateTime.now(ZoneId.of("UTC")).toInstant();
        		
        String emailId = u.getEmailID();
        Timestamp agreementTimestamp  = new Timestamp(now.toEpochMilli());
        Timestamp statusTimestamp     = new Timestamp(now.toEpochMilli());
        Timestamp expirationTimestamp = new Timestamp(now.plus(365,ChronoUnit.DAYS).toEpochMilli());
        int agreementVersion = uat.getVersionNumber();
        String status = "review";

        String answers = userAgreementObject.getJSONObject("questions").toString();
        String userOrganization = userAgreementObject.getString("organization");
        String userSignature = userAgreementObject.getString("signatureName");
        
        
        UserAgreement ua = new UserAgreement(emailId,agreementTimestamp,agreementVersion,status,statusTimestamp,expirationTimestamp,answers,userOrganization,userSignature,"","","");
        ua.signAgreement();
        
        if (ua.submit()){
        	ua.notifyAdjudicatorsOfSubmission();
            JSONObject result = new JSONObject().put("status", "success");
            return result.toString();
        } else {
            JSONObject result = new JSONObject().put("status", "failed");
            return result.toString();
        }       
    }
    
    /**
     * performs the specified action for an adjudicator
     *
     * @throws ValidationException
     * @throws IOException
     */
    @RequestMapping(value= "/user/{emailid:.+}",method = RequestMethod.PUT,headers = "Accept=application/json")
    public @ResponseBody String performAdjudicatorAction(HttpServletRequest request, @PathVariable("domain") String domainStr, @RequestBody String data, @PathVariable("emailid") String emailID) throws ValidationException, IOException {
        logger.log(Level.FINER, "UserAgreementText controller: adjudicate user agreement for user.");
        this.validateAuthorization(request, domainStr, RoleType.ADJUDICATOR);
        
        User u = this.getUser(request);
        JSONObject dataObject = new JSONObject(data);
        Timestamp agreementTimestamp =  Timestamp.from(DateUtilities.getFromString(dataObject.getString("agreementTimestamp")).toInstant());
        UserAgreement ua = UserAgreement.retrieve(emailID, agreementTimestamp);
        if (ua == null) {
        	JSONObject result = new JSONObject().put("status", "error").put("message", "Unable to locate user agreement record");
        	return result.toString();           
        }      	        
        
        String action = dataObject.getString("action");
		this.instrumentAPI("edu.ncsu.las.rest.collector.UserAgreementController.performAdjudicatorAction", new JSONObject().put("userID", emailID).put("adjudicatorAction", action),System.currentTimeMillis(),null, request,domainStr);
                
        if (action.equals("deny")) {
            String adjudicatorComments = dataObject.getString("adjudicatorComments");

            ua.deny(u.getEmailID(),adjudicatorComments);
            
            JSONObject result = new JSONObject().put("status", "success").put("message", "deny success");
            return result.toString();                	
        }
        else if (action.equals("rework")) {
            String adjudicatorComments = dataObject.getString("adjudicatorComments");

            ua.rework(u.getEmailID(),adjudicatorComments);
            
            JSONObject result = new JSONObject().put("status", "success").put("message", "rework success");
            return result.toString();            	
        }
        else if (action.equals("revoke")) {
            String adjudicatorComments = dataObject.getString("adjudicatorComments");

            ua.revoke(u.getEmailID(),adjudicatorComments);
            
            JSONObject result = new JSONObject().put("status", "success").put("message", "revoke success");
            return result.toString();               	
        }
        else if (action.equals("changeExpiration")) {
        	long milliseconds = dataObject.getLong("expirationTimestamp");

            Timestamp newExpirationTimestamp = new Timestamp(milliseconds);
            Timestamp maxExpirationTimestamp = Timestamp.from(ua.getAgreementTimestamp().toInstant().plus(365, ChronoUnit.DAYS));
            if (newExpirationTimestamp.after(maxExpirationTimestamp)) {
                JSONObject result = new JSONObject().put("status", "failed").put("message", "The new expiration date cannot be more than a year out from the signature date: " + DateUtilities.getDateTimeISODateTimeFormat(ua.getAgreementTimestamp().toInstant()));
                return result.toString();               	
            }
  
            ua.changeExpirationTime(u.getEmailID(), newExpirationTimestamp);
            JSONObject result = new JSONObject().put("status", "success").put("message", "revoke success");
            return result.toString();               	
        }
        else if (action.equals("approve")) {
        	ua.markApproved(u.getEmailID());
            JSONObject result = new JSONObject().put("status", "success").put("message", "revoke success");
            return result.toString();               	

        }
        else {
            JSONObject result = new JSONObject().put("status", "failed").put("message", "invalid action");
            return result.toString();                	
        }
    }    
    
}
