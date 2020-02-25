package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * DuckDuckGoHandler provides an interface to web search at https://duckduckgo.com
 * 
 * 
 * The DuckDuckGoHandler handler will take a search criteria and get the searchresults
 * The handler will then use those results as the starting seeds.  If the user only wants those results to be returned, 
 * then the depth should be set to 0.
 * 
 */
public class DuckDuckGoHandler extends AbstractSearchHandler implements SourceHandlerInterface {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	/** 
	 * what is the maximum number of results that DuckDuckGo can return.  
	 *
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 180;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	public static final String SOURCE_HANDLER_NAME = "duckduckgo";
	public static final String SOURCE_HANDLER_DISPLAY_NAME = "DuckDuckGo Search";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent (skipping AbstractSearch as it is abstract) to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("duckduckgo", new SourceParameter("duckduckgo", "JSON Object containing Duck Duck Go specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("duckduckgo.length", new SourceParameter("duckduckgo.length", "number of results to return.  Must be between 1 and 180.",true,"100",false,SourceParameterType.INT,false,true));
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
		return "Provides an interface to get up to 180 search results from DuckDuckgo. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue,configuration));
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
	public java.util.List<SearchRecord> generateSearchResults(String domain, String queryText, JSONObject configuration, int numberOfSearchResults, JSONObject advConfiguration) {  //TODO: maxlength doesn't work for this
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();
		// these values can probably be statics
		String[] regexs =  { "'https:\\/\\/duckduckgo-owned-server.yahoo.net\\/d.js\\?.*?'",
				            "'\\/d.js\\?.*?'"	};
		Pattern[] patterns = new Pattern[regexs.length];
		for (int i=0;i<regexs.length;i++) {
			patterns[i] = Pattern.compile(regexs[i]);
		}
		
		int position = 0;
		String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
		
		try (CloseableHttpClient  httpclient = HttpClients.custom().setUserAgent(userAgent).build()) {	
			
			String uri = this.createURI(queryText).toString();
			srcLogger.log(Level.INFO, "URI for DuckDuckGo: "+uri);

			HttpGet duckDuckGo = new HttpGet(uri);

			String resultURL = null;
			try (CloseableHttpResponse response = httpclient.execute(duckDuckGo)) {
				int code = response.getStatusLine().getStatusCode();
				if (code != HttpStatus.SC_OK) {
					srcLogger.log(Level.SEVERE, "DuckDuckGo HTTP Response code: " + code);
					srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
					return null;
				}
				
				String content = FileUtilities.read(response.getEntity().getContent());	
				for (Pattern p: patterns) {
					Matcher matcher = p.matcher(content);
					if (matcher.find())	{
						resultURL = matcher.group(0);
						resultURL = resultURL.substring(1, resultURL.length()-1);
						break;
					}
				}
			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "DuckDuckGo exception: " + e.toString());
				return null;
			} 			
			if (resultURL == null) {
				srcLogger.log(Level.SEVERE, "DuckDuckGo no initial search query string found.");
				return null;
			}
		
			uri = resultURL;
			int numTimesCalled = 0;
			String content = "";
			while (true) {
				if (uri.equals("STOP")) {break;}
				if (uri.startsWith("http") == false) {
					uri = "https://duckduckgo.com" + uri;
				}
				numTimesCalled++;
				if (numTimesCalled >1) {	TimeUnit.MILLISECONDS.sleep(100);	}  // sleep to be respectful of DuckDuckgo

				srcLogger.log(Level.INFO, "URI for DuckDuckGO("+numTimesCalled+"): "+uri);
				HttpGet httpget = new HttpGet(uri);
				uri = "STOP";
				try (CloseableHttpResponse response = httpclient.execute(httpget)) {
					int code = response.getStatusLine().getStatusCode();
					if (code != HttpStatus.SC_OK) {
						srcLogger.log(Level.SEVERE, "DuckDuckGo HTTP Response code: " + code);
						srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
						return result;
					}
					
					content = FileUtilities.read(response.getEntity().getContent());
					//System.out.println(content);
					//System.exit(0);
					
					/*
					int ddgInjectIndex = content.indexOf("DDG.inject");  //Doesn't appear to have all of the results
					if (ddgInjectIndex >0) {
						int startJSON = content.indexOf("{",ddgInjectIndex);
						int endJSON   = content.indexOf(");if",ddgInjectIndex);
						String jsonURLs = content.substring(startJSON, endJSON);
						System.out.println(jsonURLs);
						JSONObject jo = new JSONObject(jsonURLs);
						JSONArray ja = jo.getJSONArray("en");
						for (int j=0;j<ja.length();j++) {
							//result.add(ja.getString(j));
						}
						//return result;
					}
					*/
					int startPOS = 0;
					while (true) {
						int startNRN = content.indexOf("if (nrn) nrn(",startPOS);
						if (startNRN <0) {break;}
						
						int endNRN   = content.indexOf(");",startNRN+10);
						//System.out.println(startNRN + " ---- "+endNRN);
						int jStart = content.indexOf("[",startNRN);
						int jEnd   = content.indexOf("]);",startNRN+10);
						
						if (jStart <0) {break; }
						
						String jString = content.substring(jStart, jEnd+1);
						//System.out.println(jString);
						JSONArray ja = new JSONArray(jString);
						//System.out.println(ja.toString(2));
						for (int j=0;j<ja.length();j++) {
							JSONObject jo = ja.getJSONObject(j);
							//System.out.println(jo.toString(4));
							if (jo.optInt("p") == 1) { //this signifies an ad
								continue;
							}
							if (jo.has("u")) {
								position++;
								String url = jo.getString("u");
								String description = jo.getString("a").replaceAll("<b>","").replaceAll("<\\/b>", "");
								String title = jo.getString("t").replaceAll("<b>","").replaceAll("<\\/b>", "").replaceAll("<span class=\"result__type\">PDF<\\/span>","");

								SearchRecord ddr = new SearchRecord(title, url, description, position,SOURCE_HANDLER_NAME);
								
								result.add(ddr);
							}
							if (jo.has("n")) {
								uri = jo.getString("n");
							}
						}
						
						
						startPOS = endNRN;
					}
				} 
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "DuckDuckGo exception: " + e.toString());
					srcLogger.log(Level.SEVERE, content);
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
	
	private URI createURI(String searchCriteria) throws URISyntaxException {
		URI uri = new URIBuilder()
		        .setScheme("https")
		        .setHost("duckduckgo.com")
		        .setPath("/")
		        .setParameter("q", searchCriteria)
		        .setParameter("ia", "web")
		        .build();
		return new URI(uri.toString()); 
	}
			
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);		
		
		DuckDuckGoHandler ddgh = new DuckDuckGoHandler();
		List<SearchRecord> results = ddgh.generateSearchResults("test", "quadcopters drones", new JSONObject(), 100, new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}
	
}
