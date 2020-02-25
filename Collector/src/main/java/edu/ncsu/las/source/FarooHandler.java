package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * FarooHandler provides an interface to web and news API feed provided by Faroo
 * (http://www.faroo.com/)
 * 
 * The Faroo handler will take a search criteria, news/web flag, and a number of results to generate (10-100) by 10s.
 * The handler will then use those results as the starting seeds.  If the user only wants those results to be returned, 
 * then the depth should be set to 0.
 * 
 *   
 */
public class FarooHandler extends AbstractSearchHandler implements SourceHandlerInterface {

	/** 
	 * what is the maximum number of results that Faroo can return.  
	 * Must be an even multiple of 10. 
	 * Faroo can only return 10 records at a time, with a second delay between each result. 
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 100;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	private static final String SOURCE_HANDLER_NAME = "faroo";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "FAROO Search";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("faroo", new SourceParameter("faroo", "JSON Object containing Faroo specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("faroo.source", new SourceParameter("faroo.source", "\"web\" for a websource, \"news\" to search news", true, "\"news\"",false,SourceParameterType.STRING,false,true));
		    put("faroo.length", new SourceParameter("faroo.length", "number of results to return.  Must be between 10 and 100 in multiples of 10.",true,"100",false,SourceParameterType.INT,false,true));
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
		return "Provides an interface to web and news API feed provided by Faroo. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
		
		if (!configuration.has("faroo")) {
			errors.add("No Faroo object defined.");
		}
		else {
			
			//check source
			if (!configuration.getJSONObject("faroo").has("source")) {
				errors.add("Faroo source not present.  Source must be \"web\" or \"news\"");
			}
			else {
				String value = configuration.getJSONObject("faroo").getString("source");	
				if (value.equals("web") == false && value.equals("news") == false) {
					errors.add("Invalid Faroo source - must be \"web\" or \"news\": "+ value);
				}
			}
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
		
		String farooSource = "web";
		try {
			farooSource = configuration.getJSONObject("faroo").getString("source");
		}
		catch (org.json.JSONException e) {
			srcLogger.log(Level.INFO, "faroo configuration not present, defaulting to web");
		}
 
		
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();


		String farooKey       = Configuration.getConfigurationProperty(domain,ConfigurationType.FAROO_KEY);

		int position = 0;
		numberOfResults = Math.min(MAX_NUMBER_OF_RESULTS, numberOfResults); // Faroo can only do up to 100 results
		
		String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
		
		try (CloseableHttpClient  httpclient = HttpClients.custom().setUserAgent(userAgent).build()) {
			for (int start = 1; start <= numberOfResults; start += 10) {
				if (start >1) {
					TimeUnit.MILLISECONDS.sleep(100);
				}
				
				URI uri = createURI(queryText, farooSource, farooKey, start);
				HttpGet httpget = new HttpGet(uri);
				srcLogger.log(Level.INFO, "Faroo URI: " + uri);
				
				try (CloseableHttpResponse response = httpclient.execute(httpget)) {
					int code = response.getStatusLine().getStatusCode();
					if (code != HttpStatus.SC_OK) {
						srcLogger.log(Level.SEVERE, "Faroo HTTP Response code: " + code);
						srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
						return null;
					}
					
					String content = FileUtilities.read(response.getEntity().getContent());
					//System.out.println(content);
					JSONObject farooResult = new JSONObject(content);
					JSONArray farooEntries = farooResult.getJSONArray("results");
					for (int i=0; i < farooEntries.length(); i++) {
						JSONObject entry = farooEntries.getJSONObject(i);
						position++;
						SearchRecord sr = new SearchRecord(entry.getString("title"), entry.getString("url"), entry.getString("kwic"), position,SOURCE_HANDLER_NAME);
						result.add(sr);			
					}	
				} 
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Faroo exception: " + e.toString());
					return null;
				} 
			}
		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;			
		}
		
		//result.stream().forEach(System.out::println);
		
		return result;
	}

	private URI createURI(String searchCriteria, String farooSource, String farooKey, int start) throws URISyntaxException {
		URI uri = new URIBuilder()
		        .setScheme("http")
		        .setHost("www.faroo.com")
		        .setPath("/api")
		        .setParameter("q", searchCriteria)
		        .setParameter("start", Integer.toString(start))
		        .setParameter("length", "10")
		        .setParameter("l", "en")
		        .setParameter("src", farooSource)
		        .setParameter("i", "false")
		        .setParameter("f", "json")
		        .build();
		return new URI(uri.toString() + "&key="+farooKey); // farooKey can't be encoded, so we need to manually add
	}
		
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);		
		

		
		JSONObject configuration = new JSONObject().put("faroo",new JSONObject().put("source", "web").put("length", 200));
		
		FarooHandler fh = new FarooHandler();
		List<SearchRecord> results =fh.generateSearchResults("test","insanity",configuration,MAX_NUMBER_OF_RESULTS,new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
}
