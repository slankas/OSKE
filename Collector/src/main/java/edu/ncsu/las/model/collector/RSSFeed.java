package edu.ncsu.las.model.collector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import com.google.common.cache.CacheLoader;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.RSS;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.json.JSONUtilities;

/**
 * 
 * 
 */
public class RSSFeed {
	private static Logger logger =Logger.getLogger(Collector.class.getName());

	private String _domain;
	private List<String> _urls;
	private List<String> _keywords; 
	
	private java.util.List<SearchRecord> _feedEntries;
	private JSONArray _feedEntriesAsJSON;
	
	private static  com.google.common.cache.LoadingCache<String,RSSFeed> _rssFeedCache = com.google.common.cache.CacheBuilder.newBuilder()
			.expireAfterWrite(600, TimeUnit.SECONDS)
            .build(new CacheLoader<String, RSSFeed>() {
                public RSSFeed load(String key) throws Exception {
                	RSSFeed rssfeed;
                	if (key.contains("#")) {
                		String[] data = key.split("#");
                		rssfeed = new RSSFeed(data[0],data[1]);                		
                	}
                	else {
                		rssfeed = new RSSFeed(key);
                	}
                	rssfeed.processFeeds();
        			return rssfeed;
                  }
                });
	
	private static  com.google.common.cache.LoadingCache<String,String> _feedTitleCache = com.google.common.cache.CacheBuilder.newBuilder()
			.expireAfterWrite(14, TimeUnit.DAYS)
			.maximumSize(2000)
            .build(new CacheLoader<String, String>() {
                public String load(String url) throws Exception {
                	String userAgent = SourceHandlerInterface.getNextUserAgent(Domain.DOMAIN_SYSTEM);
            		//String userAgent = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBCRAWLER_USERAGENTSTRING);
            		JSONObject result = RSS.validateRSSFeed(url, userAgent);
            		return result.optString("title", url);
                  }
                });

	public static String getRSSFeedTitle(String url) {
		try {
			return _feedTitleCache.get(url);
		} catch (ExecutionException e) {
			logger.log(Level.SEVERE,"Unable to get feed title (url: "+url+")",e);
			return url;
		}	
	}
	
	private static String createCacheKey(String domain, String userID) {
		return domain+"#"+userID;
	}
	
	public static RSSFeed create(String domain, String userID) {
		try {		
			UserOption customFeeds = UserOption.retrieve(userID,domain, UserOptionName.BreakingNewsURLs);
			if (customFeeds == null) {
				return _rssFeedCache.get(domain);
			}
			else {
				return _rssFeedCache.get(createCacheKey(domain,userID));
			}
		} catch (ExecutionException e) {
			logger.log(Level.SEVERE,"Unable to create entry for breaking news",e);
			return null;
		}	
	}
	
	public static void invalidate(String domain, String userID) {
		_rssFeedCache.invalidate(createCacheKey(domain,userID));
	}
	
	private RSSFeed(String domain) {
		_domain = domain;
		_urls     = JSONUtilities.toStringList(Configuration.getConfigurationPropertyAsArray(_domain, ConfigurationType.NEWS_FEED_URLS));
		_keywords = JSONUtilities.toStringList(Configuration.getConfigurationPropertyAsArray(_domain, ConfigurationType.NEWS_FEED_KEYWORDS));
	}
	
	private RSSFeed(String domain, String userID) throws ExecutionException {
		_domain = domain;
		UserOption customFeeds = UserOption.retrieve(userID,domain, UserOptionName.BreakingNewsURLs);
		if (customFeeds == null ) {
			throw new ExecutionException("Unable to find user custom feed data", new Exception());
		}
		JSONObject obj = new JSONObject(customFeeds.getOptionValue());
		
		_keywords = JSONUtilities.toStringList(obj.getJSONArray("keywords"));
		_urls = new ArrayList<String>();
		JSONArray feeds = obj.getJSONArray("feeds");
		for (int i=0;i < feeds.length(); i++) {
			String url = feeds.getJSONObject(i).getString("url");
			_urls.add(url);
		}
	}
	
	public List<SearchRecord> getFeedEntries() {
		return new java.util.ArrayList<SearchRecord>(_feedEntries);
	}
	
	public JSONArray getFeedArray() {
		return _feedEntriesAsJSON;
	}
	
	private boolean useRSSEntry(SearchRecord sr, List<String> keywords) {
		if (keywords.size() == 0) { return true; }
		
		for (String keyword: keywords) {
			if (sr.getName().toLowerCase().contains(keyword)) { return true; }
			if (sr.getDescription().toLowerCase().contains(keyword)) { return true; }
			
		}
		
		return false;
	}
	
	private void processFeeds() {
		String userAgent = SourceHandlerInterface.getNextUserAgent(_domain);
		//String userAgent = Configuration.getConfigurationProperty(_domain, ConfigurationType.WEBCRAWLER_USERAGENTSTRING);
		
		_feedEntries = new java.util.ArrayList<SearchRecord>();
		
		for (String url: _urls) {
			logger.log(Level.FINEST, "Processing feed: " + url);
			
			try {
				List<SearchRecord> results = SearchRecord.getFeedEntries(url, userAgent);
				results.forEach( sr -> { if (useRSSEntry(sr, _keywords)) { sr.setDescription(Jsoup.parse(sr.getDescription()).text());  _feedEntries.add(sr); }}); // Jsoup removes html tags
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Unable to process feed: "+url,e);
			}
			
		}
		_feedEntries.sort(new Comparator<SearchRecord>() {
			@Override
			public int compare(SearchRecord o1, SearchRecord o2) {
				return o2.getPublishedDateTime().compareTo(o1.getPublishedDateTime());
			}
		});
		
		_feedEntriesAsJSON = new JSONArray();
		for (SearchRecord sr: _feedEntries) {
			_feedEntriesAsJSON.put(sr.toMinimalJSON());
		}
		
	}
	
			
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
		logger.log(Level.INFO, "Application Started");
		logger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);		

		
		System.out.println(System.currentTimeMillis());
		RSSFeed bn = create("testnewtech","-");
		System.out.println(System.currentTimeMillis());
		bn._feedEntries.forEach(System.out::println);
		long t = System.currentTimeMillis();
		bn = create("testnewtech","-");
		System.out.println( System.currentTimeMillis() - t);
		System.out.println(bn.getFeedArray().toString(4));
	}	
	
}
