package edu.ncsu.las.rest.collector;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;


import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;



//TODO: Do we need sytem and application level stats???

/**
 * Returns back data for the document and source handlers
 * 
 */
@Controller
public class StatisticsController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	@RequestMapping(value = "rest/{domain}/statistics",  method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getElasticSearchStatistics(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO,"Statistics Controller: get");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.StatisticsController.getElasticSearchStatistics", new JSONObject(), System.currentTimeMillis(),null, httpRequest,domainStr);

		RestTemplate template = new RestTemplate();
		HttpHeaders requestHeaders = new HttpHeaders();
		//requestHeaders.set("Authorization", "Basic am9lOnByZXBhcmUx");
		HttpEntity<String> requestEntity = new HttpEntity<String>("",requestHeaders);
			
		String searchURL = Configuration.getConfigurationProperty(domainStr,  FileStorageAreaType.REGULAR, ConfigurationType.ELASTIC_STOREJSON_LOCATION) + "_stats";
		HttpEntity<String> response = template.exchange(searchURL, HttpMethod.GET, requestEntity, String.class);
		
		// TODO: proper validation, look at response codes.  
		String searchResult = response.getBody();
		return searchResult;
	}

	
}