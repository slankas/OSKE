package edu.ncsu.las.api;

import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.time.model.HeidelTime;


/**
 * Provides a REST interface to perform temporal tagging using HeidelTime
 * 
 *
 */
@Path("v1")
public class HeidelTimeREST {
	private static Logger logger =Logger.getLogger(HeidelTimeREST.class.getName());
	
	private static Semaphore semaphore = new Semaphore(3,true);  // limit the # of threads that can be parsing sentences concurrently to just 3.
    
	private static long minimumResponseTimeMS = Long.MAX_VALUE;
	private static long maximumResponseTimeMS = Long.MIN_VALUE;
	private static long totalResponseTimeMS   = 0;
	
	private static long minimumAcquireTimeMS = Long.MAX_VALUE;
	private static long maximumAcquireTimeMS = Long.MIN_VALUE;
	private static long totalAcquireTimeMS   = 0;
	
	private static long totalRequestCount = 0;
	
	/**
	 * 
	 * 
	 * @param document string representing a JSON object.  The JSON object must have "text" attribute
	 *                 as well another JSON object accessed by "published_date" that has a "date" field.
	 *                 The date field should be in ISO8601 format.   
	 *                 
	 * @return
	 */
    @POST
    @Path("/extract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
     public String processDocument(String document) {
    	JSONObject result;
    	
    	try {
    		long startTime = System.currentTimeMillis();
    		
    		semaphore.acquire();
    		long acquisitionTime = System.currentTimeMillis();

			JSONObject jsonDocument = new JSONObject(document);
			String text = jsonDocument.getString("text");		
			Instant date = Instant.parse(jsonDocument.getJSONObject("published_date").getString("date"));

			HeidelTime ht = HeidelTime.getTheHeidelTimePool().borrowObject();
			JSONArray ja = ht.processDocument(text,date);
			HeidelTime.getTheHeidelTimePool().returnObject(ht);
			
			long processingTime = System.currentTimeMillis() - acquisitionTime;
			long acquiringTime  = acquisitionTime - startTime;
			
			synchronized (this) {  // an instance is create per request
				minimumResponseTimeMS = Math.min(processingTime, minimumResponseTimeMS);
				maximumResponseTimeMS = Math.max(processingTime, maximumResponseTimeMS);
				totalResponseTimeMS  += processingTime;
				
				minimumAcquireTimeMS = Math.min(acquiringTime, minimumAcquireTimeMS);
				maximumAcquireTimeMS = Math.max(acquiringTime, maximumAcquireTimeMS);
				totalAcquireTimeMS  += acquiringTime;
				
				totalRequestCount++;
			}
			
			result = new JSONObject().put("result", ja)
					                 .put("status", "success");
    	}
    	catch (Exception e) {
    		logger.log(Level.WARNING, "Unable to process request", e);
			result = new JSONObject().put("message", e.toString())
					                 .put("status", "error");
    	}
    	finally {
    		semaphore.release();
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

    	double averageTime = ((double) totalResponseTimeMS)/  Math.max((double)totalRequestCount, 1.0);
    	double averageAcquireTime = ((double) totalAcquireTimeMS)/  Math.max((double)totalRequestCount, 1.0);
    	
		JSONObject processStats = new JSONObject()
				               .put("minimumResponseTimeMS", minimumResponseTimeMS)
				               .put("maximumResponseTimeMS", maximumResponseTimeMS)
				               .put("totalResponseTimeMS", totalResponseTimeMS)
				               .put("averageResponseTimeMS", averageTime)
				               .put("minimumAcquireTimeMS", minimumAcquireTimeMS)
				               .put("maximumAcquireTimeMS", maximumAcquireTimeMS)
				               .put("totalAcquireTimeMS", totalAcquireTimeMS)
				               .put("averageAcquireTimeMS", averageAcquireTime)
				               .put("totalRequests", totalRequestCount);

		JSONObject pool = new JSONObject().put("idle", HeidelTime.getTheHeidelTimePool().getNumIdle())
				                          .put("active", HeidelTime.getTheHeidelTimePool().getNumActive());
				
    	JSONObject result = new JSONObject().put("process", processStats)
    			                            .put("memory", vmStats )
    			                            .put("pool", pool);
    	
    	return result.toString();
    }    
    
}
