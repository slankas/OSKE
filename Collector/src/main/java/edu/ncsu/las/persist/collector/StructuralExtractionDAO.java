package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.StructuralExtractionRecord;
import edu.ncsu.las.persist.DAO;

public class StructuralExtractionDAO extends DAO {

	private static String SELECT = "SELECT 	id, domain_instance_name, hostname, pathregex , record_name, record_selector, record_extract_by, record_extract_regex, record_parent_id, user_email_id,last_database_ts from structural_extraction";
	private static String SELECT_ALL_FOR_DOMAIN = SELECT + " WHERE domain_instance_name = ?";
	private static String SELECT_BY_ID = SELECT + " WHERE id = ?";
	

	private static String DELETE_BY_ID  = "DELETE FROM structural_extraction WHERE id=? ";
	private static String INSERT_ROW = "INSERT INTO structural_extraction (id, domain_instance_name, hostname, pathregex , record_name, record_selector, record_extract_by, record_extract_regex,  record_parent_id, user_email_id,last_database_ts) VALUES (?,?,?,?,?,?,?,?,?,?,now())";

	private static String UPDATE_ROW = "UPDATE structural_extraction  set hostname = ?,  pathregex = ?, record_name = ?, record_selector = ?, record_extract_by =?, record_extract_regex =?, record_parent_id = ?, user_email_id = ?, last_database_ts = now()  WHERE id = ?";
	
	
	public static class ContentExtractionRowMapper implements RowMapper<StructuralExtractionRecord> {
		public StructuralExtractionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new StructuralExtractionRecord(( java.util.UUID ) rs.getObject(1),rs.getString(2),rs.getString(3),rs.getString(4), rs.getString(5), rs.getString(6),rs.getString(7),rs.getString(8), ( java.util.UUID ) rs.getObject(9),rs.getString(10), rs.getTimestamp(11));
		}
	}
	
	public boolean insert(StructuralExtractionRecord c){
		int numRows = this.getJDBCTemplate().update(INSERT_ROW,c.getId(), c.getDomainInstanceName(), c.getHostname(), c.getPathRegex(), c.getRecordName(), c.getRecordSelector(),  c.getRecordExtractBy(), c.getRecordExtractRegex(), c.getRecordParentID(), c.getUserEmailID()  );
		return (numRows == 1);
	}
	
	public boolean update(StructuralExtractionRecord c){
		int numRows = this.getJDBCTemplate().update(UPDATE_ROW,c.getHostname(), c.getPathRegex(), c.getRecordName(), c.getRecordSelector(), c.getRecordExtractBy(), c.getRecordExtractRegex(), c.getRecordParentID(), c.getUserEmailID(), c.getId());
		return (numRows == 1);
	}
	
	public List<StructuralExtractionRecord> selectAll(String domainInstanceName){
		return this.getJDBCTemplate().query(SELECT_ALL_FOR_DOMAIN, new ContentExtractionRowMapper(), domainInstanceName);
	}
	

	public int delete(UUID uuid){
		return this.getJDBCTemplate().update(DELETE_BY_ID,uuid);
	}


	/**
	 * Retrieves the record based upon the given ID.  If found, and there is only one result, then that record is returned
	 * Otherwise, null is returned.
	 * 
	 * @param id
	 * @return
	 */
	public StructuralExtractionRecord retrieve(UUID id) {
		List<StructuralExtractionRecord> records = this.getJDBCTemplate().query(SELECT_BY_ID, new ContentExtractionRowMapper(), id);
		
		if (records.size() == 1) {
			return records.get(0);
		}
		
		return null;
	}
	
	

	
}