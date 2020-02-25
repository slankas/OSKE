package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.RSS;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.source.DomainDiscoveryHandler;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.storage.StorageProcessor;

import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Handles requests for the Domain Discovery Page.
 */
@RequestMapping(value = "rest/{domain}/analytics")
@Controller
public class AnalyticController extends AbstractRESTController{
	
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	
	@RequestMapping(value = "/{storageArea}/{documentType}/{documentID}/rss", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] findRSS(HttpServletRequest request,@RequestBody String postData,
			                            @PathVariable("storageArea") String storageArea,
			                            @PathVariable("documentType") String documentType,
			                            @PathVariable("documentID") UUID documentID,
			                            @PathVariable("domain") String domainStr)  throws JsonGenerationException, JsonMappingException, IOException, ValidationException {
		logger.log(Level.INFO, "DDSessionController - create RSS Job: "+documentID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONObject postDataObject = new JSONObject(postData);
				
		FileStorageAreaType fsat = null;
		if      (storageArea.equals("normal")) { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else {
			throw new ValidationException("Invalid storage area defined");
		}
		
		if (documentType.equals("_doc") == false) {
			throw new ValidationException("Invalid document type defined");
		}
		
		// load the data
		byte[] data = StorageProcessor.getTheStorageProcessor().loadRawData(fsat,domainStr, documentID.toString());
		String content = null;
		if (data != null) {
			content = new String(data,"UTF-8");
		}
		Set<RSS.RSSEntry> feeds = RSS.findRSSFeeds(postDataObject.getString("url"), content, SourceHandlerInterface.getNextUserAgent(domainStr));
		
		JSONArray feedArray = new JSONArray();
		for (RSS.RSSEntry f: feeds) {
			feedArray.put(f.toJSON());
		}
		JSONObject result = new JSONObject().put("feeds", feedArray).put("url", postDataObject.getString("url"));
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.AnalyticController.findRSS", result, startTime,System.currentTimeMillis(), request,domainStr);

		
		return result.toString().getBytes("UTF-8");
	}			
		
	
	/**
	 * Calls the DB Spotlight API Service for the passed in text and confidence level.
	 * 
	 * @param request
	 * @param body json object with two parameters, text and confidence level (between 0.0 and 1.0) as a double
	 * @param domainStr
	 * @return
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ValidationException
	 * @throws UnirestException
	 */
	@RequestMapping(value = "/textAnalytics/annotateResources", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody  byte[] annotateTextWithDBPedia(HttpServletRequest request,@RequestBody String body, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "Analytic Controller - annotate DBPedia resources");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONObject requestJSON = new JSONObject(body);
		
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.DBPEDIA_SPOTLIGHT_API) )
                .header("accept", "application/json")
                .field("text", requestJSON.getString("text"))
                .field("confidence", requestJSON.getDouble("confidence"))
                .asJson();
		
		JSONObject result = jsonResponse.getBody().getObject();
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.AnalyticController.annotateTextWithDBPedia", new JSONObject(), startTime,System.currentTimeMillis(), request,domainStr);
		
		return result.toString().getBytes("UTF-8");	
	}	
	
	
	@RequestMapping(value = "/{sessionID}/execution/{executionID}/document/{documentID}/summary", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentTextrankSummary(HttpServletRequest request,@PathVariable("sessionID") UUID sessionID,
			                                              @PathVariable("executionID") int executionNumber,
			                                              @PathVariable("documentID") UUID documentID, 
			                                              @RequestParam("percentage") double percentage,
			                                              @PathVariable("domain") String domainStr)  throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "Analytic Controller - summarize text ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
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
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.AnalyticController.getDocumentTextrankSummary", new JSONObject().put("uuid", documentID), startTime,System.currentTimeMillis(), request,domainStr);
		return result.toString().getBytes("UTF-8");	
		
	}		
	
	@RequestMapping(value = "/whois/{domainName:.+}", method = RequestMethod.GET)
	public @ResponseBody byte[] passThroughToWhoisAPI(HttpServletRequest request, @PathVariable("domain") String domainStr, @PathVariable("domainName") String domainName) throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "Analytic Controller - pass to whois API");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		HttpResponse<JsonNode> jsonResponse = Unirest.get(Configuration.getConfigurationProperty(domainStr,ConfigurationType.WHOIS_API)+"v1/find/"+domainName)
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .asJson();
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.AnalyticController.getWhoIs", new JSONObject().put("domain", domainName), startTime,System.currentTimeMillis(), request,domainStr);
		
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");			
	}			
	
}	