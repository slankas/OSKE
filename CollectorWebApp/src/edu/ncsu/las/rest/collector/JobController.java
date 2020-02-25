package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.EmailClient;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.JobStatus;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.source.AbstractHandler;
import edu.ncsu.las.source.EmailSourceHandler;
import edu.ncsu.las.source.FormAuthenticationSupport;
import edu.ncsu.las.util.CronExpression;



/**
 * Handles requests for the Job.
 * 
 *
 */
@RequestMapping(value = "rest/{domain}/jobs")
@Controller
public class JobController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	
	/**
	 * Return all jobs
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getJobsForDomain(HttpServletRequest request,
			@RequestParam(value = "q", required = false) String query, 
			@PathVariable("domain") String domainStr)
			throws IOException, ValidationException {
		logger.log(Level.FINER, "Job controller: Get Job");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.getJobsForDomain", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);		

		
		JSONObject result = new JSONObject();
		JSONArray jobs = new JSONArray();
			
		for (Job j: Job.getAllJobs(domainStr)) {
			jobs.put(j.toJSON());		
		}
		result.put("jobs", jobs);
		return result.toString().getBytes("UTF-8");
	}	
	
	/**
	 * Returns questions that a job creator should complete to submit a job
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/questions", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
	public @ResponseBody byte[] retreiveAdjudicatorQuesions(HttpServletRequest request, @PathVariable("domain") String domainStr)	throws IOException, ValidationException {
		logger.log(Level.FINE, "Job controller: adjudication questions");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.retreiveAdjudicatorQuesions", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);		
		
		return Configuration.getConfigurationObject(domainStr, ConfigurationType.JOB_ADJUDICATION).toString().getBytes("UTF-8");
	}	
	
	
	/**
	 * Returns a specific job record
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/{jobId}", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
	public @ResponseBody byte[] retreiveSpecificJob(HttpServletRequest request,
			@PathVariable("jobId") UUID jobId,
			@PathVariable("domain") String domainStr)
			throws IOException, ValidationException {
		logger.log(Level.FINER, "Job controller: retreiveSpecificJob for Job ID: "+jobId);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.retreiveSpecificJob", new JSONObject().put("jobID", jobId.toString()), System.currentTimeMillis(), null, request,domainStr);		
		
		Job job = null;
		try {
			job = Job.getJob(jobId);

			if (job != null) {
				return job.toJSON().toString().getBytes("UTF-8");
			}
			else {
				throw new ValidationException("Unable to find job",HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (Exception e) {
			logger.log(Level.FINER, "Unable to retreive job by ID: "+jobId);
			throw new ValidationException("Unable to find job",HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
	/**
	 * Returns a specific job record
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/jobname/{jobName}", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
	public @ResponseBody byte[] retreiveSpecificJobByName(HttpServletRequest request, HttpServletResponse response,	
			                                              @PathVariable("jobName") String jobName,
			                                              @PathVariable("domain") String domainStr)	throws ValidationException, IOException {
		logger.log(Level.FINER, "Job controller: retreiveSpecificJob for Job Name: "+jobName);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.retreiveSpecificJobByName", new JSONObject().put("jobName", jobName), System.currentTimeMillis(), null, request,domainStr);		
		
		Job job = Job.retrieveJob(domainStr,jobName);

		if (job == null) {
			throw new ValidationException("Unable to find job",HttpServletResponse.SC_NOT_FOUND);
		}
		else {
			return job.toJSON().toString().getBytes("UTF-8");
		}
	}

	/**
	 * Test the authentication settings
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/authentication/test", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
	public @ResponseBody byte[] testAuthentication(HttpServletRequest request, HttpServletResponse response, @RequestBody String data,@PathVariable("domain") String domainStr)	throws ValidationException, IOException {
		logger.log(Level.FINER, "Job controller: test authentication");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.testAuthentication", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);		

		try {
			JSONObject dataObject = new JSONObject(data);
			String handler = dataObject.getString("sourceHandler");
			JSONObject emailResult = null;
			if (handler.equals("email")) {
				String confString = dataObject.getString("configuration");
				JSONObject config = new JSONObject(confString);
	
				EmailSourceHandler esh = new EmailSourceHandler();
				emailResult = esh.testAuthentication(config);
				
				if  (!(config.has("webCrawler") && config.getJSONObject("webCrawler").has("authenticationForm"))) {
					return emailResult.toString().getBytes("UTF-8");				
				}
			}
			
			String confString = dataObject.getString("configuration");
			JSONObject authenticationFormObject = (new JSONObject(confString)).getJSONObject("webCrawler").getJSONObject("authenticationForm");

			FormAuthenticationSupport fas = new FormAuthenticationSupport();
			JSONObject result = fas.testAuthentication(domainStr, authenticationFormObject);
			
			if (emailResult != null) {
				result.put("emailStatus", emailResult);
			}
			
			return result.toString().getBytes("UTF-8");
		}
		catch (Throwable t) {
			throw new ValidationException(t.toString());
		}
	}

	/**
	 * Test the authentication settings
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/configuration/test", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
	public @ResponseBody byte[] testConfiguration(HttpServletRequest request, HttpServletResponse response, @RequestBody String data,@PathVariable("domain") String domainStr)	throws ValidationException, IOException {
		logger.log(Level.FINER, "Job controller: test authentication");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.testConfiguration", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);		//authentication data might be in dataObject - that should not be logged
		
		try {
			JSONObject dataObject = new JSONObject(data);	
			String sourceHandler = dataObject.getString("sourceHandler");
			JSONObject configuration = new JSONObject(dataObject.getString("configuration"));
			
			AbstractHandler shi = AbstractHandler.getSourceHandler(sourceHandler);
			
			return shi.testConfiguration(dataObject.getString("name"), domainStr, configuration, dataObject.getString("url")).toString().getBytes("UTF-8");
		}
		catch (Throwable t) {
			throw new ValidationException(t.toString());
		}
	}	
	
	
	/**
	 * Creates a job
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody byte[] createJob(HttpServletRequest request, @RequestBody String job,@PathVariable("domain") String domainStr) throws ValidationException, IOException {
		logger.log(Level.FINER, "Job Controller: Create Job : " + job);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		JSONObject jobJSON = new JSONObject(job);

		Job jobObject = createJobFromJSON(jobJSON, this.getEmailAddress(request),domainStr);
		
		String status = jobJSON.getString("status");
		
		//TODO: add better validation (can't trust client side)
		// This includes checking limites per source handler and that job neames are unique
		
		//validate job configuration items
		AbstractHandler shi = AbstractHandler.getSourceHandler(jobObject.getSourceHandler());
		java.util.List<String> errors = shi.validateConfiguration(domainStr, jobObject.getPrimaryFieldValue(), jobObject.getConfiguration());
		errors.addAll(shi.validateInstantCount(domainStr,jobObject));
		if (shi.supportsJob() == false) {
			errors.add(shi.getSourceHandlerDisplayName() + " can not be used a source for jobs.");
		}
		if (errors.size() > 0) {
			String message = errors.stream().collect(Collectors.joining("\\n"));
			throw new ValidationException(message);
		}
		
		shi.getSourceParameterRepository().checkConfigurationForEncryptedFields(jobObject.getConfiguration());
		
		if (status.equals("draft")) {	jobObject.setStatus(JobStatus.DRAFT);	}
		else {							jobObject.setStatus(JobStatus.ADJUDICATION);}
		
		
		try {
			jobObject.createJob(this.getEmailAddress(request));
			this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.createJob", jobObject.toJSON(), System.currentTimeMillis(), null, request,domainStr);

			if (jobObject.getStatus() == JobStatus.ADJUDICATION) {
				this.sendAdjudicationNoticeEmail(domainStr,getAdjudicationHREF(request,domainStr), jobObject);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ValidationException("Unable to create job : "	+ e.toString());
		}
		logger.log(Level.FINER, "Job Controller: job created: "+ jobObject.getID());
		
		return jobObject.toJSON().toString().getBytes("UTF-8");
	}
	
	/**
	 * Schedule all errored jobs
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/scheduleErrored", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody byte[] scheduleAllErrrorJobs(HttpServletRequest request,@PathVariable("domain") String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER, "Job controller: schedule all errored jobs");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.scheduleAllErrrorJobs", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);

		String userID = this.getEmailAddress(request);
		
		JSONArray updatedJobs = new JSONArray();
		
		for (Job j: Job.getJobsByDomainAndStatus(domainStr,JobStatus.ERRORED)) {
			Job updatedJob = Job.updateJobStatus(j.getID(), JobStatus.SCHEDULED, userID);
			if (updatedJob != null) {
				updatedJob.updateNextRun();
				updatedJobs.put(j.getID().toString());
			}
		}
		
		JSONObject result = new JSONObject().put("status", "success")
				                            .put("jobs", updatedJobs);
		return result.toString().getBytes("UTF-8");
	}	
	

	/**
	 * Performs edit on job (Status change)
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/{jobId}/{action}", method = RequestMethod.POST)
	public @ResponseBody byte[] performJobOperation(HttpServletRequest request, HttpServletResponse response, 
			                                  @PathVariable("jobId") UUID jobId, 
			                                  @PathVariable("action") String action,
			                                  @PathVariable("domain") String domainStr,
			                                  @RequestBody(required=false) String data) throws ValidationException, IOException {
		logger.log(Level.FINER, "Job Controller: Edit Job status: " + jobId + ", Action = " + action);

		if (jobId == null) {
			throw new ValidationException("Unable to update job status, no job-id specified ");
		}
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.performJobOperation", new JSONObject().put("jobID",jobId.toString()).put("action",action), System.currentTimeMillis(), null, request,domainStr);

		String userID = this.getEmailAddress(request);

		if (action.equals("purge")) {
			Job job = Job.getJob(jobId);
			if (job.getOwnerEmail().equals(this.getEmailAddress(request)) == false && this.getUser(request).hasAccess(domainStr,  RoleType.ADJUDICATOR)) {
				throw new ValidationException("Unable to purge job - must be the job owner or an adjudicator");
			}
			return executePurgeJob(domainStr, jobId, new JSONObject(data).getString("rationale"), userID).getBytes("UTF-8");
		} 
		else if (action.equals("delete")) {
			Job job = Job.getJob(jobId);
			if (job.getOwnerEmail().equals(this.getEmailAddress(request)) == false && this.getUser(request).hasAccess(domainStr,  RoleType.ADJUDICATOR)) {
				throw new ValidationException("Unable to delete job - must be the job owner or an adjudicator");
			}
			return executeDeleteJob(domainStr, jobId, new JSONObject(data).getString("rationale"), userID).getBytes("UTF-8");
		}
		else if (action.equals("hold")) {
			Job job1 = Job.getJob(jobId);
			if (job1.getStatus() == JobStatus.COMPLETE  || 
			    job1.getStatus() == JobStatus.READY     || 
			    job1.getStatus() == JobStatus.SCHEDULED ||
			    job1.getStatus() == JobStatus.ERRORED )	{
				Job job = Job.updateJobStatus(jobId, JobStatus.HOLD, userID);
				if (job != null) {
					return JobStatus.HOLD.toString().getBytes("UTF-8");
				}
				else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Job status not updated in record");
				}
			}
			else {
				throw new ValidationException("Job cannot be put on hold.");
			}
		} else if (action.equals("stop")) {
			Job job = Job.updateJobStatus(jobId, JobStatus.STOPPING,userID);
			
			if (job != null) {
				UUID jobHistoryID = job.getLatestJobID();
				JobHistory jh = JobHistory.retrieve(jobHistoryID);
				if (jh != null) {
					User u = this.getUser(request);
					jh.updateStatusAndComments(JobHistoryStatus.STOPPING, "Job stopping on request of "+u.getEmailID());
				}
			}
			else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Job status not updated in record");
			}
			return JobStatus.STOPPING.toString().getBytes("UTF-8");
		} else if (action.equals("run")) {
			Job job = Job.updateJobStatus(jobId, JobStatus.READY,userID);
			if (job != null) {
				return JobStatus.READY.toString().getBytes("UTF-8");
			}
			else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Job status not updated in record");
			}
		} else if (action.equals("schedule")) {
			Job job1 = Job.getJob(jobId);
			if (job1.getStatus() == JobStatus.HOLD || job1.getStatus() == JobStatus.ERRORED) {
				Job job = Job.updateJobStatus(jobId, JobStatus.SCHEDULED,userID);
	
				if (job != null) {
					job.updateNextRun();
					return JobStatus.SCHEDULED.toString().getBytes("UTF-8");
				}
				else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Job status not updated in record");
				}
			}
			else {
				throw new ValidationException("Job cannot be scheduled.");
			}
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
		}
		return "".getBytes("UTF-8");
	}

	private String executePurgeJob(String domain, UUID jobID, String rationale, String userID)	throws ValidationException {
		Job job = Job.getJob(jobID);

		if (job.getStatus() == JobStatus.STOPPING  || job.getStatus() == JobStatus.PROCESSING) {
			throw new ValidationException("Job may not be purged while running.", HttpServletResponse.SC_BAD_REQUEST);
		}
		else {
			Runnable runTask = new Runnable() {
				@Override
				public void run() {				
					try {
						long startTime = System.currentTimeMillis();
						
						job.purgeJobData(userID,job.getStatus());
						String adjudicators[] = User.getAdjudicatorEmails(job.getDomainInstanceName());
						String title = "Job Purged: "+job.getName();
						String message = "Job has been purged. (i.e., collected data has been removed) .<br>Rationale: "+rationale;
						Collector.getTheCollecter().getEmailClient().sendMessage(Arrays.asList(adjudicators),new java.util.ArrayList<String>(),new java.util.ArrayList<String>(), title, message);
						
						JSONObject jobObject = job.toJSON().put("purgerUserID", userID)
								                           .put("purgeRationale", rationale);
						
						Instrumentation.createAndSendEvent(domain, "job.purged", startTime, System.currentTimeMillis(),jobObject, null );
					} catch (Exception e) {
						logger.log(Level.WARNING,"Unable to purge job record: "+jobID,e);
					}				
				}
			};
			Collector.getTheCollecter().runTask(runTask);
			return new JSONObject().put("result", "success")
					               .put("message", "Job purge initiated")
					               .put("id", jobID).toString();
			
		}
	}
	
	
	private String executeDeleteJob(String domain, UUID jobID, String rationale, String userID)	throws ValidationException {
		Job job = Job.getJob(jobID);

		if (job.getStatus() == JobStatus.STOPPING  || job.getStatus() == JobStatus.PROCESSING) {
			throw new ValidationException("Job may not be deleted while running.", HttpServletResponse.SC_BAD_REQUEST);
		}
		else {
			Runnable runTask = new Runnable() {
				@Override
				public void run() {				
					try {
						long startTime = System.currentTimeMillis();
						job.delete(userID);
						String adjudicators[] = User.getAdjudicatorEmails(job.getDomainInstanceName());
						String title = "Job Deleted: "+job.getName();
						String message = "Job has been deleted. (i.e., collected data has been removed and job record has been deleted) .<br>Rationale: "+rationale;
						Collector.getTheCollecter().getEmailClient().sendMessage(Arrays.asList(adjudicators),new java.util.ArrayList<String>(),new java.util.ArrayList<String>(), title, message);
						
						JSONObject jobObject = job.toJSON().put("deleteUserID", userID)
								                           .put("deleteRationale", rationale);
						
						Instrumentation.createAndSendEvent(domain, "job.deleted", startTime, System.currentTimeMillis(),jobObject, null );
					} catch (Exception e) {
						logger.log(Level.WARNING,"Unable to delte job record: "+jobID,e);
					}				
				}
			};
			Collector.getTheCollecter().runTask(runTask);
			return new JSONObject().put("result", "success")
					               .put("message", "Job delete initiated")
					               .put("id", jobID).toString();
			
		}
	}	

	/**
	 * Performs edit on job
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/{jobId}", method = RequestMethod.PUT)
	public @ResponseBody byte[] editJobOperations(HttpServletRequest request,  
			                                      @PathVariable("jobId") UUID jobId, 
			                                      @RequestBody String job,
			                                      @PathVariable("domain") String domainStr) throws ValidationException, IOException {
		logger.log(Level.FINER, "Job Controller: Edit Job : " + jobId);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		String adjudicationHREF = getAdjudicationHREF(request,domainStr);
		
		JSONObject jobJSON = new JSONObject(job);
		Job storedJob = Job.retrieveJob(jobId);
		if (storedJob.getStatus().isEditable() == false) {
			throw new ValidationException("Status '"+storedJob.getStatus()+"' invalid for update.");
		}
		
		Job editedJob = editJob(jobId, jobJSON, this.getEmailAddress(request), adjudicationHREF,domainStr); //validation occurs in edit job
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.editJob", editedJob.toJSON(), System.currentTimeMillis(), null, request,domainStr);

		return editedJob.toJSON().toString().getBytes("UTF-8");
	}

	private String getAdjudicationHREF(HttpServletRequest request, String domain) {
		String referrer = request.getHeader("Referer");
		String result = referrer.substring(0,referrer.indexOf(domain)) + domain +  "/adjudication";
		return result;
		//String adjudicationHREF = request.getRequestURL().substring(0,request.getRequestURL().indexOf("/rest")) + "/"+ domain +"/adjudication";
		//return adjudicationHREF;
	}

	/**
	 * TODO:  probably need to move this and some of the ancillary methods to the models
	 * 
	 * @param jobId
	 * @param jobJSON
	 * @param email  email of the current user;
	 * @return
	 * @throws ValidationException
	 */
	private Job editJob(UUID jobId, JSONObject jobJSON, String email, String hrefAdjudication, String domain) throws ValidationException {
		if (jobId == null) {
			throw new ValidationException("Unable to edit job, no job-id specified ");
		}

		logger.log(Level.FINER, "Job Controller: POST job. (Edit job) " + jobId);

		Job job = createJobFromJSON(jobJSON, email,domain);
			
		AbstractHandler shi = AbstractHandler.getSourceHandler(job.getSourceHandler());
		java.util.List<String> errors = shi.validateConfiguration(domain, job.getPrimaryFieldValue(), job.getConfiguration());
		if (shi.supportsJob() == false) {
			errors.add(shi.getSourceHandlerDisplayName() + " can not be used a source for jobs.");
		}
		if (errors.size() > 0) {
			String message = errors.stream().collect(Collectors.joining("\\n"));
			throw new ValidationException(message);
		}
		
		shi.getSourceParameterRepository().checkConfigurationForEncryptedFields(job.getConfiguration());
		
		job.setID(jobId);
		job.setStatus(JobStatus.ADJUDICATION);
		job.setStatusTimestamp(Timestamp.from(Instant.now()));
		try {
			job.editJob(email);

			sendAdjudicationNoticeEmail(domain,hrefAdjudication, job);
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Job Controller: POST job. (Edit job) " + jobId, e);
			throw new ValidationException("Unable to create job : "	+ e.toString());
		}
		return job;
	}

	private void sendAdjudicationNoticeEmail(String domain, String hrefAdjudication, Job job)	throws AddressException, MessagingException, ParseException {
		StringBuilder body = new StringBuilder(job.getName());
		body.append(" needs to be approved.");
		body.append("<br>Owner: " + job.getOwnerEmail());
		body.append("<br>Schedule: " + job.getCronSchedule() + "<br>&nbsp;&nbsp;&nbsp;" + new CronExpression(job.getCronSchedule()).getExpressionSummary().replaceAll("\n","<br>&nbsp;&nbsp;&nbsp;") );
		body.append("<br>Priority: " + job.getPriority());
		body.append("<br>URL: " + job.getPrimaryFieldValue());
		body.append("<br>Source Handler: " + job.getSourceHandler());
		body.append("<br>Justification: " + job.getJustification());
		body.append("<br>Randomize Wait Perecentage: "+ job.getRandomPercent());
		body.append("<br>Configuration: " + job.getConfiguration());
		
		if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.JOB_ADJUDIDCATION_UTILIZE)) {
			body.append("<br>Adjudication Questions: " + job.getAdjudicationAnswers().toString(4).replace("\n", "<br>"));
		}
		
		body.append("<p>&nbsp;<br><a href=\""+hrefAdjudication+"\">Adjudicate Collector Jobs</a>");
		
		String adjudicators[] = User.getAdjudicatorEmails(job.getDomainInstanceName());
		
		EmailClient ec = Collector.getTheCollecter().getEmailClient();
		
		String title = Collector.getTheCollecter().getDomain(job.getDomainInstanceName()).getFullName() +": Collector Job Requires Adjudication";
		
		ec.sendMessage(Arrays.asList(adjudicators),new java.util.ArrayList<String>(),new java.util.ArrayList<String>(), title, body.toString());
	}

	/**
	 * Creates a Job object from request parameters.
	 * @param request 
	 * 
	 * @throws ValidationException
	 */
	private Job createJobFromJSON(JSONObject jobJSON, String email, String domain) throws ValidationException {
		
	
		
		//TODO: need better error checking through all of the parameters

		String name = jobJSON.getString("name");
		String primaryFieldValue = jobJSON.getString("primaryFieldValue");
		String sourceHandler = jobJSON.getString("sourceHandler");
		JSONObject configuration = new JSONObject(jobJSON.getString("configuration"));
		String schedule       = jobJSON.getString("schedule");
		String adjudicationAnswers = jobJSON.getJSONArray("adjudicationAnswers").toString();
		
		//Need to csee if configuration has any password fields and if the field needs to be encrypted.
		JSONObject webConfiguration = configuration.optJSONObject("webCrawler");
		if (webConfiguration != null) {
			if (webConfiguration.has("authorizationBasic")) { checkJSONObjectForMD5Password(webConfiguration.getJSONObject("authorizationBasic"), "password"); }
			if (webConfiguration.has("authorizationForm"))  { checkJSONObjectForMD5Password(webConfiguration.getJSONObject("authorizationForm"),  "password"); }
			if (webConfiguration.has("authorizationNTLM"))  { checkJSONObjectForMD5Password(webConfiguration.getJSONObject("authorizationNTLM"),  "password"); }
		}

		java.sql.Timestamp nextRun;
		try {
			CronExpression ce = new CronExpression(schedule);
			ce.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
			java.util.Date d = ce.getNextValidTimeAfter(new java.util.Date());
			nextRun = Timestamp.from(Instant.ofEpochMilli(d.getTime()));
		}
		catch (ParseException pe) {
			throw new ValidationException("Invalid schedule: "+schedule+", "+pe.toString());
		}
		
		int priority;
		try {
			priority = jobJSON.getInt("priority");
		} catch (Exception e) {
			priority = -1;
		}
		
		// note random percent should be between 0 and 500.  0 implies no randomization. 
		int randomPercent;
		try {
			randomPercent = jobJSON.getInt("randomPercent");
		} catch (Exception e) {
			randomPercent = 0;
		}
		
		boolean isExport;
		try {
			isExport = jobJSON.getBoolean("exportData");
		} catch (Exception e) {
			isExport = false;
		}
		
		String justification;
		try {
			justification = jobJSON.getString("justification");
		} catch (Exception e) {
			justification = "";
		}
		String latestJobCollector = "";
		logger.log(Level.FINEST, "priority: " + priority);
		logger.log(Level.FINEST, "name: " + name);
		logger.log(Level.FINEST, "primaryFieldValue: " + primaryFieldValue);
		logger.log(Level.FINEST, "sourceHandler: " + sourceHandler);
		logger.log(Level.FINEST, "justification: " + justification);
		logger.log(Level.FINEST, "configuration: " + configuration);
		logger.log(Level.FINEST, "schedule: " + schedule);
		logger.log(Level.FINEST, "randomPercent: " + randomPercent);
		logger.log(Level.FINEST, "exportData: " + isExport);

		UUID uuid = edu.ncsu.las.util.UUID.createTimeUUID();

		Job job = new Job(uuid, primaryFieldValue, sourceHandler, JobStatus.NEW, null, null, email, configuration, name,
				latestJobCollector, justification, priority, schedule, randomPercent, nextRun, domain, adjudicationAnswers, isExport); 
		return job;
	}
	
	private void checkJSONObjectForMD5Password(JSONObject obj, String fieldName) {
		String password = obj.getString(fieldName);
		if (password.startsWith(FormAuthenticationSupport.HASH_MD5_INDICATOR)) {
			password = FormAuthenticationSupport.getMD5HashForPassword(password);
		}
	}
	

	/**
	 * Gets the contents for "Number Of Jobs" on index page.
	 * 
	 * @throws IOException
	 * @throws ValidationException 
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "/count", method = RequestMethod.GET)
	public @ResponseBody byte[] getNumberOfJobs(HttpServletRequest request,
			@RequestParam(value = "running", required = false) Boolean running,
			@PathVariable("domain") String domainStr)	throws IOException, ValidationException {
		logger.log(Level.FINER, "Home controller: get number of jobs, running param = " + running);
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.getNumberOfJobs", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);

		Integer numberOfJobs;

		if (running != null) {
			numberOfJobs = JobHistory.getRunningNumberOfJobs(domainStr);
		} else {
			numberOfJobs = JobHistory.getNumberOfJobs(domainStr);
		}
		
		return numberOfJobs.toString().getBytes("UTF-8");
	}
	
	/**
	 * Gets number of jobs to be adjudicated.
	 * 
	 * @throws IOException
	 * @throws ValidationException 
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	@RequestMapping(value = "/adjudication/count", method = RequestMethod.GET)
	public @ResponseBody byte[] getNumberOfAdjudicationJobs(HttpServletRequest request,@PathVariable("domain") String domainStr)	throws  IOException, ValidationException {
		logger.log(Level.FINER, "Home controller: get number of jobs to be adjudicated, running param = ");
		this.validateAuthorization(request, domainStr, RoleType.ADJUDICATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.getNumberOfAdjudicationJobs", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);

		Integer numberOfJobs = Job.getNumberOfAdjudicationJobs(domainStr);
			
		return numberOfJobs.toString().getBytes("UTF-8");
	}
	
	/**
	 * Return jobs that need adjudication
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 */
	@RequestMapping(value = "/adjudication", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
	public @ResponseBody byte[] getAdjucationJobs(HttpServletRequest request,@PathVariable("domain") String domainStr)
			throws IOException, ValidationException {
		logger.log(Level.FINER, "Job controller: getAdjucationJobs");
		this.validateAuthorization(request, domainStr, RoleType.ADJUDICATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.getAdjucationJobs", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		JSONObject result = new JSONObject();
		JSONArray jobs = new JSONArray();
			
		for (Job j: Job.getJobsByDomainAndStatus(domainStr,JobStatus.ADJUDICATION)) {
			jobs.put(j.toJSON());		
		}
		result.put("jobs", jobs);
		return result.toString().getBytes("UTF-8");		
	}
	
	/**
	 * Take action on jobs that need adjudication: action = 'approve' or 'disapprove'
	 * 
	 * @throws ValidationException
	 * @throws IOException
	 * @throws MessagingException 
	 * @throws AddressException 
	 */
	@RequestMapping(value = "/adjudication/{jobID}", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody byte[] adjucationJobsAction(HttpServletRequest request,
			@PathVariable("jobID") UUID jobID,
			@PathVariable("domain") String domainStr,
			@RequestBody String messageData) throws ValidationException, IOException, AddressException, MessagingException {
		this.validateAuthorization(request, domainStr, RoleType.ADJUDICATOR);
		
		JSONObject msgData = new JSONObject(messageData);
		String action  = msgData.getString("action");
		String comment = msgData.getString("comment");

		this.instrumentAPI("edu.ncsu.las.rest.collector.JobController.adjucationJobsAction", new JSONObject().put("jobID", jobID.toString()).put("action", action), System.currentTimeMillis(), null, request,domainStr);
		logger.log(Level.FINER, "Job controller: adjucationJobsAction: jobId="+jobID+", action="+action+", comment="+comment);
		
		String newstatus = null;
		String userID = this.getEmailAddress(request);
		
		if (action.equalsIgnoreCase("approve")){
			newstatus = JobStatus.READY.toString();
			Job job = Job.updateJobStatus(jobID, JobStatus.SCHEDULED, userID);
			job.appendToJustification(comment);
			job.updateNextRun();
		}
		else if (action.equalsIgnoreCase("disapprove")){
			newstatus = JobStatus.INACTIVE.toString();
			Job job = Job.updateJobStatus(jobID, JobStatus.INACTIVE, userID);
			job.appendToJustification(comment);

			String body = this.getUser(request).getName()+ " disapproved your job: "+job.getName()+"<p>Reason: "+comment+"</p>";
			EmailClient ec = Collector.getTheCollecter().getEmailClient();
			ec.sendMessage(job.getOwnerEmail(), "Job disapproved by Adjudicator.", body);
		}
		else {
			throw new ValidationException("Invalid action: "+action,HttpServletResponse.SC_BAD_REQUEST);
		}
				
		return (new JSONObject()).put("result", newstatus).toString().getBytes("UTF-8");		
		
	}
}