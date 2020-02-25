package edu.ncsu.las.controller;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import edu.ncsu.las.model.collector.DocumentBucket;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.RoleType;

/**
 * Handles requests for the application home page.
 */
@Controller
public class PageController extends AbstractController {
	private static Logger logger = Logger.getLogger(PageController.class.getName());

	
	/**
	 * Simply selects the home view to render by returning its name.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/")
	public String indexGET(HttpServletRequest request) throws Exception {
		logger.log(Level.INFO, "Page controller: Index Get");
		this.validateUserAndSetPageAttributes(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.home", new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);

		return "index";
	}

	/**
	 * Simply selects the home view to render by returning its name.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/{domain}", method = RequestMethod.GET)
	public String indexViaHomeGET(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		logger.log(Level.INFO, "Page controller: Index Get");
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.domainHome", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		if (domain.equals("whpubmed")) {
			return "literatureDiscoveryHome";
		}
		
		Object homeAttribute = request.getSession(true).getAttribute("home");
		if ( homeAttribute == null || !homeAttribute.toString().equals("collector")) {
			return "domainHome";			
		}
		else {
			return "domainHomeCollector";
		}
	}	
	
	/**
	 * Simply selects the home view to render by returning its name.
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "/{domain}/jobStatus", method = RequestMethod.GET)
	public String getJobStatusPage(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		logger.log(Level.INFO, "Page controller: Index Get");
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.jobStatus", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return "jobStatus";
	}		
	
	@RequestMapping(value = "/system/domains", method = RequestMethod.GET)
	public String maintainDomains(HttpServletRequest request) throws Exception {
		logger.log(Level.INFO, "Page controller: maintain domains Get");
		this.validateUserWithDomainAndSetPageAttributes(request,"system");
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.maintainDomains", new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);
		
		return this.getCheckAuthorization(request,RoleType.ADMINISTRATOR,"system","domainManage","index");
	}

	@RequestMapping(value = "/system/addEditDomain", method = RequestMethod.GET)
	public String addEditDomains(HttpServletRequest request,  @RequestParam(value = "domain", required = false)   String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,"system");
		logger.log(Level.INFO, "Page controller: Add / Edit Domain");
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.addEditDomain", new JSONObject(), System.currentTimeMillis(), null, request,domain == null ? "system" : domain);
		
		if (domain == null) {
			request.setAttribute("domain", "");
			return this.getCheckAuthorization(request,RoleType.ADMINISTRATOR,"system","domainAddEditPage","index");  // only a "system" domain administrator can create a new domain
		}
		else {
			request.setAttribute("domain", domain);
			//either need to be a system administrator or a domain administrator to edit the domain configuration.
			return  this.getCheckAuthorization(request,RoleType.ADMINISTRATOR,domain,"domainAddEditPage","index").equals("index") ? this.getCheckAuthorization(request,RoleType.ADMINISTRATOR,"system","domainAddEditPage","index")  : "domainAddEditPage";
		}
	}	
	
	@RequestMapping(value = "/{domain}/handlers", method = RequestMethod.GET)
	public String handlers(HttpServletRequest request,@PathVariable("domain") String domain) throws Exception {
		logger.log(Level.INFO, "Page controller: handlers Get");
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.handlers", new JSONObject(), System.currentTimeMillis(), null, request,domain);
		
		//return this.getCheckAuthorization(request,RoleType.ANALYST,"handlers","index");		
		// any authenticated users should be able to access
		return "handlers";
	}

	@RequestMapping(value = "/{domain}/maintainUsers", method = RequestMethod.GET)
	public String maintainUsers(HttpServletRequest request,@PathVariable("domain") String domain) throws Exception {
		logger.log(Level.INFO, "Page controller: maintainUsers Get");
		this.validateUserWithDomainAndSetPageAttributes(request, domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.maintainUsers", new JSONObject(), System.currentTimeMillis(), null, request,domain);
		
		return this.getCheckAuthorization(request,RoleType.ADMINISTRATOR,domain,"maintainUsers","index");
	}

	@RequestMapping(value = "/{domain}/jobHistory", method = RequestMethod.GET)
	public String jobHistory(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "jobId", required = false) UUID jobId) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		request.setAttribute("jobId", jobId);
		logger.log(Level.INFO, "Page controller: job history1 Get ("+jobId+")");
		
		JSONObject obj = new JSONObject();
		if (jobId != null) { obj.put("jobID", jobId.toString()); }
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.jobHistory", obj, System.currentTimeMillis(), null, request,domain);
		
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"jobHistory","index");
	}
	
	@RequestMapping(value = "/{domain}/fileUpload", method = RequestMethod.GET)
	public String fileUpload(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		logger.log(Level.INFO, "Page controller: file upload Get");
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.fileUpload", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"fileUpload","index");
	}

	@RequestMapping(value = "/{domain}/manageJobs", method = RequestMethod.GET)
	public String manageJobs(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.manageJobs", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		logger.log(Level.INFO, "Page controller: manage jobs Get");
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"manageJobs","index");
	}

	/**
	 * Allow for a complete query string to be posted to the search page
	 * 
	 * @param request
	 * @param domain
	 * @param queryString
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{domain}/search", method = RequestMethod.POST)
	public String elasticSearchPOST(HttpServletRequest request, @PathVariable("domain") String domain,  @RequestParam("searchQuery") String queryString) throws Exception {
		logger.log(Level.INFO, "Page controller: search, post query passed");
		
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		request.setAttribute("query", "");
		request.setAttribute("queryObject", new JSONObject(queryString));
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.search", new JSONObject().put("queryString",queryString), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"search","index");
	}
	
	/**
	 * Search 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{domain}/search", method = RequestMethod.GET)
	public String elasticSearchGet(HttpServletRequest request, @PathVariable("domain") String domain,  
			                        @RequestParam(value="searchQuery", defaultValue="",          required=false) String queryString) throws Exception {
		logger.log(Level.INFO, "Page controller: search, get query passed");
		
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.search", new JSONObject(), System.currentTimeMillis(), null, request,domain);
		
		request.setAttribute("query", queryString);
		request.setAttribute("queryObject", "");

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"search","index");
	}	
	
	@RequestMapping(value = "/{domain}/summary", method = RequestMethod.GET)
	public String manageSummary(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		request.setAttribute("summaryText","");
		logger.log(Level.INFO, "Page controller: summary get request");
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.summary", new JSONObject(), System.currentTimeMillis(), null, request,domain);
		
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"summary","index");
	}	
	
	@RequestMapping(value = "/{domain}/summary", method = RequestMethod.POST)
	public String manageSummaryPOST(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "summaryText", required = false) String text) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.summaryWithDocument", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		request.setAttribute("summaryText",text);
		logger.log(Level.INFO, "Page controller: summary post");
		
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"summary","index");
	}		
	

	@RequestMapping(value = "/system/notImplemented", method = RequestMethod.GET)
	public String notImplemented(HttpServletRequest request) throws Exception {
		this.validateUserAndSetPageAttributes(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.notImplemented", new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);

		logger.log(Level.INFO, "Page controller: not implemented");
		return "notImplemented";
	}

	@RequestMapping(value = "/{domain}/visitedPages", method = RequestMethod.GET)
	public String visitedPages(HttpServletRequest request, @PathVariable("domain") String domain,  @RequestParam(value = "jobHistoryID", required = false) java.util.UUID jobHistoryID, @RequestParam(value = "id", required = false) java.util.UUID id) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		logger.log(Level.INFO, "Page controller: visited pages ");
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.visitedPages", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		try{
			 if (jobHistoryID != null ) {
				 request.setAttribute("jobHistoryID", jobHistoryID);
			 } else {
				 request.setAttribute("jobHistoryID", "");
			 }
			 if (id != null)
				 request.setAttribute("id", id);
			 else
				 request.setAttribute("id", "");
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"visitedPages","index");
	}

	@RequestMapping(value = "/{domain}/addEditJob", method = RequestMethod.GET)
	public String addEditJob(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "jobId", required = false)   java.util.UUID jobID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		JSONObject obj = new JSONObject();
		
		if (jobID == null) {
			request.setAttribute("jobID", "");
		}
		else {
			request.setAttribute("jobID", jobID.toString());
			obj.put("jobID", jobID.toString());
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.addEditJob", obj, System.currentTimeMillis(), null, request,domain);

		logger.log(Level.INFO, "Page controller: Add / Edit Job");
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"addEditJob","domainHome");
	}
	
	@RequestMapping(value = "/{domain}/jobParameters", method = RequestMethod.GET)
	public String viewJobParameters(HttpServletRequest request, @RequestParam(value = "name", required = false) String message, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.viewJobParameters", new JSONObject(), System.currentTimeMillis(), null, request,domain);
		
		//return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"addEditJobParameters","index");
		
		// any one can access - just lists parameter for a type of job
		return "addEditJobParameters";
	}
	
	@RequestMapping(value = "/{domain}/adjudication", method = RequestMethod.GET)
	public String adjudication(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.adjudication", new JSONObject(), System.currentTimeMillis(), null, request,domain);
		
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"adjudication","index");
	}
	
	@RequestMapping(value = "/{domain}/citationsVisualize", method = RequestMethod.GET)
	public String visualizeCitations(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.citationsVisualize", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"citationsVisualize","index");
	}

	@RequestMapping(value = "/{domain}/citationsHeatMap", method = RequestMethod.GET)
	public String visualizeCitationHeatMap(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.citationsHeatMap", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"citationsHeatMap","index");
	}
	
	
	@RequestMapping(value = "/{domain}/citationsFilter", method = RequestMethod.GET)
	public String visualizeCitationFilter(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.citationsFilter", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"citationsFilter","index");
	}
	
	@RequestMapping(value = "/{domain}/analyticFilter", method = RequestMethod.GET)
	public String visualizeAnalyticFilter(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analyticFilter", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"analyticFilter","index");
	}
	
	@RequestMapping(value = "/{domain}/analyticVisualize", method = RequestMethod.GET)
	public String visualizeAnalytic(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analyticVisualize", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"analyticVisualize","index");
	}

	@RequestMapping(value = "/{domain}/analyticHeatMap", method = RequestMethod.GET)
	public String visualizeAnalyticHeatMap(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analyticHeatMap", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"analyticHeatMap","index");
	}
	
	@RequestMapping(value = "/{domain}/analyticHeatMapTimeline", method = RequestMethod.GET)
	public String visualizeAnalyticHeatMapTimeline(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analyticHeatMapTimeline", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"analyticHeatMapTimeline","index");
	}
	
	@RequestMapping(value = "/{domain}/analyticChoroplethMap", method = RequestMethod.GET)
	public String visualizeAnalyticChoroplethMap(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analyticChoroplethMap", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"analyticChoroplethMap","index");
	}
	
	@RequestMapping(value = "/{domain}/analyticFrequencies", method = RequestMethod.GET)
	public String visualizeAnalyticFrequencies(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analyticFrequencies", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"analyticFrequencies","index");
	}
		
	@RequestMapping(value = "/{domain}/concept", method = RequestMethod.GET)
	public String concept(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.concept", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"concept","index");
	}
	
	@RequestMapping(value = "/{domain}/conceptImport", method = RequestMethod.GET)
	public String conceptImport(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.conceptImport", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"conceptImport","index");
	}	
	
	@RequestMapping(value = "/{domain}/structuralExtraction", method = RequestMethod.GET)
	public String structuralExtraction(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.structuralExtraction", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"structuralExtraction","index");
	}
	
	
	@RequestMapping(value = "/{domain}/domainDiscovery", method = RequestMethod.GET)
	public String domainDiscovery(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.domainDiscovery", new JSONObject(), System.currentTimeMillis(), null, request,domain);
				
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"domainDiscovery","index");
	}
	
	@RequestMapping(value = "/{domain}/domainDiscoverySession", method = RequestMethod.GET)
	public String domainDiscoverySession(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "sessionUUID", required = false) java.util.UUID sessionID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		
		JSONObject obj = new JSONObject();
		if (sessionID != null) {
			request.setAttribute("sessionUUID", sessionID.toString());
			obj.put("sessionID", sessionID.toString());
		}
		else {
			request.setAttribute("sessionUUID", "");
		}
		request.setAttribute("searchTerms", "");
		request.setAttribute("searchSource", "");
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.domainDiscoverySession",obj, System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"domainDiscoverySession","index");
	}
	
	@RequestMapping(value = "/{domain}/domainDiscoverySession", method = RequestMethod.POST)
	public String domainDiscoverySession(HttpServletRequest request, @PathVariable("domain") String domain, 
			                             @RequestParam(value = "searchTerms", required = true) String searchTerms,
			                             @RequestParam(value = "searchSource", required = true) String searchSource) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		
		JSONObject obj = new JSONObject();
		obj.put("searchTerms", searchTerms);
		obj.put("searchSource", searchSource);
		request.setAttribute("searchTerms", Base64.getEncoder().encodeToString(searchTerms.getBytes(StandardCharsets.UTF_8)));
		request.setAttribute("searchSource", Base64.getEncoder().encodeToString(searchSource.getBytes(StandardCharsets.UTF_8)));
		request.setAttribute("sessionUUID", "");
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.domainDiscoverySession",obj, System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"domainDiscoverySession","index");
	}	
	
	
	@RequestMapping(value = "/{domain}/viewIndex", method = RequestMethod.GET)
	public String viewIndex(HttpServletRequest request, @PathVariable("domain") String domain,@RequestParam(value = "documentArea") String documentArea, @RequestParam(value = "documentIndexID") java.util.UUID documentIndexID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
			
		if  (!documentArea.equals("normal") && !documentArea.equals("sandbox") && !documentArea.equals("archive"))  {
			throw new ValidationException("Invalid storage area");
		}
		
		request.setAttribute("documentArea", documentArea.toString());
		request.setAttribute("documentIndexID", documentIndexID.toString());
		JSONObject instrObj = new JSONObject().put("documentIndexID", documentIndexID.toString());
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.viewIndex",instrObj, System.currentTimeMillis(), null, request,domain);

		return "viewIndex";
	}
	
	
	
	@RequestMapping(value = "/{domain}/manageDocumentBuckets")
	public String manageDocumentBuckets(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "uuid", required = false)   java.util.UUID documentBucketID, @RequestParam(value = "tag", required = false, defaultValue="")  String tag) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		JSONObject instrumentObject = new JSONObject();
		if (documentBucketID == null) {
			request.setAttribute("documentBucketID", "");
			request.setAttribute("documentBucketTag", "");
		}
		else {
			request.setAttribute("documentBucketID", documentBucketID.toString());
			request.setAttribute("documentBucketTag", java.net.URLDecoder.decode(tag, "UTF-8").replaceAll(DocumentBucket.TAG_INVALID_CHARACTERS_REGEX,""));
			instrumentObject.put("documentBucketUUID", documentBucketID.toString());
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.manageDocumentBuckets", instrumentObject, System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"manageDocumentBuckets","index");
	}

	@RequestMapping(value = "/{domain}/documentIndex")
	public String documentIndex(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.documentIndex", new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"documentIndex","index");
	}

	
	@RequestMapping(value = "/{domain}/addEditDocumentBucket", method = RequestMethod.GET)
	public String addEditCollection(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "CollectionId", required = false)   String documentBucketID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		
		if (documentBucketID == null) {
			request.setAttribute("documentBucketID", "");
		}
		else {
			request.setAttribute("documentBucketID", documentBucketID.toString());
		}

		JSONObject instrObj = new JSONObject().put("documentBucketUUID", request.getAttribute("documentBucketID").toString());
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.addEditCollection",instrObj, System.currentTimeMillis(), null, request,domain);
		
		logger.log(Level.FINER, "Page controller: Add / Edit Document Bucket " + documentBucketID);
		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"addEditDocumentBucket","index");
	}
	

	@RequestMapping(value = "/{domain}/viewDocument")
	public String viewDocument(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.viewDocument",new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return this.getCheckAuthorization(request,RoleType.ANALYST,domain,"viewDocument","index");
	}	
	
	
	@RequestMapping(value="/system/uaHistory", method = RequestMethod.GET)
	public String userAgreementHistory(HttpServletRequest request) throws Exception {
		this.validateUserAndSetPageAttributes(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.userAgreementHistory",new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);

		return "userAgreementHistory";
	}

	@RequestMapping(value = "/system/userAgreementSign", method = RequestMethod.GET)
	public String userAgreementSign(HttpServletRequest request) throws Exception {
		this.validateUserAndSetPageAttributes(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.userAgreementSign",new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);
		
		return "userAgreement";
	}
	
	@RequestMapping(value = "/system/userAgreementAdjudicator", method = RequestMethod.GET)
	public String userAgreementApproval(HttpServletRequest request) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,Domain.DOMAIN_SYSTEM);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.userAgreementApproval",new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);
		
		return this.getCheckAuthorization(request,RoleType.ADJUDICATOR,Domain.DOMAIN_SYSTEM,"userAgreementAdjudicator","index");
	}

	@RequestMapping(value = "/system/userAgreementUnderReview", method = RequestMethod.GET)
	public String userAgreementUnderReview(HttpServletRequest request) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,Domain.DOMAIN_SYSTEM);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.userAgreementUnderReview",new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);

		return this.getCheckAuthorization(request,RoleType.ADJUDICATOR,Domain.DOMAIN_SYSTEM,"userAgreementReview","index");
	}
	
	@RequestMapping(value = "/system/userProfile", method = RequestMethod.GET)
	public String userProfile(HttpServletRequest request) throws Exception {
		this.validateUserAndSetPageAttributes(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.userProfile",new JSONObject(), System.currentTimeMillis(), null, request,Domain.DOMAIN_SYSTEM);
		
		return "userProfile";
	}

	
	@RequestMapping(value = "/{domain}/manageSearchAlerts", method = RequestMethod.GET)
	public String alertNotificationsForUser(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.manageSearchAlerts",new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return "manageSearchAlerts";
	}
	
	@RequestMapping(value = "/{domain}/feedback", method = RequestMethod.GET)
	public String feedback(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.feedback",new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return "feedback";
	}	
	
	@RequestMapping(value = "/{domain}/mapView", method = RequestMethod.GET)
	public String showMap(HttpServletRequest request, @PathVariable("domain") String domain,
			             @RequestParam(value = "uuid", required = false) java.util.UUID uuid) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.mapView",new JSONObject(), System.currentTimeMillis(), null, request,domain);

		if (uuid != null) {
			request.setAttribute("uuid", uuid.toString());
		}
		else {
			request.setAttribute("uuid", "");
		}
		
		return "mapView";
	}	
	
	@RequestMapping(value = "/{domain}/conceptSelector", method = RequestMethod.GET)
	public String showConceptSelector(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.conceptSelector",new JSONObject(), System.currentTimeMillis(), null, request,domain);

		return "conceptSelector";
	}	
	
	
	@RequestMapping(value = "/{domain}/plan", method = RequestMethod.GET)
	public String showPlanHome(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "projectUUID", required = false)   java.util.UUID projectUUID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		
		if (projectUUID == null) {
			request.setAttribute("projectUUID", "");
		}
		else {
			request.setAttribute("projectUUID", projectUUID.toString());
		}
		
		JSONObject instrObj = new JSONObject().put("projectUUID", request.getAttribute("projectUUID").toString());
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.project",instrObj, System.currentTimeMillis(), null, request,domain);
		
		return "domainHomePlan";
	}
	
	@RequestMapping(value = "/{domain}/analyze", method = RequestMethod.GET)
	public String showAnalyticsHome(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "projectUUID", required = false)   java.util.UUID projectUUID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		
		if (projectUUID == null) {
			request.setAttribute("projectUUID", "");
		}
		else {
			request.setAttribute("projectUUID", projectUUID.toString());
		}
		
		JSONObject instrObj = new JSONObject().put("projectUUID", request.getAttribute("projectUUID").toString());
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.analytics",instrObj, System.currentTimeMillis(), null, request,domain);
		
		return "domainHomeAnalytics";
	}
	
	
	@RequestMapping(value = "/{domain}/document", method = RequestMethod.GET)
	public String showDocumentHome(HttpServletRequest request, @PathVariable("domain") String domain, @RequestParam(value = "documentUUID", required = false)   java.util.UUID documentUUID) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		
		if (documentUUID == null) {
			request.setAttribute("documentUUID", "");
		}
		else {
			request.setAttribute("documentUUID", documentUUID.toString());
		}
		JSONObject instrObj = new JSONObject().put("documentUUID", request.getAttribute("documentUUID").toString());
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.document",instrObj, System.currentTimeMillis(), null, request,domain);
		
		return "domainHomeDocument";
	}	
	
	@RequestMapping(value = "/{domain}/collector", method = RequestMethod.GET)
	public String showCollectorHome(HttpServletRequest request, @PathVariable("domain") String domain) throws Exception {
		this.validateUserWithDomainAndSetPageAttributes(request,domain);
		this.instrumentAPI("edu.ncsu.las.rest.collector.PageController.domainHomeCollector",new JSONObject(), System.currentTimeMillis(), null, request,domain);
				
		return "domainHomeCollector";
	}		
}

