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
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * PastebinSearchHandler provides an interface to web search results from onion.link (which utilizes a custom google search engine by scraping the page results.
 * 
 */
public class PastebinSearchHandler extends AbstractSearchHandler {

	/** 
	 * what is the maximum number of results that google can return.  
	 * Must be an even multiple of 10. 
	 * Google can only return 10 records at a time, with a second delay between each result. 
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 100;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	
	private static final String SOURCE_HANDLER_NAME = "pastebinsearch";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Pastebin and Associated Sites";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("SOURCE_HANDLER_NAME", new SourceParameter(SOURCE_HANDLER_NAME, "JSON Object containing Pastebin Search specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put(SOURCE_HANDLER_NAME+".length",   new SourceParameter(SOURCE_HANDLER_NAME+".length",   "number of results to return.  Must be between 10 and "+MAX_NUMBER_OF_RESULTS+" in multiples of 10.",true,""+MAX_NUMBER_OF_RESULTS,false,SourceParameterType.INT,false,true));

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
		return "Provides an interface to search pastebin.com and similar sites through scraping (https://inteltechniques.com/osint/menu.pastebins.html). From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
	 * Scraps google to get a list of all possible search results
	 * 
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String searchCriteria, JSONObject configuration, int numberOfResults, JSONObject newAdvConfiguration) {
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();
		
		int position = 0;
		numberOfResults = Math.min(MAX_NUMBER_OF_RESULTS, numberOfResults); 
		
		try (CloseableHttpClient  httpclient = HttpClients.createDefault()) {
			for (int start = 0; start < numberOfResults  ; start += 10) {
				if (start >1) {
					TimeUnit.MILLISECONDS.sleep(100);
				}
				
				String uri = this.createURIString(domain,searchCriteria, start);
				HttpGet httpget = new HttpGet(uri);
				
				String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
				//String userAgent = Configuration.getConfigurationProperty(domain, ConfigurationType.WEBCRAWLER_USERAGENTSTRING);
				//String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36";
				httpget.setHeader("User-Agent", userAgent);
				srcLogger.log(Level.INFO, "Google URI: " + uri);
				
				try (CloseableHttpResponse response = httpclient.execute(httpget)) {
					int code = response.getStatusLine().getStatusCode();
					if (code != HttpStatus.SC_OK) {
						srcLogger.log(Level.SEVERE, "Google HTTP Response code: " + code);
						srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
						return null;
					}
					
					String content = FileUtilities.read(response.getEntity().getContent());
					int firstIndex = content.indexOf("{");
					content = content.substring(firstIndex);
					content = content.substring(0, content.length() -2);

					JSONObject jo = new JSONObject(content);
					
					JSONArray results = jo.getJSONArray("results");
					for (int i=0;i<results.length();i++) {
						JSONObject entry = results.getJSONObject(i);
						
						position++;
						SearchRecord ddr = new SearchRecord(entry.getString("titleNoFormatting"), entry.getString("unescapedUrl"), entry.getString("contentNoFormatting"), position,SOURCE_HANDLER_NAME);
						result.add(ddr);
					}
				} 
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "Google exception: " + e.toString());
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

	private String createURIString(String domain, String searchCriteria, int start) throws URISyntaxException {
		try {
			//String result = "https://www.googleapis.com/customsearch/v1element?key=AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY&rsz=filtered_cse&num=10&hl=en&prettyPrint=false&source=gcsc&gss=.com&sig=01d3e4019d02927b30f1da06094837dc&cx=013212718322258083429:lha4khxcshs&q="+URLEncoder.encode(searchCriteria, "UTF-8")+"&cse_tok=AHKYotWVmvV1wohA3g8oNFAm_6cK:1495660148313&sort=&googlehost=www.google.com&oq="+URLEncoder.encode(searchCriteria, "UTF-8")+"&gs_l=partner.12...12496.14492.0.22068.10.10.0.0.0.0.108.822.9j1.10.0.gsnos%2Cn%3D13...0.6848j24127640j11..1ac.1.25.partner..8.2.174.HXPtlqrTNIY&callback=google.search.Search.apiary11660&nocache=1505240809268"; //https://netbootcamp.org/pastesearch.html
			
			//This is Michael Bizzel's: https://inteltechniques.com/osint/menu.pastebins.html
			//String result = "https://www.googleapis.com/customsearch/v1element?key=AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY&rsz=filtered_cse&num=10&hl=en&prettyPrint=false&source=gcsc&gss=.com&sig=1ca94331d67f5f17586b15e6157d4d31&cx=001580308195336108602:mhdmrvbspnm&q="+URLEncoder.encode(searchCriteria, "UTF-8")+"&cse_tok=AF14hliGo66WR7-_3vGKF2Ni7Jl8qNtx7A:1537376447658&googlehost=www.google.com&oq="+URLEncoder.encode(searchCriteria, "UTF-8")+"&gs_l=partner-generic.3...8180.10948.0.11280.0.0.0.0.0.0.0.0..0.0.gsnos%2Cn%3D13...0.2784j630384j20j1..1ac.1.25.partner-generic..0.0.0.&callback=google.search.Search.apiary18657&nocache=1537376449183";
			
			// from https://netbootcamp.org/pastesearch.html
			//String result = "https://www.googleapis.com/customsearch/v1element?key=AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY&rsz=filtered_cse&num=10&hl=en&prettyPrint=false&source=gcsc&gss=.com&sig=ddc80d38c7e2cb7b462cb57d9710fb58&cx=013212718322258083429:lha4khxcshs&q="+URLEncoder.encode(searchCriteria, "UTF-8")+"&safe=off&cse_tok=AF14hlgcigMZoMvSa3CM9b-tKoxrLC2rZw:1535300017743&sort=&googlehost=www.google.com&oq="+URLEncoder.encode(searchCriteria, "UTF-8")+"&gs_l=partner-generic.12...7867.12730.0.21402.19.19.0.0.0.0.133.1425.17j2.19.0.gsnos%2Cn%3D13...0.4864j3011238j19..1ac.1.25.partner-generic..14.5.427.Lh3a1D-jDdw&callback=google.search.Search.apiary14224&nocache=1535300020537";
			//String result = "https://www.googleapis.com/customsearch/v1element?key=AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY&rsz=filtered_cse&num=10&hl=en&prettyPrint=false&source=gcsc&gss=.com&sig=1ca94331d67f5f17586b15e6157d4d31&cx=013212718322258083429:lha4khxcshs&q="+URLEncoder.encode(searchCriteria, "UTF-8")+"&safe=off&cse_tok=AF14hlgJIuCnGE5VlxOlFD1m4AsEE2SZSg:1537376807710&sort=&googlehost=www.google.com&oq="+URLEncoder.encode(searchCriteria, "UTF-8")+"&gs_l=partner-generic.12..0.9536.15066.1.16617.15.11.4.0.0.0.101.785.10j1.11.0.gsnos%2Cn%3D13...0.15040j65508896j17j4..1ac.1.25.partner-generic..14.11.882.sAYkdHp5O6g&callback=google.search.Search.apiary17953&nocache=1537376809239";
			
			String searchURL = Configuration.getConfigurationProperty(domain, ConfigurationType.PASTEBIN_SEARCH_URL);
			searchURL.replaceAll("REPLACEME", URLEncoder.encode(searchCriteria, "UTF-8"));
			
			if (start >0) {
				searchURL += "&start="+start;
			}
						
			return searchURL;
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "create google URI: "+searchCriteria,e);
			return "";
		}
	}
		
	
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);		
		
		PastebinSearchHandler fh = new PastebinSearchHandler();
		JSONObject configuration = new JSONObject().put(SOURCE_HANDLER_NAME,new JSONObject().put("length", 100));
		List<SearchRecord> results = fh.generateSearchResults("system","explosives",configuration, MAX_NUMBER_OF_RESULTS, new JSONObject() );
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
}
