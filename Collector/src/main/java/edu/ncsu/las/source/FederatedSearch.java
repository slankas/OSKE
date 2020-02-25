package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.json.JSONUtilities;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



import org.json.JSONObject;




/**
 * FederatedSearch combines DuckDuckGo, Google, and MillionShort into a single handler.  
 * Search results are returned in a round-robin fashion of the individual results
 * (eg., the first search result of google, then duckduckgo, then millionshort,
 *       followed by the second search result ....
 *    
 */
public class FederatedSearch extends AbstractSearchHandler implements SourceHandlerInterface {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"federatedSearch\": { \"length\": 20 }, \"duckduckgo\":{\"length\":20},\"google\":{\"length\":20},\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true,}") ;
	private static final String SOURCE_HANDLER_NAME = "federatedSearch";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Federated Search";
	
	//private static final String MS_SOURCE_HANDLER_NAME = "millionshort";
	
	public static final int MAX_NUMBER_OF_RESULTS = 150;

	
	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}
			put("google", new SourceParameter("google", "JSON Object containing Google specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("google.length",   new SourceParameter("google.length",   "number of results to return.  Must be between 10 and "+GoogleHandler.MAX_NUMBER_OF_RESULTS+" in multiples of 10.",true,"20",false,SourceParameterType.INT,false,true));
		    put("google.numDay",   new SourceParameter("google.numDay",   "number of days previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.numWeek",  new SourceParameter("google.numWeek",  "number of weeks previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.numMonth", new SourceParameter("google.numMonth", "number of months previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.numYear",  new SourceParameter("google.numYear",  "number of years previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.beforeDate", new SourceParameter("google.beforeDate", "Results must be published before this date,  m/d/yyyy format. Precedence over numX fields",false,"6/15/2015",false,SourceParameterType.STRING,false,false));
		    put("google.afterDate",  new SourceParameter("google.afterDate",  "Results must be published after this date,  m/d/yyyy format. Precedence over numX fields",false,"5/15/2015",false,SourceParameterType.STRING,false,false));

			put("duckduckgo", new SourceParameter("duckduckgo", "JSON Object containing Duck Duck Go specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("duckduckgo.length", new SourceParameter("duckduckgo.length", "number of results to return.  Must be between 1 and 180.",true,"20",false,SourceParameterType.INT,false,true));
		    
		    put(SOURCE_HANDLER_NAME, new SourceParameter(SOURCE_HANDLER_NAME, "JSON Object containing length for the federated search results.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put(SOURCE_HANDLER_NAME+".length", new SourceParameter(SOURCE_HANDLER_NAME + ".length", "number of results to return.  Must be between 1 and 180.",true,"20",false,SourceParameterType.INT,false,true));
		    
		    
		    /*
			put(MS_SOURCE_HANDLER_NAME, new SourceParameter(MS_SOURCE_HANDLER_NAME, "JSON Object containing MillionShort specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT));
		    put(MS_SOURCE_HANDLER_NAME+".length", new SourceParameter(MS_SOURCE_HANDLER_NAME+".length", "number of results to return.  Must be between 8 and "+MillionShort.MAX_NUMBER_OF_RESULTS+" in multiples of 8.",true,""+MillionShort.MAX_NUMBER_OF_RESULTS,false,SourceParameterType.INT));
			put(MS_SOURCE_HANDLER_NAME+".filterTopN", new SourceParameter(MS_SOURCE_HANDLER_NAME+".filterTopN", "Filters out the top N web sites.  Must be between 1 and 1,000,000.  Default:1000000",false,"100",false,SourceParameterType.INT));
			put(MS_SOURCE_HANDLER_NAME+".includeFiltered", new SourceParameter(MS_SOURCE_HANDLER_NAME+".includeFiltered", "Should results filtered by being in the top N be included?  true/false.  default: false",false,"false",false,SourceParameterType.BOOLEAN));
			*/
		}};
	
	@Override
	public int getMaximumNumberOfSearchResults() {
		return MAX_NUMBER_OF_RESULTS;
	}
	
		
	@Override
	public boolean supportsDomainDiscovery() { 
		return true;
	}
		
	@Override
	public JSONObject getHandlerDefaultConfiguration() {
		return SOURCE_HANDLER_CONFIGURATION;
	}

	@Override
	public String getSourceHandlerName() {
		return SOURCE_HANDLER_NAME;
	}
	
	@Override
	public String getSourceHandlerDisplayName() {
		return SOURCE_HANDLER_DISPLAY_NAME;
	}

	@Override
	public String getDescription() {
		return "Provides an interface to get search results from duckduckgo and google. Search results are round-robin from each of the underlying handlers. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.SEARCH_TERMS;
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}

	
	/**
	 * Generate the possible search results that will be used as the seeds for a search
	 * Scraps MillionShort to get these results.
	 * 
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String searchCriteria, JSONObject configuration, int numberOfSearchResults, JSONObject advConfiguration) {  
		java.util.LinkedHashSet<SearchRecord> overallResults = new java.util.LinkedHashSet<SearchRecord>();
		
		java.util.List<SearchRecord> googleResults;      
		java.util.List<SearchRecord> duckduckgoResults;  
		//java.util.List<SearchRecord> millionshortResults;
		
		try { 
			googleResults = (new GoogleHandler()).generateSearchResults(domain, searchCriteria, configuration, numberOfSearchResults/2, advConfiguration);
			if (googleResults == null) { googleResults = new java.util.ArrayList<SearchRecord>(); }
		}
		catch (Exception e) {
			srcLogger.log(Level.WARNING, "unable to get results from google: ",e);
			googleResults = new java.util.ArrayList<SearchRecord>();
		}
		try { 
			duckduckgoResults   = (new DuckDuckGoHandler()).generateSearchResults(domain, searchCriteria, configuration, numberOfSearchResults/2, advConfiguration);
			if (duckduckgoResults == null) { duckduckgoResults = new java.util.ArrayList<SearchRecord>(); }
		}
		catch (Exception e) {
			srcLogger.log(Level.WARNING, "unable to get results from duckduckgo: ",e);
			duckduckgoResults = new java.util.ArrayList<SearchRecord>();
		}	
		/*
		try { 
			millionshortResults = (new MillionShort()).generateSearchResults(domain, searchCriteria, configuration, numberOfSearchResults/2, advConfiguration);
			if (millionshortResults == null) { millionshortResults = new java.util.ArrayList<SearchRecord>(); }
		}
		catch (Exception e) {
			srcLogger.log(Level.WARNING, "unable to get results from millionshort: ",e);
			millionshortResults = new java.util.ArrayList<SearchRecord>();
		}			
		*/
		int maxFoundResults = Math.max(googleResults.size(), duckduckgoResults.size());  //Math.max(duckduckgoResults.size(), millionshortResults.size()));
		
		for (int i=0;i<maxFoundResults;i++) {
			SearchRecord gsr = i < googleResults.size()       ? googleResults.get(i)       : null;
			SearchRecord dsr = i < duckduckgoResults.size()   ? duckduckgoResults.get(i)   : null;
			//SearchRecord msr = i < millionshortResults.size() ? millionshortResults.get(i) : null;
			
			if (gsr != null && !overallResults.contains(gsr)) {overallResults.add(gsr); }
			if (dsr != null && !overallResults.contains(dsr)) {overallResults.add(dsr); }
			//if (msr != null && !overallResults.contains(msr)) {overallResults.add(msr); }
		}
		
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>(overallResults);
						
		return result;
	}	
		
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject originalConfiguration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		JSONObject alteredConfiguration = JSONUtilities.copyDeep(originalConfiguration);
		
		alteredConfiguration.remove(DuckDuckGoHandler.SOURCE_HANDLER_NAME);
		alteredConfiguration.remove(FederatedSearch.SOURCE_HANDLER_NAME);
		errors.addAll((new GoogleHandler()).validateConfiguration(domainName, primaryFieldValue, alteredConfiguration));
		
		alteredConfiguration  = JSONUtilities.copyDeep(originalConfiguration);
		alteredConfiguration.remove(GoogleHandler.SOURCE_HANDLER_NAME);
		alteredConfiguration.remove(FederatedSearch.SOURCE_HANDLER_NAME);
		errors.addAll((new DuckDuckGoHandler()).validateConfiguration(domainName, primaryFieldValue, alteredConfiguration));
		
		//configuration  = JSONUtilities.copyDeep(originalConfiguration);
		//configuration.remove(GoogleHandler.SOURCE_HANDLER_NAME);
		//configuration.remove(DuckDuckGoHandler.SOURCE_HANDLER_NAME);
		//configuration.remove(FederatedSearch.SOURCE_HANDLER_NAME);
		//errors.addAll((new MillionShort()).validateConfiguration(domainName, primaryFieldValue, configuration));
		
		alteredConfiguration  = JSONUtilities.copyDeep(originalConfiguration);
		//configuration.remove(GoogleHandler.SOURCE_HANDLER_NAME);
		//configuration.remove(DuckDuckGoHandler.SOURCE_HANDLER_NAME);
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, alteredConfiguration));
		
		return errors;
	}
	
		
	public static void main(String args[]) throws Exception {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");

		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);
		//mainCollector.getConfiguration();
		FederatedSearch fs = new FederatedSearch();
		
		List<SearchRecord> results = fs.generateSearchResults("symposium","SCADA cyber", new JSONObject(), MAX_NUMBER_OF_RESULTS, new JSONObject() );
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
	
}

