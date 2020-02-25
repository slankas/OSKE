package edu.ncsu.las.topicmodel.api;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;


import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initiates an HTTP server for providing a REST-based service
 * for providing topic modeling capabilities.
 *
 */
public class Main {
	private static Logger logger =Logger.getLogger(Main.class.getName());

    
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(String serverURI) {
        // create a resource config that scans for JAX-RS resources and providers
        // in edu.ncsu.las.graph.api package
        final ResourceConfig rc = new ResourceConfig().packages("edu.ncsu.las.topicmodel.api");
        
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(serverURI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
		String serverURI = "http://0.0.0.0:9001/topicmodel/";
		
		if (args.length == 0) {
	        Main.startServer(serverURI); //final HttpServer server = startServer();
	        logger.log(Level.INFO,String.format("Jersey app started with WADL available at %sapplication.wadl", serverURI));
		}
    }
}

