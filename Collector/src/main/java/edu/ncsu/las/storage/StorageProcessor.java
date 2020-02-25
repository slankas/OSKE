package edu.ncsu.las.storage;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;
import edu.ncsu.las.model.collector.type.MimeType;

/**
 * 
 */
public class StorageProcessor {
	private static Logger logger =Logger.getLogger(Collector.class.getName());
	
	
	private static StorageProcessor _theStorageProcessor;

	
	public static StorageProcessor getTheStorageProcessor() {
		if (_theStorageProcessor == null) {
			_theStorageProcessor = new StorageProcessor();
		}
		return _theStorageProcessor;
	}
	
	private StorageProcessor()  {
	}	

	/**
	 * Sends any data to be saved to to the appropriate storage mechanism (file, HDFS, etc.)
	 * 
	 * @param filename What filename should be used to save the file?  Normally this will just be the assigned UUID. Prepended to the fullclasspathname value.
	 * @param data contents of the file.
	 * @param metaData optional details about the file to begin to store in the file
	 */
	public void saveRawData(FileStorageAreaType area, String domain,  String filename, byte[] data, JSONObject metaData) {
		
		// need to store in file / Accumulo as required...
		if (Configuration.storeRawInAccumulo(domain, area)) {
			logger.log(Level.FINER, "Storing  "+filename+" in Accumulo");
			(new AccumuloStorage()).store(domain, area, filename, data, metaData);
			logger.log(Level.INFO, "Stored "+filename+" to Accumulo");
		}
		
		if (Configuration.storeRawInFile(domain, area)) {
			logger.log(Level.FINER, "Storing  "+filename+" to file storage");
			(new FileStorage()).store(domain, area, filename, data, metaData);
			logger.log(Level.INFO, "Stored "+filename+" to file storage");
		}		
		
	}
	
	/**
	 * loads data from the first located storage mechanism (file, HDFS, etc.)
	 * 
	 * @param fir
	 * @param filename What filename should be used to load the file?  Normally this will just be the assigned UUID. 
	 * return data contents of the file.  Null is return if the file can not be loaded.
	 * 
	 */
	public byte[] loadRawData(FileStorageAreaType area, String domain, String filename) {
		if (Configuration.storeRawInAccumulo(domain, area)) {
			logger.log(Level.FINER, "loading  "+filename+" from Accumulo");
			byte data[] = (new AccumuloStorage()).retrieve(domain, area, filename);
			if (data != null) {
				logger.log(Level.INFO, "loaded "+filename+" from Accumulo");
				return data;
			}
			else {
				logger.log(Level.FINER, filename+" not found in Accumulo");
			}
		}
		
		if (Configuration.storeRawInFile(domain, area)) {
			logger.log(Level.FINER, "loading  "+filename+" from file");
			byte data[] = (new FileStorage()).retrieve(domain, area, filename);
			if (data != null) {
				logger.log(Level.INFO, "loaded "+filename+" from file");
				return data;
			}
			else {
				logger.log(Level.FINER, filename+" not found in file");
			}
		}			
			
		return null;
	}	

	
	/**
	 * deletes raw data (ie,crawled data) from all storage mechanisms (file, accumulo, etc.) in use
	 * 
	 * @param area what area should be checked for the files
	 * @param domain what is the domain
	 * @param filename What filename should be used to delete the file?  Normally this will just be the assigned UUID. 
	 * return data contents of the file.  Null is return if the file can not be loaded.
	 * 
	 */
	public void deleteRawData(FileStorageAreaType area, String domain, String filename) {
		
		if (Configuration.storeRawInAccumulo(domain, area)) {
			logger.log(Level.FINER, "deleting  "+filename+" from Accumulo");
			FileStorageStatusCode sc = (new AccumuloStorage()).delete(domain, area, filename);
			if (sc == FileStorageStatusCode.UNKNOWN_FAILURE) {
				logger.log(Level.FINER, filename+" unable to delete from Accumulo");				
			}
			else {
				logger.log(Level.INFO, "deleted "+filename+" from Accumulo");
			}
		}
		
		if (Configuration.storeRawInFile(domain, area)) {
			logger.log(Level.FINER, "deleting  "+filename+" from file");
			FileStorageStatusCode sc= (new FileStorage()).delete(domain, area, filename);
			if (sc == FileStorageStatusCode.UNKNOWN_FAILURE) {			
				logger.log(Level.FINER, filename+" unable to delete from file");
			}
			else {
				logger.log(Level.INFO, "deleted "+filename+" from file");
			}
		}
	}		
	
	/**
	 *  Sends any data to be saved to to the appropriate storage mechanism (file, HDFS, etc.)
	 *  
	 * @param area
	 * @param checkForDuplicateContent checks to see if there's an existing hash of the text content already present.  If so won't store if set to true
	 * @param UUID
	 * @param document
	 * @param currentDocument
	 * @return
	 */
	public FileStorageStatusCode saveJSONData(FileStorageAreaType area, String domain, boolean checkForDuplicateContent, String UUID, JSONObject document, Document currentDocument) {
		if (MimeType.isCompressedType(currentDocument.getMimeType())) {
			logger.log(Level.INFO, "Not saving compressed file");	
			return FileStorageStatusCode.INVALID_CONTENT; 
		}
		
		FileStorageStatusCode elasticResult = FileStorageStatusCode.NOT_EXECUTED;
		FileStorageStatusCode kafkaResult   = FileStorageStatusCode.NOT_EXECUTED;
		FileStorageStatusCode hdfsResult    = FileStorageStatusCode.NOT_EXECUTED;
		
		
		if (Configuration.storeJSONInElasticSearch(domain, area)) {
			elasticResult = ElasticSearch.sendToElasticSearchEngine(domain,area, checkForDuplicateContent, UUID, document, false);
		}

		if (elasticResult == FileStorageStatusCode.ALREADY_PRESENT){
			logger.log(Level.INFO, "Duplicate content present in JSON, not sending to KAFKA or HDFS");
			return elasticResult;
		}
		
		if (Configuration.sendJSONToKafka(domain, area)) {
			kafkaResult = KafkaQueue.sendToQueue(area, domain, UUID, document);
		}		

		if (Configuration.storeJSONInHDFS(domain, area)) {
			hdfsResult = (new HDFSStorage()).store(area, domain, UUID, document);
		}	
		
		if (elasticResult == FileStorageStatusCode.UNKNOWN_FAILURE || kafkaResult == FileStorageStatusCode.UNKNOWN_FAILURE ||  hdfsResult == FileStorageStatusCode.UNKNOWN_FAILURE) {
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		else {
			return elasticResult;
		}
	}

	
	/**
	 *  Sends any data to be saved to to the appropriate storage mechanism (file, HDFS, etc.)
	 *  
	 * @param area
	 * @param UUID
	 * @param document
	 * @param currentDocument
	 * @return
	 */
	public FileStorageStatusCode updateJSONData(Document currentDocument) {
		if (MimeType.isCompressedType(currentDocument.getMimeType())) {
			logger.log(Level.INFO, "Not saving compressed file");	
			return FileStorageStatusCode.INVALID_CONTENT; 
		}
		FileStorageAreaType area = currentDocument.getStorageArea();
	    String domain = currentDocument.getDomainInstanceName();
	    String UUID = currentDocument.getUUID();
		
		FileStorageStatusCode elasticResult = FileStorageStatusCode.NOT_EXECUTED;
		FileStorageStatusCode kafkaResult   = FileStorageStatusCode.NOT_EXECUTED;
		FileStorageStatusCode hdfsResult    = FileStorageStatusCode.NOT_EXECUTED;
		
		JSONObject updateObject = currentDocument.getUnsavedAnnotations();
		
		
		if (Configuration.storeJSONInElasticSearch(domain, area)) {
			elasticResult = ElasticSearch.sendUpdateToElasticSearchEngine(domain,area, UUID, updateObject, false);
		}

		JSONObject document = currentDocument.createJSONDocument();
	
		if (Configuration.sendJSONToKafka(domain, area)) {
			kafkaResult = KafkaQueue.sendToQueue(area, domain, UUID, document);
		}		

		if (Configuration.storeJSONInHDFS(domain, area)) {
			hdfsResult = (new HDFSStorage()).store(area, domain, UUID, document);
		}	
		
		if (elasticResult == FileStorageStatusCode.UNKNOWN_FAILURE || kafkaResult == FileStorageStatusCode.UNKNOWN_FAILURE ||  hdfsResult == FileStorageStatusCode.UNKNOWN_FAILURE) {
			return FileStorageStatusCode.UNKNOWN_FAILURE;
		}
		else {
			return elasticResult;
		}
	}
	
	
	
	
	
	
	
	
	
	public void deleteJSONRecords(FileStorageAreaType area, String domain, UUID jobID, java.util.Set<UUID> storageIDs) {
		
		if (Configuration.storeJSONInHDFS(domain, area)) {
			HDFSStorage.purgeJobRecords(domain,area, jobID, storageIDs);
		}
		
		if (Configuration.storeJSONInElasticSearch(domain, area)) {
			JSONObject jobQuery = ElasticSearchQuery.createSelectByJobUUID(jobID);
			ElasticSearchREST.deleteByQuery(domain, area, jobQuery);
		}
		
		if (Configuration.sendJSONToKafka(domain, area)) {
			for (UUID sourceUUID: storageIDs) {
				JSONObject purgeRecord = new JSONObject().put("ACTION", "purge")
						                                 .put("jobID", jobID.toString())
					                                     .put("source_UUID", sourceUUID.toString());
				KafkaQueue.sendToQueue(area, domain, sourceUUID.toString(), purgeRecord);
			}
		}
		
	}
	

	/**
	 * 
	 * @param area
	 * @param UUID
	 * @return null if the record is not found, or if elastic search is not configured
	 */
	public JSONObject loadJSONRecord(FileStorageAreaType area, String domain, String UUID) {
		if (Configuration.storeJSONInElasticSearch(domain, area)) {
			//(new ElasticSearch()).retrieve(area, UUID);
			//TODO: Implement
		}
		return null;
				
	}
	
}
