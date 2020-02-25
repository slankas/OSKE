package edu.ncsu.las.source.util;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.SiteCrawlRule;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.source.VideoHandler;
import edu.ncsu.las.source.WebSourceHandler;
import edu.uci.ics.crawler4j.crawler.Page;

import edu.uci.ics.crawler4j.url.WebURL;



public class SourceCrawler extends  edu.uci.ics.crawler4j.crawler.WebCrawler {
	private static final Logger srcLogger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	private static final Pattern mediaWikiActionURL = Pattern.compile("&action=[a-z]+");
	private static final Pattern mediaWikiPrintURL  = Pattern.compile("wiki/.+?\\&printable=yes");
	
	private WebSourceHandler _myWebSourceHandler = null;
	
	public SourceCrawler() {  // called by crawler4j when establishing the crawling session
		super();
	}

	
	public SourceCrawler(WebSourceHandler wsh) {
		_myWebSourceHandler = wsh;
	}
	
	public WebSourceHandler getMyWebSourceHandler() {
		if (_myWebSourceHandler == null) {
			_myWebSourceHandler = (WebSourceHandler) this.getMyController().getCustomData();

		}
		return _myWebSourceHandler;
	}
	
	/**
	 * This method checks whether or not the URL should be visited.
	 * 
	 * @param referringPage  page in which the new url was discovered.  May be null
	 * @param url URL to check whether or not to visit.
	 */
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {	
		//SourceHandlerData shData = this.getMyWebSourceHandler().getSourceHandlerData();
		
		ProcessConfiguration configData = this.getMyWebSourceHandler().getProcessCheckConfiguration();
		
		//TODO: may need to fix this ...
		//shData._router.setSourceHandler(this);  // by setting this, we ensure router has an pointer to the created version of WebSourceHandler by the crawler
		
		String href = url.getURL().toLowerCase();
		String urlHost = url.getSubDomain().trim().equals("") ? url.getDomain() : url.getSubDomain() + "." + url.getDomain();
		urlHost = urlHost.toLowerCase();
		String urlDomain = url.getDomain().toLowerCase();
		
		String referringDomain = "";
		if (referringPage != null) {
			referringDomain = referringPage.getWebURL().getDomain();
		}
		
		//TODO: Need to refactor this code in functions...
		
		//TODO: Add check on block TLDs by domain instance name
		// 0. Check that TLD isn't prohibited
		if (Collector.getTheCollecter().getDomain(this.getMyWebSourceHandler().getDomainInstanceName()).isTopLevelDomainBlocked(urlDomain)) {
			this.getMyWebSourceHandler().addBlockedURL("blocked TLD:", url.getURL());
			srcLogger.log(Level.FINE, "Blocked TLD: " + href);
			return false;			
		}
		

		// 1. Check that domain isn't prohibited
		HashMap<String,SiteCrawlRule> siteRules = Collector.getTheCollecter().getSiteRules(this.getMyWebSourceHandler().getDomainInstanceName());
		if (siteRules != null && siteRules.containsKey(urlHost) ) {
			SiteCrawlRule scr = siteRules.get(urlHost);
			if (scr.getFlag().equalsIgnoreCase("EXCLUDE")) {
				this.getMyWebSourceHandler().addBlockedURL("siteCrawlRule", url.getURL());
				srcLogger.log(Level.FINE, "exclude domain rule found: " + href);
				return false;
			}
		}

		// 2. Check Exclude filters
		for (Pattern p : configData.excludeFilters) {
			if (p.matcher(href).find()) {
				this.getMyWebSourceHandler().addBlockedURL("excludeFilter", url.getURL());
				//srcLogger.log(Level.FINE, "exclude filter rule matched (" + p + "): " + href);
				return false;
			}
		}
		
		// 2.5: Check include Filter
		if (configData._includeFilter != null) {
			if (!configData._includeFilter.matcher(href).find()) {
				this.getMyWebSourceHandler().addBlockedURL("includeFilterFailed", url.getURL());
				srcLogger.log(Level.FINE, "include filter rule did not match (" + configData._includeFilter + "): " + href);
				return false;
			}
		}

		// 3. Check limit to domain on seed 
		if (this.getMyWebSourceHandler().getJob().getConfiguration().has("limitToDomain") && this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("limitToDomain")) {
			if ( this.getMyController().hasDomainInSeeds(urlDomain) == false) {
				if (this.checkDomainTraversal(this.getMyWebSourceHandler().getJob().getConfiguration(), urlDomain,referringDomain) == false) {
					this.getMyWebSourceHandler().addBlockedURL("originatingDomain", url.getURL());
					srcLogger.log(Level.FINE, "Not in the originating domain: " + href);
					return false;
				}
			}

		}
		
		// 3.5 Check limit to host on seed
		if (this.getMyWebSourceHandler().getJob().getConfiguration().has("limitToHost") && this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("limitToHost")) {
			if (this.getMyController().hasHostInSeeds(urlHost) == false) {
				if (this.checkDomainTraversal(this.getMyWebSourceHandler().getJob().getConfiguration(), urlDomain,referringDomain) == false) {
					this.getMyWebSourceHandler().addBlockedURL("originatingHost", url.getURL());
					srcLogger.log(Level.FINE, "Not in the originating domain: " + href);
					return false;
				}
			}
		}		

		// 4. Check if starts with path on seed
		if (this.getMyWebSourceHandler().getJob().getConfiguration().has("startsWithPath")) {
			String urlPath = url.getPath().toLowerCase();

			if (urlPath.startsWith(this.getMyWebSourceHandler().getJob().getConfiguration().getString("startsWithPath")) == false) {
				this.getMyWebSourceHandler().addBlockedURL("startsWithPath", url.getURL());
				srcLogger.log(Level.FINE, "URL does not start with path ("+ this.getMyWebSourceHandler().getJob().getConfiguration().getString("startsWithPath") + "): " + href);
				return false;
			}
		}

		// 5. Check if ReferringPage was a page we cared about. if not, return false
		// it is possible for ReferringPage to be null
		boolean checkForRelevancy = true;
		if (url.getDepth() == 0) { checkForRelevancy = false; } // can't check for relevancy on seeds
		if (url.getDepth() == 1) {
			if (this.getMyWebSourceHandler().getJob().getConfiguration().has("ignoreRelevancyForInitialURL") && this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("ignoreRelevancyForInitialURL")) {
				checkForRelevancy = false;
			}
		}
		if (this.getMyWebSourceHandler().getJob().getConfiguration().has("ignoreRelevancyForImages") && 
			this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("ignoreRelevancyForImages") &&
			referringPage !=null && referringPage.getContentType() !=null && referringPage.getContentType().contains("image") ) {
			checkForRelevancy = false;
		}

		if (checkForRelevancy) {
			if (referringPage != null && configData._relevancyPattern != null) {		
				if (!referringPage.hasRelevancyPattern(configData._relevancyPattern)) {
					this.getMyWebSourceHandler().addBlockedURL("relevancy", url.getURL());
					srcLogger.log(Level.FINEST, "Regular expression relevancy check failed: " + configData._relevancyPattern);
					return false;
				}
			}
		}
		
		// 6. check if limitToLanguage is set. If so, referring page must be in that language to continue
		boolean languageGood = true;
		if (referringPage != null && this.getMyWebSourceHandler().getJob().getConfiguration().has("limitToLanguage")) {
			languageGood = false;
			String documentLanguage = referringPage.getLanguage();
					
			JSONArray allowableLanguages = this.getMyWebSourceHandler().getJob().getConfiguration().getJSONArray("limitToLanguage");
			for (int i=0;i<allowableLanguages.length();i++) {
				String al = allowableLanguages.getString(i);
				if (documentLanguage.equals(al)) {
					languageGood = true;
					break;
				}
			}
		}
		if (languageGood == false) {
			this.getMyWebSourceHandler().addBlockedURL("language", url.getURL());
			srcLogger.log(Level.FINE, "Referring page was an invalid language: " + referringPage.getLanguage());
			return false;
		}

		// 7. Check wiki-media ignore special pages 
		if (this.getMyWebSourceHandler().getJob().getConfiguration().has("excludeWikiSpecialPages") && this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("excludeWikiSpecialPages") && referringPage != null && referringPage.isWikiMediaGenerated()) {
			if (href.contains("wiki/special") ||                      // href was set to all lower case at the start of this function...
				href.contains("wiki/index.php/special")   ||
				href.contains("wiki/talk:")   ||
				href.contains("wiki/index.php/talk:")   ||
				href.contains("wiki/index.php/user_talk:")   ||
				href.contains("wiki/index.php/user:")   ||
				href.contains("wiki/index.php?title=special%3a") ||
				mediaWikiActionURL.matcher(href).find() ||
				mediaWikiPrintURL.matcher(href).find()) {
				this.getMyWebSourceHandler().addBlockedURL("wikiSpecial", url.getURL());
				srcLogger.log(Level.FINE, "Skipping media-wiki special pages: " + href);
				return false;
			}
		}
		
		// 8. Check if the link points to a video. If it is a video, the domain has to allow videos and the job has to have download video set to true.
		//    this process will create a new VideoHandler based off of our configuration and then download the file.  We will return false from this so no further processing
		//    will take place.  (TODO: need to move this into its own function.)
		//
		if (MimeType.doesSuffixIndicateVideo(url.getURL())) {
			String domainInstanceName = this.getMyWebSourceHandler().getDomainInstanceName();
			
			if (Configuration.getConfigurationPropertyAsBoolean(domainInstanceName, ConfigurationType.VIDEO_UTILIZE)) {
				if (_visitedVideoURLs.contains(url.getURL()) == false && this.getMyWebSourceHandler().getJob().getConfiguration().has("downloadVideo") && this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("downloadVideo")) {
					VideoHandler vh = new VideoHandler();
					vh.initialize(this.getMyWebSourceHandler().getJobHistory(), this.getMyWebSourceHandler().getJob());
					Document retrievedVideo = vh.processURL(url.getURL()); // putting this into a separate method so that we can call it externally with a URL from the web source handler.
					if (retrievedVideo == null) { // we weren't able to download
						srcLogger.log(Level.INFO, "Unable to download video: " + url.getURL());
					}
					else {
						if (referringPage != null) { retrievedVideo.setReferrer(referringPage.getWebURL().getURL()); }
						vh.getDocumentRouter().processPage(retrievedVideo,"noSaveRaw  ignoreDuplicates");
					}
					_visitedVideoURLs.add(url.getURL()); // mark that we have visited this URL for videos
				}
			}
			
			return false;
		}
		
		srcLogger.log(Level.FINE, "Will visit " + url.getURL());

		return true;
	}

	
	/**
	 * This function is called when a page is fetched and ready to be processed
	 */
	@Override
	public void visit(Page page) {
		this.getMyWebSourceHandler().markActivity();
		srcLogger.log(Level.INFO, "Visited: " + page.getWebURL());
		
		try {
			JSONObject objConfig = this.getMyWebSourceHandler().getJob().getConfiguration();
			Document currentDocument = new Document(page, objConfig, this.getMyWebSourceHandler().getJob().getSummary(),this.getMyWebSourceHandler().getDomainInstanceName(), this.getMyWebSourceHandler().getJobHistory());
											
			boolean shouldProcess = this.checkToProcessPageOnVisit(currentDocument);
			if (shouldProcess) {
				this.getMyWebSourceHandler().getDocumentRouter().processPage(currentDocument,"");
				visitCheckForVideo(page, currentDocument);
			}
			else {
				srcLogger.log(Level.INFO, "Not processing: " + page.getWebURL());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	

	/**
	 * Checks whether or not its possible to link across domains
	 * 
	 * @param configuration
	 * @param pagehost
	 * @param seedHost
	 * @param referringDomain
	 * @return
	 */
	private boolean checkDomainTraversal(JSONObject configuration, String urlDomain, String referringDomain) {
		if (configuration.has("allowSingleHopFromReferrer") == true &&
			configuration.getBoolean("allowSingleHopFromReferrer") == true && 
			this.getMyController().hasDomainInSeeds(referringDomain)) {
				return true;
		}
		
		if (configuration.has("allowDomains")) {
			JSONArray domains = configuration.getJSONArray("allowDomains");
			for (int i=0; i < domains.length(); i++) {
				String domain = domains.getString(i);
				if (urlDomain.contains(domain)) {
					return true;
				}
			}
		}
		
		return false;
	}		
	
	/**
	 * should we process/store a given page or not?
	 * 
	 * @param page
	 * @param currentDocument
	 * @return
	 */
	public boolean checkToProcessPageOnVisit(Document currentDocument) {
	    ProcessConfiguration pc = this.getMyWebSourceHandler().getProcessCheckConfiguration();
	    
	    if (pc.hasExcludeParameter()) {
	    	// Check text
	    	if (pc.excludeText != null && pc.excludeText.matcher(currentDocument.getExtractedTextFromTika()).matches()) {
	    		this.getMyWebSourceHandler().addBlockedProcessURL("excludeText", currentDocument.getURL());
	    		srcLogger.log(Level.INFO, "not processing (exclude text): " + currentDocument.getURL());
	    		return false;
	    	}
	    		    	
	    	// Check URL
	    	if (pc.excludeURL != null && pc.excludeURL.matcher(currentDocument.getURL()).matches()) {
	    		this.getMyWebSourceHandler().addBlockedProcessURL("excludeURL", currentDocument.getURL());
	    		srcLogger.log(Level.INFO, "not processing (exclude URL): " + currentDocument.getURL());
	    		return false;
	    	}
	    	// check MimeType
	    	if (pc.excludeMimeTypes != null && pc.excludeMimeTypes.contains(currentDocument.getMimeType().trim().toLowerCase())) {
	    		this.getMyWebSourceHandler().addBlockedProcessURL("excludeMimeType", currentDocument.getURL());
	    		srcLogger.log(Level.INFO, "not processing (exclude mime type): " + currentDocument.getURL());
	    		return false;
	    	}    	
	    }
	    // at this point, the current document didn't meet any of the exclude criteria, check to see whether or not it should be processed/stored
		if (pc.hasIncludeParameter()) {
			if (pc.includeText != null && pc.includeText.matcher(currentDocument.getExtractedTextFromTika()).matches()) {
				srcLogger.log(Level.INFO, "processing (include text matched): " + currentDocument.getURL());
	    		return true;
	    	}
			if (pc.includeURL != null && pc.includeURL.matcher(currentDocument.getURL()).matches()) {
				srcLogger.log(Level.INFO, "processing (include URL matched): " + currentDocument.getURL());
	    		return true;
	    	}
			if (pc.includeMimeTypes != null && pc.includeMimeTypes.contains(currentDocument.getMimeType().trim().toLowerCase())) {
				srcLogger.log(Level.INFO, "processing (include mimeType matched): " + currentDocument.getURL());
	    		return true;
	    	}
			
			// none of the include criteria have been met.  Exclude
			this.getMyWebSourceHandler().addBlockedProcessURL("includeNotMeet", currentDocument.getURL());
			srcLogger.log(Level.INFO, "not processing (no inclusion criteria met): " + currentDocument.getURL());
			return false;
		}
		
		return true;
	}		
	
	private HashSet<String> _visitedVideoURLs = new HashSet<String>();
	
	
	private Pattern[] _videoPatternsURLs = null;

	/**
	 * Checks the given URL to see if it is a well-known video URL.
	 * 
	 * @param url
	 * @return
	 */
	public synchronized boolean isVideoWellKnownURL(String url) {
		if (_videoPatternsURLs == null) {
			JSONArray patterns = Configuration.getConfigurationPropertyAsArray(this.getMyWebSourceHandler().getDomainInstanceName(), ConfigurationType.VIDEO_URL_REGEXES);
			if (patterns == null) {patterns = new JSONArray(); } // this is semantically equivalent if the patterns weren't set.
			
			_videoPatternsURLs = new Pattern[patterns.length()];
			for (int i=0;i<patterns.length();i++) {
				_videoPatternsURLs[i] = Pattern.compile(patterns.getString(i));
			}
		}
		
		for (Pattern p: _videoPatternsURLs) {
			if (p.matcher(url).find()) { return true; }
		}
		return false;
	}
	
	/**
	 * Checks if the current page is a well-known video site and will process that if necessary.
	 * Both the domain configuration has to allow videos as well as the job configuration to download videos
	 * 
	 * @param p
	 * @param pageDocument
	 */
	public void visitCheckForVideo(Page p, Document pageDocument) {
		if (Configuration.getConfigurationPropertyAsBoolean(this.getMyWebSourceHandler().getDomainInstanceName(), ConfigurationType.VIDEO_UTILIZE) == false) { return; }
		if (this.getMyWebSourceHandler().getJob().getConfiguration().has("downloadVideo") == false) {return;}
		if (this.getMyWebSourceHandler().getJob().getConfiguration().getBoolean("downloadVideo") == false) { return; }
		
		String url = p.getWebURL().getURL();
		if (_visitedVideoURLs.contains(url)) { return; }
		
		if (this.isVideoWellKnownURL(url)) {
			VideoHandler vh = new VideoHandler();
			vh.initialize(this.getMyWebSourceHandler().getJobHistory(), this.getMyWebSourceHandler().getJob());
			Document retrievedVideo = vh.processURL(url); // putting this into a separate method so that we can call it externally with a URL from the web source handler.
			if (retrievedVideo == null) { // we weren't able to download
				srcLogger.log(Level.INFO, "Unable to download video: " + url);
			}
			else {
				if (pageDocument.getReferrer() != null) {
					retrievedVideo.setReferrer(pageDocument.getReferrer());
				}
				vh.getDocumentRouter().processPage(retrievedVideo,"noSaveRaw  ignoreDuplicates");
			}
			_visitedVideoURLs.add(url); // mark that we have visited this URL for videos
		}
		else {
			if (pageDocument.getMimeType().equals(MimeType.TEXT_HTML)) {
				String htmlText = new String(pageDocument.getContentData(), StandardCharsets.UTF_8);	
				
				org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlText,pageDocument.getURL());
				Elements videoTags = jsoupDoc.select("video");
				for (Element videoTag: videoTags) {
					Elements sources = videoTag.select("source");
					for (Element source: sources) {
						url  =source.absUrl("src");
						
						if (_visitedVideoURLs.contains(url)) {break;}  //we've downloaded one of the URLs already for set, no need to process
						_visitedVideoURLs.add(url);
						
						VideoHandler vh = new VideoHandler();
						vh.initialize(this.getMyWebSourceHandler().getJobHistory(), this.getMyWebSourceHandler().getJob());
						Document retrievedVideo = vh.processURL(url); // putting this into a separate method so that we can call it externally with a URL from the web source handler.
						if (retrievedVideo == null) { // we weren't able to download
							srcLogger.log(Level.FINER, "Unable to download video: " + url);
							continue;
						}
						else {
							if (pageDocument.getReferrer() != null) {
								retrievedVideo.setReferrer(pageDocument.getReferrer());
							}
							vh.getDocumentRouter().processPage(retrievedVideo,"noSaveRaw  ignoreDuplicates");
						}
						break; // we only need to download one of the sources per video tag
						
					}
				}
			}
			
		}

	}
	
}