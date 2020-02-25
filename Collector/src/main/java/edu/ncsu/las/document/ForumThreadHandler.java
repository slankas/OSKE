package edu.ncsu.las.document;

import java.util.logging.Level;

import org.json.JSONObject;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.storage.HDFSStorage;
import edu.ncsu.las.storage.KafkaQueue;

/**
 * Used to process messages in a thread
 * It will also pull out the user's record and forward that on for processing.
 * 
 *
 */
public class ForumThreadHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.FORUM_THREAD } ;
	


	private boolean storeThreadInElasticSearch(Document threadDocument, JSONObject threadObject) {
		String domain = threadDocument.getDomainInstanceName();
		if (Configuration.storeJSONInElasticSearch(domain, threadDocument.getStorageArea()) == false) {
			return true;
		}
		/*
		String documentID = threadDocument.getDomain()+"_thread_"+threadObject.getString("threadID");
		
		JSONObject threadHitResult = ElasticSearchREST.retrieveDocument(domain, threadDocument.getStorageArea(),  documentID);
		*/
		JSONObject threadHitResult = ElasticSearchREST.retrieveDocumentByFieldSearch(domain, threadDocument.getStorageArea(), "alternateID", threadDocument.getAlternateID());
		
		
		
		boolean storageResult;
		if (threadHitResult == null) {
			storageResult = ElasticSearchREST.insertDocument(domain,threadDocument.getStorageArea(),  threadDocument.getUUID(), threadDocument.createJSONDocument());
		}
		else {		
			String originalID = threadHitResult.getString("_id");
			storageResult = ElasticSearchREST.updateDocument(domain,threadDocument.getStorageArea(), originalID, threadDocument.createJSONDocument());
		}
		if (storageResult) {
			logger.log(Level.INFO, "Stored thread record");
		}
		else {
			logger.log(Level.SEVERE, "Unable to store thread record: "+threadDocument.toString());
		}	
		return storageResult;
	}	
	
	protected void processDocument() {
		Document doc = this.getCurrentDocument();
		
		//run the post-document annotations now.  We already have "text" defined so this should produce the concepts..
		Annotator.annotate(AnnotatorExecutionPoint.POST_DOCUMENT, doc);
		
		JSONObject object = new JSONObject(doc.getContentDataAsString());
		doc.updatesourceDocument(object);
		this.storeThreadInElasticSearch(doc, object);
		
		KafkaQueue.sendToQueue(doc.getStorageArea(), doc.getDomainInstanceName(), doc.getUUID(), doc.createJSONDocument());
		(new HDFSStorage()).store(doc.getStorageArea(), doc.getDomainInstanceName(), doc.getUUID(), doc.createJSONDocument());	
		
		logger.log(Level.FINEST, "Processed thread record: "+object.toString());
	}
	
	@Override
	public String[] getMimeType() {
		return MIME_TYPE;
	}
	
	@Override
	public String getDocumentDomain() {
		return "";
	}
	@Override
	public String getDescription() {
		return "Places threads from forum-based websites in JSON-based stores";
	}
	@Override
	public int getProcessingOrder() {
		return 0;
	}	
}
