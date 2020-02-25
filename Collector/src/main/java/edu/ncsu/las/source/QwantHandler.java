package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.FileUtilities;

import java.io.File;
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
 * QwantHandler provides an interface to web and news API feed provided by Qwant
 * (http://www.qwant.com/)
 * 
 * The Qwant handler will take a search criteria, news/web flag, and a locale.
 * The handler will then use those results as the starting seeds.  If the user only wants those results to be returned, 
 * then the depth should be set to 0.
 * 
 *   
 */
public class QwantHandler extends AbstractSearchHandler implements SourceHandlerInterface {
	
	public static final int MAX_NUMBER_OF_RESULTS = 100;
	
	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	private static final String SOURCE_HANDLER_NAME = "qwant";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Qwant";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("qwant", new SourceParameter("qwant", "JSON Object containing Qwant specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("qwant.type", new SourceParameter("qwant.type", "\"web\" for a websource, \"news\" to search news", true, "\"news\"",false, SourceParameterType.STRING,false,true));
		    put("qwant.locale", new SourceParameter("qwant.locale", "What language and country_code to use?.",true,"en_us",false, SourceParameterType.STRING,false,true));
		    put("qwant.length", new SourceParameter("qwant.length", "What is the maximum number of search results to use?",true,"en_us",false, SourceParameterType.INT,false,true));
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
		return "Provides an interface to web and news API feed provided by Qwant. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
	 * Qwant API to get a list of all possible search results
	 * 
	 * @param domain
	 * @param queryText
	 * @param configuration  if a "qwant" json object is not present, assume the search type is web with a locale of "en_us"
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String searchCriteria, JSONObject configuration, int numberOfResults, JSONObject advConfiguration) {  // 
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();
		
		if (configuration.has("qwant") == false) {
			configuration.put("qwant", new JSONObject().put("type", "web").put("locale", "en_us"));
		}

		String qwantType   = configuration.getJSONObject("qwant").getString("type");
		String qwantLocale = configuration.getJSONObject("qwant").getString("locale");
		
		int position = 0;
		
		try (CloseableHttpClient  httpclient = HttpClients.createDefault()) {
			for (int start = 0; start < 50; start += 10) {  // qwant only seems to bring back 50 results
				if (start >1) {
					TimeUnit.MILLISECONDS.sleep(100);
				}
				
				URI uri = createURI(searchCriteria, qwantType, qwantLocale, start);
				srcLogger.log(Level.INFO, "getting qwant API: "+uri);
				HttpGet httpget = new HttpGet(uri);
				
				try (CloseableHttpResponse response = httpclient.execute(httpget)) {
					int code = response.getStatusLine().getStatusCode();
					if (code != HttpStatus.SC_OK) {
						srcLogger.log(Level.SEVERE, "Qwant HTTP Response code: " + code);
						srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
						return null;
					}
					
					String content = FileUtilities.read(response.getEntity().getContent());
					JSONArray qwantItems = new JSONObject(content).getJSONObject("data").getJSONObject("result").getJSONArray("items");
					for (int i=0; i < qwantItems.length(); i++) {
						JSONObject entry = qwantItems.getJSONObject(i);
						position++;
						
						SearchRecord sr = new SearchRecord(entry.getString("title"), entry.getString("url"), entry.getString("desc"), position,SOURCE_HANDLER_NAME);
						result.add(sr);						
					}
				} 
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Qwant exception: " + e.toString());
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

	private URI createURI(String searchCriteria, String qwantType, String qwantLocale, int start) throws URISyntaxException {
		String path = "/api/search/web";
		if (qwantType.equalsIgnoreCase("news")) {
			path = "/api/search/news";
		}
		URI uri = new URIBuilder()
		        .setScheme("https")
		        .setHost("api.qwant.com")
		        .setPath(path)
		        .setParameter("q", searchCriteria)
		        .setParameter("offset", Integer.toString(start))
		        .setParameter("count", "10")
		        .setParameter("f", "safesearch:1")
		        .setParameter("locale", "en_us")
		        .build();
		return uri;
	}
	
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		if (!configuration.has("qwant")) {
			errors.add("No qwant object defined.");
		}
		else {
			//check type
			if (!configuration.getJSONObject("qwant").has("type")) {
				errors.add("Qwant type not present.  type must be \"web\" or \"news\"");
			}
			else {
				String value = configuration.getJSONObject("qwant").getString("type");
	
				if (value.equals("web") == false && value.equals("news") == false) {
					errors.add("Invalid Qwant type - must be \"web\" or \"news\": "+ value);
				}
			}
			
			//check locale
			if (!configuration.getJSONObject("qwant").has("locale")) {
				errors.add("Quant locale not present.  Need to specify language and country code.  Example: \"en_us\"");
			}
		}
		
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));
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
		QwantHandler qh = new QwantHandler();
		JSONObject configuration = new JSONObject().put("qwant",new JSONObject().put("length", 100).put("type", "web").put("locale", "en_us"));
		List<SearchRecord> results = qh.generateSearchResults("test","quadcopter",configuration, MAX_NUMBER_OF_RESULTS, new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
}
