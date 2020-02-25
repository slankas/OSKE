package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.persist.collector.DomainDiscoveryDAO;
import edu.ncsu.las.storage.AccumuloStorage;
import edu.ncsu.las.storage.ElasticSearchDomainDiscoveryQuery;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.storage.FileStorage;
import net.jodah.expiringmap.ExpiringMap;


/**
 * 
 * 
 * 
 */
public class DomainDiscoverySession {
	private static Logger logger =Logger.getLogger(DomainDiscoverySession.class.getName());
	
	// stores user domain discovery sessions
	private static Map<UUID,DomainDiscoverySession> sessionMap = ExpiringMap.builder()
			  .expiration(30, TimeUnit.MINUTES)
			  .build();
	
	private UUID _sessionID;
	private String _sessionName;
	private String _userID;
	private Instant _creationDateTime;
	private Instant _lastActivityDateTime;
	private String _domainInstanceName;
	
	private java.util.List<DomainDiscoveryExecution> _executions;
	
	
	/**
	 * Create a new discovery session for the given domain and session name.  Stores the result into the database and 
	 * places a copy of the data into the session.
	 * 
	 * @param domainInstanceName
	 * @param sessionName
	 * @param userID
	 * @return
	 */
	public static DomainDiscoverySession createSession(String domainInstanceName, String sessionName, String userID) {
		DomainDiscoverySession dds = new DomainDiscoverySession();
		UUID sessionID = edu.ncsu.las.util.UUID.createTimeUUID();
		dds._sessionID = sessionID;
		dds._sessionName = sessionName;
		dds._userID = userID;
		dds._creationDateTime = Instant.now();
		dds._lastActivityDateTime = dds._creationDateTime;
		dds._domainInstanceName = domainInstanceName;
		
		if ( ((new DomainDiscoveryDAO()).insertSession(dds)) == false) {
			dds = null;
		}
		else {
			sessionMap.put(sessionID, dds);
		}
		
		return dds;
	}
	
	
	private DomainDiscoverySession() {
	}
		
	public DomainDiscoverySession(UUID sessionID, String domainInstanceName, String sessionName, String userID, Timestamp createdTimestamp, Timestamp lastActivityTimestamp) {
		super();
		this._sessionID = sessionID;
		this._sessionName = sessionName;
		this._userID = userID;
		this._creationDateTime = createdTimestamp.toInstant();
		this._lastActivityDateTime = lastActivityTimestamp != null ? lastActivityTimestamp.toInstant() : createdTimestamp.toInstant();
		this._domainInstanceName = domainInstanceName;
	}
	
	
	public String getUserID() {
		return _userID;
	}
	public void setUserID(String _userID) {
		this._userID = _userID;
	}
	
	public java.sql.Timestamp getCreationDateTimeAsTimeStamp() {
		return Timestamp.from(_creationDateTime);
	}	
	
	public java.sql.Timestamp getLastActivityDateTimeAsTimeStamp() {
		return Timestamp.from(_lastActivityDateTime);
	}	
	
	public UUID getSessionID() {
		return _sessionID;
	}

	public void setSessionID(UUID sessionID) {
		this._sessionID = sessionID;
	}
	
	public String getSessionName() {
		return _sessionName;
	}
	public void setSessionName(String sessionName) {
		this._sessionName = sessionName;
	}
	

	
	public String getDomainInstanceName() {
		return _domainInstanceName;
	}


	public void setDomainInstanceName(String domainInstanceName) {
		this._domainInstanceName = domainInstanceName;
	}


	public JSONObject toJSON() {
		JSONObject result = new JSONObject().put("sessionID", this.getSessionID())
                                            .put("userID", this.getUserID())
                                            .put("sessionName", this.getSessionName())
                                            .put("creationDateTime", java.time.format.DateTimeFormatter.ISO_INSTANT.format(_creationDateTime))
                                            .put("lastActivityDateTime", java.time.format.DateTimeFormatter.ISO_INSTANT.format(_lastActivityDateTime)); 
		return result;
	}
	
	
	public java.util.List<DomainDiscoveryExecution> getExecutions() {
		if (_executions == null) {
			_executions = DomainDiscoveryExecution.getAllExectuionsForSession(this.getSessionID());
		}
		return _executions;
	}
	
	public void addSessionExecution(DomainDiscoveryExecution dde) {
		_executions.add(dde);
	}
	
	public DomainDiscoveryExecution getExecution(int number) {
		DomainDiscoveryExecution result = null;
		for (DomainDiscoveryExecution dde: this.getExecutions()) {
			if (dde.getExecutionNumber() == number) {
				result = dde;
				break;
			}
		}
		return result;
		
	}
	
	private int getHighestExecutionNumber() {
		int max = 0;
		for (DomainDiscoveryExecution dde: this.getExecutions()) {
			max = Math.max(max, dde.getExecutionNumber());
		}
		return max;
	}
	
	/** 
	 * Retrieve the session object for the given ID.  The sessionID should be a unique value and hence, domainInstanceName is not used.  
	 * 
	 * @param sessionID
	 * @return
	 */
	public static DomainDiscoverySession findSession(UUID sessionID) {
		if (sessionMap.get(sessionID) == null) {
			DomainDiscoverySession dds = (new DomainDiscoveryDAO()).retrieveSession(sessionID);
			if (dds == null) { return null;}
			sessionMap.put(sessionID, dds);
		}
		return sessionMap.get(sessionID);
	}
	
	public static boolean destroySession(String domainInstanceName,UUID sessionID) {
		sessionMap.remove(sessionID);

		//Configuration domainConfig = Configuration.getDomainConfiguration(domainInstanceName);
		//String restURL = Configuration.getConfigurationProperty(domainInstanceName, FileStorageAreaType.SANDBOX, ConfigurationType.ELASTIC_STOREJSON_LOCATION);
		//		domainConfig.getDerivedConfiguration(ConfigurationType.ELASTIC_REST,FileStorageAreaType.SANDBOX);
		
		//TODO: delegate to a storage manager to delete files
		//TODO: look at how this will change when we merge domains
		FileStorage fs = new FileStorage();
		AccumuloStorage as = new AccumuloStorage();
		
		java.util.List<JSONObject> documents = ElasticSearchDomainDiscoveryQuery.getDocumentsForSessionAndExecution(domainInstanceName, sessionID.toString(),0);
		for (JSONObject document: documents) {
			String documentID = document.getString("source_uuid");
			JSONArray retreivalRecords = ElasticSearchDomainDiscoveryQuery.removeRetrievalFromDocument(domainInstanceName,documentID, sessionID.toString());
			if (retreivalRecords.length() == 0) {
				ElasticSearchREST.deleteDocument(domainInstanceName, FileStorageAreaType.SANDBOX, documentID);
			}
			fs.delete(domainInstanceName, FileStorageAreaType.SANDBOX, documentID);
			as.delete(domainInstanceName, FileStorageAreaType.SANDBOX, documentID);
			
		}
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds != null) { // we will still try to delete even if not found, so don't return.  if found, delete any indexes on the executions
			dds.getExecutions().stream().forEach(e -> { DiscoveryIndex.deleteIndex(e.getDocumentIndexID()); });
		}
		DiscoveryIndex.deleteIndex(sessionID);
		
		int numRows = (new DomainDiscoveryDAO()).deleteDiscoverySessionExecutions(sessionID);
		logger.log(Level.FINE, "Deleted "+numRows+" executions for sessionID: "+sessionID);
		numRows = (new DomainDiscoveryDAO()).deleteDiscoverySession(sessionID);
		logger.log(Level.FINE, "Deleted "+numRows+" session record(s) for sessionID: "+sessionID);
		
		return true;
	}
	
	
	public static java.util.List<DomainDiscoverySession> getAllSessionsForDomain(String domain) {
		return (new DomainDiscoveryDAO()).retrieveAllSessionsForDomain(domain);
	}

	public static java.util.List<DomainDiscoverySession> getAllSessionsByDomainAndName(String domain,String name) {
		return (new DomainDiscoveryDAO()).retrieveAllSessionsByDomainAndName(domain,name);
	}
	
	public DomainDiscoveryExecution createExecution(String searchTerms, String userID, int numberOfSearchResults, String searchAPI, JSONObject advConfig, String domainInstanceName, boolean shouldTranslate, String sourceLanguageDigraph, String searchTermsTranslate) {
		DomainDiscoveryExecution dde = new DomainDiscoveryExecution(this.getSessionID());
		dde.setExecutionNumber(this.getHighestExecutionNumber() + 1);
		dde.setSearchTerms(searchTerms);
		dde.setNumberOfSearchResults(numberOfSearchResults);
		dde.setUserID(userID);
		dde.setStartTime(Instant.now());
		dde.setSearchAPI(searchAPI);
		dde.setAdvancedConfiguration(advConfig);
		dde.setDomainInstanceName(domainInstanceName);
		dde.setDocumentIndexID(edu.ncsu.las.util.UUID.createTimeUUID());
		dde.setShouldTranslate(shouldTranslate);
		dde.setSourceLanguage(sourceLanguageDigraph);
		dde.setSearchTermsTranslated(searchTermsTranslate);

		boolean result = dde.insertSessionExecution(userID);
		if (result) {
			java.sql.Timestamp lastActivityTS = new java.sql.Timestamp(System.currentTimeMillis());
			this._lastActivityDateTime = lastActivityTS.toInstant();
			(new DomainDiscoveryDAO()).updateLatestActivityDate(this.getSessionID(), lastActivityTS);
			this.addSessionExecution(dde);
			return dde;
		} else {
			return null;
		}
	}
	
	/**
	 * updates the object and corresponding database record with a new session name
	 * 
	 * @param newName
	 * @return
	 */
	public boolean updateSessionName(String newName) {
		this.setSessionName(newName);
		return (new DomainDiscoveryDAO()).updateSessionName(this.getSessionID(), newName);
	}
	
}