package edu.ncsu.las.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.jar.Manifest;
import java.util.logging.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;


import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.JobCollector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.persist.DBConstants;
import edu.ncsu.las.persist.DataSourceManager;
import edu.ncsu.las.util.crypto.AESEncryption;

/**
 * Servlet implementation class SystemInitServlet
 */
public class SystemInitServlet extends HttpServlet {
	private static Logger logger =Logger.getLogger(SystemInitServlet.class.getName());
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SystemInitServlet() {
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		try {
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
			
			logger.setLevel(Level.ALL);  //These two lines should be handled by configuration files 
			
			InitialContext ic = new InitialContext();
			String configurationDirectory = (String) ic.lookup("java:comp/env/collectorConfigurationDirectory");
			
			logger.log(Level.INFO, "Application Started");
			logger.log(Level.INFO, "Configuration directory: "+ configurationDirectory);
			logger.log(Level.INFO, "Testing encryption policy");
			if (AESEncryption.hasUnlimitedStrengthPolicy() == false) {
				logger.log(Level.SEVERE, "Collector web application halting: Unlimited Strength Jurisdiction Policy Files are not installed for Java.");
				throw new Exception("Unlimited Strength Jurisdiction Policy Files are not installed for Java");
			}
			logger.log(Level.INFO, "Encryption policy based, initializing application");
			Collector.initializeCollector(configurationDirectory,"system_properties.json",true,true,true);
			
			JobCollector jc = JobCollector.getTheCollecter();
			jc.startInteractiveServices();
			
			String ldapProviderURL = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.LDAP_URL);
			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, ldapProviderURL);
			env.put("com.sun.jndi.ldap.connect.pool", "true");
			DataSourceManager.getTheDataSourceManager().addLDAPEnvironments(DBConstants.PERSON_LDAP, env);
		}
		catch (Throwable ne) {
			logger.log(Level.SEVERE, "Application start exception: ",ne);
			System.err.println("Application start exception: "+ne);
		}		
	}

	
	private static String _webApplicationTimeStamp = null;
	public static String getWebApplicationBuildTimestamp(javax.servlet.ServletContext application) {
		if (_webApplicationTimeStamp == null) {
			try {
				InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(inputStream);
				_webApplicationTimeStamp = manifest.getMainAttributes().getValue("buildTimeStamp");
			}
			catch (IOException ex) {
				logger.log(Level.SEVERE, "Unable to get build timestamp", ex);
				_webApplicationTimeStamp = "UNAVAILABLE";
			}
		}
		return _webApplicationTimeStamp;
	}
	
	
}
