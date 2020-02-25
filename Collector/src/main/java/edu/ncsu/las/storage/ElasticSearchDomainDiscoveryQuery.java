package edu.ncsu.las.storage;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.FileStorageAreaType;

/**
 * Class contains methods to update the DomainDiscovery portion of documents
 * within ElasticSearch.  (Note: the primary insert of these records is performed as past of the standard processing of a document)
 * 
 *
 */
public class ElasticSearchDomainDiscoveryQuery {
	private static Logger logger =Logger.getLogger(ElasticSearchDomainDiscoveryQuery.class.getName());

	public static JSONObject getDocument(String domain, String documentID) {
		logger.log(Level.FINEST, "ElasticSearch - add retrieval to document");
			
		return ElasticSearchREST.retrieveDocument(domain, FileStorageAreaType.SANDBOX, documentID);
	}
		
	
	/**
	 * Adds a retrievalRecord object to the list of "retrievals" for a document (e.g., URL).
	 * The retrievalRecord contains the sessionID, executionNumber, UUID used internally, and the corresponding user
	 * 
	 * <code>
	 * JSONObject retrievalRecord = new JSONObject().put("sessionID", this._sessionID.toString())
	 *			                        		    .put("executionNumber", this._executionNumber)
	 *			                        		    .put("uuid", ddr.getDocumentUUID().toString())
	 *			                        		    .put("userID", userID)));
	 * </code>
	 * 
	 * @param retrievalRecord
	 * @param documentID
	 * @param discoveryObject - use this value if passed, otherwise retrieve from the stored record
	 * @return true if the update was successful, false if not.  Error messages put into the log.
	 */
	public static boolean addRetrievalToDocument(String domain, JSONObject retrievalRecord, String documentID, JSONObject discoveryObject) {
		logger.log(Level.FINEST, "ElasticSearch - add retrieval to document");
		
		if (discoveryObject == null) {
			JSONObject documentObject = getDocument(domain,documentID);
			discoveryObject = documentObject.getJSONObject("domainDiscovery");
		}
		
		JSONArray  retrievalArray  = discoveryObject.getJSONArray("retrievals");
		retrievalArray.put(retrievalRecord);
		discoveryObject.put("retrievals", retrievalArray);
		
		JSONObject updateObject = new JSONObject().put("domainDiscovery", discoveryObject);
		
		return ElasticSearchREST.updateDocument(domain, FileStorageAreaType.SANDBOX, documentID, updateObject);
	}

	/**
	 * Adds the processing metrics to a document
	 * 	 
	 * @param processingMetrics
	 * @param documentID
	 * @return true if the update was successful, false if not.  Error messages put into the log.
	 */
	public static boolean addProcessingMetricsToDocument(String domain, JSONObject processingMetrics, String documentID) {
		logger.log(Level.FINEST, "ElasticSearch - add processingMetrics to document");
		
		JSONObject documentObject = getDocument(domain,documentID);
		if (documentObject == null) {
			logger.log(Level.WARNING,"Unable to retrieve document in sandbox: (Domain: "+domain+", documentID: "+documentID);			
			return false;
		}
		
		JSONObject discoveryObject = documentObject.optJSONObject("domainDiscovery");
		if (discoveryObject == null) {
			logger.log(Level.WARNING,"Unable to retrieve domainDiscovery object for document in sandbox: (Domain: "+domain+", documentID: "+documentID);			
			return false;
		}
		
		discoveryObject.put("processingMetrics", processingMetrics);
		
		JSONObject updateObject = new JSONObject().put("domainDiscovery", discoveryObject);
		
		return ElasticSearchREST.updateDocument(domain, FileStorageAreaType.SANDBOX, documentID, updateObject);
	}	
	
	
	/**
	 * Removes retrievalRecord object with the given sessionID from the list of "retrievals" for a document (e.g., URL).
	 * The retrievalRecord contains the sessionID, executionNumber, UUID used internally, and the corresponding user
	 * 
	 * <code>
	 * JSONObject retrievalRecord = new JSONObject().put("sessionID", this._sessionID.toString())
	 *			                        		    .put("executionNumber", this._executionNumber)
	 *			                        		    .put("uuid", ddr.getDocumentUUID().toString())
	 *			                        		    .put("userID", userID)));
	 * </code>
	 * 
	 * @param domain
	 * @param documentID
	 * @param sessionID
	 * @return JSONArray containing the remaining retrievals.  Returns null if an exception occurred.  
	 *         If the array is blank, then the document is not tied to any DiscoverySessions 
	 */
	public static JSONArray removeRetrievalFromDocument(String domain, String documentID, String sessionID) {
		logger.log(Level.FINEST, "ElasticSearch - remove retrieval ("+sessionID+") from document: "+documentID);
		
		JSONObject documentObject = getDocument(domain,documentID);
		
		JSONObject discoveryObject = documentObject.getJSONObject("domainDiscovery");
		JSONArray  retrievalArray  = discoveryObject.getJSONArray("retrievals");
		for (int i= retrievalArray.length()-1; i >= 0; i--) {
			JSONObject retrievalRecord = retrievalArray.getJSONObject(i);
			if (sessionID.equals(retrievalRecord.getString("sessionID"))) {
				retrievalArray.remove(i);
			}
		}
		discoveryObject.put("retrievals", retrievalArray);
		JSONObject updateObject = new JSONObject().put("domainDiscovery", discoveryObject);

		ElasticSearchREST.updateDocument(domain, FileStorageAreaType.SANDBOX, documentID, updateObject);

		return retrievalArray;
	
	}	
	
	

	/**
	 * Updates the relevancy flag and the user who made the change in the domainDiscovery record
	 * 
	 * @param documentID
	 * @param userID
	 * @param relevantFlag
	 * @return true/false depending if the change was made in ElasticSearch or not.
	 */
	public static boolean updateRelevancy(String domain, String documentID, String userID, Boolean relevantFlag) {
		logger.log(Level.FINEST, "ElasticSearch - add retrieval to document");

		JSONObject documentObject = getDocument(domain,documentID);
		JSONObject discoveryObject = documentObject.getJSONObject("domainDiscovery");
		if (relevantFlag != null) {
			discoveryObject.put("relevant", relevantFlag.booleanValue());
		}
		else {
			discoveryObject.remove("relevant");
		}
		discoveryObject.put("relevantUser", userID);
		
		JSONObject updateObject = new JSONObject().put("domainDiscovery", discoveryObject);

		return ElasticSearchREST.updateDocument(domain, FileStorageAreaType.SANDBOX, documentID, updateObject);	
	}	
	
	public static java.util.List<JSONObject> getDocumentsForURL(String domain, String url) {		
	
		JSONObject queryClause = createQueryClauseForURL(url);
		JSONArray tempResults = ElasticSearchREST.searchQueryForAllResults(domain, FileStorageAreaType.SANDBOX, queryClause);
		
		java.util.ArrayList<JSONObject> results = new java.util.ArrayList<JSONObject>();
		for (int i=0; i<tempResults.length();i++) {
			results.add(tempResults.getJSONObject(i).getJSONObject("_source"));
		}
		logger.log(Level.INFO, "Number of documents retrieved for URL: "+results.size());
		return results;		
	}	
	
	/**
	 * 
	 * @param url
	 * @return
	 */
	public static JSONObject createQueryClauseForURL(String url) {

		JSONObject result = new JSONObject().put("term", new JSONObject().put("url.raw", url));
		
		return result;
	}		
	
	
	/**
	 * 
	 * @param index
	 * @param type
	 * @param sessionID
	 * @param exectionNumber if the execution number < 1, then this parameter is ignored.
	 * @return
	 */
	public static JSONObject createQueryClauseForSessionAndExecution(String sessionID, int executionNumber) {
		
		
		JSONArray must = new JSONArray();
		must.put(new JSONObject().put("match", new JSONObject().put("domainDiscovery.retrievals.sessionID.raw", sessionID)));
		if (executionNumber >0 ) {
			must.put(new JSONObject().put("match", new JSONObject().put("domainDiscovery.retrievals.executionNumber", executionNumber)));
		}
		
		JSONObject result = new JSONObject().put("nested", new JSONObject().put("path", "domainDiscovery.retrievals") 
				                            .put("query", new JSONObject().put("bool", new JSONObject().put("must", must)) ));
		
		return result;
	}
	
	
	/**
	 * 
	 * @param sessionID
	 * @return
	 */
	public static JSONObject createQueryClauseForSession(String sessionID) {
		
		
		JSONArray must = new JSONArray();
		must.put(new JSONObject().put("match", new JSONObject().put("domainDiscovery.retrievals.sessionID.raw", sessionID)));
		
		JSONObject result = new JSONObject().put("nested", new JSONObject().put("path", "domainDiscovery.retrievals") 
				                            .put("query", new JSONObject().put("bool", new JSONObject().put("must", must)) ));
		
		return result;
	}
	
	
	/**
	 * 
	 * @param jobID
	 * @return
	 */
	public static JSONObject createQueryClauseForJob(String jobID) {
		
		JSONArray must = new JSONArray();
		must.put(new JSONObject().put("match", new JSONObject().put("provenance.jobHistoryID", jobID)));
		
		JSONObject result = new JSONObject().put("bool", new JSONObject().put("must", must)) ;
		
		return result;
	}


	
	/**
	 * Returns all documents for the given sessionID and, optionally, executionNumber.  
	 * 
	 * @param sessionID
	 * @param executionNumber if the execution number < 1, then this parameter is ignored.
	 * @return
	 */
	public static java.util.List<JSONObject> getDocumentsForSessionAndExecution(String domain, String sessionID, int executionNumber) {	
		JSONObject queryClause = createQueryClauseForSessionAndExecution(sessionID, executionNumber);
		JSONArray tempResults = ElasticSearchREST.searchQueryForAllResults(domain, FileStorageAreaType.SANDBOX, queryClause);
		
		java.util.ArrayList<JSONObject> results = new java.util.ArrayList<JSONObject>();
		for (int i=0; i<tempResults.length();i++) {
			results.add(tempResults.getJSONObject(i).getJSONObject("_source"));
		}
		logger.log(Level.INFO, "Number of documents retrieved: "+results.size());
		return results;
	}		
	
	
	/**
	 * 
	 * @param uuid
	 * @return
	 */
	public static JSONObject createQueryClauseForScratchpad(String uuid) {
		
		JSONObject result = new JSONObject().put("nested", new JSONObject().put("path", "domainDiscovery.retrievals") 
				                            .put("query", new JSONObject().put("term", new JSONObject()
				                            .put("domainDiscovery.retrievals.uuid.keyword", uuid))));
		
		return result;
	}


	/**
	 * Return docs during export with sources from scratchpad
	 * 
	 * @param domain
	 * @param uuid from document in scratchpad
	 * @return
	 */
	public static java.util.List<JSONObject> getDocumentsForScratchpad(String domain, String uuid) {
		java.util.ArrayList<JSONObject> results = new java.util.ArrayList<JSONObject>();
		JSONObject queryClause = createQueryClauseForScratchpad(uuid);
		
		logger.log(Level.INFO, "queryClause: "+queryClause.toString());
		
		JSONArray tempResults = ElasticSearchREST.searchQueryForAllResults(domain, FileStorageAreaType.REGULAR, queryClause);
		for (int i=0; i<tempResults.length();i++) {
			results.add(tempResults.getJSONObject(i).getJSONObject("_source"));
		}
		logger.log(Level.INFO, "Number of documents retrieved: "+results.size());
		return results;
	}

}
