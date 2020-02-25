package edu.ncsu.las.api;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.nlp.Document;




/**
 * Provides a REST interface to perform natural language processing via the Stanford CoreNLP
 * 
 */
@Path("v1")
public class StanfordCoreNLPREST {
	private static Logger logger =Logger.getLogger(StanfordCoreNLPREST.class.getName());
    
	private static long minimumProcessingTimeMS = Long.MAX_VALUE;
	private static long maximumProcessingTimeMS = Long.MIN_VALUE;
	
	private static long minimumAcquisitionTimeMS = Long.MAX_VALUE;
	private static long maximumAcquisitionTimeMS = Long.MIN_VALUE;
	
	private static long totalProcessingTimeMS   = 0;
	private static long totalAcquisitionTimeMS   = 0;

	private static long totalRequestCount = 0;
	
	public static final int MAX_CONCURRENT_REQUESTS = 5;
	public static final int MAX_CACHE_SIZE = 10000;
	
	
	private static Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS,true);  // limit the # of threads that can be parsing sentences concurrently to just MAX_CONCURRENT_REQUESTS.
	
	private static com.google.common.cache.Cache<String,JSONObject> _sentenceCache;
	
	static {			
		_sentenceCache = com.google.common.cache.CacheBuilder.newBuilder()
				                   .maximumSize(MAX_CACHE_SIZE)
				                   .recordStats()
				                   .expireAfterAccess(10000, TimeUnit.DAYS)   //basically, we want to keep things, in cache, but using LRU access as the policy
				                   .build();
	}
	
	/**
	 * 
	 * 
	 * Note: this method runs within the context of a semaphore to prevent more than MAX_CONCURRENT_REQUESTS
	 *       from operating simultaneously
	 * 
	 * @param document string representing a JSON object.  The JSON object must have "text" attribute.
	 * @param filter optional query that has the value of "none", "min", or "max".  If set to min or max,
	 *        this will filter out the relations such that only "nodes" with the minimum amount or words
	 *        or the maximum amout of words will be returned.
	 *        
	 * @return
	 */
    @POST
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
     public String processDocument(String document, @DefaultValue("none") @QueryParam("filterRelations") String filter, 
    		                                        @DefaultValue("-1") @QueryParam("maxSentenceSize") Integer maxSentenceSize) {   	
    	JSONObject result;
    	String text = null; // by checking null we can see where an exception occurred and not releast the semaphore if it occurred prior
    	
    	try {
			JSONObject jsonDocument = new JSONObject(document);
			text = jsonDocument.getString("text");	
			String cacheKey = text+";"+filter+";"+maxSentenceSize;

			result = _sentenceCache.getIfPresent(cacheKey);
			
			if (result == null) {
				long startTime = System.currentTimeMillis();
				semaphore.acquire();
				long acquireTime = System.currentTimeMillis();
				
				JSONArray spacyEntities  = jsonDocument.has("spacy")   ? jsonDocument.getJSONObject("spacy").getJSONArray("entities") : new JSONArray();
				JSONArray dbpediaEntries = jsonDocument.has("dbpedia") ? jsonDocument.getJSONArray("dbpedia") : new JSONArray();
			
				Document myDoc = Document.parse(jsonDocument.getString("text"),2, spacyEntities, dbpediaEntries);

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
				
				result =  myDoc.toJSONObject();
				
				_sentenceCache.put(cacheKey, result);
				semaphore.release();
				logger.log(Level.INFO,  "Process time: "+ processingTime + ", Acquisition time: "+ acquisitionTime);
			}
			else {
				logger.log(Level.INFO, "Cache hit");
			}
    	}
    	catch (Exception e) {
    		if (text != null) {semaphore.release(); }
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

		com.google.common.cache.CacheStats cacheStats = _sentenceCache.stats();
		JSONObject cStats = new JSONObject().put("averageLoadPenalty",   cacheStats.averageLoadPenalty())
				                            .put("evictionCount",        cacheStats.evictionCount())
				                            .put("hitCount",             cacheStats.hitCount())
				                            .put("hitRate",              cacheStats.hitRate())
				                            .put("missCount",            cacheStats.missCount())
				                            .put("missRate",             cacheStats.missRate())
				                            .put("requestCount",         cacheStats.requestCount())
				                            .put("totalLoadTimeNanoSec", cacheStats.totalLoadTime())
				                            .put("size",                 _sentenceCache.size());		
				
    	JSONObject result = new JSONObject().put("process", processStats)
                                            .put("cacheStatistics", cStats )
    			                            .put("memory", vmStats );
    	
    	return result.toString();
    }    
    
}
