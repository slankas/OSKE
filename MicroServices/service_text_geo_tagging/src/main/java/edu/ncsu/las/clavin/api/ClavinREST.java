package edu.ncsu.las.clavin.api;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;



import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.ResolvedLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.ncsu.las.geo.model.Clavin;


@Path("v1")
public class ClavinREST {

	private static long minimumResponseTimeMS = Long.MAX_VALUE;
	private static long maximumResponseTimeMS = Long.MIN_VALUE;
	private static long lastResponseTimeMS = -1;
	private static long totalResponseTimeMS   = 0;
	private static long totalRequestCount = 0;

	private static Semaphore semaphore = new Semaphore(5,true);  // limit the # of threads that can be parsing sentences concurrently to just 5.
	
	
	private static Logger logger =Logger.getLogger(ClavinREST.class.getName());
		
	/*
	 Takes ann input json document extracts the text and processes it to get geotagged locations in following format:
	 	
	 	location:{					// Original location name found in text
	 	textposition: ""			// The actual position of location in text
	 	geoname:""					// geographic entity resolved from the location name
	 	confidence:""				// Confidence score for resolution
	 	fuzzy:""					// Was this a fuzzy Match
	 	matchedName:""				// name from the gazetteer record that the inputName was matched against
	 	}
	 	
	 */
	@POST
	@Path("/process")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String processDocument(String document) throws JsonProcessingException {
		JsonNode  result;
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			long startTime = System.currentTimeMillis();
			semaphore.acquire();
			//long acquireTime = System.currentTimeMillis();

			

			JsonNode jsonDocument = mapper.readTree(document);	
			
			String text = jsonDocument.get("text").asText();	
			
			Clavin clavin = Clavin.getTheClavinPool().borrowObject();
			List<ResolvedLocation> resolvedLocations = clavin.parseArticle(text);
			Clavin.getTheClavinPool().returnObject(clavin);
			
			//JsonArrayBuilder locations = Json.createArrayBuilder();
			ArrayNode locations = mapper.createArrayNode();
			
			for (ResolvedLocation resolvedLocation : resolvedLocations) {
				GeoName gn = resolvedLocation.getGeoname();
				//JsonObject personObject = Json.createObjectBuilder()
				JsonNode details = mapper.createObjectNode()
						                 .put("textPosition", resolvedLocation.getLocation().getPosition())
						                 .put("textMatched", resolvedLocation.getLocation().getText()) 
						                 .put("matchedname", resolvedLocation.getMatchedName())
						                 .put("fuzzy", resolvedLocation.isFuzzy())
						                 .put("confidence",resolvedLocation.getConfidence())
						                 .set("geoData", mapper.createObjectNode().put("geoNameID",gn.getGeonameID())
						                            		                       .put("timezone", gn.getTimezone().getID())
						                            		                       .put("latitude", gn.getLatitude())
						                            		                       .put("longitude", gn.getLongitude())
						                            		                       .put("preferredName", gn.getPreferredName())
						                            		                       .put("elevation", gn.getElevation())
						                            		                       .put("parentGeoNameID", gn.getParentId())
						                            		                       .put("population", gn.getPopulation())
						                            		                       .put("primaryCountryCode", gn.getPrimaryCountryCode().name)
						                            		                       .put("primaryCountryName", gn.getPrimaryCountryName())					                              
						                             );
				locations.add(details);
			}


			long processingTime = System.currentTimeMillis() - startTime;

			synchronized (ClavinREST.class) {  // an instance is create per request so we need to synchronize on a static
				minimumResponseTimeMS = Math.min(processingTime, minimumResponseTimeMS);
				maximumResponseTimeMS = Math.max(processingTime, maximumResponseTimeMS);
				totalResponseTimeMS  += processingTime;
				lastResponseTimeMS    = processingTime;
				totalRequestCount++;
			}
			result = mapper.createObjectNode().put("status", "success")
					                          .set("result", locations);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to process request", e);
			result = mapper.createObjectNode().put("message", e.toString())
					                           .put("status", "error");
		}
		finally {
			semaphore.release();
		}
		return mapper.writeValueAsString(result);
	}
	
    /**
     * Produces monitoring statistics
     * @throws JsonProcessingException 
     */
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public String processStatistics() throws JsonProcessingException {
    	Runtime runtime = Runtime.getRuntime();
    	ObjectMapper mapper = new ObjectMapper();
    	
    	JsonNode vmStats = mapper.createObjectNode().put("usedMemory",  (runtime.totalMemory() - runtime.freeMemory()))
    			                                    .put("freeMemory",  runtime.freeMemory())
    			                                    .put("totalMemory", runtime.totalMemory())
    			                                    .put("maxMemory",   runtime.maxMemory());

    	double averageTime = ((double) totalResponseTimeMS)/  Math.max((double)totalRequestCount, 1.0);
    	JsonNode processStats = mapper.createObjectNode().put("minimumResponseTimeMS", minimumResponseTimeMS)
				               .put("maximumResponseTimeMS", maximumResponseTimeMS)
				               .put("lastResponseTimeMS", lastResponseTimeMS)
				               .put("totalResponseTimeMS", totalResponseTimeMS)
				               .put("averageTimeMS", averageTime)
				               .put("totalRequests", totalRequestCount);

    	JsonNode pool = mapper.createObjectNode().put("idle", Clavin.getTheClavinPool().getNumIdle())
				                          .put("active", Clavin.getTheClavinPool().getNumActive());
				
    	JsonNode result = mapper.createObjectNode().set("process", processStats);
    	((ObjectNode) result).set("memory", vmStats );
    	((ObjectNode) result).set("pool", pool);
    	
    	return mapper.writeValueAsString(result);
    }    	
	
	
}
