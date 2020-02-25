package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.json.JSONObject;


/**
 * FeedHandler
 * The feed handler will take a URL for a feed, get the contents of the feed, and then
 * use the links in the content to act as the initial seeds for a web crawl.
 * 
 */
public class FeedHandler extends WebSourceHandler implements SourceHandlerInterface, DomainDiscoveryInterface {

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\":2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");

	private static final String SOURCE_HANDLER_NAME = "feed";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "RSS Feed Handler";

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
		return "Processes URLs from a feed (RSS, ATOM, etc.). The handler first queries the feed to get the list of the URLs.  From that, it initializes a WebSource Handler / crawler to use those URLs as the seeds.";
	}
	
	public java.util.List<SearchRecord>  getFeedEntries(String domain, String feedURL, int numTriesRemaining) {
		java.util.List<SearchRecord> seeds = null;
		try {
			String userAgent = SourceHandlerInterface.getNextUserAgent(domain);
			seeds = SearchRecord.getFeedEntries(feedURL,userAgent);
		}
		catch (Exception e) {
			if (e instanceof java.net.ConnectException && numTriesRemaining >0) {
				srcLogger.log(Level.INFO, "conection exception, retrying: "+feedURL +", exception:" + e);
				try {
					TimeUnit.MILLISECONDS.sleep(new SecureRandom().nextInt(1000) + 1000); // sleep for a random time between 1 and 2 seconds.
				} catch (InterruptedException e1) {
					srcLogger.log(Level.SEVERE, "Unable to sleep",e);
				}
				
				return this.getFeedEntries(domain, feedURL, numTriesRemaining-1);
			}
			else {
				srcLogger.log(Level.SEVERE, "Unable to process feed: "+feedURL,e);
				this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process feed: "+feedURL+", exception: "+e);
				this.setJobHistoryStatus(JobHistoryStatus.ERRORED);	
				return null;
			}
		}
		return seeds;
	}
	
	public void process() {
		String feedURL = this.getJob().getPrimaryFieldValue();
		java.util.List<SearchRecord> seeds = null; 
		try {
			seeds = this.getFeedEntries(this.getDomainInstanceName(),feedURL,2);
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "Unable to process feed: "+feedURL);
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process feed: "+feedURL+", exception: "+e);
			this.setJobHistoryStatus(JobHistoryStatus.ERRORED);	
			return;
		}
		
		if (seeds != null && seeds.size() > 0) {
			List<String> stringURLs = seeds.stream().map( result -> result.getUrl()).collect(Collectors.toList());
			
			this.processInternal(stringURLs);
		}
		else {
			srcLogger.log(Level.SEVERE, "Unable to process feed: "+feedURL+", no results found in feed");
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED,"Unable to process feed: "+feedURL+", no results returned in feed.");
			this.setJobHistoryStatus(JobHistoryStatus.ERRORED);
		}
	}
	
	/**
	 * Calls getFeedEntries with the feed/RSS URL to generate the search results
	 */
	@Override
	public List<SearchRecord> generateSearchResults(String domain, String urlOrSearchTerms, JSONObject configuration, int numResults, JSONObject advConfiguration) {
		List<SearchRecord> records = this.getFeedEntries(domain, urlOrSearchTerms,2);
		return records;
	}
	
	
	public static void main(String args[]) throws Exception {
		
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
		srcLogger.log(Level.INFO, "FeedHandler Test Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);	

		FeedHandler fh = new FeedHandler();
		fh.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36");
		
		//java.util.List<SearchRecord> results = fh.getFeedEntries("system","https://www.google.com/alerts/feeds/08975834481537808953/5972174373636429622");
		
		//java.util.List<SearchRecord> results = fh.getFeedEntries("system","https://www.nytimes.com/services/xml/rss/nyt/Economy.xml");
		java.util.List<SearchRecord> results = fh.getFeedEntries("system","http://www.iran-daily.com/News/Feed/",2);
		results.forEach(System.out::println);
		
	}
	
}
