package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.servlet.http.HttpServletRequest;


import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;

/**
 * Returns back data for the 
 * 
 */
@RequestMapping(value = "rest/{domain}/graph/")
@Controller
public class GraphController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "wikipedia/{term}",  headers = "Accept=application/json")
	public @ResponseBody String lookForWikipediaPage(HttpServletRequest httpRequest, @PathVariable("term") String term, @PathVariable("domain") String domainStr ) throws IOException, ValidationException {
		logger.log(Level.INFO,"Graph Controller: wikipedia - " + term);
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		long startTime= System.currentTimeMillis();
		
		RestTemplate template = new RestTemplate();
		HttpHeaders requestHeaders = new HttpHeaders();
		//requestHeaders.set("Authorization", "Basic am9lOnByZXBhcmUx");
		HttpEntity<String> requestEntity = new HttpEntity<String>(requestHeaders);
			
		String searchURL = "https://en.wikipedia.org/wiki/"+term;
		
		JSONObject result;
		try {
			ResponseEntity<String> response = template.exchange(searchURL, HttpMethod.GET, requestEntity, String.class);
			
			HttpStatus status = response.getStatusCode();
			
			result = new JSONObject().put("code", status.value());
		}
		catch (org.springframework.web.client.HttpClientErrorException e) {
			result = new JSONObject().put("code",e.getStatusCode());
		}
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.GraphController.lookForWikipediaPage", new JSONObject().put("term",term), startTime, System.currentTimeMillis(), httpRequest,domainStr);
		
		return result.toString();
	}

	@RequestMapping(value = "triples",  headers = "Accept=application/json")
	public @ResponseBody String getTriples(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr ) throws IOException, ValidationException {
		String queryString = java.net.URLDecoder.decode(httpRequest.getQueryString(), "UTF-8");
		logger.log(Level.INFO,"Graph Controller: getTriples - " + queryString);
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		String result =  proxyToCollectorGraphAPI("/v1/getTriples?"+queryString);
		this.instrumentAPI("edu.ncsu.las.rest.collector.GraphController.getTriples", new JSONObject().put("queryString",queryString), startTime, System.currentTimeMillis(), httpRequest,domainStr);		
		return result;
	}
	
	@RequestMapping(value = "neighbors",  headers = "Accept=application/json")
	public @ResponseBody String getNeighbors(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr ) throws IOException, ValidationException {
		String queryString = java.net.URLDecoder.decode(httpRequest.getQueryString(), "UTF-8");
		logger.log(Level.INFO,"Graph Controller: getTriples - " + queryString);
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		String result =  proxyToCollectorGraphAPI("/v1/getNeighbors?"+queryString);
		this.instrumentAPI("edu.ncsu.las.rest.collector.GraphController.getNeighbors", new JSONObject().put("queryString",queryString), startTime, System.currentTimeMillis(), httpRequest,domainStr);		

		return result;
	}
	
	private String proxyToCollectorGraphAPI(String path) {
		RestTemplate template = new RestTemplate();
		HttpHeaders requestHeaders = new HttpHeaders();
		HttpEntity<String> requestEntity = new HttpEntity<String>(requestHeaders);
			
		String graphURL = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.GRAPH_API)+path;
		
		ResponseEntity<String> response = template.exchange(graphURL, HttpMethod.GET, requestEntity, String.class);

		return response.getBody();		
	}

}