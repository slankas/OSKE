package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.document.DocumentRouterSecondaryProcessor;
import edu.ncsu.las.model.RSS;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.DomainDiscoveryExecution;
import edu.ncsu.las.source.DomainDiscoveryHandler;
import edu.ncsu.las.source.DomainDiscoveryHandler.HandlerStatusType;
import edu.ncsu.las.source.DomainDiscoveryInterface;
import edu.ncsu.las.source.GoogleHandler;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.storage.ElasticSearchDomainDiscoveryQuery;
import edu.ncsu.las.storage.StorageProcessor;
import edu.ncsu.las.storage.export.ExportAssistant;
import edu.ncsu.las.translate.OpenKEAmazonTranslate;
import edu.ncsu.las.util.StringUtilities;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.DomainDiscoverySession;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Handles requests for the Domain Discovery Page.
 */
@RequestMapping(value = "rest/{domain}/searchSession")
@Controller
public class DomainDiscoverySessionController extends AbstractRESTController{
	
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	

	/**
	 * Creates a new search sandbox / domain discovery sandbox.
	 * 
	 * @param request
	 * @param sessionData
	 * @return
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ValidationException 
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String createNewSession(HttpServletRequest request, @RequestBody String sessionData, @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - InsertSessionData");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);

		JSONObject sessionJSON = new JSONObject(sessionData);
		
		String sessionName = sessionJSON.getString("sessionName");
		sessionName = sessionName.trim();
		
		boolean isNameValid = sessionName.length() > 0 && StringUtilities.isAlphaNumberWithLimitedSpecialCharacters(sessionName);
		
		if (!isNameValid) {
			JSONObject result = new JSONObject().put("status","error")
			                                    .put("message","Session name can not be empty.  Must onlycontain alphanumbers, commas, spaces, dashes, periods, and paranthesis");
			return result.toString();
		}
		
		DomainDiscoverySession dds = DomainDiscoverySession.createSession(domainStr, sessionName, this.getUser(request).getEmailID());
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.createNewSession", dds != null ? dds.toJSON() : new JSONObject().put("status","failure"), System.currentTimeMillis(), null, request,domainStr);
		
		if (dds != null) {
			JSONObject result = new JSONObject().put("status", "success").put("sessionID", dds.getSessionID());
			return result.toString();
		}
		else {
			JSONObject result = new JSONObject().put("status", "error")
					                            .put("message","Unable to create session record.");
			return result.toString();
		}		
	}
	
	
	/**
	 * For the given domain, list all of the sessions that have been created
	 * @param request
	 * @param domainStr
	 * 
	 * @return JSON array of the domain discovery session objects represented in JSON
	 * 
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getAllSessionsForDomain(HttpServletRequest request, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - RetrieveSessionData");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getAllSessionsForDomain", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);

		List<DomainDiscoverySession> allSessions = DomainDiscoverySession.getAllSessionsForDomain(domainStr);
		JSONArray ja = new JSONArray();
		allSessions.stream().forEach(dds -> ja.put(dds.toJSON()));
		
		return ja.toString();	
	}
	
	/**
	 * Find the specified domain discovery session
	 * 
	 * @param request
	 * @param sessionID
	 * @param domainStr
	 * @return
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getSession(HttpServletRequest request, @PathVariable("sessionID") UUID sessionID,
			                                               @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - RetrieveSessionData");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getSession", new JSONObject().put("sessionID",sessionID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);

		if (dds != null) {
			return dds.toJSON().toString().getBytes("UTF-8");
		}
		throw new ValidationException("No such session exists: "+ sessionID.toString());
		
	}	

	/**
	 * Delete the particular session.  
	 * This will also delete the executions and remove any retrieval records in the provenance data for collected documents.  
	 * If a document no longer has any retrievals, it will also be deleted.
	 * 
	 * @param request
	 * @param sessionID
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}", method = RequestMethod.DELETE, headers = "Accept=application/json")
	public @ResponseBody byte[] deleteSession(HttpServletRequest request, @PathVariable("sessionID") UUID sessionID, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController -delete session: "+sessionID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds.getUserID().equals(this.getEmailAddress(request)) || this.getUser(request).hasAccess(domainStr, RoleType.ADMINISTRATOR)) {
			DomainDiscoverySession.destroySession(domainStr,sessionID);
			this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.deleteSession", new JSONObject().put("sessionID",sessionID.toString()), startTime, System.currentTimeMillis(), request,domainStr);
			return new JSONObject().put("status","successful").toString().getBytes("UTF-8");		
		}
		else {
			this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.deleteSession",  new JSONObject().put("sessionID", sessionID.toString()).put("status","failure").put("message", "not owner"), System.currentTimeMillis(), null, request,domainStr);
			return new JSONObject().put("status","failure").put("message","user is not the session owner").toString().getBytes("UTF-8");
		}
	}	
	
	
	/**
	 * Initiate a new search execution within the specific session passed.
	 *  
	 * @param request
	 * @param sessionID
	 * @param targetLanguage
	 * @return
	 * @throws IOException
	 * @throws ValidationException 
	 */
	@RequestMapping(value = "/{sessionID}/execution/{targetLanguage}", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] initiateSearchExecution(
			HttpServletRequest request,
			@PathVariable("sessionID") UUID sessionID, 
			@PathVariable("targetLanguage") String translateTargetLanguage,
			@RequestBody String sessionData,
			@PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - start a search execution "+sessionID);
		logger.log(Level.INFO, "DDSessionController - ---- targetLanguage = "+translateTargetLanguage);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		String userID = this.getUser(request).getEmailID();
		JSONObject sessionJSON = new JSONObject(sessionData);
		String searchTerms = sessionJSON.getString("searchTerms");
		String searchTermsTranslate = sessionJSON.getString("searchTermsTranslate");
		int numberOfSearchResults = sessionJSON.getInt("numberOfSearchResults");
		String searchAPI = sessionJSON.getString("searchAPI");
		JSONObject advConfigObj = sessionJSON.optJSONObject("advConfig"); if (advConfigObj == null) { advConfigObj = new JSONObject(); }
		String targetLanguage = sessionJSON.getString("targetLanguage");
		boolean translateCheck = sessionJSON.getBoolean("translateCheck");
		
		String searchTermsToUse = searchTerms;
		
		String sessionName = sessionJSON.getString("sessionName");
		
		// TODO: validation
		// 1. Search terms are present (part of execution, not session)
        // 2. numberOfTopics > 0, < 50
		// 3. maxRunningTime > 0, < 20 
		// 4. numberofSearch Results.  >0, < 250
		// 5. searchAPI must be in "duckduckgo", "faroo", "millionshort", "qwant", "google"
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {
			throw new ValidationException("No such session exists: "+ sessionID.toString());
		}
		
		// if the user changed the session name, update both the object and the database record 
		if (!dds.getSessionName().equals(sessionName)) {
			dds.updateSessionName(sessionName);
		}
		
		// if user wants translate
		if ( searchTermsTranslate.length() > 0 ) {
			searchTermsToUse = searchTermsTranslate;
		}
		
		DomainDiscoveryExecution dde = dds.createExecution(searchTerms, userID, numberOfSearchResults, searchAPI, advConfigObj, domainStr, translateCheck, targetLanguage, searchTermsTranslate);
		if (dde == null) {
			throw new ValidationException("Unable to create execution record.");		
		}
					
		DomainDiscoveryHandler ddh = new DomainDiscoveryHandler(domainStr, sessionID,dde.getExecutionNumber());
		java.util.List<SearchRecord> records = null;
		
		//set passed in target language
		ddh.setTranslateTargetLanguage(translateTargetLanguage);
		
		JSONObject configuration = new JSONObject(); 
		DomainDiscoveryInterface ddi = DomainDiscoveryInterface.getSourceHandler(searchAPI);
		
		if (ddi == null) {
			throw new ValidationException("Invalid source handler: "+searchAPI);
		}
		
		ddi.setUserAgent(SourceHandlerInterface.getNextUserAgent(domainStr));
		//ddi.setUserAgent(Configuration.getConfigurationProperty(domainStr, ConfigurationType.WEBCRAWLER_USERAGENTSTRING));
		records = ddi.generateSearchResults(domainStr, searchTermsToUse, configuration, numberOfSearchResults, advConfigObj);	

		if (records == null || records.size() == 0) {
			throw new ValidationException("No results from underlying search engine.");
		}
		//System.out.println("***DEBUG: DomainDiscoverySessionController: records = "+records.toString());
		//remove duplicate entries - conversion to the LinkedHashSet does this while maintaining the insert order...
		records = new ArrayList<SearchRecord>(new LinkedHashSet<SearchRecord>(records));
		
		if (records.size() > numberOfSearchResults) {
			records = new ArrayList<SearchRecord>(records.subList(0,  numberOfSearchResults));
		}
		
		//translate the title and description
		if (translateTargetLanguage != null  && translateTargetLanguage.length() == 2 && translateCheck != false ) {   
			logger.log(Level.INFO, "translating record title and description to english from: "+translateTargetLanguage);
			
			OpenKEAmazonTranslate translator = OpenKEAmazonTranslate.getTheAmazonTranslator();
			
			for (SearchRecord record:records) {
				String rTitle = translator.getTranslation(translateTargetLanguage, "en", record.getName());
				String rDescription = translator.getTranslation(translateTargetLanguage, "en", record.getDescription());
				
				record.setNativeName(record.getName());
				record.setNativeDescription(record.getDescription());
				record.setName(rTitle);
				record.setDescription(rDescription);
			} 
		}
		
		ddh.setDiscoveryRecords(records);
		
		DomainDiscoveryHandler ddhPrior = DomainDiscoveryHandler.getDomainDiscoveryHandlerPrior(domainStr,sessionID, dde.getExecutionNumber());
		if (ddhPrior != null) {
			ddh.compareDiscoveryRecordPositions(ddhPrior);
		}
		
		JSONObject result = new JSONObject();
		result.put("status", "success");
		result.put("searchAPI", searchAPI);
		result.put("executionnumber", dde.getExecutionNumber());
		
		JSONArray ja = new JSONArray();
		for(SearchRecord r : ddh.getDiscoveryRecords()){
			ja.put(r.toJSON());
		}
		result.put("searchResults",ja);
		
		JSONArray jaDropped = new JSONArray();
		for(SearchRecord r : ddh.getDiscoveryRecordsDropped(ddhPrior)){
			jaDropped.put(r.toJSON());
		}
		result.put("droppedResults",jaDropped);
		
		
		ddh.crawlDocuments(dde); //starts crawling process in the background
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.initiateSessionExecution", new JSONObject(sessionData.toString()).put("sessionID",sessionID.toString()), startTime, System.currentTimeMillis(), request,domainStr);

		return result.toString().getBytes("UTF-8");
		
	}
	
	/**
	 * Gets the file uploaded at client.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/fileUpload", method = RequestMethod.POST)
	public @ResponseBody byte[] upload(MultipartHttpServletRequest request, HttpServletResponse response, @PathVariable("domain") String domainStr)
			throws IOException, ValidationException {
		logger.log(Level.INFO, "FileUploadController: POST files");
		this.validateAuthorization(request,domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		String strSessionID   = request.getParameter("sessionID");
		String sessionName = request.getParameter("sessionName");
		
		UUID sessionID;
		
		//System.out.println("sessionID: " +strSessionID);
		//System.out.println("sessionName: " +sessionName);
		
		DomainDiscoverySession dds;
		JSONObject result = new JSONObject();

		if (strSessionID == null || strSessionID.trim().equals("")) {
			sessionName = sessionName.trim();
			
			boolean isNameValid = sessionName.length() > 0 && StringUtilities.isAlphaNumberWithLimitedSpecialCharacters(sessionName);
			if (!isNameValid) {
				result.put("status","error")
				      .put("message","Session name can not be empty.  Must onlycontain alphanumbers, commas, spaces, dashes, periods, and paranthesis");
				return result.toString().getBytes("UTF-8");
			}
			
			dds = DomainDiscoverySession.createSession(domainStr, sessionName, this.getUser(request).getEmailID());
			
			if (dds != null) {
				result.put("status", "success");//.put("sessionID", dds.getSessionID().toString());
			}
			else {
				result.put("status", "error").put("message","Unable to create session record.");
				return result.toString().getBytes("UTF-8");
			}
		} 
		else {
			sessionID = UUID.fromString(strSessionID);
			dds = DomainDiscoverySession.findSession(sessionID);
			if (dds == null) {
				throw new ValidationException("No such session exists: "+ sessionID.toString());
			}
		}
		
		String userID = this.getUser(request).getEmailID();
		String searchTerms        = "";
		int numberOfSearchResults = 1;
		String searchAPI          = "fileUpload";
		JSONObject advConfigObj   = new JSONObject();
		String targetLanguage = "none";
		boolean translateCheck = false;
		String searchTermsTranslate = "none";
		
		boolean isNameValid = sessionName.length() > 0 && StringUtilities.isAlphaNumberWithLimitedSpecialCharacters(sessionName);	
		 
		if (!dds.getSessionName().equals(sessionName) && isNameValid) {// if the user changed the session name, update both the object and the database record
			dds.updateSessionName(sessionName);
		}
		
		DomainDiscoveryExecution dde = dds.createExecution(searchTerms, userID, numberOfSearchResults, searchAPI, advConfigObj, domainStr, translateCheck, targetLanguage, searchTermsTranslate);
		if (dde == null) {
			throw new ValidationException("Unable to create execution record.");		
		}
					
		DomainDiscoveryHandler ddh = new DomainDiscoveryHandler(domainStr, dds.getSessionID(),dde.getExecutionNumber());
		
		java.util.List<SearchRecord> records = new java.util.ArrayList<SearchRecord>(); 
		
		Iterator<String> itr = request.getFileNames();
		while (itr.hasNext()) {
			MultipartFile mpf = request.getFile(itr.next());
			String contentType = mpf.getContentType();
			String fileName = mpf.getOriginalFilename();
			logger.log(Level.INFO,	"DomainDiscoverySessonController: POST file: " + mpf.getOriginalFilename() + " (" + contentType + ") ");

			dde.setSearchTerms(fileName);
			SearchRecord sr = new SearchRecord(fileName,fileName,"",1,"file");
			sr.setUploadedContentType(contentType);
			sr.setUploadedData(mpf.getBytes());
			records.add(sr);
		}
		
		ddh.setDiscoveryRecords(records);
		result.put("sessionID", dds.getSessionID().toString());
		result.put("executionnumber", dde.getExecutionNumber());
		
		JSONArray ja = new JSONArray();
		for(SearchRecord r : ddh.getDiscoveryRecords()){
			ja.put(r.toJSON());
		}
		result.put("searchResults",ja);		
		result.put("droppedResults",new JSONArray());
		
		ddh.crawlDocuments(dde); //starts crawling process in the background

		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.fileUploadForExecution", result, startTime, System.currentTimeMillis(), request,domainStr);		
		return result.toString().getBytes("UTF-8");
	}	
	
	
	/**
	 * Reload the given document in the corresponding DomainDiscoveryHandler into the cache.
	 * (Updates for manipulating documentBuckets are made directly to elasticsearch)
	 * 
	 * @param request
	 * @param bodyData
	 * @param sessionID
	 * @param executionNumber
	 * @param documentID
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{uuid}/updateCache", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] updateCacheDocument(HttpServletRequest request, 
			                                        @PathVariable("sessionID") UUID sessionID,
			                                        @PathVariable("executionID") int executionNumber,
			                                        @PathVariable("uuid") UUID documentID, 
			                                        @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - update cache for document ("+sessionID+","+executionNumber+","+documentID+")");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		long startTime = System.currentTimeMillis();		
		JSONObject instrObj = new JSONObject().put("sessionID", sessionID.toString()).put("executionID",executionNumber).put("documentID", documentID.toString());
		
		DomainDiscoveryHandler.refreshDocumentInCache(domainStr,sessionID,documentID);
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.updateCacheDocument", instrObj, startTime, System.currentTimeMillis(), request,domainStr);		

		return new JSONObject().put("status", "success").toString().getBytes("UTF-8");
	}		
	
	

	/**
	 * For the given document, start a new search execution based upon it's outgoing links 
	 * 
	 * @param request
	 * @param bodyData
	 * @param sessionID
	 * @param executionNumber
	 * @param documentID
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{uuid}/outbound", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] inititateSearchExecutionForOutboundLinks(HttpServletRequest request, 
			                                                             @RequestBody String bodyData,
			                                                             @PathVariable("sessionID") UUID sessionID,
			                                                             @PathVariable("executionID") int executionNumber,
			                                                             @PathVariable("uuid") UUID documentID, 
			                                                             @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - start a search execution for outboundURLs ("+sessionID+","+executionNumber+","+documentID+")");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONObject instrObj = new JSONObject().put("sessionID", sessionID.toString()).put("executionID",executionNumber).put("documentID", documentID.toString());
		
		String userID = this.getUser(request).getEmailID();
		
		DomainDiscoveryHandler ddhPrior = DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber,true); 
		
		Document d = ddhPrior.getDocument(documentID);
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {
			throw new ValidationException("No such session exists: "+ sessionID.toString());
		}
		
		DomainDiscoveryExecution dde = dds.createExecution(d.getURL(), userID, -1, "custom", new JSONObject(),domainStr, false, "none", "none"); //TODO: do we need to check translate?
		if (dde == null) {
			throw new ValidationException("Unable to create execution record.");		
		}
		DomainDiscoveryHandler ddh = new DomainDiscoveryHandler(domainStr, sessionID,dde.getExecutionNumber());			
		
		
		// Remove anchor tags from URLS so that they are not crawled (probably should factor this into a function)
		Set<String> urls = d.getOutgoingURLs();
		Set<String> urlsToAdd = new java.util.HashSet<String>();
		Iterator<String> iterator = urls.iterator();
		while (iterator.hasNext()) {
		    String url = iterator.next();
		    if (url.contains("#")) {
		    	iterator.remove();
		    	urlsToAdd.add(url.substring(0,url.indexOf("#")));
		    }
		    else if (url.startsWith("mailto")) {
		    	iterator.remove();
		    }
		    else if (url.trim().length()==0) {
		    	iterator.remove();
		    }
		}
		urls.addAll(urlsToAdd);
		urls.remove(d.getURL()); // don't crawl the source page
		
		//Create search records from outbound URLS
		java.util.List<SearchRecord> records = urls.stream().map(e -> new SearchRecord(e,e,"",-1,"outboundURL")).collect(Collectors.toList());
		
		
		if (records == null || records.size() == 0) {
			throw new ValidationException("No outbound links were available.");
		}
		
		ddh.setDiscoveryRecords(records);	
		JSONObject result = new JSONObject();
		result.put("status", "success");
		result.put("executionnumber", dde.getExecutionNumber());
		
		JSONArray ja = new JSONArray();
		for(SearchRecord r : ddh.getDiscoveryRecords()){
			ja.put(r.toJSON());
		}
		result.put("searchResults",ja);
		result.put("droppedResults",new JSONArray());
		
		
		ddh.crawlDocuments(dde); //starts crawling process in the background
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.inititateSearchExecutionForOutboundLinks", instrObj, startTime, System.currentTimeMillis(), request,domainStr);		

		return result.toString().getBytes("UTF-8");
	}	
	
	
	
	
	/**
	 * Returns all of the executions for a given sessionID
	 * @param request
	 * @param sessionID
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}/execution", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllExecutionsForSession(
			HttpServletRequest request,
			@PathVariable("sessionID") UUID sessionID, 
			@PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get all executions "+sessionID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getAllExecutionsForSession", new JSONObject().put("sessionID",sessionID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {
			throw new ValidationException("No such session exists: "+ sessionID.toString());
		}
		
		List<DomainDiscoveryExecution> ddeList = dds.getExecutions();
		JSONArray result = new JSONArray();
		for (DomainDiscoveryExecution dde: ddeList) {
			result.put(dde.toJSON());
		}
		
		return result.toString().getBytes("UTF-8");
	}
	
	
	
	/**
	 * 
	 * @param request
	 * @param sessionID
	 * @return
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}/documents", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllSessionDocuments(HttpServletRequest request,@PathVariable("sessionID") UUID sessionID, @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get all documents for session: "+sessionID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getAllSessionDocuments", new JSONObject().put("sessionID",sessionID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {
			throw new ValidationException("No such session exists: "+ sessionID.toString());
		}
		
		List<JSONObject> documents = ElasticSearchDomainDiscoveryQuery.getDocumentsForSessionAndExecution(domainStr,sessionID.toString(), 0);
		JSONArray result = new JSONArray();
		for (JSONObject o: documents) {
			result.put(o);
		}
		
		return result.toString().getBytes("UTF-8");
	}		
	
	
	/**
	 * 
	 * @param request
	 * @param sessionID
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionID}/documentsSummary", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllSessionDocumentsSummary(HttpServletRequest request,@PathVariable("sessionID") UUID sessionID, @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get all documents for session: "+sessionID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getAllSessionDocumentsSummary", new JSONObject().put("sessionID",sessionID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {
			throw new ValidationException("No such session exists: "+ sessionID.toString());
		}
		List<DomainDiscoveryExecution> ddeList = dds.getExecutions();
		
		List<JSONObject> documents = ElasticSearchDomainDiscoveryQuery.getDocumentsForSessionAndExecution(domainStr,sessionID.toString(), 0);
		JSONArray result = new JSONArray();
		for (JSONObject o: documents) {
			Document doc = Document.createFromJSON(domainStr,o);
			int numConcepts =  (doc.getAnnotation("concepts") !=null) ? ((JSONArray) doc.getAnnotation("concepts")).length(): 0; 
			String publishDate = (doc.getAnnotation("published_date") !=null) ? doc.getAnnotation("published_date").toString() : "";
			
			JSONObject record = new JSONObject().put("title", o.getJSONObject("domainDiscovery").optString("title",""))
					                            .put("url", o.getJSONObject("domainDiscovery").optString("url",""))
					                            .put("description", o.getJSONObject("domainDiscovery").optString("description",""))
					                            .put("textLength", doc.getExtractedText().length())
					                            .put("textMinimizedLength", StringUtilities.eliminateNonSentences(doc.getExtractedText()).length())
					                            .put("totalOutgoingLinks", doc.getOutgoingURLs().size())
					                            .put("totalOutgoingLinksDifferentDomain", doc.getOutgoingURLsToAnotherDomain().size())
					                            .put("numConcepts",  numConcepts)
					                            .put("publishedDate",  publishDate);
			
			JSONArray retrievals = o.getJSONObject("domainDiscovery").getJSONArray("retrievals");
			for (int i=0; i < retrievals.length(); i++) {
				JSONObject instance = retrievals.getJSONObject(i);
				record.put("exec_"+instance.getInt("executionNumber"), instance.getInt("searchPosition"));
			}
			// now add in additional exec fields where the document didn't exist
			for (DomainDiscoveryExecution dde: ddeList) {
				int execNumber = dde.getExecutionNumber();
				if (record.has("exec_"+execNumber) == false) {
					record.put("exec_"+execNumber, -1);
				}
			}
			result.put(record);
		}
		
		return result.toString().getBytes("UTF-8");
	}	
	
	//TODO - doesn't appear to be called on the client side.  This may have been built to allow a list of URLs to be crawled....
	@RequestMapping(value = "/{sessionID}/executionByURL", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] createExecutionByURL(HttpServletRequest request,@PathVariable("sessionID") UUID sessionID, @PathVariable("domain") String domainStr, @RequestBody String body) throws  IOException, ValidationException {
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {
			throw new ValidationException("No such session exists: "+ sessionID.toString());
		}
		JSONObject bodyObject = new JSONObject(body);
		JSONArray urls = bodyObject.getJSONArray("urls");
		
		//TODO - does dde need to check translate stuff?
		DomainDiscoveryExecution dde = dds.createExecution("", this.getUser(request).getUserID(), -1, "weblist", new JSONObject().put("urls", urls), domainStr, false, "none", "none");
		if (dde == null) {
			throw new ValidationException("Unable to create execution record.");		
		}		
		
		
		String sessionName = bodyObject.getString("sessionName");
		boolean isNameValid = sessionName.length() > 0 && StringUtilities.isAlphaNumberWithLimitedSpecialCharacters(sessionName);	
		 
		if (!dds.getSessionName().equals(sessionName) && isNameValid) {// if the user changed the session name, update both the object and the database record
			dds.updateSessionName(sessionName);
		}
				
		List<SearchRecord> records = new ArrayList<SearchRecord>();
		int position = 0;
		for (Object url: urls) {
			position++;
			records.add(new SearchRecord(url.toString(), url.toString(), "", position,"url"));
		}
		//remove duplicate entries - conversion to the LinkedHashSet does this while maintaining the insert order...
		records = new ArrayList<SearchRecord>(new LinkedHashSet<SearchRecord>(records));

		DomainDiscoveryHandler ddh = new DomainDiscoveryHandler(domainStr, sessionID,dde.getExecutionNumber());
		ddh.setDiscoveryRecords(records);
		
		JSONObject result = new JSONObject();
		result.put("status", "success");
		result.put("executionnumber", dde.getExecutionNumber());
		
		JSONArray ja = new JSONArray();
		for(SearchRecord r : records){
			ja.put(r.toJSON());
		}
		result.put("searchResults",ja);
		result.put("droppedResults",new JSONArray());
		
		ddh.crawlDocuments(dde); //starts crawling process in the background
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.createExecutionByURL", new JSONObject().put("sessionID",sessionID.toString()).put("urls", urls), startTime, System.currentTimeMillis(), request,domainStr);
		
		return result.toString().getBytes("UTF-8");		
	}

	/**
	 * Returns the status of the current execution, including details for each search record
	 * @param request
	 * @param domainStr
	 * @param sessionID
	 * @param executionNumber
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionId}/execution/{executionNumber}/status", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getExecutionStatus(HttpServletRequest request, @PathVariable("domain") String domainStr,@PathVariable("sessionId") UUID sessionID,@PathVariable("executionNumber") int executionNumber) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - getStatus");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getExecutionStatus", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoveryHandler ddh = DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {
			throw new ValidationException("Unable to locate discovery handler.");
		}		
				
		String executionStatus = ddh.getHandlerStatus().toString();
		int totalPagesToCrawl  = ddh.getNumberOfPagesToCrawl();
		int crawledPageCount   = ddh.getCrawledPageCount();
		long processTime       = (ddh.getDomainDiscoveryExecution().getEndTime() != null ? ddh.getDomainDiscoveryExecution().getEndTime().toEpochMilli() : Instant.now().toEpochMilli()) - ddh.getDomainDiscoveryExecution().getStartTime().toEpochMilli();
		JSONObject result = new JSONObject().put("status", executionStatus)
				                            .put("total", totalPagesToCrawl)
				                            .put("crawled", crawledPageCount)
				                            .put("errored", ddh.getNumPageCrawledFailed())
				                            .put("successful", ddh.getNumPagesCrawledSuccessfully())
				                            .put("processTime",processTime)
				                            .put("documentIndexID", ddh.getDomainDiscoveryExecution().getDocumentIndexID().toString())
				                            .put("executionNumber", ddh.getDomainDiscoveryExecution().getExecutionNumber())
				                            .put("executionParameters", ddh.getDomainDiscoveryExecution().toJSON());
		
		JSONArray searchResults = new JSONArray();

		for(SearchRecord r : ddh.getDiscoveryRecords()){
			JSONObject ddrJSON = r.toJSON();
			if (r.getStatus().equals("crawled")) {
				Document doc = ddh.getDocument(r.getDocumentUUID());
				if (doc != null) {
					ddrJSON.put("relevant", doc.getDomainDiscoveryRelevancy());
					ddrJSON.put("originalSourceUUID", doc.getUUID());
					JSONObject sourceObject= doc.createJSONDocument();
					if (sourceObject.has("user_collection")) {
						JSONObject limitedSourceObject = new JSONObject().put("user_collection", sourceObject.get("user_collection"));
						ddrJSON.put("source", limitedSourceObject);
					}
				}
				
			}
			searchResults.put(ddrJSON);
		}
		result.put("searchResults",searchResults);

		return result.toString().getBytes("UTF-8");
	}
	
	/**
	 * For the given execution, return a list of documents that are still in the secondary processing queue.
	 * This should not be called unless the initial crawling is complete
	 * @param request
	 * @param domainStr
	 * @param sessionID
	 * @param executionNumber
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionId}/execution/{executionNumber}/secondaryStatus", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getExecutionSecondaryStatus(HttpServletRequest request, @PathVariable("domain") String domainStr,@PathVariable("sessionId") UUID sessionID,@PathVariable("executionNumber") int executionNumber) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - secondary status");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getExecutionSecondaryStatus", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoveryHandler ddh = DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {
			throw new ValidationException("Unable to locate discovery handler.");
		}		
		if (ddh.getHandlerStatus() !=  HandlerStatusType.COMPLETE) {throw new ValidationException("Crawl is not in a complete status - unable to provide secondary status");}
		
		JSONArray documentsInSecondary = new JSONArray();
		ddh.getDocuments().stream().forEach(d -> { 
			int position = DocumentRouterSecondaryProcessor.getTheSecondaryProcessor().getDocumentPositioninProcessingQueue(UUID.fromString(d.getUUID()));
			int size     = DocumentRouterSecondaryProcessor.getTheSecondaryProcessor().getProcessingQueueSize();
			if (position > -1) {
				documentsInSecondary.put(new JSONObject().put("documentID", d.getUUID())
				                                    .put("queuePosition", position)
				                                    .put("queueSize", size));
			}
		});
		
		JSONObject result = new JSONObject().put("numDocumentsInSecondary", documentsInSecondary.length())
				                            .put("documents", documentsInSecondary);
		return result.toString().getBytes("UTF-8");
	}	
	
	
	@RequestMapping(value = "/{sessionId}/execution/{executionNumber}/summary", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getExecutionSummaryStatistics(HttpServletRequest request, @PathVariable("domain") String domainStr,@PathVariable("sessionId") UUID sessionID,@PathVariable("executionNumber") int executionNumber) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - getStatus");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getExecutionSummaryStatistics", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoveryHandler ddh = DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {	throw new ValidationException("Unable to locate discovery handler.");	}
		if (ddh.getHandlerStatus() !=  HandlerStatusType.COMPLETE) {throw new ValidationException("Crawl is not in a complete status - unable to provide overall summary");}
		
		DomainDiscoverySession dds = DomainDiscoverySession.findSession(sessionID);
		if (dds == null) {	throw new ValidationException("No such session exists: "+ sessionID.toString());	}
		DomainDiscoveryExecution dde = dds.getExecution(executionNumber);
		if (dde == null) {	throw new ValidationException("No such execution exists: "+ dde);	}
		
		int totalPagesToCrawl  = ddh.getNumberOfPagesToCrawl();
		int crawledPageCount   = ddh.getCrawledPageCount();
		long processTime;
		try {
			processTime = (ddh.getDomainDiscoveryExecution().getEndTime() != null ? ddh.getDomainDiscoveryExecution().getEndTime().toEpochMilli() : Instant.now().toEpochMilli()) - ddh.getDomainDiscoveryExecution().getStartTime().toEpochMilli();
		}
		catch (NullPointerException ex) {
			processTime       = -1;
		}
		JSONObject crawlSummary = new JSONObject().put("status", ddh.getHandlerStatus().toString())
				                            .put("total", totalPagesToCrawl)
				                            .put("crawled", crawledPageCount)
				                            .put("errored", ddh.getNumPageCrawledFailed())
				                            .put("successful", ddh.getNumPagesCrawledSuccessfully())
				                            .put("processTime",processTime); 
		
		JSONArray documents = new JSONArray();
		for(SearchRecord r : ddh.getDiscoveryRecords()){
			JSONObject ddrJSON = r.toJSON();
			if (r.getStatus().equals("crawled")) {
				Document doc = ddh.getDocument(r.getDocumentUUID());
				int numConcepts =  (doc.getAnnotation("concepts") !=null) ? ((JSONArray) doc.getAnnotation("concepts")).length(): 0; 
				String publishDate = (doc.getAnnotation("publish_date") !=null) ? doc.getAnnotation("publish_date").toString() : "";
				
				ddrJSON.put("textLength", doc.getExtractedText().length())
				       .put("textMinimizedLength", StringUtilities.eliminateNonSentences(doc.getExtractedText()).length())
				       .put("totalOutgoingLinks", doc.getOutgoingURLs().size())
					   .put("totalOutgoingLinksDifferentDomain", doc.getOutgoingURLsToAnotherDomain().size())
				       .put("numConcepts",  numConcepts)
				       .put("publishDate",  publishDate);
			}
			else {
				ddrJSON.put("textLength", 0)
			           .put("textMinimizedLength", 0)
			           .put("totalOutgoingLinks", 0)
				       .put("totalOutgoingLinksDifferentDomain", 0)
			           .put("numConcepts",  0)
				       .put("publishDate",  "");
			}
			documents.put(ddrJSON);
		}
		
		JSONObject result = new JSONObject().put("crawlSummary",crawlSummary)
				                            .put("documentSummary",documents)
				                            .put("searchCriteria", dde.toJSON());
		
		return result.toString().getBytes("UTF-8");
	}	
	
	
	@RequestMapping(value = "/{sessionId}/execution/{executionNumber}/cancel", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] cancelExecution(HttpServletRequest request,@PathVariable("sessionId") UUID sessionID,@PathVariable("executionNumber") int executionNumber, @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - getStatus");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.cancelExecution", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber), System.currentTimeMillis(), null, request,domainStr);
				
		DomainDiscoveryHandler ddh = DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {
			throw new ValidationException("Unable to locate discovery handler.");
		}		
		ddh.setHandlerStatus(DomainDiscoveryHandler.HandlerStatusType.CANCELLING);
		
		String executionStatus = ddh.getHandlerStatus().toString();
		int totalPagesToCrawl  = ddh.getNumberOfPagesToCrawl();
		int crawledPageCount   = ddh.getCrawledPageCount();
		long processTime       = (ddh.getDomainDiscoveryExecution().getEndTime() != null ? ddh.getDomainDiscoveryExecution().getEndTime().toEpochMilli() : Instant.now().toEpochMilli()) - ddh.getDomainDiscoveryExecution().getStartTime().toEpochMilli();
		JSONObject result = new JSONObject().put("status", executionStatus)
				                            .put("total", totalPagesToCrawl)
				                            .put("crawled", crawledPageCount)
				                            .put("errored", ddh.getNumPageCrawledFailed())
				                            .put("successful", ddh.getNumPagesCrawledSuccessfully())
				                            .put("processTime",processTime); 
		
		JSONArray searchResults = new JSONArray();
		for(SearchRecord r : ddh.getDiscoveryRecords()){
			JSONObject ddrJSON = r.toJSON();
			if (r.getStatus().equals("crawled")) {
				ddrJSON.put("relevant", ddh.getDocument(r.getDocumentUUID()).getDomainDiscoveryRelevancy());
				ddrJSON.put("originalSourceUUID", ddh.getDocument(r.getDocumentUUID()).getUUID());
			}
			searchResults.put(ddrJSON);
		}
		result.put("searchResults",searchResults);
		
		return result.toString().getBytes("UTF-8");
	}	
	
	/**
	 * Start LDA topic modelling on the given execution
	 * @param request
	 * @param sessionID
	 * @param executionNumber
	 * @param bodyData
	 * @param domainStr
	 * @return
	 * @throws UnirestException
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionId}/execution/{executionNumber}/LDA", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody byte[] executionStartLDATopicModelling(HttpServletRequest request,@PathVariable("sessionId") UUID sessionID,@PathVariable("executionNumber") int executionNumber, @RequestBody String bodyData, @PathVariable("domain") String domainStr) throws UnirestException, IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - starting LDA");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		JSONObject dataJSON = new JSONObject(bodyData);
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.executionStartLDATopicModelling", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber).put("parameters", dataJSON), System.currentTimeMillis(), null, request,domainStr);
		int numTopics = dataJSON.getInt("numTopics");
		boolean stemWords = dataJSON.getBoolean("stemWords");
		boolean useRelevantDocs    = dataJSON.getBoolean("relevantFlag");
		boolean useUnkRelevantDocs = dataJSON.getBoolean("unkrelevantFlag");
		boolean useNotRelevantDocs = dataJSON.getBoolean("notrelevantFlag");
		
		if (numTopics < 1 || numTopics > 100) { 
			throw new ValidationException("Num topics must be between 1 and 100");
		}
		
		JSONArray documents = new JSONArray();
		DomainDiscoveryHandler ddh = DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber,true);
		if (ddh == null) {
			throw new ValidationException("Unable to create set of prior documents.");
		}
		List<Document> docs = ddh.getDocuments();
		for (int i=docs.size() -1; i>=0; i--) {
			Document d = docs.get(i);
			if (d.getDomainDiscoveryRelevancy() == null && useUnkRelevantDocs == false) { continue; }
			if (d.getDomainDiscoveryRelevancy() != null && d.getDomainDiscoveryRelevancy() && useRelevantDocs == false) {continue; }
			if (d.getDomainDiscoveryRelevancy() != null && d.getDomainDiscoveryRelevancy() == false && useNotRelevantDocs == false) { continue; }
	
			documents.put(new JSONObject().put("url", d.getURL()).put("uuid", d.getUUID()).put("text", d.getExtractedText()));
		}
   		
   		JSONObject ldaRequest =  new JSONObject().put("documents", documents);
   		ldaRequest.put("numTopics",numTopics);  
   		ldaRequest.put("numKeywords",10);
   		ldaRequest.put("maxIterations",50);
   		ldaRequest.put("stemWords",stemWords);
		
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.TOPICMODEL_API)+"v1/LDA")
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .body(ldaRequest).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");	
	}
	
	/**
	 * Gets the current status of the LDA Topic Modelling.  If the result is complete, all of the topic data will be returned as well
	 * 
	 * @param request
	 * @param sessionID - must be a valid UUID, but otherwise ignored
	 * @param executionNumber - 
	 * @param domainStr
	 * @param ldaSessionID This is the UUID generated during the start LDA process.  The client will use this value to get the value. 
	 * @return
	 * @throws JsonGenerationException
	 * @throws UnirestException
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{sessionId}/execution/{executionNumber}/LDA/{ldaSessionID}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getExecutionLDAStatus(HttpServletRequest request,@PathVariable("sessionId") UUID sessionID, @PathVariable("executionNumber") int executionNumber, @PathVariable("domain") String domainStr, @PathVariable("ldaSessionID") UUID ldaSessionID) throws  UnirestException, IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - getLdaStatus");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getExecutionLDAStatus", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber), System.currentTimeMillis(), null, request,domainStr);
		
		String url = Configuration.getConfigurationProperty(domainStr,ConfigurationType.TOPICMODEL_API)+"v1/LDA/"+ldaSessionID;
		logger.log(Level.INFO, "DDSessionController - getLdaStatus: "+ url);
		
		HttpResponse<JsonNode> jsonResponse = Unirest.get(url).header("accept", "application/json").asJson();
		JSONObject result = jsonResponse.getBody().getObject();
	
		return result.toString().getBytes("UTF-8");	
	}
			
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getAllDocumentsForSessionAndExecution(HttpServletRequest request,
			                                                          @PathVariable("sessionID") UUID sessionID,
			                                                          @PathVariable("executionID") int executionNumber, 
			                                                          @PathVariable("domain") String domainStr)  throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - AllDocumentsForSessionAndExecution  ");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getAllDocumentsForSessionAndExecution", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoveryHandler ddh =DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber,true);
		
		JSONArray documents = new JSONArray();
		for (Document d: ddh.getDocuments()) {
			documents.put(d.createJSONDocument());
		}
		
		JSONObject result = new JSONObject().put("documents", documents);
		
		if ( ddh.getDomainDiscoveryExecution() != null) {
			result.put("execution", ddh.getDomainDiscoveryExecution().toJSON());
		}
		else {
			DomainDiscoveryExecution dde = DomainDiscoveryExecution.loadExectuion(sessionID, executionNumber);
			if (dde != null) {
				result.put("execution", dde.toJSON());
			}
		}
		
		
		return result.toString().getBytes("UTF-8");
	}

	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{documentID}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocument(HttpServletRequest request,@PathVariable("sessionID") UUID sessionID,
			                                           @PathVariable("executionID") int executionNumber,
			                                           @PathVariable("documentID") String documentID, 
			                                           @PathVariable("domain") String domainStr)  throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getDocument", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber).put("documentID", documentID), System.currentTimeMillis(), null, request,domainStr);

		Document doc = null;  

		DomainDiscoveryHandler ddh =DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh != null) {
			try {
				UUID uuid =  UUID.fromString(documentID);
				doc = ddh.getDocument(uuid);
			}
			catch(Exception e) {
				doc = ddh.getDocument(documentID);
			}
		}
		if (doc == null) {
			JSONObject o = ElasticSearchDomainDiscoveryQuery.getDocument(domainStr,documentID);
			if (o != null) { return o.toString().getBytes("UTF-8");} 
			
			throw new ValidationException("Unable to locate document.");
		}
		
		byte[] result = doc.createJSONDocument().toString().getBytes("UTF-8");		
		return result;
	}	
	
	
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{documentID}/statistics", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentStatisticsByUUID(HttpServletRequest request,
			                                                @PathVariable("sessionID") UUID sessionID,
			                                                @PathVariable("executionID") int executionNumber,
			                                                @PathVariable("documentID") UUID documentID, 
			                                                @PathVariable("domain") String domainStr)  throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getDocumentStatistics", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber).put("documentID", documentID), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoveryHandler ddh =DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {
			throw new ValidationException("Unable to locate document handler.");
		}
		
		Document doc = ddh.getDocument(documentID);

		if (doc == null) {
			throw new ValidationException("Unable to locate document.");
		}
		
		int numConcepts =  (doc.getAnnotation("concepts") !=null) ? ((JSONArray) doc.getAnnotation("concepts")).length(): 0; 
		String publishDate = (doc.getAnnotation("publish_date") !=null) ? doc.getAnnotation("publish_date").toString() : "";
		
		JSONObject result = new JSONObject().put("textLength", doc.getExtractedText().length())
		                                    .put("textMinimizedLength", StringUtilities.eliminateNonSentences(doc.getExtractedText()).length())
		                                    .put("totalOutgoingLinks", doc.getOutgoingURLs().size())
		                                    .put("totalOutgoingLinksDifferentDomain", doc.getOutgoingURLsToAnotherDomain().size())
		                                    .put("numConcepts",  numConcepts)
		                                    .put("publishDate",  publishDate)
		                                    .put("url", doc.getURL())
		                                    .put("source_uuid", doc.getSourceUUID());
		return result.toString().getBytes("UTF-8");
	}		
	
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{documentID}/rss", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] findRSS(HttpServletRequest request,@RequestBody String postData,
			                            @PathVariable("sessionID") UUID sessionID,
			                            @PathVariable("executionID") int executionNumber,
			                            @PathVariable("documentID") UUID documentID,
			                            @PathVariable("domain") String domainStr)  throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - create RSS Job: "+documentID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.findRSSFeedsInDocument", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber).put("documentID", documentID), System.currentTimeMillis(), null, request,domainStr);
		
		JSONObject postDataObject = new JSONObject(postData);
				
		// load the data
		byte[] data = StorageProcessor.getTheStorageProcessor().loadRawData(FileStorageAreaType.SANDBOX,domainStr, documentID.toString());
		String content = null;
		if (data != null) {
			content = new String(data,"UTF-8");
		}
		Set<RSS.RSSEntry> feeds = RSS.findRSSFeeds(postDataObject.getString("url"), content, SourceHandlerInterface.getNextUserAgent(domainStr));
		
		JSONArray feedArray = new JSONArray();
		for (RSS.RSSEntry f: feeds) {
			feedArray.put(f.toJSON());
		}
		JSONObject result = new JSONObject().put("feeds", feedArray );
		return result.toString().getBytes("UTF-8");
	}			
	
	
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{uuid}", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody String updateRelevancyFlag(HttpServletRequest request, @RequestBody String relevancyData,
			                                        @PathVariable("sessionID") UUID sessionID,
			                                        @PathVariable("executionID") int executionNumber,
			                                        @PathVariable("uuid") UUID documentID, 
			                                        @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - InsertSessionData");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		JSONObject relevancyJSON = new JSONObject(relevancyData);

		Boolean relevantFlag;
		try { 
			relevantFlag = relevancyJSON.getBoolean("relevant");
		}
		catch (Throwable t) {
			relevantFlag = null; // there was not a valid true/false sent, so assume that it's being changed to "not set"
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.updateRelevancyFlag", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber).put("documentID", documentID).put("relevancy", relevantFlag == null ? "" : relevantFlag.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		String userID = this.getUser(request).getEmailID();
		
		DomainDiscoveryHandler ddh =DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {
			JSONObject result = new JSONObject().put("status", "error")
                                                .put("message","Unable to locate document handler.");
			return result.toString();	
		}
		Document doc = ddh.getDocument(documentID);
		if (doc == null) {
			JSONObject result = new JSONObject().put("status", "error")
                                                .put("message","Unable to locate document.");
			return result.toString();	
		}		
		
		if (ElasticSearchDomainDiscoveryQuery.updateRelevancy(domainStr,doc.getUUID(), userID, relevantFlag)) {
			doc.setDomainDiscoveryRelevancy(relevantFlag,userID);
			JSONObject result = new JSONObject().put("status", "success");
			return result.toString();
		}
		else {
			JSONObject result = new JSONObject().put("status", "error")
					                            .put("message","Unable to update relevancy flag.");
			return result.toString();
		}
		
	}
	
	/**
	 * Exports the domain discovery data 
	 * @param httpRequest
	 * @param q
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/export", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String performExport(HttpServletRequest httpRequest, @RequestBody String q,
			                           @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		
		logger.log(Level.FINE,"DomainDiscoveryController: perform export");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
				
		JSONObject exportObject = new JSONObject(q);
		JSONObject optionObject = exportObject.getJSONObject("options");
		
		JSONObject domainDiscoveryObj = exportObject.getJSONObject("domainDiscovery");
		JSONObject queryClause;
		if (domainDiscoveryObj.has("executionNumber")) {
			queryClause = ElasticSearchDomainDiscoveryQuery.createQueryClauseForSessionAndExecution(domainDiscoveryObj.getString("sessionID"),domainDiscoveryObj.getInt("executionNumber"));
		}
		else {
			queryClause = ElasticSearchDomainDiscoveryQuery.createQueryClauseForSession(domainDiscoveryObj.getString("sessionID"));
		}
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.exportSession", exportObject, System.currentTimeMillis(), null, httpRequest,domainStr);

		String downloadURL = URI.create(httpRequest.getRequestURL().toString()).resolve(httpRequest.getContextPath()).toString();
		ExportAssistant.initiateDownload(domainStr, exportObject, queryClause, optionObject, this.getUser(httpRequest), downloadURL);
		
		return "Success";
			
	}	
	
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{documentID}/summary", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentTextrankSummary(HttpServletRequest request,@PathVariable("sessionID") UUID sessionID,
			                                              @PathVariable("executionID") int executionNumber,
			                                              @PathVariable("documentID") UUID documentID, 
			                                              @RequestParam("percentage") double percentage,
			                                              @PathVariable("domain") String domainStr)  throws  IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getDocumentTextrankSummary", new JSONObject().put("sessionID",sessionID.toString()).put("executionNumber", executionNumber).put("documentID", documentID), System.currentTimeMillis(), null, request,domainStr);
		
		DomainDiscoveryHandler ddh =DomainDiscoveryHandler.getDomainDiscoveryHandler(domainStr,sessionID, executionNumber);
		if (ddh == null) {
			throw new ValidationException("Unable to locate document handler.");
		}
		Document doc = ddh.getDocument(documentID);
		if (doc == null) {
			throw new ValidationException("Unable to locate document.");
		}

		JSONObject queryJSON = new JSONObject().put("text",doc.getExtractedText());
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.TEXTRANK_API)+"summary/"+percentage)
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .body(queryJSON).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");	
	}		
	
	@RequestMapping(value = "/googleRelated", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody  byte[] getGoogleRelatedItems(HttpServletRequest request,@PathVariable("domain") String domainStr,
			                                   @RequestParam("query") String query) throws  IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "DDSessionController - getting google related searches: "+query);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getSuggestionsFromGoogle", new JSONObject().put("query",query), System.currentTimeMillis(), null, request,domainStr);
		
		GoogleHandler gh = new GoogleHandler();
		List<String> related = gh.getRelatedSearches(domainStr, query);
		java.util.Collections.sort(related);
		
		JSONArray results = new JSONArray();
		related.forEach(i -> results.put(i));
		
		JSONObject result = new JSONObject().put("relatedSearches", results);
		
		return result.toString().getBytes("UTF-8");
	
	}
	
	
	/**
	 * Translate search terms using amazon translate service
	 * @param request
	 * @param srcLanguage
	 * @param destLanguage
	 * @param searchTerms
	 * @param domainStr
	 * @return The translated terms
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/translate/searchTerms/{srcLang}/{destLang}/{searchterms}", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getSearchTermTranslation(HttpServletRequest request, 
			@PathVariable("srcLang") String srcLanguage,
			@PathVariable("destLang") String destLanguage,
			@PathVariable("searchterms") String searchTerms, 
			@PathVariable("domain") String domainStr) 
			throws  IOException, ValidationException {
		logger.log(Level.INFO, "DomainDiscoverySessionController getSearchTermTranslation - search terms to translate: "+searchTerms);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getSearchTermTranslation", new JSONObject().put("searchterms",searchTerms).put("destLang", destLanguage), System.currentTimeMillis(), null, request,domainStr);

		OpenKEAmazonTranslate translator = OpenKEAmazonTranslate.getTheAmazonTranslator();
		String translated = translator.getTranslation(srcLanguage, destLanguage, searchTerms);
		logger.log(Level.INFO, "getSearchTermTranslation - translated: "+translated);
		JSONObject result = new JSONObject().put("translated", translated);
		return result.toString();
	}

	/**
	 * 
	 * @param request
	 * @param domainStr
	 * @param term space separated list of terms
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 * @throws UnirestException 
	 */
	@RequestMapping(value = "/wordnetDefinition/{language}/{terms}", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getWordNetDefinitions(HttpServletRequest request, 
			@PathVariable("domain") String domainStr,
			@PathVariable("language") String language,
			@PathVariable("terms") String terms ) throws IOException, ValidationException, UnirestException {
		logger.log(Level.INFO,"DomainDiscoverySessionController: getWordNetDefiniitions - " + terms);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		String[] termArray = terms.split(" ");
		JSONArray result = new JSONArray();
		for (String term: termArray) {
			String url = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.NLDEFINITIONEXPANSION_API)+ term.replace(" ","+")+"/definition";
			
			HttpResponse<JsonNode> jsonResponse = Unirest.get(url)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json").asJson();
			JSONArray definitions  = jsonResponse.getBody().getArray();
			JSONObject item = new JSONObject().put("term",term).put("definitions", definitions);
			
			if (definitions.length() > 0) {
				String urlExpansions = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.NLDEFINITIONEXPANSION_API)+
						               term.replace(" ","%20")+"/"+language +"/definition/"+
						               definitions.getString(0).replace(" ","%20");
				
				HttpResponse<JsonNode> jsonResponseExpansions = Unirest.get(urlExpansions)
	                    .header("accept", "application/json")
	                    .header("Content-Type", "application/json").asJson();
				JSONArray expansions  = jsonResponseExpansions.getBody().getArray();
				
				item.put("expansions", expansions);
			}
			else {
				item.put("expansions", new JSONArray());
			}
			result.put(item);		
		}
		
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getWordNetDefinitions", new JSONObject().put("terms",terms), startTime, System.currentTimeMillis(), request,domainStr);				
		return result.toString();
	}
	
	/**
	 * 
	 * @param request
	 * @param domainStr
	 * @param term 
	 * @param language
	 * @param definition
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 * @throws UnirestException 
	 */
	@RequestMapping(value = "/wordnetDefinition/{language}/{term}/definition/{definition}", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getWordNetDefinitionExpansion(HttpServletRequest request, 
			@PathVariable("domain") String domainStr,
			@PathVariable("language") String language,
			@PathVariable("term") String term,
			@PathVariable("definition") String definition) throws IOException, ValidationException, UnirestException {
		logger.log(Level.INFO,"DomainDiscoverySessionController: getWordNetDefiniitionExpansion - " + term+", "+definition);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		String urlExpansion = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.NLDEFINITIONEXPANSION_API)+
				                  term.replace(" ","%20")+"/"+language+"/definition/"+
				                  definition.replace(" ","%20");
				
		HttpResponse<JsonNode> jsonResponseExpansions = Unirest.get(urlExpansion)
	                    .header("accept", "application/json")
	                    .header("Content-Type", "application/json").asJson();
		JSONArray expansions  = jsonResponseExpansions.getBody().getArray();
				
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getWordNetDefinitionExpansion", new JSONObject().put("term",term), startTime, System.currentTimeMillis(), request,domainStr);				
		return expansions.toString();
	}	


	@RequestMapping(value = "/wordnet/{type}/{term}", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getWordNet(HttpServletRequest request, 
			@PathVariable("domain") String domainStr,
			@PathVariable("type") String type,
			@PathVariable("term") String term ) throws IOException, ValidationException {
		logger.log(Level.INFO,"DomainDiscoverySessionController: getWordNet - " + term);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getWordNet", new JSONObject().put("term",term), startTime, System.currentTimeMillis(), request,domainStr);		
		
		JSONObject expanded = new JSONObject();
		String[] arrTerm = term.split("\\s+");
		for ( String s: arrTerm ) {
			String result =  proxyToWordNetAPI(s,type);
			if ( result != null && result.length() > 0 ) {
				expanded.put(s, result);
			}
		}
		logger.log(Level.INFO,"DomainDiscoverySessionController: getWordNet - expanded.toString() = " + expanded.toString());
		return expanded.toString();
	}
	
	private String proxyToWordNetAPI(String term, String type) {
		RestTemplate template = new RestTemplate();
		HttpHeaders requestHeaders = new HttpHeaders();
		HttpEntity<String> requestEntity = new HttpEntity<String>(requestHeaders);
		ConfigurationType confType;
		
		if ( type.equalsIgnoreCase("general") ) {
			confType = ConfigurationType.WORDNET_GENERALIZED;
		}else {
			confType = ConfigurationType.WORDNET_SPECIALIZED;
		}
		String url = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, confType)+term;
		
		ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, requestEntity, String.class);
		logger.log(Level.INFO,"DomainDiscoverySessionController: proxyToWordNetAPI response.getBody() = " + response.getBody());
		return response.getBody();		
	}
	
	
	@RequestMapping(value = "/nlex/{type}/{term}", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getNLEX(HttpServletRequest request, 
			@PathVariable("domain") String domainStr,
			@PathVariable("type") String type,
			@PathVariable("term") String term ) throws Exception {
		logger.log(Level.INFO,"DomainDiscoverySessionController: getNLEX - " + term);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getNLEX", new JSONObject().put("term",term), startTime, System.currentTimeMillis(), request,domainStr);		
		
		JSONObject expanded = new JSONObject();
		String[] arrTerm = term.split("\\s+");
		for ( String s: arrTerm ) {
			String result =  proxyToNLEX_API(s,type);
			if ( result != null && result.length() > 0 ) {
				expanded.put(s, result);
			}
		}
		logger.log(Level.INFO,"DomainDiscoverySessionController: getNLEX - expanded.toString() = " + expanded.toString());
		return expanded.toString();
	}
	
	private String proxyToNLEX_API(String term, String type) throws Exception {
		RestTemplate template = new RestTemplate();
		HttpHeaders requestHeaders = new HttpHeaders();
		HttpEntity<String> requestEntity = new HttpEntity<String>(requestHeaders);
		ConfigurationType confType;
		
		if ( type.equalsIgnoreCase("semantic") ) {
			confType = ConfigurationType.NLEXPANSION_SEMANTIC;
		}else {
			confType = ConfigurationType.NLEXPANSION_SYNTACTIC;
		}
		String url = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, confType)+term;
		
		ResponseEntity<String> response = template.exchange(url, HttpMethod.GET, requestEntity, String.class);

		JSONArray returnArray = new JSONArray();
		
		if (type.equalsIgnoreCase("semantic")) {
			JSONObject jObj = new JSONObject(response.getBody());
			JSONArray ja = (JSONArray) jObj.get(term);
			if (ja != null && ja.length() > 0) {
				for (int i = 0; i < ja.length(); i++) {
					System.out.println("jsonarray for " + term + " = " + ja.optString(i));
					JSONObject jo = new JSONObject();
					jo.put("word", ja.optJSONArray(i).optString(0));
					returnArray.put(i, jo);

				}
			} 
		}else {
			//{'program', 'programs', 'programming', 'programmed'}
			String[] arrRs;
			String rs = response.getBody().substring(1, response.getBody().length()-1);
			//JSONArray ja = new JSONArray();
			if(rs.indexOf(",") != -1) {
				arrRs = rs.split(",");
			}else {
				arrRs = new String[] {rs};
			}
			
			for (int i=0; i < arrRs.length; i++) {
				String word = arrRs[i].trim();
				word = word.replaceAll("^\'|\'$", "");
				System.out.println("arrRs for " + term + " = " + word);
				JSONObject jo = new JSONObject();
				jo.put("word", word);
				returnArray.put(i, jo);
			}
		}
		logger.log(Level.INFO,"DomainDiscoverySessionController: proxyToNLEX_API returnArray = " + returnArray.toString());
		//return response.getBody();
		return returnArray.toString();
	}

}	