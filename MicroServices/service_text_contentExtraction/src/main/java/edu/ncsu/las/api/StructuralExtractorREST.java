package edu.ncsu.las.api;

import java.net.MalformedURLException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.StructuralExtractrionCache;
import edu.ncsu.las.model.collector.StructuralExtractionRecord;
import edu.ncsu.las.model.collector.StructuralExtractrionCache.CacheObject;
import edu.ncsu.las.util.InternetUtilities;


/**
 * Provides a REST interface to extract content from an HTML page via CSS extractors
 * 
 *
 */
@Path("v1")
public class StructuralExtractorREST {
	private static Logger logger =Logger.getLogger(StructuralExtractorREST.class.getName());
    
	private static long minimumProcessingTimeMS = Long.MAX_VALUE;
	private static long maximumProcessingTimeMS = Long.MIN_VALUE;
	
	private static long minimumAcquisitionTimeMS = Long.MAX_VALUE;
	private static long maximumAcquisitionTimeMS = Long.MIN_VALUE;
	
	private static long totalProcessingTimeMS   = 0;
	private static long totalAcquisitionTimeMS   = 0;

	private static long totalRequestCount = 0;
	
	public static final int MAX_CONCURRENT_REQUESTS = 10;
	public static final int MAX_CACHE_SIZE = 10000;
	private static String userAgent = "";
	
	private static Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS,true);  // limit the # of threads that can be parsing sentences concurrently to just MAX_CONCURRENT_REQUESTS.
	
	private static StructuralExtractrionCache contentCache = new StructuralExtractrionCache(60 * 60, false);
	
	private static StructuralExtractrionCache getContentCache() {
		return contentCache;
	}
	
	public static void setUserAgent(String newValue) {
		userAgent  = newValue;
	}
	
	/**
     * Invalid the content extraction cache for the given domain.
     */
    @GET
    @Path("/invalidateCache/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public String invalidateCache(@PathParam("domain") String domain) {
    	
    	getContentCache().invalidate(domain);
    	JSONObject result = new JSONObject().put("message", "Cache invalidated")
                                            .put("status", "success")
                                            .put("domain", domain);


    	return result.toString();
    }
    

    @POST
    @Path("/createCache/{domain}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createCacheForDomain(@PathParam("domain") String domain,String document) {   	
    	JSONObject result = new JSONObject();
    
    	JSONArray cers = new JSONArray(document);
    	java.util.ArrayList<StructuralExtractionRecord> fullList = new java.util.ArrayList<StructuralExtractionRecord>();
    	for (int i=0;i<cers.length();i++) {
    		fullList.add( new StructuralExtractionRecord(cers.getJSONObject(i)));
    	}
    	java.util.List<StructuralExtractionRecord> records = StructuralExtractionRecord.getRecordsForAnnotation(fullList);
    	CacheObject co = StructuralExtractrionCache.createCacheObject(records);
    	getContentCache().insertIntoCache(domain, co);
    	
    	result.put("status", "success")
    	      .put("domain", domain);
    	
    	return result.toString();
    }
    
    
    
	/**
	 * 
	 * 
	 * Note: this method runs within the context of a semaphore to prevent more than MAX_CONCURRENT_REQUESTS
	 *       from operating simultaneously
	 * 
	 * @param document string representing a JSON object.  The JSON object must have these attributes:
	 *   "html"   : HTML content to extract
	 *   "url"    : URL to utilize as the base for the html document
	 *   "domain" : domain that should be used to get the content extraction records.  must be a valid openKE domain
	 *        
	 * @return
	 */
    @POST
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
     public String processDocument(String document) {   	
    	JSONObject result;
    	String html = null; // by checking null we can see where an exception occurred and not release the semaphore if it occurred prior
    	String domain = null;
    	String url    = null;
    	try {

			long startTime = System.currentTimeMillis();
			semaphore.acquire();
			long acquireTime = System.currentTimeMillis();

			JSONObject jsonDocument = new JSONObject(document);
			html   = jsonDocument.getString("html");		
			url    = jsonDocument.getString("url");
			domain = jsonDocument.getString("domain");
								
			// extract content
			try {
				result = new JSONObject()
		                 .put("content", StructuralExtractionRecord.annotateForStructuralExtraction(url, html,  getContentCache().retrieveDomainStructuralExtractionRecords(domain),false))
		                 .put("status", "success");
			}
			catch (MalformedURLException mue) {
				logger.log(Level.WARNING, "Unable to extract content from page, bad url: "+url);
				result = new JSONObject().put("message", "Unable to extract content from page, bad url: "+url)
		                                 .put("status", "error");
			}
			
			long processingTime = System.currentTimeMillis() - acquireTime;
			long acquisitionTime = acquireTime - startTime;
				
			synchronized (this) {  // an instance is create per request
				minimumProcessingTimeMS = Math.min(processingTime, minimumProcessingTimeMS);
				maximumProcessingTimeMS = Math.max(processingTime, maximumProcessingTimeMS);

				minimumAcquisitionTimeMS = Math.min(acquisitionTime, minimumAcquisitionTimeMS);
				maximumAcquisitionTimeMS = Math.max(acquisitionTime, maximumAcquisitionTimeMS);
				
				totalProcessingTimeMS  += processingTime;
				totalAcquisitionTimeMS  += acquisitionTime;
				totalRequestCount++;
			}
				
			
			semaphore.release();
    	}
    	catch (Exception e) {
    		if (html != null) {semaphore.release(); }
    		logger.log(Level.WARNING, "Unable to process request", e);
			result = new JSONObject().put("message", e.toString())
					                 .put("status", "error");
    	}
    	
		return result.toString();
	}
    
	/**
	 * 
	 * 
	 * Note: this method runs within the context of a semaphore to prevent more than MAX_CONCURRENT_REQUESTS
	 *       from operating simultaneously
	 * 
	 * @param base64URL
	 *        
	 * @return
	 */
    @GET
    @Path("/process/{domain}/{url:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public String processURL(@PathParam("domain") String domain, @PathParam("url") String urlSent) {   	
    	JSONObject result;

    	String url = null; 
    	try {

			long startTime = System.currentTimeMillis();
			semaphore.acquire();
			long acquireTime = System.currentTimeMillis();

			url = urlSent;  // assignment is used to test whether or not to release the semaphore
								
			
			// extract content
			try {
				java.util.List<StructuralExtractionRecord> records = getContentCache().retrieveDomainStructuralExtractionRecords(domain);
				if (records.size() > 0) {
					InternetUtilities.HttpContent page = InternetUtilities.retrieveURL(url, userAgent, 0, true);
					result = new JSONObject()
			                 .put("content", StructuralExtractionRecord.annotateForStructuralExtraction(page, records, false))
			                 .put("status", "success");
				}
				else {
					result = new JSONObject().put("message", "No content extraction records defined for the domain")
                                             .put("status", "error")
                                             .put("domain", domain);
				}
			}
			catch (MalformedURLException mue) {
				logger.log(Level.WARNING, "Unable to extract content from page, bad url: "+url);
				result = new JSONObject().put("message", "Unable to extract content from page, bad url: "+url)
		                                 .put("status", "error");
			}
			
			long processingTime = System.currentTimeMillis() - acquireTime;
			long acquisitionTime = acquireTime - startTime;
				
			synchronized (this) {  // an instance is create per request
				minimumProcessingTimeMS = Math.min(processingTime, minimumProcessingTimeMS);
				maximumProcessingTimeMS = Math.max(processingTime, maximumProcessingTimeMS);

				minimumAcquisitionTimeMS = Math.min(acquisitionTime, minimumAcquisitionTimeMS);
				maximumAcquisitionTimeMS = Math.max(acquisitionTime, maximumAcquisitionTimeMS);
				
				totalProcessingTimeMS  += processingTime;
				totalAcquisitionTimeMS  += acquisitionTime;
				totalRequestCount++;
			}
				
			
			semaphore.release();
    	}
    	catch (Exception e) {
    		if (url != null) {semaphore.release(); }
    		logger.log(Level.WARNING, "Unable to process request", e);
			result = new JSONObject().put("message", e.toString())
					                 .put("status", "error");
    	}
    	
		return result.toString();
	}
    
    
    /**
     * Produces monitoring statistics
     */
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public String processStatistics() {
    	Runtime runtime = Runtime.getRuntime();
    	JSONObject vmStats = new JSONObject().put("usedMemory",  (runtime.totalMemory() - runtime.freeMemory()))
                .put("freeMemory",  runtime.freeMemory())
                .put("totalMemory", runtime.totalMemory())
                .put("maxMemory",   runtime.maxMemory());

    	double averageProcessingTime = ((double) totalProcessingTimeMS)/  Math.max((double)totalRequestCount, 1.0);
    	double averageAcquisitionTime = ((double) totalAcquisitionTimeMS)/  Math.max((double)totalRequestCount, 1.0);
		JSONObject processStats = new JSONObject()
				               .put("minimumProcessingTimeMS", minimumProcessingTimeMS)
				               .put("maximumProcessingTimeMS", maximumProcessingTimeMS)
				               .put("minimumAcquisitionTimeMS", minimumAcquisitionTimeMS)
				               .put("maximumAcquisitionTimeMS", maximumAcquisitionTimeMS)
				               .put("totalProcessingTimeMS", totalProcessingTimeMS)
				               .put("averageProcessingTimeMS", averageProcessingTime)
				               .put("averageAcquisitionTimeMS", averageAcquisitionTime)
				               .put("totalRequests", totalRequestCount);
	
				
    	JSONObject result = new JSONObject().put("process", processStats)
    			                            .put("memory", vmStats );
    	
    	return result.toString();
    }    
    
}
