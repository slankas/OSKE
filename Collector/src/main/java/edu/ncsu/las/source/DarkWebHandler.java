package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.util.InternetUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONObject;




/**
 * DarkWebHandler provides a mechanism to query/crawl dark web site.
 * This is currently accomplished by using a 3rd party proxy service - onion.cab
 * This service does put an agreement flag on the hits to it until, the user agrees to understanding the ramifications (ie., they aren't completely hidden).
 * When the user clicks on the agreement flag, a cookie is created on the client side and stored.  The cookie is computed by taking the MD5 of two values produced 
 * of the original page and a javascript page.  output of that is in md5 format.  
 * 
 * We also need to convert ".onion" links to ".onion.cab" - only necessary for the first URL.  The proxy service converts all other URLs.
 *
 * Note: onion.link is also a another good starting point.
 *   
 */
public class DarkWebHandler extends WebSourceHandler implements SourceHandlerInterface {

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\": 2000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	
	private static final String SOURCE_HANDLER_NAME = "darkweb";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Darkweb Source Handler";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}
	}};
	
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
		return "Provides an interface to the dark through using a proxy service.  URLs with at domain of \"*.onion\" are re-written to utilize that proxy service as needed.";
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}
	
	/**
	 * If a url domain name has ".onion", replace with ".onion.cab"
	 * @param url
	 * @return
	 */
	public static String getProxiedURL(String url) {
		url  = url.replaceAll("(https{0,1}://)([A-Za-z0-9]+)(\\.onion/|\\.onion$)(.*$)", "$1$2.onion.link/$4");  // change from onion.cab to onion.link
		return url;
	}
					
	public void process() {
		java.util.List<String> errors = this.validateConfiguration(this.getDomainInstanceName(),this.getJob().getPrimaryFieldValue(), this.getJob().getConfiguration());
		if (errors.size()>0) {
			srcLogger.log(Level.SEVERE, "Unable to process darkWeb - invalid parameters: "+this.getJob().getPrimaryFieldValue());
			for (String error: errors) {
				srcLogger.log(Level.SEVERE, "    "+error);
			}
			
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.INVALID_PARAMS, "Unable to process darkWeb - invalid parameters: "+this.getJob().getPrimaryFieldValue());
			this.setJobHistoryStatus(JobHistoryStatus.INVALID_PARAMS);
			return;
		}
		
		ArrayList<String> proxiedSeeds = new ArrayList<String>();
		for (String url: InternetUtilities.expandURLs(this.getJob().getPrimaryFieldValue())) {
			proxiedSeeds.add(DarkWebHandler.getProxiedURL(url));
		}

				
		this.processInternal(proxiedSeeds);
	}	
	
	/**
	 * simply produces a search records based upon the passed into URL to be used within the domain discovery area. Differs slightly from the general web source handler in that it sends 
	 * it through a proxy service
	 */
	@Override
	public List<SearchRecord> generateSearchResults(String domain, String urlOrSearchTerms, JSONObject configuration, int numResults, JSONObject advConfiguration) {
		List<SearchRecord> records = new java.util.ArrayList<SearchRecord>(); 
		records.add(new SearchRecord(urlOrSearchTerms,DarkWebHandler.getProxiedURL(urlOrSearchTerms),"",1,SOURCE_HANDLER_NAME));
		
		return records;
	}	
	
	
	public static void main(String args[]) throws Exception {		
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);
		
		
		System.exit(1);
		
	}	
}