package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.document.ForumUserMerger;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.HTMLUtilities;
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;
import edu.ncsu.las.util.json.JSONUtilities;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * ForumHandler extracts threads, posts, and member information from a starting forum.
 * 
 * This handler requires extensive configuration based up a site's user of HTML and CSS to extract the various elements.
 * 
 */
public class ForumHandler extends AbstractHandler implements SourceHandlerInterface {
	static final Logger srcLogger =Logger.getLogger(ForumHandler.class.getName());
	
	public static String LOGIN_ERROR_MESSAGE = "invalid username or password";
	
	private ZoneId _zone;
	
	private URLTracker _seenMembers = new URLTracker();
	private URLTracker _seenURLs    = new URLTracker();
	private URLTracker _seenImages  = new URLTracker();
	
	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{}");
	private static final String SOURCE_HANDLER_NAME = "forum";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Forum Handler";

	/*
	public static final String LINK_TYPE_NAMED = "named";
	public static final String LINK_TYPE_WITH_ID = "withID";
*/
	public static final String LINK_TYPE_ID_ONLY = "idOnly";
	
	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			put("retrieveImages", new SourceParameter("retrieveImages", "Should images be retrieved when crawling this forum site? defaults to false", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("retrieveURLs",   new SourceParameter("retrieveURLs",   "Should URLs link from posts be retrieved when crawling this forum site? defaults to false", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("retrieveMemberInfo", new SourceParameter("retrieveMemberInfo", "Should detailed member information be retrieved? defaults to true", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("mergeMemberRecords", new SourceParameter("mergeMemberRecords", "Should we merge a member record with the result of a prior run?  Default is true.  If set to false, then the user record in ElasticSearch will be replaced with the latest version.", false,"true",false,SourceParameterType.BOOLEAN,false,true));			
			put("defaultTimeZone",  new SourceParameter("defaultTimeZone", "What is the default time zone ID for this site? Possible values: http://www.javadb.com/list-possible-timezones-or-zoneids-in-java/", true,"US/Pacific",false,SourceParameterType.STRING,false,true));
			put("lastCrawlEpochMillis", new SourceParameter("lastCrawlEpochMillis", "When was this job last executed?  Defaults to Unix Epoch and is then updated on every run", false,"1488418007988",false,SourceParameterType.LONG,false,true));

			put("forumsToCrawl",    new SourceParameter("forumsToCrawl", "Array of forum URLs to be crawled",true,"",true,SourceParameterType.STRING,false,true));
			
			put("linkType",    new SourceParameter("linkType", "Structure of the links. possible values 'idOnly.  'idOnly' will have the format of site.com/viewforum.php?f=3 ",true,"idOnly",false,SourceParameterType.STRING,false,true));
			
			put("userAgentString",    new SourceParameter("userAgentString", "What UserAgent string should be sent by the crawler.  Uses webcrawler.userAgentString for the domain if not set.",false,"",false,SourceParameterType.STRING,false,true));
			put("requestDelay",    new SourceParameter("requestDelay", "Number of milliseconds to pause in between requests.  Will be a random value between .5 and 1 of this value.  Defaults to 1000",false,"2000",false,SourceParameterType.INT,false,true));

			put("board", new SourceParameter("board","JavaScript object that manages the crawl config for bulletin boards ", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.forum", new SourceParameter("board.forum","JavaScript object that manages the crawl config for forum pages within bulletin board sites.  The major purpose of crawling the forum page is to get the list of threads to crawl.", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.forum.pagingMethod", new SourceParameter("board.forum.pagingMethod","method the site uses to page through list of threads within a forum.  Possible values: relativeLink, number.  If relativeLink, the system uses the nextPageSelector to find hyperlink for the next page.  If number, then the system uses the paging object to step through the pages.  (see that object entry for more details)", true,"relativeLink",false,SourceParameterType.STRING,false,true));
			put("board.forum.nextPageSelector", new SourceParameter("board.forum.nextPageSelector","CSS Selector to get the next page of threads within a forum.  Only the first result found is used as the link may appear multiple times on a page (e.g., at the top and bottom)", true,"a[rel=next]",false,SourceParameterType.STRING,false,true));
			put("board.forum.threadSelector", new SourceParameter("board.forum.threadSelector","Used to find and iterate through each thread on the page", true,"div.forumbg:not(.announcement) ul.topics>li",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadURL", new SourceParameter("board.forum.findThreadURL","Used to get the URL for a thread.  Assumes a hyperlink(&gt;a&lt;) is selected.", true,"a.topictitle",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadTitle", new SourceParameter("board.forum.findThreadTitle","Used to extract the URL for a thread.  Can be any tag.  \"text()\" called to get the value", true,"a.topictitle",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadAuthor", new SourceParameter("board.forum.findThreadAuthor","Used to find the area that contains the author, which is extracted from the text of the \"a\" tag, the url from the href of that tag.", true,"dl dt div.list-inner div:contains(by)",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadStartDate", new SourceParameter("board.forum.findThreadStartDate","Used to find the area that contains the thread start(initial) date.  The date regular expression is used to scan the text of this area for the date.", true,"dl dt div.list-inner div:contains(by)",false,SourceParameterType.STRING,false,true));
			put("board.forum.findLastPoster", new SourceParameter("board.forum.findLastPoster","Used to find the hyperlink that points to the last poster.  The poster field is extracted from the text of the \"a\" tag, the url from the href of that tag.", true,"dd.lastpost a[class^=username]",false,SourceParameterType.STRING,false,true));
			put("board.forum.findLastPosterDate", new SourceParameter("board.forum.findLastPosterDate","Used to find the area that last post date.  The date regular expression is used to extract the date string.", true,"dd.lastpost",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadReplyCount", new SourceParameter("board.forum.findThreadReplyCount","Finds the areat that contains the text with the reply count.  ownText() called for extraction.  Leave blank to use ...", true,"dd.posts",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadViewCount", new SourceParameter("board.forum.findThreadViewCount","Finds the areat that contains the text with the view count.  ownText() called for extraction.  Leave blank to use ...", true,"dd.views",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadReplyText", new SourceParameter("board.forum.findThreadReplyText","If an area cannot be defined to uniquely get the reply count, use this setting to get the text.  The extract reply regular expression will then get the count.", true,"td.stats",false,SourceParameterType.STRING,false,true));
			put("board.forum.extractThreadReplyCount", new SourceParameter("board.forum.extractThreadReplyCount","Extracts the reply count using a regulrar expression", true,"([0-9]+) Repl",false,SourceParameterType.STRING,false,true));
			put("board.forum.findThreadViewText", new SourceParameter("board.forum.findThreadViewText","If an area cannot be defined to uniquely get the thread count, use this setting to get the text.  The extract view regular expression will then get the count.", true,"td.stats",false,SourceParameterType.STRING,false,true));
			put("board.forum.extractThreadViewCount", new SourceParameter("board.forum.extractThreadViewCount","Extracts the view count using a regular expression", true,"([0-9]+) View",false,SourceParameterType.STRING,false,true));
			put("board.forum.extractThreadViewCount", new SourceParameter("board.forum.extractThreadViewCount","Extracts the view count using a regular expression", true,"([0-9]+) View",false,SourceParameterType.STRING,false,true));
			put("board.forum.paging", new SourceParameter("board.forum.paging","JavaScript object that manages the paging if the method is set to number.  This method assumes that a number is placed at the end of url with a fixed/pre-determined delimiter such as . or &amp;page= ", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.forum.paging.endSeparator", new SourceParameter("board.forum.paging.endSeparator","text that separates the paging position (assumed to be at the end from the rest of the URL", true,"page=",false,SourceParameterType.STRING,false,true));
			put("board.forum.paging.startNumber", new SourceParameter("board.forum.paging.startNumber","what number should be used as the starting point", true,"1",false,SourceParameterType.INT,false,true));
			put("board.forum.paging.incrementValue", new SourceParameter("board.forum.paging.incrementValue","by how much should we increment each page/count", true,"1",false,SourceParameterType.INT,false,true));
			
			
			
			put("board.thread", new SourceParameter("board.thread","JavaScript object that manages the crawl config for thread pages within bulletin board sites.  Each thread page should containing the posts which bascially form the \"unit\" of extraction", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.thread.pagingMethod", new SourceParameter("board.thread.pagingMethod","method the site uses to page through list of posts within a thread.  Possible values: relativeLink, number.  If relativeLink, the system uses the nextPageSelector to find hyperlink for the next page.  If number, then the system uses the paging object to step through the pages.  (see that object entry for more details)", true,"relativeLink",false,SourceParameterType.STRING,false,true));
			put("board.thread.nextPageSelector", new SourceParameter("board.thread.nextPageSelector","CSS Selector to get the next page of posts within a thread.  Only the first result found is used as the link may appear multiple times on a page (e.g., at the top and bottom)", true,"a[rel=next]",false,SourceParameterType.STRING,false,true));
			put("board.thread.postSelector", new SourceParameter("board.thread.postSelector","Used to find and iterate through each post within a thread.", true,"div.post",false,SourceParameterType.STRING,false,true));
			put("board.thread.findDate", new SourceParameter("board.thread.findDate","Finds the text that contains the post date.  Date regular expression used to extract the date value.", true,"p.author",false,SourceParameterType.STRING,false,true));
			put("board.thread.findID", new SourceParameter("board.thread.findID","finds the tag containing the id with the post ID", true,"div.post",false,SourceParameterType.STRING,false,true));
			put("board.thread.extractPostID", new SourceParameter("board.thread.extractPostID","Extracts the post ID from an ID string found through findID", true,"p([0-9]+)",false,SourceParameterType.STRING,false,true));
			put("board.thread.findPostURL", new SourceParameter("board.thread.findPostURL","finds the hyperlink containing a link to the post's URL", true,"div.postbody>div[id^=post_content] h3 a",false,SourceParameterType.STRING,false,true));
			put("board.thread.findTitle", new SourceParameter("board.thread.findTitle","where is the post title?", true,"div.postbody h3",false,SourceParameterType.STRING,false,true));
			put("board.thread.findContent", new SourceParameter("board.thread.findContent","extracts the content from the page", true,"div.postbody div.content",false,SourceParameterType.STRING,false,true));
			put("board.thread.findAuthor", new SourceParameter("board.thread.findAuthor","finds a hyperlink to the author profile page.  text=author name.", true,"p.author a[class^=username]",false,SourceParameterType.STRING,false,true));
			put("board.thread.paging", new SourceParameter("board.thread.paging","JavaScript object that manages the paging if the method is set to number.  This method assumes that a number is placed at the end of url with a fixed/pre-determined delimiter such as . or &amp;page= ", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.thread.paging.endSeparator", new SourceParameter("board.thread.paging.endSeparator","text that separates the paging position (assumed to be at the end from the rest of the URL", true,"page=",false,SourceParameterType.STRING,false,true));
			put("board.thread.paging.startNumber", new SourceParameter("board.thread.paging.startNumber","what number should be used as the starting point", true,"1",false,SourceParameterType.INT,false,true));
			put("board.thread.paging.incrementValue", new SourceParameter("board.thread.paging.incrementValue","by how much should we increment each page/count", true,"1",false,SourceParameterType.INT,false,true));
			
			put("board.member", new SourceParameter("board.member","JavaScript object that manages the crawl config for a member/user page.", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.member.findUserNameText", new SourceParameter("board.member.findUserNameText","css selector that defines the area containing the user name", true,"h2.memberlist-title",false,SourceParameterType.STRING,false,true));
			put("board.member.extractUserName", new SourceParameter("board.member.extractUserName","regex that gets the actual username from the extracted text.", true,"- (\\w*)",false,SourceParameterType.STRING,false,true));
			put("board.member.findUserProperties", new SourceParameter("board.member.findUserProperties","CSS selector to fined user properties.  If type=dlList, use dt as the last item in the selector. For list, use li", true,"dl dt",false,SourceParameterType.STRING,false,true));
			put("board.member.userPropertiesType", new SourceParameter("board.member.userPropertiesType","How are user properties put on tha page.  Use \"dlList\" or \"list\". ", true,"dlList",false,SourceParameterType.STRING,false,true));
			put("board.member.userContentArea", new SourceParameter("board.member.userContentArea","Used for general text extraction and to select the area from which hyperlink and images links are extracted.", true,"div.panel",false,SourceParameterType.STRING,false,true));
			put("board.member.normalizeDateFields", new SourceParameter("board.member.normalizeDateFields","For extracted fields in the profile, which ones should have the date field normalized to ISO?", true,"[\"joined\",\"last_active\"]",true,SourceParameterType.STRING,false,true));
			put("board.member.searchProfileFields", new SourceParameter("board.member.searchProfileFields","What user profile fields should automatically be searched for Google?", true,"[\"interests\"]",true,SourceParameterType.STRING,false,true));
		
			
			put("board.datePattern", new SourceParameter("board.datePattern","JavaScript object ", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.datePattern.dateTime", new SourceParameter("board.datePattern.dateTime","Java date format for the date and time on a site.", true,"MMM dd y h:mm a VV",false,SourceParameterType.STRING,false,true));
			put("board.datePattern.date", new SourceParameter("board.datePattern.date","Java date format for just the date on a site.", true,"MMM dd y",false,SourceParameterType.STRING,false,true));
			put("board.regex", new SourceParameter("board.regex","JavaScript object that contains regular expressions used to extract content ", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.regex.extractForumIDFromURL", new SourceParameter("board.regex.extractForumIDFromURL","extracts the forum ID from a url.  Should only have 1 group present", true,"f=([\\d]+)",false,SourceParameterType.STRING,false,true));
			put("board.regex.extractThreadIDFromURL", new SourceParameter("board.regex.extractThreadIDFromURL","extracts the thread ID from a url", true,"t=([0-9]+)",false,SourceParameterType.STRING,false,true));
			put("board.regex.extractUserIDFromURL", new SourceParameter("board.regex.extractUserIDFromURL","extracts user ID from a url", true,"u=([0-9]+)",false,SourceParameterType.STRING,false,true));
			put("board.regex.extractDateString", new SourceParameter("board.regex.extractDateString","extracts the string containing a date from a larger string. Usefully for eliminating unnecessary content.", true,"[A-Z][a-z][a-z] [0-9][0-9], [0-9]{4} [0-9]{1,2}:[0-9]{2} [ap][m]",false,SourceParameterType.STRING,false,true));
			
			put("board.filterLinks", new SourceParameter("board.filterLinks","JavaScript object that ", true,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("board.filterLinks.url", new SourceParameter("board.filterLinks.url","JSON array containing regular expressions.  If a url matches one of the expressions, then it should be excluded.  This is useful for administrative links or other items we don't wish to crawl.", true,"[\"ucp.php.i=zebra\",\"sr=posts\",\"mode=email\"]",true,SourceParameterType.STRING,false,true));
			put("board.filterLinks.image", new SourceParameter("board.filterLinks.image","JSON array containing regular expressions.  If an image source matches one of the expressions, then it should be excluded.  This is useful for  images commonly used on sites that offer no value", true,"[\"images/ranks\"]",true,SourceParameterType.STRING,false,true));
			
			put("authenticationForm", new SourceParameter("authenticationForm","JavaScript object consisting of the following 5 fields to use Form authentication when crawling a site.  Logout pages should be specified in the exclude filter", false,"",false,SourceParameterType.JSON_OBJECT,false,true));
			put("authenticationForm.username", new SourceParameter("authenticationForm.username","what is the user's identity to access the site?", false,"",false,SourceParameterType.STRING,false,true));
			put("authenticationForm.password", new SourceParameter("authenticationForm.password","what is the user's password?  This should be entered as cleartext.  The value will be encrypted (using AES) and stored with a {AES} prefix.  If you start with {hashMD5}, then the following value will be hashed with MD5, and then encrypted.", false,"",false,SourceParameterType.STRING,true,false));
			put("authenticationForm.loginURL", new SourceParameter("authenticationForm.loginURL","what is the URL used to authenticate the user?", false,"http://www.atari-forum.com/ucp.php?mode=login",false,SourceParameterType.STRING,false,true));
			put("authenticationForm.userFieldName", new SourceParameter("authenticationForm.userFieldName","what is the field name used to specify the user's identity?", false,"",false,SourceParameterType.STRING,false,true));
			put("authenticationForm.passwordFieldName", new SourceParameter("authenticationForm.passwordFieldName","what is the field name used to specify/enter user's password? ", false,"",false,SourceParameterType.STRING,false,true));
			put("authenticationForm.additionalFormData", new SourceParameter("authenticationForm.additionalFormData","Array of java objects.  Each object has fieldName and fieldValue.  Used to set additional properties during the login process.", false,"",true,SourceParameterType.JSON_OBJECT,false,true));
			put("authenticationForm.additionalFormData.fieldName", new SourceParameter("authenticationForm.additionalFormData.fieldName","name of a field to pass to the server during a form-based authentication.", false,"login",false,SourceParameterType.STRING,false,true));
			put("authenticationForm.additionalFormData.fieldValue", new SourceParameter("authenticationForm.additionalFormData.fieldValue","value of a field to pass to the server during a form-based authentication.", false,"login",false,SourceParameterType.STRING,false,true));
	
			//TODO:  how do I make the SID logic in authenticate method configurable.  See authenticate.
			
			
			
	}};
		
	private DateTimeFormatter _dateFormatter; 
	private DateTimeFormatter _dateTimeFormatter;
	private Pattern[] _urlFilterPatterns;
	private Pattern[] _imageURLFilterPatterns;
	
	private Pattern _extractForumIDfromURLPattern;
	private Pattern _extractUserIDFromURLPattern;
	private Pattern _extractThreadIDFromURLPattern;
	private Pattern _extractDateStringPattern;
	
	@Override
	public void initialize(JobHistory jobRecord, Job job) {
		// make parameters available to the rest of the object (specifically so
		// that shouldVisit and visit can access)
		super.initialize(jobRecord, job);
		
		_dateFormatter = DateTimeFormatter.ofPattern(this.getJobConfigurationFieldAsString("board.datePattern.date"));
		_dateTimeFormatter = DateTimeFormatter.ofPattern(this.getJobConfigurationFieldAsString("board.datePattern.dateTime"));
		
		JSONArray urlPatterns = this.getJobConfigurationFieldAsJSONArray("board.filterLinks.url");
		_urlFilterPatterns = new Pattern[urlPatterns.length()];
		for (int i=0;i<_urlFilterPatterns.length;i++) {
			_urlFilterPatterns[i] = Pattern.compile(urlPatterns.getString(i));
		}
		
		JSONArray imagePatterns = this.getJobConfigurationFieldAsJSONArray("board.filterLinks.image");
		_imageURLFilterPatterns = new Pattern[imagePatterns.length()];
		for (int i=0;i<_imageURLFilterPatterns.length;i++) {
			_imageURLFilterPatterns[i] = Pattern.compile(imagePatterns.getString(i));
		}
	
		_extractForumIDfromURLPattern  = Pattern.compile(this.getJobConfigurationFieldAsString("board.regex.extractForumIDFromURL"));
		_extractUserIDFromURLPattern   = Pattern.compile(this.getJobConfigurationFieldAsString("board.regex.extractUserIDFromURL"));
		_extractThreadIDFromURLPattern = Pattern.compile(this.getJobConfigurationFieldAsString("board.regex.extractThreadIDFromURL"));
		_extractDateStringPattern      = Pattern.compile(this.getJobConfigurationFieldAsString("board.regex.extractDateString"));	
		
	}

	@Override
	public void forceShutdown() {
		this.stop();		
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
		return "Provides an interface to systematically crawl a forum-board based site";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.URL;
	}		
		
	
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}
	
	public java.util.List<String> validateConfiguration(JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		errors.addAll(this.getSourceParameterRepository().validateConfiguration(configuration));
		
		String linkType = configuration.optString("linkType",null);
		if (linkType != null && linkType.equals(LINK_TYPE_ID_ONLY) == false) {
			errors.add("linkType must be \\\"idOnly\\\".");
		}
		
		return errors;
	}		
	

	/**
	 * Authenticates the user to the site
	 * @param userAgentString 
	 * 
	 * @return
	 */
	public CloseableHttpClient authenticate() {
				
		CloseableHttpClient  httpclient;	
		
		String userAgentString = this.getJobConfigurationFieldAsString("userAgentString");
		
		if (userAgentString == null || userAgentString.equals("")) {
			userAgentString = SourceHandlerInterface.getNextUserAgent(this.getDomainInstanceName());
		}
		try {
			httpclient = HttpClients.custom().setUserAgent(userAgentString).build();
			/*  Use the following code to get the SID if necessary.  Otherwise let's skip it.
			HttpGet loginGet = new HttpGet(loginURL);
			String sid;
			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, loginGet)) {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, loginURL);
				sid = doc.select("input[name=sid]").first().attr("value");
			}
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "Unable to access login page, url: "+loginURL, e );
				return null;			
			}
			*/
			
			//formParameters
			
			ArrayList<NameValuePair> loginPostParameters;
			loginPostParameters = new ArrayList<NameValuePair>();
			loginPostParameters.add(new BasicNameValuePair(this.getJobConfigurationFieldAsString("authenticationForm.userFieldName"), this.getJobConfigurationFieldAsString("authenticationForm.username")));
			loginPostParameters.add(new BasicNameValuePair(this.getJobConfigurationFieldAsString("authenticationForm.passwordFieldName"), this.getJobConfigurationFieldAsString("authenticationForm.password")));
			
			JSONArray additionalFields = this.getJobConfigurationFieldAsJSONArray("authenticationForm.additionalFormData");
			for (int i=0;i< additionalFields.length(); i++) {
				JSONObject data = additionalFields.getJSONObject(i);
				loginPostParameters.add(new BasicNameValuePair(data.getString("fieldName"), data.getString("fieldValue")));
			}

			HttpPost loginPost = new HttpPost(this.getJobConfigurationFieldAsString("authenticationForm.loginURL"));
			loginPost.setEntity(new UrlEncodedFormEntity(loginPostParameters));

			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, loginPost)) {
				int code = response.getStatusLine().getStatusCode();
				if (code != HttpStatus.SC_MOVED_TEMPORARILY) {
					srcLogger.log(Level.SEVERE, "forum login HTTP Response code (expected 302 for a re-direct on good login): " + code);
					srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
					httpclient.close();
					return null;
				}							
			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "forum exception: " + e.toString());
				return null;
			} 			

		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;			
		}
		
		return httpclient;
	}
	
	/**
	 * Converts
	 * @param date
	 * @param gmtOffset
	 * @return
	 */
	public Instant toISOInstantDate(String date, ZoneId zoneID) {
		
		if (date.contains("Today")) {
			ZonedDateTime zdt = Instant.now().atZone(zoneID);
			String today = _dateFormatter.format(zdt);
			date = date.replace("Today", today);
		}
		else if (date.contains("Yesterday")) {
			ZonedDateTime zdt = Instant.now().atZone(zoneID).minusDays(1);
			String yesterday = _dateFormatter.format(zdt);
			date = date.replace("Yesterday", yesterday);
		}
		if (date.matches(".*?Week[s]? Ago")) {
			String numWeekStr = date.substring(0, date.indexOf(' '));
			int numWeek = Integer.parseInt(numWeekStr);
			return ZonedDateTime.now(zoneID).minusWeeks(numWeek).toInstant();
		}
		if (date.matches(".*?Day[s]? Ago")) {
			String numDayStr = date.substring(0, date.indexOf(' '));
			int numDay = Integer.parseInt(numDayStr);
			return ZonedDateTime.now(zoneID).minusDays(numDay).toInstant();
		}
		if (date.matches(".*?Hour[s]? Ago")) {
			String numHourStr = date.substring(0, date.indexOf(' '));
			int numHours = Integer.parseInt(numHourStr);
			return ZonedDateTime.now(zoneID).minusHours(numHours).toInstant();
		}
		
		if (date.length() <12) {  //if no time, just assume noon local time
			date += " " + "12:00 PM";
		}
		date = date.replaceAll("am", "AM");
		date = date.replaceAll("pm", "PM");
		
		date = date +" " + zoneID.toString();
		date = date.replaceAll(",", "");
		date = replaceAlternativeWhitespaceCharacters(date);
	
		ZonedDateTime zdt = ZonedDateTime.parse(date, _dateTimeFormatter);
		return zdt.toInstant();
	}
	
	public void normalizeDateField(JSONObject jo, String field) {
		if (jo.has(field)) {	
			try {
				String value = jo.getString(field);
				Matcher dateMatcher = _extractDateStringPattern.matcher(value);
				if (dateMatcher.find()) {
					value= dateMatcher.group(0);
				}
				String newValue = toISOInstantDate(value, _zone).toString();
				jo.put(field, newValue);
			}
			catch (Throwable t) {
				srcLogger.log(Level.WARNING, "Unable to normalize date field");
			}
		}
	}
	
	public JSONObject processThreadInfo(Element t, Instant startTime, Instant lastJobStartTime, String currentPageURL) {
		if (t.hasClass("moved")) {	return new JSONObject("{ \"continue\": true} "); }
		
		JSONObject threadInfo = new JSONObject();
		threadInfo.put("crawled_dt", startTime.toString());
		//threadInfo.put("infoTitle", t.select("div.threadinfo").first().attr("title"));
		threadInfo.put("threadTitle", t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadTitle")).text());
		
		try {
			String threadURL = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadURL")).first().attr("abs:href");  
			threadInfo.put("threadURL", threadURL);
			
			Matcher m = _extractThreadIDFromURLPattern.matcher(threadURL);
			if (m.find()) {
				String threadID = m.group(1);
				threadInfo.put("threadID",threadID);
			}
		}
		catch (NullPointerException npe) {
			srcLogger.log(Level.WARNING, "Unable to find thread URL.  selector: "+this.getJobConfigurationFieldAsString("board.forum.findThreadURL"));
			return null;
		}
		
		Elements authors = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadAuthor"));
		for (Element e: authors) {
			if (e.toString().contains("ast post")) {continue;}
			
			Element author = e.select("a").first();
			threadInfo.put("author", author.text().trim());
			String authorURL = author.attr("abs:href");
			_seenMembers.addURLToCrawl(authorURL);
			threadInfo.put("authorURL", authorURL);
			Matcher ma = _extractUserIDFromURLPattern.matcher(authorURL);
			if (ma.find()) {
				String authorID = ma.group(1);
				threadInfo.put("authorID",authorID);
			}
		}
		
		Elements startDates = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadStartDate"));
		for (Element e: startDates) {
			//extract thread start date
			String dateAreaText= e.text();
			Matcher postDateMatcher = _extractDateStringPattern.matcher(dateAreaText);
			if (postDateMatcher.find()) {
				String dateString = postDateMatcher.group(0);
				threadInfo.put("threadStartDate", this.toISOInstantDate(dateString,_zone).toString());
			}
		}
		
		//Last activity information - poster
		Element lastPostAuthor = t.select(this.getJobConfigurationFieldAsString("board.forum.findLastPoster")).first();
		threadInfo.put("latestPoster", lastPostAuthor.text().trim());
		String lastPosterURL = lastPostAuthor.attr("abs:href");
		_seenMembers.addURLToCrawl(lastPosterURL);
		threadInfo.put("latestPosterURL", lastPosterURL);
		Matcher lastPostIDMatcher = _extractUserIDFromURLPattern.matcher(lastPosterURL);
		if (lastPostIDMatcher.find()) {
			String lastPosterID = lastPostIDMatcher.group(1);
			threadInfo.put("latestPosterID",lastPosterID);
		}		
		
		//Last activity information - date
		String lastPostDateArea =t.select(this.getJobConfigurationFieldAsString("board.forum.findLastPosterDate")).text();
		Matcher lastpostDateMatcher = _extractDateStringPattern.matcher(lastPostDateArea);
		if (lastpostDateMatcher.find()) {
			String dateString = lastpostDateMatcher.group(0);
			Instant isoActivityDate = this.toISOInstantDate(dateString,_zone);
			if (isoActivityDate.isBefore(lastJobStartTime)) {
				srcLogger.log(Level.FINE, "Found old thread, stop looking for threads");
				return new JSONObject("{ \"aged\": true} ");
			}
			
			threadInfo.put("latestPostDate", isoActivityDate.toString());
		}		
		
		if (this.getJobConfigurationFieldAsString("board.forum.findThreadReplyCount").trim().length() >0) {
			String replyCount = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadReplyCount")).first().ownText();
			threadInfo.put("replyCount", getIntFromString(replyCount));
		}
		if (this.getJobConfigurationFieldAsString("board.forum.findThreadViewCount").trim().length() >0) {
			String viewCount = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadViewCount")).first().ownText();
			threadInfo.put("viewCount", getIntFromString(viewCount));
		}
		
		//Alternate methods to get Thread reply and thread view counts
		if (this.getJobConfigurationFieldAsString("board.forum.findThreadReplyText").trim().length() >0) {
			String countArea  = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadReplyText")).text();
			Pattern numberPattern = Pattern.compile(this.getJobConfigurationFieldAsString("board.forum.extractThreadReplyCount")); 
			Matcher countMatcher = numberPattern.matcher(countArea);
			if (countMatcher.find()) {
				String countStr = countMatcher.group(1).trim();
				int count = Integer.parseInt(countStr);
				threadInfo.put("replyCount",count);
			}
		}
		if (this.getJobConfigurationFieldAsString("board.forum.findThreadViewText").trim().length() >0) {
			String countArea  = t.select(this.getJobConfigurationFieldAsString("board.forum.findThreadViewText")).text();
			Pattern numberPattern = Pattern.compile(this.getJobConfigurationFieldAsString("board.forum.extractThreadViewCount")); 
			Matcher countMatcher = numberPattern.matcher(countArea);
			if (countMatcher.find()) {
				String countStr = countMatcher.group(1).trim();
				int count = Integer.parseInt(countStr);
				threadInfo.put("viewCount",count);
			}			
		}
		
		return threadInfo;
	}
	
	private int getIntFromString(String s) {
		s= s.replace(",", "").trim();
		return Integer.parseInt(s);
	}
	
	/**
	 * Processes a specific forum, looking for any threads with activity since the last run
	 * 
	 * @param httpclient
	 * @param url
	 * @param startTime this is when the current job has started.  no threads after this time should be processed
	 * @param lastJobStartTime this is when the last job started.  No threads before this time should be processed
	 * @return
	 */
	public JSONArray processForumPage(CloseableHttpClient  httpclient, String url, Instant startTime, Instant lastJobStartTime) {
		JSONArray results = new JSONArray();
		
		String getURL = url;
		
		String pagingMethod       = this.getJobConfigurationFieldAsString("board.forum.pagingMethod");
		String pageEndSeparator   = this.getJobConfigurationFieldAsString("board.forum.paging.endSeparator");

		int pageIncrementValue    = 0; 
		int currentPagePosition   = 0;
		
		try { 
			currentPagePosition = Integer.parseInt(this.getJobConfigurationFieldAsString("board.forum.paging.startNumber"));
			pageIncrementValue  = Integer.parseInt(this.getJobConfigurationFieldAsString("board.forum.paging.incrementValue"));
		} 
		catch (NumberFormatException nfe) {
			srcLogger.log(Level.WARNING, "Invalid starting number: "+this.getJobConfigurationFieldAsString("board.forum.paging.startNumber"));
			return results;
		}
		
		if (pagingMethod.equalsIgnoreCase("number")) {
			int lastIndex = getURL.lastIndexOf(pageEndSeparator);
			if (lastIndex >0) {
				getURL = getURL.substring(0,lastIndex);
			}
			getURL += pageEndSeparator + currentPagePosition;
		}
		
		int loopCount = 0;
		FORUM_LOOP:
		while (true) {
			loopCount++;
			HttpGet httpGet = new HttpGet(getURL);
			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, httpGet)) {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, getURL);
				
				//process threads
				Elements threads = doc.select(this.getJobConfigurationFieldAsString("board.forum.threadSelector"));
				for (org.jsoup.nodes.Element t: threads) {
					JSONObject threadInfo= processThreadInfo(t, startTime,lastJobStartTime,getURL);
					if (threadInfo == null) {
						srcLogger.log(Level.WARNING, "Skipping adding thread to forum, null result received");
						continue;
					}
					if (threadInfo.has("continue")) { continue; }
					if (threadInfo.has("aged"))  { 
						if (loopCount == 1) { // when checking the age of threads, sticky threads may be at the top.  Allow these to occur on the first page, but no others.
							continue;
						}
						else {
							break FORUM_LOOP;
						}
					}
					results.put(threadInfo);
				}
				
				//process next pages...
				if (pagingMethod.equalsIgnoreCase("number")) {
					currentPagePosition += pageIncrementValue;
					int lastIndex = getURL.lastIndexOf(pageEndSeparator);
					if (lastIndex >0) {
						getURL = getURL.substring(0,lastIndex);
					}
					getURL += pageEndSeparator + currentPagePosition;
					
					// does the URL exist on the page???
					Elements aLinks = doc.select("a[href*="+pageEndSeparator + currentPagePosition+"]");
					if (aLinks.size() ==0) {
						break;
					}
				}
				else {
					Elements items = doc.select(this.getJobConfigurationFieldAsString("board.forum.nextPageSelector"));
					if (items.size() > 0) {
						org.jsoup.nodes.Element e = items.get(0);
						getURL = e.attr("abs:href"); 
					}
					else {
						break;
					}
				}
			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "forum exception: ",e);
				return results;
			} 			
		}
		return results;
	}
	
	/**
	 * Grabs all of the posts from a "thread display" page.
	 * 
	 * @param httpclient
	 * @param threadInfo
	 * @param startTime this is when the current job has started.  no threads after this time should be processed
	 * @param lastJobStartTime this is when the last job started.  No threads before this time should be processed	 * @return
	 */
	public JSONArray processThreadPage(CloseableHttpClient  httpclient, JSONObject threadInfo, Instant startTime, Instant lastJobStartTime, String hostName ) {
		JSONArray results = new JSONArray();
		
		String getURL =  threadInfo.getString("threadURL");
		
		String pagingMethod       = this.getJobConfigurationFieldAsString("board.thread.pagingMethod");
		String pageEndSeparator   = this.getJobConfigurationFieldAsString("board.thread.paging.endSeparator");

		int pageIncrementValue    = 0; 
		int currentPagePosition   = 0;
		
		try { 
			currentPagePosition = Integer.parseInt(this.getJobConfigurationFieldAsString("board.thread.paging.startNumber"));
			pageIncrementValue  = Integer.parseInt(this.getJobConfigurationFieldAsString("board.thread.paging.incrementValue"));
		} 
		catch (NumberFormatException nfe) {
			srcLogger.log(Level.WARNING, "Invalid starting number: "+this.getJobConfigurationFieldAsString("board.thread.paging.startNumber"));
			return results;
		}
		
		if (pagingMethod.equalsIgnoreCase("number")) {
			int lastIndex = getURL.lastIndexOf(pageEndSeparator);
			if (lastIndex >0) {
				getURL = getURL.substring(0,lastIndex);
			}
			getURL += pageEndSeparator + currentPagePosition;
		}		
		

		while (true) {
			HttpGet testGet = new HttpGet(getURL);
			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, testGet)) {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, getURL);
								
				Elements posts = doc.select(this.getJobConfigurationFieldAsString("board.thread.postSelector"));
				
				for (org.jsoup.nodes.Element p: posts) {
					JSONObject postInfo = processPost(p, startTime, lastJobStartTime, getURL, threadInfo.getString("threadID"), hostName);
					if (postInfo.has("continue")) { continue; }
					results.put(postInfo);
				}
				
				//process next pages...
				if (pagingMethod.equalsIgnoreCase("number")) {
					currentPagePosition += pageIncrementValue;
					int lastIndex = getURL.lastIndexOf(pageEndSeparator);
					if (lastIndex >0) {
						getURL = getURL.substring(0,lastIndex);
					}
					getURL += pageEndSeparator + currentPagePosition;
					
					// does the URL exist on the page???
					Elements aLinks = doc.select("a[href*="+pageEndSeparator + currentPagePosition+"]");
					if (aLinks.size() ==0) {
						break;
					}					
				}
				else {
					Elements items = doc.select(this.getJobConfigurationFieldAsString("board.thread.nextPageSelector"));
					if (items.size() > 0) {
						org.jsoup.nodes.Element e = items.get(0);
						getURL = e.attr("abs:href"); 
						if (getURL.contains("&p=")) {
							getURL = getURL.substring(0,getURL.indexOf("&p="));
						}
					}
					else {
						break;
					}
				}
			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "Forum exception: ", e);
				return results;
			} 			
		}

		return results;
	}
	
	public JSONObject processPost(Element p, Instant startTime, Instant lastJobStartTime, String currentPageURL,String threadID, String hostName) {
		JSONObject postInfo = new JSONObject();
		postInfo.put("crawled_dt", startTime.toString());
		
		String dateTextArea = p.select(this.getJobConfigurationFieldAsString("board.thread.findDate")).text();
		try {
			Matcher postDateMatcher = _extractDateStringPattern.matcher(dateTextArea);
			if (postDateMatcher.find()) {
				String dateString = postDateMatcher.group(0);
				Instant isoActivityDate = this.toISOInstantDate(dateString,_zone);
				if (isoActivityDate.isBefore(lastJobStartTime)) {
					srcLogger.log(Level.FINER, "skipping old post");
					return new JSONObject("{ \"continue\": true} ");
				}
				
				postInfo.put("postDate", isoActivityDate.toString());
			}
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "forum unable to convert date: "+dateTextArea);
		}
		
		String postIDFullString = p.select(this.getJobConfigurationFieldAsString("board.thread.findID")).first().id();
		
		Pattern postIDPattern = Pattern.compile(this.getJobConfigurationFieldAsString("board.thread.extractPostID"));
		Matcher m = postIDPattern.matcher(postIDFullString);
		if (m.find()) {
			String postID = m.group(1);
			postInfo.put("postID",postID);
		}
		
		postInfo.put("threadID", threadID);
		postInfo.put("postTitle", p.select(this.getJobConfigurationFieldAsString("board.thread.findTitle")).text());
		postInfo.put("text", p.select(this.getJobConfigurationFieldAsString("board.thread.findContent")).text());
		postInfo.put("html", p.select(this.getJobConfigurationFieldAsString("board.thread.findContent")).html());
		
		postInfo.put("postURL", p.select(this.getJobConfigurationFieldAsString("board.thread.findPostURL")).first().attr("abs:href"));
		
		//author information
		Element authorElement = p.select(this.getJobConfigurationFieldAsString("board.thread.findAuthor")).first();
		postInfo.put("author",authorElement.text().trim());
		String authorURL = authorElement.attr("abs:href");
		_seenMembers.addURLToCrawl(authorURL);
		postInfo.put("authorURL", authorURL);
		
		Matcher ma = _extractUserIDFromURLPattern.matcher(authorURL);
		if (ma.find()) {
			String authorID = ma.group(1);
			postInfo.put("authorID",authorID);
			postInfo.put("authorDocumentID", this.createUserID(hostName, authorID));
		}
		
		JSONObject attributes = this.extractExtraFields( p.select(this.getJobConfigurationFieldAsString("board.thread.findContent")).html());
		if (attributes.length() >0) {
			postInfo.put("attributes", attributes);
		}
		
		//look for images
		JSONArray imageURLs = HTMLUtilities.extractImageURLs(p.select(this.getJobConfigurationFieldAsString("board.thread.findContent")).select("img"));
		filterMatchingHREFs(_imageURLFilterPatterns, imageURLs);
		if (imageURLs.length() >0) {
			postInfo.put("images", imageURLs); 
			for (int i=0;i<imageURLs.length();i++) { _seenImages.addURLToCrawl(imageURLs.getJSONObject(i).getString("href")); }
		}
						
		//look for hyperlinks
		JSONArray hyperLinks = HTMLUtilities.extractHyperlinks(p.select(this.getJobConfigurationFieldAsString("board.thread.findContent")).select("a"));
		filterMatchingHREFs(_urlFilterPatterns, hyperLinks);
		if (hyperLinks.length() >0 ) { 
			postInfo.put("hyperlinks", hyperLinks); 
			for (int i=0;i<hyperLinks.length();i++) { _seenURLs.addURLToCrawl(hyperLinks.getJSONObject(i).getString("href")); }
		}
			
		return postInfo;
	}
	
	private static String replaceAlternativeWhitespaceCharacters(String s) {
		s = s.replace('\u00A0',' ');
		s = s.replace('\u2007',' ');
		s = s.replace('\u202F',' ');
		return s;
	}
	
	private JSONObject extractFieldValuePairsUnderDefinitionList(Elements  memberInfoList) {
		JSONObject attributes = new JSONObject();
		
		for (org.jsoup.nodes.Element item: memberInfoList) {
			
				String property = replaceAlternativeWhitespaceCharacters(item.text().toLowerCase()).replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_').trim();
				String value    = item.nextElementSibling().text().trim();
				if (property.length()>0 && value.length() >0 ) {
					attributes.put(property, value);
				}
		}
		
		return attributes;
	}	
	
	private JSONObject extractFieldValuePairsColonSeparateListItem(Elements listItems) {
		JSONObject attributes = new JSONObject();
		
		for (org.jsoup.nodes.Element item: listItems) {
			String text = item.text();
			if (text.contains(":")) {
				String property = text.substring(0,text.indexOf(":")).trim().toLowerCase().replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_');
				String value    = text.substring(text.indexOf(":")+1).trim();
				attributes.put(property, value);
			}
			else if (text.contains("™")) {
				String property = text.substring(0,text.indexOf("™")).trim().toLowerCase().replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_');
				String value    = text.substring(text.indexOf("™")+1).trim();
				attributes.put(property, value);
			}
		}	
		return attributes;
	}
	
	
	private Pattern extraFieldPattern = Pattern.compile(	"\\<b\\>(.*?):\\</b\\>(.*)");

	/**
	 * This doesn't get values that exist on the next line...  probably need to use the comments to get sections, but that 
	 * probably gets very site specific.
	 * 
	 * <!-- BEGIN TEMPLATE: showthread_extra_fields --> \n   <div>\n    <b>Service Type:<\/b> Servicer \n   <\/div> \n   <!-- END TEMPLATE: showthread_extra_fields --> \n
	 * 
	 * @param htmlText
	 * @return
	 */
	private JSONObject extractExtraFields(String htmlText) {
		JSONObject attributes = new JSONObject();
		
		String[] lines = htmlText.split("\n");
		for (String line: lines) {
			//System.out.println(line);
			Matcher matcher = extraFieldPattern.matcher(line);
			if (matcher.find()) {
				String name  = matcher.group(1).trim();
				String value = matcher.group(2).trim();
				if (name.length()>0 && value.length() >0 && value.startsWith("<") == false && value.endsWith(">") ==false) {
					attributes.put(name,value);			
				}
			}
		}
		
		return attributes;
	}
	
	private void filterMatchingHREFs(Pattern[] filterPatterns, JSONArray data)  {
		 for (int i=data.length()-1; i>=0; i--) {
			 JSONObject jo = data.getJSONObject(i);
			 String href = jo.getString("href");
			 
			 for (int p=0; p< filterPatterns.length; p++) {
				 Matcher m = filterPatterns[p].matcher(href);
				 if (m.find()) {
					 data.remove(i);
					 break;
				 }
			 }
		 }
		
	}
	
	
	private JSONArray findGoogleSearchResults(String searchText, int maxResults) {
		GoogleHandler gh = new GoogleHandler();
		JSONObject configuration = new JSONObject().put("google",new JSONObject().put("length", maxResults));
		java.util.List<SearchRecord> records = gh.generateSearchResults(this.getDomainInstanceName(),searchText, configuration, maxResults, new JSONObject());
		
		JSONArray googleResults = new JSONArray();
		for (SearchRecord sr: records) {
			JSONObject rec = new JSONObject().put("title", sr.getName())
					                         .put("url",   sr.getUrl())
					                         .put("description", sr.getDescription());
			googleResults.put(rec);
		}
		return googleResults;	
	}
	
	public JSONObject processMember(CloseableHttpClient  httpclient, String memberURL, Instant startTime, Instant lastJobStartTime) {
		JSONObject member = new JSONObject();
		member.put("user_url", memberURL);
		member.put("first_crawled_dt", startTime.toString());
		member.put("latest_crawled_dt", startTime.toString());
		
		
		Matcher ma = _extractUserIDFromURLPattern.matcher(memberURL);
		if (ma.find()) {
			String authorID = ma.group(1);
			member.put("user_id",authorID);
		}
		
		HttpGet testGet = new HttpGet(memberURL);
		try (CloseableHttpResponse response = this.executeHttpCall(httpclient,testGet)) {
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "UTF-8");

			org.jsoup.nodes.Document doc = Jsoup.parse(content, memberURL);
			
			String nameArea  = doc.select(this.getJobConfigurationFieldAsString("board.member.findUserNameText")).text();
			Pattern namePattern = Pattern.compile(this.getJobConfigurationFieldAsString("board.member.extractUserName")); 
			Matcher mName = namePattern.matcher(nameArea);
			if (mName.find()) {
				String name = mName.group(1).trim();
				member.put("user_name",name);
			}
			
			if (this.getJobConfigurationFieldAsString("board.member.userPropertiesType").equals("dlList")) {
				member.put("profile", extractFieldValuePairsUnderDefinitionList(doc.select(this.getJobConfigurationFieldAsString("board.member.findUserProperties")))); 
			}
			else if (this.getJobConfigurationFieldAsString("board.member.userPropertiesType").equals("list")) {
				member.put("profile", extractFieldValuePairsColonSeparateListItem(doc.select(this.getJobConfigurationFieldAsString("board.member.findUserProperties")))); 
			}
			
			String text = HTMLUtilities.extractText(doc, this.getJobConfigurationFieldAsString("board.member.userContentArea"));
			member.put("text", text);
			member.put("html", doc.select(this.getJobConfigurationFieldAsString("board.member.userContentArea")).html());
			
			JSONArray images = HTMLUtilities.extractImageURLs(doc.select(this.getJobConfigurationFieldAsString("board.member.userContentArea")).select("img"));
			filterMatchingHREFs(_imageURLFilterPatterns, images);
			if (images.length() >0) {
				member.put("images", images); 
				for (int i=0;i<images.length();i++) { _seenImages.addURLToCrawl(images.getJSONObject(i).getString("href")); }
			}
							
			//look for hyperlinks
			JSONArray hyperLinks = HTMLUtilities.extractHyperlinks(doc.select(this.getJobConfigurationFieldAsString("board.member.userContentArea")).select("a"));
			filterMatchingHREFs(_urlFilterPatterns, hyperLinks);
			if (hyperLinks.length() >0 ) { 
				member.put("hyperlinks", hyperLinks); 
				for (int i=0;i<hyperLinks.length();i++) { _seenURLs.addURLToCrawl(hyperLinks.getJSONObject(i).getString("href")); }
			}
			
			// see if we need to search Google on any of the profile fields
			JSONArray fieldsToSearch = this.getJobConfigurationFieldAsJSONArray("board.member.searchProfileFields");
			for (int i=0; i < fieldsToSearch.length(); i++) {
				String value = member.getJSONObject("profile").optString(fieldsToSearch.getString(i),null);
				if (value != null && value.trim().length()>0)
				member.put("google_"+fieldsToSearch.getString(i), this.findGoogleSearchResults(value, 20));
			}

			//normalize date fields
			JSONArray dateFields = this.getJobConfigurationFieldAsJSONArray("board.member.normalizeDateFields");
			for (int i=0;i< dateFields.length(); i++) {
				this.normalizeDateField(member.getJSONObject("profile"),dateFields.getString(i));
			}
			
			// convert profile to a text so we have something to store in that space
			/*
			StringBuilder textSB = new StringBuilder(member.getString("user_name")); textSB.append("\n\n");
			JSONObject profile = member.getJSONObject("profile");
			for (String key: profile.keySet()) {
				textSB.append(key); textSB.append(": "); textSB.append(profile.getString(key));textSB.append("\n");
			}
			member.put("text", textSB.toString());
			 */

		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "forum exception: " ,e);
			return member;
		} 
		
		return member;
	}	
	
	/**
	 * Creates an alternate ID to be used when storing and recalling thread objects 
	 * from ElasticSearch
	 * 
	 * @param hostName
	 * @param forumID
	 * @param threadID
	 * @return
	 */
	public String createThreadID(String hostName, String forumID, String threadID) {
		return hostName + "_" + forumID + "_" + threadID;
	}

	/**
	 * Creates an alternate ID to be used when storing and recalling post objects
	 * from ElasticSearch
	 * 
	 * @param hostName
	 * @param forumID
	 * @param threadID
	 * @param postID
	 * 
	 * @return
	 */
	public String createPostID(String hostName, String forumID, String threadID, String postID) {
		return hostName + "_" + forumID + "_" + threadID + "_" + postID;
	}

	/**
	 * 
	 * @param hostName
	 * @param userID
	 * @return
	 */
	public String createUserID(String hostName, String userID) {
		return hostName + ":" +userID;
	}
	
	
	public void process() {
		srcLogger.log(Level.INFO, "Job procesing starting: " + this.getJob().getName());

		try {
			boolean mergeMemberRecords = this.getJobConfigurationFieldAsBoolean("mergeMemberRecords",true);		
			boolean retrieveImages     = this.getJobConfigurationFieldAsBoolean("retrieveImages", false);
			boolean retrieveURLs       = this.getJobConfigurationFieldAsBoolean("retrieveURLs", false);
			boolean retrieveMemberInfo = this.getJobConfigurationFieldAsBoolean("retrieveMemberInfo", true);
						
			_delayMilliseconds = this.getJobConfigurationFieldAsInteger("requestDelay", 1000);
			this.setTimeZoneID( this.getJobConfigurationFieldAsString(("defaultTimeZone")));
					
			java.util.List<String> forumsToCrawl = JSONUtilities.toStringList(this.getJobConfigurationFieldAsJSONArray("forumsToCrawl"));
						
			Instant startTime = Instant.now();
			Instant lastJobStartTime = Instant.ofEpochMilli(this.getJobConfigurationFieldAsLong("lastCrawlEpochMillis",0));
			
			srcLogger.log(Level.INFO, "Job last processed time: " + lastJobStartTime.toString());
			
			CloseableHttpClient httpClient = this.authenticate();
			if (httpClient == null) {
				srcLogger.log(Level.INFO, "Job procesing errored: " + this.getJob().getName());
				JobHistoryStatus status = JobHistoryStatus.ERRORED;
				String message = "unable to authenticate";				
				this.getJobCollector().sourceHandlerCompleted(this, status,message);
				this.setJobHistoryStatus(status);
				return;
			}
			String hostName = "unknown";
			for (String forum: forumsToCrawl) {
				String forumURL = forum;
				
				String forumID = "unknown";
				Matcher m = _extractForumIDfromURLPattern.matcher(forumURL);
				if (m.find()) {
					forumID = m.group(1);
				}
				java.net.URI uriForum = new java.net.URI(forumURL);
				hostName= uriForum.getHost(); if (hostName.startsWith("www.")) { hostName = hostName.substring(4); }
				
				JSONArray threads = this.processForumPage(httpClient, forumURL, startTime, lastJobStartTime);
				this.markActivity();
				for (int i=0; i< threads.length();i++) {
					
					JSONObject threadInfo = threads.getJSONObject(i);
					String text = threadInfo.optString("threadTitle", "") +"\n" + threadInfo.optString("infoTitle", "");
									
					threadInfo.put("text", text);
					threadInfo.put("authorDocumentID", this.createUserID(hostName, threadInfo.getString("authorID")));
					
					String threadURL = threadInfo.getString("threadURL");
					Job job = this.getJob();
					String alternateThreadID = this.createThreadID(hostName, forumID, threadInfo.getString("threadID"));
					edu.ncsu.las.document.Document threadDocument = new edu.ncsu.las.document.Document(threadInfo, MimeType.FORUM_THREAD,"forum_thread",job.getConfiguration(), job.getSummary(),threadURL,hostName,this.getDomainInstanceName(),alternateThreadID, this.getJobHistory());
					this.getDocumentRouter().processPage(threadDocument,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
					
					JSONArray posts = this.processThreadPage(httpClient, threadInfo,startTime,lastJobStartTime,hostName);
					this.markActivity();
					
					for (int j=0; j < posts.length();j++) {
						JSONObject postInfo = posts.getJSONObject(j);
						postInfo.put("authorDocumentID", this.createUserID(hostName, threadInfo.getString("authorID")));
						postInfo.put("threadDocumentID", alternateThreadID);
						String alternatePostID = this.createPostID(hostName, forumID, threadInfo.getString("threadID"),postInfo.getString("postID"));
						edu.ncsu.las.document.Document postDocument = new edu.ncsu.las.document.Document(postInfo, MimeType.FORUM_POST,"forum_post",job.getConfiguration(), job.getSummary(),postInfo.getString("postURL"),hostName,this.getDomainInstanceName(),alternatePostID, this.getJobHistory());
						this.getDocumentRouter().processPage(postDocument,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
					}				
				}
			}
			
			if (retrieveMemberInfo) {	processAllSeenMembers(httpClient, startTime, lastJobStartTime,hostName, mergeMemberRecords); }
			else { srcLogger.log(Level.INFO, "Not procesing detailed membership info - config setting: "+this.getJob().getName());}

			if (retrieveImages) {	processImages(httpClient); }
			else { srcLogger.log(Level.INFO, "Not processing linked images - config setting: "+this.getJob().getName());}
			
			if (retrieveURLs) {    processURLs(httpClient);	}
			else { srcLogger.log(Level.INFO, "Not procesing linked - config setting: "+this.getJob().getName());}
			
			httpClient.close();
			
			//update the configuration with our latest run time
			long lastCrawlEpochMillisNewValue = startTime.toEpochMilli();
			JSONObject config = this.getJob().getConfiguration();
			config.put("lastCrawlEpochMillis", lastCrawlEpochMillisNewValue);
			String id = Configuration.getConfigurationProperty(this.getDomainInstanceName(),ConfigurationType.COLLECTOR_ID);
			this.getJob().updateConfiguration(config,id);
		}
		catch (Exception e1) {
			if (this.isManuallyStopped() == false) {
				srcLogger.log(Level.SEVERE, "Unable to process forum site: ",e1);
				this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.ERRORED, "Unable to process forum site: " + e1);
				this.setJobHistoryStatus(JobHistoryStatus.ERRORED);
				return;
			}
		}

		srcLogger.log(Level.INFO, "Job procesing complete: " + this.getJob().getPrimaryFieldValue());
		JobHistoryStatus status = JobHistoryStatus.COMPLETE;
		String message = "";
		if (this.isManuallyStopped()) {
			status=JobHistoryStatus.STOPPED;
			message = "Job stopped upon request.";
		}
		
		this.getJobCollector().sourceHandlerCompleted(this, status,message);
		this.setJobHistoryStatus(status);
		
		return;
	}
	
	//private static String CONFIG_FORUM_URL = "/viewforum.php?f=";           // helps to create the initial link to the forums.  (make more more sense to just have full URL in the config)
	

	public void processImages(CloseableHttpClient  httpclient) {
		srcLogger.log(Level.INFO, "Processing linked images: "+this.getJob().getName());
		this.processURLSet(_seenImages.getURLsToCrawl(), httpclient);
	}
	
	public void processURLs(CloseableHttpClient  httpclient) {
		srcLogger.log(Level.INFO, "Processing linked pages: "+this.getJob().getName());
		this.processURLSet(_seenURLs.getURLsToCrawl(), httpclient);
	}
	
	public void processURLSet(java.util.HashSet<String> urls, CloseableHttpClient  httpclient) {	
		for (String visitURL: urls) {
			try {
				srcLogger.log(Level.INFO, "Processing url: "+visitURL);
				CloseableHttpResponse response = executeHttpCall( httpclient, new HttpGet(visitURL));
				HttpContent hc = InternetUtilities.createHttpContent(visitURL, response);
				
				edu.ncsu.las.document.Document entityDocument = new edu.ncsu.las.document.Document(edu.ncsu.las.util.UUID.createTimeUUID(),hc,visitURL,this.getJob().getConfiguration(),this.getJob().getSummary(), this.getDomainInstanceName(), this.getJobHistory());
				this.getDocumentRouter().processPage(entityDocument, "");
				this.markActivity();
				response.close();
			}
			catch (Throwable t) {
				if (t instanceof java.io.FileNotFoundException) {
					srcLogger.log(Level.INFO, "Unable to adhoc URL not found: "+visitURL);
				}
				else {
					srcLogger.log(Level.WARNING, "Unable to parse adhoc URL: "+visitURL, t);
				}
			}
		}
	}	
	
	public void processAllSeenMembers(CloseableHttpClient  httpclient, Instant startTime, Instant lastJobStartTime, String hostName, boolean mergeMemberRecords) {
		srcLogger.log(Level.INFO, "Processing seen members for detailed record: "+this.getJob().getName());
		
		String memberURL = null;
		while ( (memberURL = _seenMembers.getNextURLToCrawl()) != null) {
			_seenMembers.markURLCrawled(memberURL);
			JSONObject member = this.processMember(httpclient, memberURL, startTime, lastJobStartTime);
			this.markActivity();
			
			Job job = this.getJob();
			String alternateUserID = this.createUserID(hostName, member.getString("user_id"));
			edu.ncsu.las.document.Document userDocument = new edu.ncsu.las.document.Document(member, MimeType.FORUM_USER,"forum_user",job.getConfiguration(), job.getSummary(),memberURL,hostName,this.getDomainInstanceName(),alternateUserID, this.getJobHistory());
			if (mergeMemberRecords) { userDocument.setDocumentMerger(new ForumUserMerger()); }
			this.getDocumentRouter().processPage(userDocument,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
			
			System.out.println("Processed member: "+memberURL);
		}
	}
				
	public void setTimeZoneID(String timeZoneID) {
		_zone = ZoneId.of(timeZoneID);
	}
	
	private int _delayMilliseconds = 1000;
	private long _lastHTTPCallTime  = 0;
	private SecureRandom _random = new SecureRandom();
	
	/**
	 * Wrapper around httpClient execute.  Tracks the last time a call was made and then enforces
	 * a random delay between 1/2 and 1 of the delay milliseconds setting.
	 * 
	 * Example: setting delayMilliseconds to 1000 will ensure there is at least 500 milliseconds between calls
	 *          with a maximum of 1000 milliseconds.  
	 *          
	 * The method does take into account other processing time through tracking the last time when an http call
	 * was made so it is possible that there may not be any sleeping necessary.         
	 * 
	 * @param httpclient
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private CloseableHttpResponse executeHttpCall(CloseableHttpClient  httpclient, HttpUriRequest request) throws ClientProtocolException, IOException {
		if (this.isManuallyStopped()) {
			return null;
		}
		
		int halvedDelay = _delayMilliseconds/2;
		long delayTime =  halvedDelay + _random.nextInt(halvedDelay);	
		long currentTime = System.currentTimeMillis();
		
		if (_lastHTTPCallTime + delayTime > currentTime) {
			long sleepTime = _lastHTTPCallTime + delayTime - currentTime;
			try {
				srcLogger.log(Level.FINEST, "Sleeping between calls: "+sleepTime);
				TimeUnit.MILLISECONDS.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		_lastHTTPCallTime = System.currentTimeMillis();
		srcLogger.log(Level.INFO, "Calling: "+request.getURI());
		CloseableHttpResponse response = httpclient.execute(request);
		
		return response;
	}
	
	
	public static void main(String args[]) throws IOException {	
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
		srcLogger.log(Level.INFO, "Forum test Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);	

		JobHistory jh = new JobHistory();
		Job job = new Job(); 
		job.setDomainInstanceName("crazy");
		job.setConfig(new JSONObject("{\r\n\t\"mergeMemberRecords\": \"true\",\r\n\t\"forumsToCrawl\": [\"http://www.atari-forum.com/viewforum.php?f=28\",\"http://www.atari-forum.com/viewforum.php?f=3\"],\r\n\t\"defaultTimeZone\": \"US/Pacific\",\r\n\t\"lastCrawlEpochMillis\": \"1488418007988\",\r\n\t\"userAgentString\": \"\",\r\n\t\"linkType\": \"idOnly\",\r\n\t\"retrieveURLs\": \"false\",\r\n\t\"board\": {\r\n\t\t\"forum\": {\r\n\t\t\t\"extractThreadViewCount\": \"([0-9]+) View\",\r\n\t\t\t\"findThreadReplyCount\": \"dd.posts\",\r\n\t\t\t\"findThreadViewText\": \"td.stats\",\r\n\t\t\t\"extractThreadReplyCount\": \"([0-9]+) Repl\",\r\n\t\t\t\"nextPageSelector\": \"a[rel=next]\",\r\n\t\t\t\"findThreadTitle\": \"a.topictitle\",\r\n\t\t\t\"findLastPoster\": \"dd.lastpost a[class^=username]\",\r\n\t\t\t\"findThreadViewCount\": \"dd.views\",\r\n\t\t\t\"findThreadURL\": \"a.topictitle\",\r\n\t\t\t\"threadSelector\": \"div.forumbg:not(.announcement) ul.topics>li\",\r\n\t\t\t\"findThreadAuthor\": \"dl dt div.list-inner div:contains(by)\",\r\n\t\t\t\"findThreadReplyText\": \"td.stats\",\r\n\t\t\t\"findThreadStartDate\": \"dl dt div.list-inner div:contains(by)\",\r\n\t\t\t\"findLastPosterDate\": \"dd.lastpost\"\r\n\t\t},\r\n\t\t\"filterLinks\": {\r\n\t\t\t\"image\": [\"images/ranks\"],\r\n\t\t\t\"url\": [\"ucp.php.i=zebra\", \"sr=posts\", \"mode=email\"]\r\n\t\t},\r\n\t\t\"regex\": {\r\n\t\t\t\"extractDateString\": \"[A-Z][a-z][a-z] [0-9][0-9], [0-9]{4} [0-9]{1,2}:[0-9]{2} [ap][m]\",\r\n\t\t\t\"extractThreadIDFromURL\": \"t=([0-9]+)\",\r\n\t\t\t\"extractUserIDFromURL\": \"u=([0-9]+)\"\r\n\t\t},\r\n\t\t\"datePattern\": {\r\n\t\t\t\"date\": \"MMM dd y\",\r\n\t\t\t\"dateTime\": \"MMM dd y h:mm a VV\"\r\n\t\t},\r\n\t\t\"member\": {\r\n\t\t\t\"normalizeDateFields\": [\"joined\", \"last_active\"],\r\n\t\t\t\"userPropertiesType\": \"dlList\",\r\n\t\t\t\"searchProfileFields\": [\"interests\"],\r\n\t\t\t\"findUserProperties\": \"dl dt\",\r\n\t\t\t\"findUserNameText\": \"h2.memberlist-title\",\r\n\t\t\t\"userContentArea\": \"div.panel\",\r\n\t\t\t\"extractUserName\": \"- (\\\\w*)\"\r\n\t\t},\r\n\t\t\"thread\": {\r\n\t\t\t\"findAuthor\": \"p.author a[class^=username]\",\r\n\t\t\t\"nextPageSelector\": \"a[rel=next]\",\r\n\t\t\t\"extractPostID\": \"p([0-9]+)\",\r\n\t\t\t\"findContent\": \"div.postbody div.content\",\r\n\t\t\t\"findDate\": \"p.author\",\r\n\t\t\t\"findID\": \"div.post\",\r\n\t\t\t\"findTitle\": \"div.postbody h3\",\r\n\t\t\t\"postSelector\": \"div.post\"\r\n\t\t}\r\n\t},\r\n\t\"retrieveImages\": \"false\",\r\n\t\"requestDelay\": \"2000\",\r\n\t\"retrieveMemberInfo\": \"true\",\r\n\t\"authenticationForm\": {\r\n\t\t\"additionalFormData\": [{\r\n\t\t\t\"fieldName\": \"login\",\r\n\t\t\t\"fieldValue\": \"login\"\r\n\t\t}],\r\n\t\t\"loginURL\": \"http://www.atari-forum.com/ucp.php?mode=login\",\r\n\t\t\"userFieldName\": \"username\",\r\n\t\t\"passwordFieldName\": \"password\",\r\n\t\t\"username\": \"USERNAME\",\r\n\t    \"password\": \"PASSWORD\"\r\n\t}\t  \r\n}"));

		//Config for bitcoingarden
		job.setConfig(new JSONObject("{\r\n\t\"mergeMemberRecords\": \"true\",\r\n\t\"forumsToCrawl\": [\r\n\t\t\"https://bitcoingarden.org/forum/index.php?board=11.0\"\r\n\t],\r\n\t\"defaultTimeZone\": \"US/Pacific\",\r\n\t\"lastCrawlEpochMillis\": \"1488418007988\",\r\n\t\"userAgentString\": \"\",\r\n\t\"linkType\": \"idOnly\",\r\n\t\"retrieveURLs\": \"true\",\r\n\t\"board\": {\r\n\t\t\"forum\": {\r\n\t\t\t\"extractThreadViewCount\": \"([0-9]+) View\",\r\n\t\t\t\"findThreadReplyCount\": \"dd.posts\",\r\n\t\t\t\"findThreadViewText\": \"td.stats\",\r\n\t\t\t\"pagingMethod\": \"number\",\r\n\t\t\t\"extractThreadReplyCount\": \"([0-9]+) Repl\",\r\n\t\t\t\"nextPageSelector\": \"a[rel=next]\",\r\n\t\t\t\"paging\": {\r\n\t\t\t\t\"startNumber\": \"0\",\r\n\t\t\t\t\"incrementValue\": \"20\",\r\n\t\t\t\t\"endSeparator\": \".\"\r\n\t\t\t},\r\n\t\t\t\"findThreadTitle\": \"a.topictitle\",\r\n\t\t\t\"findLastPoster\": \"dd.lastpost a[class^=username]\",\r\n\t\t\t\"findThreadViewCount\": \"dd.views\",\r\n\t\t\t\"findThreadURL\": \"a.topictitle\",\r\n\t\t\t\"threadSelector\": \"div.topic_table>table>tbody>tr\",\r\n\t\t\t\"findThreadAuthor\": \"dl dt div.list-inner div:contains(by)\",\r\n\t\t\t\"findThreadReplyText\": \"td.stats\",\r\n\t\t\t\"findThreadStartDate\": \"dl dt div.list-inner div:contains(by)\",\r\n\t\t\t\"findLastPosterDate\": \"dd.lastpost\"\r\n\t\t},\r\n\t\t\"filterLinks\": {\r\n\t\t\t\"image\": [\r\n\t\t\t\t\"images/ranks\"\r\n\t\t\t],\r\n\t\t\t\"url\": [\r\n\t\t\t\t\"ucp.php.i=zebra\",\r\n\t\t\t\t\"sr=posts\",\r\n\t\t\t\t\"mode=email\"\r\n\t\t\t]\r\n\t\t},\r\n\t\t\"regex\": {\r\n\t\t\t\"extractDateString\": \"[A-Z][a-z][a-z] [0-9][0-9], [0-9]{4} [0-9]{1,2}:[0-9]{2} [ap][m]\",\r\n\t\t\t\"extractThreadIDFromURL\": \"t=([0-9]+)\",\r\n\t\t\t\"extractUserIDFromURL\": \"u=([0-9]+)\"\r\n\t\t},\r\n\t\t\"datePattern\": {\r\n\t\t\t\"date\": \"MMM dd y\",\r\n\t\t\t\"dateTime\": \"MMM dd y h:mm a VV\"\r\n\t\t},\r\n\t\t\"member\": {\r\n\t\t\t\"normalizeDateFields\": [\r\n\t\t\t\t\"joined\",\r\n\t\t\t\t\"last_active\"\r\n\t\t\t],\r\n\t\t\t\"userPropertiesType\": \"dlList\",\r\n\t\t\t\"searchProfileFields\": [\r\n\t\t\t\t\"interests\"\r\n\t\t\t],\r\n\t\t\t\"findUserProperties\": \"dl dt\",\r\n\t\t\t\"findUserNameText\": \"h2.memberlist-title\",\r\n\t\t\t\"userContentArea\": \"div.panel\",\r\n\t\t\t\"extractUserName\": \"- (\\\\w*)\"\r\n\t\t},\r\n\t\t\"thread\": {\r\n\t\t\t\"findAuthor\": \"p.author a[class^=username]\",\r\n\t\t\t\"pagingMethod\": \"relativeLink\",\r\n\t\t\t\"nextPageSelector\": \"a[rel=next]\",\r\n\t\t\t\"paging\": {\r\n\t\t\t\t\"startNumber\": \"1\",\r\n\t\t\t\t\"incrementValue\": \"1\",\r\n\t\t\t\t\"endSeparator\": \"page=\"\r\n\t\t\t},\r\n\t\t\t\"extractPostID\": \"p([0-9]+)\",\r\n\t\t\t\"findContent\": \"div.postbody div.content\",\r\n\t\t\t\"findDate\": \"p.author\",\r\n\t\t\t\"findID\": \"div.post\",\r\n\t\t\t\"findTitle\": \"div.postbody h3\",\r\n\t\t\t\"postSelector\": \"div.post\"\r\n\t\t}\r\n\t},\r\n\t\"retrieveImages\": \"true\",\r\n\t\"authenticationForm\": {\r\n\t\t\"additionalFormData\": [\r\n\t\t\t{\r\n\t\t\t\t\"fieldName\": \"cookielength\",\r\n\t\t\t\t\"fieldValue\": \"1440\"\r\n\t\t\t}\r\n\t\t],\r\n\t\t\"password\": \"\",\r\n\t\t\"loginURL\": \"https://bitcoingarden.org/forum/index.php?action=login2\",\r\n\t\t\"userFieldName\": \"user\",\r\n\t\t\"passwordFieldName\": \"passwrd\",\r\n\t\t\"username\": \"\"\r\n\t},\r\n\t\"requestDelay\": \"2000\",\r\n\t\"retrieveMemberInfo\": \"true\"\r\n}"));
		
		
		
		ForumHandler ddgh = new ForumHandler();
		ddgh.initialize( jh, job);
		ddgh.setTimeZoneID("US/Pacific");
		
		String sample = ddgh.getSampleConfiguration();
		System.out.println(sample);
				
		Instant startTime = Instant.now();
		Instant lastJobStartTime = Instant.now().minus(60, ChronoUnit.DAYS);
		System.out.println(lastJobStartTime);
		
		CloseableHttpClient httpClient = ddgh.authenticate();

		if (httpClient != null) {
			//JSONArray threads = ddgh.processForumPage(httpClient, "http://www.atari-forum.com/viewforum.php?f=3", startTime, lastJobStartTime);
			JSONArray threads = ddgh.processForumPage(httpClient, "https://bitcoingarden.org/forum/index.php?board=11.0", startTime, lastJobStartTime);
			System.out.println(threads.toString(4));

		
			for (int i=0; i< threads.length();i++) {
				JSONObject threadInfo = threads.getJSONObject(i);
				System.out.println(threadInfo.toString(4));
				JSONArray posts = ddgh.processThreadPage(httpClient, threadInfo,startTime,lastJobStartTime,"bitcoingardern.org");
				System.out.println("postCount: "+posts.length());
				if (posts.length() > 0) {
					System.out.println(posts.getJSONObject(0).toString(4));
					break;
				}
				
			}
			
			JSONObject member = ddgh.processMember(httpClient, "http://www.atari-forum.com/memberlist.php?mode=viewprofile&u=16868", startTime, lastJobStartTime);
			System.out.println(member.toString(4));

	
			httpClient.close();
		}
	}

	
	/**
	 * Returns true if this source Handler can provide test results for a configuration.
	 * False otherwise.
	 * 
	 * @return
	 */
	public boolean isConfigurationTestable() {
		return true;
	}		
	
	/**
	 * Tests the passed in configuration and returns the results as a JSON object
	 * 
	 * @param configution
	 * @return
	 */
	public JSONObject testConfiguration(String jobName, String domainInstanceName, JSONObject configuration, String url) {
		JSONObject result = new JSONObject();
		
		Job job = new Job();
		job.setDomainInstanceName(domainInstanceName);
		job.setConfig(configuration);
		job.setPrimaryFieldValue(url);
		JobHistory jobRecord = new JobHistory();
		jobRecord.setJobID( java.util.UUID.randomUUID());
		this.initialize(jobRecord, job);
		
		srcLogger.log(Level.INFO, "Job procesing starting: " + this.getJob().getName());

		try {
			_delayMilliseconds = this.getJobConfigurationFieldAsInteger("requestDelay", 1000);
			this.setTimeZoneID( this.getJobConfigurationFieldAsString(("defaultTimeZone")));
					
			java.util.List<String> forumsToCrawl = JSONUtilities.toStringList(this.getJobConfigurationFieldAsJSONArray("forumsToCrawl"));
			if (forumsToCrawl.size() ==0) {
				result.put("error", "No forums specified for crawl");
				
				return result;
			}
						
			Instant startTime = Instant.now();
			Instant lastJobStartTime = Instant.ofEpochMilli(this.getJobConfigurationFieldAsLong("lastCrawlEpochMillis",0));
			
			srcLogger.log(Level.INFO, "Job last processed time: " + lastJobStartTime.toString());
			
			CloseableHttpClient httpClient = this.authenticate();
			if (httpClient == null) {
				srcLogger.log(Level.INFO, "Job processing errored: " + this.getJob().getName());
				result.put("error", "Unable to authenticate");
				
				return result;
			}
			
			String hostName = "unknown";
			
			String forumURL = forumsToCrawl.get(0);

				
			String forumID = "unknown";
			Matcher m = _extractForumIDfromURLPattern.matcher(forumURL);
			if (m.find()) {
				forumID = m.group(1);
			}
			else {
				result.put("error", "Unable to extract forum ID");
				return result;
			}
			
			java.net.URI uriForum = new java.net.URI(forumURL);
			hostName= uriForum.getHost(); if (hostName.startsWith("www.")) { hostName = hostName.substring(4); }
				
			JSONArray threads = this.processForumPage(httpClient, forumURL, startTime, lastJobStartTime);
			
			if (threads.length() == 0) {
				result.put("error", "No threads found");
				return result;
			}
			
			JSONObject threadInfo = threads.getJSONObject(0);  // use the first thread as the example to return
			
			String text = threadInfo.optString("threadTitle", "") +"\n" + threadInfo.optString("infoTitle", "");
									
			threadInfo.put("text", text);
			threadInfo.put("authorDocumentID", this.createUserID(hostName, threadInfo.getString("authorID")));
					
			String alternateThreadID = this.createThreadID(hostName, forumID, threadInfo.getString("threadID"));
			result.put("threadID",alternateThreadID);
			result.put("thread", threadInfo);
			
					
			JSONArray posts = this.processThreadPage(httpClient, threadInfo,startTime,lastJobStartTime,hostName);
			if (posts.length() == 0) {
				result.put("error", "No posts found");
				return result;
			}
			
			JSONObject postInfo = posts.getJSONObject(0);
			postInfo.put("threadDocumentID", alternateThreadID);
			String alternatePostID = this.createPostID(hostName, forumID, threadInfo.getString("threadID"),postInfo.getString("postID"));
			
			result.put("postID", alternatePostID);
			result.put("post", postInfo);
			
			String memberURL = _seenMembers.getNextURLToCrawl();
			if (memberURL != null) {
				JSONObject member = processMember(httpClient, memberURL, startTime, lastJobStartTime);
				result.put("userID", this.createUserID(hostName, member.getString("user_id")));
				result.put("user", member);
			}
			else {
				result.put("error", "no users found during crawl");
			}
						
		}
		catch (Exception e1) {
			srcLogger.log(Level.SEVERE, "Unable to process forum site: ",e1);

			result.put("exception", e1);
			return result;
		}

		return result;

	}
	
}
