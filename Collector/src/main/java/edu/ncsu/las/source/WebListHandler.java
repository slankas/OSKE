package edu.ncsu.las.source;


import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * FeedHandler
 * 
 */
public class WebListHandler extends WebSourceHandler implements SourceHandlerInterface {

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{\"webCrawler\":{\"maxPagesToFetch\": 10000,\"politenessDelay\":200,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":2,\"respectRobotsTxt\":true},\"limitToHost\":true}");
	private static final String SOURCE_HANDLER_NAME = "weblist";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Web List Handler";

	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

			put("weblist", new SourceParameter("weblist", "JSON Object containing an arrary of seed URLS.",true,"",false,SourceParameterType.JSON_OBJECT,false,true));
		    put("weblist.seedURLs", new SourceParameter("weblist.seedURLs", "array of seed URLs",true,"['url']",true,SourceParameterType.STRING,false,true));
	}};
	
	@Override
	public boolean supportsDomainDiscovery() {
		return false;
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
		return "The Web List Handler functions the same as normal \"web\" handler (\"Web Crawler\"), except that an array of URLs is used as the seed for a web crawl rather than a specific URL.";
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}	
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.NOT_APPLICABLE;
	}	
	
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();	
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));
		
		if (errors.size() == 0) { // no errors found, now validate to make sure are URLs are valid
			JSONArray seedURLs = configuration.getJSONObject("weblist").getJSONArray("seedURLs");
			
			int blankURLCount = 0;
			
			for (int i=0;i< seedURLs.length();i++) {
				String url = seedURLs.getString(i); 
				if (url.trim().length() == 0) { blankURLCount++; continue; }
				try {
					new URL(url);
				}
				catch (MalformedURLException mue) {
					errors.add("Invalid URL: "+url);
				}
			}
			
			if (seedURLs.length() == 0 || blankURLCount == seedURLs.length()) {
				errors.add("No seed URLs specificed in the array");
			}
		}
		
		return errors;
	}	
	
	
	public void process() {
		JSONArray seedURLs = this.getJob().getConfiguration().getJSONObject("weblist").getJSONArray("seedURLs");
		
		java.util.ArrayList<String> seeds = new java.util.ArrayList<String>();
		for (int i=0;i< seedURLs.length();i++) {
			if (seedURLs.getString(i).trim().length() > 0) {
				seeds.add(seedURLs.getString(i));
			}
		}
		
		this.processInternal(seeds);
	}
}
