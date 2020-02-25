package edu.ncsu.las.model.collector.concept;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.cache.CacheLoader;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Instrumentation;

public class ConceptCache {
	static Logger logger =Logger.getLogger(Collector.class.getName());
	
	public static class CacheObject {
		public ConceptCategoryTable domainCategoryTable;
		public java.util.ArrayList<Concept> domainConceptList;
	}
	
	private com.google.common.cache.LoadingCache<String,CacheObject> _cache;  //cache by domains
	
	/**
	 * 
	 * @param expirationSeconds
	 */
	public ConceptCache(int expirationSeconds) {
		_cache = com.google.common.cache.CacheBuilder.newBuilder()
			.expireAfterWrite(expirationSeconds, TimeUnit.SECONDS)
            .recordStats()
            .build(new CacheLoader<String, CacheObject>() {
                public CacheObject load(String domainInstanceName) throws Exception {
                    return ConceptCache.createCacheObject(domainInstanceName);
                  }
                });
	}
	
	public static CacheObject createCacheObject(String domain) {
		CacheObject result = new CacheObject();
		
		Instrumentation.createAndSendEvent(domain, "concepts.load", System.currentTimeMillis(), null, null, null);
		

		List<Concept> conceptList = Concept.getAllConcepts(domain);
		java.util.ArrayList<Concept> patterns =  new java.util.ArrayList<Concept>(); 
		for (Concept concept: conceptList) {
			try {
				concept.getRegexPattern(); // force the regex pattern to be created
				patterns.add(concept);
			}
			catch (java.util.regex.PatternSyntaxException pse) {
				logger.log(Level.SEVERE, "Unable to compile regular expression: "+pse);	
				logger.log(Level.SEVERE, concept.toJSONObject().toString(4));
			}
		}
		
		result.domainConceptList  = patterns;
		result.domainCategoryTable = new ConceptCategoryTable(domain);
		
		return result;
		
	}
	
	public void invalidate(String domainInstanceName) {
		_cache.invalidate(domainInstanceName);
	}
	
	public java.util.ArrayList<Concept> retrieveDomainConcepts(String domainInstanceName) {
		try {
			return _cache.get(domainInstanceName).domainConceptList;
		}
		catch (ExecutionException ee) {
			logger.log(Level.SEVERE, "Unable to retrieve concepts: "+domainInstanceName);
			return null;
		}
	}

	public ConceptCategoryTable retrieveDomainCategoryTable(String domainInstanceName) {
		try {
			return _cache.get(domainInstanceName).domainCategoryTable;
		}
		catch (ExecutionException ee) {
			logger.log(Level.SEVERE, "Unable to retrieve concepts: "+domainInstanceName);
			return null;
		}
	}
	
	
}
