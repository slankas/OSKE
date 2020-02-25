package edu.ncsu.las.dictionary;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides a simple interface for an external system to send a
 * location string to query and then return back the latitude and longitude
 * coordinates.
 * 
 */
@Path("v1")
public class RestAPI {
	private static Logger logger =Logger.getLogger(RestAPI.class.getName());
	public static final String FUNCTION_NAME = "geoCoder";
	public static final String VERSION       = "20170304";

   
    /**
     * 
     * If an error occurs, the response will have a "status" field with "error"
     * and another field "error" containing the issue.
     *
     * @return String that represents the JSONObject containing the latitude and longitude
     */
    @GET
    @Path("/wordnet/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public String processLocation(@PathParam("term") String term) {
    	try {
    		return Main.accessDiectionaryManager().lookup(term).toString();
    	}
    	catch (Throwable t) {
    		logger.log(Level.WARNING, "Unable to get wordnet data: "+t.toString(),t);
    		return "";
    	}
    	//return "";
    }  
    
    /**
     * runtime statistics of this service
     *
     * @return String that represents the JSONObject containing the latitude and longitude
     */
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public String processStatistics() {
    	return Main.accessDiectionaryManager().generateStatistics().toString();
    }  
    
}
