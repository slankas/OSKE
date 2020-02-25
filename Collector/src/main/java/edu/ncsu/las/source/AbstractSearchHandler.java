package edu.ncsu.las.source;


import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.reflections.Reflections;


/**
 * Provides a common "process" method that the various search handlers can implement.
 *    
 */
public abstract class AbstractSearchHandler extends WebSourceHandler implements DomainDiscoveryInterface {
	//static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	/**
	 * Returns true if this source Handler has the capability to generate search results
	 * 
	 * @return
	 */
	public boolean isSearchBased() {
		return true;
	}		
		
	@Override
	public boolean supportsDomainDiscovery() {
		return false;
	}	
	

	public  java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();

		String sourceHandlerName = this.getSourceHandlerName();
		
		if (!configuration.has(sourceHandlerName)) {
			errors.add("No \""+sourceHandlerName+"\" object defined.");
		}
		else if (!configuration.getJSONObject(sourceHandlerName).has("length")) {
			errors.add(sourceHandlerName +".length not present.  Length must be a number between 1 and "+this.getMaximumNumberOfSearchResults());
		}
		else {
			int length =-1;
			try {
				length = configuration.getJSONObject(sourceHandlerName).getInt("length");
				
				if (length < 1 || length > this.getMaximumNumberOfSearchResults()) {
					errors.add(sourceHandlerName +".length invalid.  Length must be a number between 1 and "+this.getMaximumNumberOfSearchResults()+": "+length);
				}
			}
			catch (Throwable t) {
				errors.add(sourceHandlerName +".length invalid.  Length must be a number between 1 and "+this.getMaximumNumberOfSearchResults()+": "+length );
			}
		}	
		
		
		return super.validateConfiguration(domainName, primaryFieldValue,configuration);
	}		
	
	/*
	public static final int UNASSIGNED_LENGTH = -100;
	
	private int _numSearchResults = UNASSIGNED_LENGTH;
	
	public final void setNumSearchResults(int newSearchLength) {
		_numSearchResults = newSearchLength;
	}
	*/

	/**
	 * Get the number of search results. This first tries to get this directly from a setting a num search results
	 * If that has not been set, then from a job configuration.
	 * 
	 * @return
	 */
	/*
	public final int getNumSearchResults() {
		if (_numSearchResults != UNASSIGNED_LENGTH ) { return _numSearchResults; }
		else { return this.getNumSearchResultsFromConfiguration(); }
	}
	*/
	
	/**
	 * Using a configuration setting, retrieves the maximum number of search results that will be computed
	 * 
	 * @return number of search results
	 * @throws runtime exceptio if the length can not be retrieved
	 */
	public final int getNumSearchResultsFromConfiguration()  {
		JSONObject configuration = this.getJob().getConfiguration();
		try {
			int length = configuration.getJSONObject(this.getSourceHandlerName()).getInt("length");
			return length;
		}
		catch (Throwable t) {
			throw new RuntimeException(this.getSourceHandlerName() +" SourceHandler - Unable to retieve length from configruation: "+t.getMessage());
		}
	}			
		
	@Override
	public final void process() {
		List<String> errors = this.validateConfiguration(this.getDomainInstanceName(),this.getJob().getPrimaryFieldValue(), this.getJob().getConfiguration());
		if (errors.size() > 0) {
			srcLogger.log(Level.SEVERE, "Unable to process "+this.getSourceHandlerName()+" - invalid parameters: "+this.getJob().getName());
			for (String error: errors) {
				srcLogger.log(Level.SEVERE, "        "+error);
			}
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.INVALID_PARAMS, "Unable to process "+this.getSourceHandlerName()+" - invalid parameters: "+this.getJob().getName());
			this.setJobHistoryStatus(JobHistoryStatus.INVALID_PARAMS);
			return;
		}
		String domain = this.getDomainInstanceName();
		
		int numberOfSearchResults = this.getNumSearchResultsFromConfiguration();
		java.util.List<SearchRecord> records = this.generateSearchResults(domain, this.getJob().getPrimaryFieldValue(), this.getJob().getConfiguration(), numberOfSearchResults, new JSONObject()); // typically the number of results are part of the configuration
		
		if (records != null) {
			java.util.List<String> seeds = records.stream().map(SearchRecord::getUrl).collect(Collectors.toList());
			
			this.processInternal(seeds);
		}
		else {
			srcLogger.log(Level.SEVERE, "Unable to process "+this.getSourceHandlerName()+" (null search results): "+this.getJob().getPrimaryFieldValue());
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process DuckDuckGo (null search results): "+this.getJob().getPrimaryFieldValue());
			this.setJobHistoryStatus(JobHistoryStatus.ERRORED);
			return;
		}
	}
	
	
	
	/**
	 * Searches for an AbstractSearchHandler with a given name and returns an instance of that source handler.
	 * Null is returned if the source handler is not found.
	 * 
	 * @param name
	 * @return
	 */
	public static AbstractSearchHandler getSourceHandler(String name) {
		Reflections reflections = new Reflections("edu.ncsu.las");    
		java.util.Set<Class<? extends AbstractSearchHandler>> classes = reflections.getSubTypesOf(AbstractSearchHandler.class);
	
		for (Class<? extends AbstractSearchHandler> c: classes) {
			if (!Modifier.isAbstract(c.getModifiers())) {
				try {
					AbstractSearchHandler shi = c.newInstance();
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
