package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.VisitedPage;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.storage.StorageProcessor;
import edu.ncsu.las.util.DateUtilities;

/**
 * Handles requests for the visited pages.
 */
@RequestMapping(value = "rest/{domain}/visitedPages")
@Controller
public class VisitedPagesController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	/**
	 * Gets the contents for "Visited Pages" table.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getVisitedPages(HttpServletRequest request,
			@RequestParam(value = "jobID", required = false) java.util.UUID jobID,
			@RequestParam(value = "jobHistoryID", required = false) java.util.UUID jobHistoryID,
			@RequestParam(value = "startTime", required = false) String startDateParam,
			@RequestParam(value = "endTime", required = false) String endDateParam,
			@PathVariable("domain") String domainStr) throws	IOException, ValidationException {
		logger.log(Level.FINE, "Visited page controller: Visited pages table, jobHistoryID = " + jobHistoryID);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		JSONObject vp = new JSONObject();
		if (jobID != null) {vp.put("jobID", jobID.toString()); } 
		if (jobHistoryID != null) {vp.put("jobHistoryID", jobHistoryID.toString());  }
		if (startDateParam != null) {vp.put("startDate", DateUtilities.getFromString(startDateParam).toInstant().toString()); }
		if (endDateParam != null) {vp.put("endDate", DateUtilities.getFromString(endDateParam).toInstant().toString()); }
		
        this.instrumentAPI("edu.ncsu.las.rest.collector.VisitedPagesController.getVisitedPages", vp, System.currentTimeMillis(),null, request,domainStr);

		
		java.util.List<VisitedPage> visitedPages;
		ZonedDateTime start = null, end = null;
		if(jobHistoryID!=null)
			visitedPages = VisitedPage.getVisitedPages(jobHistoryID);
		else if ( (startDateParam != null && startDateParam.trim().length() > 0) || 
		          (endDateParam   != null && endDateParam.trim().length()   > 0) ) {
			
			if (startDateParam != null && startDateParam.trim().length() > 0) {
				start = DateUtilities.getFromString(startDateParam);
				if (start == null) {
					throw new ValidationException("Invalid end parameter: "+startDateParam);
				}
			}
			if (endDateParam != null && endDateParam.trim().length() > 0) {
				end   = DateUtilities.getFromString(endDateParam);
				if (end == null) {
					throw new ValidationException("Invalid end parameter: "+endDateParam);
				}
			}
			
			visitedPages = VisitedPage.getVisitedPages(domainStr,start,end,jobID);
		}
		else {
			if (jobID == null && jobHistoryID==null) {
				start = ZonedDateTime.now().minusDays(1);
			}
			visitedPages = VisitedPage.getVisitedPages(domainStr,start,null,jobID);
		}
		
		if (visitedPages != null) {
			return createReturnResponse(visitedPages);
		}
		else {
			throw new ValidationException("visited pages not found", HttpServletResponse.SC_NOT_FOUND);
		}
		
	}
	
	/**
	 * Gets the contents for "Visited Pages" table.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "/recent", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getVisitedPageTableRecent(HttpServletRequest request, @PathVariable("domain") String domainStr)	throws IOException, ValidationException {
		logger.log(Level.FINE, "Visited page controller: Visited pages table, most recent");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
        this.instrumentAPI("edu.ncsu.las.rest.collector.VisitedPagesController.getVisitedPagesMostRecent", new JSONObject(), System.currentTimeMillis(),null, request,domainStr);

		return createReturnResponse(VisitedPage.getVisitedPages(domainStr, FileStorageAreaType.REGULAR.getLabel()));
	}	
	

	/**
	 * returns back the content for a particular page
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "/{pageID}/content", method = RequestMethod.GET)
	public void getVisitedPage(HttpServletRequest request , HttpServletResponse response,	@PathVariable("pageID") UUID pageID, @PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO, "Home controller: Visited pages table (" + pageID + ")" );
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.VisitedPagesController.getVisitedPageRawContent", new JSONObject().put("documentID", pageID.toString()), System.currentTimeMillis(),null, request,domainStr);
		
		VisitedPage vp = VisitedPage.getVisitedPage(pageID);
		if (vp == null) {
			throw new ValidationException("Record not found",HttpServletResponse.SC_NOT_FOUND);
		}
		else {
			// load the data
			byte[] data = StorageProcessor.getTheStorageProcessor().loadRawData(FileStorageAreaType.REGULAR, domainStr, vp.getStorageUUID().toString());	
			if (data != null) {
				response.setContentType(vp.getMimeType());
			    OutputStream out = response.getOutputStream();
			    out.write(data);
			}
			else {
				// deal with data not found
				System.out.println("File not found!");
			}
		}
	}

	/**
	 * returns back the content for a particular page
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "/{pageID}/storage", method = RequestMethod.GET)
	public void getVisitedPageByStorageID(HttpServletRequest request , HttpServletResponse response, @PathVariable("pageID") UUID storageID,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO, "Home controller: Visited pages table get by storageID (" + storageID + ")" );
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.VisitedPagesController.getVisitedPageRawContentByStorageID", new JSONObject().put("storageID", storageID.toString()), System.currentTimeMillis(),null, request,domainStr);
		
		VisitedPage vp = VisitedPage.getVisitedPageByStorageID(storageID);
		if (vp == null) {
			throw new ValidationException("Record not found",HttpServletResponse.SC_NOT_FOUND);
		}
		else {
			// load the data
			byte[] data = StorageProcessor.getTheStorageProcessor().loadRawData(FileStorageAreaType.REGULAR, domainStr, vp.getStorageUUID().toString());	
			if (data != null) {
				response.setContentType(vp.getMimeType());
			    OutputStream out = response.getOutputStream();
			    out.write(data);
			}
			else {
				// deal with data not found
				System.out.println("File not found!");
			}
		}
	}	
	
	
	/**
	 * Gets the contents for "Number Of Pages visited" on index page.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public @ResponseBody String numberOfPagesVisited(HttpServletRequest request,@PathVariable("domain") String domainStr) throws IOException {
		logger.log(Level.INFO, "Home controller: get number of pages visited");
		this.instrumentAPI("edu.ncsu.las.rest.collector.VisitedPagesController.numberOfPagesVisited", new JSONObject(),System.currentTimeMillis(),null, request,domainStr);

		Integer numberOfJobs = VisitedPage.getNumberOfPagesVisited(domainStr, FileStorageAreaType.REGULAR.getLabel());

		return numberOfJobs.toString();
	}

	
	private String createReturnResponse(java.util.List<VisitedPage> pages) {
		JSONObject result = new JSONObject();
		JSONArray vps = new JSONArray();
			
		pages.stream().forEach( vp -> vps.put(vp.toJSON()) );			
		result.put("visitedPages", vps);
		return result.toString();
	}
}