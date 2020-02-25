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

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Handles requests to the NLP service API
 */
@RequestMapping(value = "rest/{domain}/nlp")
@Controller
public class NLPController extends AbstractRESTController{
	
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	
	@RequestMapping(value = "/process", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody byte[] passThroughToAPI(HttpServletRequest request, @RequestBody String requestData, @PathVariable("domain") String domainStr) throws JsonGenerationException, JsonMappingException, IOException, ValidationException, UnirestException {
		logger.log(Level.INFO, "NLPController - process");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONObject jsonDocument = new JSONObject(requestData);
		JSONObject queryJSON = new JSONObject().put("text", jsonDocument.getString("text"));
		HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainStr,ConfigurationType.NLP_API)+"v1/process?filterRelations=max")
				                                     .header("accept", "application/json")
				                                     .header("Content-Type", "application/json")
				                                     .body(queryJSON).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.NLPController.processRequest", new JSONObject(), startTime, System.currentTimeMillis(), request,domainStr);

		return result.toString().getBytes("UTF-8");			
	}		

		
}	
