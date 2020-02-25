package edu.ncsu.las.source;

import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.reflections.Reflections;

import edu.ncsu.las.model.collector.SearchRecord;


/**
 * Interface class that represents providers to the DomainDiscovery area.
 *   
 */
public interface DomainDiscoveryInterface  extends SourceHandlerInterface {
	
	
	/**
	 * Generate the possible search results that will be used as the seeds for a search or for a discovery session
	 * 
	 * @param domain
	 * @param urlOrSearchTerms
	 * @param configuration
	 * @param numResults
	 * @param advConfiguration
	 * 
	 * @return list of generated search records
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String urlOrSearchTerms, JSONObject configuration, int numResults, JSONObject advConfiguration);
	
	
	/**
	 * Searches for an AbstractSearchHandler with a given name and returns an instance of that source handler.
	 * Null is returned if the source handler is not found.
	 * 
	 * @param name
	 * @return
	 */
	public static DomainDiscoveryInterface getSourceHandler(String name) {
		final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
		
		Reflections reflections = new Reflections("edu.ncsu.las");    
		java.util.Set<Class<? extends DomainDiscoveryInterface>> classes = reflections.getSubTypesOf(DomainDiscoveryInterface.class);
	
		for (Class<? extends DomainDiscoveryInterface> c: classes) {
			if (!Modifier.isAbstract(c.getModifiers())) {
				try {
					DomainDiscoveryInterface shi = c.newInstance();
					if (shi.getSourceHandlerName().equals(name)){
						return shi;
					}
				} catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
					srcLogger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
				}
			}
		}
		
		return null;
	}		
	
}

