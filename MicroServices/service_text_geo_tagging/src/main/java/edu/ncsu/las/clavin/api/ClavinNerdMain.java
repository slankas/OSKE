package edu.ncsu.las.clavin.api;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import edu.ncsu.las.geo.model.Clavin;




public class ClavinNerdMain {
	
private static Logger logger = Logger.getLogger(ClavinREST.class.getName());
	
	public static HttpServer startServer(String serverURI) {
		// create a resource config that scans for JAX-RS resources and
		// providers
		// in edu.ncsu.las.graph.api package
		final ResourceConfig rc = new ResourceConfig().packages("edu.ncsu.las.clavin.api");
		rc.register(MultiPartFeature.class);

		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(serverURI), rc);
	}

	public static void main(String[] args) throws IOException {
		
		
		String serviceURL = System.getenv("SERVICE_URL"); 
		String configurationPath =  System.getenv("SERVICE_INDEX_DIRECTORY");
		
		if (serviceURL == null) { serviceURL  = "http://0.0.0.0:9001/geoTagger/"; }
	  	if (configurationPath == null) { configurationPath = "./IndexDirectory"; }
	  	
	  	if (args.length == 1) {
	  		configurationPath = args[0];
	  	}
	  	Clavin.initialize(configurationPath);
	  	
	  	ClavinNerdMain.startServer(serviceURL); 
	  	logger.log(Level.INFO, String.format("TemporalTagger REST service started with WADL available at %sapplication.wadl", serviceURL));
	}
	
}
