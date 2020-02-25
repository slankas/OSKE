package edu.ncsu.las.api;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import edu.ncsu.las.time.model.HeidelTime;


/**
 * Starts up the micro-service to tag text with temporal taggings
 * 
 */
public class HeidelTimeMain {
	private static Logger logger = Logger.getLogger(HeidelTimeMain.class.getName());
	
	public static HttpServer startServer(String serverURI) {
		// create a resource config that scans for JAX-RS resources and
		// providers
		// in edu.ncsu.las.graph.api package
		final ResourceConfig rc = new ResourceConfig().packages("edu.ncsu.las.api");

		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(serverURI), rc);
	}

	public static void main(String[] args) throws IOException {

		String serviceURI = System.getenv("SERVICE_URL"); 
		String configurationPath =  System.getenv("SERVICE_CONFIGURATION_FILE");
		
	  	if (args.length == 1) {
	  		configurationPath = args[0];
	  	}
	  	
	  	HeidelTime.initialize(configurationPath);
	  	HeidelTimeMain.startServer(serviceURI); 
	  	logger.log(Level.INFO, String.format("TemporalTagger REST service started with WADL available at %sapplication.wadl", serviceURI));
	}
}
