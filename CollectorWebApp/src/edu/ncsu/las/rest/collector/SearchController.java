package edu.ncsu.las.rest.collector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.storage.export.ExportAssistant;

/**
 * Returns back data for the document and source handlers
 * 
 */
@RequestMapping(value = "rest/{domain}/search")
@Controller
public class SearchController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "",  headers = "Accept=application/json")
	public @ResponseBody byte[] passThroughToElasticSearch(HttpServletRequest httpRequest, @RequestBody String query,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		//instrumentation in the next method
		return passThroughToElasticSearchSandboxNormal(httpRequest, query, domainStr);
	}
	
	@RequestMapping(value = "/normal",  headers = "Accept=application/json")
	public @ResponseBody byte[] passThroughToElasticSearchSandboxNormal(HttpServletRequest httpRequest, @RequestBody String query,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO,"Search Controller: Search : \n" + query);  // kept at info so we can it
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		
		JSONObject queryObject = new JSONObject(query);
		long startTime = System.currentTimeMillis();

		//System.out.println(queryObject.toString(4));
		
		String searchResult = ElasticSearchREST.queryFullTextSearch(domainStr,FileStorageAreaType.REGULAR,queryObject);
		this.instrumentAPI("edu.ncsu.las.rest.collector.SearchController.passThroughToElasticSearch", queryObject, startTime, System.currentTimeMillis(), httpRequest,domainStr);

		return searchResult.getBytes("UTF-8");
	}	
	
	@RequestMapping(value = "/sandbox",  headers = "Accept=application/json")
	public @ResponseBody byte[] passThroughToElasticSearchSandbox(HttpServletRequest httpRequest, HttpServletResponse response, @RequestBody String query,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER,"Search Controller: Search : \n" + query);
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		
		JSONObject queryObject = new JSONObject(query);
		long startTime = System.currentTimeMillis();
		//System.out.println(queryObject.toString(4));
		
		String searchResult = ElasticSearchREST.queryFullTextSearch(domainStr,FileStorageAreaType.SANDBOX,queryObject);
		this.instrumentAPI("edu.ncsu.las.rest.collector.SearchController.passThroughToElasticSearchSandbox", queryObject, startTime, System.currentTimeMillis(), httpRequest,domainStr);

		return searchResult.getBytes("UTF-8");
	}	

	@RequestMapping(value = "/mapping", method = RequestMethod.GET)
	public @ResponseBody String getElasticSearchMappings(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER,"Search Controller: get mapping");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.SearchController.getElasticSearchMappings", new JSONObject(), System.currentTimeMillis(), null, httpRequest,domainStr);

		return ElasticSearchREST.retrieveIndexMappings(domainStr, FileStorageAreaType.REGULAR).toString();		
		
	}	

	/**
	 * Used to download a created export file
	 * @param httpRequest
	 * @param httpResponse
	 * @param fileID
	 * @param domainStr
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/download/{fileID:.+}", method = RequestMethod.GET)
	public void downloadExportFile(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			@PathVariable("fileID") String fileID,
			@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER,"Search Controller: get file: " + fileID);
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.SearchController.downloadExportFile", new JSONObject().put("fileID", fileID), System.currentTimeMillis(), null, httpRequest,domainStr);
		
		final int BUFFER_SIZE = 1024*1024; // 1 MB buffer

		//TODO: information leakage in message returned back to user.  (shows full path ...)
		
	    // Create a path to temp export directory
	    Path tempPath = Paths.get(Configuration.getConfigurationProperty(domainStr,ConfigurationType.EXPORT_DOWNLOAD_PATH));

		if (!Files.exists(tempPath)) {
			logger.log(Level.WARNING, "Temp export folder not found");
		}

		Path path = Paths.get(tempPath.toString(), fileID);		
        File downloadFile = new File(path.toString());
        FileInputStream in = new FileInputStream(downloadFile);
        byte[] buffer = new byte[BUFFER_SIZE];
        
        int bytesRead = 0;
        
        // write bytes read from the input stream into the output stream
		httpResponse.setContentType("application/octet-stream");
	    OutputStream out = httpResponse.getOutputStream();

        while ((bytesRead = in.read(buffer)) != -1) {
    	    out.write(buffer,0,bytesRead);
        }		
        out.flush();
        in.close();
	}	
	
	/**
	 * Export data to a local file on the server (user will download it via a link sent through email)
	 * 
	 * @param httpRequest
	 * @param q
	 * @param domainStr
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "/export", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String export(HttpServletRequest httpRequest, @RequestBody String q,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER,"Search Controller: download");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);

		long startTime = System.currentTimeMillis();
		
		JSONObject exportObject = new JSONObject(q);
		JSONObject queryClauseObject = exportObject.getJSONObject("query").getJSONObject("query");
		JSONObject optionObject = exportObject.getJSONObject("options");
		
		User user = this.getUser(httpRequest);
		
		String downloadURL = URI.create(httpRequest.getRequestURL().toString()).resolve(httpRequest.getContextPath()).toString();
		this.instrumentAPI("edu.ncsu.las.rest.collector.SearchController.export", exportObject, startTime, System.currentTimeMillis(), httpRequest,domainStr);
		
		ExportAssistant.initiateDownload(domainStr, exportObject, queryClauseObject, optionObject, user, downloadURL);
		
		return "Success";			
	}



}