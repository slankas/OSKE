package edu.ncsu.las.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import edu.ncsu.las.controller.AbstractController;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.servlet.SystemInitServlet;

/**
 * Handles instrumentation events from users.
 * 
 * Event objects are expected to be in this form:
 * 
 *  evtDesc
 *  evtTime   new Date().getTime();  Should be the number of milliseconds since Unix Epoch
 *  evtInfo { // specific to the event.  Can include eventStartTime and eventEndTime }
 *      
		
		var domain = $('#domain').val();
		
		if( !(domain && domain !== "null" )) {
			domain="system";
		}
 * 
 */
@Controller
@RequestMapping("rest/{domain}/instrumentation")
public class InstrumentationController extends AbstractController {

	private static final Logger logger =  Logger.getLogger(InstrumentationController.class.getName());
	
	@RequestMapping(value = "/event", method = RequestMethod.POST, headers="Accept=application/json")
	@ResponseBody
	public String event(@RequestParam Map<String, String> params, HttpServletRequest request,@PathVariable("domain") String domainStr) {
		
		if (!Configuration.getConfigurationPropertyAsBoolean(domainStr, ConfigurationType.INSTRUMENTATION_SEND_EVENTS)) {
			logger.log(Level.INFO, "Ignoring instrumentation event: "+request.getParameter("data"));
			return "{ \"status\" : \"instrumentation ignored\" }";
		}
		
		User u = edu.ncsu.las.webapp.Authentication.getUser(request);
		if (u == null) {
			return new JSONObject().put("status", "failure").put("message", "user not defined").toString();
		}
		
		JSONObject requestData = new JSONObject(request.getParameter("data"));
		JSONObject contentObject = createEventObject(request,  domainStr, u, requestData);
		
		logger.log(Level.FINEST,"instrumentation content: "+contentObject.toString());
		
    	//Send to skylr
		return Instrumentation.sendEvent(contentObject, domainStr);
	}

	public static JSONObject createRequestDataObject(String eventDescription, long startTime, Long endTime, JSONObject eventSpecificData) {
		JSONObject evtInfo = new JSONObject(eventSpecificData.toString()).put("eventStartTime", startTime);
		if (endTime != null) {
			evtInfo.put("eventEndTime", endTime.longValue());
		}
		JSONObject result = new JSONObject().put("evtDesc", eventDescription)
				                            .put("evtInfo", evtInfo);
		return result;
	}
	//TODO: will need to use this pattern in the other rest APIs
	//JSONObject InstrumentationController.createEventObject(HttpServletRequest request,String domainStr, User u, 
	//                                              InstrumentationController.createRequestDataObject(String eventDescription, long startTime, Long endTime, JSONObject eventSpecificData));
	// Instrumentation.sendEvent(contentObject, domainStr);
	public static JSONObject createEventObject(HttpServletRequest request,String domainStr, User u, JSONObject requestData) {
		return createEventObject(request, domainStr, u.getUserID(),requestData);
	}

	public static JSONObject createEventObject(HttpServletRequest request,String domainStr, String userID, JSONObject requestData) {
		String build = SystemInitServlet.getWebApplicationBuildTimestamp(request.getSession().getServletContext());
		String remoteAddress =  AbstractController.getRemoteAddress(request);
		String sessionID = AbstractController.getInternalUniqueSessionID(request);
		String userAgent = request.getHeader("user-agent");
		return createEventObject(userAgent, domainStr, userID, sessionID, requestData, remoteAddress, build);
	}
	
	
	public static JSONObject createEventObject(String userAgent, String domainStr, String userID, String sessionID, JSONObject requestData, String remoteAddress, String build) {
		JSONObject eventObject = new JSONObject();
		// Core Event Header fields
		eventObject.put("UserId", userID);
		eventObject.put("SysId", userAgent);
		eventObject.put("ProjId",  Configuration.getConfigurationProperty(domainStr, ConfigurationType.INSTRUMENTATION_PROJECTID));
		eventObject.put("AppName", domainStr);
		eventObject.put("ProjVer", build);
		eventObject.put("SessnId", sessionID);
		eventObject.put("ProtocolVer", "0.3");
		//eventObject.put("ChildId","");  // array of ids
		
		// Event Content - When
		eventObject.put("EvtTime", System.currentTimeMillis());
		// need to allow a client time if it exists as the evttime object would almost always occur after the end time, even though we want it to be the start
		if (requestData.has("evtInfo") && requestData.getJSONObject("evtInfo").has("eventStartTime")) {
			eventObject.put("EvtTime", requestData.getJSONObject("evtInfo").get("eventStartTime"));
		}
		if (requestData.has("evtInfo") && requestData.getJSONObject("evtInfo").has("eventEndTime")) {
			eventObject.put("EvtEndTime", requestData.getJSONObject("evtInfo").get("eventEndTime"));
		}
		
		// Event Content - Where
		eventObject.put("NetAddr", remoteAddress);
		//eventObject.put("GeoLoc","");
		
		// Event Content - What
		eventObject.put("EvtType", "GUI");
		eventObject.put("EvtDesc", requestData.get("evtDesc"));
		if (requestData.has("evtInfo")) {
			eventObject.put("AppData", requestData.get("evtInfo"));		
		}
		
		JSONObject contentObject = new JSONObject();
		contentObject.put("content", eventObject);
		return contentObject;
	}	
}
