package edu.ncsu.las.collector.util;

public class ApplicationConstants {
	public static final java.util.UUID UUID_DEFAULT = new java.util.UUID(0L,0L);

	public static final String INCLUDE = "include";
	public static final String EXCLUDE = "exclude";

	public static final String VISITED_PAGE_STATUS_NEW           = "new";        // first time that we've seen this page
	public static final String VISITED_PAGE_STATUS_UNCHANGED    = "unchanged";   // we've crawled the same URL before and the content hasn't changed
	public static final String VISITED_PAGE_STATUS_OTHER_SOURCE = "othersource"; // we've seen the exact same content, but at another location
	public static final String VISITED_PAGE_STATUS_IRRELEVANT   = "irrelevant";  // page isn't currently relevant to what we are trying to process
	public static final String DATE_FORMAT = "yyyyMMdd HH:mm:ss"; 
	
}