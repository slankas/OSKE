package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.json.JSONUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;


/**
 * MicrosoftAcademicHandler provides an interface to web search results from MicrosoftAcademic by scraping the page results.
 * 
 *   
 */
public class MicrosoftAcademic extends AbstractSearchHandler implements SourceHandlerInterface {

	/** 
	 * what is the maximum number of results that microsoftacademic can return.  
	 * Must be an even multiple of 8. 
	 * MicrosoftAcademic can only return 8 records at a time, with a 100 millisecond delay between each result. 
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 100;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"microsoftacademic\" : { \"length\" : 20 },\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	public static final String SOURCE_HANDLER_NAME = "microsoftacademic";
	public static final String SOURCE_HANDLER_DISPLAY_NAME = "Microsoft Academic";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put(SOURCE_HANDLER_NAME, new SourceParameter(SOURCE_HANDLER_NAME, "JSON Object containing Microsoft Academic specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put(SOURCE_HANDLER_NAME+".length", new SourceParameter(SOURCE_HANDLER_NAME+".length", "number of results to return.  Must be between 8 and "+MAX_NUMBER_OF_RESULTS+" in multiples of 8.",true,"100",false,SourceParameterType.INT,false,true));
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
		return "Provides an interface to web search result from MicrosoftAcademic through scraping. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
				
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));
		return errors;
	}
	
	
	/**
	 * Generate the possible search results that will be used as the seeds for a search
	 * Scraps microsoftacademic to get a list of all possible search results
	 * 
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @param numberOfResult
	 * @param advConfiguration.  Not utilized.  may be null or an empty JSON object
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String queryText, JSONObject configuration, int numberOfResults, JSONObject advConfiguration) {
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();

		numberOfResults = Math.min(MAX_NUMBER_OF_RESULTS, numberOfResults); 
		String uri = this.createURIString(queryText, numberOfResults);
		String apiKey = Configuration.getConfigurationProperty(domain, ConfigurationType.AZURE_ACADEMIC_KEY);
		
		JSONObject responseObject;
		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.get(uri)
						          .header("Ocp-Apim-Subscription-Key", apiKey)
						          .asJson();
			responseObject = jsonResponse.getBody().getObject();
		} catch (UnirestException e) {
			srcLogger.log(Level.SEVERE, "MicrosoftAcademic exception: " + e.toString());
			return null;
		}
		
		if (responseObject.has("error")) {
			srcLogger.log(Level.SEVERE, "MicrosoftAcademic error: " + responseObject.optString("messsage"));
			return null;
		}
		
		int position = 0;
		JSONArray entities = responseObject.getJSONArray("entities");
		for (int i=0; i < entities.length(); i++) {
			JSONObject entity = entities.getJSONObject(i);
			JSONObject extendedAttributes = new JSONObject(entity.getString("E"));
			entity.put("E", extendedAttributes);
			 
			//System.out.println(entity.toString(4));
			try {
				String url = extendedAttributes.getJSONArray("S").getJSONObject(0).getString("U");
				String title = extendedAttributes.getString("DN");
				JSONObject invertedData = extendedAttributes.getJSONObject("IA");
				String description = JSONUtilities.convertInvertedIndexToString(invertedData.getJSONObject("InvertedIndex"), invertedData.getInt("IndexLength"));
											
				position++;
				SearchRecord sr = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
				result.add(sr);	
			}
			catch (Exception e) {
				srcLogger.log(Level.WARNING, "MicrosoftAcademic, processing results, skipping record: " + e+"\n"+entities.getJSONObject(i).toString(4));
			}
		}
		
		return result;
	}

	private String createURIString(String searchCriteria, int numResults)  {
		try {
			String words[] = searchCriteria.split("\\s+");
			for (int i=0; i<words.length;i++) {
				words[i]="W=='"+words[i].toLowerCase()+"'";
			}
			String andList = String.join(",",words);
			
			String encodedCriteria = URLEncoder.encode(andList, "UTF-8");
			String result = "https://api.labs.cognitive.microsoft.com/academic/v1.0/evaluate?expr=And("+encodedCriteria+")&count="+numResults+"&complete=0&attributes=Id,Ti,L,Y,D,CC,ECC,AA.AuN,AA.AuId,AA.AfN,AA.AfId,AA.S,F.FN,F.FId,J.JN,J.JId,C.CN,C.CId,RId,W,E";
			return result;
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "create microsoftacademic URI: "+searchCriteria,e);
			return null;
		}
	}
	
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);		
		
		MicrosoftAcademic fh = new MicrosoftAcademic();
		JSONObject configuration = new JSONObject().put("microsoftacademic",new JSONObject().put("length", 10));
		List<SearchRecord> results = fh.generateSearchResults("scada","SCADA cyber",configuration,10,new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}


}
