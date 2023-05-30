package edu.ncsu.las.geo.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Provides a simple interface for an external system to send a
 * location string to query and then return back the latitude and longitude
 * coordinates.
 * 
 */
@Path("v1")
public class RestAPI {
	
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
    @Path("/geoCode")
    @Produces(MediaType.APPLICATION_JSON)
    public String processLocation(@QueryParam("location") String location) {
    	return Main._theGeoManager.geocodeLocation(location).toString();
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
    	return Main._theGeoManager.generateStatistics().toString();
    }  
    
}
