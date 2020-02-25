package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;


import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.VisitedPage;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.persist.DAO;

public class VisitedPageDAO extends DAO {
	//private static final Logger logger = Logger.getLogger(Collector.class.getName());

	private static final String INSERT_NEW_RECORD = "INSERT into visited_pages (id, job_history_id, job_id, url, visited_ts, mime_type, storage_uuid, sha256_hash, status, related_id,full_text_send_result,domain_instance_name, storage_area) VALUES (?,?, ?, ?, CURRENT_TIMESTAMP::timestamp, ?, ?, ?, ?, ?,?,?,?)";

	private static final String SELECT = "SELECT id,job_history_id, job_id, url, visited_ts, mime_type, storage_uuid, sha256_hash, status, related_id,full_text_send_result,domain_instance_name, storage_area  FROM visited_pages ";
	
	private static final String DELETE_BY_DOMAIN = "DELETE from visited_pages where domain_instance_name = ?";
	private static final String DELETE_BY_JOB_ID = "DELETE from visited_pages where job_id = ?";
	
	private String SELECT_MOST_RECENT_MATCH_BY_HASH = SELECT + " WHERE sha256_hash=? and domain_instance_name=? and storage_area=? order by visited_ts desc";

	//private String SELECT_VISITED_PAGES = SELECT + " order by visited_ts desc";
	//
	private String SELECT_VISITED_PAGES_BY_DOMAIN_AREA_LIMIT_ROWS = SELECT + " where domain_instance_name = ? and storage_area=? order by id desc  LIMIT ?";
	private String SELECT_VISITED_PAGES_BY_JOB_HISTORY_ID = SELECT + " where job_history_id=? order by visited_ts desc";

	private String SELECT_VISITED_PAGES_BY_ID = SELECT + " where id=?";
	private String SELECT_VISITED_PAGES_BY_STORAGE_ID = SELECT + " where storage_uuid=? limit 1";

	private String SELECT_NUMBER_VISITED_PAGES_BY_DOMAIN_AREA = "select count(*) from visited_pages where domain_instance_name=? and storage_area=?";

	private static class VisitedPageMapper implements RowMapper<VisitedPage> {
		public VisitedPage mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new VisitedPage((java.util.UUID) rs.getObject(1), (java.util.UUID) rs.getObject(2), (java.util.UUID) rs.getObject(3),  rs.getString(4),
					rs.getTimestamp(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
					(java.util.UUID) rs.getObject(10), rs.getString(11), rs.getString(12), rs.getString(13));
		}
	}

	public List<VisitedPage> getVisitedPagesLimitRecords(String domain, String storageArea, int numRecords) {
		List<VisitedPage> pages = this.getJDBCTemplate().query(SELECT_VISITED_PAGES_BY_DOMAIN_AREA_LIMIT_ROWS, new VisitedPageMapper(),domain, storageArea, numRecords);
		return pages;
	}
	
	public List<VisitedPage> getVisitedPagesByJobHistoryID(java.util.UUID jobHistoryID) {
		List<VisitedPage>  pages = null;
		pages = this.getJDBCTemplate().query(SELECT_VISITED_PAGES_BY_JOB_HISTORY_ID, new VisitedPageMapper(),jobHistoryID);
		return pages;
	}
	


	public List<VisitedPage> getVisitedPagesByDate(String domainInstanceName, ZonedDateTime startDate, ZonedDateTime endDate, java.util.UUID jobID) {
		List<VisitedPage>  pages = null;
		
		List<Object> parameters = new java.util.ArrayList<Object>();
		parameters.add(domainInstanceName);
		String query = SELECT + " where domain_instance_name = ? ";
		if (startDate != null) {
			query += " and visited_ts >= ? ";
			parameters.add(Timestamp.from(startDate.toInstant()));
		}
		if (endDate != null) {
			query += " and visited_ts <= ?";
			parameters.add(Timestamp.from(endDate.toInstant()));
		}
		
		if (jobID != null) {
			query += " and job_id = ?";
			parameters.add(jobID);
		}
		pages = this.getJDBCTemplate().query(query, new VisitedPageMapper(),parameters.toArray());
		return pages;
	}	
	
	public VisitedPage getVisitedPageForId(java.util.UUID id) {
		return this.getJDBCTemplate().queryForObject(SELECT_VISITED_PAGES_BY_ID, new VisitedPageMapper(),id);
	}
	
	public VisitedPage getVisitedPageForStorageId(java.util.UUID id) {
		return this.getJDBCTemplate().queryForObject(SELECT_VISITED_PAGES_BY_STORAGE_ID, new VisitedPageMapper(),id.toString());
	}

	public VisitedPage findMostRecentRecord(String hashValue, String domainInstanceName, FileStorageAreaType fileStorageAreaType) {
		List<VisitedPage> tempResults = this.getJDBCTemplate().query(SELECT_MOST_RECENT_MATCH_BY_HASH,	new VisitedPageMapper(), hashValue, domainInstanceName, fileStorageAreaType.getLabel());

		if (tempResults.size() == 0) {
			return null;
		} else {
			return tempResults.get(0);
		}
	}

	/**
	 * 
	 * Side effect: the ID and timestamp for the vp record is updated based upon
	 * the values inserted into the database.
	 * 
	 * @param vp
	 * @return true if the record was successfully inserted. VP record is also
	 *         updated.
	 */
	public boolean insertRecord(VisitedPage vp) {
		
		int numRows = this.getJDBCTemplate().update(INSERT_NEW_RECORD, vp.getID(), vp.getJobHistoryID(),vp.getJobID(), vp.getURL(),
				vp.getMimeType(), vp.getStorageUUID(), vp.getSHA256Hash(), vp.getStatus(), vp.getRelatedID(),
				vp.getFullTextSendResult(),vp.getDomainInstanceName(), vp.getStorageArea());
		vp.setVisitedTimeStamp(new java.sql.Timestamp(System.currentTimeMillis())); // may be off a little bit from the database,but for practical purposes in looking at listings, this will be fine.

		return (numRows == 1);
	}
	
	public int getNumberOfPagesVisited(String domain, String storageArea) {
		Object[] args = new Object[2];
		args[0] = domain;
		args[1] = storageArea;
		int count = this.getJDBCTemplate().queryForObject(SELECT_NUMBER_VISITED_PAGES_BY_DOMAIN_AREA, args, Integer.class);
		
		return count;
	}
	
	public int deleteByDomain(String domain) {
		return this.getJDBCTemplate().update(DELETE_BY_DOMAIN,domain);
	}
	
	public int deleteByJobID(UUID jobID) {
		return this.getJDBCTemplate().update(DELETE_BY_JOB_ID,jobID);
	}	
	
}