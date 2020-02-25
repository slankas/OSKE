package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.FileUtilities;
import edu.uci.ics.crawler4j.fetcher.SniSSLConnectionSocketFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONArray;
import org.json.JSONObject;




/**
 * MillionShortHandler provides an interface to web search feed at https://millionshort.com
 * 
 * The millionshort handler will take a search criteria and then return the results generated by the service, ignoring the top 1,000,000 web sites
 * 
 * TODO: at some point, use length...
 *   
 */
public class MillionShort extends AbstractSearchHandler implements SourceHandlerInterface {
	static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true,\"millionshort\":{\"includeFiltered\":true,\"filterTopN\":1}}");
	
	private static final String SOURCE_HANDLER_NAME = "millionshort";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Million Short";
	
	public static final int MAX_NUMBER_OF_RESULTS = 50;

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put(SOURCE_HANDLER_NAME, new SourceParameter(SOURCE_HANDLER_NAME, "JSON Object containing MillionShort specific parameters.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put(SOURCE_HANDLER_NAME+".length", new SourceParameter(SOURCE_HANDLER_NAME+".length", "number of results to return.  Must be between 8 and "+MAX_NUMBER_OF_RESULTS+" in multiples of 8.",true,"20",false,SourceParameterType.INT,false,true));
			put(SOURCE_HANDLER_NAME+".filterTopN", new SourceParameter(SOURCE_HANDLER_NAME+".filterTopN", "Filters out the top N web sites.  Must be between 1 and 1,000,000.  Default:1000000",false,"100",false,SourceParameterType.INT,false,true));
			put(SOURCE_HANDLER_NAME+".includeFiltered", new SourceParameter(SOURCE_HANDLER_NAME+".includeFiltered", "Should results filtered by being in the top N be included?  true/false.  default: false",false,"false",false,SourceParameterType.BOOLEAN,false,true));
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
		return "Provides an interface to get search results from millionShort.com. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.SEARCH_TERMS;
	}
	
	@Override
	public boolean isOnline() {
		return false; // millionshort has implemented captcha to reduce/limited automated queries.  Take offline while exploring options.
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
	public java.util.List<SearchRecord> generateSearchResults(String domain, String searchCriteria, JSONObject configuration, int numberOfSearchResults, JSONObject advConfiguration) {   // TODO: need to trim results to numberOfSearchResults
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();
		String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
		
		int filterTopN = 1000000;
		boolean includeFiltered= false;
		
		if (advConfiguration.length() > 0) {
			configuration = advConfiguration;
		}

		if (configuration.has("millionshort")) {
			JSONObject jo = configuration.getJSONObject("millionshort");
			if (jo.has("filterTopN")) {
				filterTopN= jo.optInt("filterTopN");
			}
			if (jo.has("includeFiltered")) {
				includeFiltered= jo.getBoolean("includeFiltered");
			}		
		}
	
				
		CloseableHttpClient  httpclient = null;
		try  {
			SSLContext sslContext =
	                SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
	                    @Override
	                    public boolean isTrusted(final X509Certificate[] chain, String authType) {
	                        return true;
	                    }
	                }).build();
			SSLConnectionSocketFactory sslsf = new SniSSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
			
			httpclient = HttpClients.custom().setSSLSocketFactory( sslsf).setUserAgent(userAgent).build();
			
					
			String uri = this.createURI(searchCriteria,filterTopN).toString();
			srcLogger.log(Level.INFO, "URI for MillionShort: "+uri);
			
			CookieStore cookieStore = new BasicCookieStore(); 
			//TODO: this cookie was hacked in from running on my machine not sure how long it will last.
			BasicClientCookie sidCookie = new BasicClientCookie("connect.sid", "s%3Af7AM8DIdoOwTrwwqbdVUPlX6_BMygiFn.WMXLQQtLAQH5BlSEWblyn5fFyKhRUDmp7%2Fkxoeec5gk");
			sidCookie.setDomain(".millionshort.com");
			sidCookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
			cookieStore.addCookie(sidCookie); 
			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		
			

			HttpGet millionHome = new HttpGet("https://millionshort.com/");
			//String nonce = "";
			String cookie = "";
			try (CloseableHttpResponse response = httpclient.execute(millionHome,localContext)) {
				int code = response.getStatusLine().getStatusCode();
				if (code != HttpStatus.SC_OK) {
					srcLogger.log(Level.SEVERE, "MillionShort HTTP Response code: " + code);
					srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
					return null;
				}
				//nonce = response.getFirstHeader("ETag").getValue();
				cookie = response.getFirstHeader("Set-Cookie").getValue();		

			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "MillionShort exception: " + e.toString());
				//return null;
			}
			
			int numIterations = (numberOfSearchResults / 10) +  (numberOfSearchResults %10 == 0 ? 0 : 1);
			
			JSONObject rejectDomains = null;
			int position = 0;
			for (int offset=0; offset < numIterations; offset++) {				
				if (offset > 0) {
					TimeUnit.MILLISECONDS.sleep(100);
				}
				String uriAPI = this.createURIForAPI(searchCriteria,filterTopN, offset *10).toString();
				HttpGet httpget = new HttpGet(uriAPI);
				httpget.setHeader("Referer",uri); // "https://millionshort.com/");
				//httpget.setHeader("If-None-Match",nonce);
				//httpget.setHeader("Cookie", cookie);
				httpget.setHeader("Accept", "*/*");
				httpget.setHeader("Accept-Encoding", "gzip, deflate, br");
				httpget.setHeader("Accept-Language", "en-US,en;q=0.9");
				httpget.setHeader("Connection", "keep-alive");
				httpget.setHeader("Host","millionshort.com");
				httpget.setHeader("Referer",uriAPI);
				
				try (CloseableHttpResponse response = httpclient.execute(httpget,localContext)) {
					int code = response.getStatusLine().getStatusCode();
					if (code != HttpStatus.SC_OK) {
						srcLogger.log(Level.SEVERE, "MillionShort HTTP Response code: " + code);
						srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
						return null;
					}
							
					String content = FileUtilities.read(response.getEntity().getContent());
	
					JSONObject msResults = new JSONObject(content);
					//System.out.println(msResults.toString(4));
	
					
					JSONArray webPages = msResults.getJSONObject("content").getJSONArray("webPages");
					for (int i=0; i < webPages.length(); i++) {
						JSONObject msItem = webPages.getJSONObject(i);
						String title = msItem.getString("name");
						String url   = msItem.getString("displayUrl");
						String description = msItem.getString("snippet");
						if(title != null || url != null || description !=null)	{
							position++;
							SearchRecord r = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
							result.add(r);
						}
					}
	
					
					if (includeFiltered && offset == 0) {
						rejectDomains = msResults.getJSONObject("content").getJSONObject("rejected");
					}
				
					
					
					if (webPages.length() <10) { break;}
					
				} 
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "MillionShort exception: " + e.toString());
					return null;
				}
			}
			
			// now process the saved rejecteddomains
			if (rejectDomains != null) {
				for (String rDomain: rejectDomains.keySet()) {
					JSONArray rejectedPages = rejectDomains.getJSONArray(rDomain);
					for (int i=0; i < rejectedPages.length(); i++) {
						JSONObject msItem = rejectedPages.getJSONObject(i);
						String title = msItem.getString("name");
						String url   = msItem.getString("displayUrl");
						String description = msItem.getString("snippet");
						if(title != null || url != null || description !=null)	{
							position++;
							SearchRecord r = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
							result.add(r);
						}
					}
				}	
			}
		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;			
		}
		finally {
			if (httpclient != null) { 
				try{
					httpclient.close();
				}
				catch (IOException ex) {
					srcLogger.log(Level.SEVERE, "httpclient exception: " + ex.toString());
				}
			}
		}
						
		return result;
	}	
		
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		
		if (configuration.has("millionshort")) {
			try {
				JSONObject jo = configuration.getJSONObject("millionshort");
				if (jo.has("filterTopN")) {
					int filterTopN= jo.optInt("filterTopN");
					
					if (filterTopN < 1 || filterTopN > 1000000) {
						errors.add( "millionshort - filterTopN must be between 1 and 1000000");
					}
					
				}
				if (jo.has("includeFiltered")) {
					jo.getBoolean("includeFiltered");
				}

			}
			catch(Throwable t) {
				errors.add( "millionshort - invalid parameters");
			}		
		}
		
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));
		return errors;
	}
	
	private URI createURI(String searchCriteria, int filterTopN) throws URISyntaxException {
		URI uri = new URIBuilder()
		        .setScheme("https")
		        .setHost("millionshort.com")
		        .setPath("search")
		        .setParameter("country", "")
		        .setParameter("shopping", "")
		        .setParameter("advertising", "")
		        .setParameter("chat", "")
		        .setParameter("keywords", searchCriteria)
		        .setParameter("remove", Integer.toString(filterTopN))
		        .build();
		return new URI(uri.toString()); 
	}	
	
	private URI createURIForAPI(String searchCriteria, int filterTopN, int offset) throws URISyntaxException {
		URI uri = new URIBuilder()
		        .setScheme("https")
		        .setHost("millionshort.com")
		        .setPath("api/search")
		        .setParameter("country", "")
		        .setParameter("shopping", "")
		        .setParameter("advertising", "")
		        .setParameter("chat", "")
		        .setParameter("keywords", searchCriteria)
		        .setParameter("remove", Integer.toString(filterTopN))
		        .setParameter("offset", Integer.toString(offset))
		        .build();
		return new URI(uri.toString()); 
	}	
	
		
	public static void main(String args[]) throws Exception {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");

		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);
		//mainCollector.getConfiguration();
		MillionShort ms = new MillionShort();
		
		List<SearchRecord> results = ms.generateSearchResults("sandbox","SCADA cyber", new JSONObject(), MAX_NUMBER_OF_RESULTS, new JSONObject() );
		if (results !=null) {
			results.forEach(System.out::println);
		}
	}	
	
	
}

