package edu.ncsu.las.model.collector;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.google.common.cache.CacheLoader;

import edu.ncsu.las.collector.Collector;



/**
 * 
 *
 */
public class StructuralExtractrionCache {
	static Logger logger =Logger.getLogger(Collector.class.getName());
	
	public static class CacheObject {
		public java.util.List<StructuralExtractionRecord> extractionRecords;
	}
	
	private com.google.common.cache.LoadingCache<String,CacheObject> _cache;  //cache by domains
	
	/**
	 * 
	 * @param expirationSeconds
	 * @param useInstrumentation 
	 */
	public StructuralExtractrionCache(int expirationSeconds, boolean useInstrumentation) {
		_cache = com.google.common.cache.CacheBuilder.newBuilder()
			.expireAfterWrite(expirationSeconds, TimeUnit.SECONDS)
            .recordStats()
            .build(new CacheLoader<String, CacheObject>() {
                public CacheObject load(String domainInstanceName) throws Exception {
                    return StructuralExtractrionCache.createCacheObject(domainInstanceName, useInstrumentation);
                  }
                });
	}
	
	public static CacheObject createCacheObject(String domain, boolean useInstrumentation) {
		CacheObject result = new CacheObject();
		
		if (useInstrumentation) {	Instrumentation.createAndSendEvent(domain, "structuralExtractionCache.load", System.currentTimeMillis(), null, null, null); }
		
		result.extractionRecords = StructuralExtractionRecord.getRecordsForAnnotation(domain);

		return result;
		
	}
	
	public static CacheObject createCacheObject(java.util.List<StructuralExtractionRecord> records) {
		CacheObject result = new CacheObject();
		
		result.extractionRecords = records;

		return result;
	}
	
	public void insertIntoCache(String domain, CacheObject value) {
		_cache.put(domain, value);
	}
	
	public void invalidate(String domainInstanceName) {
		_cache.invalidate(domainInstanceName);
	}
	
	public java.util.List<StructuralExtractionRecord> retrieveDomainStructuralExtractionRecords(String domainInstanceName) {
		try {
			return _cache.get(domainInstanceName).extractionRecords;
		}
		catch (ExecutionException ee) {
			logger.log(Level.SEVERE, "Unable to retrieve extraction records: "+domainInstanceName);
			return null;
		}
	}	
}
