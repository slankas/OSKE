package edu.ncsu.las.source;


import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.ConfigurationTypeInterface;
import edu.ncsu.las.model.collector.type.SourceParameter;

/**
 * Interface class that represents "source" handlers, which are used to pull data
 * from multiple sources.
 * 
 */
public interface SourceHandlerInterface  {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	static java.util.HashMap<String,java.util.ArrayList<String>> _useragents = new java.util.HashMap<>();
	
	/**
	 * Represents what should be the primary field label on the job edit screen. If validURL, the error check
	 * should exist to ensure that the value is a valid URL...
	 * 
	 *
	 */
	public static enum ParameterLabelType {
		URL("URL",false,true),
		SEARCH_TERMS("Search Terms",false,false),
		NOT_APPLICABLE("na",true,false);
		
		private String _label;
		private boolean _hidden;
		private boolean _validURL;

		private JSONObject _jsonObject; 
		
		private ParameterLabelType(String label, boolean hidden, boolean validURL) {
			_label = label;
			_hidden = hidden;
			_validURL = validURL;
			
			_jsonObject = new JSONObject().put("label", _label).put("hidden", _hidden).put("validURL", _validURL);
		}
		
		public String toString() { return _label; }

		public JSONObject toJSONObject() {
			return _jsonObject;
		}
		
		public static ParameterLabelType getEnum(String label) {
			return ParameterLabelType.valueOf(label.toUpperCase());
		}
		
		public boolean isHidden() { 
			return _hidden;
		}
		
		public boolean useValidURL() {
			return _validURL;
		}
	}
	
	/**
	 * Define that we can set the user agent for any source handler.  
	 * Added to allow this to be generically set in the domain discovery porcessing area.
	 * 
	 * @param userAgent user agent value to be used when making web requests.
	 */
	public abstract void setUserAgent(String userAgent);
	
	
	/**
	 * Get random user agent string for crawlers
	 * 
	 * @param the domain
	 * @return randomized user agent string
	 */
	public static String getNextUserAgent(String domain) {
		String agentString;
		
		if (_useragents.containsKey(domain) == false) {
			JSONArray useragentList = Configuration.getConfigurationPropertyAsArray(domain,ConfigurationType.USER_AGENT);
			java.util.ArrayList<String> userAgents = new java.util.ArrayList<String>();
			for (int i=0;i< useragentList.length();i++) {
				userAgents.add(useragentList.getString(i));
			}
			Collections.sort(userAgents);
			_useragents.put(domain, userAgents);
		}
		
		java.util.ArrayList<String> agentList = _useragents.get(domain);
		java.security.SecureRandom randomGenerator = new java.security.SecureRandom();
		int randomIndex = randomGenerator.nextInt(agentList.size());
		agentString = agentList.get(randomIndex);
		
		
		return agentString;
	}
	
	
	public static int UNLIMITED_INSTANCES = -1;
	
	static final int MAX_ENTRIES = 1000;
	
	/** 
	 * limited size set of MAX_ENTRIES.  use this to avoid crawling linked URL's that have been visited recently.
	 * (as determined by when the url was first accessed.
	 * 
	 */
	java.util.Set<String> visitedURLs = java.util.Collections.newSetFromMap(new java.util.LinkedHashMap<String, Boolean>(){
		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(java.util.Map.Entry<String, Boolean> eldest) {
	        return size() > MAX_ENTRIES;
	    }
	});
	


	

	/**
	 * Called to actually execute the source handler to process data.
	 * 
	 * This should create an internal Runnable.run() and then launch a new thread to do the processing. 
	 */
	public void process();
	
	/**
	 * Default configuration for the Source Handler.  See getJobConfiguration to access the configuration for a specific job.
	 * 
	 * @return
	 */
	public JSONObject getHandlerDefaultConfiguration();
	
	public String getSourceHandlerName();
	
	public String getSourceHandlerDisplayName();
	
	public String getDescription();
	
	
	public ParameterLabelType getPrimaryLabel();
	
	/**
	 * Is this search interface available for current use? 
	 * Implemented so that a flag can simply be set for a class, rather than removing the class from the project itself when a source handler interface 
	 * needs to be taken offline.
	 * 
	 * @return true if the source handler is available for use, false if the source handler is offline.
	 */
	public boolean isOnline();
	
	/**
	 * 
	 * @return
	 */
	public default String getSampleConfiguration() {
		return this.getSourceParameterRepository().convertParametersToSampleString();
	}
	
	/**
	 * Maintains a key-value listing of of configuration parameters and their settings.
	 * 
	 * @return
	 */
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters();
	

	
	/**
	 * Returns true if this source Handler is interactive with an outside system/agent/user
	 * 
	 * @return
	 */
	public default boolean isInteractive() {
		return false;
	}	
	
	/**
	 * Returns true if this source Handler is a service.  ServiceHandlers that are a service
	 * are automatically started at runtime and execute in their own thread.  Directory Watcher is 
	 * the best example.
	 * 
	 * @return
	 */
	public default boolean isService() {
		return false;
	}
	
	/**
	 * Returns true if this source Handler directly primarily uses an API to accomplish its
	 * work versus a web crawler. 
	 * 
	 * @return
	 */
	public default boolean isAPIDriven() {
		return false;
	}

	/**
	 * Returns true if this source Handler has the capability to generate search results
	 * 
	 * @return
	 */
	public default boolean isSearchBased() {
		return false;
	}	
	
	/**
	 * Returns true if this source Handler should be used within the context of domain discovery.
	 * 
	 * @return
	 */
	public default boolean supportsDomainDiscovery() {
		return false;
	}	
	
	/**
	 * Returns true if this source Handler can be used for jobs
	 * 
	 * @return
	 */
	public default boolean supportsJob() {
		return true;
	}		
	
	/**
	 * What is the maximum number of search results that this handler can produce?
	 * 
	 * @return if not a search-based handler, -1  should be returned
	 */
	public int getMaximumNumberOfSearchResults();	
	
	/**
	 * Returns the number of instances allowed by the particular source handler.
	 * 
	 * If there are no limits, UNLIMITED_INSTANCES is returned.
	 * 
	 * @return number of allowed instances
	 */
	public default int getMaximumNumberOfInstances() {
		return UNLIMITED_INSTANCES;
	}
		
	
	/** Used to start services, which typically kick-off their own threads for processing. */
	public default void startService() {
		throw new IllegalAccessError("StartService can only be called for SourceHandlers that are services");
	}
		
	/**
	 * Returns true if this source Handler can provide test results for a configuration.
	 * False otherwise.
	 * 
	 * @return
	 */
	public default boolean isConfigurationTestable() {
		return false;
	}	
	
	/**
	 * Returns true if this source Handler should be the default for the domain discovery area.
	 * Defaults to true.
	 * 
	 * @return
	 */
	public default boolean isDefaultDomainDiscoverySearchMethod() {
		return false;
	}	
	
	
	/**
	 * Tests the passed in configuration and returns the results as a JSON object
	 * 
	 * @param configution
	 * @return
	 */
	public default JSONObject testConfiguration(String jobName, String domainInstanceName, JSONObject configution, String url) {
		return new JSONObject();
	}		

	/**
	 * What's the job that's currently associated with this SourceHandler?
	 */
	public Job getJob();
	public JSONObject getJobConfiguration();
	
	
	public default String getJobConfigurationFieldAsString(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsString(fieldName, this.getJobConfiguration());
	}
	
	public default String getJobConfigurationFieldAsString(String fieldName, String defaultValue) {
		return this.getSourceParameterRepository().getFieldAsString(fieldName, this.getJobConfiguration(), defaultValue);
	}

	/**
	 * To use this, the string name must match between the domain-level configuration and the job configuration
	 * 
	 * @param key
	 * @return
	 */
	public default String getJobConfigurationPropertyAsString(ConfigurationTypeInterface key) {
		return this.getJobConfigurationFieldAsString(key.toString(), Configuration.getConfigurationProperty(this.getJob().getDomainInstanceName(), key));
	}	
	
	public default boolean getJobConfigurationFieldAsBoolean(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsBoolean( fieldName, this.getJobConfiguration());
	}
	
	public default boolean getJobConfigurationFieldAsBoolean(String fieldName, boolean defaultValue) {
		return this.getSourceParameterRepository().getFieldAsBoolean( fieldName, this.getJobConfiguration(), defaultValue);
	}	
	
	public default int getJobConfigurationFieldAsInteger(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsInteger( fieldName, this.getJobConfiguration());
	}
	
	public default int getJobConfigurationFieldAsInteger(String fieldName, int defaultValut) {
		return this.getSourceParameterRepository().getFieldAsInteger( fieldName, this.getJobConfiguration(),defaultValut);
	}	
	
	public default long getJobConfigurationFieldAsLong(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsLong( fieldName, this.getJobConfiguration());
	}	
	
	public default long getJobConfigurationFieldAsLong(String fieldName, long defaultValue) {
		return this.getSourceParameterRepository().getFieldAsLong( fieldName, this.getJobConfiguration(), defaultValue);
	}	
	
	public default double getJobConfigurationFieldAsDouble(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsDouble( fieldName, this.getJobConfiguration());
	}
	
	public default double getJobConfigurationFieldAsDouble(String fieldName, double defaultValut) {
		return this.getSourceParameterRepository().getFieldAsDouble( fieldName, this.getJobConfiguration(),defaultValut);
	}		
	
	/**
	 * To use this, the string name must match between the domain-level configuration and the job configuration
	 * 
	 * @param key
	 * @return
	 */
	public default double getJobConfigurationPropertyAsDouble(ConfigurationTypeInterface key) {
		return this.getJobConfigurationFieldAsDouble(key.toString(), Configuration.getConfigurationPropertyAsDouble(this.getJob().getDomainInstanceName(), key));
	}
	
	public default JSONObject getJobConfigurationFieldAsJSONObject(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsJSONObject( fieldName, this.getJobConfiguration());
	}		
	
	public default JSONArray getJobConfigurationFieldAsJSONArray(String fieldName) {
		return this.getSourceParameterRepository().getFieldAsJSONArray( fieldName, this.getJobConfiguration());
	}		
	
	public static JSONObject getFieldAsJSONObject(String fieldName, JSONObject obj) {
		String fields[] = fieldName.split("\\.");
		
		JSONObject temp = obj;
		for (int i=0;i <(fields.length-1); i++) {
			temp = temp.getJSONObject(fields[i]);
		}
		return temp.getJSONObject(fields[fields.length-1]);
	}		
	
	
	public default JSONObject toJSON() {
		JSONObject result = new JSONObject()
				.put("sourceHandlerName", this.getSourceHandlerName())
				.put("description", this.getDescription())
				.put("sourceHandlerConfiguration", this.getHandlerDefaultConfiguration())
				.put("configurationParameters", this.getConfigurationParameters());
		return result;
	}
	
	
	/** 
	 * Validates whether or not the configuration has been appropriately setup by a user
	 * 
	 * @param primaryField - this is the "url" / "search term" field on the job page
	 * @param configuration
	 * @return List of an issues with the configuration.  If the list is empty then no issues exist with the config
	 */
	public java.util.List<String> validateConfiguration(String domainName, String primaryField, JSONObject configuration);
	
	
	/**
	 *  validates whether or not the number of instances allowed for the source handler is violated.
	 * 
	 * @param jobObject
	 * @return
	 */
	public default java.util.List<String> validateInstantCount(String domainInstanceName, Job job) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		if (this.getMaximumNumberOfInstances() != UNLIMITED_INSTANCES) {
			java.util.List<Job> currentJobs = Job.getJobs(domainInstanceName,this);
			
			if (currentJobs.size() >= this.getMaximumNumberOfInstances()) {
				boolean found = false;
				UUID passedInJobID = job.getID(); 
				for (Job j: currentJobs) {
					if (j.getID().equals(passedInJobID)) {
						found = true;
						break;
					}
					
				}
				
				if (!found) {
					errors.add("Job's source handler already has the maximum number of instances.");
				}
				
			}
		}
		
		return errors;
	}	




	
	public SourceParameterRepository getSourceParameterRepository();
	
}
