package edu.ncsu.las.webapp;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ldap.LdapName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * 
 * for client-side authentication, it is assumed that the user's email ID is the same value as the CN
 *
 */
public class Authentication {
	private static Logger logger = Logger.getLogger(Authentication.class.getName());
	
	public static User getUser(HttpServletRequest request) {
		HttpSession httpSession = request.getSession();
		
		if (httpSession.getAttribute("user") == null) {
			
			String method = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_METHOD);
			if (method != null && method.equalsIgnoreCase("oauth2")) {
				//handled in a post method to user controller
			}
			else if (method != null && method.equalsIgnoreCase("singleuser")) {
				User u = User.findUser("testuser@ncsu.edu");
				if (u != null) {
					httpSession.setAttribute("user", u);
				}
			}
			else if (method.equalsIgnoreCase("local") || method.equalsIgnoreCase("ldap")) {
				return null;
			}
			else if (method.equalsIgnoreCase("x509")) {
				try {
					X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
				    if (null != certs && certs.length > 0) {
				    	X509Certificate cert = certs[0];
				    	String dn = cert.getSubjectX500Principal().getName();
				    	LdapName ldapDN = new LdapName(dn);	
				    	String cn  = (String) ldapDN.getRdns().stream().filter(rdn -> rdn.getType().equalsIgnoreCase("CN")).findFirst().get().getValue();
				    	User u = User.findUser(cn);
						if (u != null) {
							httpSession.setAttribute("user", u);
						}
						else {
							logger.log(Level.WARNING, "User record not found from X509 Certificate/CN: " +cn);
						}
				    }
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Unable to validate user via certificates: " +e);
				}
			}
			else {
				JSONArray headers = Configuration.getConfigurationPropertyAsArray(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_HEADER);
				for (int i=0; i < headers.length(); i++) {
					String possibleUserEmailID = request.getHeader(headers.getString(i));
					if (possibleUserEmailID !=null) {
						User u = User.findUser(possibleUserEmailID.toLowerCase());
						if (u != null) {
							httpSession.setAttribute("user", u);
							break;
						}
					}
				}
			}
		}
		
		return (User) httpSession.getAttribute("user");
		
	}	
	
	public static User getUser(HttpServletRequest request, String googleToken) {
		HttpSession httpSession = request.getSession();
		
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
			    .setAudience(Collections.singletonList(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_OAUTH_CLIENTID)))
			    .build();

		try {
			GoogleIdToken idToken = verifier.verify(googleToken);
			if (idToken != null) {
				Payload payload = idToken.getPayload();
				String email = payload.getEmail();

				User u = User.findUser(email.toLowerCase());
				if (u != null) {
					httpSession.setAttribute("user", u);
					return u;
				}
				logger.log(Level.INFO,	"Authentication: user not found: "+email);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING,	"Authentication: Exception received validating via google oauth2: "+ e.toString());
		} 
		logger.log(Level.INFO,	"Authentication: token not validated: "+googleToken);
		return null;
	}
	
}
