package edu.ncsu.las.image.api;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import com.drew.imaging.ImageProcessingException;

import edu.ncsu.las.image.model.EXIFExtractor;


/**
 * Provides a simple interface for an external system to upload an image file, 
 * receiving back a JSONObject of the EXIF meta-data within that image.
 */
@Path("v1")
public class RestAPI {
	
	public static final String FUNCTION_NAME = "exifExtractor";
	public static final String VERSION       = "20170227";

   
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     * 
     * If an error occurs, the response will have a "status" field with "error"
     * and another field "error" containing the issue.
     *
     * @return String that represents the JSONObject containing the metadata
     */
    @POST
    @Path("/extract")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String processImage(@FormDataParam("file") InputStream uploadedInputStream,
    		                   @FormDataParam("file") FormDataContentDisposition fileDetail) {
    	
    	try {
    		JSONObject result = EXIFExtractor.produceEXIF(uploadedInputStream);
    		result.put("processTicket", new JSONObject().put("function", FUNCTION_NAME)
    				                                    .put("version", VERSION)
    				                                    .put("timestamp", Instant.now().toString())
    				                                    .put("parameters", new JSONObject()));
    		return result.toString();
		} catch (ImageProcessingException | IOException e) {
			JSONObject errorResult = new JSONObject().put("status", "error")
					                                 .put("error", e.toString());
			return errorResult.toString();
		}
    	
    }  
    
    
}
