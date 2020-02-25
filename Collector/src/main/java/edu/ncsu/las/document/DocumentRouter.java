package edu.ncsu.las.document;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.util.ApplicationConstants;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.VisitedPage;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.storage.StorageProcessor;

public class DocumentRouter {
	private static Logger logger =Logger.getLogger(Collector.class.getName());

	private java.util.UUID _jobHistoryID;

	private JobHistory _jobHistoryRecord;
	private Job _job;
	private Pattern _relevancyPattern;
	//private JSONObject _jobConfiguration;
	private String _currentDomainInstanceName;
	private SourceHandlerInterface _sourceHandler;
	
	public DocumentRouter(String currentDomainInstanceName, java.util.List<DocumentHandler> documentHandlers, JobHistory jobRecord, Job job) {
		_jobHistoryID = jobRecord.getJobHistoryID();
		_jobHistoryRecord = jobRecord;
		_currentDomainInstanceName = currentDomainInstanceName;
		_job = job;
		//_jobConfiguration = configuration;
	}
		
	public SourceHandlerInterface getSourceHandler() { return _sourceHandler; }
	public void setSourceHandler(SourceHandlerInterface handler) {_sourceHandler = handler; }	
	
	public void setRelevancyPattern(Pattern newRelevancyPattern) {
		_relevancyPattern = newRelevancyPattern;
	}
	
	private Job getJob() { return _job; }
	
	/**
	 * Relevancy is determined by either language it "limitToLanguage" has been set or by a relevancy pattern;
	 * @param d
	 * @return
	 */
	private boolean isRelevant(Document d) {
		
		if (this.getJob() != null && 
			this.getJob().getConfiguration().has("ignoreRelevancyForImages") && 
			this.getJob().getConfiguration().getBoolean("ignoreRelevancyForImages") &&
				d.getMimeType().contains("image") ) {
				return true;
			}
		
		String text = d.getExtractedTextFromTika();
		
		if (_relevancyPattern != null && !_relevancyPattern.matcher(text).find()) {
			logger.log(Level.FINE, "Regular expression relevancy check failed: " + _relevancyPattern.toString());
			return false;
		}
	
		boolean languageGood=true;
		// the directory watcher and other services do not have a job with configuration
		if (this.getJob() != null && this.getJob().getConfiguration().has("limitToLanguage")) {
			languageGood = false;
			String documentLanguage = d.getLanguage();
			
			JSONArray allowableLanguages = this.getJob().getConfiguration().getJSONArray("limitToLanguage");
			for (int i=0;i<allowableLanguages.length();i++) {
				String al = allowableLanguages.getString(i);
				if (documentLanguage.equals(al)) {
					languageGood = true;
					break;
				}
			}
		}
		
		return languageGood;
	}
		
	/**
	 * 
	 * @param currentDocument
	 * @param params   ignoreRecent  noSaveRaw  ignoreDuplicates
	 */
	public void processPage(Document currentDocument, String params) {	
		boolean relevant = this.isRelevant(currentDocument);
		
		_jobHistoryRecord.incrementNumPageVisited();
		_jobHistoryRecord.incrementTotalPageSize(currentDocument.getContentSize());
		if ( (_jobHistoryRecord.getNumPageVisited() % 100) == 0) {
			_jobHistoryRecord.updateJobStats();
		}
		
		
		
		String hashValue = currentDocument.getContentHash();
		if (hashValue == null) {
			logger.log(Level.SEVERE, "Unable to create hash, not processing page: "+currentDocument.getURL());
			return;
		}
		
		VisitedPage mostRecentPageMatch = VisitedPage.findMostRecentMatch(hashValue,currentDocument.getDomainInstanceName(), currentDocument.getStorageArea());
		if (mostRecentPageMatch != null && !params.contains("ignoreRecent") && !Configuration.getConfigurationPropertyAsBoolean(_currentDomainInstanceName, ConfigurationType.DOMAIN_DUPLICATE_TEXT)) { // We've already seen this page. Record that we visited and stop processing.
			String status = currentDocument.getURL().equals(mostRecentPageMatch.getURL()) ? ApplicationConstants.VISITED_PAGE_STATUS_UNCHANGED : ApplicationConstants.VISITED_PAGE_STATUS_OTHER_SOURCE;	 
					
			VisitedPage vp = new VisitedPage(_jobHistoryID, _jobHistoryRecord.getJobID(), currentDocument.getURL(), currentDocument.getMimeType(), mostRecentPageMatch.getStorageUUID(), hashValue, status, mostRecentPageMatch.getID(), VisitedPage.ElasticSearchResultCode.NOT_APPLICABLE.toString(),currentDocument.getDomainInstanceName(),currentDocument.getStorageArea().getLabel());	
			if (vp.create()) {	logger.log(Level.FINER, "VP record created: "+vp);			 }
			else {				logger.log(Level.SEVERE, "Unable to create VP record: "+vp); }
			return;
		}
		
		if (!relevant) {			
			VisitedPage vp = new VisitedPage(_jobHistoryID, _jobHistoryRecord.getJobID(), currentDocument.getURL(), currentDocument.getMimeType(), "not applicable", hashValue, ApplicationConstants.VISITED_PAGE_STATUS_IRRELEVANT,ApplicationConstants.UUID_DEFAULT, VisitedPage.ElasticSearchResultCode.NOT_APPLICABLE.toString(),currentDocument.getDomainInstanceName(),currentDocument.getStorageArea().getLabel());	
			if (vp.create()) {	logger.log(Level.FINER, "VP record created: "+vp);			 }
			else {				logger.log(Level.SEVERE, "Unable to create VP record: "+vp); }
			return;			
		}
		
		if (params.contains("noSaveRaw") == false) {
			JSONObject rawStorageMetaData = currentDocument.createMetaDataRecordForRawStorage();
			StorageProcessor.getTheStorageProcessor().saveRawData(currentDocument.getStorageArea(),_currentDomainInstanceName,currentDocument.getUUID(), currentDocument.getContentData(), rawStorageMetaData);
		}
		
		Annotator.annotate(AnnotatorExecutionPoint.PRE_DOCUMENT, currentDocument);
		// Send to the proper handler to let it take care of the data;
		DocumentHandler dh = DocumentHandler.getHandler(currentDocument.getMimeType(), currentDocument.getDomain());
		logger.log(Level.FINER, "routing to "+dh.getClass().getName());
		
		dh.process(currentDocument, this);
		
		Annotator.annotate(AnnotatorExecutionPoint.POST_DOCUMENT, currentDocument);
		
		//validate text content exists - use 
		currentDocument.setExtractedText(currentDocument.getExtractedTextFromTika(),false);
		
		boolean checkForDuplicateContent = true;
		if (params.contains("ignoreDuplicates") || Configuration.getConfigurationPropertyAsBoolean(_currentDomainInstanceName, ConfigurationType.DOMAIN_DUPLICATE_TEXT)) {
			checkForDuplicateContent = false;
		}
		
		// Note - this needs to come after the "process" call as that call may change how the text is extracted for certain mime types/domains
		FileStorageStatusCode jsonResult = StorageProcessor.getTheStorageProcessor().saveJSONData(currentDocument.getStorageArea(),_currentDomainInstanceName, checkForDuplicateContent, currentDocument.getUUID(), currentDocument.createJSONDocument(), currentDocument);

		VisitedPage vp = new VisitedPage(_jobHistoryID, _jobHistoryRecord.getJobID(), currentDocument.getURL(), currentDocument.getMimeType(), currentDocument.getUUID(), hashValue, ApplicationConstants.VISITED_PAGE_STATUS_NEW, ApplicationConstants.UUID_DEFAULT, VisitedPage.ElasticSearchResultCode.getCode(jsonResult),currentDocument.getDomainInstanceName(),currentDocument.getStorageArea().getLabel());	
		if (vp.create()) {	logger.log(Level.FINER, "VP record created: "+vp);			 }
		else {				logger.log(Level.SEVERE, "Unable to create VP record: "+vp); }
		
		currentDocument.markDocumentSaved();
		if (jsonResult != FileStorageStatusCode.ALREADY_PRESENT) {
			this.secondaryProcessing(currentDocument);
		}

	}
		

	/**
	 * 
	 * @param currentDocument
	 * @param sendToFullText
	 * @param markActivity
	 */
	public void processPage(Document currentDocument, boolean sendToFullText, boolean markActivity) {
		if (markActivity) {
			_jobHistoryRecord.incrementNumPageVisited();
			_jobHistoryRecord.incrementTotalPageSize(currentDocument.getContentData().length);
			if ( (_jobHistoryRecord.getNumPageVisited() % 100) == 0) {
				_jobHistoryRecord.updateJobStats();
			}
		}
		
		Annotator.annotate(AnnotatorExecutionPoint.PRE_DOCUMENT, currentDocument);
		DocumentHandler dh = DocumentHandler.getHandler(currentDocument.getMimeType(), currentDocument.getDomain());
		logger.log(Level.FINER, "subrouting to "+dh.getClass().getName());
		dh.process(currentDocument, this);
		Annotator.annotate(AnnotatorExecutionPoint.POST_DOCUMENT, currentDocument);
		
		FileStorageStatusCode saveResult = null;
		if (sendToFullText) {
			saveResult = StorageProcessor.getTheStorageProcessor().saveJSONData(currentDocument.getStorageArea(),_currentDomainInstanceName, true, currentDocument.getUUID(), currentDocument.createJSONDocument(), currentDocument);

			JSONObject rawStorageMetaData = currentDocument.createMetaDataRecordForRawStorage();
			StorageProcessor.getTheStorageProcessor().saveRawData(currentDocument.getStorageArea(),_currentDomainInstanceName,currentDocument.getUUID(), currentDocument.getContentData(), rawStorageMetaData);
			
			currentDocument.markDocumentSaved();
		}
		if (saveResult != FileStorageStatusCode.ALREADY_PRESENT && sendToFullText) {
			this.secondaryProcessing(currentDocument);
		}
	}

	/**
	 * SecondaryPorcessing is used for long-running annotations and will update the json document
	 * 
	 * @param currentDocument
	 * @param sendToFullText
	 */
	public void secondaryProcessing(Document currentDocument) {
		DocumentRouterSecondaryProcessor.getTheSecondaryProcessor().submitDocumentForSecondaryProcessing(currentDocument);
	}
}
