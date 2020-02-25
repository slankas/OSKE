package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;


/**
 * GoogleHandler provides an interface to web search results from Google by scraping the page results.
 *
 */
public class GoogleHandler extends AbstractSearchHandler implements SourceHandlerInterface {

	/**
	 * what is the maximum number of results that google can return.
	 * Must be an even multiple of 10.
	 * Google can only return 10 records at a time, with a second delay between each result.
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 100;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");

	public static final String SOURCE_HANDLER_NAME = "google";
	public static final String SOURCE_HANDLER_DISPLAY_NAME = "Google Search";

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
		    put("google.length",   new SourceParameter("google.length",   "number of results to return.  Must be between 10 and "+MAX_NUMBER_OF_RESULTS+" in multiples of 10.",true,"20",false,SourceParameterType.INT,false,true));
		    put("google.newsOnly", new SourceParameter("google.newsOnly",   "If set to \"true\", will add a flag to just search news.  Boolean value",false,"",false,SourceParameterType.BOOLEAN,false,false));
		    put("google.numDay",   new SourceParameter("google.numDay",   "number of days previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.numWeek",  new SourceParameter("google.numWeek",  "number of weeks previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.numMonth", new SourceParameter("google.numMonth", "number of months previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.numYear",  new SourceParameter("google.numYear",  "number of years previous to limit the results to.  Integer value",false,"",false,SourceParameterType.INT,false,false));
		    put("google.beforeDate", new SourceParameter("google.beforeDate", "Results must be published before this date,  m/d/yyyy format. Precedence over numX fields",false,"6/15/2015",false,SourceParameterType.STRING,false,false));
		    put("google.afterDate",  new SourceParameter("google.afterDate",  "Results must be published after this date,  m/d/yyyy format. Precedence over numX fields",false,"5/15/2015",false,SourceParameterType.STRING,false,false));

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
		return "Provides an interface to web search result from Google through scraping. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
	}

	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.SEARCH_TERMS;
	}

	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}

	public boolean isDefaultDomainDiscoverySearchMethod() {
		return true;
	}

	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();

		if (!configuration.has("google")) {
			errors.add("No google object defined.");
		}
		else {
			//TODO: Add  beforeDate and after date should be convertable toa a date in m/d/y format
			/*
		    put("google.beforeDate", new SourceParameter("google.beforeDate", "Results must be published before this date,  m/d/yyyy format. Precedence over numX fields",false,"6/15/2015"));
		    put("google.afterDate",  new SourceParameter("google.afterDate",  "Results must be published after this date,  m/d/yyyy format. Precedence over numX fields",false,"5/15/2015"));
			 */


		}

		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));
		return errors;
	}


	/**
	 * Generate the possible search results that will be used as the seeds for a search
	 * Scraps Google to get these results.
	 *
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @param numberOfSeearchResults
	 * @param advConfiguration
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String searchCriteria, JSONObject configuration, int numberOfResults, JSONObject advConfiguration) {
		if (advConfiguration.length() > 0) {
			configuration = advConfiguration;
		}
		
		JSONObject googleConfiguration = configuration.optJSONObject("google");
		if (googleConfiguration == null) { googleConfiguration = new JSONObject(); }

		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();

		int position = 0;
		numberOfResults = Math.min(MAX_NUMBER_OF_RESULTS, numberOfResults);
		String ei = null;

		try (CloseableHttpClient  httpclient = HttpClients.createDefault()) {
			for (int start = 0; start < numberOfResults; start += 10) {
				if (start >1) {
					TimeUnit.MILLISECONDS.sleep(100);
				}

				String uri = this.createURIString(searchCriteria, start,ei,googleConfiguration);
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
					//System.out.println(content);
					//System.out.println("=====================================================================");

					org.jsoup.nodes.Document doc = Jsoup.parse(content, uri);
					boolean skippedRecord = false;

					Elements items = doc.select("div.g");
					for (org.jsoup.nodes.Element e: items) {
						String url = null;
						String title = null;
						String description = null;

						// EXTRACT URL and Title
						Elements links = e.select("h3.r a");
						boolean alternateTags = false;
						if (links.size() == 0) {
							links = e.select("div.r a:has(h3)");
							alternateTags = true;
						}
						for (org.jsoup.nodes.Element l: links) {
							//System.out.println(l.attr("href"));
							String hrefFull = l.attr("href");
							if (hrefFull != null && hrefFull.indexOf("http") > -1) {
								hrefFull = hrefFull.substring(hrefFull.indexOf("http"));
								if (hrefFull.indexOf("&sa") > 0) {
									hrefFull = hrefFull.substring(0,hrefFull.indexOf("&sa"));
								}
								else if (hrefFull.indexOf("&amp;sa") > 0) {
									hrefFull = hrefFull.substring(0,hrefFull.indexOf("&amp;sa"));
								}
								url = hrefFull;
							}
							if (alternateTags) {
								try {
									title = l.select("h3").first().text();
								}
								catch (Exception ex) {
									srcLogger.log(Level.WARNING, "Unable to select title, defaulting to URL.");
									title = url;
								}
							}
							else { title = l.text(); }
						}
						Elements descElements = e.select("span.st");
						for (org.jsoup.nodes.Element l: descElements) {
							description = l.text();
						}
						if (url != null) {
							position++;
							SearchRecord sr = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
							result.add(sr);
						}
						else {
							//srcLogger.log(Level.WARNING,"===============================================================================");
							//srcLogger.log(Level.WARNING, e.toString());
							//srcLogger.log(Level.WARNING,"===============================================================================");
							skippedRecord =true;
						}
					}
					if (skippedRecord) {
						srcLogger.log(Level.FINE, "Skipped google record.  Complete content from google:\n"+content);
					}

					// find bottom links to set ei
					Elements nextLinks = doc.select("a.fl");
					for (org.jsoup.nodes.Element e: nextLinks) {
						String href= e.attr("href");
						if (href.contains("ei=")) {
							href = href.substring(href.indexOf("ei=")+3);
							href = href.substring(0,href.indexOf("&"));
							ei = href;
							break;
						}
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
		LinkedHashSet<SearchRecord> temp = new LinkedHashSet<SearchRecord>(result);
		return new java.util.ArrayList<SearchRecord>(temp);
	}

	private String createURIString(String searchCriteria, int start, String ei, JSONObject googleConfig) throws URISyntaxException {
		try {
			String result = "https://www.google.com/search?q="+URLEncoder.encode(searchCriteria, "UTF-8");
			if (start >0) {
				result += "&start="+start;
			}
			if (ei != null) {
				result += "&biw=1096&bih=7847&sa=N&ei="+ei;
			}

			if (googleConfig.has("beforeDate") || googleConfig.has("afterDate")) {
				String dateRange="cdr:1";
				if (googleConfig.has("afterDate"))  { dateRange += ",cd_min:"+googleConfig.getString("afterDate"); }
				if (googleConfig.has("beforeDate")) { dateRange += ",cd_max:"+googleConfig.getString("beforeDate"); }

				result += "&tbs=" + URLEncoder.encode(dateRange, "UTF-8");
			}
			else if (googleConfig.has("numDay"))   { result += "&tbs=qdr:d" + googleConfig.getInt("numDay");   }
			else if (googleConfig.has("numWeek"))  { result += "&tbs=qdr:w" + googleConfig.getInt("numWeek");  }
			else if (googleConfig.has("numMonth")) { result += "&tbs=qdr:m" + googleConfig.getInt("numMonth"); }
			else if (googleConfig.has("numYear"))  { result += "&tbs=qdr:y" + googleConfig.getInt("numyear");  }
			
			if (googleConfig.optBoolean("newsOnly"))  { result += "&tbm=nws";   }

			return result;
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "create google URI: "+searchCriteria,e);
			return "";
		}
	}

	private String createAlternateRelatedSearchURIString(String searchCriteria) throws URISyntaxException {
		try {
			return "https://www.google.com/complete/search?q="+
			        URLEncoder.encode(searchCriteria+" ", "UTF-8") +
					"&cp=17&client=psy-ab&xssi=t&gs_ri=gws-wiz&hl=en&authuser=0&pq=cohen%20trump%20lawyer&gs_mss=" +
					URLEncoder.encode(searchCriteria, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	/**
	 * Execute a google search result and retrieve the related search text
	 *
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @return
	 */
	public java.util.List<String> getRelatedSearches(String domain, String searchCriteria) {

		java.util.Set<String> result = new java.util.HashSet<String>();

		try (CloseableHttpClient  httpclient = HttpClients.createDefault()) {
			String uri = this.createURIString(searchCriteria, 0,null,new JSONObject());
			HttpGet httpget = new HttpGet(uri);

			String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
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
				//System.out.println(content);
				//System.out.println("=====================================================================");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, uri);
				//System.out.println(doc);

				Elements items = doc.select("p._e4b");
				if (items.size() == 0) { // fall back to another method
					items = doc.select("p.nVcaUb");
					if (items.size() == 0) {
						items = doc.select("div[id^=eobm] div div");
					}
				}
				for (org.jsoup.nodes.Element e: items) {
					String text =e.text().trim();
					if (text.length() > 0 && text.equalsIgnoreCase("People also search for") == false) {
						result.add(e.text());
					}
				}

			}
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "Google exception: " + e.toString());
				return null;
			}
		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;
		}

		//result.stream().forEach(System.out::println);
		java.util.List<String> results = result.stream().sorted().collect(Collectors.toList());

		if (results.isEmpty()) {
			return this.getRelatedSearchesAlternate(domain, searchCriteria);
		}
		else {
			return results;
		}

	}

	/**
	 * Execute a google search result and retrieve the related search text
	 *
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @return
	 */
	public java.util.List<String> getRelatedSearchesAlternate(String domain, String searchCriteria) {

		java.util.Set<String> result = new java.util.HashSet<String>();

		try (CloseableHttpClient  httpclient = HttpClients.createDefault()) {
			String uri = this.createAlternateRelatedSearchURIString(searchCriteria);
			HttpGet httpget = new HttpGet(uri);

			String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
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

				String[] lines = content.split("\n");
				JSONArray ja = new JSONArray(lines[1]).getJSONArray(0);
				for (int i=0;i < ja.length(); i++) {
					String item = ja.getJSONArray(i).getString(0);
					item = item.replaceAll("<b>", "");
					item = item.replaceAll("<\\/b>", "");
					result.add(item);
				}
			}
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "Google exception: " + e.toString());
				return null;
			}
		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;
		}

		//result.stream().forEach(System.out::println);
		java.util.List<String> results = result.stream().sorted().collect(Collectors.toList());

		return results;


	}



	public static void main(String args[]) throws IOException {

		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();

		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");

		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);

		//JSONObject configuration = new JSONObject().put("google",new JSONObject().put("length", 100));

		GoogleHandler fh = new GoogleHandler();

		//List<SearchRecord> results = fh.generateSearchResults("system","emerging technologies",new JSONObject(),10, new JSONObject());

		List<String> results = fh.getRelatedSearches("system", "emerging technologies");
		if (results !=null) {
			results.forEach(System.out::println);
		}

		/*
		String testValue = "";
		String url = null;
		String title = null;
		String description = null;


		org.jsoup.nodes.Document doc = Jsoup.parse(testValue);
		// EXTRACT URL and Title
		Elements links = doc.select("div.r a:has(h3)");
		if (links.size() == 0) {
			links = doc.select("div.r a:has(h3)");
		}
		for (org.jsoup.nodes.Element l: links) {
			//System.out.println(l.attr("href"));
			String hrefFull = l.attr("href");
			if (hrefFull != null && hrefFull.indexOf("http") > -1) {
				hrefFull = hrefFull.substring(hrefFull.indexOf("http"));
				if (hrefFull.indexOf("&sa") > 0) {
					hrefFull = hrefFull.substring(0,hrefFull.indexOf("&sa"));
				}
				else if (hrefFull.indexOf("&amp;sa") > 0) {
					hrefFull = hrefFull.substring(0,hrefFull.indexOf("&amp;sa"));
				}
				url = hrefFull;
			}
			title = l.select("h3").first().text();
		}
		Elements descElements = doc.select("span.st");
		for (org.jsoup.nodes.Element l: descElements) {
			description = l.text();
		}
		System.out.println("URL: " + url);
		System.out.println("Title: " + title);
		System.out.println("Description: " + description);
		*/
	}

}
