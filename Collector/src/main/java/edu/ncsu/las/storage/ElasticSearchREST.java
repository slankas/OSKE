package edu.ncsu.las.storage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.storage.export.ElasticSearchRecord;
import edu.ncsu.las.storage.export.ElasticSearchRecordProcessor;

/**
 * Provides a standard interface to query ElasticSearch through the REST API.
 * 
 * Side note: With ElasticSearch 2.x, the Java API has been rather finicky
 * -- as ElasticSearch uses Java Serialization, the link "breaks" if the client and server are using libraries from different versions
 * -- large # of dependencies used by ElasticSearch, which in turn causes issues with using other libraries that use the same dependency, but different versions 
 * -- occasional "node not available", eveny though it is there...
 * 
 *
 */
public class ElasticSearchREST {
	private static Logger logger = Logger.getLogger(ElasticSearchREST.class.getName());
	
	/** 
	 * 
	 * @param area what "storage" area should this be placed.
     *
	 * @return the appropriate REST end point for the area in ElasticSearch, returns null if the endpoint is not valid 
	 */
	public static String getRESTEndPoint(String domain, FileStorageAreaType area) {
		
		//String baseURL = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ELASTIC_REST)+Configuration.getDomainAndArea(domain, area);
		String baseURL = Configuration.getConfigurationProperty(domain, area, ConfigurationType.ELASTIC_STOREJSON_LOCATION);
		return baseURL;
	}	
	
	/**
	 * Search elastic search for the given query, returning upto a maximum of 10,000 records.
	 * https://www.elastic.co/guide/en/elasticsearch/reference/5.2/search-request-from-size.html
	 * 
	 * 
	 * @param domainInstanceName
	 * @param area
	 * @param queryClause
	 * @return
	 */
	public static JSONArray searchQueryForAllResults(String domainInstanceName, FileStorageAreaType area, JSONObject queryClause) {	
		return searchQueryForAllResults(domainInstanceName,area,queryClause,null);
	}
	
	/**
	 * Search ElasticSearch based upon the given query.
	 * 
	 * @param domainInstanceName
	 * @param area
	 * @param queryClause
	 * @param fieldsToReturn list of fields that should be returned, rather than the entire object.  If null return all fields
	 * @return
	 */
	public static JSONArray searchQueryForAllResults(String domainInstanceName, FileStorageAreaType area, JSONObject queryClause, String[] fieldsToReturn) {
		String url = getRESTEndPoint(domainInstanceName, area);
		JSONArray results = new JSONArray();
		
		String restEndPoint = url + "/_search";
		logger.log(Level.INFO,"restEndPoint: "+restEndPoint);
		
		long fromValue = 0;
		long totalHits = 1;
		try {
			while (fromValue < totalHits) {
				JSONObject fullQuery = new JSONObject().put("from", fromValue)
						                               .put("size", 100)
						                               .put("query", queryClause);
				
				if (fieldsToReturn != null) {
					JSONArray fields = new JSONArray();
					for (String field: fieldsToReturn) {
						fields.put(field);
					}
					fullQuery.put("_source", fields);
				}
				logger.log(Level.INFO,"fullQuery: "+fullQuery.toString());
				
				HttpResponse<JsonNode> jsonResponse = Unirest.post(restEndPoint)				
						  .header("accept", "application/json")
						  .header("Content-Type","application/json")
						  .body(fullQuery)
						  .asJson();
				JSONObject esResult = jsonResponse.getBody().getObject();		
				
				if (esResult.has("error") == false) {
					totalHits = esResult.getJSONObject("hits").getJSONObject("total").getLong("value");
					JSONArray hits =  esResult.getJSONObject("hits").getJSONArray("hits");
					for (int i=0;i<hits.length();i++) {
						results.put(hits.get(i));
					}
					fromValue += hits.length();
				}
				else {
					logger.log(Level.WARNING,"Possible query issue: "+esResult.toString());
					logger.log(Level.WARNING,"The above may be a non-issue if searching for nested documents that do not exist.");
					break;
				}
			}
		}			
		catch (UnirestException ure) {
			logger.log(Level.SEVERE,"Unirest Exception: ",ure);
			return null;
		}
		
		return results;
	}	

	/**
	 * 
	 * @param domainInstanceName
	 * @param area
	 * @param queryClause
	 * @param maxRecords maximum number of records to return.  -1 if no limited.
	 * @return
	 */
	public static void searchQueryForAllResultsUsingScroll(String domainInstanceName, FileStorageAreaType area, 
			                                                    JSONObject queryClause, long maxRecords,
			                                                    ElasticSearchRecordProcessor processor) throws IOException {
		String url = getRESTEndPoint(domainInstanceName, area);
		
		String fullURL = url + "/_search?scroll=1m";
		//System.out.println("******* fullUrl  =" + fullURL);
		String scrollURL =  Configuration.getConfigurationProperty(domainInstanceName, ConfigurationType.ELASTIC_REST) + "_search/scroll";
		String scrollID = "";
		
		long fromValue = 0;
		long totalHits = 1;
		try {
			JSONObject fullQuery = new JSONObject()
					.put("size", 1000)
					.put("track_total_hits", true)
                    .put("query", queryClause)
                    .put("sort", new JSONArray().put("_doc"));
			//System.out.println("*********** fullQuery = " + fullQuery.toString());
			HttpResponse<JsonNode> jsonResponse = Unirest.post(fullURL)				
					  .header("accept", "application/json")
					  .header("Content-Type","application/json")
					  .body(fullQuery)
					  .asJson();
			
			if ( jsonResponse.getBody().getObject().has("_scroll_id") ) {
				scrollID = jsonResponse.getBody().getObject().getString("_scroll_id");
			}
			 
			
			searchLoop:
			while (fromValue < totalHits ) {

				JSONObject esResult = jsonResponse.getBody().getObject();
				
				totalHits = esResult.getJSONObject("hits").getJSONObject("total").getLong("value");
				JSONArray hits =  esResult.getJSONObject("hits").getJSONArray("hits");
				
				for (int i=0;i<hits.length();i++) {
					processor.processRecord(new ElasticSearchRecord(hits.getJSONObject(i)));
					fromValue++;
					if (maxRecords > 0 && fromValue >= maxRecords) {
						break searchLoop;
					}
				}
				
				if (fromValue < totalHits) {
					JSONObject scrollQuery = new JSONObject().put("scroll", "1m")
	                        .put("scroll_id", scrollID);
					jsonResponse = Unirest.post(scrollURL)				
							.header("accept", "application/json")
							.header("Content-Type","application/json")
							.body(scrollQuery)
							.asJson();
				}
			}
			jsonResponse = Unirest.delete(scrollURL)	
					.header("accept", "application/json")
					.header("Content-Type","application/json")
					.body(new JSONObject().put("scroll_id", new JSONArray().put(scrollID)))
					.asJson();
		}			
		catch (UnirestException ure) {
			logger.log(Level.SEVERE,"Unirest Exception: ",ure);
			return;
		}
	}		
	
	
	
	public static String queryFullTextSearch(String domainInstanceName, FileStorageAreaType area, JSONObject query) {
		String url = getRESTEndPoint(domainInstanceName, area);
		
		String restEndPoint = url + "_search";
		String result = "";
		
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(restEndPoint)
				  .header("accept", "application/json")
				  .header("Content-Type","application/json")
				  .body(query.toString())
				  .asJson();
			result = jsonResponse.getBody().toString();
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE,"Unirest Exception: ",ure);

			result = "error: "+ure.toString();
		}
		
		return result;
	}	

	/**
	 * Checks if the given document exists within ElasticSearch.   Utilizes the "head" method so that no data is returned.
	 * 
	 * @param area In which index should we look.
	 * @param id   what is the document's ID?
	 * @return true if the document is found (http status code 200 return from ElasticSearch), otherwise false for everything else
	 */
	public static boolean documentExists(String domainInstanceName, FileStorageAreaType area, String id) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"_doc/"+id;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.head(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			return (jsonResponse.getStatus() == 200);
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to check if document exists in ElasticSearch: "+ url);
			return false;
		}
	}
	
	/**
	 * Finds a given document exists by searching the hash field for a given index(area) and type in ElasticSearch.
	 * 
	 * If more than one document is found, only the first one is returned and a warning message is printed to the console
	 * 
	 * @param area In which index should we look.
	 * @param id   what is the hash code?
	 * @return The document as a JSON record.   If not found, null is returned.
	 */
	public static JSONObject retrieveDocumentByHashCode(String domainInstanceName, FileStorageAreaType area, String hashCode) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"/_search?q=hash%3A%22"+hashCode+"%22";
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.get(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			
			if(jsonResponse.getStatus() != 200) {
				logger.log(Level.SEVERE, "ElasticSearch document retrevial failed: " + url);
				return null;
			}
			
			JSONObject esResult = jsonResponse.getBody().getObject();
			long numHits = esResult.getJSONObject("hits").getJSONObject("total").getLong("value");
			if ( numHits> 0) {
				if (numHits >1) {
					logger.log(Level.WARNING, "More than one document found by hashcode: "+ url);
				}
				return esResult.getJSONObject("hits").getJSONArray("hits").getJSONObject(0).getJSONObject("_source");
			}
			else {
				return null;
			}
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to get document by hashcode in ElasticSearch: "+ url);
			return null;
		}
	}
		
		
	
	/**
	 * Returns the specific document if it exists within ElasticSearch. 
	 * If the document is not found, then null is returned.
	 * 
	 * @param area In which index should we look.
	 * @param id   what is the document's ID?
	 * @return The document as a JSON record.   If not found, null is returned.
	 */
	public static JSONObject retrieveDocument(String domainInstanceName, FileStorageAreaType area, String id) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"_doc/"+id;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.get(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
						
			if (esResult.getBoolean("found")) {
				return esResult.getJSONObject("_source");
			}
			else {
				return null;
			}
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to retrieve document in ElasticSearch: "+ url);
			return null;
		}
	}	

	
	/**
	 * Deletes the given document if it exists within ElasticSearch.
	 * 
	 * @param area In which index should we delete.
	 * @param id   what is the document's ID?
	 * @return true if the document is deleted, otherwise false for everything else
	 */
	public static boolean deleteDocument(String domainInstanceName, FileStorageAreaType area, String id) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"_doc/"+id;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.delete(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
			return esResult.optBoolean("found", true);
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to delete document in ElasticSearch: "+ url);
			return false;
		}
	}


	/**
	 * Creates the given document in ElasticSearch.
	 * 
	 * @param area In which index should we should create the document.
	 * @param id   what is the document's ID?
	 * @param document what is the document to be inserted
	 * @return true if the document is successfully inserted, false if it was an update.
	 */

	public static boolean insertDocument(String domainInstanceName, FileStorageAreaType area, String id, JSONObject document) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"_doc/"+id;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .body(document)
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
			
			if (esResult.has("error")) {
				logger.log(Level.WARNING, "Unable to insert document in ElasticSearch: "+ url);
				logger.log(Level.WARNING, "                                            "+ esResult.toString());
				return false;
			}
			
			return esResult.getString("result").equalsIgnoreCase("created");
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to insert document in ElasticSearch: "+ url);
			return false;
		}	
	}
	
	/**
	 * Creates the given document in ElasticSearch.
	 * 
	 * @param domain used to access the configuration property for the rest end point
	 * @param id   what is the document's ID?
	 * @param document what is the document to be inserted
	 * @return true if the document is successfully inserted, false if it was an update.
	 */
	public static boolean insertDocument(String domain, String index, JSONObject document) {
		String restURL = Configuration.getConfigurationProperty(domain, ConfigurationType.ELASTIC_REST) + index +"/_doc/";
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(restURL)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .body(document)
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
			
			if (esResult.has("error")) {
				logger.log(Level.WARNING, "Unable to insert document in ElasticSearch: "+ restURL);
				logger.log(Level.WARNING, "                                            "+ esResult.toString());
				return false;
			}
			
			return esResult.getString("result").equalsIgnoreCase("created");
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to insert document in ElasticSearch: "+ restURL);
			return false;
		}	
	}	
	
	
	/**
	 * Updates the given document in ElasticSearch.  
	 * In ElasticSearch, this is technically equal to a delete and insert, but atomic..
	 * 
	 * @param area In which index should we update.
	 * @param id   what is the document's ID?
	 * @param document the replacement document.
	 * @return true if the document is successfully updated, false otherwise.
	 */

	public static boolean updateDocument(String domainInstanceName, FileStorageAreaType area, String id, JSONObject document) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		JSONObject elasticDoc = new JSONObject().put("doc",document);
		
		String url = restURL+"_update/"+id;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .body(elasticDoc)
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
			
			if (esResult.has("error")) {
				logger.log(Level.WARNING, "Unable to update document in ElasticSearch: "+ url);
				logger.log(Level.WARNING, "                                            "+ esResult.toString());
				return false;
			}
			
			return true;
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to update document in ElasticSearch: "+ url);
			return false;
		}
		
	}


	/**
	 * Returns the specific document if it exists within ElasticSearch. 
	 * If the document is not found, then null is returned.
	 * 
	 * @param area In which index should we look.
	 * @param alternateIDField what is the alternate ID field that can be used to search
	 * @param id   what is the document's ID?
	 * @return the "result" document from Elastic search.  the actual record is in "_source" field.  
	 *         This is needed so that the _id is available.  If not found, null is returned.
	 */
	public static JSONObject retrieveDocumentByFieldSearch(String domainInstanceName, FileStorageAreaType area, String alternateIDField, String id) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"_search?q="+alternateIDField+":"+id;
		
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.get(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
			long totalCount = esResult.getJSONObject("hits").getJSONObject("total").getLong("value");
			if (totalCount > 0) {
				JSONObject hit = esResult.getJSONObject("hits").getJSONArray("hits").getJSONObject(0);
				return hit;
			}
			else {
				return null;
			}
		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to retrieve document in ElasticSearch: "+ url);
			return null;
		}		

	}

	/**
	 * Checks whether or not the appropriate index exists for the domain and storage area.
	 * 
	 * @param domainName
	 * @param area
	 * @return
	 */
	public static boolean indexExists(String domainName, FileStorageAreaType area) {
		String restURL = getRESTEndPoint(domainName, area);
		
		String url = restURL;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.head(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			return (jsonResponse.getStatus() == 200);
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to check if index exists in ElasticSearch: "+ url);
			return false;
		}
	}

	/**
	 * Checks whether or not the given index exists.
	 * 
	 * @param indexName
	 * @return
	 */
	public static boolean indexExists(String indexName) {
		String restURL = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ELASTIC_REST);
		
		String url = restURL+indexName;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.head(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			return (jsonResponse.getStatus() == 200);
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to check if index exists in ElasticSearch: "+ url);
			return false;
		}
	}	
	


	public static void createIndex(String domainName, FileStorageAreaType area) {
		String url = getRESTEndPoint(domainName, area);
		
		JSONObject settings = new JSONObject().put("settings",Configuration.getConfigurationObject(domainName, ConfigurationType.ELASTIC_DEFAULT_SETTINGS));

		try {
			// create index w/ default settings from the configuration
			HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
					.header("accept", "application/json")
					.header("Content-Type","application/json")
					.body(settings)
	                .asJson();
			
			JSONObject esResult = jsonResponse.getBody().getObject();
			if (esResult.optBoolean("acknowledged",false) == false) {
				logger.log(Level.SEVERE, "Unable to create index exists in ElasticSearch (URL: "+url+" ): "+ esResult.optString("error",""));
				logger.log(Level.SEVERE, "JSON Object: "+settings.toString());
				return;
			}
			
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to create index in ElasticSearch: "+ url);
		}
	}	
	
	protected static void ensureIndexWritableWithURL(String url) {
		try {
			// create index w/ default settings from the configuration
			HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
					.header("accept", "application/json")
					.header("Content-Type","application/json")
					.body("{ \"index.blocks.read_only_allow_delete\": null }")
	                .asJson();
			
			JSONObject esResult = jsonResponse.getBody().getObject();
			if (esResult.optBoolean("acknowledged",false) == false) {
				logger.log(Level.SEVERE, "Unable to ensure index writable in ElasticSearch (URL: "+url+" ): "+ esResult.optString("error",""));
				return;
			}
			
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to ensure index writable in ElasticSearch: "+ url);
		}
	}
	
	public static void ensureIndexWritable(String domainName, FileStorageAreaType area) {
		String restURL = getRESTEndPoint(domainName, area);
		
		String url = restURL+"_settings";
		ensureIndexWritableWithURL(url);
	}	
	
	public static void ensureIndexWritable(String indexName) {
		String restURL = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ELASTIC_REST);
		
		String url = restURL+indexName+"/_settings";
		ensureIndexWritableWithURL(url);
	}		
	
	
	public static void establishMappings(String domainName, FileStorageAreaType area) {
		String url = getRESTEndPoint(domainName, area);
		try {
			// Create the schemas for properties from annotators
			JSONObject mappings = Configuration.getConfigurationObject(domainName, ConfigurationType.ELASTIC_DEFAULT_MAPPINGS);
			JSONObject properties = null;
			if (mappings.has("web")) {
				properties = mappings.getJSONObject("web").getJSONObject("properties");  
			}
			else {
				properties = mappings.getJSONObject("properties");  
			}
			
			for(Annotator annotator : Annotator.getAllAnnotators()) {
				JSONObject newSchema = annotator.getSchema();
				if(newSchema == null) {
					continue;
				}
				
				if(properties.has(annotator.getCode())) {
					logger.log(Level.WARNING, "Possible annotator schema conflict:" + annotator.getCode());
				}
				
				properties.put(annotator.getCode(), newSchema);
			}
			
			// Send default mapping to ElasticSearch
			HttpResponse<JsonNode> jsonMappingResponse = Unirest.put(url+"_mapping")
					.header("accept", "application/json")
					.header("Content-Type","application/json")
					.body(mappings)
	                .asJson();
			
			JSONObject esMappingResult = jsonMappingResponse.getBody().getObject();
			if (esMappingResult.optBoolean("acknowledged",false) == false) {
				logger.log(Level.SEVERE, "Unable to put default mapping in ElasticSearch (URL: "+url+" ): "+ esMappingResult.optString("error",""));
				return;
			}			
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to establishMappings  in ElasticSearch: "+ url);
		}
	}	
	
	
	// used to create the instrumentation index
	public static void createIndex(String indexName, JSONObject settings, JSONObject mappings) {
		String restURL = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.ELASTIC_REST);
		String url = restURL+indexName;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
					.header("accept", "application/json")
					.header("Content-Type","application/json")
					.body(settings)
	                .asJson();
			
			JSONObject esResult = jsonResponse.getBody().getObject();
			if (esResult.optBoolean("acknowledged",false) == false) {
				logger.log(Level.SEVERE, "Unable to create index exists in ElasticSearch (URL: "+url+" ): "+ esResult.optString("error",""));
				return;
			}
			
			HttpResponse<JsonNode> jsonMappingResponse = Unirest.put(url+"/_mapping")
					.header("accept", "application/json")
					.header("Content-Type","application/json")
					.body(mappings)
	                .asJson();
			
			JSONObject esMappingResult = jsonMappingResponse.getBody().getObject();
			if (esMappingResult.optBoolean("acknowledged",false) == false) {
				logger.log(Level.SEVERE, "Unable to put  mapping in ElasticSearch (URL: "+url+" ): "+ esMappingResult.optString("error",""));
				return;
			}			
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to create if index exists in ElasticSearch: "+ url);
		}
	}	
	
	public static boolean deleteIndex(String domain, FileStorageAreaType area) {
		
		String url = getRESTEndPoint(domain, area);
		
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.delete(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			//TODO: need to check the response and set the return code appropriately
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to delete Index: "+ url);
			return false;
		}
		return true;
		
	}
	
	/**
	 * Deletes records in ElasticSearch based upon the based in query.
	 * 
	 * @param domainInstanceName
	 * @param area
	 * @param query
	 * @return JSONArray of any failures that occured.  if this array is emptry, then no errors occurred
	 */
	public static JSONArray deleteByQuery(String domainInstanceName, FileStorageAreaType area, JSONObject query) {
		String url = getRESTEndPoint(domainInstanceName, area);
		
		String restEndPoint = url + "_doc/_delete_by_query";
		JSONArray failures = new JSONArray();
		
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(restEndPoint)
				  .header("accept", "application/json")
				  .header("Content-Type","application/json")
				  .body(query.toString())
				  .asJson();
			failures = jsonResponse.getBody().getObject().getJSONArray("failures");
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE,"Unirest Exception: ",ure);

			failures.put("error: "+ure.toString());
		}
		
		return failures;
	}


	/**
	 * Returns the mapping for the index as identified by the domain and storage area
	 * 
	 * @param domainInstanceName
	 * @param area In which index should we look.
	 * @return mappings for a particular Elasticsearch mapping
	 */
	public static JSONObject retrieveIndexMappings(String domainInstanceName, FileStorageAreaType area) {
		String restURL = getRESTEndPoint(domainInstanceName, area);
		
		String url = restURL+"_mapping";
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.get(url)
	                .header("accept", "application/json")
	                .header("Content-Type","application/json")
	                .asJson();
			JSONObject esResult = jsonResponse.getBody().getObject();
						
			return esResult;

		}
		catch (UnirestException ure) {
			logger.log(Level.WARNING, "Unable to retrieve index mapping in ElasticSearch: "+ url);
			return null;
		}
	}		
	
	
}


