package edu.ncsu.las.document;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.FileUtilities;
import edu.ncsu.las.util.HTMLUtilities;
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;
import edu.ncsu.las.util.json.JSONUtilities;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Represents a document from a Source Handler.  This wrapper is then used process that content through 
 * a series of document handlers.
 * 
 *
 */
public class Document {
	private static Logger logger =Logger.getLogger(Collector.class.getName());
	
	private String _type;   //web, file, etc..
	private String _mimeType;
	private String _url;
	private String _domain;
	private int    _crawlDepth;
	private byte[] _contentData;
	private String _extractedTextFromTika;
	private org.apache.http.Header[] _httpHeaders; 
	
	private String _referrer;
	
	private String _charset = StandardCharsets.UTF_8.name();  // character set.  Defaults to UTF-8
	
	private String _sha256Hash;
	private String _uuid = edu.ncsu.las.util.UUID.createTimeUUID().toString();
	private String _sourceUUID = null; //only set for "derived" files - ie, expanded zip/tar files
	
	private String _alternateID = "undefined";   // used by Forum and other such handlers to have an "ID" that can be regenerate based upon the attributes of crawling...
	
	private HashMap<String, Object> _annotations = new HashMap<String,Object>();

	private String _extractedText;
	
	private JSONObject _relatedJobConfiguration;
	private JSONObject _jobSummaryInformation;
	
	
	private JSONObject _sourceDocument; // if this document was created from a JSON Object, this value contains the data.
	
	private boolean _derived = false;  // may not be needed, or can be calculated from the sourceUUID field != null
	
	private Set<String> _outgoingURLs = new HashSet<String>();
	
	private JSONObject _originalJSONObject = null;
	
	/**
	 * what language is the document in?  Follows 2-character abbreviation from ISO-639-1.  Calculated by Tika
	 */
	private String _language = null;
	
	/** when was the document last modified?  Generally this is only set for Documents created from files.  Will be set to -1 if undefined */
	private long _lastModificationTimeEpochMillis = -1;
	
	
	private FileStorageAreaType _storageArea = FileStorageAreaType.REGULAR;
	
	private String _domainInstanceName;
	
	private JSONDocumentMerger _documentMerger;
	
	private java.io.File _location;
	
	/** references a the current job execution underyway */
	private JobHistory _jobHistory;
	
	/**
	 * Keeps track of annotations that have not yet been saved.  This will allow use to create an update object with just these records
	 */
	private Set<String> _unsavedAnnotations = new HashSet<String>(); 
	
	private String _translateTargetLanguage = null;
	
	
	private Document() {
	}
	
	/**
	 * Creates a document wrapper around content from crawler4j.  
	 * 
	 * @param page - crawled page from crawler4j.  
	 * @param jobConfiguration      - this is the json configuration object for a particular job
	 * @param jobSummaryInformation - comes form Job.getSummary().  id, name, url, sourceHandler
	 */
	public Document(Page page, JSONObject jobConfiguration, JSONObject jobSummaryInformation, String domainInstanceName, JobHistory currentJobHistory ) {	
		_type = "_doc";
		_mimeType = MimeType.getMimeType(page);
		_url = page.getWebURL().getURL();
		_domain = page.getWebURL().getDomain();
		_crawlDepth = page.getWebURL().getDepth();
		_relatedJobConfiguration = jobConfiguration;
		_jobSummaryInformation = jobSummaryInformation;
		
		_contentData = page.getContentData();
		_extractedTextFromTika = page.getTextContent();
		
		_httpHeaders = page.getFetchResponseHeaders();
		
		if (page.getWebURL().getParentUrl() != null) {
			_referrer =  page.getWebURL().getParentUrl().toString();
		}
		
		for (WebURL w: page.getOutgoingUrls()) {
			_outgoingURLs.add(w.getURL());
		}

		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
		_domainInstanceName = domainInstanceName;
		
		_jobHistory = currentJobHistory;
	}
	
	private static final String[] emptyKeywords = new String[0];
	
	public Document(java.io.File f, JSONObject jobConfiguration, JSONObject jobSummaryInformation, long lastModificationTimeEpochMillis,  String domainInstanceName, JobHistory currentJobHistory) throws IOException {
		_type = "_doc";
		_mimeType = MimeType.getMimeType(f);
		_url = f.toURI().toString();
		_domain = "localhost";
		_crawlDepth = 0;
		_relatedJobConfiguration = jobConfiguration;
		_jobSummaryInformation   = jobSummaryInformation;
		
		_contentData = Files.readAllBytes(f.toPath());
		_extractedTextFromTika = TikaUtilities.extractText(_contentData);

		_httpHeaders = null;
		_referrer    = null;
		
		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
		
		if (_mimeType == MimeType.TEXT_HTML) {
			_outgoingURLs = HTMLUtilities.getAllLinks(this.getContentDataAsString(), "file://localhost/"+f.getName(),emptyKeywords);
		}
		
		_lastModificationTimeEpochMillis = lastModificationTimeEpochMillis;
		_domainInstanceName = domainInstanceName;
		_jobHistory = currentJobHistory;

	}

	public Document(byte[] content, String mimeType, JSONObject jobConfiguration, JSONObject jobSummaryInformation, long lastModificationTimeEpochMillis,  String domainInstanceName, JobHistory currentJobHistory) throws IOException {
		_type = "_doc";
		_mimeType = mimeType;
		_url = "";
		_domain = "localhost";
		_crawlDepth = 0;
		_relatedJobConfiguration = jobConfiguration;
		_jobSummaryInformation   = jobSummaryInformation;
		
		_contentData = content;
		
		_httpHeaders = null;
		_referrer    = null;
		
		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
				
		_lastModificationTimeEpochMillis = lastModificationTimeEpochMillis;
		_domainInstanceName = domainInstanceName;
		_jobHistory = currentJobHistory;

	}
	
	
	
	/**
	 * Creates a document object from a given URL.
	 * 
	 * @param targetURL
	 * @param jobConfiguration      - this is the json configuration object for a particular job
	 * @param jobSummaryInformation - comes form Job.getSummary().  id, name, url, sourceHandler
	 * @throws Exception Can be thrown when the URL can not be loaded or is not formatted correctly.
	 */
	public Document(java.util.UUID documentID, String targetURL, JSONObject jobConfiguration, JSONObject jobSummaryInformation, String domainInstanceName, JobHistory currentJobHistory) throws Exception{	
		this(documentID, InternetUtilities.retrieveURL(targetURL, SourceHandlerInterface.getNextUserAgent(domainInstanceName), 0),
		     targetURL, jobConfiguration, jobSummaryInformation,domainInstanceName, currentJobHistory);
		
	}	
	
	public Document(java.util.UUID documentID, HttpContent content, String targetURL, JSONObject jobConfiguration, JSONObject jobSummaryInformation, String domainInstanceName, JobHistory currentJobHistory) throws Exception{	
		if (content == null) {
			throw new Exception("Unable to get results from url: "+targetURL);
		}

		_uuid = documentID.toString(); //overrides the initial documentID created
			
		_charset = content.charset;
		String contentType = content.contentType;		
		_contentData = content.contentData;
		_mimeType = MimeType.getMimeType(_contentData,contentType,content.url);
		_extractedTextFromTika =  TikaUtilities.extractText(_contentData);
		
		_outgoingURLs = content.outgoingURLs;
		_type = "_doc";		
		_url = content.url;  // this contains the final url if there were redirects
		_domain = content.domain;
		_crawlDepth = 0;
		
		_relatedJobConfiguration = jobConfiguration;
		_jobSummaryInformation = jobSummaryInformation;
		
		_httpHeaders = content.httpHeaders;
		
		_referrer = null;
		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
		_domainInstanceName = domainInstanceName;
		_jobHistory = currentJobHistory;
	}
	
	public Document(String documentID, String type, HttpContent content, File location, JSONObject jobConfiguration, JSONObject jobSummaryInformation, String domainInstanceName, JSONObject typeMetaInfo, JobHistory currentJobHistory) {	
		_uuid = documentID; //overrides the initial documentID created
			
		_charset = content.charset;
		_contentData = null;
		_mimeType = content.contentType;
		_extractedTextFromTika = "";
		
		_outgoingURLs = content.outgoingURLs;
		_type = type;		
		_url = content.url;  // this contains the final url if there were redirects
		_domain = content.domain;
		_crawlDepth = 0;
		
		_relatedJobConfiguration = jobConfiguration;
		_jobSummaryInformation = jobSummaryInformation;
		
		_httpHeaders = content.httpHeaders;
		
		_referrer = null;
		_sha256Hash = FileUtilities.generateSHA256Hash(location);
		_domainInstanceName = domainInstanceName;
		_sourceDocument = typeMetaInfo;
		_location = location;
		_jobHistory = currentJobHistory;
	}	
	
	
	
	
	public Document(Document originalDocument, byte[] newData, String newMimeType, long lastModificationTimeEpochMillis) {
		_mimeType      = newMimeType;
		_contentData   = newData;

		_type          = originalDocument.getType();
		_url           = originalDocument.getURL();
		_domain        = originalDocument.getDomain();
		_crawlDepth    = originalDocument.getCrawlDepth();
		
		_relatedJobConfiguration = originalDocument.getRelatedJobConfiguration();
		
		_extractedTextFromTika = TikaUtilities.extractText(_contentData);

		_httpHeaders = originalDocument.getHTTPHeaders();
		_referrer    = originalDocument.getReferrer();
		
		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
		
		_derived = true;
		_sourceUUID = originalDocument.getUUID();
		
		_outgoingURLs = originalDocument.getOutgoingURLs();
		
		_storageArea = originalDocument.getStorageArea();
		
		_lastModificationTimeEpochMillis  = lastModificationTimeEpochMillis;
		_jobSummaryInformation = originalDocument.getJobSummaryInformation();
		_domainInstanceName = originalDocument.getDomainInstanceName();
		_jobHistory = originalDocument.getJobHistory();
		
		_translateTargetLanguage = originalDocument.getTranslateTargetLanguage();
	}
	

    /**
     * 
     * Expected fields in the sourceDocument:
     * - "text"
     * - "created_at" -> when was the "sourceDocumment" created.  Needs to be parseable by DateUtilities.
     * 
     * @param sourceDocument
     * @param mimeType
     * @param type
     * @param sourceJob
     * @param url
     * @param domain
     * @throws Exception
     */
	public Document(JSONObject sourceDocument, String mimeType, String type, JSONObject jobConfiguration, JSONObject jobSummary, String url, String domain, String domainInstanceName, String alternateID, JobHistory currentJobHistory) {

		_charset = StandardCharsets.UTF_8.name();
		
		try {
			_contentData = sourceDocument.toString().getBytes(_charset);
		} catch (UnsupportedEncodingException e) {
			_contentData = sourceDocument.toString().getBytes();
		}
		
		_mimeType = mimeType;
		_extractedTextFromTika = sourceDocument.optString("text","");
		_extractedText         = sourceDocument.optString("text","");
		
		_type = type;		
		_url = url;
		_domain = domain;
		_crawlDepth = sourceDocument.optInt("crawlDepth",-1);
		
		_relatedJobConfiguration = jobConfiguration;
		_jobSummaryInformation   = jobSummary;
		
		_httpHeaders = new org.apache.http.Header[0];
		
		_referrer = null;
		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
		_domainInstanceName = domainInstanceName;
		_alternateID = alternateID;
		
		try {
			_lastModificationTimeEpochMillis = DateUtilities.getFromString(sourceDocument.getString("created_at")).toEpochSecond()*1000;
		}
		catch (Throwable t) {
			logger.log(Level.FINER,"Using current time for lastModification");
			_lastModificationTimeEpochMillis = Instant.now().getEpochSecond()*1000;
		}
		_sourceDocument = sourceDocument;
		
		_jobHistory = currentJobHistory;
	}		
	
	
	/**
	 * What type of a document is this?  file, web, email, attachment etc...
	 * @return
	 */
	public String getType() {
		return _type;
	}	
	
	/**
	 * set What type of a document is this?  file, web, etc...
	 */
	public void  setType(String newType) {
		_type = newType;
	}	
		
	
	public String getTranslateTargetLanguage() {
		return _translateTargetLanguage;
	}

	public void setTranslateTargetLanguage(String translateTarget) {
		this._translateTargetLanguage = translateTarget;
	}

	public String getMimeType() {
		return _mimeType;
	}
	
	public String getURL() {
		return  _url;
	}
	public void setURL(String newURLValue) {
		_url = newURLValue;
	}
	
	public String getDomain() {
		return _domain;
	}
	
	/**
	 * 
	 * @return -1 if unknown
	 */
	public int getCrawlDepth() {
		return _crawlDepth;
	}
	
	public byte[] getContentData() {
		return _contentData;
	}
	
	public String getContentDataAsString() {
		try {
			return new String(_contentData,_charset);
		} catch (UnsupportedEncodingException e) {
			return new String(_contentData);
		}
	}
	
	public String getExtractedTextFromTika() {
		return _extractedTextFromTika;
	}
	public void setExtractedTextFromTika(String text) {
		_extractedTextFromTika = text;
	}

	
	public JSONObject getRelatedJobConfiguration() {
		return _relatedJobConfiguration; 
	}
	
	public org.apache.http.Header[] getHTTPHeaders() {
		return _httpHeaders;
	}

	/**
	 * Adds an annotation to the document. Annotations should either be instances of JSONObject, JSONArray, or 
	 * String.
	 * 
	 * @param code
	 * @param value
	 * 
	 */
	public void addAnnotation(String code, Object value) throws RuntimeException {
		if (value instanceof JSONObject || value instanceof JSONArray || value instanceof String) {
			_annotations.put(code, value);
			_unsavedAnnotations.add(code);
		}
		else {
			String message = "Invalid type passwed to add annotation: "+ value.getClass().getName();
			RuntimeException re = new RuntimeException(message);
			logger.log(Level.SEVERE,message,re);
			throw re;
		}
	}
	
	public Object getAnnotation(String annotationKey) {
		return _annotations.get(annotationKey);
	}	
	
	public boolean hasAnnotation(String annotationKey) {
		return _annotations.containsKey(annotationKey);
	}	
	
	public String getReferrer() {
		return _referrer;
	}
	
	public String getContentHash() {
		return _sha256Hash;
	}
	
	public String getUUID() {
		return _uuid;
	}
	
	public void setExtractedText(String text, boolean overrideExistingExtraction) {
		if (_extractedText == null || overrideExistingExtraction == true) {
			_extractedText = text;
		}
	}
	
	public void setExtractedText(String text) {
		this.setExtractedText(text, false);
	}

	public String getExtractedText() {
		return _extractedText;
	}

	public String getSourceUUID() {
		return _sourceUUID;
	}

	/**
	 *  What language is this document in?  Tika used for language detection
	 *  
	 * @return 2 character language abbreviation according to ISO-639-1.
	 */
	public String getLanguage() {
		if (_language == null) {
			_language = TikaUtilities.detectLanguage(this.getExtractedTextFromTika());
			
		}
		return _language;
	}
	
	/**
	 * Adds the url to the list of outgoing URLs for the current document.  The URL should be fully specified
	 * (ie, protocol, machine, path) as necessary.
	 * 
	 * @param url
	 */
	public void addOutgoingURL(String url) {
		_outgoingURLs.add(url);
	}
	
	/**
	 * Returns the set of outgoing URLS that were on the document.  If no outgoing links are
	 * present, or the document is not an HMTL document, then an empty set is returned.
	 * 
	 * The set is created when the object is created.
	 * 
	 * @return set of URLs represented as strings.
	 */
	public Set<String> getOutgoingURLs() {
		return _outgoingURLs;
	}
	
	/**
	 * Returns the set of outgoing URLS that were on the document, but don't include
	 * the domain of this document in the url
	 * 
	 * @return set of URLs represented as strings.
	 */
	public Set<String> getOutgoingURLsToAnotherDomain() {
		HashSet<String> results = new HashSet<String>();
		for (String url: this.getOutgoingURLs()) {
			if (url.contains(this.getDomain()) == false) {
				results.add(url);
			}
		}
		return results;
	}	
	
	public JSONObject getJobSummaryInformation() {
		return _jobSummaryInformation;
	}
	
	public Boolean getDomainDiscoveryRelevancy() {
		JSONObject jo = (JSONObject) this.getAnnotation("domainDiscovery");
		if (jo.has("relevant")) {
			return jo.getBoolean("relevant");
		}
		else { return null; }
	}	

	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainDiscoveryRelevancy(Boolean flag, String userID) {
		JSONObject jo = (JSONObject) this.getAnnotation("domainDiscovery");
		if (flag != null) {
			jo.put("relevant",flag);
		}
		else {
			jo.remove("relevant");
		}
		jo.put("relevantUser", userID);
	}	

	
	public String getAlternateID() { return _alternateID; }
	public void setAlternateID(String newIDValue) { _alternateID = newIDValue; }
	
	
	/**
	 * Creates a JSON representation of the document.  This is used to send to ElasticSearch, 
	 * kafka (for graph loading), and HDFS..
	 * 
	 * @return JSONObject for the document's information ...
	 */
	public JSONObject createJSONDocument() {  
		if (_originalJSONObject !=null ) { return _originalJSONObject;}  // WARNING / TODO - this may cause inconsistencies!!!
		
    	JSONObject result = new JSONObject();
    	
    	result.put("domain", this.getDomain());
		result.put("url", this.getURL());
		result.put("crawlDepth",  this.getCrawlDepth());
		result.put("mime_type", this.getMimeType());
		result.put("type", this.getType());
		result.put("crawled_dt", Instant.now().toString()); 
		
		if (this.getSourceUUID() != null) {
			result.put("source_uuid", this.getSourceUUID());			
		}
		else {
			result.put("source_uuid", this.getUUID());
		}
		
		result.put("uuid", this.getUUID());  // this should be the same as the document id (_id) as stored in ElasticSearch
		
		if (this.getExtractedText() == null) {
			result.put("text", "");
			result.put("hash", FileUtilities.generateSHA256Hash(""));
			result.put("text_length", -1);
		}
		else {
			result.put("text", this.getExtractedText());
			result.put("hash", FileUtilities.generateSHA256Hash(this.getExtractedText()));			
			result.put("text_length", this.getExtractedText().length());			
		}
		
		if (_location != null) {
			result.put("raw_data_length",_location.length());
		}
		else if (this.getContentData() != null) {
			result.put("raw_data_length", this.getContentData().length);
		}
		else {
			result.put("raw_data_length", -1);
		}
		
		if (_sourceDocument != null) {
			result.put("sourceDocument", _sourceDocument);
		}
		
		if (this.getAlternateID() != null) {
			result.put("alternateID", this.getAlternateID());
		}

		
		for (String annotationKey: _annotations.keySet()) {
			result.put(annotationKey, this.getAnnotation(annotationKey));
			//System.out.println("***Document.createJSONDocument key, value = "+annotationKey+", "+this.getAnnotation(annotationKey));
		}
		
		/*
		control annotations by setting what's in the configuration ....
		JSONArray annotationList = Collector.getTheCollecter().getConfigurationPropertyAsArray(Configuration.ANNOTATIONS);
    	for (int i=0;i<annotationList.length();i++) {
    		String annotationKey = annotationList.getString(i);
    		if (this.hasAnnotation(annotationKey)) {
    			result.put(annotationKey, this.getAnnotation(annotationKey));
    		}
    	}
    	*/
    	
    	JSONObject tempResult = new JSONObject(result.toString());  // necessary to convert some of the underlying data types to the appropriate JSON types.  (e.g., hashmap)
    	JSONUtilities.correctFieldNamesWithPeriods(tempResult);

    	return tempResult;
	}
	
	public static Document createFromJSON(String domainInstanceName, JSONObject obj) {
		Document result = new Document();
		result._originalJSONObject = obj;
		
		result._type = "_doc";
		result._mimeType = obj.getString("mime_type");
		result._url      = obj.getString("url");
		result._domain   = obj.getString("domain");
		result._crawlDepth = obj.optInt("crawlDepth",-1);
		result._type       = obj.optString("type");
	
		result._sourceUUID = obj.getString("source_uuid");
		result._uuid       = result._sourceUUID;

		if (obj.has("provenance")) {
			JSONObject provenance = obj.getJSONObject("provenance");
			result._relatedJobConfiguration = provenance.getJSONObject("configuration");
			result._jobSummaryInformation   = provenance.getJSONObject("job");
		}
		else {
			result._relatedJobConfiguration = new JSONObject();
			result._jobSummaryInformation   = new JSONObject();
		}
		
		result._contentData           = obj.getString("text").getBytes(); 
		result._extractedTextFromTika = obj.getString("text");
		result._extractedText         = obj.getString("text");
		result._sha256Hash = FileUtilities.generateSHA256Hash(result._contentData);	
		result._domainInstanceName = domainInstanceName;
		
		if (obj.has("sourceDocument")) {
			result._sourceDocument = obj.optJSONObject("sourceDocument");
		}
		
		if (obj.has("alternateID")) {
			result._alternateID = obj.getString("alternateID");
		}
		
		java.util.List<Annotator> annnotators =  Annotator.getAllAnnotators();
		for (Annotator a: annnotators) {
			String code = a.getCode();
			Object o = obj.opt(code);
			if (o != null) {
				result._annotations.put(code,o);
			}	
		}
		
		JSONObject domainDiscoveryObject = obj.optJSONObject("domainDiscovery");
		if (domainDiscoveryObject == null) { domainDiscoveryObject =new JSONObject(); }
		result._annotations.put("domainDiscovery", domainDiscoveryObject);  // this isn't an annottator, so it would be missed ....
		
		/*
		_httpHeaders = page.getFetchResponseHeaders();

		if (page.getWebURL().getParentUrl() != null) {
			_referrer =  page.getWebURL().getParentUrl().toString();
		}
		*/
		
		result._outgoingURLs = new HashSet<String>();
		try {
			JSONArray ja = obj.getJSONArray("html_outlinks");
			for (int i=0;i< ja.length();i++) {
				Object o = ja.get(i);
				result._outgoingURLs.add(o.toString());
			}
		}
		catch (org.json.JSONException ex) {
			; // nothing to do - field didn't exist before
		}
	    
		return result;
	}
	
	
	public JSONObject createMetaDataRecordForRawStorage() {
		JSONObject rawStorageMetaData = new JSONObject();
		rawStorageMetaData.put("url", this.getURL());
		rawStorageMetaData.put("content-type", this.getMimeType());
		rawStorageMetaData.put("referring-url", this.getReferrer());
		if (this.getJobSummaryInformation() != null && this.getJobSummaryInformation().has("id")) {
			rawStorageMetaData.put("job_id", this.getJobSummaryInformation().getString("id"));
		}
		return rawStorageMetaData;
	}	
	
	/** 
	 * Where should files be stored?
	 * 
	 * @return
	 */
	public FileStorageAreaType getStorageArea() {
		return _storageArea;
	}
	
	/** 
	 * Where should files be stored?  Defaults to "normal"
	 * 
	 * @param newArea
	 */
	public void setStorageArea(FileStorageAreaType newArea) {
		_storageArea = newArea;
	}

	/**
	 * Returns the number of milliseconds since epoch for when this document was last modified.
	 * Generally, this is only set for documents created from the filesystem.  Will be -1 if undefined.
	 * 
	 * @return milliseconds since epoch.
	 */
	public long getLastModificationTimeEpochMillis() {
		return _lastModificationTimeEpochMillis;
	}
	
	/**
	 * Updates a document created from a JSON object with the contents of sourceDocument.
	 * 
	 * @param sourceDocument
	 */
	public void updatesourceDocument(JSONObject sourceDocument) {
		_charset = StandardCharsets.UTF_8.name();
		
		try {
			_contentData = sourceDocument.toString().getBytes(_charset);
		} catch (UnsupportedEncodingException e) {
			_contentData = sourceDocument.toString().getBytes();
		}
		
		_sha256Hash = FileUtilities.generateSHA256Hash(_contentData);
		
		_sourceDocument = sourceDocument;
	}
	
	public JSONDocumentMerger getDocumentMerger() {
		return _documentMerger;
	}
	
	public void setDocumentMerger(JSONDocumentMerger merger) {
		_documentMerger = merger;
	}
	
	
	public void setOutgoingURLs(Set<String> urls) {
		_outgoingURLs = urls;
	}
	
	
	public java.io.File getFileLocation() {
		return _location;
	}

	public long getContentSize() {
		long rawDataSize = -1;
		if (_location != null) {
			rawDataSize = _location.length();
		}
		else if (this.getContentData() != null) {
			rawDataSize = this.getContentData().length;
		}

		return rawDataSize;
	}

	public void setReferrer(String url) {
		_referrer = url;
	}

	
	// functions to allow use to track unsaved annotations
	/**
	 * empties out the list of unsaved annotations
	 */
	public void markDocumentSaved() {
		 _unsavedAnnotations.clear();
	}
	
	/**
	 * Returns a json object of all of the annotations that were not yet saved...
	 * 
	 * @return
	 */
	public JSONObject getUnsavedAnnotations() {
		JSONObject result = new JSONObject();
		
		for (String annotation: _unsavedAnnotations) {
			result.put(annotation, this.getAnnotation(annotation));
		}
		
		return result;
	}
	
	public JobHistory getJobHistory() { return _jobHistory; }
 
}
