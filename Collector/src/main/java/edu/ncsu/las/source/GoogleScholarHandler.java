package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.storage.citation.PubMedDownloadFullReport;
import edu.ncsu.las.util.FileUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
 * GoogleScholarHandler provides an interface to web search results from Google Scholar (https://scholar.google.com/) by scraping the page results.
 * 
 *   
 */
public class GoogleScholarHandler extends AbstractSearchHandler implements SourceHandlerInterface {

	/** 
	 * what is the maximum number of results that google scholar 
	 * Must be an even multiple of 10
	 * googlescholar can only return 10 records at a time, with a 100 millisecond delay between each result. 
	 **/
	public static final int MAX_NUMBER_OF_RESULTS = 900;

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"googlescholar\" : { \"length\" : 20 }, \"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	public static final String SOURCE_HANDLER_NAME = "googlescholar";
	public static final String SOURCE_HANDLER_DISPLAY_NAME = "Google Scholar";

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
		    put("googlescholar.length", new SourceParameter("googlescholar.length", "number of results to return.  Must be between 10 and "+MAX_NUMBER_OF_RESULTS+" in multiples of 10.",true,"100",false,SourceParameterType.INT,false,true));
		    put("googlescholar.startYear", new SourceParameter("googlescholar.startYear", "Results must be published after this year (inclusive)",false,"2012",false,SourceParameterType.STRING,false,false));
		    put("googlescholar.endYear",  new SourceParameter("googlescholar.endYear",  "Results must be published before this year (inclusisve",false,"2015",false,SourceParameterType.STRING,false,false));

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
		return "Provides an interface to web search result from Google Scholar through scraping. From those results, it initializes a WebSource Handler / crawler to use those URLs as the seeds.  The URL should provide the search criteria.";
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
	 * 
	 * @param domain
	 * @param queryText
	 * @param configuration
	 * @param numberOfResults
	 * @param advConfig  not used by this handler.  may be null or empty
	 * 
	 * @return
	 */
	public java.util.List<SearchRecord> generateSearchResults(String domain, String queryText, JSONObject configuration, int numberOfResults, JSONObject advConfig) {
		if (advConfig.length() > 0) {
			configuration = advConfig;
		}
		
		JSONObject googleScholarConfiguration = configuration.optJSONObject("googlescholar");
		if (googleScholarConfiguration == null) { googleScholarConfiguration = new JSONObject(); }
	
		java.util.List<SearchRecord> result = new java.util.ArrayList<SearchRecord>();


		int position = 0;
		numberOfResults = Math.min(MAX_NUMBER_OF_RESULTS, numberOfResults); 
		String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
		
		try (CloseableHttpClient  httpclient = HttpClients.custom().setUserAgent(userAgent).build()) {
			String uri = this.createURIString(queryText, numberOfResults, googleScholarConfiguration);
			srcLogger.log(Level.INFO, "googlescholar URI: " + uri);
			
			while (result.size() < numberOfResults && uri != null) {
				try (CloseableHttpResponse response = httpclient.execute(new HttpGet(uri))) {
	
					int code = response.getStatusLine().getStatusCode();
					if (code != HttpStatus.SC_OK) {
						srcLogger.log(Level.SEVERE, "googlescholar HTTP Response code: " + code);
						srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
						return null;
					}
						
					String content = FileUtilities.read(response.getEntity().getContent());
					org.jsoup.nodes.Document doc = Jsoup.parse(content, uri);

					/*
					 
			'title': '.gs_rt a *::text',
            'url': '.gs_rt a::attr(href)',
            'related-text': '.gs_ggsS::text',
            'related-type': '.gs_ggsS .gs_ctg2::text',
            'related-url': '.gs_ggs a::attr(href)',
            'citation-text': '.gs_fl > a:nth-child(1)::text',
            'citation-url': '.gs_fl > a:nth-child(1)::attr(href)',
            'authors': '.gs_a a::text',
            'description': '.gs_rs *::text',
            'journal-year-src': '.gs_a::text',
					 
					 */
					
					Elements items = doc.select("div.gs_r");
					for (org.jsoup.nodes.Element e: items) {
						String url = null;
						// First, check to see if a full text document is available.  If so, use that as the url.
						Elements links = e.select("div.gs_ggsd a");
						for (Element linkElement: links) {
							if (linkElement.text().contains("Find Text @ NCSU") == false) {
								url = linkElement.attr("href");
								break;
							}
						}
						
						Element link = e.select("h3.gs_rt a").first();
						if (link == null) { continue; }
						String title = link.text(); 
						if (url == null) { url = link.attr("href"); }
						String description = null; 
						try {
							description = e.select(".gs_rs").first().text();
						} catch (NullPointerException npe) {
							srcLogger.log(Level.WARNING,"No description fround in google scholar handler, null pointer exception");
						}
						if (title != null || url != null || description !=null)	{
							position++;
							SearchRecord r = new SearchRecord(title,url,description,position,SOURCE_HANDLER_NAME);
							result.add(r);
						}
					}
					
					// Next, let's get the next page ...
					try {
						Element nextLink = doc.select("a:contains(Next):has(span.gs_ico_nav_next)").first();
						uri = nextLink.attr("href");
						if (uri.startsWith("http") == false) {
							uri = "https://scholar.google.com" + uri;
						}
						//System.out.println(uri);	
					}
					catch (Exception e) {
						uri = null; // link not found
					}
				} 
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "googlescholar exception: ",e);
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

	private String createURIString(String searchCriteria, int numResults, JSONObject scholarConfig) throws URISyntaxException {
		try {
			String encodedCriteria = URLEncoder.encode(searchCriteria, "UTF-8");
			String result = "https://scholar.google.com/scholar?hl=en&q=" + encodedCriteria;
			
			if (scholarConfig.has("startYear"))  { result += "&as_ylo=" + scholarConfig.getString("startYear");   }
			if (scholarConfig.has("endYear"))  { result += "&as_yhi=" + scholarConfig.getString("endYear");   }
			
			return result;
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "create googlescholar URI: "+searchCriteria,e);
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
		
		GoogleScholarHandler fh = new GoogleScholarHandler();
		fh.setUserAgent("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36 Edge/15.15063");
		JSONObject configuration = new JSONObject().put("googlescholar",new JSONObject().put("length", 100));
		
		List<SearchRecord> results = fh.generateSearchResults("test","\"search text\"",configuration, 100, new JSONObject());
		if (results !=null) {
			results.forEach(System.out::println);
		}
		
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("C:\\tmp\\records.csv"));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withHeader("Title", "Description", "URL"));
            ) 
        {
        	for (SearchRecord sr: results) {
        		csvPrinter.printRecord(sr.getName(),sr.getDescription(),sr.getUrl());
        	}
        	csvPrinter.flush();
        	csvPrinter.close();
        }
        catch (Exception e) {
        	System.err.println(e);
            }		
		for (SearchRecord sr: results) {
			System.out.print("Downloading: "+sr.getUrl() + " -  ");
			String name = sr.getName().replace(':', '_').replace('?','_').replace('"', '_');
			boolean result = PubMedDownloadFullReport.downloadCitationFullReportWithStartingURL(name, sr.getUrl(), name, "C:/temp/"+name+".html", "C:/temp/"+name+".pdf");
			System.out.println(result);
		}
	}	
	
	
	
}
