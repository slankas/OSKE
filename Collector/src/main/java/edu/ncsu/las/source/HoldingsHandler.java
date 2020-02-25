package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.util.DateUtilities;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * HoldingsHandler provides a mechanism for discovery sessions to search and retrieve content from the current holdings
 * which are stored in the ElasticSearch database.
 * 
 */
public class HoldingsHandler extends AbstractSearchHandler implements SourceHandlerInterface {

	/** 
	 * what is the maximum number of results that Faroo can return.  
	 * Must be an even multiple of 10. 
	 * Faroo can only return 10 records at a time, with a second delay between each result. 
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 100;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{}");
	
	private static final String SOURCE_HANDLER_NAME = "holdings";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Holdings Search";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("holdings", new SourceParameter("holdings", "JSON Object containing holding specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("holdings.length", new SourceParameter("holdings.length", "number of results to return.  Must be between 10 and 100 in multiples of 10.",true,"100",false,SourceParameterType.INT,false,true));
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
	public boolean supportsJob() {
		return false;
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
		return "Allows users to search the current holdings within domain discovery sessions.  Jobs may not be created with this source, but SearchAlerts are feasible.";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.SEARCH_TERMS;
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}
	
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();	
		
		if (!configuration.has("holdings")) {
			errors.add("No holdings object defined.");
		}
		
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));
		return errors;
	}
	
	/**
	 * Generate the possible search results that will be used as the seeds for a search
	 * 
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String queryText, JSONObject configuration, int numberOfResults, JSONObject advConfiguration) {
				
		java.util.List<SearchRecord> results = new java.util.ArrayList<SearchRecord>();

		JSONObject query = new JSONObject().put("from", 0)
				                           .put("size", numberOfResults)
				                           .put("query",
				                         	    new JSONObject().put("bool", new JSONObject().put("must", new JSONObject().put("match", new JSONObject().put("text", queryText)))				                         	    		            )
				                        	   )
				                           .put("highlight", new JSONObject().put("fields", new JSONObject().put("text", new JSONObject().put("number_of_fragments", 1).put("fragment_size", 400))))
				                           .put("sort", new JSONArray().put(new JSONObject().put("_score", new JSONObject().put("order", "desc"))));
		
		String searchResult = ElasticSearchREST.queryFullTextSearch(domain,FileStorageAreaType.REGULAR,query);
		JSONObject elasticResults = new JSONObject(searchResult);
		JSONArray hits = elasticResults.getJSONObject("hits").getJSONArray("hits");
		for (int i=0;i< hits.length();i++) {
			JSONObject docObject = hits.getJSONObject(i).getJSONObject("_source");
			String description = hits.getJSONObject(i).getJSONObject("highlight").getJSONArray("text").getString(0);
			//SearchRecord sr = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
			//Document doc = Document.createFromJSON(domain,docObject);
			
			SearchRecord sr = new SearchRecord(docObject.optString("html_title",docObject.getString("url")), docObject.getString("url"), description, i+1,"holdings"); //TODO: need to store source in elasticSearch records
			sr.setPublishedDateTime( DateUtilities.getFromString(docObject.optString("crawled_dt","")) );
			sr.setStatus("new");  // will be set to "crawled" in the loop of DomainDiscoveryHandler.run()
			sr.setDocument(docObject);
			sr.setDocumentUUID(UUID.fromString(docObject.getString("source_uuid")));	
			results.add(sr);
		}	
		
		return results;
	}

		
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);		
		

		
		JSONObject configuration = new JSONObject().put("holdings",new JSONObject().put("length", 10));
		
		HoldingsHandler fh = new HoldingsHandler();
		List<SearchRecord> results =fh.generateSearchResults("sandboxv2","cyber",configuration,10,new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
}
