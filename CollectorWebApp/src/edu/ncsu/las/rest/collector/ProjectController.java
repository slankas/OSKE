package edu.ncsu.las.rest.collector;

import java.io.IOException;
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
import edu.ncsu.las.model.collector.Project;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.RoleType;


/**
 * Provides basic CRUD capabilities for projects
 * 
 */

@Controller
@RequestMapping(value = "rest/{domain}/project")
public class ProjectController extends AbstractRESTController {

    private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

    @RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
    public @ResponseBody
    String getProjects(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        logger.log(Level.FINER, "Get projects");
  
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.getProjects", new JSONObject(), System.currentTimeMillis(),null, httpRequest,domainStr);

        List<Project> projects = Project.retrieveAvailableProjects(domainStr);
        
        JSONArray results = new JSONArray();
        projects.stream().forEach( project -> results.put(project.toJSON()));

        logger.log(Level.FINEST, "Projects : " + results.toString());

        return results.toString();
    }

    @RequestMapping(value = "/{projectID}", headers = "Accept=application/json", method = RequestMethod.DELETE)
    public @ResponseBody
    String deleteProject(HttpServletRequest httpRequest,
                            @PathVariable("projectID") UUID projectID,
                            @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.FINER, "Deleting project: " + projectID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.deleteProject", new JSONObject().put("projectID", projectID), System.currentTimeMillis(),null, httpRequest,domainStr);

        if (Project.deleteProject(projectID)) {
        	return "{ \"status\" : \"success\"}";
        }
        else { return "{ \"status\" : \"failed\" }"; }
    }

    @RequestMapping(value = "/{projectID}", headers = "Accept=application/json", method = RequestMethod.GET)
    public @ResponseBody
    String getProject(HttpServletRequest httpRequest,
                         @PathVariable("projectID") UUID projectID,
                         @PathVariable("domain") String domainStr) throws IOException, ValidationException {
        logger.log(Level.FINER, "Getting project plan " + projectID);
        this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.getProject", new JSONObject().put("projectID", projectID), System.currentTimeMillis(),null, httpRequest,domainStr);

        Project result = Project.retrieve(projectID);
        return result.toJSON().toString();
    }


    @RequestMapping(value = "/{projectID}", headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody String editProject(HttpServletRequest request, HttpServletResponse response, @RequestBody String data, 
    		                   @PathVariable("domain") String domainStr,
    		                   @PathVariable("projectID") UUID projectID)
            throws ValidationException, IOException {

        logger.log(Level.FINER, "Edit project: "+projectID);
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        User u = this.getUser(request);
        
        JSONObject projectJSON = new JSONObject(data);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.editProject", new JSONObject(data).put("projectID", projectID), System.currentTimeMillis(),null, request,domainStr);
        
        projectJSON.put("id", projectID.toString());
        try {
        	Project proj = Project.create(projectJSON, domainStr, u.getUserID());
            if (proj.update()) {
            	JSONObject ancillary = proj.createAncillaryObjects(domainStr, u.getUserID());
            	
                JSONObject result = new JSONObject().put("status", "success")
                                                    .put("project", proj.toJSON())
                                                    .put("ancillary", ancillary);
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
    public @ResponseBody String createProject(HttpServletRequest request, HttpServletResponse response,
                            @PathVariable("domain") String domainStr, @RequestBody String bodyStr)     throws ValidationException, IOException {

        logger.log(Level.INFO, "create new project");

        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        
        User u = this.getUser(request);
        JSONObject projectJSON = new JSONObject(bodyStr);
        try {
        	Project proj = Project.create(projectJSON, domainStr, u.getUserID());
            if (proj.create()) {
            	JSONObject ancillary = proj.createAncillaryObjects(domainStr, u.getUserID());
            	
                this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.createProject", new JSONObject(bodyStr).put("projectID", proj.getID().toString()), System.currentTimeMillis(),null, request,domainStr);

                JSONObject result = new JSONObject().put("status", "success")
                                                    .put("project", proj.toJSON())
                                                    .put("ancillary", ancillary);
                return result.toString();
            }
            else {
                this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.createProject", new JSONObject(bodyStr).put("status", "failed"), System.currentTimeMillis(),null, request,domainStr);

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

    @RequestMapping(value = "/{projectID}/status", headers = "Accept=application/json", method = RequestMethod.PUT)
    public @ResponseBody String updateProjectStatus(HttpServletRequest request, HttpServletResponse response, @RequestBody String data, 
    		                   @PathVariable("domain") String domainStr,
    		                   @PathVariable("projectID") UUID projectID)
            throws ValidationException, IOException {

        logger.log(Level.INFO, "Edit project: "+projectID);
        this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.ProjectController.changeProjectStatus", new JSONObject(data).put("projectID", projectID), System.currentTimeMillis(),null, request,domainStr);

        User u = this.getUser(request);
        
        JSONObject statusJSON = new JSONObject(data);
        
        String newStatus = statusJSON.getString("status");
        
        if (!newStatus.equals("active") && !newStatus.equals("inactive")) {
        	throw new ValidationException("Invalid status: "+newStatus);
        }

    	if (Project.updateStatus(projectID, newStatus, u.getUserID())) {
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
   
}