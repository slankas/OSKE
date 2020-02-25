package edu.ncsu.las.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;

/**
 * Functionality to find possible RSS feeds on a given page.  The primary entry point is findRSSFeeds(String url, String content)
 * 
 *
 */
public class RSS {
	private static Logger logger = Logger.getLogger(RSS.class.getName());
	
	/**
	 * Represents an RSS feed entry found on an html page.
	 * 
	 *
	 */
	public static class RSSEntry {
		
		public String _url;
		public String _title;
		
		public RSSEntry(String url, String title) {
			_url = url;
			_title = title;
		}
		
		public String getURL() { 
			return _url;
		}
		
		public String getTitle() {
			return _title;
		}
		
		public boolean equals(Object o) {
			if (o instanceof RSSEntry == false) {
				return false;
			}
			RSSEntry other = (RSSEntry) o;
			if ( _url.equals(other._url)) { return true; } // && _title.equals(other._url);
			
			//logic to deal with one href ending in a slash, but the other one not ..
			if (_url.endsWith("/")) {
				return _url.equals(other._url+"/");
			}
			
			if (other._url.endsWith("/")) {
				return other._url.equals(this._url+"/");
				
			}
			return false;
		}
		
		public int hashCode() {
			if (_url.endsWith("/")) {
				return _url.substring(0, _url.length()-1).hashCode();
			}
			
			return _url.hashCode();
			/*  for right now, just use the URL to be distinct.
		    long result = _url.hashCode();
		    result = result * 31 + _title.hashCode();
		    return (int) result;
		    */
		}
		
		public String toString() {
			return "(title: "+_title+", url: "+_url+")";
		}
		
		public JSONObject toJSON() {
			JSONObject result = (new JSONObject()).put("title", _title).put("url", _url);
			return result;
		}
	}

	
	public static boolean isRSSFeed(String url, String userAgentString) {
		try {
			Response resp = Jsoup.connect(url).followRedirects(true)
					                          .timeout(10*1000)
					                          .userAgent(userAgentString)
					                          .execute();
			String contentType = resp.contentType(); 
			return contentType !=null && (contentType.startsWith("application/rss+xml") || contentType.startsWith("application/atom+xml"));
			
		} catch (IOException e) {
			logger.log(Level.INFO, "Unable to test for isRSSFeed("+url+"): "+e);
		}
			
		return false;
	}
	
	/**
	 * Looks from RSS feeds on HTML page for <link> tags.  These tags must have a "rel" attribute of "alternate" and a 
	 * content type of  "application/rss+xml" or "application/atom+xml".  The title and href are extracted and placed into an RSSEntry
     *
	 * see http://www.rssboard.org/rss-autodiscovery
	 * 
	 * @param doc
	 * @return set of all possible RSS feeds from the link tag
	 */
	public static Set<RSSEntry> getRSSFeedFromMetaLink(Document doc) {
		HashSet<RSSEntry> result = new HashSet<RSSEntry>();
		
		Elements possibleLinks = doc.select("link[rel=alternate]");
		for (Element e: possibleLinks) {
			if (e.attr("type").equals("application/rss+xml") || e.attr("type").equals("application/rss+xml")) {
				String title = e.attr("title");
				String href  = e.attr("abs:href");
				RSSEntry re= new RSSEntry(href, title);
				result.add(re);
			}
		}
		
		return result;
	}

	public static Set<RSSEntry> getRSSFeedFromHyperLink(Document doc) {
		HashSet<RSSEntry> result = new HashSet<RSSEntry>();
		
		Elements possibleLinks = doc.select("a");
		for (Element e: possibleLinks) {
			String href = e.attr("abs:href");
			if ( href.contains("/rss/") ||
			     ( (href.contains("rss")  && href.endsWith(".xml"))) ||            //note: not testing "for /feed/".  Will be handled by testing URLs
				  href.contains("feeds.feedburner.com")) {
				String title = e.attr("title");
				if (title.equals("")) {
					title = e.text();
				}
				
				RSSEntry re= new RSSEntry(href, title);
				result.add(re);
			}
			
		}
		
		return result;
	}
	
	
	
	public static Document getDocument(String url, String userAgentString) {
		try {
			Document d = Jsoup.connect(url).followRedirects(true)
					                          .timeout(10*1000)
					                          .userAgent(userAgentString)
					                          .get();
			return d;
		} catch (IOException e) {
			logger.log(Level.INFO, "Unable to getDocument("+url+"): "+e);
		}
			
		return null;
	}
	
	
	/**
	 * 	
	 * possible RSS feeds = {}

		if page content can be retrieved (either by Accumulo / or from URL)
		  - test for <link rel="alternate" type="application/rss+xml" title="???" href="http://feeds.feedburner.com/TheRssBlog">
		    type can be "application/rss+xml" or "application/atom+xml"
		  - look for links like <a href="http://www.nytimes.com/services/xml/rss/nyt/Europe.xml">Europe</a>
		  - look for links like http://feeds.feedburner.com/realclearpolitics/qlMj
		  - search for hyperlinks that have the string somewhere within the entire <a> element
		    use title or text for the title of the rss feed
		  
		  
		create a set of URLs   (http://domainName/{rss|feed)).  Use the same http / https as the originating.  Allow redirects
		- if mime-type is rss/xml just add,
		- otherwise, examine for links like the above.
	
	 * 
	 * @param url
	 * @param content if we already have the URL's content, it is in this string.  If this value is null, will retrieve the content from the url
	 * @return
	 */
	public static Set<RSSEntry> findRSSFeeds(String url, String content, String userAgentString) {
		HashSet<RSSEntry> result = new HashSet<RSSEntry>();
		Document doc = null;
		
		if (content == null) {
			doc = RSS.getDocument(url,userAgentString);
		}
		else {
			doc = Jsoup.parse(content, url);
		}

		result.addAll(RSS.getRSSFeedFromMetaLink(doc));
		result.addAll(RSS.getRSSFeedFromHyperLink(doc));

		try {
			String baseURL = InternetUtilities.getBaseURL(url);
			result.addAll(getRSSFeedFromBaseLocation(baseURL,userAgentString));
			result.addAll(getRSSFeedFromBaseLocation(baseURL +"feed",userAgentString));
			result.addAll(getRSSFeedFromBaseLocation(baseURL +"rss",userAgentString));
			result.addAll(getRSSFeedFromBaseLocation(baseURL +"rss-help",userAgentString));
		}
		catch (MalformedURLException e) {
			logger.log(Level.INFO, "Unable to test /feed and /rss urls for ("+url+"): "+e);			
		}
		
		return result;
	}

	public static HashSet<RSSEntry> getRSSFeedFromBaseLocation(String url, String userAgentString ) {
		HashSet<RSSEntry> result = new HashSet<RSSEntry>();
		
		if (RSS.isRSSFeed(url,userAgentString)) {
			result.add(new RSSEntry(url,url));
		}
		else {
			Document feedDoc = RSS.getDocument(url,userAgentString);
			if (feedDoc != null) {
				result.addAll(RSS.getRSSFeedFromMetaLink(feedDoc));
				result.addAll(RSS.getRSSFeedFromHyperLink(feedDoc));
			}
		}
		return result;
	}
	
	
	public static JSONObject validateRSSFeed(String url, String userAgent) {
		String s = null;
		try {
			Response resp = Jsoup.connect(url).followRedirects(true)
                    .timeout(10*1000)
                    .userAgent(userAgent)
                    .execute();
			
			s = resp.body();
			// The SyndFeedInput doesn't handle document types / XML declarations, so remove.
			s = s.replaceAll("<!DOCTYPE.*?>","");
			s = s.replaceAll("<\\?xml.*?>", "");
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(new java.io.ByteArrayInputStream(s.getBytes()),true));
			JSONObject result = new JSONObject().put("url",resp.url())
					                            .put("title", feed.getTitle())
					                            .put("size", feed.getEntries().size())
					                            .put("status", "success");
			return result;
		} catch (IOException | IllegalArgumentException | FeedException e) {
			System.out.println(s);
			JSONObject result = new JSONObject().put("url", url)
					                            .put("error", e.toString())
					                            .put("status", "failure");
			return result;
		}		
	}
	
}
