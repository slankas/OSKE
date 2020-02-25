package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.Project;
import edu.ncsu.las.persist.DAO;


/**
 * Maps Project to relational database table and provides necessary CRUD queries.
 * 
 */
public class ProjectDAO extends DAO {
	private static String SELECT = "SELECT id,domain_instance_name, name, status, purpose, key_questions, assumptions, related_urls, date_created, " +
			                                  " date_updated, created_by_user_email_id, updated_by_user_email_id FROM project";
	private static String SELECT_BY_ID     = SELECT + " WHERE id = ?";
	private static String SELECT_BY_DOMAIN = SELECT + " WHERE domain_instance_name=? order by name ";
	private static String DELETE_BY_ID     = "DELETE FROM project WHERE id=? ";
	private static String DELETE_BY_DOMAIN = "DELETE FROM project WHERE domain_instance_name=? ";
	private static String INSERT_RECORD    = "INSERT INTO project (id,domain_instance_name, name, status, purpose, key_questions, assumptions, related_urls, date_created, " + 
	                                                              " date_updated, created_by_user_email_id, updated_by_user_email_id) " + 
			                                                      "VALUES (?,?,?,?,?,(to_json(?::json)),(to_json(?::json)),(to_json(?::json)),?,?,?,?)";
	
    private static String UPDATE_RECORD    = "UPDATE project set name = ?, status = ?, purpose = ?, key_questions = (to_json(?::json)), "+ 
                                                               " assumptions=(to_json(?::json)), related_urls=(to_json(?::json)),  " + 
                                                               " date_updated = ?, updated_by_user_email_id= ? where id = ?"; 
    private static String UPDATE_RECORD_STATUS = "UPDATE project set status = ?, date_updated = ?, updated_by_user_email_id= ? where id = ?"; 

	public static class ProjectRowMapper implements RowMapper<Project> {
		public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Project (( java.util.UUID ) rs.getObject(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
					            new JSONArray(rs.getString(6)), new JSONArray(rs.getString(7)), new JSONArray(rs.getString(8)),
					            rs.getTimestamp(9), rs.getTimestamp(10), rs.getString(11), rs.getString(12));
		}
	}

	public Project retrieve(UUID indexID) {
		List<Project> indexes =  this.getJDBCTemplate().query(SELECT_BY_ID, new ProjectRowMapper(), indexID);
		if (indexes.size() == 0) { return null;}
		else { return indexes.get(0);}
	}

	public List<Project> selectByDomain(String domain) {
		return this.getJDBCTemplate().query(SELECT_BY_DOMAIN, new ProjectRowMapper(), domain);
	}
	
	
	public boolean delete(UUID uuid){
		return (this.getJDBCTemplate().update(DELETE_BY_ID,uuid) == 1);
	}

	public int deleteByDomain(String domain){
		return this.getJDBCTemplate().update(DELETE_BY_DOMAIN,domain);
	}

	
    
	public boolean insert(Project p) {
		int numRows = this.getJDBCTemplate().update(INSERT_RECORD, p.getID(),p.getDomainInstanceName(), p.getName(), p.getStatus(), p.getPurpose(),
				                                                   p.getKeyQuestions().toString(), p.getAssumptions().toString(), p.getRelatedURLs().toString(),
				                                                   p.getDateCreated(), p.getdateUpdated(), p.getCreateUserID(), p.getLastUpdateUserID());
		return (numRows == 1);
	}

	public boolean update(Project p) {
		int numRows = this.getJDBCTemplate().update(UPDATE_RECORD, p.getName(), p.getStatus(), p.getPurpose(),
				                                                   p.getKeyQuestions().toString(), p.getAssumptions().toString(), p.getRelatedURLs().toString(),
				                                                   p.getdateUpdated(), p.getLastUpdateUserID(), p.getID());
		return (numRows == 1);
	}

	public boolean updateStatus(UUID projectID, String newStatus, String userID) {
		int numRows = this.getJDBCTemplate().update(UPDATE_RECORD_STATUS, newStatus, new java.sql.Timestamp(System.currentTimeMillis()), userID, projectID );
		return (numRows == 1);
	}
	
}