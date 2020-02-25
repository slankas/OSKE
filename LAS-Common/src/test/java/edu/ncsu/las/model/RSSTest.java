package edu.ncsu.las.model;

import static org.testng.Assert.assertEquals;

import java.net.MalformedURLException;

import org.testng.annotations.Test;

/**
 * 
 * 
 *
 */
public class RSSTest {
	
	// List of popular RSS feeds taken from https://feeder.co/knowledge-base/rss-content/popular-rss-feeds/
	public static String[] POPULAR_FEEDS = { 
			        "http://xkcd.com/atom.xml",
	                "http://feeds.feedburner.com/CssTricks",
	                "http://feeds.feedburner.com/Explosm",
	                "http://feeds.bbci.co.uk/news/rss.xml",
	                "http://feeds.bbci.co.uk/news/world/rss.xml",
	                "http://feeds.hanselman.com/ScottHanselman",
	                "http://rss.slashdot.org/Slashdot/slashdot",
	                "http://www.engadget.com/rss.xml",
	                "http://rss.cnn.com/rss/cnn_topstories.rss",
	                "http://www.nytimes.com/services/xml/rss/nyt/HomePage.xml",
	                "http://www.nytimes.com/services/xml/rss/nyt/Business.xml",
	                "http://feeds.reuters.com/reuters/topNews",
	                "http://feeds.reuters.com/Reuters/worldNews",
	                "http://feeds.reuters.com/reuters/technologyNews",
	                "http://feeds.reuters.com/reuters/businessNews",
	                "http://feeds.mashable.com/Mashable",
	                "http://www.giantitp.com/comics/oots.rss",
	                "http://www.theverge.com/rss/index.xml",
	                "http://www.polygon.com/rss/index.xml",
	                "http://www.vox.com/rss/index.xml",
	                "http://feeds.arstechnica.com/arstechnica/index/",
	                "http://feeds.wired.com/wired/index",
	                "https://lifehacker.com/rss",
	                "http://feeds.feedburner.com/Techcrunch",
	                "http://www.smbc-comics.com/rss.php",
	                "http://www.dumbingofage.com/feed/",
	                "http://www.lemonde.fr/rss/une.xml",
	                "https://seths.blog/feed/",
	                "http://feeds.macrumors.com/MacRumors-All",
	                "http://feeds.feedburner.com/TEDTalks_video",
	                "http://www.npr.org/rss/rss.php?id=1001",
	                "http://feeds.feedburner.com/oatmealfeed",
	                "https://gizmodo.com/rss",
	                "http://cucumber.gigidigi.com/feed/",
	                "http://feeds.howtogeek.com/HowToGeek",
	                "http://thepunchlineismachismo.com/feed",
	                "http://feeds2.feedburner.com/TheNextWeb",
	                "http://feeds.feedburner.com/HighScalability",
	                "http://feeds.nature.com/nature/rss/current",
	                "http://feeds.feedburner.com/codinghorror",
	                "https://news.ycombinator.com/rss",
	                "http://oglaf.com/feeds/rss/",
	                "http://www.awkwardzombie.com/awkward.php",
	                "http://feeds.feedburner.com/acs/jacsat",
	                "http://feeds.feedburner.com/satwcomic",
	                "http://www.androidpolice.com/feed/",
	                "http://feeds.searchengineland.com/searchengineland",
	                "https://kotaku.com/rss",
	                "http://blog.humblebundle.com/rss",
	                "http://contentmarketinginstitute.com/feed/",
	                "http://krebsonsecurity.com/feed/",
	                "https://www.schneier.com/blog/atom.xml",
	                "http://blog.hubspot.com/marketing/rss.xml",
	                "http://waitbutwhy.com/feed",
	                "http://www.gunnerkrigg.com/rss.xml",
	                "http://www.paranatural.net/rss.php",
	                "https://nakedsecurity.sophos.com/feed/",
	                "http://feedpress.me/mozblog",
	                "http://feeds.feedburner.com/insideintercom",
	                "http://feeds.feedburner.com/seriouseatsfeaturesvideos",
	                "https://www.smashingmagazine.com/feed/",
	                "https://www.entrepreneur.com/topic/marketing.rss",
	                "https://m.signalvnoise.com/feed",
	                "https://www.buzzfeed.com/tasty.xml",
	                "https://martinfowler.com/feed.atom",
	                "https://feeder.co/blog.rss"};
	

	private static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.67 Safari/537.36";
	
	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testURLForRSS() throws MalformedURLException {
		
		assertEquals(RSS.isRSSFeed("https://krebsonsecurity.com/",userAgent),false);
		assertEquals(RSS.isRSSFeed("https://krebsonsecurity.com/feed/",userAgent),true);
		assertEquals(RSS.isRSSFeed("https://krebsonsecurity.cadsfadsfasdeom/feed/",userAgent),false);
		
	}

	
	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testPageForHeaderLinks() throws MalformedURLException {
		
		org.jsoup.nodes.Document d = RSS.getDocument("https://krebsonsecurity.com/",userAgent);
		System.out.println(RSS.getRSSFeedFromMetaLink(d));
		System.out.println(RSS.getRSSFeedFromHyperLink(d));
		

	}	

	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testPage() {
		//System.out.println(RSS.findRSSFeeds("https://krebsonsecurity.com/", null, userAgent));
		//System.out.println(RSS.findRSSFeeds("http://www.nytimes.com/2016/10/15/world/africa/kigali-deal-hfc-air-conditioners.html", null,userAgent));
		System.out.println(RSS.findRSSFeeds("https://news.mit.edu",null,userAgent));
	}

	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testValidatePopular() {
		for (String url: POPULAR_FEEDS) {
			System.out.println(RSS.validateRSSFeed(url, userAgent).toString());
		}
	}
	
	
}
