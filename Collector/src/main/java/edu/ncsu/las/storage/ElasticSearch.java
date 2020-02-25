package edu.ncsu.las.storage;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;


/**
 * 
 * 	 * @param configuration JSONObject specifying the configuration information to connet to ElasticSearch.  Must contain "host", "port", and "index".
	 *                      the port number should be the port used for the native java integration (typically 9300).  "index" is the database
 * 
 *
 */
public class ElasticSearch {
	private static Logger logger =Logger.getLogger(ElasticSearch.class.getName());
	
	
	/**
	 * For all of the domains currently managed by the system, ensure their domains exist.
	 * 
	 * @param domainList
	 */
	public static void checkAllIndexExistence(java.util.Collection<String> domainList) {
		for (String domainName: domainList) {
			if (domainName.equals(Domain.DOMAIN_SYSTEM)) { continue; }
			
			ensureIndexExists(domainName,FileStorageAreaType.REGULAR);
			ensureIndexExists(domainName,FileStorageAreaType.SANDBOX);
			ensureIndexExists(domainName,FileStorageAreaType.ARCHIVE);
		}
		logger.log(Level.INFO, "ElasticSearch index existence validated");
	}	
	
	
	/**
	 * Checks to see whether or not the given index exists.
	 * If it does not exist, then that index is created
	 * Regardless of existence, the mappings are also applied to handle any changes.
	 * Any pre-existing mappings will remain unchanged due to how ElasticSearch works. 
	 *
	 */
	public static void ensureIndexExists(String domainName, FileStorageAreaType fsat) {
		if (ElasticSearchREST.indexExists(domainName, fsat) == false) {
			logger.log(Level.INFO, "ElasticSearch - creating index (Domain: "+domainName+", Area: "+fsat.getLabel()+")");
			ElasticSearchREST.createIndex(domainName, fsat);
		}
		ElasticSearchREST.ensureIndexWritable(domainName, fsat);
		ElasticSearchREST.establishMappings(domainName, fsat);
	}

	/**
	 * Sends a document to be stored and indexed in ElasticSearch.
	 *
	 * @param domainInstance what "domain" are we sending the record to?
	 * @param area what "storage" area should this be placed.
	 * @param checkForDuplicateContent checks to see if there's an existing hash of the text content already present.  If so won't store if set to true
	 * @param uuid Unique identifier that will be used to store the object in ElasticSearch
	 * @param document  JSON representation of the document to store. (see createFromWebPageCrawl for details)
	 * 
	 * @return ResultCode  SENT- if the document was successfully sent to ElasticSearch.  ALREADY_PRESENT - if the document was already in elasticSearch, FAILURE otherwise.
	 */
	public static FileStorageStatusCode sendToElasticSearchEngine(String domainInstanceName, FileStorageAreaType area, boolean checkForDuplicateContent, String uuid, JSONObject document, boolean onRetry){
		logger.log(Level.FINER, "Push to ElasticSearch: "+uuid);
		
		if (checkForDuplicateContent) {
			JSONObject existingDocument = ElasticSearchREST.retrieveDocumentByHashCode(domainInstanceName, area, document.getString("hash"));
			if (existingDocument != null) {
				logger.log(Level.FINER, "ElasticSearch already contains content: "+document.getString("url"));
				return FileStorageStatusCode.ALREADY_PRESENT;
			}
		}
		
		boolean inserted = ElasticSearchREST.insertDocument(domainInstanceName, area,uuid, document);
		if (inserted) {
			logger.log(Level.FINER, "ElasticSearch saved document: "+uuid);
			return FileStorageStatusCode.SUCCESS;
		}
		else {
			logger.log(Level.SEVERE, "Unable to save to ElasticSearch - ");
			logger.log(Level.SEVERE,document.toString(4));
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		
	}
	
	

	
	/**
	 * Sends a document to be stored and indexed in ElasticSearch.
	 *
	 * @param domainInstance what "domain" are we sending the record to?
	 * @param area what "storage" area should this be placed.
	 * @param uuid Unique identifier that will be used to store the object in ElasticSearch
	 * @param document  JSON representation of the document to store. (see createFromWebPageCrawl for details)
	 * 
	 * @return ResultCode  SENT- if the document was successfully sent to ElasticSearch.  ALREADY_PRESENT - if the document was already in elasticSearch, FAILURE otherwise.
	 */
	public static FileStorageStatusCode sendUpdateToElasticSearchEngine(String domainInstanceName, FileStorageAreaType area, String uuid, JSONObject document, boolean onRetry){
		logger.log(Level.FINER, "Push to ElasticSearch: "+uuid);
				
		boolean updated = ElasticSearchREST.updateDocument(domainInstanceName, area, uuid, document);
		if (updated) {
			logger.log(Level.FINER, "ElasticSearch updated document: "+uuid);
			return FileStorageStatusCode.SUCCESS;
		}
		else {
			logger.log(Level.SEVERE, "Unable to update in ElasticSearch - ");
			logger.log(Level.SEVERE,document.toString(4));
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		
	}

	
	
}
