package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.JobHistory;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.persist.DAO;

public class JobHistoryDAO extends DAO {
	private static final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	
	private static final Logger logger = Logger.getLogger(Collector.class.getName());

	private static final String INSERT_NEW_RECORD = "insert into job_history (job_history_id, job_id, job_name, status,startTime,comments, job_collector, num_pages_visited, total_page_size_visited,domain_instance_name) values (?,?,?,?,CURRENT_TIMESTAMP::timestamp,?,?,?,?,?)";

	private static final String UPDATE_JOB_HISTORY = "update job_history set status=?, endtime=CURRENT_TIMESTAMP::timestamp, comments=?, num_pages_visited=?, total_page_size_visited=? where job_history_id=?";

	private static final String UPDATE_JOB_STATS = "update job_history set num_pages_visited=?, total_page_size_visited=? where job_history_id=?";

	private static final String SELECT_JOB_HISTORY = "select job_history_id, job_id, job_name, status, starttime, endtime, comments, job_collector, num_pages_visited, total_page_size_visited, domain_instance_name from job_history";

	private static final String SELECT_JOBS_LIMIT_ROWS_BY_DOMAIN = SELECT_JOB_HISTORY	+ " where domain_instance_name=? order by starttime desc LIMIT ?";

	private static final String SELECT_JOBS_JOBID = SELECT_JOB_HISTORY + " where job_id = ?";
	
	private static final String SELECT_JOB_HISTORY_ID = SELECT_JOB_HISTORY + " where job_history_id = ?";
	
	private static final String SELECT_JOBS_COUNT_DOMAIN = "select count(*) from job_history where domain_instance_name = ? ";

	private static final String SELECT_RUNNING_JOBS_COUNT_DOMAIN = "select count(*) from job_history where  domain_instance_name = ? and status = 'processing' and job_name not in('DomainDiscoveryHandler','DirectoryWatcher')";
	
	private static final String UPDATE_ORPHANED_RECORDS = "update job_history a set status='killed', endtime=CURRENT_TIMESTAMP::timestamp, comments='collector process died for an unknown reason - examine logs',  num_pages_visited = (select count(*) from visited_pages b where a.job_history_id=b.job_history_id) where (a.status='processing' or a.status='stopping') and a.job_collector=?";

	private static final String DELETE_JOB_HISTORY_BY_DOMAIN = "delete from job_history where domain_instance_name = ?";
	
	private static final String DELETE_JOB_HISTORY_BY_ID = "delete from job_history where job_id = ?";
	
	private static class JobHistoryMapper implements RowMapper<JobHistory> {
		public JobHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new JobHistory((java.util.UUID) rs.getObject(1),
					(java.util.UUID) rs.getObject(2), rs.getString(3),
					JobHistoryStatus.getEnum(rs.getString(4)),
					rs.getTimestamp(5,utcCalendar), rs.getTimestamp(6,utcCalendar), rs.getString(7),
					rs.getString(8), rs.getLong(9),
					rs.getLong(10), rs.getString(11));
		}
	}

	public JobHistory getJobHistoryRecord(UUID jobHistoryID) {
		return  this.getJDBCTemplate().queryForObject(SELECT_JOB_HISTORY_ID, new JobHistoryMapper(), jobHistoryID);
	}
	
	public List<JobHistory> getRecentJobsLimitResultCount(String domainStr, int limit) {
		List<JobHistory> pages = this.getJDBCTemplate().query(	SELECT_JOBS_LIMIT_ROWS_BY_DOMAIN, new JobHistoryMapper(), domainStr, limit);
		return pages;
	}

	public List<JobHistory> getJobHistory(UUID jobId) {
		List<JobHistory> pages;
		pages = this.getJDBCTemplate().query(SELECT_JOBS_JOBID, new JobHistoryMapper(), jobId);
		return pages;
	}
	

	public List<JobHistory> getJobHistory(String domainStr, ZonedDateTime startTime, ZonedDateTime endTime, UUID jobID) {
		List<JobHistory> history = null;

		List<Object> parameters = new java.util.ArrayList<Object>();
		String query = SELECT_JOB_HISTORY + " where domain_instance_name = ? ";
		parameters.add(domainStr);

		
		if (startTime != null) {
			query += " and starttime >= ? ";
			parameters.add(Timestamp.from(startTime.toInstant()));
		}
		if (endTime != null) {
			query += " and endtime <= ? "; 
			parameters.add(Timestamp.from(endTime.toInstant()));
		}
		if (jobID != null) {
			query += " and job_id = ? ";
			parameters.add(jobID);
		}
		logger.log(Level.FINEST, "getJobHistory - query: "+query);
		history = this.getJDBCTemplate().query(query, new JobHistoryMapper(),parameters.toArray());
		return history;
	}

	public boolean insertRecord(JobHistory jh) {
		jh.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
		return (this.getJDBCTemplate().update(INSERT_NEW_RECORD,
				jh.getJobHistoryID(), jh.getJobID(), jh.getJobName(),
				jh.getStatus().toString(), jh.getComments(),
				jh.getJobCollector(),
				jh.getNumPageVisited(), jh.getTotalPageSizeVisited(), jh.getDomainInstanceName()) == 1);
	}

	public boolean updateRecord(JobHistory jh) {
		int numRows = this.getJDBCTemplate().update(UPDATE_JOB_HISTORY,
				jh.getStatus().toString(), jh.getComments(),
				jh.getNumPageVisited(),
				jh.getTotalPageSizeVisited(), jh.getJobHistoryID());
		jh.setEndTime(new java.sql.Timestamp(System.currentTimeMillis()));

		return (numRows == 1);
	}

	public boolean updateJobStats(JobHistory jh) {
		int numRows = this.getJDBCTemplate().update(UPDATE_JOB_STATS,
				jh.getNumPageVisited(), jh.getTotalPageSizeVisited(),
				jh.getJobHistoryID());

		return (numRows == 1);
	}
	
	/**
	 * Marks records that have been orphaned in the database.
	 * 
	 * @param collectorID
	 * @return number of records that were orphaned.
	 */
	public int updateOrphanedRecords(String collectorID) {
		int numRows = this.getJDBCTemplate().update(UPDATE_ORPHANED_RECORDS, collectorID);

		return numRows;
	}

	public int getNumberOfJobs(String domain) {
		Object[] args = new Object[1];
		args[0] = domain;
		int count = this.getJDBCTemplate().queryForObject(SELECT_JOBS_COUNT_DOMAIN, args, Integer.class);

		return count;
	}

	public int getRunningNumberOfJobs(String domain) {
		Object[] args = new Object[1];
		args[0] = domain;
		int count = this.getJDBCTemplate().queryForObject(SELECT_RUNNING_JOBS_COUNT_DOMAIN, args, Integer.class);

		return count;
	}
	
	public int deleteByDomain(String domain) {
		return this.getJDBCTemplate().update(DELETE_JOB_HISTORY_BY_DOMAIN,domain);
	}

	public int deleteByJobID(UUID jobID) {
		return this.getJDBCTemplate().update(DELETE_JOB_HISTORY_BY_ID,jobID);
	}	
	
}