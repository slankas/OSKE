package edu.ncsu.las.topicmodel.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.topicmodel.model.LDASession;
import edu.ncsu.las.topicmodel.model.LDASession.DocumentResult;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;


/**
 * Provides a simple interface for an external system to either provide a pointer 
 * to an ElasticSearch instance (service address, index, type, and associated query) to retrieve data
 * or to directly send text-based information to have topic modeling performed
 */
@Path("v1")
public class RestAPI {
	private static Logger logger =Logger.getLogger(Main.class.getName());
	
	public static final String FUNCTION_NAME = "topicModel-LDA";
	public static final String VERSION       = "20170503";
	
	private static ExecutorService executionPool = Executors.newFixedThreadPool(1);   // this allows us to only have one thread using the SparkContext at a time
	
	
	private static Map<UUID,edu.ncsu.las.topicmodel.model.LDASession > _ldaCache = ExpiringMap.builder().expiration(2, TimeUnit.MINUTES).expirationPolicy(ExpirationPolicy.ACCESSED).build();
	
	private static JSONArray toJSONArray(java.util.List<String> list) throws JSONException {
		JSONArray result = new JSONArray();
		for (String s: list) {
			result.put(s);
		}
		return result;
	}	
	
	
    /**
     *
     *
     * @return 
     */
    @POST
    @Path("/LDA")
    @Consumes(MediaType.APPLICATION_JSON) 
    @Produces(MediaType.APPLICATION_JSON)
    public String initiateLDASessin(String requestObject) {
    	logger.log(Level.FINEST, requestObject);
    	
    	java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
    	
    	JSONObject request = null;
    	try {
    		request = new JSONObject(requestObject); 
    	}
    	catch (JSONException je) {
    		errors.add("Invalid JSON Object received.");
    	}

    	edu.ncsu.las.topicmodel.model.LDASession ldaSession = new edu.ncsu.las.topicmodel.model.LDASession();
    	java.util.ArrayList<LDASession.Document> docs = new java.util.ArrayList<LDASession.Document>();

    	if (request != null ) {	    	
	    	try { ldaSession.setNumberOfTopics(request.getInt("numTopics"));            } catch (Exception e) { errors.add("numTopics not defined or not a valid positive integer.");     }
	    	try { ldaSession.setNumberOfKeywords(request.getInt("numKeywords"));        } catch (Exception e) { errors.add("numKeywords not defined or not a valid positive integer.");   }
	    	try { ldaSession.setMaxNumberOfIterations(request.getInt("maxIterations")); } catch (Exception e) { errors.add("maxIterations not defined or not a valid positive integer."); }
	    	try { ldaSession.setStemWords(request.getBoolean("stemWords"));             } catch (Exception e) { errors.add("stemKeywords not defined as a valid boolean value.");         }
	    	
	    	try {
		    	JSONArray sentDocs = request.getJSONArray("documents");
		    	for (int i=0; i < sentDocs.length(); i++) {
		    		JSONObject sentDoc = sentDocs.getJSONObject(i);
		    		LDASession.Document doc = new LDASession.Document(sentDoc.getString("uuid"), sentDoc.getString("url"), sentDoc.getString("text"));
		    		docs.add(doc);
		    	}
	    	}
	    	catch (Exception e) {
	    		errors.add("Invalid document specification.");
	    	}
    	}
    	
    	if (errors.size() > 0) {
        	JSONObject result = new JSONObject().put("status", "failure")
        			                            .put("errors", RestAPI.toJSONArray(errors))
        			                            .put("format", new JSONObject("{\"numKeywords\": 10,\t\"maxIterations\": 50, \"stemWords\": false, \"numTopics\": 2,\"documents\":[{\"text\":\"some text.\",\"uuid\":\"uuid\",\"url\":\"http://someURL.com\"}]}"));
        	return result.toString();
    	}
    	ldaSession.setDocuments(docs);
    	
    	UUID sessionID = UUID.randomUUID();
    	_ldaCache.put(sessionID, ldaSession);
    	executionPool.submit(ldaSession);
    	
    	JSONObject result = new JSONObject().put("status", "success")
    			                            .put("message", "LDA initiated")
    			                            .put("sessionUUID", sessionID );
    	
		result.put("processTicket", this.createProcessTicket(ldaSession)); 
		return result.toString();
    }  
    
    
   /**
    * Based upon the current session ID, checks to see 
    * 1) does the session exist
    * 2) if it does exist, what is it's current state?
    * 3) if the state is completed, returns the topics and associated documents
    *
    * @return 
    */
   @GET
   @Path("/LDA/{sessionID}")
   @Produces(MediaType.APPLICATION_JSON)
   public String getLDAResults(@PathParam("sessionID") UUID sessionID) {
   	
	   LDASession ldaSession = _ldaCache.get(sessionID);
	   if (ldaSession == null) {
		   JSONObject result = new JSONObject().put("status", "failure")
		   			                            .put("errors", new JSONArray().put("Session ID does not exist."))
		   			                            .put("sessionUUID", sessionID );
		   return result.toString();
	   }
	   if (ldaSession.getLDAProcessingError() != null) {
		   JSONObject result = new JSONObject().put("status", "failure")
                      .put("errors", new JSONArray().put(ldaSession.getLDAProcessingError()))
                      .put("sessionUUID", sessionID );
		   return result.toString();
		   
	   }
   	
	   String status = ldaSession.getStatus();
	   if (status.equals("complete") == false) {
	    	JSONObject result = new JSONObject().put("status", "success")
                    .put("message", "LDA status: "+status)
                    .put("sessionUUID", sessionID );
	    	return result.toString();
	   }
	   //status = complete at this point ...
	
	   try {
		   List<String>[] topic_distribution = ldaSession.getTopicDistributionMap();
		   List<DocumentResult>[] topDocs    = ldaSession.getTopDocumentsForTopics();

		   JSONArray topics = new JSONArray();
		   for (int i=0; i< topic_distribution.length; i++) {
			   JSONObject topic = new JSONObject();

			   JSONArray keywords = new JSONArray();
			   for (String keyword: topic_distribution[i]) {
				   keywords.put(keyword);
			   }
			   topic.put("keywords", keywords);

			   JSONArray documents = new JSONArray();
			   for (DocumentResult dr: topDocs[i]) {
				   documents.put(dr.toJSON());
			   }
			   topic.put("documents", documents);
			   topics.put(topic);
		   }

		   JSONObject result = new JSONObject().put("topics", topics)
				   .put("status", "success")
				   .put("message", "LDA status: "+status)
				   .put("sessionUUID", sessionID );

		   result.put("processTicket", this.createProcessTicket(ldaSession));

		   return result.toString();
	   }
	   catch (Exception e) {
		   logger.log(Level.SEVERE, "Unknown exception getting LDA status: ",e);
		   JSONObject result = new JSONObject().put("status", "failure")
                      .put("errors", new JSONArray().put(e.toString()))
                      .put("sessionUUID", sessionID );
		   return result.toString();
	   }
   }
   
   /**
    * Produces monitoring statistics
    */
   @GET
   @Path("/statistics")
   @Produces(MediaType.APPLICATION_JSON)
   public String processStatistics() {
	   	Runtime runtime = Runtime.getRuntime();
	   	JSONObject vmStats = new JSONObject().put("usedMemory",  (runtime.totalMemory() - runtime.freeMemory()))
	               .put("freeMemory",  runtime.freeMemory())
	               .put("totalMemory", runtime.totalMemory())
	               .put("maxMemory",   runtime.maxMemory());
	
	   	JSONObject result = new JSONObject().put("memory", vmStats )
	   	                                    .put("process", LDASession.getProcessStatistics());
	   	
	   	return result.toString();
   }    	
	   
   
   
   public JSONObject createProcessTicket(LDASession ldaSession) {
	   return new JSONObject().put("function", FUNCTION_NAME)
               .put("version", VERSION)
               .put("timestamp", Instant.now().toString())
               .put("parameters", new JSONObject().put("numTopics", ldaSession.getNumberOfTopics())
               		                           .put("numKeywords", ldaSession.getNumberOfKeywords())
               		                           .put("maxIterations", ldaSession.getMaxNumberOfIterations())
               		                           .put("stemKeywords", ldaSession.getStemWords())
               		                           .put("numDocumentProcessed", ldaSession.getDocumentCount()));
   }
   
}
