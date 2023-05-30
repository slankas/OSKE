package edu.ncsu.las.geo.api;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.geo.model.GeoManager;

import java.io.IOException;
import java.net.URI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class
 * Sample configuration for APPLICATION_PROPERTIES

{
    "geo_api": {
        "service_url": "http://0.0.0.0:9001/geo/",
        "cacheSize": 20000,
        "maxSleepTime": 1000
    },
    "providers": [
        {
            "longitudeField": "lon",
            "requestRate": 1,
            "parentArray": "",
            "name": "openStreetMap",
            "latitudeField": "lat",
            "userAgent": "IDENTIFYING_EMAIL_ADDRESS",
            "maxPerDay": 86400,
            "restEndPoint": "http://nominatim.openstreetmap.org/search?format=json&q="
        },
        {
            "longitudeField": "lon",
            "requestRate": 1,
            "parentArray": "",
            "name": "locationIQ",
            "latitudeField": "lat",
            "userAgent": "IDENTIFYING_EMAIL_ADDRESS",
            "maxPerDay": 10000,
            "restEndPoint": "https://locationiq.org/v1/search.php?key=LOCATIONID_KEY&format=json&q="
        }
    ]
}

 *
 */
public class Main {
	private static Logger logger =Logger.getLogger(Main.class.getName());
	private static JSONObject _properties;
	
	public static GeoManager _theGeoManager;
   
	/**
	 * Loads the json configuration from a value contained in an environment variable
	 * 
	 */
	public static void initialize(String environmentVariable) {
		if (_properties != null) {
			logger.log(Level.SEVERE, "System has already been initialized", new Exception("Illegal call"));
			System.exit(-1);
		}
		
		try {
			// Load the properties file
			String content = System.getenv(environmentVariable);
			logger.log(Level.INFO, "System configuration: " +content);
			_properties = new JSONObject(content);
		}
		catch (JSONException je) {
			logger.log(Level.SEVERE, "Unable to read configuration property file - malformed json: " +je);

			System.exit(-1);
		}		
		
		// Check all required properties are present:
		boolean foundMissing=false;
		for (Configuration c: Configuration.values()) {
			if ( (c.getParentConfiguration() == null || c.getParentConfiguration().isJSONArray()==false) &&  Main.hasConfigurationProperty(c) == false) {
				logger.log(Level.SEVERE, "Missing parameter - "+c.toString()+": "+c.getDescription());
				foundMissing = true;
			}
		}
		if (foundMissing) {
			logger.log(Level.SEVERE, "Exiting System - not all required properties are present.");
			System.exit(-1);
		}
		
		_theGeoManager = new GeoManager(getConfigurationPropertyAsArray(Configuration.GEO_PROVIDER), 
				                       getConfigurationPropertyAsInt(Configuration.GEO_API_CACHE_SIZE),
				                       getConfigurationPropertyAsLong(Configuration.GEO_API_MAX_SLEEP_TIME));
		
		logger.log(Level.INFO, "System initialized.");
	}    
    
	
	public static JSONObject getConfiguration() { return _properties;	}
	
	
	private static JSONObject getConfigurationObject(Configuration property){
		if (property == null) { return _properties; }
		else {
			return Main.getConfigurationObject(property.getParentConfiguration()).getJSONObject(property.toString());
		}
	}
	
	public static String getConfigurationProperty(Configuration property) {
		return Main.getConfigurationObject(property.getParentConfiguration()).optString(property.toString());	
	}
	
	public static boolean hasConfigurationProperty(Configuration property) {
		return Main.getConfigurationObject(property.getParentConfiguration()).has(property.toString());	
	}

	/**
	 * 
	 * @param property
	 * @return 0 if it doesn't exist
	 */
	public static long getConfigurationPropertyAsLong(Configuration property) {
		return Main.getConfigurationObject(property.getParentConfiguration()).optLong(property.toString());	
	}
	
	/**
	 * 
	 * @param property
	 * @return 0 if it doesn't exist
	 */
	public static int getConfigurationPropertyAsInt(Configuration property) {
		return Main.getConfigurationObject(property.getParentConfiguration()).optInt(property.toString());	
	}	
	
	/**
	 * 
	 * @param property
	 * @return 0 if it doesn't exist
	 */
	public  static boolean getConfigurationPropertyAsBoolean(Configuration property) {
		return Main.getConfigurationObject(property.getParentConfiguration()).optBoolean(property.toString());	
	}	

	/**
	 * 
	 * @param property
	 * @return 
	 */
	public static JSONArray getConfigurationPropertyAsArray(Configuration property) {
		return Main.getConfigurationObject(property.getParentConfiguration()).getJSONArray(property.toString());	
	}
	
	/**
	 * 
	 * @param property
	 * @return 
	 */
	public static java.util.Properties getConfigurationPropertyAsProperties(Configuration property) {
		JSONObject jo = Main.getConfigurationObject(property);	
		java.util.Properties prop = new java.util.Properties();
		for (String key: jo.keySet()) {
			prop.setProperty(key, jo.getString(key));
		}
		return prop;
	}			
	
	
    
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in edu.ncsu.las.graph.api package
        final ResourceConfig rc = new ResourceConfig().packages("edu.ncsu.las.geo.api");
        
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(Main.getConfigurationProperty(Configuration.GEO_API_SERVICE_URL)), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
    	
		Main.initialize("APPLICATION_PROPERTIES");
    	
		Main.startServer(); //final HttpServer server = startServer();
	    System.out.println(String.format("Jersey app started with WADL available at %sapplication.wadl", Main.getConfigurationProperty(Configuration.GEO_API_SERVICE_URL)));
    }
}

