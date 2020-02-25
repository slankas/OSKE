package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.collector.util.ApplicationConstants;
import edu.ncsu.las.model.RSS;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.RSSFeed;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.UserOption;
import edu.ncsu.las.model.collector.UserOptionName;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.json.JSONUtilities;


/**
 * Returns the news feed results for a specified domain
 * 
 */
@RequestMapping(value = "rest/{domain}/rssFeed")
@Controller
public class RSSFeedController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "",  headers = "Accept=application/json")
	public @ResponseBody byte[] accessFeed(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER,"access feed" );
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.RSSFeedController.accessFeed", new JSONObject(), System.currentTimeMillis(),null, httpRequest,domainStr);

		RSSFeed news = RSSFeed.create(domainStr,this.getEmailAddress(httpRequest));
		if (news != null) {
			JSONObject result = new JSONObject().put("feed", news.getFeedArray());
			return result.toString().getBytes("UTF-8");
		}
		else {
			return new JSONObject().put("feed", new JSONArray()).toString().getBytes("UTF-8");
		}

	}

	
	/**
	 * Validates whether or not the given feed is valid.
	 * 
	 * Returns a json object that contains status (success/failure) and url.  If success, also contains title and size.  if failure, contains error
	 */
	@RequestMapping(value = "/validateFeed/{url:.+}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String validateFeed(HttpServletRequest request, @PathVariable("url") String urlBase64,HttpServletRequest httpRequest,@PathVariable("domain") String domainStr)	throws  IOException, ValidationException {
		logger.log(Level.FINER,	"validate feed");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		
		String url = new String(Base64.getDecoder().decode(urlBase64.replace("$", "/")));
		String userAgent = SourceHandlerInterface.getNextUserAgent(domainStr);
		JSONObject result = RSS.validateRSSFeed(url, userAgent);
			
	    this.instrumentAPI("edu.ncsu.las.rest.collector.RSSFeedController.validateFeed",result,startTime,System.currentTimeMillis(), httpRequest,domainStr);

		return result.toString();
	}	
	
	/**
	 * Retrieves the feeds for the current user.  If a user does not have any customizations for a feed, then the default for the domain is returned.
	 * Return object: 
	 *    "feeds" - json array with each element containing a json object of title,url, uuid
	 *    "keywords" - json array of strings
	 *
	 */
	@RequestMapping(value = "/userFeed", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String retrieveUserFeed(HttpServletRequest request, HttpServletRequest httpRequest,@PathVariable("domain") String domainStr)	throws  IOException, ValidationException {
		logger.log(Level.FINER,	"retrieve user feed");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		long startTime = System.currentTimeMillis();
		JSONObject result;
		boolean usedDefault = true;
		
		UserOption customFeeds = UserOption.retrieve(this.getEmailAddress(request),domainStr, UserOptionName.BreakingNewsURLs);
		if (customFeeds == null) {
			List<String> urls     = JSONUtilities.toStringList(Configuration.getConfigurationPropertyAsArray(domainStr, ConfigurationType.NEWS_FEED_URLS));
			JSONArray keywords = Configuration.getConfigurationPropertyAsArray(domainStr, ConfigurationType.NEWS_FEED_KEYWORDS);
			JSONArray feeds = new JSONArray();
			for (String url: urls) {
				JSONObject feedObject = new JSONObject().put("title",RSSFeed.getRSSFeedTitle(url))
						                                .put("url", url)
						                                .put("uuid", UUID.randomUUID().toString());
				feeds.put(feedObject);
			}
			result = new JSONObject().put("feeds",feeds)
					                 .put("keywords",keywords);
		}
		else {
			usedDefault = false;
			result = new JSONObject(customFeeds.getOptionValue());
		}
			
	    this.instrumentAPI("edu.ncsu.las.rest.collector.RSSFeedController.retrieveUserFeed",new JSONObject().put("usedDefault",usedDefault),startTime,System.currentTimeMillis(), httpRequest,domainStr);

		return result.toString();
	}	
	
	
    @RequestMapping(value = "/userFeed", consumes = "application/json",  headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody String saveUserFeed(HttpServletRequest request, HttpServletResponse response,
                            @PathVariable("domain") String domainStr, @RequestBody String bodyStr)     throws ValidationException, IOException {
        logger.log(Level.FINER, "create/update ");
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        long startTime = System.currentTimeMillis();
        
        User u = this.getUser(request);
        JSONObject feedJSON = new JSONObject(bodyStr);

        // TODO - validation of the feedJSON object... see return object notes above.  should validate URLs work.  Copy titles from them..
        
        RSSFeed.invalidate(domainStr, u.getEmailID());
        if (feedJSON.getJSONArray("feeds").length() == 0) {
        	UserOption.destroy(u.getEmailID(), domainStr, UserOptionName.BreakingNewsURLs);
        	return new JSONObject().put("status","success").toString();
        }
        else {
	        UserOption uo = new UserOption(u.getEmailID(), domainStr, UserOptionName.BreakingNewsURLs, feedJSON.toString());
	        if (uo.save()) {
	        	this.instrumentAPI("edu.ncsu.las.rest.collector.RSSFeedController.saveUserFeed",new JSONObject(),startTime,System.currentTimeMillis(), request,domainStr);
	        	return new JSONObject().put("status","success").toString();
	        }
	        else {
	        	return new JSONObject().put("status","failure").put("error", "unable to save").toString();
	        }
        }
    }	
	
	
}