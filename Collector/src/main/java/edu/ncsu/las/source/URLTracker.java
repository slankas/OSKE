package edu.ncsu.las.source;

public class URLTracker {
	private java.util.HashSet<String> _urlsToCrawl = new java.util.HashSet<String>();
	private java.util.HashSet<String> _urlsCrawled = new java.util.HashSet<String>();;

	public URLTracker() {
	}
	
	/**
	 * When we see a new URL during crawling, add it to the list.  This method also checks that we have not
	 * yet crawled that url.
	 * 
	 * @param url
	 */
	public void addURLToCrawl(String url) {
		if (_urlsCrawled.contains(url) == false) {
			_urlsToCrawl.add(url);
		}
	}
	
	public void markURLCrawled(String url) {
		_urlsToCrawl.remove(url);
		_urlsCrawled.add(url);
	}
	
	/** 
	 * gets the next member URL to crawl.  Returns null if empty.
	 * @return
	 */
	public String getNextURLToCrawl() {
		if (_urlsToCrawl.size() >0) {   return _urlsToCrawl.iterator().next(); }
		else { return null; }
	}
	

	public java.util.HashSet<String> getURLsToCrawl() {
		return _urlsToCrawl;
	}
	
	public int crawlSize() { return _urlsToCrawl.size(); }
	public int crawledSize() { return _urlsCrawled.size(); }
}