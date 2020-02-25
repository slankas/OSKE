package edu.ncsu.las.model.collector;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.storage.ElasticSearchREST;

public class Instrumentation {
	private static final Logger logger =  Logger.getLogger(Instrumentation.class.getName());
	
	public static void initializeInstrumentation() {
		if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.INSTRUMENTATION_USE_ELASTICSEARCH)) {
			String index = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.INSTRUMENTATION_ELASTIC_INDEX);
			JSONObject settings = Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.INSTRUMENTATION_ELASTIC_SETTINGS);
			JSONObject mappings = Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM, ConfigurationType.INSTRUMENTATION_ELASTIC_MAPPINGS);
			
			if (ElasticSearchREST.indexExists(index) == false) {
				ElasticSearchREST.createIndex(index, settings, mappings);
			}
			ElasticSearchREST.ensureIndexWritable(index);
		}
	}	
	public static String createAndSendEvent(String domain, String eventName, Long eventStartTime, Long eventEndTime, JSONObject dataObject, UUID jobHistoryID) {
		if (dataObject == null) { dataObject = new JSONObject(); }
		return createAndSendEvent(domain, eventName, "appData", eventStartTime, eventEndTime, dataObject,  jobHistoryID);
	}
	
	
	public static String createAndSendEvent(String domain, String eventName, String dataFieldName, Long eventStartTime, Long eventEndTime, JSONObject dataObject, UUID jobHistoryID) {
		if (!Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.INSTRUMENTATION_SEND_EVENTS)) {
			logger.log(Level.INFO, "Ignoring instrumentation event: "+dataObject);
			return "{ \"status\" : \"instrumentation ignored\" }";
		}
		
		JSONObject eventObject = new JSONObject();
		// Core Event Header fields
		eventObject.put("UserId", "collector");
		eventObject.put("SysId",  Configuration.getConfigurationProperty(domain, ConfigurationType.COLLECTOR_ID)); 
		
		eventObject.put("ProjId",  Configuration.getConfigurationProperty(domain, ConfigurationType.INSTRUMENTATION_PROJECTID));
		eventObject.put("AppName", domain +": Collector");
		eventObject.put("ProjVer", Configuration.getCollectorDaemonBuildTimestamp());
		if (jobHistoryID != null) {
			eventObject.put("SessnId", jobHistoryID.toString());
		}
		else {
			eventObject.put("SessnId", "n/a");
		}
		eventObject.put("ProtocolVer", "0.3");
			
		// Event Content - When
		eventObject.put("EvtTime", System.currentTimeMillis());
		
		if (eventStartTime != null) { eventObject.put("EvtTime",    eventStartTime.longValue());  }
		if (eventEndTime   != null) { eventObject.put("EvtEndTime", eventEndTime.longValue());    } 
			
		// Event Content - Where
		eventObject.put("NetAddr", "");
		//eventObject.put("GeoLoc","");
			
		// Event Content - What
		eventObject.put("EvtType", "daemon");
		eventObject.put("EvtDesc", eventName);
		
		
		if (dataObject != null) {
			eventObject.put(dataFieldName, dataObject);		
		}
			
		JSONObject contentObject = new JSONObject();
		contentObject.put("content", eventObject);
		logger.log(Level.INFO,"instrumentation content: "+contentObject.toString());
				
		return sendEvent(contentObject,domain);
	}
	
	
	
	
	public static String sendEvent(JSONObject eventObject, String domain) {
		if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.INSTRUMENTATION_USE_ELASTICSEARCH)) { // send events to everything to ES
			JSONObject dataObject  = eventObject.getJSONObject("content");
			eventObject.put("data", dataObject);
			eventObject.remove("content");
			eventObject.put("time", System.currentTimeMillis());
			
			ElasticSearchREST.insertDocument(domain, Configuration.getConfigurationProperty(domain, ConfigurationType.INSTRUMENTATION_ELASTIC_INDEX), eventObject);
		}
		
    	//Send to skylr
		if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.INSTRUMENTATION_USE_SKYLR)) {
			String uri = Configuration.getConfigurationProperty(domain, ConfigurationType.INSTRUMENTATION_API);
			try {		
				HttpResponse<JsonNode> jsonResponse = Unirest.post(uri)
						                              .header("accept", "application/json")
						                              .header("Content-Type", "application/json")
						                              .header("AuthToken", Configuration.getConfigurationProperty(domain, ConfigurationType.INSTRUMENTATION_TOKEN))
						                              .body(eventObject).asJson();
				JSONObject result = jsonResponse.getBody().getObject();	
				return result.toString();
			}
			catch (UnirestException ue) {
				logger.log(Level.WARNING, "Unable to send to instrumentation: ",ue);
				return "{ \"status\" : \"failure\" }";
			}
		}
		return "{ \"status\" : \"success\" }";
	}	
}
