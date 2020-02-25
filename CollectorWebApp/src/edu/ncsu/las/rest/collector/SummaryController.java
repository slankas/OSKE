package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONObject;
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
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;


/**
 * Returns back data for the document and source handlers
 * 
 */
@Controller
public class SummaryController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	
	@RequestMapping(value = "rest/{domain}/summary/textRankSummary/", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] getDocumentTextrankSummary(HttpServletRequest request, @RequestBody byte[] requestBody,@PathVariable("domain") String domainStr)  throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "Summary Controller - textrank ");		
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		//not instrumenting content as the json object could be any valid json object that has a text field
		this.instrumentAPI("edu.ncsu.las.rest.collector.SummaryController.getDocumentTextrankSummary", new JSONObject(),System.currentTimeMillis(),null, request,domainStr);
		
		JSONObject requestJSON = new JSONObject(new String(requestBody,"UTF-8"));
		double percentage  = requestJSON.getInt("ratio")/100.0;

		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.TEXTRANK_API)+"summary/"+percentage)
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .body(requestJSON.toString().getBytes("UTF-8")).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		return result.toString().getBytes("UTF-8");	
	}		
	
	
	
}