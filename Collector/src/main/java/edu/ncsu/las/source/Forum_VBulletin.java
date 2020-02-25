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
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;
import edu.ncsu.las.util.json.JSONUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * VBulletin provides an interface to web search at ...
 * 
 * 
 *   
 * TODO: as we add more bulletin-board type sites, need to abstract to a parent class
 */
public class Forum_VBulletin extends AbstractHandler implements SourceHandlerInterface {
	static final Logger srcLogger =Logger.getLogger(Forum_VBulletin.class.getName());
	
	public static String LOGIN_ERROR_MESSAGE = "invalid username or password";
	
	private ZoneId _zone;
	
	private URLTracker _seenMembers = new URLTracker();
	private URLTracker _seenURLs    = new URLTracker();
	private URLTracker _seenImages  = new URLTracker();
	

	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{}");
	private static final String SOURCE_HANDLER_NAME = "vbulletin";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "VBulletin Forum Handler";

	public static final String LINK_TYPE_NAMED = "named";
	public static final String LINK_TYPE_WITH_ID = "withID";
	public static final String LINK_TYPE_ID_ONLY = "idOnly";
	
	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			put("retrieveImages", new SourceParameter("retrieveImages", "Should images be retrieved when crawling this forum site? defaults to false", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("retrieveURLs",   new SourceParameter("retrieveURLs",   "Should URLs link from posts be retrieved when crawling this forum site? defaults to false", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("retrieveMemberInfo", new SourceParameter("retrieveMemberInfo", "Should detailed member information be retrieved? defaults to true", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("searchMemberEmail",  new SourceParameter("searchMemberEmail",  "If a member has an email address, should we search google for that email address? defaults to false", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("searchMemberPhone",  new SourceParameter("searchMemberPhone",  "If a member has a phone number, should we search google for that phone number? defaults to false", false,"true",false,SourceParameterType.BOOLEAN,false,true));
			put("mergeMemberRecords", new SourceParameter("mergeMemberRecords", "Should we merge a member record with the result of a prior run?  Default is true.  If set to false, then the user record in ElasticSearch will be replaced with the latest version.", false,"true",false,SourceParameterType.BOOLEAN,false,true));			
			put("defaultTimeZone",  new SourceParameter("defaultTimeZone", "What is the default time zone ID for this site? Possible values: http://www.javadb.com/list-possible-timezones-or-zoneids-in-java/", true,"US/Pacific",false,SourceParameterType.STRING,false,true));
			put("lastCrawlEpochMillis", new SourceParameter("lastCrawlEpochMillis", "When was this job last executed?  Defaults to Unix Epoch and is then updated on every run", false,"1488418007988",false,SourceParameterType.LONG,false,true));

			put("forumsToCrawl",    new SourceParameter("forumsToCrawl", "Array of forum IDs to be crawled",true,"",true,SourceParameterType.STRING,false,true));
			
			put("linkType",    new SourceParameter("linkType", "Structure of the links. possible values 'idOnly', 'named', 'withID'.  'idOnly' will have the format of site.com/forumdisplay.php?f=x   'named' will have formats of site.com/forumName.  'withID' will have formats of site.com/forumdisplay?id-forumName",true,"",false,SourceParameterType.STRING,false,true));
			
			put("username",    new SourceParameter("username", "Username to authenticate to the site",true,"",false,SourceParameterType.STRING,true,true));
			put("password",    new SourceParameter("password", "Password to authenticate to the site",true,"",false,SourceParameterType.STRING,true,true));
			put("userAgentString",    new SourceParameter("userAgentString", "What UserAgent string should be sent by the crawler.  Uses webcrawler.userAgentString for the domain if not set.",false,"",false,SourceParameterType.STRING,false,false));
			put("requestDelay",    new SourceParameter("requestDelay", "Number of milliseconds to pause in between requests.  Will be a random value between .5 and 1 of this value.  Defaults to 1000",false,"1000",false,SourceParameterType.INT,false,true));

	}};
	
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
		return "Provides an interface to systematically crawl a vBulletin-board based site";
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
		if (linkType != null && linkType.equals(LINK_TYPE_NAMED) == false && linkType.equals(LINK_TYPE_WITH_ID) == false && linkType.equals(LINK_TYPE_ID_ONLY) == false) {
			errors.add("linkType must be \\\"idOnly\\\", \\\"named\\\", or \\\"withID\\\".");
		}
		
		return errors;
	}		
	
	/**
	 * Authenticates the user to the site
	 * @param userAgentString 
	 * 
	 * @return
	 */
	public CloseableHttpClient authenticate(String loginURL, String userName, String password, String userAgentString) {
				
		String md5Password = org.apache.commons.codec.digest.DigestUtils.md5Hex(password);
		CloseableHttpClient  httpclient;
		
		//formParameters
		ArrayList<NameValuePair> loginPostParameters;
		loginPostParameters = new ArrayList<NameValuePair>();
		loginPostParameters.add(new BasicNameValuePair("vb_login_username", userName));
		loginPostParameters.add(new BasicNameValuePair("vb_login_password", password));
		loginPostParameters.add(new BasicNameValuePair("cookieuser", "1"));
		loginPostParameters.add(new BasicNameValuePair("s", ""));
		loginPostParameters.add(new BasicNameValuePair("securitytoken", "guest"));
		loginPostParameters.add(new BasicNameValuePair("do", "login"));
		loginPostParameters.add(new BasicNameValuePair("vb_login_md5password", md5Password));
		loginPostParameters.add(new BasicNameValuePair("vb_login_md5password_utf", md5Password));
		
		if (userAgentString == null || userAgentString.equals("")) {
			userAgentString = SourceHandlerInterface.getNextUserAgent(this.getDomainInstanceName());
		}
		try {
			httpclient = HttpClients.custom().setUserAgent(userAgentString).build();
			HttpPost loginPost = new HttpPost(loginURL);
			loginPost.setEntity(new UrlEncodedFormEntity(loginPostParameters));

			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, loginPost)) {
				int code = response.getStatusLine().getStatusCode();
				if (code != HttpStatus.SC_OK) {
					srcLogger.log(Level.SEVERE, "vBulletin HTTP Response code: " + code);
					srcLogger.log(Level.SEVERE,  "   status line: " + response.getStatusLine());
					httpclient.close();
					return null;
				}
				
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");
				
				if (content.indexOf(LOGIN_ERROR_MESSAGE) > 0) {
					srcLogger.log(Level.SEVERE, "vBulletin exception: login error message detected in response.");
					return null;
				}
								
			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "vBulletin exception: " + e.toString());
				return null;
			} 			


		}
		catch (Exception ioe) {
			srcLogger.log(Level.SEVERE, "httpclient exception: " + ioe.toString());
			return null;			
		}
		
		return httpclient;
	}
	
	
	private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM-dd-yyyy");
	private static DateTimeFormatter vBulletinFormat = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a VV");

	/**
	 * Converts
	 * @param date
	 * @param gmtOffset
	 * @return
	 */
	public static Instant toISOInstantDate(String date, ZoneId zoneID) {
		
		//System.out.println(getISODate("Today, 12:35 PM","US/Pacific"));
		
		if (date.contains("Today")) {
			ZonedDateTime zdt = Instant.now().atZone(zoneID);
			String today = dateFormat.format(zdt);
			date = date.replace("Today", today);
		}
		else if (date.contains("Yesterday")) {
			ZonedDateTime zdt = Instant.now().atZone(zoneID).minusDays(1);
			String yesterday = dateFormat.format(zdt);
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
		
		date = date +" " + zoneID.toString();
		date = date.replaceAll(",", "");
		date = date.replace('\u00A0',' ');
		date = date.replace('\u2007',' ');
		date = date.replace('\u202F',' ');
		
		ZonedDateTime zdt = ZonedDateTime.parse(date, vBulletinFormat);
		return zdt.toInstant();
	}
	
	public void normalizeDateField(JSONObject jo, String field) {
		if (jo.has(field)) {
			try {
				String value = jo.getString(field);
				String newValue = toISOInstantDate(value, _zone).toString();
				jo.put(field, newValue);
			}
			catch (Throwable t) {
				srcLogger.log(Level.WARNING, "Unable to normalize date field");
			}
		}
	}
	
	
	public JSONObject processThreadInfoListBased(Element t, Instant startTime, Instant lastJobStartTime, String currentPageURL) {
		if (t.hasClass("moved")) {	return new JSONObject("{ \"continue\": true} "); }

		JSONObject threadInfo = new JSONObject();
		threadInfo.put("crawled_dt", startTime.toString());
		threadInfo.put("infoTitle", t.select("div.threadinfo").first().attr("title"));
		threadInfo.put("threadTitle", t.select("h3.threadtitle").text());
		String threadURL = t.select("a.title").first().attr("abs:href");
		threadInfo.put("threadURL", threadURL);
		
		if (_linkType.equals(LINK_TYPE_WITH_ID)) {
			String threadID = threadURL.substring(threadURL.indexOf("?")+1);
			try { threadID = threadID.substring(0, threadID.indexOf("-") );}
			catch (java.lang.StringIndexOutOfBoundsException se) {
				srcLogger.log(Level.WARNING, "Unable to extract thread ID: "+threadURL+", using "+threadID);
				srcLogger.log(Level.WARNING, "current page: "+currentPageURL);
			}
			threadInfo.put("threadID",threadID);
		}
		else {
			String threadID = threadURL.substring(threadURL.lastIndexOf('-')+1);
			try { threadID = threadID.substring(0, threadID.indexOf(".") );}
			catch (java.lang.StringIndexOutOfBoundsException se) {
				srcLogger.log(Level.WARNING, "Unable to extract thread ID: "+threadURL+", using "+threadID);
				srcLogger.log(Level.WARNING, "current page: "+currentPageURL);
			}
			threadInfo.put("threadID",threadID);
			
		}
		threadInfo.put("author", t.select("div.author a").text().trim());
		String authorURL = t.select("div.author a").first().attr("abs:href");
		_seenMembers.addURLToCrawl(authorURL);
		threadInfo.put("authorURL", authorURL);
		String authorID = authorURL.substring(authorURL.indexOf("?")+1, authorURL.indexOf("-") <0 ? authorURL.length() :  authorURL.indexOf("-") );
		if (authorID.startsWith("u=")) { authorID = authorID.substring(2); }
		threadInfo.put("authorID", authorID);
		String labelText = t.select("span.label").text();
		threadInfo.put("label", labelText);
		labelText = labelText.substring(labelText.lastIndexOf(",")+2);
		threadInfo.put("labelDate", Forum_VBulletin.toISOInstantDate(labelText,_zone).toString());

		String lastActivityDate = t.select("dl.threadlastpost>dd:eq(2)").text();
		try {
			Instant isoActivityDate = Forum_VBulletin.toISOInstantDate(lastActivityDate, _zone);
			if (isoActivityDate.isBefore(lastJobStartTime)) {
				srcLogger.log(Level.FINE, "Found old thread, stop looking for threads");
				return new JSONObject("{ \"aged\": true} ");
			}
		}
		catch (Throwable x) {
			srcLogger.log(Level.WARNING, "Unable to extract last activity date: "+lastActivityDate);
		}
		
		for (org.jsoup.nodes.Element e: t.select("ul.threadstats > li")) {
			String text = e.text();
			if (text.startsWith("Replies")) {
				threadInfo.put("replyCount", Integer.parseInt(text.substring(text.indexOf(":")+1).trim().replace(",", "")));
			}
			if (text.startsWith("Views")) {
				threadInfo.put("viewCount", Integer.parseInt(text.substring(text.indexOf(":")+1).trim().replace(",", "")));
				//threadInfo.put("viewCount", Integer.parseInt(text.substring(text.indexOf(":"+1))));
			}
			if (text.startsWith("Rating")) {
				threadInfo.put("rating", text.substring(6));
			}
		}
		return threadInfo;
	}
	
	public JSONObject processThreadInfoTableBased(Element t, Instant startTime, Instant lastJobStartTime, String currentPageURL) {
		if (t.hasClass("moved")) {	return new JSONObject("{ \"continue\": true} "); }
		
		// Check activity first so we don't do unnecessary work
		String lastActivityDate = t.select("td:eq(3)").text();
		try {
			lastActivityDate = lastActivityDate.substring(0,lastActivityDate.indexOf("by")).trim();
			Instant isoActivityDate = Forum_VBulletin.toISOInstantDate(lastActivityDate, _zone);
			if (isoActivityDate.isBefore(lastJobStartTime)) {
				srcLogger.log(Level.FINE, "Found old thread, stop looking for threads");
				
				if (t.select("td[id*=threadtitle]").text().contains("Sticky:")) {
					return new JSONObject("{ \"continue\": true} ");
				}
				else {
					return new JSONObject("{ \"aged\": true}");
				}
			}
		}
		catch (Throwable x) {
			srcLogger.log(Level.WARNING, "Unable to extract last activity date: "+lastActivityDate);
			return new JSONObject("{ \"continue\": true} ");
		}
		JSONObject threadInfo = new JSONObject();
				
		threadInfo.put("crawled_dt", startTime.toString());
		threadInfo.put("infoTitle", t.select("td[id*=threadtitle]").first().attr("title"));
		threadInfo.put("threadTitle", t.select("td[id*=threadtitle] a[id^=thread_title]").text());
        
		String threadURL = t.select("td[id*=threadtitle] a[id^=thread_title]").first().attr("abs:href");
		threadInfo.put("threadURL", threadURL);
		
		if (_linkType.equals(LINK_TYPE_WITH_ID)) {
			String threadID = threadURL.substring(threadURL.indexOf("?")+1);
			try { threadID = threadID.substring(0, threadID.indexOf("-") );}
			catch (java.lang.StringIndexOutOfBoundsException se) {
				srcLogger.log(Level.WARNING, "Unable to extract thread ID: "+threadURL+", using "+threadID);
				srcLogger.log(Level.WARNING, "current page: "+currentPageURL);
			}
			threadInfo.put("threadID",threadID);
		}
		else if (_linkType.equalsIgnoreCase(LINK_TYPE_ID_ONLY)) {
			String threadID = threadURL.substring(threadURL.indexOf("?t=")+3);
			threadInfo.put("threadID",threadID);
		}
		else {
			String threadID = threadURL.substring(threadURL.lastIndexOf('-')+1);
			try { threadID = threadID.substring(0, threadID.indexOf(".") );}
			catch (java.lang.StringIndexOutOfBoundsException se) {
				srcLogger.log(Level.WARNING, "Unable to extract thread ID: "+threadURL+", using "+threadID);
				srcLogger.log(Level.WARNING, "current page: "+currentPageURL);
			}
			threadInfo.put("threadID",threadID);
			
		}
		
		Element threadAuthorElement = t.select("td[id*=threadtitle] div.smallfont span").first(); 
		
		threadInfo.put("author", threadAuthorElement.text().trim());
		String onClickText = threadAuthorElement.attr("onclick");
		int firstIndex = onClickText.indexOf('\'') +1;
		String relativeURL = onClickText.substring(firstIndex,onClickText.indexOf('\'',firstIndex+1));
		
		
		String authorURL = InternetUtilities.absUrl(relativeURL,threadAuthorElement.baseUri());
		String authorID  = authorURL.substring(authorURL.indexOf("?u=")+3);
		
		_seenMembers.addURLToCrawl(authorURL);
		threadInfo.put("authorURL", authorURL);
		threadInfo.put("authorID", authorID);
		
		/*
		String labelText = t.select("span.label").text();
		threadInfo.put("label", labelText);
		labelText = labelText.substring(labelText.lastIndexOf(",")+2);
		threadInfo.put("labelDate", Forum_VBulletin.toISOInstantDate(labelText,_zone).toString());
        */
		
		String replyCount = t.select("td:eq(4)").text();
		String viewCount = t.select("td:eq(5)").text();

		threadInfo.put("replyCount", getIntFromString(replyCount));
		threadInfo.put("viewCount", getIntFromString(viewCount));
		
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
	public JSONArray processForum(CloseableHttpClient  httpclient, String url, Instant startTime, Instant lastJobStartTime) {
		JSONArray results = new JSONArray();
		
		String getURL = url;
		FORUM_LOOP:
		while (true) {
		
			HttpGet httpGet = new HttpGet(getURL);
			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, httpGet)) {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, getURL);
				
				//process threads
				boolean isTableBased = false;
				Elements threads = doc.select("li.threadbit");
				if (threads.size() == 0) {
					isTableBased = true;
					threads = doc.select("table#threadslist tbody[id^=threadbit] tr");
				}
				for (org.jsoup.nodes.Element t: threads) {
					JSONObject threadInfo;
					if (isTableBased) {
						threadInfo = processThreadInfoTableBased(t, startTime,lastJobStartTime,getURL);;
					}
					else {
						threadInfo = processThreadInfoListBased(t, startTime,lastJobStartTime,getURL);
					}
					
					if (threadInfo.has("continue")) { continue; }
					if (threadInfo.has("aged"))  { break FORUM_LOOP; }

					
					results.put(threadInfo);
				}
				
				//process next pages...
				Elements items = doc.select("a[rel=next]");
				if (items.size() > 0) {
					org.jsoup.nodes.Element e = items.get(0);
					getURL = e.attr("abs:href"); 
				}
				else {
					break;
				}
			} 
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "vBulletin exception: ",e);
				return results;
			} 			
		}
		return results;
	}
	
	/**
	 * 
	 * @param httpclient
	 * @param threadInfo
	 * @param startTime this is when the current job has started.  no threads after this time should be processed
	 * @param lastJobStartTime this is when the last job started.  No threads before this time should be processed	 * @return
	 */
	public JSONArray processThread(CloseableHttpClient  httpclient, JSONObject threadInfo, Instant startTime, Instant lastJobStartTime ) {
		JSONArray results = new JSONArray();
		
		String getURL =  this.getThreadHybridURL(threadInfo.getString("threadURL"));
		TreeResult postTree = null;
		
		while (true) {
			//System.out.println(getURL);
			
			HttpGet testGet = new HttpGet(getURL);
			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, testGet)) {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, getURL);
				
				if (postTree == null && getURL.contains("mode=hybrid")) {
					Elements postTreeScriptElements  = doc.select("div#posttree > script");
					if (postTreeScriptElements.size() > 0) {
						String scriptText =postTreeScriptElements.first().html();
						postTree = this.createTreeFromScriptText(scriptText);
					
						if (_linkType.equals(LINK_TYPE_WITH_ID)) {
							getURL = threadInfo.getString("threadURL")+"&mode=linear";
						}
						else {
							getURL = threadInfo.getString("threadURL")+"?mode=linear";
						}
						continue;
					}
					
					//System.out.println(postTree.root.toString(2));
				}
				
				boolean isTableBased = false;
				Elements posts = doc.select("ol#posts > li");
				if (posts.size() == 0) {
					isTableBased = true;
					posts = doc.select("table[id^=post]");
				}
				
				
				for (org.jsoup.nodes.Element p: posts) {
					JSONObject postInfo = null;
					if (isTableBased) {
						postInfo = processPostTableBased(p, startTime, lastJobStartTime, getURL, postTree, threadInfo.getString("threadID"));
					}
					else {
						postInfo = processPostListBased(p, startTime, lastJobStartTime, getURL, postTree, threadInfo.getString("threadID"));
					}
					
					if (postInfo.has("continue")) { continue; }

					results.put(postInfo);
				}
				
				//process next pages...
				Elements items = doc.select("a[rel=next]");
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
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "vBulletin exception: ", e);
				return results;
			} 			
			
			
		}
		return results;
	}
	
	public JSONObject processPostListBased(Element p, Instant startTime, Instant lastJobStartTime, String currentPageURL, TreeResult postTree, String threadID) {
		JSONObject postInfo = new JSONObject();
		postInfo.put("crawled_dt", startTime.toString());

		String date = p.select("span.date").text();
		try {
			Instant isoActivityDate = Forum_VBulletin.toISOInstantDate(date, _zone);
			//System.out.println(isoActivityDate.toString());
			if (isoActivityDate.isBefore(lastJobStartTime)) {
				srcLogger.log(Level.FINE, "skipping old post");
				return new JSONObject("{ \"continue\": true} ");
			}
			
			postInfo.put("postDate", isoActivityDate.toString());
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "vBulletin unable to convert date: "+date);
		}
		
		String postID = p.select("li").first().attr("id");
		if (postID.length()>0 && postID.startsWith("post_")) { postID = postID.substring(5); }  // ignore the leading "post_" in the ID
		postInfo.put("postID", postID);
		
		// link to the parent record if it exists
		if (postTree != null && postTree.nodes.containsKey(postID)) {
			PostNode pn = postTree.nodes.get(postID);
			if (pn._parentNode != null) {
				postInfo.put("replyToPostID", pn._parentNode._postID);
			}
		}
		
		postInfo.put("threadID", threadID);
		postInfo.put("postTitle", p.select("h2.title").text());
		postInfo.put("text", p.select("div.content").text());
		postInfo.put("html", p.select("div.content").html());
		
		postInfo.put("author", p.select("a.username").text().trim());
		String authorURL = p.select("a.username").first().attr("abs:href");
		_seenMembers.addURLToCrawl(authorURL);
		postInfo.put("authorURL", authorURL);
		String authorID = authorURL.substring(authorURL.indexOf("?")+1, authorURL.indexOf("-") <0 ? authorURL.length() :  authorURL.indexOf("-") );
		if (authorID.startsWith("u=")) { authorID = authorID.substring(2); }
		postInfo.put("authorID", authorID);
		
		JSONObject attributes = this.extractExtraFields( p.select("div.content").html());
		if (attributes.length() >0) {
			postInfo.put("attributes", attributes);
		}
		
		//look for images
		Elements images = p.select("div.content").select("img");
		if (images.size() > 0) {
			JSONArray imageArray = new JSONArray();
			for (org.jsoup.nodes.Element a: images) {
				String imageURL = a.attr("abs:src");
				_seenImages.addURLToCrawl(imageURL);
				JSONObject jo = new JSONObject().put("href",imageURL);
				imageArray.put(jo);
			}
			postInfo.put("images", imageArray);
		}					
		
						
		//look for hyperlinks
		Elements hrefs = p.select("div.content").select("a");
		if (hrefs.size() > 0) {
			JSONArray hrefArray = new JSONArray();
			for (org.jsoup.nodes.Element a: hrefs) {
				String linkURL = a.attr("abs:href");
				_seenURLs.addURLToCrawl(linkURL);
				JSONObject jo = new JSONObject().put("href",linkURL)
						                        .put("linkText",a.text());
				hrefArray.put(jo);
			}
			postInfo.put("hyperlinks", hrefArray);
		}					
		return postInfo;
	}
	
	public JSONObject processPostTableBased(Element p, Instant startTime, Instant lastJobStartTime, String currentPageURL, TreeResult postTree, String threadID) {
		JSONObject postInfo = new JSONObject();
		postInfo.put("crawled_dt", startTime.toString());

		System.out.println(p);
		
		String date = p.select("tr td").first().text();
		try {
			Instant isoActivityDate = Forum_VBulletin.toISOInstantDate(date, _zone);
			//System.out.println(isoActivityDate.toString());
			if (isoActivityDate.isBefore(lastJobStartTime)) {
				srcLogger.log(Level.FINE, "skipping old post");
				return new JSONObject("{ \"continue\": true} ");
			}
			
			postInfo.put("postDate", isoActivityDate.toString());
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "vBulletin unable to convert date: "+date);
		}
		
		String postID = p.select("table[id^=post]").first().attr("id");
		if (postID.length()>0 && postID.startsWith("post")) { postID = postID.substring(4); }  // ignore the leading "post" in the ID
		postInfo.put("postID", postID);
		
		// link to the parent record if it exists
		if (postTree != null && postTree.nodes.containsKey(postID)) {
			PostNode pn = postTree.nodes.get(postID);
			if (pn._parentNode != null) {
				postInfo.put("replyToPostID", pn._parentNode._postID);
			}
		}
		
		postInfo.put("threadID", threadID);
		
		String titleSelectorID = "td#td_post_"+postID +">div.smallfont";
		String contentSelectorID = "div#post_message_"+postID;
		
		postInfo.put("postTitle", p.select(titleSelectorID).text());
		postInfo.put("text", p.select(contentSelectorID).text());
		postInfo.put("html", p.select(contentSelectorID).html());
		
		postInfo.put("author", p.select("div#postmenu_"+postID+">a").text().trim());
		String authorURL = p.select("div#postmenu_"+postID+">a").first().attr("abs:href");
		_seenMembers.addURLToCrawl(authorURL);
		postInfo.put("authorURL", authorURL);
		String authorID = authorURL.substring(authorURL.indexOf("?")+1, authorURL.indexOf("-") <0 ? authorURL.length() :  authorURL.indexOf("-") );
		if (authorID.startsWith("u=")) { authorID = authorID.substring(2); }
		postInfo.put("authorID", authorID);
		
		JSONObject attributes = this.extractExtraFields( p.select(contentSelectorID).html());
		if (attributes.length() >0) {
			postInfo.put("attributes", attributes);
		}
		
		//look for images
		Elements images = p.select(contentSelectorID).select("img");
		if (images.size() > 0) {
			JSONArray imageArray = new JSONArray();
			for (org.jsoup.nodes.Element a: images) {
				String imageURL = a.attr("abs:src");
				_seenImages.addURLToCrawl(imageURL);
				JSONObject jo = new JSONObject().put("href",imageURL);
				imageArray.put(jo);
			}
			postInfo.put("images", imageArray);
		}					
		
						
		//look for hyperlinks
		Elements hrefs = p.select(contentSelectorID).select("a");
		if (hrefs.size() > 0) {
			JSONArray hrefArray = new JSONArray();
			for (org.jsoup.nodes.Element a: hrefs) {
				String linkURL = a.attr("abs:href");
				_seenURLs.addURLToCrawl(linkURL);
				JSONObject jo = new JSONObject().put("href",linkURL)
						                        .put("linkText",a.text());
				hrefArray.put(jo);
			}
			postInfo.put("hyperlinks", hrefArray);
		}					
		return postInfo;
	}
	
	
	
	/**
	 * From the current document (in doc), extract out any friends that are listed.
	 * then process the next set of results for friends until there are not any more...
	 * 
	 * @param httpclient
	 * @param doc
	 * @param memberURL
	 * @return
	 */
	public JSONArray processMemberFriendList(CloseableHttpClient  httpclient, org.jsoup.nodes.Document doc, String memberURL, Instant jobStartTime) {
		JSONArray friends = new JSONArray();
		String nextURL = null;
		
		do {
			Elements friendList = doc.select("ol.friends_list a.username");
			for (org.jsoup.nodes.Element item: friendList) {
				JSONObject friend = createMemberObject(item.attr("abs:href"),item.text(),jobStartTime);
				friends.put(friend);
			}
			
			
			nextURL = null;
			Elements nextList = doc.select("div#view-friends-content a[rel=next]");
			if (nextList.size() > 0) {
				org.jsoup.nodes.Element e = nextList.get(0);
				nextURL = e.attr("abs:href"); 
			}
		
			if (nextURL != null ) {
				try (CloseableHttpResponse response = this.executeHttpCall(httpclient, new HttpGet(nextURL))) {
					HttpEntity entity = response.getEntity();
					String content = EntityUtils.toString(entity, "UTF-8");
	
					doc = Jsoup.parse(content, memberURL);
				}
				catch (Exception e) {
					srcLogger.log(Level.SEVERE, "vBulletin exception: " + e.toString());
					doc = null;
				}
			}
		}
		while (nextURL != null  && doc != null  );
		
		return friends;
	}
	
	private JSONObject createMemberObject(String memberURL, String userName, Instant startJobTime) {
		JSONObject member = new JSONObject();
		member.put("url",memberURL);
		member.put("user_name", userName);
		member.put("firstSeen_dt", startJobTime.toString());
		member.put("latestSeen_dt", startJobTime.toString());
		
		if (memberURL.contains("member.php?u=")) {
			String userID = memberURL.substring(memberURL.indexOf("?")+3);
			member.put("user_id", userID);
		}
		else {
			String param = memberURL.substring(memberURL.indexOf("?")+1);
			String[] parts = param.split("-");
			member.put("user_id", parts[0]);			
		}

		return member;
	}


	
	private JSONObject extractFieldValuePairs(org.jsoup.nodes.Document doc, String selector) {
		JSONObject attributes = new JSONObject();

		Elements memberInfoList = doc.select(selector);
		for (org.jsoup.nodes.Element item: memberInfoList) {
			String property = item.select("dt").text().toLowerCase().replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_');
			String value    = item.select("dd").text();
			attributes.put(property, value);
		}
		
		return attributes;
	}	
	
	private JSONObject extractFieldValuePairsSingleDLTag(Elements  memberInfoList) {
		JSONObject attributes = new JSONObject();
		
		for (org.jsoup.nodes.Element item: memberInfoList) {
			for (Element dtItem: item.select("dt")) {
				String property = dtItem.text().toLowerCase().replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_');
				String value    = dtItem.nextElementSibling().text();
				attributes.put(property, value);

			}
		}
		
		return attributes;
	}	
	
	private void extractFieldValuePairsColonSeparateListItem(JSONObject profile, Elements listItems) {
		for (org.jsoup.nodes.Element item: listItems) {
			String text = item.text();
			if (text.contains(":")) {
				String property = text.substring(0,text.indexOf(":")).trim().toLowerCase().replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_');
				String value    = text.substring(text.indexOf(":")+1).trim();
				profile.put(property, value);
			}
			else if (text.contains("™")) {
				String property = text.substring(0,text.indexOf("™")).trim().toLowerCase().replaceAll("\\?", "").replaceAll("\\(","").replaceAll("\\)","").replaceAll(":", "").trim().replace(" ","_").replace('.', '_');
				String value    = text.substring(text.indexOf("™")+1).trim();
				profile.put(property, value);
			}

		}	
	}
	
	
	private Pattern extraFieldPattern = Pattern.compile(	"\\<b\\>(.*?):\\</b\\>(.*)");

	private String _linkType;
	
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
	
	public JSONObject processMember(CloseableHttpClient  httpclient, String memberURL, Instant startTime, Instant lastJobStartTime, boolean searchMemberEmail, boolean searchMemberPhone) {
		JSONObject member = new JSONObject();
		member.put("user_url", memberURL);
		member.put("first_crawled_dt", startTime.toString());
		member.put("latest_crawled_dt", startTime.toString());
		
		if (memberURL.contains("member.php?u=")) {
			String userID = memberURL.substring(memberURL.indexOf("?")+3);
			member.put("user_id", userID);
		}
		else {
			String param = memberURL.substring(memberURL.indexOf("?")+1);
			String[] parts = param.split("-");
			member.put("user_id", parts[0]);			
		}
			
		HttpGet testGet = new HttpGet(memberURL);
		try (CloseableHttpResponse response = this.executeHttpCall(httpclient,testGet)) {
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "UTF-8");

			org.jsoup.nodes.Document doc = Jsoup.parse(content, memberURL);
			
			Elements name  = doc.select("span.member_username");
			if (name.size() ==0) {
				processMemberTableBased(httpclient, member, doc, memberURL, startTime, lastJobStartTime, searchMemberEmail, searchMemberPhone);
			}
			else {
				processMemberListBased(httpclient, member, doc, memberURL, startTime, lastJobStartTime, searchMemberEmail, searchMemberPhone);
			}


		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "vBulletin exception: " ,e);
			return member;
		} 
		
		return member;
	}	
	
	
	
	private void processMemberListBased(CloseableHttpClient  httpclient, JSONObject member, Document doc, String memberURL, Instant startTime, Instant lastJobStartTime, boolean searchMemberEmail, boolean searchMemberPhone) {
		member.put("user_name", doc.select("span.member_username").text());
		member.put("user_title", doc.select("span.usertitle").text());
							
		member.put("profile", extractFieldValuePairs(doc,"div.member_content dl"));    //"div.member_blockrow>dl"));
		//member.put("statistics", extractFieldValuePairs(doc, "div#view-stats dl"));
		
		
		JSONArray recentVisitors = new JSONArray();
		Elements visitorList = doc.select("div.visitors a.username");
		for (org.jsoup.nodes.Element item: visitorList) {
			JSONObject visitor = createMemberObject(item.attr("abs:href"),item.text(),startTime);
			recentVisitors.put(visitor);
		}
		
		member.put("recentVisitors", recentVisitors);
		member.put("friends", this.processMemberFriendList(httpclient, doc, memberURL,startTime));
		
		Elements visitCountList = doc.select("span.totalvisits>strong");
		if (visitCountList.size()>0) {
			String count = visitCountList.text();
			count = count.replaceAll(",", "");
			member.put("visitCount", Integer.parseInt(count));
		}
		
		Elements reputationList = doc.select("h3#reputation");
		if (reputationList.size()>0) {
			String pointStr = reputationList.text();
			pointStr = pointStr.substring(0,pointStr.indexOf("point")).trim();
			pointStr = pointStr.replaceAll(",", "");
			member.put("reputation", Integer.parseInt(pointStr));
		}
		
		Elements avatarImageList = doc.select("span.avatarcontainer>img");
		if (avatarImageList.size()>0) {
			String imageURL = avatarImageList.first().attr("abs:src");
			_seenImages.addURLToCrawl(imageURL);
			member.put("avatar_image", imageURL);
		}
		
		String email       = member.getJSONObject("profile").optString("email_address", "");
		String phoneNumber = member.getJSONObject("profile").optString("phone_number", "");
		
		if (searchMemberEmail && email.length()>0) {
			member.put("google_email", this.findGoogleSearchResults(email, 20));
		}
		if (searchMemberPhone && phoneNumber.length()>0) {
			member.put("google_phone", this.findGoogleSearchResults(phoneNumber, 20));
		}
		
		Elements albumHREF = doc.select("div.albums a:containsOwn(more)");
		if (albumHREF.size() >0) {
			String albumURL = albumHREF.first().attr("abs:href");
			JSONArray albums = this.processMemberAlbums(httpclient,albumURL,lastJobStartTime);
			member.put("albums",albums);
		}
		
		//Bring in photostream
		String photoStreamURL = memberURL+"&tab=activitystream&type=photos";
		member.put("photoStream", processPhotoStream(httpclient,photoStreamURL));
		
		this.normalizeDateField(member.getJSONObject("profile"),"last_activity");
		this.normalizeDateField(member.getJSONObject("profile"),"join_date");
		
		// convert profile to a text so we have something to store in that space
		StringBuilder textSB = new StringBuilder(member.getString("user_name")); textSB.append("\n\n");
		JSONObject profile = member.getJSONObject("profile");
		for (String key: profile.keySet()) {
			textSB.append(key); textSB.append(": "); textSB.append(profile.getString(key));
		}
		member.put("text", textSB.toString());
		
	}

	private void processMemberTableBased(CloseableHttpClient  httpclient, JSONObject member, Document doc, String memberURL, Instant startTime, Instant lastJobStartTime, boolean searchMemberEmail, boolean searchMemberPhone) {
	
		
		member.put("user_name", doc.select("div#main_userinfo h1").text());
		member.put("user_title", doc.select("div#main_userinfo h2").text());
							
		JSONObject profile = extractFieldValuePairsSingleDLTag(doc.select("div#profile_tabs dl"));
		extractFieldValuePairsColonSeparateListItem(profile, doc.select("div#profile_tabs li"));
		member.put("profile", profile);  
		//member.put("statistics", extractFieldValuePairs(doc, "div#view-stats dl"));
		
		
		JSONArray recentVisitors = new JSONArray();
		Elements visitorList = doc.select("div.visitors a.username");
		for (org.jsoup.nodes.Element item: visitorList) {
			JSONObject visitor = createMemberObject(item.attr("abs:href"),item.text(),startTime);
			recentVisitors.put(visitor);
		}
		
		member.put("recentVisitors", recentVisitors);
		member.put("friends", this.processMemberFriendList(httpclient, doc, memberURL,startTime));
		
		Elements visitCountList = doc.select("span.totalvisits>strong");
		if (visitCountList.size()>0) {
			String count = visitCountList.text();
			count = count.replaceAll(",", "");
			member.put("visitCount", Integer.parseInt(count));
		}
		
		Elements reputationList = doc.select("h3#reputation");
		if (reputationList.size()>0) {
			String pointStr = reputationList.text();
			pointStr = pointStr.substring(0,pointStr.indexOf("point")).trim();
			pointStr = pointStr.replaceAll(",", "");
			member.put("reputation", Integer.parseInt(pointStr));
		}
		
		Elements avatarImageList = doc.select("span.avatarcontainer>img");
		if (avatarImageList.size()>0) {
			String imageURL = avatarImageList.first().attr("abs:src");
			_seenImages.addURLToCrawl(imageURL);
			member.put("avatar_image", imageURL);
		}
		
		String email       = member.getJSONObject("profile").optString("email_address", "");
		String phoneNumber = member.getJSONObject("profile").optString("phone_number", "");
		
		if (searchMemberEmail && email.length()>0) {
			member.put("google_email", this.findGoogleSearchResults(email, 20));
		}
		if (searchMemberPhone && phoneNumber.length()>0) {
			member.put("google_phone", this.findGoogleSearchResults(phoneNumber, 20));
		}
		
		Elements albumHREF = doc.select("div.albums a:containsOwn(more)");
		if (albumHREF.size() >0) {
			String albumURL = albumHREF.first().attr("abs:href");
			JSONArray albums = this.processMemberAlbums(httpclient,albumURL,lastJobStartTime);
			member.put("albums",albums);
		}
		
		//Bring in photostream
		String photoStreamURL = memberURL+"&tab=activitystream&type=photos";
		member.put("photoStream", processPhotoStream(httpclient,photoStreamURL));
		
		this.normalizeDateField(member.getJSONObject("profile"),"last_activity");
		this.normalizeDateField(member.getJSONObject("profile"),"join_date");
		
		// convert profile to a text so we have something to store in that space
		StringBuilder textSB = new StringBuilder(member.getString("user_name")); textSB.append("\n\n");
		JSONObject profileForText = member.getJSONObject("profile");
		for (String key: profileForText.keySet()) {
			textSB.append(key); textSB.append(": "); textSB.append(profileForText.getString(key));
		}
		member.put("text", textSB.toString());
		
	}

	public JSONArray processPhotoStream(CloseableHttpClient  httpclient, String photoStreamURL) {
		JSONArray result = new JSONArray();
			
		HttpGet getPhotos = new HttpGet(photoStreamURL);
		try (CloseableHttpResponse response = this.executeHttpCall(httpclient, getPhotos)) {
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "UTF-8");

			org.jsoup.nodes.Document doc = Jsoup.parse(content, photoStreamURL);
			Elements photos = doc.select("ul#activitylist img");
			for (org.jsoup.nodes.Element photo: photos) {
				String image = photo.attr("abs:src");
				image = image.replace("&thumb=1", "");
				_seenImages.addURLToCrawl(image);
				result.put(image);
			}
		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "vBulletin exception: " + e.toString());
			return result;
		} 
		
		return result;
	}

	
	
	public JSONArray processMemberAlbums(CloseableHttpClient  httpclient, String albumsURL, Instant lastJobStartTime) {
		JSONArray result = new JSONArray();
		
		JSONArray albumList = new JSONArray();
		
		HttpGet testGet = new HttpGet(albumsURL);
		try (CloseableHttpResponse response = this.executeHttpCall(httpclient, testGet)) {
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "UTF-8");

			org.jsoup.nodes.Document doc = Jsoup.parse(content, albumsURL);
			Elements albumElements = doc.select("li.albumlist_entry");
			for (org.jsoup.nodes.Element albumItem: albumElements) {
				JSONObject album = new JSONObject();
				album.put("title",   albumItem.select("h3.albumtitle").text());
				String albumURL = albumItem.select("h3.albumtitle>a").first().attr("abs:href");
				album.put("albumURL", albumURL);
				album.put("albumID", albumURL.substring(albumURL.indexOf("=")+1));
				
				String textDate = albumItem.select("dl dt:contains(Last)+dd").text();
				try {
					Instant lastUpdateDate = Forum_VBulletin.toISOInstantDate(textDate, _zone);
					if (lastUpdateDate.isBefore(lastJobStartTime)) {
						continue;
					}
					album.put("lastUpdated", lastUpdateDate.toString());
				}
				catch (DateTimeParseException dtpe) {
					srcLogger.log(Level.WARNING, "Unable to convert date: "+textDate);
				}
				
				albumList.put(album);
			}

		}
		catch (Exception e) {
			srcLogger.log(Level.SEVERE, "vBulletin exception: " + e.toString());
			return result;
		} 		
		
	
		
		for (int i=0;i<albumList.length();i++) {
			JSONObject album = albumList.getJSONObject(i);

			HttpGet albumHTTPGet = new HttpGet(album.getString("albumURL"));
			try (CloseableHttpResponse response = this.executeHttpCall(httpclient, albumHTTPGet)) {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");

				org.jsoup.nodes.Document doc = Jsoup.parse(content, album.getString("albumURL"));
				Elements imageElements = doc.select("ol#thumbnails>li");
				JSONArray images = new JSONArray();
				for (org.jsoup.nodes.Element imageElement: imageElements) {
					String image = imageElement.select("img").first().attr("abs:src");
					image = image.substring(0,image.indexOf("&thumb"));
					_seenImages.addURLToCrawl(image);
					images.put(image);
				}
				album.put("images", images);
				result.put(album);
			}
			catch (Exception e) {
				srcLogger.log(Level.SEVERE, "vBulletin exception: " + e.toString());
				return result;
			} 		
			
		}
		
		return result;
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
			JSONObject config = this.getJob().getConfiguration();

			boolean searchMemberEmail  = config.optBoolean("searchMemberEmail",false);
			boolean searchMemberPhone  = config.optBoolean("searchMemberPhone",false);
			boolean mergeMemberRecords = config.optBoolean("mergeMemberRecords",true);		
			boolean retrieveImages     = config.optBoolean("retrieveImages", false);
			boolean retrieveURLs       = config.optBoolean("retrieveURLs", false);
			boolean retrieveMemberInfo = config.optBoolean("retrieveMemberInfo", true);
			
			_linkType = config.getString("linkType");
			
			String userName = config.getString("username");
			String password = this.getJobConfigurationFieldAsString("password");
			String userAgentString = config.optString("userAgentString","");
			_delayMilliseconds = config.optInt("requestDelay", 1000);
			this.setTimeZoneID(config.getString("defaultTimeZone"));
			
			URI u = new URI(this.getJob().getPrimaryFieldValue() );
			String uriDomain = u.getHost();  if (uriDomain.startsWith("www.")) { uriDomain = uriDomain.substring(4); }
			
			String loginURL = this.getJob().getPrimaryFieldValue() + "/login.php?do=login";
			
			java.util.List<String> forumsToCrawl = JSONUtilities.toStringList(config.getJSONArray("forumsToCrawl"));
			
					
			Instant startTime = Instant.now();
			Instant lastJobStartTime = Instant.ofEpochMilli(config.optLong("lastCrawlEpochMillis",0));
			
			srcLogger.log(Level.INFO, "Job last processed time: " + lastJobStartTime.toString());
			
			CloseableHttpClient httpClient = this.authenticate(loginURL,userName,password,userAgentString);
			if (httpClient == null) {
				srcLogger.log(Level.INFO, "Job procesing errored: " + this.getJob().getName());
				JobHistoryStatus status = JobHistoryStatus.ERRORED;
				String message = "unable to authenticate";				
				this.getJobCollector().sourceHandlerCompleted(this, status,message);
				this.setJobHistoryStatus(status);
				return;
			}
			
			for (String forum: forumsToCrawl) {
				String forumURL = this.getForumURL(forum);
				
				JSONArray threads = this.processForum(httpClient, forumURL, startTime, lastJobStartTime);
				for (int i=0; i< threads.length();i++) {
					
					JSONObject threadInfo = threads.getJSONObject(i);
					String text = threadInfo.optString("threadTitle", "") +"\n" + threadInfo.optString("infoTitle", "");
					String authorDocumentID = this.createUserID(uriDomain, threadInfo.getString("authorID"));
					
					threadInfo.put("text", text);
					threadInfo.put("authorDocumentID", authorDocumentID);
					
					String threadURL = threadInfo.getString("threadURL");
					Job job = this.getJob();
					String alternateThreadID = this.createThreadID(uriDomain, forum, threadInfo.getString("threadID"));
					edu.ncsu.las.document.Document threadDocument = new edu.ncsu.las.document.Document(threadInfo, MimeType.FORUM_THREAD,"forum_thread",job.getConfiguration(), job.getSummary(),threadURL,uriDomain,this.getDomainInstanceName(),alternateThreadID, this.getJobHistory());
					this.getDocumentRouter().processPage(threadDocument,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
					
					JSONArray posts = this.processThread(httpClient, threadInfo,startTime,lastJobStartTime);
					for (int j=0; j < posts.length();j++) {
						JSONObject postInfo = posts.getJSONObject(j);
											
						postInfo.put("authorDocumentID",  this.createUserID(uriDomain, postInfo.getString("authorID")));
						postInfo.put("threadDocumentID", alternateThreadID);
						String postURL = threadInfo.getString("threadURL")+"&p="+postInfo.getString("postID");
						String postAlternateId = this.createPostID(uriDomain, forum, threadInfo.getString("threadID"), postInfo.getString("postID"));
						edu.ncsu.las.document.Document postDocument = new edu.ncsu.las.document.Document(postInfo, MimeType.FORUM_POST,"forum_post",job.getConfiguration(), job.getSummary(),postURL,uriDomain,this.getDomainInstanceName(),postAlternateId, this.getJobHistory());
						this.getDocumentRouter().processPage(postDocument,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
					}				
				}
			}
			
			if (retrieveMemberInfo) {	processAllSeenMembers(httpClient, startTime, lastJobStartTime,uriDomain, searchMemberEmail, searchMemberPhone, mergeMemberRecords); }
			else { srcLogger.log(Level.INFO, "Not procesing detailed membership info - config setting: "+this.getJob().getName());}

			if (retrieveImages) {	processImages(httpClient); }
			else { srcLogger.log(Level.INFO, "Not processing linked images - config setting: "+this.getJob().getName());}
			
			if (retrieveURLs) {    processURLs(httpClient);	}
			else { srcLogger.log(Level.INFO, "Not procesing linked - config setting: "+this.getJob().getName());}
			
			httpClient.close();
			
			//update the configuration with our latest run time
			long lastCrawlEpochMillisNewValue = startTime.toEpochMilli();
			config = this.getJob().getConfiguration();
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
	
	private String getForumURL(String forum) {
		if (_linkType.equals(LINK_TYPE_WITH_ID)) {
			return this.getJob().getPrimaryFieldValue() + "/forumdisplay.php?"+ forum;
		}
		else if (_linkType.equalsIgnoreCase(LINK_TYPE_ID_ONLY)) {
			return this.getJob().getPrimaryFieldValue() + "/forumdisplay.php?f="+forum;
		}
		else {
			return this.getJob().getPrimaryFieldValue() + "/"+ forum;
		}		
	}

	private String getThreadHybridURL(String url) {
		if (_linkType.equals(LINK_TYPE_WITH_ID)) {
			return url + "&mode=hybrid";
		}
		else {
			return url+"?mode=hybrid";
		}		
	}

	
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
	
	public void processAllSeenMembers(CloseableHttpClient  httpclient, Instant startTime, Instant lastJobStartTime, String uriDomain, boolean searchMemberEmail, boolean searchMemberPhone, boolean mergeMemberRecords) {
		srcLogger.log(Level.INFO, "Processing seen members for detailed record: "+this.getJob().getName());
		
		String memberURL = null;
		while ( (memberURL = _seenMembers.getNextURLToCrawl()) != null) {
			_seenMembers.markURLCrawled(memberURL);
			JSONObject member = this.processMember(httpclient, memberURL, startTime, lastJobStartTime, searchMemberEmail, searchMemberPhone);
			
			Job job = this.getJob();
			String alternateID = this.createUserID(uriDomain, member.getString("user_id"));
			edu.ncsu.las.document.Document userDocument = new edu.ncsu.las.document.Document(member, MimeType.FORUM_USER,"forum_user",job.getConfiguration(), job.getSummary(),memberURL,uriDomain,this.getDomainInstanceName(),alternateID, this.getJobHistory());
			if (mergeMemberRecords) { userDocument.setDocumentMerger(new ForumUserMerger()); }
			this.getDocumentRouter().processPage(userDocument,false,true); //sending to this method so as to keep out of the original store and creating visited pages. underlying handlers are responsible for the storage
			
			System.out.println("Processed member: "+memberURL);
		}
	}
	
	public static class PostNode {
		private String _postID;
		private PostNode _parentNode;
		private java.util.ArrayList<PostNode> _children = new java.util.ArrayList<PostNode>();
		
		public PostNode(String postID, PostNode parent) {
			_postID = postID;
			_parentNode = parent;
		}
		
		public String getPostID() { return _postID; }
		
		public PostNode getParentNode() { return _parentNode; }
		
		public int getChildrenCount() { return _children.size(); }
		
		public void addChild(PostNode child) { _children.add(child); }
		
		public PostNode getChildAt(int i) { return _children.get(i); }
		public PostNode getLastChild() { return _children.get(_children.size()-1); }
		
		
		public PostNode getLastChildAtDepth(int depth) {
			if (depth <=0) {
				return this;
			}
			else {
				return this.getLastChild().getLastChildAtDepth(depth-1);
			}
		}
		
		public String toString(int indentFactor) {
			StringBuilder sb = new StringBuilder();
			for (int i=0;i<indentFactor; i++) {
				sb.append(" ");
			}
			return toString("", sb.toString());
		}
		
		private String toString(String depth, String depthAddlString) {
			StringBuilder result = new StringBuilder();
			
			result.append(depth);
			result.append(_postID);
			result.append("\n");
			for (PostNode child: _children) {
				result.append(child.toString(depth+depthAddlString,depthAddlString));
			}
			return result.toString();
		}
		
	}
	
	private JSONObject getPostIDAndTreeString(String line) {
		if (line.contains("writeLink") == false) {return null;}
		
		line = line.substring(line.indexOf("(")+1);
		int endOfPostIDIndex = line.indexOf(",");
		int endOfSecondField = line.indexOf(",", endOfPostIDIndex+1);
		int endOfThirdField  = line.indexOf(",", endOfSecondField+1);
		int endOfAuthorID    = line.indexOf(",", endOfThirdField+1);
		int endOfTreeTag     = line.indexOf("\"", endOfAuthorID+3);
		String postID = line.substring(0, endOfPostIDIndex);
		String treeTag = line.substring(endOfAuthorID+3,endOfTreeTag);
		
		return new JSONObject().put("postID", postID).put("treeTag", treeTag);
	}
	
	private int getDepthFromTreeTag (String tag) {
		int depth = 0;
		
		String[] tags = tag.split(",");
		for (String t: tags) {
			try {
				int i = Integer.parseInt(t);
				depth += i;
			}
			catch (NumberFormatException nfe) {
				depth++;
			}
			
		}
		
		return depth;
	}
	
	public static class TreeResult {
		public PostNode root;
		public java.util.HashMap<String,PostNode> nodes;
		
	}
	
	public TreeResult createTreeFromScriptText(String text) {
		String[] lines = text.split("\n");
		
		PostNode root = null;
		java.util.HashMap<String,PostNode> nodes = new java.util.HashMap<String,PostNode>();
				
		for (String line: lines) {
			JSONObject parts = this.getPostIDAndTreeString(line);
			if (parts == null) { continue; }
			String treeTag = parts.getString("treeTag");
			String postID  = parts.getString("postID");
			if (treeTag.equals("")) {
				root = new PostNode(postID,null);
				nodes.put(postID, root);
			}
			else {
				//need to figure out depths, and get last child to that depth
				int depth = this.getDepthFromTreeTag(treeTag);
				PostNode parent = root.getLastChildAtDepth(depth-1);
				PostNode child = new PostNode(postID,parent);
				parent.addChild(child);
				nodes.put(postID, child);
			}
		}
		
		TreeResult result = new TreeResult();
		result.root  = root;
		result.nodes = nodes;
		
		return result;
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
		CloseableHttpResponse response = httpclient.execute(request);
		
		return response;
	}
	
	
	public static void main(String args[]) throws IOException {
		/*
		System.out.println(toISODate("Today, 12:35 PM",ZoneId.of("US/Pacific")));
		System.out.println(toISODate("Yesterday, 12:35 PM",ZoneId.of("US/Pacific")));
		System.out.println(toISODate("02-22-2017 12:35 PM",ZoneId.of("US/Pacific")));
		System.out.println(toISODate("02-19-2017 12:35 PM",ZoneId.of("US/Pacific")));
        */
		
		//System.exit(0);
		
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
		srcLogger.log(Level.INFO, "Forum VBulleting Test Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);	

		JobHistory jh = new JobHistory();
		Job job = new Job(); 
		job.setDomainInstanceName("crazy");
		job.setConfig(new JSONObject());
		
		Forum_VBulletin ddgh = new Forum_VBulletin();
		ddgh.initialize(jh, job);
		ddgh.setTimeZoneID("US/Pacific");
		ddgh._linkType = LINK_TYPE_ID_ONLY;
				
		Instant startTime = Instant.now();
		Instant lastJobStartTime = Instant.now().minus(40, ChronoUnit.DAYS);
		System.out.println(lastJobStartTime);
		
		String userName="";
		String password="";
		
		CloseableHttpClient httpClient = ddgh.authenticate("http://seqanswers.com/forums/login.php?do=login",userName,password,"");
		if (httpClient != null) {
/*
			JSONObject threadInfo = new JSONObject( "{\r\n    \"threadID\": \"74550\",\r\n    \"replyCount\": 5,\r\n    \"infoTitle\": \"Hello, \\n \\nI am in a lab that has recently purchased a illumina miniseq. Our main focus for using this machine is to sequence high numbers of PCR...\",\r\n    \"author\": \"valenex\",\r\n    \"threadTitle\": \"Library Prepration for MiniSeq Help\",\r\n    \"threadURL\": \"http://seqanswers.com/forums/showthread.php?t=74550\",\r\n    \"authorURL\": \"http://seqanswers.com/forums/member.php?u=73203\",\r\n    \"crawled_dt\": \"2017-03-05T15:34:55.175Z\",\r\n    \"viewCount\": 413,\r\n    \"authorID\": \"73203\"\r\n}");
			JSONArray posts = ddgh.processThread(httpClient, threadInfo,startTime,lastJobStartTime);
			System.out.println("postCount: "+posts.length());
			System.out.println(posts.getJSONObject(1).toString(4));
*/		
			/*
			JSONArray threads = ddgh.processForum(httpClient, "http://seqanswers.com/forums/forumdisplay.php?f=6", startTime, lastJobStartTime);
			for (int i=0; i< threads.length();i++) {
				JSONObject threadInfo = threads.getJSONObject(i);
				System.out.println(threadInfo.toString(4));
				JSONArray posts = ddgh.processThread(httpClient, threadInfo,startTime,lastJobStartTime);
				System.out.println("postCount: "+posts.length());
				System.out.println(posts.getJSONObject(1).toString(4));
			}
			System.out.println("Member size:" + ddgh._seenMembers.crawlSize());
			System.out.println("URLs size:" + ddgh._seenURLs.crawlSize());
			System.out.println("Images size:" + ddgh._seenImages.crawlSize());
*/
			
			JSONObject member = ddgh.processMember(httpClient, "http://seqanswers.com/forums/member.php?u=6255", startTime, lastJobStartTime,true,true);
			System.out.println(member.toString(4));

	
			httpClient.close();
		}
		/*
		List<SearchRecord> results = ddgh.generateSearchResults("quadcopters drones");
		if (results !=null) {
			results.forEach(System.out::println);
		}
		*/
	}

	
	/*  Possible error messages
	    - You are not logged in or you do not have permission to access this page. This could be due to one of several reasons
	    - Invalid User specified. If you followed a valid link, please notify the administrator  (put in garbage on a member link)
	
	 */
	
	
}
