package edu.ncsu.las.api;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;


import edu.ncsu.las.persist.DBConstants;
import edu.ncsu.las.persist.DBInitializer;
import edu.ncsu.las.persist.TestDAO;

/**
 * Starts up the micro-service to provide a way to extract content from HTML pages.
 * The content to be extracted must be defined within ... 
 * 
 * To call this program, the following environment vairables need to be set:
 * 		SERVICE_URL
 *      SERVICE_DATABASE  - JSON Object that contains the configuration to establish the connection pool
 *                          The format of the JSON object needs to be 
                            {
								"datasourcename" : "CollectorDataSource",
								"driver"         : "org.postgresql.Driver",
								"url"            : "jdbc:postgresql://serverName:5432/databaseName",
								"user"           : "userName",
								"password"       : "password",
								"maxconnections" : 10
	                        }
 * compressed: {"datasourcename":"CollectorDataSource","driver":"org.postgresql.Driver","url":"jdbc:postgresql://serverName:5432/databaseName","user":"userName","password":"password","maxconnections":10}
 * 
 */
public class StructuralExtractorMain {
	private static Logger logger = Logger.getLogger(StructuralExtractorMain.class.getName());
	
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
		String databaseProperties = System.getenv("SERVICE_DATABASE");
		String userAgent  = System.getenv("SERVICE_USER_AGENT");
		
		JSONObject databaseObj = new JSONObject(databaseProperties);
		
		DBInitializer.initialize(databaseObj, DBConstants.CONNECTION_AW);
		TestDAO td = new TestDAO();
		logger.log(Level.INFO,"Database status: "+td.getPostgreSQLVersion());
		
		StructuralExtractorREST.setUserAgent(userAgent);
		
		/*
		String configurationPath =  System.getenv("SERVICE_CONFIGURATION_FILE");
		
	  	if (args.length == 1) {
	  		configurationPath = args[0];
	  	}
	  	*/

	  	StructuralExtractorMain.startServer(serviceURI); 
	  	logger.log(Level.INFO, String.format("Structural Extractor REST service started with WADL available at %sapplication.wadl", serviceURI));
	}
}
