package edu.ncsu.las.storage;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;


import edu.ncsu.las.model.collector.type.FileStorageAreaType;

/**
 * Contains the queries used to store collection information within each document
 * 
 */
public class ElasticSearchCollectionQuery {
	private static Logger logger =Logger.getLogger(ElasticSearchCollectionQuery.class.getName());

	public static boolean addCollectionToDocument(String domain, String collectionID, String documentID, String addedBy) {
		logger.log(Level.INFO, "ElasticSearch - add collection to document");

		//String collectionType = Configuration.getConfigurationProperty(domain, ConfigurationType.ELASTIC_TYPE_COLLECTION);
		
		JSONObject documentRecord   = ElasticSearchREST.retrieveDocument(domain, FileStorageAreaType.REGULAR, documentID);
		//JSONObject collectionRecord = ElasticSearchREST.retrieveDocument(domain, FileStorageAreaType.REGULAR, collectionType, collectionID);
		
		if (documentRecord == null) {
			logger.log(Level.INFO, "ElasticSearch - unable to find document to add to collection, documentID: "+documentID);
			return false;
		}
		
		JSONArray userCollection;
		if (documentRecord.has("user_collection")) {
			userCollection = documentRecord.getJSONArray("user_collection");
		}
		else {
			userCollection = new JSONArray();
		}
		
		for (int i=0;i<userCollection.length();i++) {
			JSONObject collectionObject = userCollection.getJSONObject(i);
			if (collectionObject.getString("collection_id").equals(collectionID)) {
				logger.log(Level.FINE, "ElasticSearch - add collection to document, document already in collection");
				return true;
			}
		}
		
		JSONObject newCollectionRecord = new JSONObject();

		//String collection_name = collectionRecord.getString("name");
		newCollectionRecord.put("added_by", addedBy);
		newCollectionRecord.put("collection_id", collectionID);
		//newCollectionRecord.put("collection_name", collection_name);
		newCollectionRecord.put("date_added", Instant.now().toString());

		userCollection.put(newCollectionRecord);

		JSONObject updateObject = new JSONObject().put("user_collection", userCollection);
		return ElasticSearchREST.updateDocument(domain, FileStorageAreaType.REGULAR, documentID, updateObject);
	}


	public static boolean removeCollectionFromDocument(String domain, String collectionID, String documentID) {	
		logger.log(Level.INFO, "ElasticSearch - Remove collection from document: collectionID: "+collectionID+", documentID: "+documentID);
		
		JSONObject documentRecord   = ElasticSearchREST.retrieveDocument(domain, FileStorageAreaType.REGULAR, documentID);
		
		JSONArray userCollection;
		if (documentRecord.has("user_collection")) {
			userCollection = documentRecord.getJSONArray("user_collection");
			boolean foundID = false;
			for (int i= userCollection.length()-1;i >=0; i--) {
				JSONObject collectionObject = userCollection.getJSONObject(i);
				if (collectionObject.getString("collection_id").equals(collectionID)) {
					foundID = true;
					userCollection.remove(i);
				}
			}				
			if (foundID) {
				JSONObject updateObject = new JSONObject().put("user_collection", userCollection);
				return ElasticSearchREST.updateDocument(domain, FileStorageAreaType.REGULAR, documentID, updateObject);
			}
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param collectionID
	 * @return
	 */
	public static JSONObject createQueryClauseForCollectionID(String collectionID) {
		
		
		JSONArray must = new JSONArray();
		must.put(new JSONObject().put("match", new JSONObject().put("user_collection.collection_id", collectionID)));
		
		JSONObject result = new JSONObject().put("nested", new JSONObject().put("path", "user_collection") 
				                            .put("query", new JSONObject().put("bool", new JSONObject().put("must", must)) ));
		
		return result;
	}		
	

	public static java.util.List<JSONObject> getDocumentsInCollection(String domain, String collectionID) {	
		JSONObject queryClause = createQueryClauseForCollectionID(collectionID);

		JSONArray tempResults = ElasticSearchREST.searchQueryForAllResults(domain, FileStorageAreaType.SANDBOX, queryClause);
		
		java.util.ArrayList<JSONObject> results = new java.util.ArrayList<JSONObject>();
		for (int i=0; i<tempResults.length();i++) {
			results.add(tempResults.getJSONObject(i).getJSONObject("_source"));
		}
		logger.log(Level.INFO, "Number of documents retrieved: "+results.size());
		return results;
	}

}
