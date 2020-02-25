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
 * Process user records from a forum-based site
 * 
 *
 */
public class ForumUserHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.FORUM_USER } ;
	


	private boolean storeUserInElasticSearch(Document userDocument, JSONObject userObject) {
		String domain = userDocument.getDomainInstanceName();
		if (Configuration.storeJSONInElasticSearch(domain, userDocument.getStorageArea()) == false) {
			return true;
		}
				
		JSONObject userResult = ElasticSearchREST.retrieveDocumentByFieldSearch(domain, userDocument.getStorageArea(), "alternateID", userDocument.getAlternateID());
		
		boolean storageResult;
		if (userResult == null ) {
			storageResult = ElasticSearchREST.insertDocument(domain,userDocument.getStorageArea(), userDocument.getUUID(), userDocument.createJSONDocument());
		}
		else {
			if (userDocument.getDocumentMerger() != null) {
				JSONObject userRecord = userResult.getJSONObject("_source").getJSONObject("sourceDocument");
				JSONObject newUserDocument = userDocument.getDocumentMerger().mergeMemberRecords(userObject, userRecord);
				userDocument.updatesourceDocument(newUserDocument);
			}
			String originalID = userResult.getString("_id");
			storageResult = ElasticSearchREST.updateDocument(domain,userDocument.getStorageArea(), originalID, userDocument.createJSONDocument());
		}
		if (storageResult) {
			logger.log(Level.INFO, "Stored user record");
		}
		else {
			logger.log(Level.SEVERE, "Unable to store user record: "+userDocument.toString());
		}	
		return storageResult;
	}	
	
	protected void processDocument() {
		Document doc = this.getCurrentDocument();
		
		//run the post-document annotations now.  We already have "text" defined so this should produce the concepts..
		Annotator.annotate(AnnotatorExecutionPoint.POST_DOCUMENT, doc);
		
		JSONObject object = new JSONObject(doc.getContentDataAsString());
		doc.updatesourceDocument(object);
		this.storeUserInElasticSearch(doc, object);
		
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
		return "Places user records from forum-based websites in JSON-based stores";
	}
	@Override
	public int getProcessingOrder() {
		return 0;
	}	
}
