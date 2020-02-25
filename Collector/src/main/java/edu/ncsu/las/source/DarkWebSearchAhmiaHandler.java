package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * DarkWebSearchAhmiaHandler provides an interface to Darkweb search results from Ahmia by scraping the page results.
 * 
 */
public class DarkWebSearchAhmiaHandler extends AbstractSearchHandler {

	/** 
	 * what is the maximum number of results that google can return.  
	 * Must be an even multiple of 10. 
	 * Google can only return 10 records at a time, with a second delay between each result. 
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 100;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	
	private static final String SOURCE_HANDLER_NAME = "darkwebahmiasearch";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Dark Web Search Source Handler (Ahmia.fi)";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("darkwebahmiasearch", new SourceParameter("darkwebahmiasearch", "JSON Object containing Dark Web Search for Ahmia.fi specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("darkwebahmiasearch.length",   new SourceParameter("darkwebahmiasearch.length",   "number of results to return.  Must be between 10 and "+MAX_NUMBER_OF_RESULTS+" in multiples of 10.",true,""+MAX_NUMBER_OF_RESULTS,false,SourceParameterType.INT,false,true));

	}};
	
	@Override
	public int getMaximumNumberOfSearchResults() {
		return MAX_NUMBER_OF_RESULTS;
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
		return "Provides an interface to dark web search result from ahmia.fi through scraping. (Links are converted, though, to use onion.link as a proxy). From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
	}
	
	@Override
	public boolean supportsDomainDiscovery() { 
		return true;
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
	

	private String createURIString(String searchCriteria) throws URISyntaxException {
		String result = "https://ahmia.fi/search/?q="; 
		try {
			result += URLEncoder.encode(searchCriteria, "UTF-8");
			return result;
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "create ahmia.fi URI: "+searchCriteria,e);
			return "";
		}
	}

	/**
	 * 
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String searchCriteria, JSONObject configuration, int numberOfResults, JSONObject advConfiguration) {
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();
		
		int position = 0;
		numberOfResults = Math.min(MAX_NUMBER_OF_RESULTS, numberOfResults); 
		
		try (CloseableHttpClient  httpclient = HttpClients.createDefault()) {
			String uri = this.createURIString(searchCriteria);
			HttpGet httpget = new HttpGet(uri);
				
			String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
			httpget.setHeader("User-Agent", userAgent);
			srcLogger.log(Level.INFO, "Dark Web Search Ahmia.fi URI: " + uri);
				
			try (CloseableHttpResponse response = httpclient.execute(httpget)) {
				int code = response.getStatusLine().getStatusCode();
				if (code != HttpStatus.SC_OK) {
					srcLogger.log(Level.SEVERE, "Google HTTP Response code: " + code);
					srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
					return null;
				}
					
				String content = FileUtilities.read(response.getEntity().getContent());
				org.jsoup.nodes.Document doc = Jsoup.parse(content, uri);

				Elements items = doc.select("li.result");
				for (org.jsoup.nodes.Element e: items) {
					Element link = e.select("a").first();
					String title = link.text(); 
					String url = link.attr("href");
					url = url.replaceAll(".onion/", ".onion.link/");
					String description = e.select("p").first().text();
					if (title != null || url != null || description !=null)	{
						position++;
						SearchRecord r = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
						result.add(r);
					}
				}
			}
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "ahmia.fi exception: " + e.toString());
				return null;
			} 
		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;			
		}
		
		//result.stream().forEach(System.out::println);
		
		return result;
	}
	
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);		
		
		DarkWebSearchAhmiaHandler fh = new DarkWebSearchAhmiaHandler();
		
		JSONObject configuration = new JSONObject().put(SOURCE_HANDLER_NAME,new JSONObject().put("length", 100));
		
		List<SearchRecord> results = fh.generateSearchResults("test","cyber attack",configuration, MAX_NUMBER_OF_RESULTS, new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
	
}
