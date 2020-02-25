package edu.ncsu.las.source;

import edu.ncsu.las.collector.Collector;
import edu.uci.ics.crawler4j.fetcher.SniPoolingHttpClientConnectionManager;
import edu.uci.ics.crawler4j.fetcher.SniSSLConnectionSocketFactory;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.NameValuePair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Provides a mechanism to submit form-based authentication
 * data to a webserver/application and review the results.
 * 
 */
public class FormAuthenticationSupport {
	static final Logger logger =Logger.getLogger(FormAuthenticationSupport.class.getName());
	public static final String HASH_MD5_INDICATOR = "{hashMD5}";
	
	public static String getMD5HashForPassword(String password) {
		if (password.startsWith(HASH_MD5_INDICATOR)) {
			password = password.substring(HASH_MD5_INDICATOR.length());
		}
		String md5Password = org.apache.commons.codec.digest.DigestUtils.md5Hex(password);
		return md5Password;
	}
	
	/**
	 * Creates a connection manager that trusts any SSL-based site.
	 * 
	 * @return
	 */
	public static PoolingHttpClientConnectionManager createAlwaysTrustConnectionManager() {
		PoolingHttpClientConnectionManager connectionManager;
		
		RegistryBuilder<ConnectionSocketFactory> connRegistryBuilder = RegistryBuilder.create();
        connRegistryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);
        try {
        	SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(final X509Certificate[] chain, String authType) {
                            return true;
                        }
                    }).build();
            SSLConnectionSocketFactory sslsf =
                    new SniSSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
                connRegistryBuilder.register("https", sslsf);
        } catch (Exception e) {
        	logger.log(Level.WARNING,"Exception thrown while trying to register https",e);
        }
        
        Registry<ConnectionSocketFactory> connRegistry = connRegistryBuilder.build();
        connectionManager = new SniPoolingHttpClientConnectionManager(connRegistry);
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(5);
        
        return connectionManager;
	}
	
	/**
	 * Authenticates the user to the site
	 * 
	 * @return
	 */
	public JSONObject testAuthentication(String domain, JSONObject formAuthObj) {
				
		//String md5Password = org.apache.commons.codec.digest.DigestUtils.md5Hex(password);
		
		JSONObject result = new JSONObject();
		
		String userAgent = SourceHandlerInterface.getNextUserAgent(domain); 

		String userName          = formAuthObj.getString("username");
		String encryptedPassword = formAuthObj.getString("password");
		String decryptedPassword = Collector.getTheCollecter().decryptValue(encryptedPassword);
		String loginURL          = formAuthObj.getString("loginURL");
		String userFieldname     = formAuthObj.getString("userFieldName");
		String passwordFieldname = formAuthObj.getString("passwordFieldName");
		
		//formParameters
		ArrayList<NameValuePair> loginPostParameters = new ArrayList<NameValuePair>();
		loginPostParameters.add(new BasicNameValuePair(userFieldname, userName));
		loginPostParameters.add(new BasicNameValuePair(passwordFieldname, decryptedPassword));
		
		if (formAuthObj.has("additionalFormData")) {
			JSONArray formDataArray = formAuthObj.getJSONArray("additionalFormData");
			for (int i=0;i< formDataArray.length(); i++) {
				JSONObject data = formDataArray.getJSONObject(i);
				
				loginPostParameters.add(new BasicNameValuePair(data.getString("fieldName"), data.getString("fieldValue")));
			}
		}
		
        try (CloseableHttpClient httpclient = HttpClients.custom().setUserAgent(userAgent).build()) {
			HttpPost loginPost = new HttpPost(loginURL);
			loginPost.setEntity(new UrlEncodedFormEntity(loginPostParameters));

			try (CloseableHttpResponse response = httpclient.execute(loginPost)) {
				int code = response.getStatusLine().getStatusCode();
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, "UTF-8");
				
				Header[] headers = response.getAllHeaders();				
				JSONArray jaHeaders = new JSONArray();
				for (Header h: headers) {
					JSONObject headerObj = new JSONObject().put("name", h.getName()).put("value",h.getValue());
					jaHeaders.put(headerObj);
				}
				
				result.put("statusCode", code);
				result.put("content", content);
				result.put("headers", jaHeaders);
			} 
			catch (Exception e) {
				logger.log(Level.SEVERE, "authentication exception: " + e.toString());
				return null;
			} 			
        	
        }
        catch (Exception e) {
        	logger.log(Level.WARNING,"Exception during authentication test: ",e);
        }

		return result;
	}
	
	
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		logger.log(Level.INFO, "Application Started");
		logger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);		
		
		String authPropTest = "{\"additionalFormData\":[{\"fieldName\":\"cookieuser\",\"fieldValue\":\"1\"},{\"fieldName\":\"s\",\"fieldValue\":\"\"},{\"fieldName\":\"securitytoken\",\"fieldValue\":\"guest\"},{\"fieldName\":\"do\",\"fieldValue\":\"login\"},{\"fieldName\":\"vb_login_md5password\",\"fieldValue\":\"PASSWORD\"},{\"fieldName\":\"vb_login_md5password_utf\",\"fieldValue\":\"PASSWORD\"}],\"password\":\"PASSWORD\",\"loginURL\":\"https://www.tnaboard.com/login.php?do=login\",\"userFieldName\":\"vb_login_username\",\"passwordFieldName\":\"vb_login_password\",\"username\":\"USERNAME\"}";
		JSONObject authObj = new JSONObject(authPropTest);
		
		FormAuthenticationSupport at = new FormAuthenticationSupport();
		
		JSONObject result = at.testAuthentication("test", authObj);
		System.out.println(result.toString(4));
	}
	
}
