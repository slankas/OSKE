package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.JobCollector;
import edu.ncsu.las.document.Document;
import edu.ncsu.las.document.DocumentCreatorInterface;
import edu.ncsu.las.document.DocumentRouter;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.DomainDiscoveryExecution;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.storage.ElasticSearchDomainDiscoveryQuery;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.util.InternetUtilities;
import net.jodah.expiringmap.ExpiringMap;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;



/**
 * DomainDiscoveryHandler manages a user sessions with a specific search engine.
 * 
 */
public class DomainDiscoveryHandler extends AbstractHandler implements  SourceHandlerInterface, Runnable {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	
	public enum HandlerStatusType {
		
		NEW("new"),              // 
		CRAWLING("crawling"),    // 
		COMPLETE("complete"),    // 
		CANCELLING("cancelling"),    // 
		CANCELLED("cancelled");    // 
		
		private String _label;

		private HandlerStatusType(String label) {
			_label = label;
		}
		
		public String toString() { return _label; }
		
		public static HandlerStatusType getEnum(String label) {
			return HandlerStatusType.valueOf(label.toUpperCase());
		}
	}

	
	static class DomainConfiguration {
		
		private String _domainName;
		
		private JobHistory _jobRecord;
		
		private Job _job;
		
		
		private DocumentRouter _documentRouter;
		
		
		public DomainConfiguration (String domainName, JobHistory jobHistoryRecord, Job job) {
			_domainName   = domainName;
			_jobRecord    = jobHistoryRecord;
			_job          = job;
			
			_documentRouter = new DocumentRouter(job.getDomainInstanceName(), Collector.getTheCollecter().getDocumentHandlers(),jobHistoryRecord,job);
		}
		
		public String getDomainName() { return _domainName; }
		public JobHistory getJobHistoryRecord() { return _jobRecord; }
		public Job getJob() { return _job; }

		public DocumentRouter getDocumentRouter() {
			return _documentRouter;
		}
	}
	
	
	
	static java.util.HashMap<String, DomainConfiguration> _handlerConfiguration = new java.util.HashMap<String,DomainConfiguration>();
	
	
	
	
	
	// stores user domain discovery sessions
	private static Map<String,DomainDiscoveryHandler> sessionDocumentMap = ExpiringMap.builder()
			  .expiration(60, TimeUnit.MINUTES)
			  .build();
	
	private static String createMapKey(String domain, UUID sessionID, int executionNumber) {
		return domain+":"+sessionID.toString()+"-"+executionNumber;
	}
	
	public static DomainDiscoveryHandler getDomainDiscoveryHandler(String domain,UUID sessionID, int executionNumber) {
		return sessionDocumentMap.get(createMapKey(domain,sessionID, executionNumber));
	}
	
	/**
	 * Gets the appropriate ddh.  If not found and create is true, will re-create the DDH from ElasticSearch.
	 * 
	 * @param domain
	 * @param sessionID
	 * @param executionNumber
	 * @param create
	 * @return
	 */
	public static DomainDiscoveryHandler getDomainDiscoveryHandler(String domain, UUID sessionID, int executionNumber, boolean create) {
		DomainDiscoveryHandler ddh = sessionDocumentMap.get(createMapKey(domain,sessionID, executionNumber));
		if (ddh != null || create == false) {
			return ddh;
		}
		
		List<JSONObject> documents = ElasticSearchDomainDiscoveryQuery.getDocumentsForSessionAndExecution(domain,sessionID.toString(), executionNumber);
		ddh = new DomainDiscoveryHandler();
		ddh._sessionID = sessionID;
		ddh._executionNumber = executionNumber;
		ddh._domain = domain;
		
		ddh._documents = new java.util.Vector<Document>();
		ddh._documentsByURL  = new java.util.Hashtable<String,Document>();
		ddh._documentsByUUID = new java.util.Hashtable<UUID,Document>();
		
		ArrayList<SearchRecord> discoveryRecords = new ArrayList<SearchRecord>();
		
		int position = 0;
		for (JSONObject docObject: documents) {
			Document doc = Document.createFromJSON(domain,docObject);
			ddh._documents.add(doc);
			ddh._documentsByURL.put(doc.getURL(),doc);
			ddh._documentsByUUID.put(UUID.fromString(doc.getUUID()), doc);
			
			try {
				JSONObject domainDiscovery = docObject.getJSONObject("domainDiscovery");
				SearchRecord sr = new SearchRecord(domainDiscovery.getString("title"), domainDiscovery.getString("url"), domainDiscovery.optString("description",""), ++position,"history"); //TODO: need to store source in elasticSearch records
				sr.setStatus("crawled");
				sr.setDocument(docObject);
				sr.setDocumentUUID(UUID.fromString(doc.getUUID()));
			
				discoveryRecords.add(sr);
			}
			catch (Throwable t) {
				srcLogger.log(Level.WARNING, "unable to add document to discovery records",t);
			}

		}
		ddh.setHandlerStatus(HandlerStatusType.COMPLETE);
		ddh.setDiscoveryRecords(discoveryRecords);
		//private DomainDiscoveryExecution _domainDiscoveryExecution;

		//private Map<String, SearchRecord> _discoveryRecordsByURL;

		sessionDocumentMap.put(createMapKey(domain,sessionID, executionNumber),ddh);
		
		return ddh;
	}	
	
	/**
	 * Returns the most recent DomainDiscoveryHandler (e.g., has a smaller execution number);
	 * 
	 * @param domain
	 * @param sessionID
	 * @param executionNumber
	 * @return
	 */
	public static DomainDiscoveryHandler getDomainDiscoveryHandlerPrior(String domain, UUID sessionID, int executionNumber) {
		DomainDiscoveryHandler result = null;
		
		while (executionNumber > 0 && result == null) {
			executionNumber--;
			result = DomainDiscoveryHandler.getDomainDiscoveryHandler(domain, sessionID, executionNumber);
		}
		
		return result;
	}
	
	/**
	 * This method is called to replace the current version of the document that cached in memory
	 * with the latest version from ElasticSearch.
	 * This will iterate through all executions (loaded from the database) that are currently in memory.
	 * 
	 * @param documentUUID
	 */
	public static void refreshDocumentInCache(String domain, UUID sessionID, UUID documentUUID) {	
		JSONObject result = ElasticSearchREST.retrieveDocument(domain,FileStorageAreaType.SANDBOX, documentUUID.toString());
		if (result != null) {
			Document doc = Document.createFromJSON(domain,result);
			
			java.util.List<DomainDiscoveryExecution> executions = DomainDiscoveryExecution.getAllExectuionsForSession(sessionID);
			
			for (DomainDiscoveryExecution dde: executions) {
				DomainDiscoveryHandler ddh = DomainDiscoveryHandler.getDomainDiscoveryHandler(domain, sessionID, dde.getExecutionNumber(), false);
				if (ddh != null) {
					ddh.replaceDocumentInCache(doc);
				}
			}
			
			srcLogger.log(Level.INFO,"Updated document in discovery handler cache: "+documentUUID.toString());
		}
		else {
			srcLogger.log(Level.INFO,"Unable to update document in discovery handler cache - not found: "+documentUUID.toString());
		}	
	}			
				


	private ExecutorService createSearchPool(boolean usesProxy) {
		if (usesProxy) {
			return  Executors.newFixedThreadPool(5); 
		}
		else {
			return Executors.newFixedThreadPool(Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.COLLECTOR_SEARCH_POOLSIZE)); 
		}
	}
	
	private ExecutorService _searchPool = null;
			
	
	private boolean documentCrawlComplete = false;
	private HandlerStatusType _handlerStatus = HandlerStatusType.NEW;
		
	private AtomicInteger numPagesCrawledSuccessful = new AtomicInteger(0);
	private AtomicInteger numPagesCrawledFailed  = new AtomicInteger(0);;
	private int numPagesToCrawl;
	private Instant startTime;
	private Instant endTime;
	
	//Note: need to used synchronized(legacy) versions of the collections becuase of threading/concurrency
	
	private java.util.Hashtable<String,String> _errors;
	
	private List<SearchRecord> _discoveryRecords;
	private java.util.Vector<Document> _documents;                  //this needs to a synchronized collection, so use legacy Vector
	
	private Document[] _documentsBySearchPosition;                  //used when retreiving docs to make sure they go in the right position in the _documents vector. Index start 1 to length, not 0 to length-1
	
	private java.util.Hashtable<String,Document> _documentsByURL;
	private java.util.Hashtable<UUID,Document> _documentsByUUID;
	
	private String _domain;
	private UUID _sessionID;
	private int  _executionNumber;
	private DomainDiscoveryExecution _domainDiscoveryExecution;

	private Map<String, SearchRecord> _discoveryRecordsByURL;

	private String translateTargetLanguage = "none";
	
	public DomainDiscoveryHandler() { };
	
	public DomainDiscoveryHandler(String domain, UUID sessionID, int executionNumber){
		sessionDocumentMap.put(createMapKey(domain,sessionID, executionNumber),this);   
		
		_domain    = domain;
		_sessionID = sessionID;
		_executionNumber = executionNumber;
	}
		
	public List<SearchRecord> getDiscoveryRecords() {
		return _discoveryRecords;
	}
	
	/**
	 * By explicitly setting the records (rather than calling generateDiscoveryRecords()),
	 * users of this class can explicitly set the URLs that they want to crawl and include
	 * as part of this iteration.
	 * 
	 * @param records List of DomainDiscoveryRecords to crawl.  URL must be set.  Others can be blank/null
	 */
	public void setDiscoveryRecords(List<SearchRecord> records) {
		_discoveryRecords = records;
		
		//_discoveryRecordsByURL = records.stream().collect(Collectors.toMap(x ->x.getUrl(), x -> x));  // convert to map keyed by URLs
		
		_discoveryRecordsByURL = new java.util.HashMap<String,SearchRecord>();
		
		if (records != null) {
			for (SearchRecord ddr: records) {
				_discoveryRecordsByURL.put(ddr.getUrl(),ddr);
			}
		}
	}
	
	public String getTranslateTargetLanguage() {
		return translateTargetLanguage;
	}

	public void setTranslateTargetLanguage(String translateTargetLanguage) {
		this.translateTargetLanguage = translateTargetLanguage;
		srcLogger.log(Level.FINEST,"setTranslateTargetLanguage: "+translateTargetLanguage);
	}

	public List<Document> getDocuments() {
		return _documents;
	}
	
	private String provideDefault(Object value, String defaultValue) {
		if (value != null) {return value.toString();}
		else { return defaultValue; }
	}
	
	/** 
	 * returns a summary listing of the retrieved documents
	 * provides url, uuid, size, textLength, and title
	 * 
	 * @return
	 */
	public JSONArray getDocumentList() {
		JSONArray result = new JSONArray();
		
		for (Document d: _documents) {
			JSONObject jsonDoc = new JSONObject().put("url", d.getURL())
					                             .put("uuid", d.getUUID())
					                             .put("size", d.getContentData())
												 .put("textLength", d.getExtractedText().length())
					                             .put("title", this.provideDefault(d.getAnnotation("html_title"), d.getURL()));
			
			result.put(jsonDoc);
		}
		
		return result;
	}
	
	public edu.ncsu.las.document.Document getDocument(String url) {
		return _documentsByURL.get(url);
	}

	public edu.ncsu.las.document.Document getDocument(UUID uuid) {
		return _documentsByUUID.get(uuid);
	}

	public Instant getStartTime() {
		return startTime;
	}
	
	public Instant getEndTime() {
		return endTime;
	}

	
	private int findDocumentBySearchPosition(UUID documentUUID) {
		if (_documentsBySearchPosition != null) {
			for (int i=1; i<=_documentsBySearchPosition.length; i++ ) {   //indexing is from 1 to length.
				if (_documentsBySearchPosition[i] != null && _documentsBySearchPosition[i].getUUID().equals(documentUUID.toString())) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private int findDocumentInVector(UUID documentUUID) {
		if (_documents != null) {
			for (int i=0; i<_documents.size(); i++ ) {
				if (_documents.get(i) != null && _documents.get(i).getUUID().equals(documentUUID.toString())) {
					return i;
				}
			}
		}
		return -1;
	}	
		
	private void replaceDocumentInCache(Document doc) {
		UUID documentUUID = UUID.fromString(doc.getUUID());
		_documentsByURL.put(doc.getURL(), doc);
		_documentsByUUID.put(documentUUID, doc);
		
		int searchPosition = this.findDocumentBySearchPosition(documentUUID);
		if (searchPosition > -1) { _documentsBySearchPosition[searchPosition] = doc; }
		
		int vectorPosition = this.findDocumentInVector(documentUUID);
		if (vectorPosition > -1) { _documents.set(vectorPosition, doc); }
	}	
	
	
	
	
	public edu.ncsu.las.document.Document processURL(SearchRecord ddr, String url, String userID) throws Exception {

		long startProcessingTime = System.currentTimeMillis();
		ddr.setStartDownloadMillis(startProcessingTime);

		edu.ncsu.las.document.Document doc;
		if (ddr.getDocument() != null) {
			ddr.setFinishDownloadMillis(startProcessingTime);
			if (MimeType.isExtendedType( ddr.getMimeType())) {
				DocumentCreatorInterface dci = MimeType.getExtendedTypeDocumentCreator(ddr.getMimeType());
				doc = dci.createDocument(ddr, url, _domain, this.getJobHistory());
			}
			else {
				srcLogger.log(Level.SEVERE, "Unknown mimeType in discovery handler: "+ddr.getMimeType()+ ", source document: "+ddr.getDocument());
				return null;
			}
		}
		else if (ddr.getUploadedData() != null) {
			InternetUtilities.HttpContent hc = InternetUtilities.createHttpContent(ddr.getUploadedData(), ddr.getUploadedContentType());
			doc = new edu.ncsu.las.document.Document(ddr.getDocumentUUID(),hc,"" ,new JSONObject(), new JSONObject().put("name", "domainDiscovery"),_domain, this.getJobHistory());
			ddr.setFinishDownloadMillis(System.currentTimeMillis());
			
		}
		else {
			doc = new edu.ncsu.las.document.Document(ddr.getDocumentUUID(), url,new JSONObject(), new JSONObject().put("name", "domainDiscovery"),_domain, this.getJobHistory());
			ddr.setFinishDownloadMillis(System.currentTimeMillis());
		}
		doc.setStorageArea(FileStorageAreaType.SANDBOX);
		
		//set translated language
		doc.setTranslateTargetLanguage(translateTargetLanguage);
		
		JSONObject jo = new JSONObject().put("url", ddr.getUrl() )
				                        .put("description", ddr.getDescription() )
				                        .put("title", ddr.getName())
				                        //Change on 20170720 to not default a value for relevant
				                        //.put("relevant", true)
				                        .put("relevantUser", userID)
				                        .put("retrievals", new JSONArray().put(new JSONObject().put("sessionID", this._sessionID.toString())
				                        		                                               .put("executionNumber", this._executionNumber)
				                        		                                               .put("uuid", ddr.getDocumentUUID().toString())
				                        		                                               .put("userID", userID)
				                        		                                               .put("searchPosition", ddr.getResultPosition())));
		if (ddr.getNativeName() != null) { jo.put("nativeTitle", ddr.getNativeName()); }
		if (ddr.getNativeDescription() != null) { jo.put("nativeDescription", ddr.getNativeDescription()); }
		
		doc.addAnnotation("domainDiscovery", jo);
		
		if (ddr.getDocument() != null) {
			_handlerConfiguration.get(_domain).getDocumentRouter().processPage(doc,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
		}
		else {
			_handlerConfiguration.get(_domain).getDocumentRouter().processPage(doc,"ignoreRecent,ignoreDuplicates");
		}
		ddr.setFinishProcessingMillis(System.currentTimeMillis());		
		
		JSONObject processingMetrics = new JSONObject().put("waitingTimeMillis", ddr.getWaitingTimeMillis())
				                                       .put("downloadTimeMillis", ddr.getDownloadTimeMillis())
				                                       .put("processingTimeMillis", ddr.getProcessingTimeMillis());
		jo.put("processingMetrics", processingMetrics);
		String documentID = doc.getUUID();
		ElasticSearchDomainDiscoveryQuery.addProcessingMetricsToDocument(_domain,processingMetrics, documentID);
		
		return doc; 
	}

	private edu.ncsu.las.document.Document getDocumentIncludingPriorExecution(String url) {
		int currentExectionNumber = this._executionNumber;
		Document result = null;
		
		while (currentExectionNumber >= 0 && result == null) {
			DomainDiscoveryHandler pastHandler =  DomainDiscoveryHandler.getDomainDiscoveryHandler(_domain,_sessionID, currentExectionNumber);
			if (pastHandler != null && pastHandler._documentsByURL != null) {
					result = pastHandler._documentsByURL.get(url);
			}
			currentExectionNumber--;
		}
		if (result == null) {
			List<JSONObject> docs = ElasticSearchDomainDiscoveryQuery.getDocumentsForURL(_domain,url);
			for (JSONObject docObject: docs) {
				JSONArray retrievals = new JSONArray();
				try { retrievals = docObject.getJSONObject("domainDiscovery").getJSONArray("retrievals"); }
				catch (org.json.JSONException e) { srcLogger.log(Level.INFO, "Unable to locate retrieval object, assumming none exist, use the empty array");}
				String sessionID = this._sessionID.toString();
				for (int i=0;i< retrievals.length();i++) {
					JSONObject retrieval = retrievals.getJSONObject(i);
					if (retrieval.getString("sessionID").equals(sessionID)) {
						result = Document.createFromJSON(_domain,docObject);
					}
				}
				if (result != null) { break; }
			}
		}
		
		
		return result;
	}
	
	/*
	private edu.ncsu.las.document.Document getDocumentIncludingPriorExecution(UUID uuid) {
		int currentExectionNumber = this._executionNumber;
		Document result = null;
		
		while (currentExectionNumber >= 0 && result == null) {
			DomainDiscoveryHandler pastHandler =  DomainDiscoveryHandler.getDomainDiscoveryHandler(_sessionID, currentExectionNumber);
			if (pastHandler != null) {
					result = pastHandler.getDocument(uuid);
			}
			currentExectionNumber--;
		}
		
		return result;
	}	
	*/
	

	private static class MyFuture {
		//Future<Object> future;
		SearchRecord  ddr;
		
		public MyFuture (Future<Object> f, SearchRecord  r) {
			//future = f;
			ddr    = r;
		}
	}
	
	public void crawlDocuments(DomainDiscoveryExecution dde) {
		if (this.getDiscoveryRecords() == null) {
			throw new IllegalStateException("generateRecords() or setDiscoveryRecords() has not yet been called");
		}
		
		documentCrawlComplete = false;
		this.setHandlerStatus(HandlerStatusType.CRAWLING);
		
		numPagesToCrawl = this.getDiscoveryRecords() .size();
		numPagesCrawledSuccessful = new AtomicInteger();
		numPagesCrawledFailed     = new AtomicInteger();
		
		_documents = new java.util.Vector<edu.ncsu.las.document.Document>();
		_documentsByURL = new java.util.Hashtable<String, Document>();
		_documentsByUUID = new java.util.Hashtable<UUID, Document>();
		_errors = new java.util.Hashtable<String,String>();
		startTime = Instant.now();
		_domainDiscoveryExecution = dde;
		
		Thread t = new Thread(this);
		t.start();
	}
	
	private synchronized void checkForCompletion() {
		/*System.out.println("======= Status check");
		System.out.println("Total: "+numPagesToCrawl +"    "+documentCrawlComplete);
		System.out.println("Successful: "+numPagesCrawledSuccessful.get());
		System.out.println("Failed: "+numPagesCrawledFailed.get());
		System.out.println("=======");*/
		
		if (documentCrawlComplete == false && numPagesCrawledSuccessful.get() + numPagesCrawledFailed.get() >= numPagesToCrawl) { 
			documentCrawlComplete = true; 
			if (this.getHandlerStatus() == HandlerStatusType.CANCELLING) {
				this.setHandlerStatus(HandlerStatusType.CANCELLED); 
			}
			else {
				this.setHandlerStatus(HandlerStatusType.COMPLETE); 
			}
			
			endTime = Instant.now();
			if (_domainDiscoveryExecution != null) {
				_domainDiscoveryExecution.markComplete(endTime);
			}
		}		
	}

	@Override
	public void run() {	
		ArrayList<MyFuture> futureResults = new ArrayList<MyFuture>();  // Need to create and submit everything first.  Then start waiting...
		long initialTimeMillis = System.currentTimeMillis();
		_documentsBySearchPosition = new Document[this.getDiscoveryRecords().size()+1];  // we actually start at 1, so offset it ...
		
		

		boolean usesProxy = false;
		for (SearchRecord ddr: this.getDiscoveryRecords() ) {
			if (ddr.getUrl().contains(".onion")) {
				usesProxy = true; 
				break;
			}
		}
		long timeout = Math.max(2*this.getDiscoveryRecords().size(),120);
		if (usesProxy) {
			timeout = 240 * this.getDiscoveryRecords().size();
		}
		
		ExecutorService searchPool = this.createSearchPool(usesProxy);
		Domain currentDomain = Collector.getTheCollecter().getDomain(_domain);
		for (SearchRecord ddr: this.getDiscoveryRecords() ) {
			// Need to check if any of the URLs have blocked top level domains ....
			try {
				java.net.URL test = new java.net.URL(ddr.getUrl());
				if (currentDomain.isTopLevelDomainBlocked(test.getHost())) {
					ddr.setErrorMessage("Top level domain blocked from access");
					ddr.setStatus("error");
					numPagesCrawledFailed.incrementAndGet();
					continue;
				}
			}
			catch (MalformedURLException mue) {
				ddr.setErrorMessage("Invalid URL");
				ddr.setStatus("error");
				numPagesCrawledFailed.incrementAndGet();
				continue;				
			}
			final Future<Object> f = searchPool.submit( new Runnable() {
				@Override
				public void run() {
					
					srcLogger.log(Level.FINER, "processing: "+ ddr.getUrl());
					ddr.setStatus("crawling");
					//_handlerConfiguration.get(_domain).markActivity();  //TODO: FIX ME...
					try {
						if (DomainDiscoveryHandler.this.getHandlerStatus() == HandlerStatusType.CANCELLING) { throw new Exception("crawl cancelled"); }
						edu.ncsu.las.document.Document doc = DomainDiscoveryHandler.this.getDocumentIncludingPriorExecution(ddr.getUrl());
						
						JSONObject discoveryObject = null;
						
						if (doc == null && ddr.getDocument() != null) { // get the document from the searchRecord - holdings search does this.  Could also occur if the search brings back the full contents for other handlers
							doc = Document.createFromJSON(_domain,ddr.getDocument());
							if (doc.hasAnnotation("domainDiscovery") == false || ((JSONObject) doc.getAnnotation("domainDiscovery")).length() == 0 ) {
								discoveryObject = new JSONObject().put("url", ddr.getUrl() )
				                        .put("description", ddr.getDescription() )
				                        .put("title", ddr.getName())
				                        //Change on 20170720 to not default a value for relevant
				                        //.put("relevant", true)
				                        .put("relevantUser", _domainDiscoveryExecution.getUserID())
				                        .put("retrievals", new JSONArray());
								
								if (ddr.getNativeName() != null) { discoveryObject.put("nativeTitle", ddr.getNativeName()); }
								if (ddr.getNativeDescription() != null) { discoveryObject.put("nativeDescription", ddr.getNativeDescription()); }
								
								doc.addAnnotation("domainDiscovery", discoveryObject);
							}
							else {
								discoveryObject = (JSONObject) doc.getAnnotation("domainDiscovery");
							}
							
						}
						if (doc == null) { 
							doc = DomainDiscoveryHandler.this.processURL(ddr,ddr.getUrl(),DomainDiscoveryHandler.this._domainDiscoveryExecution.getUserID()); 
						}
						else { // need to mark that we are re-using a record in ElasticStore
							long startProcessingTime = System.currentTimeMillis();
							ddr.setStartDownloadMillis(startProcessingTime);
							ddr.setFinishDownloadMillis(startProcessingTime);
							ddr.setFinishProcessingMillis(startProcessingTime);
							
							JSONObject retrievalObject = new JSONObject().put("sessionID", DomainDiscoveryHandler.this._sessionID.toString())
									                                     .put("userID", DomainDiscoveryHandler.this._domainDiscoveryExecution.getUserID())
									                                     .put("executionNumber", DomainDiscoveryHandler.this._executionNumber)
									                                     .put("uuid", ddr.getDocumentUUID().toString())
									                                     .put("searchPosition", ddr.getResultPosition());
							String documentID = doc.getUUID();
							ElasticSearchDomainDiscoveryQuery.addRetrievalToDocument(_domain,retrievalObject, documentID,discoveryObject);
						}
						
						if (ddr.getUrl().equals(ddr.getName()) && doc.getAnnotation("html_title") != null) {
							ddr.setName(doc.getAnnotation("html_title").toString());
						}
						if (ddr.getResultPosition() <0 || ddr.getResultPosition() >= _documentsBySearchPosition.length) {
							_documents.addElement(doc);
						}
						else {
							_documentsBySearchPosition[ddr.getResultPosition()] = doc;
						}
						_documentsByURL.put(ddr.getUrl(),doc);
						_documentsByUUID.put(ddr.getDocumentUUID(),doc);
						ddr.setStatus("crawled");
						numPagesCrawledSuccessful.incrementAndGet();
						//System.out.println("MYST\t"+ddr.getResultPosition()+"\tPROCESSED");
					}
					catch (Throwable e) {
						//System.out.println("MYST\t"+ddr.getResultPosition()+"\tEXCEPTION\t"+e);
						srcLogger.log(Level.SEVERE, "Unable to process: "+ddr.getUrl(), e);
						_errors.put(ddr.getUrl(),e.toString());
						ddr.setStatus("error");
						ddr.setErrorMessage(e.toString());
						numPagesCrawledFailed.incrementAndGet();
					}
					DomainDiscoveryHandler.this.checkForCompletion();
				}
			}, new Object()
			);
			/*
			if (usesProxy) {
				try {
					f.get(20, TimeUnit.SECONDS);
				}
				catch (Exception ie) {
					; // nothing to do.  overall handling below will capture the error and report
				}
			}
			*/
			
			MyFuture mf = new MyFuture(f,ddr);
			futureResults.add(mf);
		}
		
		try {
			searchPool.shutdown();
			searchPool.awaitTermination(timeout, TimeUnit.SECONDS);
			searchPool.shutdownNow(); //kill anything still running
		}
		catch (Exception ex) {
			srcLogger.log(Level.SEVERE, "Unable to process - "+timeout+" second timeout: "+ex);
		}
		for (MyFuture mf: futureResults) {
			if (mf.ddr != null) {
				if (mf.ddr.getStatus().equals("crawled")) {continue;}
				
				if (mf.ddr.getUrl() != null) {
					srcLogger.log(Level.SEVERE, "Unable to process - "+timeout+" second timeout: "+mf.ddr.getUrl());
					_errors.put(mf.ddr.getUrl(), timeout+" second time out");
					//System.out.println("MYST\t"+mf.ddr.getResultPosition()+"\tTIMEOUT");
				}
				mf.ddr.setStatus("error");
			}
			numPagesCrawledFailed.incrementAndGet();
			DomainDiscoveryHandler.this.checkForCompletion();		
		}
		searchPool = null;
		
		for (Document d: _documentsBySearchPosition) {
			if (d != null) {
				_documents.addElement(d);
			}
		}
		/*
		for (MyFuture mf: futureResults) {
			try {
				mf.future.get(60, TimeUnit.SECONDS);
			}
			catch (Exception ex) {
				if (mf.ddr != null) {
					if (mf.ddr.getUrl() != null) {
						srcLogger.log(Level.SEVERE, "Unable to process - 60 second timeout: "+mf.ddr.getUrl()+", "+ex);
						_errors.put(mf.ddr.getUrl(), "60 second time out");
					}
					else {
						srcLogger.log(Level.SEVERE, "URL is null: "+ex);
					}
					mf.ddr.setStatus("error");
				}
				numPagesCrawledFailed.incrementAndGet();
				DomainDiscoveryHandler.this.checkForCompletion();
			}
		}
		*/
		
	}
	
	public  int getCrawledPageCount() {
		return numPagesCrawledSuccessful.get() + numPagesCrawledFailed.get();
	}
	
	public int getNumPagesCrawledSuccessfully() {
		return numPagesCrawledSuccessful.get();
	}
	
	public int getNumPageCrawledFailed() {
		return numPagesCrawledFailed.get();
	}

	public int getNumberOfPagesToCrawl() {
		return numPagesToCrawl;
	}

	/**
	 * What is the current status of this handler?
	 * 
	 * @return
	 */
	public synchronized HandlerStatusType getHandlerStatus() {	
		return _handlerStatus;
	}
	
	
	public synchronized void setHandlerStatus(HandlerStatusType newStatus) {
		if (_handlerStatus == HandlerStatusType.COMPLETE || _handlerStatus == HandlerStatusType.CANCELLED) {
			return;
		}
		
		_handlerStatus = newStatus; //TODO - need more logic around this!!!
	}
	
	
	/**
	 * Gets a list of the errored page crawls.  Indexed by URL.
	 * 
	 * @return
	 */
	public java.util.Map<String,String> getErroredPages() {
		return _errors;
	}
	
	public JSONArray getErrorPagesAsJSON() {
		JSONArray result = new JSONArray();
		
		for (Map.Entry<String, String> entry: _errors.entrySet()) {
			JSONObject jo = new JSONObject().put("url",entry.getKey())
					                        .put("error", entry.getValue());
			result.put(jo);
		}
		
		return result;
	}

	public List<String> getURLsNotReturned() {
		ArrayList<String> result = new ArrayList<String>();
		
		for (SearchRecord ddr: this.getDiscoveryRecords() ) {
			String url = ddr.getUrl();
			if (_documentsByURL.containsKey(url) == false && _errors.containsKey(url) == false) {
				result.add(url);
			}
		}
		return result;
		
	}
	
	
	//
	// Methods for SourceHandlerInterface
	//
	
	/** 
	 * Used to setup a SourceHandlerInterface.  By having this separate from a constructor, it's easier to create
	 * the initial object.
	 * 
	 * @param collector
	 * @param jobRecord
	 * @param job
	 */
	public void initializeInteractiveService(JobCollector collector, JobHistory jobRecord, Job job) {
		_domain = job.getDomainInstanceName();		
		DomainConfiguration config = new DomainConfiguration(_domain, jobRecord, job);
		_handlerConfiguration.put(_domain, config);
	}
	

	/**
	 * Called to actually execute the source handler to process data.
	 * 
	 * This should create an internal Runnable.run() and then launch a new thread to do the processing. 
	 */
	public void process() {
		throw new IllegalAccessError("interactive-based services utilize custom methods for processing");
	}
	
	/**
	 * Default configuration for the Source Handler
	 * 
	 * @return
	 */
	public JSONObject getHandlerDefaultConfiguration() {
		return new JSONObject();
	}
	
	public String getSourceHandlerName() {
		return "DomainDiscoveryHandler";
	}
	
	public String getSourceHandlerDisplayName() {
		return "Domain Discovery Handler";
	}
	
	public String getDescription() {
		return "Interactive source handler to allow users and systems to place search queries.  This handler is primarily used to support the discovery sandbox capabilities and cannot be directly used as a source handler for a job.  External applications can utilize this functionality through defined APIs.";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.NOT_APPLICABLE;
	}		
	
	/**
	 * 
	 * @return
	 */
	@Override
	public String getSampleConfiguration() {
		return "{}";
	}
	
	/**
	 * Maintains a key-value listing of of configuration parameters and their settings.
	 * 
	 * @return
	 */
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return new java.util.TreeMap<String,SourceParameter>();
	}
	
	/**
	 * Used to stop a source handler from continued execution.
	 * 
	 * @return
	 */
	@Override 
	public boolean stop() {
		_searchPool.shutdown();
		return true;
	}
	
	/**
	 * Used to help kill an inactive process...
	 * 
	 */
	@Override
	public void forceShutdown() {
		_searchPool.shutdownNow();
	}
	
	/**
	 * Returns true if this source Handler is interactive with an outside system/agent/user
	 * 
	 * @return
	 */
	@Override
	public boolean isInteractive() {
		return true;
	}	
	
	/**
	 * should never be called.  throw error
	 */
	@Override
	public List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		throw new IllegalStateException("validate configuration called on directory watcher");
	}	

	/**
	 * DomainDiscoveryHandler really doesn't have any configuration / limits as no jobs should be tied to this.
	 * throws illegalSTateException as this should not be called
	 */
	public java.util.List<String> validateInstantCount(Job job) {
		throw new IllegalStateException("validate instant called on directory watcher");
	}
	
	
	public DomainDiscoveryExecution getDomainDiscoveryExecution() {
		return _domainDiscoveryExecution;
	}


	/**
	 * Compare the search records in the current object against those in a prior iteration, setting the relative position value
	 * 
	 * @param ddhPrior
	 */
	public void compareDiscoveryRecordPositions(DomainDiscoveryHandler ddhPrior) {
		if (ddhPrior == null || ddhPrior._discoveryRecordsByURL == null) {return;}

		for (SearchRecord ddr: _discoveryRecords) {
			SearchRecord ddrPrior = ddhPrior._discoveryRecordsByURL.get(ddr.getUrl());
			if (ddrPrior == null) {
				ddr.setPositionRelativeToPriorExecution(Integer.MAX_VALUE);
			}
			else {
				int value = ddrPrior.getResultPosition() - ddr.getResultPosition();
				ddr.setPositionRelativeToPriorExecution(value);
			}
		}
		
	}

	/**
	 * Returns a list of discovery records contained in the prior execution, but not in this execution
	 * 
	 * @param ddhPrior.  if null, returns an empty list
	 * 
	 * @return
	 */
	public List<SearchRecord> getDiscoveryRecordsDropped(DomainDiscoveryHandler ddhPrior) {
		java.util.ArrayList<SearchRecord> result = new java.util.ArrayList<SearchRecord>();
		
		if (ddhPrior == null || ddhPrior.getDiscoveryRecords() == null) {
			return result;
		}
		
		for (SearchRecord ddr: ddhPrior.getDiscoveryRecords()) {
			if (this._discoveryRecordsByURL.get(ddr.getUrl()) == null) {
				result.add(ddr);
			}
		}
		
		return result;
	}

}
