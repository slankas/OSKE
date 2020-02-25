package edu.ncsu.las.api;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Starts up the micro-service to provide an API service to retrieve information for whois
 * 
 */
public class WhoIsMain {
	private static Logger logger = Logger.getLogger(WhoIsMain.class.getName());
	
	public static HttpServer startServer(String serverURI, String userAgent) {
		WhoIsService.setUserAgent(userAgent);
		final ResourceConfig rc = new ResourceConfig().packages("edu.ncsu.las.api");
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(serverURI), rc);
	}

	public static void main(String[] args) throws IOException {

		String serviceURI = System.getenv("SERVICE_URL");
		String userAgent  = System.getenv("SERVICE_USER_AGENT");
		
	  	WhoIsMain.startServer(serviceURI, userAgent); 
	  	
	  	logger.log(Level.INFO, String.format("SiteInfo REST service started with WADL available at %sapplication.wadl", serviceURI));
	}
}
