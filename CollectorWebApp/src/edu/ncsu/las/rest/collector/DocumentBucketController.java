package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.DocumentBucket;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.storage.ElasticSearchCollectionQuery;
import edu.ncsu.las.storage.ElasticSearchREST;


/**
 * Returns back data user collections
 * 
 */
@Controller

@RequestMapping(value = "rest/{domain}/documentbucket")
public class DocumentBucketController extends AbstractRESTController {

    private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

    @RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody
    String getDocumentBuckets(HttpServletRequest httpRequest, HttpServletRequest request, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.getDocumentBuckets", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);

        User u = this.getUser(httpRequest);
        logger.log(Level.FINER, "Get document buckets for " + u.getEmailID());

        JSONArray results = new JSONArray();
        List<DocumentBucket> collections = DocumentBucket.getAvailableCollections(domainStr, u.getEmailID());
        for (DocumentBucket dc: collections) {
        	results.put(dc.toJSONObject());
        }
        logger.log(Level.FINEST, "Collections : " + collections.toString());

        return results.toString();
    }

    @RequestMapping(value = "/{collectionID}/document/{documentID}", headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody
    String addDocumentToBucket(HttpServletRequest httpRequest,
                                   @PathVariable("documentID") String documentID, @PathVariable("collectionID") UUID collectionID,
                                   @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.addDocumentToBucket", new JSONObject().put("documentBucketID", collectionID).put("documentID", documentID),
        		           System.currentTimeMillis(), null, httpRequest,domainStr);
        
        //validate user can access collection
        DocumentBucket collection;
        if (Configuration.getConfigurationPropertyAsBoolean(domainStr, ConfigurationType.DOMAIN_COLLECTION_ACCESS_REQUIRED)) {
        	collection = DocumentBucket.retrieve(collectionID, this.getUser(httpRequest).getEmailID());
        	if (collection == null) {
        		return new JSONObject().put("status", "failed").put("message", "no collection access").toString();
        	}
        }
        else {
        	collection = DocumentBucket.retrieve(collectionID);
        	if (collection == null) {
        		return new JSONObject().put("status", "failed").put("message", "invalid collection").toString();
        	}
        }
        
        boolean success = ElasticSearchCollectionQuery.addCollectionToDocument(domainStr, collectionID.toString(), documentID, this.getUser(httpRequest).getEmailID());

        logger.log(Level.FINER, "Adding collection " + collectionID + " to document " + documentID + " : " + success);

        if (success) {
            return new JSONObject().put("status", "success").toString();
        } else {
            return new JSONObject().put("status", "failed").toString();
        }
    }

    @RequestMapping(value = "/{collectionID}/document/{documentID}", headers = "Accept=application/json", method = RequestMethod.DELETE)
    public @ResponseBody
    String removeDocumentFromBucket(HttpServletRequest httpRequest,
                                        @PathVariable("documentID") String documentID,
                                        @PathVariable("collectionID") UUID collectionID,
                                        @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.removeDocumentFromBucket", new JSONObject().put("documentBucketID", collectionID).put("documentID", documentID),
		           System.currentTimeMillis(), null, httpRequest,domainStr);

        //validate user can access collection       
        DocumentBucket collection;
        if (Configuration.getConfigurationPropertyAsBoolean(domainStr, ConfigurationType.DOMAIN_COLLECTION_ACCESS_REQUIRED)) {
        	collection = DocumentBucket.retrieve(collectionID, this.getUser(httpRequest).getEmailID());
        	if (collection == null) {
        		return new JSONObject().put("status", "failed").put("message", "no collection access").toString();
        	}
        }
        else {
        	collection = DocumentBucket.retrieve(collectionID);
        	if (collection == null) {
        		return new JSONObject().put("status", "failed").put("message", "invalid collection").toString();
        	}
        }
                
        boolean success = ElasticSearchCollectionQuery.removeCollectionFromDocument(domainStr, collectionID.toString(), documentID);

        logger.log(Level.INFO, "Removing collection " + collectionID + " from document " + documentID + " : " + success);

        if (success) {
            return new JSONObject().put("status", "success").toString();
        } else {
            return new JSONObject().put("status", "failed").toString();
        }
    }


    @RequestMapping(value = "/{documentBucketID}", headers = "Accept=application/json", method = RequestMethod.DELETE)
    public @ResponseBody String deleteDocumentBucket(HttpServletRequest httpRequest,
                            @PathVariable("documentBucketID") UUID collectionID,
                            @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.INFO, "Deleting collection " + collectionID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);

        long startTime = System.currentTimeMillis();
        
        java.util.List<JSONObject> documents = ElasticSearchCollectionQuery.getDocumentsInCollection(domainStr, collectionID.toString());

        for (JSONObject o : documents) {
            if (ElasticSearchCollectionQuery.removeCollectionFromDocument(domainStr, collectionID.toString(), o.getString("source_uuid")) == false) {
            	JSONObject result = new JSONObject().put("status", "failed").put("message", "unable to remove collection from documents");
            	return result.toString();
            }
        }

        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.deleteDocumentBucket", new JSONObject().put("documentBucketID", collectionID).put("numDocuments", documents.size()),
		          startTime, System.currentTimeMillis(),  httpRequest,domainStr);

        //Delete document bucket record
        if (DocumentBucket.delete(collectionID)) {
        	return "{ success: true }";
        }
        else { return "{ success: false }"; }
    }

    @RequestMapping(value = "/{documentBucketID}", headers = "Accept=application/json", method = RequestMethod.GET)
    public @ResponseBody
    String getDocumentBucket(HttpServletRequest httpRequest,
                         @PathVariable("documentBucketID") UUID collectionID,
                         @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.INFO, "Getting collection " + collectionID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.getDocumentBucket", new JSONObject().put("documentBucketID", collectionID),
        		           System.currentTimeMillis(), null, httpRequest,domainStr);
        
        DocumentBucket result = DocumentBucket.retrieve(collectionID);
        return result.toJSONObject().toString();
    }

    @RequestMapping(value = "/documents", headers = "Accept=application/json", method = RequestMethod.POST)
    public @ResponseBody
    byte[] retrieveDocumentsForDocumentBucketByQuery(HttpServletRequest httpRequest, @RequestBody String query,
                                          @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        //Note: need the query string so we can do paging ...
        logger.log(Level.INFO, "Get documents for collection " + query);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        long startTime = System.currentTimeMillis();
        
        JSONObject queryObject = new JSONObject(query);

        String searchResult = ElasticSearchREST.queryFullTextSearch(domainStr, FileStorageAreaType.REGULAR, queryObject);
        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.retrieveDocumentsForDocumentBucketByQuery", queryObject,
        		startTime,System.currentTimeMillis(), httpRequest,domainStr);
        //logger.log(Level.INFO,"Documents: " + searchResult);

        return searchResult.getBytes(StandardCharsets.UTF_8);
    }

    /*
	 * Checks if a valid email is an analyst within the specified domain
	 */
    @RequestMapping(value = "participant", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
    public @ResponseBody
    String checkPartipantIsAnAnalyst(HttpServletRequest httpRequest, @RequestParam(value = "emailid", required = true) String emailID, @PathVariable("domain") String domainStr)
            throws IOException, ValidationException {
        logger.log(Level.INFO, "Received request to validate email: " + emailID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);

        Integer index1 = emailID.indexOf('(');
        Integer index2 = emailID.indexOf(')');
        if (index1 > 0 && index2 > 0) {
			emailID = emailID.substring(index1 + 1, index2).trim();
        }
        logger.log(Level.INFO, "email: " + emailID);
        
        User u = User.findUser(emailID,RoleType.ANALYST,domainStr);


        String email = null;
        String name = null;

        if (u != null) {
            email = u.getEmailID();
            name =  u.getName();
        }

        JSONObject personJson = new JSONObject();
        personJson.put("email", email);
        personJson.put("name", name);

        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.checkPartipantIsAnAnalyst", personJson, System.currentTimeMillis(), null, httpRequest,domainStr);
        
        
        return personJson.toString();
    }

    @RequestMapping(value = "/{collectionID}", method = RequestMethod.PUT)
    public @ResponseBody String editCollection(HttpServletRequest request, HttpServletResponse response, @RequestBody String data, 
    		                   @PathVariable("domain") String domainStr,
    		                   @PathVariable("collectionID") UUID collectionID) throws ValidationException, IOException {

        logger.log(Level.FINER, "Edit collection ");
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        User u = this.getUser(request);
        
        JSONObject collection= new JSONObject(data);
        DocumentBucket dc = new DocumentBucket(collection, collectionID, u.getEmailID(), domainStr);
        
        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.editCollection", collection, System.currentTimeMillis(), null, request,domainStr);
       
        java.util.ArrayList<String> errors = dc.validate();
        if (errors.size() > 0) {
            JSONObject errorResult = new JSONObject().put("status", "failed")
            		                                 .put("message", errors);
            return errorResult.toString();
        }

        if (dc.update()) {
            JSONObject result = new JSONObject().put("status", "success")
                                                .put("collectionId", dc.getID().toString());
            return result.toString();
        }
        else {
            errors.add("Unable to update collection");
            JSONObject result = new JSONObject().put("status", "failed")
                                                .put("message", errors);
            return result.toString();
        }
    }


    @RequestMapping(consumes = "application/json", method = RequestMethod.POST)
    public @ResponseBody String createCollection(HttpServletRequest request,
                            @PathVariable("domain") String domainStr, @RequestBody String bodyStr)     throws ValidationException, IOException {

        logger.log(Level.FINER, "Add collection via API");

        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        User u = this.getUser(request);

        JSONObject collectionObject = new JSONObject(bodyStr);
        
        this.instrumentAPI("edu.ncsu.las.rest.collector.DocumentBucketController.editCollection", collectionObject, System.currentTimeMillis(), null, request,domainStr);

        
        DocumentBucket dc = new DocumentBucket(collectionObject, null, u.getEmailID(), domainStr);
        java.util.ArrayList<String> errors = dc.validate();
        if (errors.size() > 0) {
            JSONObject errorResult = new JSONObject().put("status", "failed")
            		                                 .put("message", errors);
            return errorResult.toString();
        }

        if (dc.create()) {
            JSONObject result = new JSONObject().put("status", "success")
                                                .put("collectionId", dc.getID().toString());
            return result.toString();
        }
        else {
            errors.add("Unable to create collection");
            JSONObject result = new JSONObject().put("status", "failed")
                                                .put("message", errors);
            return result.toString();
        }
    }

}