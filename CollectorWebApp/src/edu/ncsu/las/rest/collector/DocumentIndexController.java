package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.DiscoveryIndex;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;


/**
 * Returns back data for the document and source handlers
 * 
 */
@RequestMapping(value = "rest/{domain}/documentIndex")
@Controller
public class DocumentIndexController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	
	@RequestMapping(value = "",  headers = "Accept=application/json")
	public @ResponseBody byte[] getListOfAvailableIndexes(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO,"DocumentIndex Controller: get all indexes");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentIndexController.getAvailableDocumentIndexes", new JSONObject(), System.currentTimeMillis(), null, httpRequest,domainStr);
		
		
		JSONArray results = DiscoveryIndex.retrieveAvailableIndexes(domainStr, FileStorageAreaType.REGULAR);
		
		return results.toString().getBytes("UTF-8");
	}
	
	//TODO: This needs to be moved. DocumentController has the same logic as well.  (although, this is about to be OBE'd when we combine areas...)
	private FileStorageAreaType convertStorageAreaToType(String storageArea) throws ValidationException {
		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}
		return fsat;
	}	

	@RequestMapping(value = "/{storageArea}/{documentIndexID}", method = RequestMethod.HEAD, headers = "Accept=application/json")
	public @ResponseBody  ResponseEntity<String> checkIfDocumentIndexExists(HttpServletRequest request, @PathVariable("domain") String domainStr, @PathVariable("storageArea") String storageArea,  @PathVariable("documentIndexID") UUID documentIndexID) throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "getListOfAvailableIndexes: checking index exists");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentIndexController.checkIfDocumentIndexExists", new JSONObject().put("documentIndexID", documentIndexID), System.currentTimeMillis(), null, request,domainStr);
		
		boolean exists = DiscoveryIndex.hasIndex(documentIndexID);
		if (exists) {
			return new ResponseEntity<String>(HttpStatus.OK) ;
		}
		else {return new ResponseEntity<String>(HttpStatus.NOT_FOUND) ; }
	}		
	
	
	@RequestMapping(value = "/{storageArea}/{documentIndexID}", method = RequestMethod.GET)
	public @ResponseBody byte[]  getDocumentIndex(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr, @PathVariable("storageArea") String storageArea,  @PathVariable("documentIndexID") UUID documentIndexID) throws IOException, ValidationException {
		logger.log(Level.INFO,"Document Index Controller: get document index");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentIndexController.getDocumentIndex", new JSONObject().put("documentIndexID", documentIndexID), System.currentTimeMillis(), null, httpRequest,domainStr);
		
		JSONObject result = DiscoveryIndex.retreiveIndex(documentIndexID);
		if (result != null) {
			return result.toString().getBytes("UTF-8");
		}
		else {
			return new JSONObject().put("found",false).toString().getBytes("UTF-8");
		}		
	}	
	
	@RequestMapping(value = "/{storageArea}/{documentIndexID}", method = RequestMethod.DELETE)
	public @ResponseBody byte[]  deleteDocumentIndex(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr, @PathVariable("storageArea") String storageArea,  @PathVariable("documentIndexID") UUID documentIndexID) throws IOException, ValidationException {
		logger.log(Level.INFO,"Document Index Controller: get document index");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentIndexController.deleteDocumentIndex", new JSONObject().put("documentIndexID", documentIndexID), System.currentTimeMillis(), null, httpRequest,domainStr);
		
		//TODO: should validate that we are the owner or an administrator for the domain

		boolean result = DiscoveryIndex.deleteIndex(documentIndexID);
		if (result) {
			return new JSONObject().put("status","success").toString().getBytes("UTF-8");
		}
		else {
			return new JSONObject().put("status","failed").toString().getBytes("UTF-8");
		}		
	}		
	
	
	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody byte[]  createDocumentIndex(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr, @RequestBody String queryData) throws IOException, UnirestException, ValidationException {
		logger.log(Level.INFO,"Document Index Controller: create document Index");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		
		String userEmail = this.getEmailAddress(httpRequest);
		
		JSONObject json = new JSONObject(queryData);

		String indexTitle      = json.getString("title");
		
		String documentIndexID = json.getString("documentIndexID");
		String documentArea    = json.getString("documentArea");
		
		FileStorageAreaType fsat = this.convertStorageAreaToType(documentArea);
		String urlAndIndex = Configuration.getConfigurationProperty(domainStr,fsat,ConfigurationType.ELASTIC_STOREJSON_LOCATION);
		if (Configuration.getConfigurationProperty(domainStr, ConfigurationType.ELASTIC_REST_INTERNAL) != null && !Configuration.getConfigurationProperty(domainStr, ConfigurationType.ELASTIC_REST_INTERNAL).equals("")) {
			urlAndIndex = Configuration.getConfigurationProperty(domainStr,fsat,ConfigurationType.ELASTIC_STOREJSON_LOCATION_INTERNAL);
		}
				
		json.put("urlAndIndex", urlAndIndex);
		json.put("type","_doc");
		
		json.put("sessionID", documentIndexID);  
		json.put("creatorID", this.getEmailAddress(httpRequest));

		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentIndexController.createDocumentIndex", json, System.currentTimeMillis(), null, httpRequest,domainStr);

		logger.log(Level.INFO, "Discovery Index query: "+ json.toString());

		DiscoveryIndex.deleteIndex(UUID.fromString(documentIndexID)); // this is needed so that the index will not be reported as having been created
		
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.TEXTRANK_API)+"index").header("accept", "application/json")
				.body(json).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		result.put("documentArea", documentArea);
		result.put("documentIndexID", documentIndexID);
		
		DiscoveryIndex.waitForCompletion(domainStr, userEmail, indexTitle, documentIndexID, documentArea);
				
		return result.toString().getBytes("UTF-8");
	}


	

	@RequestMapping(value =  "/{storageArea}/{diocumentIndexID}/status", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] checkCreateIndexStatus(HttpServletRequest httpRequest,
			                                           @PathVariable("diocumentIndexID") UUID documentIndexID,
			                                           @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "DocumentIndexController - checking database for index");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentIndexController.checkCreateIndexStatus", new JSONObject().put("documentIndexID", documentIndexID.toString()), System.currentTimeMillis(), null, httpRequest,domainStr);
		
		boolean hasIndex = DiscoveryIndex.hasIndex(documentIndexID);
		if (hasIndex) {
			return new JSONObject().put("status","True").toString().getBytes("UTF-8");
		}
		else {
			return new JSONObject().put("status","False").toString().getBytes("UTF-8");
		}
	
	}

}