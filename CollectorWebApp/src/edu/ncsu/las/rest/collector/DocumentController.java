package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.document.DocumentRouterSecondaryProcessor;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.storage.ElasticSearchDomainDiscoveryQuery;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.util.StringUtilities;


/**
 * Supports document level functionality used across multiple pages
 * TODO: lot of duplicate code - should factor out loading of the document from ElasticSearch
 */
@RequestMapping(value = "rest/{domain}/document")
@Controller
public class DocumentController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "/{documentID}/secondaryProcessingStatus", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentSecondaryStatus(HttpServletRequest request, 
			                                                @PathVariable("documentID") UUID documentID,  
			                                                @PathVariable("domain") String domainStr)  throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentController.getDocumentSecondaryStatus", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		int position = DocumentRouterSecondaryProcessor.getTheSecondaryProcessor().getDocumentPositioninProcessingQueue(documentID);
		int size     = DocumentRouterSecondaryProcessor.getTheSecondaryProcessor().getProcessingQueueSize();
	
		JSONObject result = new JSONObject().put("documentID", documentID)
		                                    .put("queuePosition", position)
		                                    .put("queueSize", size);
		return result.toString().getBytes("UTF-8");
	}		
	
	
	@RequestMapping(value = "/{storageArea}/{type}/{documentID}",  headers = "Accept=application/json")
	public @ResponseBody byte[] getElasticSearchDocument(HttpServletRequest request,@PathVariable("documentID") UUID documentID,  @PathVariable("type") String type, @PathVariable("storageArea") String storageArea, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO,"DocumentController -  : \n" + documentID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentController.getDocumentFromElasticSearch", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, request,domainStr);

		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}

		JSONObject result = ElasticSearchREST.retrieveDocument(domainStr,fsat, documentID.toString());
		if (result != null) {
			return result.toString().getBytes("UTF-8");
		}
		else {
			return new JSONObject().put("found",false).toString().getBytes("UTF-8");
		}
	}
	
	@RequestMapping(value = "/{storageArea}/{type}/{documentID}/statistics", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentStatisticsByUUID(HttpServletRequest request, 
			                                                @PathVariable("documentID") UUID documentID,  
			                                                @PathVariable("type") String type, 
			                                                @PathVariable("storageArea") String storageArea,
			                                                @PathVariable("domain") String domainStr)  throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentController.getDocumentStatistics", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}

		JSONObject jsonDocument = ElasticSearchREST.retrieveDocument(domainStr,fsat, documentID.toString());
		if (jsonDocument == null) {
			return new JSONObject().put("found",false).toString().getBytes("UTF-8");
		}		
		
		
		int numConcepts    = jsonDocument.has("concepts")       ? jsonDocument.getJSONArray("concepts").length(): 0; 
		int numOutlinks    = jsonDocument.has("html_outlinks")  ? jsonDocument.getJSONArray("html_outlinks").length(): 0;
		int numgeotags     = jsonDocument.has("geotag")         ? jsonDocument.getJSONArray("geotag").length(): 0;
		String publishDate = jsonDocument.has("published_date") ? jsonDocument.getJSONObject("published_date").toString() : "";
		
		JSONObject result = new JSONObject().put("textLength", jsonDocument.getString("text").length())
		                                    .put("textMinimizedLength", StringUtilities.eliminateNonSentences(jsonDocument.getString("text")).length())
		                                    .put("totalOutgoingLinks", numOutlinks)
		                                    .put("totalOutgoingLinksDifferentDomain", getOutgoingURLsToAnotherDomain(jsonDocument.getString("domain"), jsonDocument.getJSONArray("html_outlinks")).size())
		                                    .put("numConcepts",  numConcepts)
		                                    .put("numGeoTags",  numgeotags)
		                                    .put("publishDate",  publishDate)
		                                    .put("url", jsonDocument.getString("url"))
		                                    .put("source_uuid", jsonDocument.getString("source_uuid"));
		return result.toString().getBytes("UTF-8");
	}	
	
	
	@RequestMapping(value = "/{storageArea}/{type}/{documentID}/keyword", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentKeywords(HttpServletRequest request, 
			                                                @PathVariable("documentID") UUID documentID,  
			                                                @PathVariable("type") String type, 
			                                                @PathVariable("storageArea") String storageArea,
			                                                @PathVariable("domain") String domainStr,
			                                                @RequestParam("percentage") double percentage)  throws ValidationException, UnirestException, UnsupportedEncodingException, JSONException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentController.getDocumentKeywords", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, request,domainStr);

		
		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}

		JSONObject jsonDocument = ElasticSearchREST.retrieveDocument(domainStr,fsat, documentID.toString());
		if (jsonDocument == null) {
			return new JSONObject().put("found",false).toString().getBytes("UTF-8");
		}		

		JSONObject queryJSON = new JSONObject().put("text", jsonDocument.getString("text"));
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.TEXTRANK_API)+"keyword/"+percentage)
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .body(queryJSON).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");	
	}		
	
	@RequestMapping(value = "/{storageArea}/{type}/{documentID}/keyphrase", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentKeyphrase(HttpServletRequest request, 
			                                                @PathVariable("documentID") UUID documentID,  
			                                                @PathVariable("type") String type, 
			                                                @PathVariable("storageArea") String storageArea,
			                                                @PathVariable("domain") String domainStr,
			                                                @RequestParam("percentage") double percentage)  throws ValidationException, UnirestException, UnsupportedEncodingException, JSONException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentController.getDocumentKeyphrases", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}

		JSONObject jsonDocument = ElasticSearchREST.retrieveDocument(domainStr,fsat, documentID.toString());
		if (jsonDocument == null) {
			return new JSONObject().put("found",false).toString().getBytes("UTF-8");
		}		

		JSONObject queryJSON = new JSONObject().put("text", jsonDocument.getString("text"));
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.TEXTRANK_API)+"keyphrase/"+percentage)
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .body(queryJSON).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");	
	}		
	
	@RequestMapping(value = "/{storageArea}/{type}/{documentID}/annotateDBPedia", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentDBPediaAnnotations(HttpServletRequest request, 
			                                                @PathVariable("documentID") UUID documentID,  
			                                                @PathVariable("type") String type, 
			                                                @PathVariable("storageArea") String storageArea,
			                                                @PathVariable("domain") String domainStr,
			                                                @RequestParam("confidence") double confidence)  throws ValidationException, UnirestException, UnsupportedEncodingException, JSONException {
		logger.log(Level.INFO, "DDSessionController - get document by UUID ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentController.getDocumentDBPediaAnnotations", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, request,domainStr);

		
		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}

		JSONObject jsonDocument = ElasticSearchREST.retrieveDocument(domainStr,fsat, documentID.toString());
		if (jsonDocument == null) {
			return new JSONObject().put("found",false).toString().getBytes("UTF-8");
		}		

		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.DBPEDIA_SPOTLIGHT_API) )
				                                     .header("accept", "application/json")
				                                     .field("text", jsonDocument.getString("text"))
				                                     .field("confidence", confidence)
				                                     .asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");	
	}		
	
	
	/**
	 * Returns the requested document from a scratchpad export with sources
	 * 
	 * @param request
	 * @param uuid
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/{uuid}/scratchpadDoc", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentsForScratchpadExport(HttpServletRequest request,@PathVariable("uuid") UUID uuid, @PathVariable("domain") String domainStr) throws  IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - get scratchpad doc: "+uuid);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainDiscoverySessionController.getDocumentsForScratchpadExport", new JSONObject().put("uuid",uuid.toString()), System.currentTimeMillis(), null, request,domainStr);
		
		List<JSONObject> documents = ElasticSearchDomainDiscoveryQuery.getDocumentsForScratchpad(domainStr,uuid.toString());
		JSONArray result = new JSONArray();
		for (JSONObject o: documents) {
			result.put(o);
		}
		
		return result.toString().getBytes("UTF-8");
	}
	
	
	/**
	 * Returns the set of outgoing URLS that were on the document, but don't include
	 * the domain of this document in the url
	 * 
	 * @return set of URLs represented as strings.
	 */
	public static Set<String> getOutgoingURLsToAnotherDomain(String domain, JSONArray outlinks) {
		HashSet<String> results = new HashSet<String>();
		for (int i=0; i< outlinks.length(); i++) {
			if (outlinks.getString(i).contains(domain) == false) {
				results.add(outlinks.getString(i));
			}
		}
		return results;
	}
	
}