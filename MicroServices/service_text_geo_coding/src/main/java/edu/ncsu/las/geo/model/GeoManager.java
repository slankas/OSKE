package edu.ncsu.las.geo.model;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class GeoManager {
	private static Logger logger =Logger.getLogger(GeoManager.class.getName());

	private GeoProvider[] providers;
	private int providerCount;
	private long maxSleepTime;

	
	private com.google.common.cache.Cache<String,JSONObject> locationCache;
	
	public GeoManager(JSONArray providerConfigurations, int cacheSize, long maxSleepTimeMS) {
		providers = new GeoProvider[providerConfigurations.length()];
		
		for (int i=0;i < providerConfigurations.length(); i++) {
			providers[i] = new GeoProvider(providerConfigurations.getJSONObject(i));
		}
		providerCount = providers.length;
		maxSleepTime = maxSleepTimeMS;
			
		locationCache = com.google.common.cache.CacheBuilder.newBuilder()
				                   .maximumSize(cacheSize)
				                   .recordStats()
				                   .expireAfterAccess(10000, TimeUnit.DAYS)   //basically, we want to keep things, in cache, but using LRU access as the policy
				                   .build();
	}
	
	private int requestCount = 0;
	private long lastRequestTime = 0;
	
	public synchronized JSONObject geocodeLocation(String location) {
		JSONObject result = locationCache.getIfPresent(location);
		
		if (result == null) {
			long requestTime = System.currentTimeMillis();
			for (int loopCount=0;loopCount<2;loopCount++) { //basically, we are trying twice.  First see if any of the providers are ready, than sleep if necessary and try again
				for (int i=0;i<providerCount; i++) {
					requestCount++;
					if (providers[requestCount%providerCount].canProcess()) {
						result = providers[requestCount%providerCount].geocodeLocation(location);
						locationCache.put(location, result);
						lastRequestTime = requestTime;
						return result;
					}
				}
				try {
					long sleepTime =  Math.max(  (lastRequestTime+maxSleepTime)-System.currentTimeMillis(), 1);
					logger.log(Level.INFO, "Sleeping for "+sleepTime);
					TimeUnit.MILLISECONDS.sleep(sleepTime);
				}
				catch (InterruptedException ie) {
					logger.log(Level.WARNING, "Unable to sleep", ie);
				}
			}
			lastRequestTime = requestTime;
			// we weren't able to run, return an error message
			result = new JSONObject().put("status", "error").put("message", "unable to call any providers");
			return result;  
		}
		else {
			return result;
		}
	}
	
	public JSONObject generateStatistics() {
		JSONArray providerStats = new JSONArray();
		for (int i=0;i<providers.length;i++) {
			providerStats.put(providers[i].createStatisticsResponse());
		}
		com.google.common.cache.CacheStats cacheStats = locationCache.stats();
		JSONObject cStats = new JSONObject().put("averageLoadPenalty",   cacheStats.averageLoadPenalty())
				                            .put("evictionCount",        cacheStats.evictionCount())
				                            .put("hitCount",             cacheStats.hitCount())
				                            .put("hitRate",              cacheStats.hitRate())
				                            .put("missCount",            cacheStats.missCount())
				                            .put("missRate",             cacheStats.missRate())
				                            .put("requestCount",         cacheStats.requestCount())
				                            .put("totalLoadTimeNanoSec", cacheStats.totalLoadTime())
				                            .put("size",                 locationCache.size());
		
		Runtime runtime = Runtime.getRuntime();
		JSONObject vmStats = new JSONObject().put("usedMemory",  (runtime.totalMemory() - runtime.freeMemory()))
				                             .put("freeMemory",  runtime.freeMemory())
				                             .put("totalMemory", runtime.totalMemory())
				                             .put("maxMemory",   runtime.maxMemory());
				
		
		JSONObject result = new JSONObject().put("providers", providerStats)
				                            .put("cacheStatistics", cStats )
				                            .put("memory", vmStats );
		return result;
	}	
}
