package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.storage.ElasticSearchREST;

/**
 * Handles citations requests
 */
@Controller
@RequestMapping(value = "rest/{domain}/citations")
public class CitationController extends AbstractRESTController {
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	/**
	 * Get Citation Data
	 */
	@RequestMapping(value = "/data", method = RequestMethod.POST)
	public @ResponseBody byte[] getData(HttpServletRequest request, HttpServletResponse response, @RequestBody String bodyStr, @PathVariable("domain") String domainStr)   throws IOException, ValidationException {
		logger.log(Level.INFO,"Citation Controller: Search : \n" + bodyStr);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		JSONObject queryObject = new JSONObject(bodyStr);
		
		String searchResult = ElasticSearchREST.queryFullTextSearch(domainStr,FileStorageAreaType.REGULAR,queryObject);
		this.instrumentAPI("edu.ncsu.las.rest.collector.CitationController.getData", queryObject, startTime, System.currentTimeMillis(), request,domainStr);
		return searchResult.getBytes("UTF-8");
	}


}