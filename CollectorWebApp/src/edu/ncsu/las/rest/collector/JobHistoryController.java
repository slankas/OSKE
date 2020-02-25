package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.JobHistory;

import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.util.DateUtilities;


/**
 * Handles requests for the JobHistory.
 * 
 */
@RequestMapping(value = "rest/{domain}/jobsHistory")
@Controller
public class JobHistoryController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	/**
	 * Gets job history records
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getJobs(HttpServletRequest request,
			@RequestParam(value = "jobId", required = false) UUID jobId,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.INFO,	"Job History controller: Get records. Params={ jobID=" + jobId + ", startTime=" + startTime + ", endTime=" + endTime + "}");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);		
		ZonedDateTime start = null, end = null;
		
		if (startTime != null && startTime.trim().length() >0) {
			start =  DateUtilities.getFromString(startTime);
			if (start == null) {
				throw new ValidationException("Invalid start parameter: "+startTime);
			}			
		}
		
		if (endTime != null && endTime.trim().length() >0) {
			end =  DateUtilities.getFromString(endTime);
			if (end == null) {
				throw new ValidationException("Invalid end parameter: "+endTime);
			}			
		}		
		
		if (jobId == null && end == null && start == null) { // nothing specified, limit to past 24 hours
			start = ZonedDateTime.now().minusDays(1);
		}
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobHistoryController.getJobHistory", new JSONObject().put("start", start).put("end", end).put("jobID", jobId), System.currentTimeMillis(), null, request,domainStr);	
		
		JSONObject result = new JSONObject();
		JSONArray jobHistories = new JSONArray();

		List<JobHistory> historyList = this.filterSystemJobs(JobHistory.getJobHistory(domainStr,start, end, jobId));
		for (JobHistory j: historyList) {
			jobHistories.put(j.toJSON());		
		}
		result.put("jobHistory", jobHistories);
		return result.toString();
	}
	
	private List<JobHistory> filterSystemJobs(List<JobHistory> historyList) {
		List<JobHistory> filteredHistoryList = historyList.stream()
                .filter(record -> !(record.getJobName().equals("DirectoryWatcher") || 
                       		        record.getJobName().equals("DomainDiscoveryHandler") ))
                .collect(Collectors.toList());
		return filteredHistoryList;
	}
	
	/**
	 * Get the most recent jobs
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/recent", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getRecentJobs(HttpServletRequest request,
			@PathVariable("domain") String domainStr)
			throws IOException, ValidationException {
		logger.log(Level.INFO, "JobHistory controller: get recent jobs");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobHistoryController.getRecentJobs", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);	

		JSONObject result = new JSONObject();
		JSONArray jobHistories = new JSONArray();
			
		for (JobHistory j:   this.filterSystemJobs(JobHistory.getRecentJobsLimitResultCount(domainStr, 50))) {
			jobHistories.put(j.toJSON());		
		}
		result.put("jobHistory", jobHistories);
		return result.toString();
	}
	

}