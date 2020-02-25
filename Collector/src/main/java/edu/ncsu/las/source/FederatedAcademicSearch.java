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
 * FederatedSearch combines results from Google Scholar and Microsoft Academic
 *   
 */
public class FederatedAcademicSearch extends AbstractSearchHandler implements SourceHandlerInterface {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{ \"federatedAcademicSearch\": { \"length\": 20 },  \"microsoftacademic\" : { \"length\" : 20 },\"googlescholar\" : { \"length\" : 20 }, \"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true,}");
	
	private static final String SOURCE_HANDLER_NAME = "federatedAcademicSearch";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Federated Academic Search";
	
	private static final String MS_SOURCE_HANDLER_NAME = "microsoftacademic";
	
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
			put("googlescholar", new SourceParameter("googlescholar", "JSON Object containing Google Scholar specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("googlescholar.length", new SourceParameter("googlescholar.length", "number of results to return.  Must be between 10 and "+GoogleScholarHandler.MAX_NUMBER_OF_RESULTS+" in multiples of 10.",true,"80",false,SourceParameterType.INT,false,true));
	    
			put(MS_SOURCE_HANDLER_NAME, new SourceParameter(MS_SOURCE_HANDLER_NAME, "JSON Object containing Microsoft Academic specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put(MS_SOURCE_HANDLER_NAME+".length", new SourceParameter(MS_SOURCE_HANDLER_NAME+".length", "number of results to return.  Must be between 8 and "+MicrosoftAcademic.MAX_NUMBER_OF_RESULTS+" in multiples of 8.",true,"80",false,SourceParameterType.INT,false,true));
		    
			put(SOURCE_HANDLER_NAME, new SourceParameter(SOURCE_HANDLER_NAME, "JSON Object containing Microsoft Academic specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put(SOURCE_HANDLER_NAME+".length", new SourceParameter(SOURCE_HANDLER_NAME+".length", "number of results to return.  Must be between 8 and "+MicrosoftAcademic.MAX_NUMBER_OF_RESULTS+" in multiples of 8.",true,"80",false,SourceParameterType.INT,false,true));

		    
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
		return "Provides an interface to get search results from Google Scholar and Microsoft Academic. Search results are round-robin from each of the underlying handlers. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
		
		java.util.List<SearchRecord> googleScholarResults;      
		java.util.List<SearchRecord> msAcademicResults;  
		
		try { 
			googleScholarResults = (new GoogleScholarHandler()).generateSearchResults(domain, searchCriteria, configuration, numberOfSearchResults/2, advConfiguration);
		}
		catch (Exception e) {
			srcLogger.log(Level.WARNING, "unable to get results from google scholar: ",e);
			googleScholarResults = new java.util.ArrayList<SearchRecord>();
		}
		try { 
			msAcademicResults   = (new MicrosoftAcademic()).generateSearchResults(domain, searchCriteria, configuration, numberOfSearchResults/2, advConfiguration);
		}
		catch (Exception e) {
			srcLogger.log(Level.WARNING, "unable to get results from duckduckgo: ",e);
			msAcademicResults = new java.util.ArrayList<SearchRecord>();
		}		
		
		int maxFoundResults = Math.max(googleScholarResults.size(), msAcademicResults.size());
		
		for (int i=0;i<maxFoundResults;i++) {
			SearchRecord gsr = i < googleScholarResults.size() ? googleScholarResults.get(i)       : null;
			SearchRecord msr = i < msAcademicResults.size()    ? msAcademicResults.get(i) : null;
			
			if (gsr != null) {
				if (gsr.getUrl() == null) {
					srcLogger.log(Level.WARNING,"Google Scholar Search result with null url");
				}
				else {
					if (!overallResults.contains(gsr)) {overallResults.add(gsr); }
				}
			}
			if (msr != null) {
				if (msr.getUrl() == null) {
					srcLogger.log(Level.WARNING,"MS Academic Search result with null url");
				}
				else {
					if (!overallResults.contains(msr)) {overallResults.add(msr); }
				}
			}
		}

		
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>(overallResults);
						
		return result;
	}	
		
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject originalConfiguration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		JSONObject alteredConfiguration = JSONUtilities.copyDeep(originalConfiguration);
		
		alteredConfiguration.remove(MicrosoftAcademic.SOURCE_HANDLER_NAME);
		alteredConfiguration.remove(FederatedAcademicSearch.SOURCE_HANDLER_NAME);
		errors.addAll((new GoogleScholarHandler()).validateConfiguration(domainName, primaryFieldValue, alteredConfiguration));
		
		alteredConfiguration = JSONUtilities.copyDeep(originalConfiguration);
		alteredConfiguration.remove(GoogleScholarHandler.SOURCE_HANDLER_NAME);
		alteredConfiguration.remove(FederatedAcademicSearch.SOURCE_HANDLER_NAME);
		errors.addAll((new MicrosoftAcademic()).validateConfiguration(domainName, primaryFieldValue, alteredConfiguration));
		
		alteredConfiguration = JSONUtilities.copyDeep(originalConfiguration);
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, alteredConfiguration));
		return errors;
	}
	
		
	public static void main(String args[]) throws Exception {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");

		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);
		//mainCollector.getConfiguration();
		FederatedAcademicSearch fs = new FederatedAcademicSearch();
		
		List<SearchRecord> results = fs.generateSearchResults("sandboxv2","SCADA cyber", new JSONObject(), MAX_NUMBER_OF_RESULTS, new JSONObject() );
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
	
}

