package edu.ncsu.las.model.collector;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.BasicAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.NtAuthInfo;

/**
 * 
 * 
 *
 */
public class CrawlConfigWithJSON extends CrawlConfig {
	private static final Logger logger =Logger.getLogger(Collector.class.getName());
	
	private boolean _respectRobotsTxtFile = true;
	
	private int _numCrawlingThreads = 1;
	
	public boolean getRespectRobotsTxtFile() { return _respectRobotsTxtFile; }
	public void setRespectRobotsTxtFile(boolean newValue) { _respectRobotsTxtFile = newValue; }
	
	public int getNumCrawlingThreads() { return _numCrawlingThreads; }
	public void setNumCrawlingThreads(int value) { _numCrawlingThreads = value; }
	
	/**
	 * Builds up configuration by applying successive web configuration
	 * 
	 * @param domainName
	 * @param obj
	 */
	public void performConfiguration (String domain, JSONObject obj) {
		boolean allowAuthentication = Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.ACCESS_AUTHENTICATED_SITES);
		for (String key: obj.keySet()) {
			switch(key) {  //TODO: add support for the other possible parameters (don't do working directory, though
				case "politenessDelay":    	this.setPolitenessDelay(obj.getInt("politenessDelay"));			break;
				case "maxDepthOfCrawling": 	this.setMaxDepthOfCrawling(obj.getInt("maxDepthOfCrawling"));	break;
				case "maxPagesToFetch": 	this.setMaxPagesToFetch(obj.getInt("maxPagesToFetch"));			break;
				case "maxDownloadSize": 	this.setMaxDownloadSize(obj.getInt("maxDownloadSize")); 		break;
				case "userAgentString": 	this.setUserAgentString(obj.getString("userAgentString")); 		break;
				case "respectRobotsTxt": 	this.setRespectRobotsTxtFile(obj.getBoolean("respectRobotsTxt")); break;
				case "numCrawlingThreads": 	this.setNumCrawlingThreads(obj.getInt("numCrawlingThreads"));     break;
				case "includeBinaryContentInCrawling": this.setIncludeBinaryContentInCrawling(obj.getBoolean("includeBinaryContentInCrawling")); break;
				case "headers":  JSONArray jaHeaders = obj.getJSONArray("headers");
							     ArrayList<Header> headers = new ArrayList<Header>();
							     for (int i=0; i < jaHeaders.length(); i++) {
							    	 String header = jaHeaders.getString(i);
							    	 String[] parts = header.split(":");
							    	 headers.add(new BasicHeader(parts[0].trim(), parts[1].trim()));
							     }
							     this.setDefaultHeaders(headers);
							     break;
				case "authenticationBasic":  this.configureBasicAuthentication(allowAuthentication, obj.getJSONObject("authenticationBasic")); break;
				case "authenticationForm":   this.configureFormAuthentication (allowAuthentication, obj.getJSONObject("authenticationForm"));  break;
				case "authenticationNTLM":   this.configureNTLMAuthentication (allowAuthentication, obj.getJSONObject("authenticationNTLM"));  break;
				
				default:
					logger.log(Level.WARNING, "Unknown configuration parameter: "+key);
					break;
			}
		}

	}
	

	private void configureNTLMAuthentication(boolean allowAuthentication, JSONObject jsonObject) {
		if (!allowAuthentication) {
			logger.log(Level.WARNING, "Ignoring authentication for job - domain does not allow accessing authenticated sites");
		}
		String userName = jsonObject.getString("username");
		String encryptedPassword = jsonObject.getString("password");
		String decryptedPassword = Collector.getTheCollecter().decryptValue(encryptedPassword);
		String loginURL = jsonObject.getString("loginURL");
		String domain = jsonObject.getString("domain");
		
		try {
			AuthInfo ai = new NtAuthInfo(userName, decryptedPassword, loginURL, domain);
			this.addAuthInfo(ai);
		}
		catch (MalformedURLException e) {
			logger.log(Level.WARNING, "Invalid URL specified: "+loginURL,e);
		}
		
	}

	private void configureFormAuthentication(boolean allowAuthentication, JSONObject jsonObject) {
		if (!allowAuthentication) {
			logger.log(Level.WARNING, "Ignoring authentication for job - domain does not allow accessing authenticated sites");
		}
		String userName = jsonObject.getString("username");
		String encryptedPassword = jsonObject.getString("password");
		String decryptedPassword = Collector.getTheCollecter().decryptValue(encryptedPassword);
		String loginURL = jsonObject.getString("loginURL");
		String userFieldname = jsonObject.getString("userFieldName");
		String passwordFieldname = jsonObject.getString("passwordFieldName");
		
		try {	
			FormAuthInfo ai = new FormAuthInfo(userName, decryptedPassword, loginURL, userFieldname, passwordFieldname );
			
			if (jsonObject.has("additionalFormData")) {
				JSONArray formDataArray = jsonObject.getJSONArray("additionalFormData");
				for (int i=0;i< formDataArray.length(); i++) {
					JSONObject data = formDataArray.getJSONObject(i);
					
					ai.addFormParameter(data.getString("fieldName"), data.getString("fieldValue"));
				}
			}
			
			this.addAuthInfo(ai);
		}
		catch (MalformedURLException e) {
			logger.log(Level.WARNING, "Invalid URL specified: "+loginURL,e);
		}
		
	}

	private void configureBasicAuthentication(boolean allowAuthentication, JSONObject jsonObject) {
		if (!allowAuthentication) {
			logger.log(Level.WARNING, "Ignoring authentication for job - domain does not allow accessing authenticated sites");
		}

		String userName = jsonObject.getString("username");
		String encryptedPassword = jsonObject.getString("password");
		String decryptedPassword = Collector.getTheCollecter().decryptValue(encryptedPassword);
		String loginURL = jsonObject.getString("loginURL");
		
		try {
			AuthInfo ai = new BasicAuthInfo(userName, decryptedPassword, loginURL );
			this.addAuthInfo(ai);
		}
		catch (MalformedURLException e) {
			logger.log(Level.WARNING, "Invalid URL specified: "+loginURL,e);
		}
	}
}
