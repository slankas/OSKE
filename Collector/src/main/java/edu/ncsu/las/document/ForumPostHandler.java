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
 * Used to process a forum post
 * It will also pull out the user's record and forward that on for processing.
 * 
 *
 */
public class ForumPostHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.FORUM_POST } ;
	


	private boolean storePostInElasticSearch(Document postDocument, JSONObject postObject) {
		String domain = postDocument.getDomainInstanceName();
		if (Configuration.storeJSONInElasticSearch(domain, postDocument.getStorageArea()) == false) {
			return true;
		}
		/*
		String documentID = postDocument.getDomain()+"_post_"+postObject.getString("postID");
		
		JSONObject postResult = ElasticSearchREST.retrieveDocument(domain, postDocument.getStorageArea(), documentID);
		*/
		
		JSONObject postResult = ElasticSearchREST.retrieveDocumentByFieldSearch(domain, postDocument.getStorageArea(), "alternateID", postDocument.getAlternateID());
		
		boolean storageResult;
		if (postResult == null) {
			storageResult = ElasticSearchREST.insertDocument(domain,postDocument.getStorageArea(), postDocument.getUUID(), postDocument.createJSONDocument());
		}
		else {		
			String originalID = postResult.getString("_id");
			storageResult = ElasticSearchREST.updateDocument(domain,postDocument.getStorageArea(), originalID, postDocument.createJSONDocument());
		}
		if (storageResult) {
			logger.log(Level.INFO, "Stored post record");
		}
		else {
			logger.log(Level.SEVERE, "Unable to store post record: "+postDocument.toString());
		}	
		return storageResult;
	}	
	
	protected void processDocument() {
		Document doc = this.getCurrentDocument();
		
		//run the post-document annotations now.  We already have "text" defined so this should produce the concepts..
		Annotator.annotate(AnnotatorExecutionPoint.POST_DOCUMENT, doc);
		
		JSONObject object = new JSONObject(doc.getContentDataAsString());
		doc.updatesourceDocument(object);
		this.storePostInElasticSearch(doc, object);
		
		KafkaQueue.sendToQueue(doc.getStorageArea(), doc.getDomainInstanceName(), doc.getUUID(), doc.createJSONDocument());
		(new HDFSStorage()).store(doc.getStorageArea(), doc.getDomainInstanceName(), doc.getUUID(), doc.createJSONDocument());	
		
		logger.log(Level.FINEST, "Processed post record: "+object.toString());
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
		return "Places posts from Forum-based websites in JSON-based stores";
	}
	@Override
	public int getProcessingOrder() {
		return 0;
	}	
}
