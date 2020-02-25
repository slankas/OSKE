package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.ProjectDocument;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.util.StringValidation;


/**
 * Provides basic CRUD capabilities for project documents (ie, scratchpads)
 * 
 */

@Controller
@RequestMapping(value = "rest/{domain}/projectdocument")
public class ProjectDocumentController extends AbstractRESTController {

    private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

    @RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody
    String getProjects(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        User u = this.getUser(httpRequest);
        logger.log(Level.FINER, "Get project documents for " + u.getEmailID());
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.getProjects", new JSONObject(), System.currentTimeMillis(), null, httpRequest,domainStr);
        
        List<ProjectDocument> projects = ProjectDocument.retrieveAvailableProjects(domainStr);
        
        JSONArray results = new JSONArray();
        projects.stream().forEach( project -> results.put(project.toJSON()));

        logger.log(Level.FINEST, "Project documents : " + results.toString());

        return results.toString();
    }


    @RequestMapping(value = "/{documentID}", headers = "Accept=application/json", method = RequestMethod.DELETE)
    public @ResponseBody String deleteProjectDocument(HttpServletRequest httpRequest,
                            @PathVariable("documentID") UUID documentID,
                            @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.INFO, "Deleting project document: " + documentID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.deleteProjectDocument", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, httpRequest,domainStr);

        if (ProjectDocument.deleteProject(documentID)) {
        	return "{ \"status\" : \"success\"}";
        }
        else { return "{ \"status\" : \"failed\" }"; }
    }

    @RequestMapping(value = "/{documentID}", headers = "Accept=application/json", method = RequestMethod.GET)
    public @ResponseBody
    String getProjectDocument(HttpServletRequest httpRequest,
                         @PathVariable("documentID") UUID documentID,
                         @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.FINER, "Getting project document " + documentID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.getProjectDocument", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, httpRequest,domainStr);
        
        ProjectDocument result = ProjectDocument.retrieve(documentID);
        if (result != null) {
        	return result.toJSON().toString();
        }
        else {
        	return "{}";
        }
    }

    @RequestMapping(value = "/{documentID}/export", method = RequestMethod.GET)
    public @ResponseBody void exportProjectDocument(HttpServletRequest httpRequest,  HttpServletResponse response,  @PathVariable("documentID") UUID documentID,  @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.FINER, "export project document " + documentID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.exportProjectDocument", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, httpRequest,domainStr);
        
        ProjectDocument result = ProjectDocument.retrieve(documentID);
        if (result != null) {
    	    String preHtml = "<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'><head><meta charset='utf-8'><title>"+result.getName()+"</title></head><body>";
    	    String postHtml = "</body></html>";
    	    String html = preHtml+ result.getContents()  +postHtml;
        	
        	
            response.setContentType("application/msword");
            response.setHeader("Content-Disposition", "attachment; filename="+result.getName()+".doc");
            response.setHeader("Content-Type","application/msword");
            
    	    OutputStream out = response.getOutputStream();
    	    out.write(html.getBytes("UTF-8"));
            out.flush();
         }
        else {
        	throw new ValidationException("Document not found");
        }
    }    
    

    @RequestMapping(value = "/{documentID}", headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody String updateProjectDocument(HttpServletRequest httpRequest, @RequestBody String data, 
    		                   @PathVariable("domain") String domainStr,
    		                   @PathVariable("documentID") UUID documentID)
            throws ValidationException, IOException {
        logger.log(Level.FINER, "Edit project document: "+documentID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.updateProjectDocument", new JSONObject().put("documentID", documentID.toString()), System.currentTimeMillis(), null, httpRequest,domainStr);

        User u = this.getUser(httpRequest);
        
        JSONObject projectDocumentJSON = new JSONObject(data);
        projectDocumentJSON.put("id", documentID.toString());
        try {
        	ProjectDocument proj = ProjectDocument.create(projectDocumentJSON, domainStr, u.getUserID());
            if (proj.update(u.getUserID())) {           	
                JSONObject result = new JSONObject().put("status", "success")
                                                    .put("document", proj.toJSON());
                return result.toString();
            }
            else {
            	JSONArray errors = new JSONArray();
                errors.put("Unable to update project");
                JSONObject result = new JSONObject().put("status", "failed")
                                                    .put("message", errors);
                return result.toString();
            }
        }
        catch (ValidationException ve) {
        	return ve.toJSONObject().toString();
        }
    }


    @RequestMapping(consumes = "application/json",  headers = "Accept=application/json", method = RequestMethod.POST)
    public @ResponseBody String createProjectDocument(HttpServletRequest httpRequest,
                            @PathVariable("domain") String domainStr, @RequestBody String bodyStr)     throws ValidationException, IOException {
        logger.log(Level.FINER, "create new project");
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);

        User u = this.getUser(httpRequest);
        JSONObject projectDocumentJSON = new JSONObject(bodyStr);
        
        try {
        	ProjectDocument proj = ProjectDocument.create(projectDocumentJSON, domainStr, u.getUserID());
            if (proj.create()) {            	
                JSONObject result = new JSONObject().put("status", "success")
                                                    .put("document", proj.toJSON());
                this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.createProjectDocument", new JSONObject().put("documentID", proj.getID().toString()), System.currentTimeMillis(), null, httpRequest,domainStr);
                return result.toString();
            }
            else {
            	JSONArray errors = new JSONArray();
                errors.put("Unable to create project");
                JSONObject result = new JSONObject().put("status", "failed")
                                                    .put("message", errors);
                return result.toString();
            }
        }
        catch (ValidationException ve) {
        	return ve.toJSONObject().toString();
        }
    }

    @RequestMapping(value = "/{documentID}/status", headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody String updateProjectDocumentStatus(HttpServletRequest httpRequest, @RequestBody String data, 
    		                   @PathVariable("domain") String domainStr,
    		                   @PathVariable("documentID") UUID documentID)
            throws ValidationException, IOException {
        logger.log(Level.INFO, "Edit project document status: "+documentID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        User u = this.getUser(httpRequest);
        
        JSONObject statusJSON = new JSONObject(data);
        
        String newStatus = statusJSON.getString("status");
        
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.updateProjectDocumentStatus", new JSONObject().put("documentID", documentID.toString()).put("status", statusJSON.getString("status")), System.currentTimeMillis(), null, httpRequest,domainStr);
        
        if (!newStatus.equals("active") && !newStatus.equals("inactive")) {
        	throw new ValidationException("Invalid status: "+newStatus);
        }

    	if (ProjectDocument.updateStatus(documentID, newStatus, u.getUserID())) {
            JSONObject result = new JSONObject().put("status", "success");
            return result.toString();
        }
        else {
        	JSONArray errors = new JSONArray();
            errors.put("Unable to update project status");
            JSONObject result = new JSONObject().put("status", "failed")
                                                .put("message", errors);
            return result.toString();
        }


    }
    
   
    @RequestMapping(value = "/{documentID}/append", headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody String appendProjectDocument(HttpServletRequest httpRequest, @RequestBody String data, 
    		                   @PathVariable("domain") String domainStr,
    		                   @PathVariable("documentID") UUID documentID)
            throws ValidationException, IOException {
        logger.log(Level.INFO, "append to project: "+documentID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        
        JSONObject dataObj = new JSONObject(data);
        String source = StringValidation.removeAllHTML(dataObj.getString("source"));
        String appendContent = dataObj.getString("content");
        appendContent = StringValidation.removeNonStandardHTMLFromString(appendContent); 
        
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectDocumentController.appendProjectDocument", new JSONObject().put("documentID", documentID.toString()).put("source", source), System.currentTimeMillis(), null, httpRequest,domainStr);
        
        ProjectDocument pd = ProjectDocument.retrieve(documentID);
        if (pd == null) {
        	throw new ValidationException("Unable to find document");
        }
        
        String newContent = pd.getContents() + "\n<div dataSource='"+source+"'>" +appendContent+"</div>\n<p><hr><p>\n&nbsp;\n";
        pd.setContents(newContent);

        if (pd.update(this.getUser(httpRequest).getUserID())) {
            JSONObject result = new JSONObject().put("status", "success");
            return result.toString();
        }
        else {
        	JSONArray errors = new JSONArray();
            errors.put("Unable to append to document");
            JSONObject result = new JSONObject().put("status", "failed")
                                                .put("message", errors);
            return result.toString();
        }


    }    
    
    
}