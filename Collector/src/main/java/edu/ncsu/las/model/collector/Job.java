package edu.ncsu.las.model.collector;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.JobStatus;
import edu.ncsu.las.persist.collector.JobDAO;
import edu.ncsu.las.persist.collector.JobHistoryDAO;
import edu.ncsu.las.persist.collector.VisitedPageDAO;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.storage.StorageProcessor;
import edu.ncsu.las.util.CronExpression;
import edu.ncsu.las.util.DateUtilities;

/**
 * Represents a crawling activity that will be executed on a regular basis
 */
public class Job {
	private static Logger logger =Logger.getLogger(Job.class.getName());
	
	public static final String DEFAULT_SCHEDULE = "0 0 10 * * ?";
	
	private UUID _id;
	private String _primaryField;
	private String _sourceHandler;
	private JobStatus _status;
	private java.sql.Timestamp _statusTimestamp = new java.sql.Timestamp(System.currentTimeMillis());
	private UUID _latestJobID;
	private String _ownerEmail;
	private JSONObject _config;
	private String _name;
	private String _latestJobCollector;
	private String _justification;
	private Integer _priority;
	
	private String  _cronSchedule;
	private Instant _cronNextRun;
	private Integer _randomPercent;
	private boolean _isExport;
	
	private String _domainInstanceName;
	
	private JSONObject _summary = null;
	
	private JSONArray _adjudicationAnswers = new JSONArray();
	
	public Job(UUID id, String primaryField, String sourceHandler, JobStatus status, 
		    java.sql.Timestamp statusTimestamp, UUID latestJobID, String ownerEmail, JSONObject config, 
		    String name, String latestJobCollector, String justification, Integer priority, 
		    String cronSchedule, Integer randomPercent, java.sql.Timestamp cronNextRun,
		    String domainInstanceName, String adjudicationAnswers, boolean isExport) {
		_id = id;
		_primaryField = primaryField;
		_sourceHandler = sourceHandler;
		_status = status;
		_statusTimestamp = statusTimestamp;
		_latestJobID = latestJobID;
		_ownerEmail = ownerEmail;
		_config = config;		
		_name = name;
		_latestJobCollector = latestJobCollector;
		_justification = justification;
		_priority = priority;
		_domainInstanceName = domainInstanceName;
		_randomPercent = randomPercent;
		_isExport = isExport;
		
		if (cronSchedule != null) {
			_cronSchedule = cronSchedule;
		}
		else {
			_cronSchedule = DEFAULT_SCHEDULE;
		}
		if (cronNextRun != null) {
			_cronNextRun = cronNextRun.toInstant();
		}
		else {
			_cronNextRun = Instant.now();
		}
		
		if (adjudicationAnswers != null && adjudicationAnswers.trim().length()>0) {
			_adjudicationAnswers = new JSONArray(adjudicationAnswers);
		}
		
	}

	public Job() {
	}

	public void setID(UUID id) {
		this._id = id;
	}

	public void setPrimaryFieldValue(String primaryFieldValue) {
		this._primaryField = primaryFieldValue;
	}

	public void setSourceHandler(String sourceHandler) {
		this._sourceHandler = sourceHandler;
	}

	public void setStatus(JobStatus status) {
		this._status = status;
	}

	public void setStatusTimestamp(java.sql.Timestamp statusTimestamp) {
		this._statusTimestamp = statusTimestamp;
	}

	public void setLatestJobID(UUID latestJobID) {
		this._latestJobID = latestJobID;
	}

	public void setOwnerEmail(String ownerEmail) {
		this._ownerEmail = ownerEmail;
	}

	public void setConfig(JSONObject config) {
		this._config = config;
	}	
	
	public void setPriority(int priority) {
		this._priority = priority;
	}	
	
	public Integer getPriority() {
		return _priority;
	}
	
	public void setRandomPercent(int random) {
		this._randomPercent = random;
	}	
	
	public Integer getRandomPercent() {
		return _randomPercent;
	}
	
	public void setExportData(boolean isExport) {
		this._isExport = isExport;
	}
	
	public boolean getExportData() {
		return _isExport;
	}
	
	public UUID getID() {
		return _id;
	}

	public String getPrimaryFieldValue() {
		return _primaryField;
	}

	public String getSourceHandler() {
		return _sourceHandler;
	}

	public JobStatus getStatus() {
		return _status;
	}

	public java.sql.Timestamp getStatusTimestamp() {
		return _statusTimestamp;
	}

	public UUID getLatestJobID() {
		return _latestJobID;
	}

	public String getOwnerEmail() {
		return _ownerEmail;
	}

	public JSONObject getConfiguration() {
		return _config;
	}	

	
	public String getLatestJobCollector() {
		return _latestJobCollector;
	}
	
	public void setLatestJobCollector(String newLatestJobCollector) {
		_latestJobCollector = newLatestJobCollector;
	}	
	
	public String getName() {
		return _name;
	}
	
	public void setName(String newName) {
		_name = newName;
	}	
	
	public String getJustification() {
		return _justification;
	}
	
	public void setJustification(String newJustification) {
		_justification = newJustification;
	}		
	
	public JSONArray getAdjudicationAnswers() {
		return new JSONArray(_adjudicationAnswers.toString());
	}
	
	public void setAdjudicationAnswers(JSONArray answers) {
		_adjudicationAnswers = new JSONArray(answers.toString());
	}
	
	
	public boolean claimJobToRun(UUID jobID, String collectorID) {
		this.setStatus(JobStatus.PROCESSING);
		this.setLatestJobID(jobID);
		this.setLatestJobCollector(collectorID);
		return (new JobDAO()).claimJobToRun(this);
	}	
	
	public boolean markComplete(String userID) {
		this.setStatus(JobStatus.COMPLETE);
		return (new JobDAO()).updateStatusAndJob(this,userID);
	}	
	

	public boolean markErrored(String userID) {
		this.setStatus(JobStatus.ERRORED);
		return (new JobDAO()).updateStatusAndJob(this,userID);
	}	
	
	public boolean markForHold(String userID) {
		this.setStatus(JobStatus.HOLD);
		return (new JobDAO()).updateStatusAndJob(this,userID);
	}
	
	public boolean updateStatusAndJobID(JobStatus status, UUID jobID,String userID) {
		this.setStatus(status);
		this.setLatestJobID(jobID);
		return (new JobDAO()).updateStatusAndJob(this,userID);
	}
	
	
	public static java.util.List<Job> getJobsAvailableForProcessing() {
		return (new JobDAO()).getJobsAvailableForProcessing();
	}
	
	public static java.util.List<Job> getJobsToStop() {
		return (new JobDAO()).getJobsToStop();
	}	
		
	public static java.util.List<Job> getAllJobs(String domainStr) {
		JobDAO dd = new JobDAO();
		return dd.getAllJobs(domainStr); 
	}

	public void createJob(String user) {
		JobDAO dd = new JobDAO();
		dd.insertJob(this,user);
	}

	public void editJob(String user) {
		JobDAO dd = new JobDAO();
		dd.editJob(this,user);
	}

	public static Job getJob(UUID jobUUID) {
		JobDAO dd = new JobDAO();
		return dd.getJob(jobUUID);
	}

	public boolean updateStatus(String userID) {
		JobDAO dd = new JobDAO();
		return dd.updateStatus(this,userID);
	}	
	
	public boolean updateNextRun() {
		JobDAO dd = new JobDAO();
		this.setNextRun();
		return dd.updateNextRun(this);
	}
	
	public JSONObject toJSON()  {
		JSONObject result = new JSONObject().put("id", _id)
				                            .put("primaryFieldValue", _primaryField)
				                            .put("sourceHandler", _sourceHandler)
				                            .put("priority", _priority)
				                            .put("status", _status.toString())
				                            .put("statusTimestamp", DateUtilities.getDateTimeISODateTimeFormat(_statusTimestamp.toInstant()))
				                            .put("latestJobID", _latestJobID)
				                            .put("ownerEmail", _ownerEmail)
				                            .put("configuration", _config)
				                            .put("latestJobCollector", _latestJobCollector)
				                            .put("name", _name)
				                            .put("justification", _justification)
				                            .put("cronSchedule", _cronSchedule)
				                            .put("randomPercent", _randomPercent)
				                            .put("exportData", _isExport)
				                            .put("adjudicationAnswers", _adjudicationAnswers)
				                            .put("nextRun", DateUtilities.getDateTimeISODateTimeFormat(_cronNextRun));  // convert me
		return result;
	}
	
	
	public static int updateOrphanedJobs(String collectorID) {
		JobDAO jd = new JobDAO();
		return jd.updateOrphanedJobs(collectorID);
	}	
	
	public String toString() {
		return this.getName();
	}
	
	public static Job retrieveJob(UUID jobID) {
		JobDAO jd = new JobDAO();
		return jd.getJob(jobID);
	}

	public static Job retrieveJob(String domainInstanceName, String jobName) {
		JobDAO dd = new JobDAO();
		return dd.getJob(domainInstanceName, jobName);
	}	
	
	public static java.util.List<Job> getJobs(String domainInstanceName, SourceHandlerInterface shi) {
		JobDAO dd = new JobDAO();
		return dd.getAllJobsBySourceHandler(domainInstanceName,shi.getSourceHandlerName()); 
	}
	
	/**
	 * 
	 * @param jobID
	 * @param newStatus
	 * @return the job record that was updated.  If not successfully, returns null
	 */
	public static Job updateJobStatus(UUID jobID, JobStatus newStatus, String userID) {
		Job job =  Job.retrieveJob(jobID);
		if (job == null) { return null;}  // unable to find the corresponding record...
		
		job.setStatus(newStatus);
		job.setStatusTimestamp(Timestamp.from(Instant.now()));
		if (job.updateStatus(userID)) {
			return job;
		}
		else {
			return null;
		}
	}

	public static Integer getNumberOfAdjudicationJobs(String domain){
		JobDAO dd = new JobDAO();
		return dd.getNumberOfAdjudicationJobsForDomain(domain, JobStatus.ADJUDICATION.toString());
	}
	
	public static java.util.List<Job>  getJobsByDomainAndStatus(String domain, JobStatus status) {
		JobDAO dd = new JobDAO();
		return dd.getJobsByDomainAndStatus(domain,status.toString());		
	}
	
	public static java.util.List<Job> getJobsByStatus(JobStatus status) {
		JobDAO dd = new JobDAO();
		return dd.getJobsByStatus(status.toString());
	}

	public boolean appendToJustification(String comment) {
		this.setJustification(this.getJustification()+" "+comment);
		
		return (new JobDAO()).updateJustification(this);
	}
	
	/**
	 * Moves any jobs that are marked as completed and their time interval has passed to "ready".
	 * This makes it more obvious to operators that there are jobs ready to run...
	 * 
	 * @return number of jobs that have been moved
	 */
	public static int moveCompletedJobsToReady() {
		return (new JobDAO()).moveCompletedJobsToReadyToRun();
	}

	
	public JSONObject getSummary() {
		if (_summary == null) {
			JSONObject newSummary = new JSONObject().put("id", this.getID().toString())
					                                .put("name", this.getName())
					                                .put("primaryFieldValue", this.getPrimaryFieldValue())
					                                .put("sourceHandler", this.getSourceHandler());
			_summary = newSummary;
		}
		return _summary;
	}

	
	public boolean isValidCronEntry(String entry) {
		try {
			new CronExpression(entry);
			return true;
		}
		catch (ParseException | NullPointerException pe) {
			return false;
		}
	}	
	
	public void setCronSchedule(String newValue) {
		_cronSchedule = newValue;
	}
	public String getCronSchedule() {
		return _cronSchedule;
	}

	public java.sql.Timestamp getNextRun() {
		return Timestamp.from(_cronNextRun);
	}		
	
	public void setNextRun() {
		try {
			CronExpression ce = new CronExpression(_cronSchedule);
			ce.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
			
			Instant scheduledNextRunTime = Instant.ofEpochMilli(ce.getNextValidTimeAfter(new java.util.Date()).getTime());
			if (this.getRandomPercent() == 0) {
				_cronNextRun = scheduledNextRunTime;
			} else {
				long typicalPerionBetweenRunsInMillis = ce.getMilliSecondsBetweenRuns();
				_cronNextRun = randomizeNextRun(scheduledNextRunTime,typicalPerionBetweenRunsInMillis,true);
			}
		}
		catch (ParseException | NullPointerException pe) {
			logger.log(Level.WARNING, "unable to set next run from cron schedule: "+pe.toString());
		}
	}	
	
	public void setNextRun(java.sql.Timestamp newTime) {
		_cronNextRun = newTime.toInstant();
    }
	
	/**
	 * Randomize when the job should next execute by a given percentage.
	 * 
	 * @param scheduledNextRunTime when is the job currently scheduled to be executed
	 * @param typicalPerionBetweenRunsInMillis 
	 * @return
	 */
	public Instant randomizeNextRun(Instant scheduledNextRunTime, long typicalPeriodBetweenRunsInMillis, boolean instrument) {
		long millisToNextRun = ChronoUnit.MILLIS.between(this.getNextRun().toInstant(),scheduledNextRunTime);  //getNextRun still contains when the current job was scheduled to start
		Instant adjustedScheduledNextRunTime = scheduledNextRunTime;
		
		// Check that if we are running in less than 50% of the expected time window, that we push that up to 100% for the period and modify the scheduled start time accordingly
		if (millisToNextRun < (.5 * typicalPeriodBetweenRunsInMillis)) {
			millisToNextRun = typicalPeriodBetweenRunsInMillis;
			adjustedScheduledNextRunTime = scheduledNextRunTime.plus(millisToNextRun,ChronoUnit.MILLIS);
		}
		
		long minimumTimeToNextRun = (long) ((Math.max( (100-this.getRandomPercent()),50) / 100.0) * millisToNextRun);
		long maxGap = (long) ((this.getRandomPercent() / 100.0) * millisToNextRun);
		
		long gapWindow = maxGap + (millisToNextRun- minimumTimeToNextRun);
		
		long randomGap = (long) ( new SecureRandom().nextDouble() * gapWindow);
		
		Instant minimumNextRun = adjustedScheduledNextRunTime.minus((millisToNextRun- minimumTimeToNextRun),ChronoUnit.MILLIS);
		
		Instant randomNextRun = minimumNextRun.plus( randomGap, ChronoUnit.MILLIS);
				
		JSONObject eventData = new JSONObject().put("scheduledNextRun", scheduledNextRunTime.toString())
				                               .put("randomizedNextRun", randomNextRun.toString())
				                               .put("periodBetweenRunsInMillis", typicalPeriodBetweenRunsInMillis)
				                               .put("millisToNextRun", millisToNextRun)
				                               .put("minimumTimeToNextRun", minimumTimeToNextRun)
				                               .put("maxGap", maxGap)
				                               .put("gapWindow", gapWindow)
				                               .put("randomGap", randomGap)
				                               .put("minimumNextRun", minimumNextRun.toString())
				                               .put("adjustedScheduledNextRunTime", adjustedScheduledNextRunTime.toString());
		logger.log(Level.INFO, eventData.toString());
		
		if (instrument) {
			JSONObject jobData = this.toJSON().put("randomValues", eventData);
			Instrumentation.createAndSendEvent(this.getDomainInstanceName(), "daemon.job.randomizeNextRun",System.currentTimeMillis(),System.currentTimeMillis(),jobData, null);
		}
		
		return randomNextRun;
	}

	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		this._domainInstanceName = domainInstanceName;
	}

	public boolean updateConfiguration(JSONObject configuration, String user) {
		this.setConfig(configuration);
		JobDAO dd = new JobDAO();
		return dd.updateConfiguration(this, user);
		
	}

	/**
	 * Removes all of the collected data for a particular job
	 * 
	 * @param userID 
	 */

	public void purgeJobData(String userID, JobStatus status) {
		purgeJobData(userID,false,status);
	}
	
	/**
	 * Removes all of the collected data for a particular job
	 * 
	 * @param userID 
	 * @param deleteJobRecord if true, will also delete the corresponding job record
	 */
	public void purgeJobData(String userID, boolean deleteJobRecord, JobStatus statusAfterPurge) {
		logger.log(Level.INFO, "Purging job (removing all data): "+this.toJSON());
		
		Job.updateJobStatus(this.getID(), JobStatus.PURGING,userID);
		
		java.util.List<VisitedPage> pages = VisitedPage.getVisitedPages(this.getDomainInstanceName(), this.getID());
		logger.log(Level.INFO, "Purge job - found visited pages: "+pages.size());
		
		StorageProcessor sp = StorageProcessor.getTheStorageProcessor();
		java.util.HashSet<UUID> storageUUIDs = new java.util.HashSet<UUID>();
		for (VisitedPage vp: pages) {
			UUID storageUUID = UUID.fromString(vp.getStorageUUID());
			if (storageUUIDs.contains(storageUUID)) {
				continue;
			}
			else {
				storageUUIDs.add(storageUUID);
				sp.deleteRawData(FileStorageAreaType.REGULAR, this.getDomainInstanceName(), vp.getStorageUUID());
			}
		}
		sp.deleteJSONRecords(FileStorageAreaType.REGULAR,  this.getDomainInstanceName(), this.getID(), storageUUIDs);
		
		(new JobHistoryDAO()).deleteByJobID(this.getID());
		(new VisitedPageDAO()).deleteByJobID(this.getLatestJobID());
		if (deleteJobRecord) {
			(new JobDAO()).deleteJobByID(this.getID());
			logger.log(Level.INFO, "Deleted job record: "+this.toJSON());			
		}
		else {
			Job.updateJobStatus(this.getID(), statusAfterPurge,userID);			
		}

		logger.log(Level.INFO, "Purged job complete: "+this.toJSON());
	}

	/**
	 * Removes the job, althought the archive records are still maintained...
	 * 
	 * @param userID 
	 */
	public void delete(String userID) {
		logger.log(Level.INFO, "delete job, purging first: "+this.toJSON());
		this.purgeJobData(userID, true,null);
	}
	
	
}
