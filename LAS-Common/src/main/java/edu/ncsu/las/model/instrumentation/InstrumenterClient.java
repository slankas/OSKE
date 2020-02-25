package edu.ncsu.las.model.instrumentation;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;




public class InstrumenterClient {
	private static Logger logger = Logger.getLogger(InstrumenterClient.class.getName());
	
	private String _applicationName;
	private String _projectID;
	private String _projectVersion;
	private String _systemID;
	private String _networkAddress; 
	
	private String _instrumentationURI;
	private String _instrumentationToken;
	
	
	public InstrumenterClient(String instrumentationURI, String instrumentationToken, String applicationName, String projectID, String projectVersion) {
		_applicationName   = applicationName;
		_projectID         = projectID;
		_projectVersion = projectVersion;
		_systemID          = System.getProperty("os.name", "OS") + ", " + 	System.getProperty("os.version", "OS version") +", " +
				             System.getProperty("os.arch", "arch") + " - " + System.getProperty("java.vm.name", "java vm name") +
				             ": (" + System.getProperty("java.vm.vendor", "java vendor") + ") - " + System.getProperty("java.runtime.version", "java version");
		try {
			_networkAddress = edu.ncsu.las.util.Network.getLocalHostLANAddress().getHostAddress();
		}
		catch (Exception e) {
			System.out.println("Unable to determine local IP: "+e.toString());
			_networkAddress = "127.0.0.1";
		}
		
		_instrumentationURI = instrumentationURI;
		_instrumentationToken = instrumentationToken;
	}
	

	
	private JSONObject createBaseEventObject(String userID, String sessionID, String eventType, String eventDescription, String appData) {
		JSONObject eventObject = new JSONObject();
		// Core Event Header fields
		eventObject.put("UserId", userID);
		eventObject.put("SysId", _systemID);
		eventObject.put("ProjId",_projectID);
		eventObject.put("AppName",_applicationName);
		eventObject.put("ProjVer",_projectVersion);
		eventObject.put("SessnId", sessionID);
		eventObject.put("ProtocolVer", "0.3");
		eventObject.put("NetAddr", _networkAddress);
		
		eventObject.put("EvtType", eventType);
		eventObject.put("EvtDesc", eventDescription);
		if (appData != null) {
			eventObject.put("AppData", appData);		
		}
		
		return eventObject;
	}
	
	private void sendEventToInstrumentationService(JSONObject event) {
		EventSender es = new EventSender(event);
		(new Thread(es)).start();
	}		
	
	
	public void sendEvent(String userID, String sessionID, String eventType, String eventDescription, String appData, long eventTime, long eventEndTime) {
		JSONObject eventObject = this.createBaseEventObject(userID, sessionID, eventType, eventDescription, appData);
		
		// Event Content - When
		eventObject.put("EvtTime", eventTime);
		eventObject.put("EvtEndTime", eventEndTime);
		
		this.sendEventToInstrumentationService(eventObject);
	}
	
	public void sendEvent(String userID, String sessionID, String eventType, String eventDescription, String appData) {
		JSONObject eventObject = this.createBaseEventObject(userID, sessionID, eventType, eventDescription, appData);
		eventObject.put("EvtTime", System.currentTimeMillis());
		
		this.sendEventToInstrumentationService(eventObject);
	}
	
	
	class EventSender implements Runnable {

		JSONObject _eventObject;
		
		EventSender(JSONObject event) {
			_eventObject = event;
		}
		
		@Override
		public void run() {
			JSONObject contentObject = new JSONObject();
			contentObject.put("content", _eventObject);
			
			logger.log(Level.FINER,"Event Object being sent: "+contentObject.toString());
			
			

			try {
				HttpResponse<JsonNode> jsonResponse = Unirest.post(_instrumentationURI)
						  .header("accept", "application/json")
						  .header("AuthToken", _instrumentationToken)
						  .header("Content-Type", "application/json")
						  .body(contentObject)
						  .asJson();
				logger.info("Instrumentation result: "+ jsonResponse.getBody().getObject());
			} catch (UnirestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			

		}
		
	}
		
}
