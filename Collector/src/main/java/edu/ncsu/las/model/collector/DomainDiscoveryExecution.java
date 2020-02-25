package edu.ncsu.las.model.collector;

import java.time.Instant;
import java.util.UUID;

import org.json.JSONObject;

import edu.ncsu.las.persist.collector.DomainDiscoveryDAO;


/**
 * Represents a query / exectuion made within the context of a domain discovery session.  e.g, this is a users query, what API was used, and other settings.
 * 
 */
public class DomainDiscoveryExecution {
	private UUID _sessionId;
	private int  _executionNumber ;
	private String _searchTerms;
	private String _userID;
	
	private int _numberOfSearchResults;
	
	private String _searchAPI;

	private JSONObject _advancedConfiguration;  //based onto search providers for any custom options that may exist.  (e.g., see google)
	
	private Instant _startTime;
	private Instant _endTime;
	
	private String _domainInstanceName;
	
	private UUID _documentIndexID;
	
	private String _sourceLanguage;
	private boolean _shouldTranslate;
	private String _searchTermsTranslated;
	/*
	public DomainDiscoveryExecution() {
		
	}
	*/
	
	public DomainDiscoveryExecution(UUID sessionId, String domainInstanceName, int executionNumber, String searchTerms, String userID, int numberOfSearchResults,
			String searchAPI, JSONObject advancedConfiguration, Instant startTime, Instant endTime, UUID documentIndexID, 
			boolean shouldTranslate, String targetLanguage, String searchTermsTranslated) {
		super();
		_sessionId = sessionId;
		_domainInstanceName = domainInstanceName;
		_executionNumber = executionNumber;
		_searchTerms = searchTerms;
		_numberOfSearchResults = numberOfSearchResults;
		_searchAPI = searchAPI;
		_advancedConfiguration = advancedConfiguration;
		
		_startTime = startTime;
		_endTime   = endTime;
		
		_documentIndexID = documentIndexID;
		
		_shouldTranslate = shouldTranslate;
		_sourceLanguage = targetLanguage;
		_searchTermsTranslated = searchTermsTranslated;
	}
	
	public DomainDiscoveryExecution(UUID _sessionID) {
		super();
		this._sessionId = _sessionID;

	}

	public UUID getSessionID() {
		return _sessionId;
	}
	public void setSessionID(UUID sessionId) {
		this._sessionId = sessionId;
	}
	
	public String getSearchTerms() {
		return _searchTerms;
	}
	public void setSearchTerms(String searchTerms) {
		this._searchTerms = searchTerms;
	}
	
	public String getSearchTermsTranslated() {
		return _searchTermsTranslated;
	}
	public void setSearchTermsTranslated(String searchTermsTranslated) {
		this._searchTermsTranslated = searchTermsTranslated;
	}
	
	public String getSourceLanguage() {
		return _sourceLanguage;
	}
	public void setSourceLanguage(String sourceLanguageDigraph) {
		this._sourceLanguage = sourceLanguageDigraph;
	}
	
	public boolean shouldTranslate() {
		return _shouldTranslate;
	}
	public void setShouldTranslate(boolean shouldTranslate) {
		this._shouldTranslate = shouldTranslate;
	}
	
	public String getUserID() {
		return _userID;
	}
	public void setUserID(String _userID) {
		this._userID = _userID;
	}
	public int getNumberOfSearchResults() {
		return _numberOfSearchResults;
	}
	public void setNumberOfSearchResults(int numberOfSearchResults) {
		this._numberOfSearchResults = numberOfSearchResults;
	}
	
	public int getExecutionNumber() {
		return _executionNumber;
	}
	public void setExecutionNumber(int _executionNumber) {
		this._executionNumber = _executionNumber;
	}
	
	public String getSearchAPI() {
		return _searchAPI;
	}

	public void setSearchAPI(String searchAPI) {
		_searchAPI = searchAPI;
	}

	public Instant getStartTime() {
		return _startTime;
	}
	public void setStartTime(Instant startTime) {
		this._startTime = startTime;
	}
	public Instant getEndTime() {
		return _endTime;
	}
	
	private void setEndTime(Instant endTime) {
		this._endTime = endTime;
	}
	
	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		_domainInstanceName = domainInstanceName;
	}
	
	public UUID getDocumentIndexID() {
		return _documentIndexID;
	}
	
	public void setDocumentIndexID(UUID docIndex) {
		this._documentIndexID = docIndex;
	}	

	public boolean insertSessionExecution(String userID){
		return (new DomainDiscoveryDAO()).insertSessionExecution(this, userID);
	}
	
	public boolean markComplete(Instant endTime) {
		this.setEndTime(endTime);
		return (new DomainDiscoveryDAO()).updateExecutionEndTime(this);		
	}
	
	public static java.util.List<DomainDiscoveryExecution> getAllExectuionsForSession(UUID sessionID) {
		return (new DomainDiscoveryDAO()).retrieveAllExecutionsForSession(sessionID);
	} 
	
	public static DomainDiscoveryExecution loadExectuion(UUID sessionID, int executionNumber) {
		return (new DomainDiscoveryDAO()).retrieveExecution(sessionID,executionNumber);
	} 	
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject().put("executionNumber", _executionNumber)
										    .put("searchTerms", _searchTerms)
										    .put("userID",      _userID)
										    .put("numSearchResults", _numberOfSearchResults)
										    .put("searchAPI", _searchAPI)
										    .put("documentIndexID", _documentIndexID.toString())
										    .put("advancedConfiguration", _advancedConfiguration)
										    .put("targetLanguage", _sourceLanguage)
										    .put("translateCheck", _shouldTranslate)
										    .put("searchTermsTranslated", _searchTermsTranslated);
		if (_startTime != null) {
			result.put("startTime", java.time.format.DateTimeFormatter.ISO_INSTANT.format(_startTime));
		}
		else {
			result.put("startTime", "");
		}
		if (_endTime != null) {
			result.put("endTime", java.time.format.DateTimeFormatter.ISO_INSTANT.format(_endTime));
		}
		else {
			result.put("endTime", "");
		}

		return result;
		
	}

	public JSONObject getAdvancedConfiguration() {
		return _advancedConfiguration;
	}

	public void setAdvancedConfiguration(JSONObject value) {
		_advancedConfiguration = value;
	}
	
}