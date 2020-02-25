package edu.ncsu.las.rest.collector;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.SearchAlert;
import edu.ncsu.las.model.collector.SearchAlertNotification;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.util.CronExpression;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles request for "SearchAlerts".  These "alerts" are basically new just new search results.
 * e.g., we continually run the same search and any new result that hasn't previously appeared is an "alert".
 * 
 * 
 * originally Created on 8/16/17.
 * 
 */
@RequestMapping(value = "rest/{domain}/searchAlert")
@Controller
public class SearchAlertController extends AbstractRESTController {

    private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

    /**
     * Updates alert table when user creates a new alert.
     *
     * @throws ValidationException
     * @throws IOException
     *
     */
    @RequestMapping(value = "", method = RequestMethod.POST, headers = "Accept=application/json")
    public @ResponseBody String createNewAlert(HttpServletRequest request, @PathVariable("domain") String domainStr, @RequestBody String data) throws ValidationException, IOException {
        logger.log(Level.FINEST, "AlertController : Inserting user details in alert table");
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        
        User u = this.getUser(request);

        JSONObject alertObject = new JSONObject(data);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.createNewAlert", alertObject, System.currentTimeMillis(), null, request,domainStr);


        UUID alertId = edu.ncsu.las.util.UUID.createTimeUUID();
        String ownerEmailId = u.getEmailID();
        Timestamp dateCreated = new Timestamp(System.currentTimeMillis());
        Timestamp dateLastRun = new Timestamp(System.currentTimeMillis());
        String alertName     = alertObject.getString("alertName");
        String searchTerm    = alertObject.getString("searchTerm");
        String cronSchedule  = alertObject.getString("cronSchedule");
        String sourceHandler = alertObject.getString("sourceHandler");
        int numberOfResult   = alertObject.getInt("numberOfSearchResults");
        boolean preAcknowlege= alertObject.getBoolean("preacknowledge");
        
        String state = "waiting";
        String currentCollector = "";
        int numTimesRun = 0;

        //TODO: add validation
        
        Timestamp cronNextRun;

        try {
            CronExpression ce = new CronExpression(cronSchedule);
            ce.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
            java.util.Date d = ce.getNextValidTimeAfter(new java.util.Date());
            cronNextRun = Timestamp.from(Instant.ofEpochMilli(d.getTime()));
        } catch (ParseException pe) {
            throw new ValidationException("Invalid schedule: " + cronSchedule + ", " + pe.toString());
        }

        SearchAlert newAlert = new SearchAlert(alertId, alertName, sourceHandler, searchTerm, numberOfResult, ownerEmailId, cronSchedule, cronNextRun, dateCreated, dateLastRun, state, currentCollector, numTimesRun, domainStr);

        if (newAlert.insert()) {
        	
        	newAlert.run();  // by running the results first, only new results appear as "searchAlerts".  TODO check results for -1 and -2 (invalid source handler / no results)
        	if (preAcknowlege) {newAlert.acknowledgeAllAlerts(); }
        	
            JSONObject result = new JSONObject().put("status", "success").put("alertID", alertId.toString());
            return result.toString();
        } else {
            JSONObject result = new JSONObject().put("status", "failed");
            return result.toString();
        }

    }

    
    @RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody String getUserAlerts(HttpServletRequest request, HttpServletResponse response, @PathVariable("domain") String domainStr,
    		                                 @RequestParam(value = "showAll", required = false, defaultValue="false") String showAll) throws ValidationException, IOException {
        logger.log(Level.FINER, "AlertController: get alerts");
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.getUserAlerts", new JSONObject().put("showAll", showAll), System.currentTimeMillis(), null, request,domainStr);

        User u = this.getUser(request);
        
        boolean showAllFlag = false;
        if (showAll.equalsIgnoreCase("true")) {
        	this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR);
        	showAllFlag = true;
        }
        
        org.json.JSONObject result = new org.json.JSONObject();
        JSONArray alerts = new JSONArray();

        for ( SearchAlert a:  SearchAlert.retrieveAlerts(domainStr, u.getEmailID(), showAllFlag)) {
            alerts.put(a.toJSON());
        }

        result.put("searchAlerts",alerts);

        return result.toString();
    }
    
    
    
    @RequestMapping(value = "/notifications", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody String getAlertNotifications(HttpServletRequest request, @PathVariable("domain") String domainStr,
			                                          @RequestParam(value = "showAll", required = false, defaultValue="false") String showAll) throws ValidationException, IOException {
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.getAlertNotifications", new JSONObject().put("showAll", showAll), System.currentTimeMillis(), null, request,domainStr);

        User u = this.getUser(request);

        boolean showAllFlag = false;
        if (showAll.equalsIgnoreCase("true")) {
        	this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR);
        	showAllFlag = true;
        }
        
        logger.log(Level.INFO, "AlertController : Getting Alert Notifications for user.");

        org.json.JSONObject result = new org.json.JSONObject();
        JSONArray allAlertNotification = new JSONArray();
        
        List<SearchAlertNotification> notifications= SearchAlert.getAllAlertNotifications(domainStr, u.getEmailID(), showAllFlag); 
        for( SearchAlertNotification an: notifications) {
            allAlertNotification.put(an.toJSON());
        }

        result.put("notifications",allAlertNotification);

        return result.toString();
    }
    
    /**
     * 
     * @param request
     * @param response
     * @param data
     * @param alertID
     * @param url
     * @return
     * @throws ValidationException
     * @throws IOException
     */
    @RequestMapping(value = "/{alertID}", method = RequestMethod.POST, headers = "Accept=application/json")
    public @ResponseBody String performAlertAction(HttpServletRequest request, @RequestBody String data,  
    		                     @PathVariable("domain") String domainStr,
    		                     @PathVariable("alertID") UUID alertID) throws ValidationException, IOException {
        logger.log(Level.INFO, "AlertController : Updating alert notification acknowledgement to true.");
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.markAlertRead", new JSONObject().put("alertID", alertID.toString()), System.currentTimeMillis(), null, request,domainStr);
       
        User u = this.getUser(request);
        
        JSONObject result = new JSONObject();
        
        SearchAlert sa = SearchAlert.retrieveAlert(alertID);
        if (sa == null) {
        	result.put("status", "failed")
        	      .put("message", "alert not found")
        	      .put("alertID", alertID.toString());
        	return result.toString();
        }
        //to perform an action, we need to own the alert or be an administrator
        if (sa.getOwnerEmailId().equals(u.getEmailID()) == false) {
        	this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR);
        }
        //System.out.println(data);
        JSONObject requestObject = new JSONObject(data);
        String action = requestObject.getString("action");
       
        
        if (action.equalsIgnoreCase("acknowledge")) {
        	 String url    = requestObject.optString("url");
        	if (SearchAlertNotification.acknowledge(alertID,url)) {
            	result.put("status", "success")
            	      .put("message", "notification acknowledged")
            	      .put("url", url)
            	      .put("alertID", alertID.toString());
            	return result.toString();
        	}
        	else {
            	result.put("status", "failed")
            	      .put("message", "Unable to acknowledge notification ")
            	      .put("url", url)
            	      .put("alertID", alertID.toString());
            	return result.toString();
        	}        	
        	
        }
        else if (action.equalsIgnoreCase("changeState")) {
        	String newState = requestObject.optString("state","invalid").toLowerCase();
        	
        	if (newState.equals(SearchAlert.STATE_PAUSED) == false &&  newState.equals(SearchAlert.STATE_WAITING) == false ) {
               	result.put("status", "failed")
               	      .put("message", "invalid state - "+newState)
               	      .put("alertID", alertID.toString());
               	return result.toString();       		
        	}
        	
        	if (sa.changeAlertState(newState)) {
        		result.put("status", "success")
        			  .put("message", "state changed")
        			  .put("state", newState)
        			  .put("alertID", alertID.toString());
        		return result.toString();
        	}
        	else {
        		result.put("status", "failed")
		  			  .put("message", "unable to change state")
		  			  .put("state", newState)
		  			  .put("alertID", alertID.toString());
        		return result.toString();
        	}
        	
       	
       }
        else {
        	result.put("status", "failed")
        	      .put("message", "No such action available: "+action)
        	      .put("validActions", "acknowledge")
  	              .put("alertID", alertID.toString());
        	return result.toString();
        }
        
        
    }
    
    /**
     * 
     * @param request
     * @param response
     * @param alertID
     * @return
     * @throws ValidationException
     * @throws IOException
     */
    @RequestMapping(value = "/{alertID}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public @ResponseBody
    String deleteAlert(HttpServletRequest request, HttpServletResponse response, 
    		                     @PathVariable("domain") String domainStr,
    		                     @PathVariable("alertID") UUID alertID) throws ValidationException, IOException {
        logger.log(Level.INFO, "AlertController : delete alert:" +alertID);
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        User u = this.getUser(request);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.deleteAlert", new JSONObject().put("alertID", alertID.toString()), System.currentTimeMillis(), null, request,domainStr);
       
        JSONObject result = new JSONObject();
        
        SearchAlert sa = SearchAlert.retrieveAlert(alertID);
        if (sa == null) {
        	result.put("status", "failed")
        	      .put("message", "alert not found")
        	      .put("alertID", alertID.toString());
        	return result.toString();
        }
        
        //to perform an action, we need to own the alert or be an administrator
        if (sa.getOwnerEmailId().equals(u.getEmailID()) == false) {
        	this.validateAuthorization(request, domainStr, RoleType.ADMINISTRATOR);
        }
        
       	if (sa.deleteAlert()) {
        	result.put("status", "success")
        	      .put("message", "alert deleted")
        	      .put("alertID", alertID.toString());
        	return result.toString();
       	}
       	else {
           	result.put("status", "failed")
           	      .put("message", "Unable to acknowledge notification ")
           	      .put("alertID", alertID.toString());
           	return result.toString();
       	}        	
    }
    
    @RequestMapping(value = "/{alertID}", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody String retrieveAlertByID(HttpServletRequest request, HttpServletResponse response, @PathVariable("domain") String domainStr,
    		 @PathVariable("alertID") UUID alertID) throws ValidationException, IOException {
        logger.log(Level.FINER, "AlertController: get alerts");
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.retrieveAlert", new JSONObject().put("alertID", alertID.toString()), System.currentTimeMillis(), null, request,domainStr);
       
        SearchAlert sa = SearchAlert.retrieveAlert(alertID);
        
        if (sa == null) {
        	return "{ status: \"failed\"}";
        }
        
        return sa.toJSON().toString();
    }   
    
    @RequestMapping(value = "/{alertID}/notifications", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody String getNotificationsForAnAlert(HttpServletRequest request, @PathVariable("domain") String domainStr,
    		                                               @PathVariable("alertID") UUID alertID,
			                                               @RequestParam(value = "showAll", required = false, defaultValue="false") String showAll) throws ValidationException, IOException {
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.SearchAlertController.getNotificationsForAnAlert", new JSONObject().put("alertID", alertID.toString()), System.currentTimeMillis(), null, request,domainStr);

        boolean showAllFlag = false;
        if (showAll.equalsIgnoreCase("true")) {
        	showAllFlag = true;
        }
        
        logger.log(Level.INFO, "AlertController : Getting notifications for an alert");

        org.json.JSONObject result = new org.json.JSONObject();
        JSONArray allAlertNotification = new JSONArray();
        
        SearchAlert sa = SearchAlert.retrieveAlert(alertID);
        if (sa == null) {
        	return "{ status: \"failed\"}";
        }
        
        List<SearchAlertNotification> notifications = sa.getAllAlertNotifications(showAllFlag);
        for( SearchAlertNotification an: notifications) {
            allAlertNotification.put(an.toJSON());
        }

        result.put("notifications",allAlertNotification);
        result.put("alertID",alertID.toString());
        result.put("status", "success");
        
        return result.toString();
    }
  
}



