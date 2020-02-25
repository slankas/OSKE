package edu.ncsu.las.api;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;

import edu.ncsu.las.model.nlp.Document;

/**
 * Starts up the micro-service to provide an API service around the Stanford CORE NLP
 */
public class StanfordCoreNLPMain {
	private static Logger logger = Logger.getLogger(StanfordCoreNLPMain.class.getName());
	
	public static HttpServer startServer(String serverURI) {
		// create a resource config that scans for JAX-RS resources and
		// providers
		// in edu.ncsu.las.api package
		final ResourceConfig rc = new ResourceConfig().packages("edu.ncsu.las.api");

		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(serverURI), rc);
	}

	public static void main(String[] args) throws IOException {

		String serviceURI = System.getenv("SERVICE_URL"); 
		if (serviceURI == null) {
			serviceURI="http://0.0.0.0:9001/nlp/";
		}
		/*
		String configurationPath =  System.getenv("SERVICE_CONFIGURATION_FILE");
		
	  	if (args.length == 1) {
	  		configurationPath = args[0];
	  	}
	  	*/

	  	StanfordCoreNLPMain.startServer(serviceURI); 
	  	logger.log(Level.INFO, String.format("Stanford CoreNLP REST service started with WADL available at %sapplication.wadl", serviceURI));
	  	logger.log(Level.INFO, "Initiating call to pre-load stanford NLP libraries...");
	  	String text = "The government has directed significant resources towards cyber security.  In 2018, the federal government announced a new strategy to fight election fraud.";
		Document myDoc = Document.parse(text,2, new JSONArray(), new JSONArray());
		System.out.println(myDoc.toJSONObject().toString(4));
		logger.log(Level.INFO, "Server ready.");
	}
}
