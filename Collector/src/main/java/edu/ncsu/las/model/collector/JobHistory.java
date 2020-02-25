package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.persist.collector.JobHistoryDAO;
import edu.ncsu.las.util.DateUtilities;

/**
 * Represents a particular run of a job.
 * 
 */
public class JobHistory {

	private UUID _jobHistoryID;
	private UUID _jobID;
	private String _jobName;
	private JobHistoryStatus _status;
	private java.sql.Timestamp _startTime;
	private java.sql.Timestamp _endTime;
	private String _comments;
	private String _jobCollector;
	private long   _numPageVisited;
	private long   _totalPageSizeVisited;
	private String _domainInstanceName;
	
	public JobHistory(){
		
	}
	public JobHistory(UUID jobHistoryID, UUID jobID, String jobName, JobHistoryStatus status, String comments,String jobCollector, String domainInstanceName) {
		this._jobHistoryID  = jobHistoryID;
		this._jobID = jobID;
		this._jobName = jobName;
		this._status = status;
		this._comments = comments;
		this._jobCollector = jobCollector;
		this._numPageVisited = 0;
		this._totalPageSizeVisited = 0;
		this._domainInstanceName = domainInstanceName;
	}

	public JobHistory(UUID jobHistoryID, UUID jobID, String jobName, JobHistoryStatus status, Timestamp startTime, Timestamp endTime,	String comments, String jobCollector,
			         long numPageVisited, long totalPageSizeVisited, String domainInstanceName) {
		super();
		this._jobHistoryID = jobHistoryID;
		this._jobID = jobID;
		this._jobName = jobName;
		this._status = status;
		this._startTime = startTime;
		this._endTime = endTime;
		this._comments = comments;
		this._jobCollector = jobCollector;
		this._numPageVisited = numPageVisited;
		this._totalPageSizeVisited = totalPageSizeVisited;
		this._domainInstanceName = domainInstanceName;
	}

	public UUID getJobHistoryID() {
		return _jobHistoryID;
	}

	public UUID getJobID() {
		return _jobID;
	}

	public String getJobName() {
		return _jobName;
	}
	
	public JobHistoryStatus getStatus() {
		return _status;
	}

	public java.sql.Timestamp getStartTime() {
		return _startTime;
	}

	public java.sql.Timestamp getEndTime() {
		return _endTime;
	}
	
	/** 
	 * Produces the elapsed time in the format of hh:mm:ss
	 * @return
	 */
	public String getProcessingTime() {
		java.sql.Timestamp start = this.getStartTime();
		java.sql.Timestamp end   = this.getEndTime();
		
		long startMillis;
		long endMillis;
		
		if (start != null) { startMillis = start.getTime();
		} else {  			 startMillis = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()*1000;}
		if (end != null) { endMillis = end.getTime();
		} else {  		   endMillis = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()*1000;}
		
		long millis = endMillis-startMillis;
		
		return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
			    TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
			    TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
		
	}

	public String getComments() {
		return _comments;
	}
	
	public void setJobID(UUID id) {
		_jobHistoryID = id;
	}
	
	public void setStartTime(java.sql.Timestamp ts) {
		_startTime = ts;
	}
	
	public void setEndTime(java.sql.Timestamp ts) {
		_endTime = ts;
	}
	
	public String getJobCollector() {
		return _jobCollector;
	}
	
	public void setJobCollector(String newJobCollector) {
		_jobCollector = newJobCollector;
	}	
	
	public long getNumPageVisited() {
		return _numPageVisited;
	}
	public void setNumPageVisited(long newNumberPages) {
		_numPageVisited = newNumberPages;
	}
	
	public synchronized void incrementNumPageVisited() {
		_numPageVisited++;
	}
	
	public long getTotalPageSizeVisited() {
		return _totalPageSizeVisited;
	}
	
	public void setTotalPageSizeVisited(long newTotalSize) {
		_totalPageSizeVisited=newTotalSize;
	}
	
	public String getDomainInstanceName() {
		return _domainInstanceName;
	}
	public void setDomainInstanceName(String domainInstanceName) {
		this._domainInstanceName = domainInstanceName;
	}
	public synchronized void incrementTotalPageSize(long pageSize) {
		_totalPageSizeVisited += pageSize;
	}
	
	/**
	 * TODO: complete
	 * @param status
	 * @param comments
	 */
	public void updateStatusAndComments(JobHistoryStatus status, String comments) {
		this._status   = status;
		this._comments = comments;
		(new JobHistoryDAO()).updateRecord(this);
	}
	
	/**
	 * updates the number of pages visited and the total number size of all pages retrieved.
	 */
	public void updateJobStats() {
		(new JobHistoryDAO()).updateJobStats(this);
	}
	
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject().put("jobHistoryID", _jobHistoryID.toString())
				                            .put("jobID", _jobID.toString())
				                            .put("jobName", _jobName)
				                            .put("status", _status.toString())
				                            .put("comments", _comments)
				                            .put("jobCollector", _jobCollector)
				                            .put("numPageVisited", _numPageVisited)
				                            .put("totalPageSizeVisited",_totalPageSizeVisited)
				                            .put("processingTime",this.getProcessingTime())
				                            .put("startTime", DateUtilities.getDateTimeISODateTimeFormat(_startTime.toInstant()));
		
		if (_endTime != null) {
			result.put("endTime", DateUtilities.getDateTimeISODateTimeFormat(_endTime.toInstant()));
		}
		else {
			result.put("endTime","");
		}
		
		return result;
	}
	
	
	/**
	 * TODO complete
	 * @param jobID
	 * @param status
	 * @param comments
	 * @return
	 */
	public static JobHistory initiateJob(UUID jobHistoryID, UUID jobID,  String jobName, JobHistoryStatus status, String comments, String jobCollector, String domainInstanceName) {
		JobHistory jh = new JobHistory(jobHistoryID, jobID, jobName, status, comments, jobCollector, domainInstanceName);
		(new JobHistoryDAO()).insertRecord(jh);
		return jh;
	}
	
	
	public static List<JobHistory> getRecentJobsLimitResultCount(String domainStr, int limit) {
		return (new JobHistoryDAO()).getRecentJobsLimitResultCount(domainStr,limit);
	}

	public static List<JobHistory> getJobHistory(String domainStr, ZonedDateTime startTime,ZonedDateTime endTime, UUID jobID) {
		return (new JobHistoryDAO()).getJobHistory(domainStr, startTime,endTime, jobID);
	}


	public static int updateOrphanedRecords(String collectorID) {
		JobHistoryDAO dd = new JobHistoryDAO();
		return dd.updateOrphanedRecords(collectorID);
	}
	
	public static Integer getRunningNumberOfJobs(String domain) {
		JobHistoryDAO dd = new JobHistoryDAO();
		Integer result = dd.getRunningNumberOfJobs(domain);
		return result;
	}

	
	public static Integer getNumberOfJobs(String domain) {
		JobHistoryDAO dd = new JobHistoryDAO();
		Integer result = dd.getNumberOfJobs(domain);
		return result;
	}
	
	public static JobHistory retrieve(UUID jobHistoryID) {
		JobHistoryDAO jhd = new JobHistoryDAO();
		return jhd.getJobHistoryRecord(jobHistoryID);
	}

	
}
