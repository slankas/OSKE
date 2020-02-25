package edu.ncsu.las.persist.collector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.model.collector.type.JobStatus;
import edu.ncsu.las.persist.DAO;




public class JobDAO extends DAO {
	
	private static final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	
	private static final Logger logger =Logger.getLogger(Collector.class.getName());
	
	private static final String SELECT_JOBS = "select id, url, source_handler, status, status_dt, latest_job_history_id, owner_email, config, name, latest_job_collector, justification, priority, cron_schedule, random_percent, cron_next_run, domain_instance_name, adjudication_answers, exportData from job ";

	private static final String SELECT_ALL_JOBS_FOR_DOMAIN = SELECT_JOBS  + " where domain_instance_name=? order by name asc";

	private static final String SELECT_JOB = SELECT_JOBS + " where id = ?";
	
	private static final String SELECT_JOB_BY_DOMAIN_AND_NAME = SELECT_JOBS + " where domain_instance_name=? and name = ?";

	private static final String SELECT_ALL_JOBS_FOR_PROCESSING = SELECT_JOBS + " where (status in ('ready')) or (status in ('complete','scheduled') and cron_next_run < now() ) order by priority desc, status_dt";

	private static final String SELECT_ALL_JOBS_TO_STOP = SELECT_JOBS + " where status ='stopping'";

	private static final String UPDATE_STATUS_AND_JOB = "update job set status=?, latest_job_history_id=?, status_dt=CURRENT_TIMESTAMP::timestamp where id=?";
		
	private static final String UPDATE_NEXT_RUN_FOR_JOB = "update job set cron_next_run = ? where id=?";
	
	private static final String UPDATE_COMPLETE_STATUS_TO_READY = "update job set status='ready', status_dt=CURRENT_TIMESTAMP::timestamp where status='complete' and cron_next_run < now())";

	private static final String UPDATE_STATUS = "update job set status=?, status_dt=CURRENT_TIMESTAMP::timestamp where id=?";

	private static final String UPDATE_CONFIGURATION = "update job set config=? where id=?";

	
	private static final String UPDATE_TO_CLAIM_JOB = "update job set status=?, latest_job_history_id=?, latest_job_collector=?, status_dt=CURRENT_TIMESTAMP::timestamp where id=? and status in ('scheduled','complete','ready')";
	
	private static final String UPDATE_JOB = "update job set name = ?, url = ?, source_handler = ?,  priority = ?, "+
	                                         "config = ?, justification = ?, owner_email = ?, status_dt=CURRENT_TIMESTAMP::timestamp, status= ?, cron_schedule=?, random_percent=?, cron_next_run =?, adjudication_answers=?, exportData=? where id = ?";
	
	private static final String INSERT_JOB = "insert into job" +
			 " (id, name, url, source_handler, priority, status, config, justification, status_dt, owner_email, latest_job_collector, cron_schedule, random_percent, cron_next_run, domain_instance_name, adjudication_answers, exportData)" +
			 " values (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP::timestamp,?,'',?,?,?,?,?,?)";

	private static final String INSERT_JOB_ARCHIVE = "insert into job_archive " +
			 " (id, update_dt, user_email, name, url, source_handler, priority, status, config, justification, status_dt, owner_email, latest_job_collector, cron_schedule, random_percent, cron_next_run, domain_instance_name, adjudication_answers, exportData)" +
			 " select id, CURRENT_TIMESTAMP::timestamp, ?, name, url, source_handler, priority, status, config, justification, status_dt, owner_email, latest_job_collector, cron_schedule, random_percent, cron_next_run, domain_instance_name, adjudication_answers, exportData from job where id = ?";
	
	private static final String INSERT_JOB_STATUS_HISTORY = "insert into job_status_history (id, status_dt, status, operator_email,domain_instance_name) values (?,CURRENT_TIMESTAMP::timestamp,?,?,?)";
	
	private static final String UPDATE_ORPHANED_JOBS = "update job set status='errored' where (status='stopping' or status='processing') and latest_job_collector=?";
	
	private static final String SELECT_JOBS_COUNT_BY_DOMAIN_AND_STATUS = "select count(*) from job where domain_instance_name=? and status = ?";

	private static final String SELECT_JOB_BY_STATUS = SELECT_JOBS + " where status=?";
	
	private static final String SELECT_JOB_BY_DOMAIN_AND_STATUS = SELECT_JOBS + " where domain_instance_name=? and status=?  ";
	
	private static final String SELECT_JOB_BY_DOMAIN_AND_SOURCE_HANDLER = SELECT_JOBS + " where domain_instance_name=? and source_handler=?";
	
	private static final String UPDATE_JUSTIFICATION = "update job set justification=? where id=?";
	
	private static final String DELETE_JOB_BY_DOMAIN   = "delete from job where domain_instance_name=?";

	private static final String DELETE_JOB_BY_ID   = "delete from job where id=?";
	private static final String DELETE_JOB_ARCHIVE_BY_ID   = "delete from job_archive where id=?";

	
	public boolean insertJob(Job job, String user) {

		int numrows = this.getJDBCTemplate().update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(INSERT_JOB);
				ps.setObject(1, job.getID());
				ps.setString(2, job.getName());
				ps.setString(3, job.getPrimaryFieldValue());
				ps.setString(4, job.getSourceHandler());
				ps.setInt(5, job.getPriority());
				ps.setString(6, job.getStatus().toString());
				ps.setString(7, job.getConfiguration().toString());
				ps.setString(8, job.getJustification());
				ps.setString(9, job.getOwnerEmail());
				ps.setString(10, job.getCronSchedule());
				ps.setInt(11, job.getRandomPercent());
				ps.setTimestamp(12, job.getNextRun());
				ps.setString(13, job.getDomainInstanceName());
				ps.setString(14,  job.getAdjudicationAnswers().toString());
				ps.setBoolean(15, job.getExportData());
				return ps;
			}
		});
		
		if (numrows == 1) {
			job.setStatusTimestamp(Timestamp.from(Instant.now()));
			return createArchiveRecord(job.getID(), user);
		}
		else {
			return false;
		}
	}

	private boolean createArchiveRecord(java.util.UUID jobID, String userEmail) {
		int numrows = this.getJDBCTemplate().update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(INSERT_JOB_ARCHIVE);
				ps.setString(1, userEmail);
				ps.setObject(2, jobID);
				return ps;
			}
		});
		
		return numrows == 1;		
		
		
		
	}
	
	private static class JobRowMapper implements RowMapper<Job> {
		public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Job(
					( java.util.UUID ) rs.getObject(1), 
					rs.getString(2),
					rs.getString(3), 
					JobStatus.getEnum(rs.getString(4)), 
					rs.getTimestamp(5, utcCalendar), 
					( java.util.UUID ) rs.getObject(6), 
					rs.getString(7),
					new JSONObject(rs.getString(8)), 
					rs.getString(9),
					rs.getString(10), 
					rs.getString(11), 
					rs.getInt(12), 
					rs.getString(13), 
					rs.getInt(14), 
					rs.getTimestamp(15,utcCalendar), 
					rs.getString(16), 
					rs.getString(17),
					rs.getBoolean(18)
				);
		}
	}
		
	public List<Job> getAllJobs(String domainStr){		
		logger.log(Level.INFO, "grabbing all jobs");
		return this.getJDBCTemplate().query(SELECT_ALL_JOBS_FOR_DOMAIN, new JobRowMapper(), domainStr);
	}
	
	public Job getJob(UUID jobUUID) {
		return  this.getJDBCTemplate().queryForObject(SELECT_JOB, new JobRowMapper(), jobUUID);
	}
	
	public Job getJob(String domainInstanceName, String jobName) {
		List<Job> jobs =  this.getJDBCTemplate().query(SELECT_JOB_BY_DOMAIN_AND_NAME, new JobRowMapper(), domainInstanceName, jobName);
		if (jobs.size() == 0) { return null;}
		else { return jobs.get(0);}
	}	
	

	public List<Job> getJobsAvailableForProcessing(){		
		logger.log(Level.INFO, "grabbing all jobs for processing");
		return this.getJDBCTemplate().query(SELECT_ALL_JOBS_FOR_PROCESSING, new JobRowMapper());
	}
	
	public List<Job> getJobsToStop(){		
		logger.log(Level.INFO, "grabbing all jobs to stop");
		return this.getJDBCTemplate().query(SELECT_ALL_JOBS_TO_STOP, new JobRowMapper());
	}	

	public boolean claimJobToRun(Job job) {
		int numRows = this.getJDBCTemplate().update(UPDATE_TO_CLAIM_JOB, job.getStatus().toString(),job.getLatestJobID(),job.getLatestJobCollector(),job.getID());
		job.setStatusTimestamp( new java.sql.Timestamp(System.currentTimeMillis()));  //may be off a little bit from the database, but for practical purposes in looking at listings, this will be fine.
		
		if (numRows == 1) {
			return insertJobStatusHistory(job, job.getLatestJobCollector());
		}
		else {
			return false;
		}
	}
	
	public boolean updateStatusAndJob(Job job, String user) {
		int numrows = this.getJDBCTemplate().update(UPDATE_STATUS_AND_JOB, job.getStatus().toString(),job.getLatestJobID(),job.getID());
		job.setStatusTimestamp( new java.sql.Timestamp(System.currentTimeMillis()));  //may be off a little bit from the database, but for practical purposes in looking at listings, this will be fine.
		
		if (numrows == 1) {
			return insertJobStatusHistory(job, user);
		}
		else {
			return false;
		}
	}

	public int updateOrphanedJobs(String collectorID) {
		int numRows = this.getJDBCTemplate().update(UPDATE_ORPHANED_JOBS,collectorID);
		return numRows;
	}
	
	
	public boolean editJob(Job job, String user) {
		int numrows = this.getJDBCTemplate().update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(UPDATE_JOB);
				ps.setString(1, job.getName());
				ps.setString(2, job.getPrimaryFieldValue());
				ps.setString(3, job.getSourceHandler());
				ps.setInt(4, job.getPriority());
				ps.setString(5, job.getConfiguration().toString());
				ps.setString(6, job.getJustification());
				ps.setString(7, job.getOwnerEmail());
				ps.setString(8, job.getStatus().toString()); // Added for Adjudication.
				ps.setString(9, job.getCronSchedule());
				ps.setInt(10, job.getRandomPercent());
				ps.setTimestamp(11, job.getNextRun());
				ps.setString(12, job.getAdjudicationAnswers().toString());
				ps.setObject(13, job.getExportData());
				ps.setObject(14, job.getID());
				
				return ps;
			}
		});
		
		if (numrows == 1) {
			return createArchiveRecord(job.getID(), user);
		}
		else {
			return false;
		}
	}

	public boolean updateStatus(Job job, String user) {
		int numRows = this.getJDBCTemplate().update(UPDATE_STATUS, job.getStatus().toString(), job.getID());
		
		job.setStatusTimestamp( new java.sql.Timestamp(System.currentTimeMillis()));  //may be off a little bit from the database, but for practical purposes in looking at listings, this will be fine.
		
		if (numRows == 1) {
			return insertJobStatusHistory(job, user);
		}
		else {
			return false;
		}
	}
	
	public boolean insertJobStatusHistory(Job job, String user) {
		int numrows = this.getJDBCTemplate().update(INSERT_JOB_STATUS_HISTORY,job.getID(), job.getStatus().toString(), user,job.getDomainInstanceName());
		
		return (numrows == 1);
	}

	public Integer getNumberOfAdjudicationJobsForDomain(String domain, String status) {
		Object[] args = new Object[2];
		args[0] = domain;
		args[1] = status;
		int count = this.getJDBCTemplate().queryForObject(SELECT_JOBS_COUNT_BY_DOMAIN_AND_STATUS, args, Integer.class);
		return count;
	}	
	
	
	public List<Job> getJobsByDomainAndStatus(String domain, String status){		
		logger.log(Level.INFO, "grabbing all jobs by domain and status");
		return this.getJDBCTemplate().query(SELECT_JOB_BY_DOMAIN_AND_STATUS, new JobRowMapper(), domain,status);
	}
	
	public List<Job> getJobsByStatus(String status){		
		logger.log(Level.INFO, "grabbing all jobs by status");
		return this.getJDBCTemplate().query(SELECT_JOB_BY_STATUS, new JobRowMapper(), status);
	}

	public boolean updateJustification(Job job) {
		return (this.getJDBCTemplate().update(UPDATE_JUSTIFICATION, job.getJustification(), job.getID()) == 1);
	}
	
	/**
	 * This method will take complete jobs whose time interval has passed and set them to ready to run.
	 * 
	 * @return
	 */
	public int moveCompletedJobsToReadyToRun() {
		return this.getJDBCTemplate().update(UPDATE_COMPLETE_STATUS_TO_READY);		
	}

	public boolean updateNextRun(Job job) {
		logger.log(Level.FINEST, "updateNextRun - job.getNextRun = "+job.getNextRun());
		return (this.getJDBCTemplate().update(UPDATE_NEXT_RUN_FOR_JOB, job.getNextRun(), job.getID()) == 1);
	}

	public List<Job> getAllJobsBySourceHandler(String domainInstanceName,String sourceHandlerName) {
		logger.log(Level.FINEST, "grabbing all jobs by source_handler and domain");
		return this.getJDBCTemplate().query(SELECT_JOB_BY_DOMAIN_AND_SOURCE_HANDLER, new JobRowMapper(), domainInstanceName, sourceHandlerName);
	}

	public boolean updateConfiguration(Job job, String user) {
		int numRows = this.getJDBCTemplate().update(UPDATE_CONFIGURATION, job.getConfiguration().toString(), job.getID());

		if (numRows == 1) {
			return createArchiveRecord(job.getID(), user);
		}
		else {
			return false;
		}
	}
	
	public int deleteRecordByDomain(String domain) {
		return this.getJDBCTemplate().update(DELETE_JOB_BY_DOMAIN,domain);
	}
	
	public int deleteJobByID(UUID jobID) {
		return this.getJDBCTemplate().update(DELETE_JOB_BY_ID,jobID);
	}
	
	public int deleteJobArchiveByID(UUID jobID) {
		return this.getJDBCTemplate().update(DELETE_JOB_ARCHIVE_BY_ID,jobID);
	}
	
}