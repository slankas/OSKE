package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.ProjectDocument;
import edu.ncsu.las.persist.DAO;


/**
 * Maps project_document to relational database table and provides necessary CRUD queries.
 * 
 */
public class ProjectDocumentDAO extends DAO {
	private static String SELECT = "SELECT id,domain_instance_name, name, status, contents, date_created, " +
			                                  " date_updated, created_by_user_email_id, updated_by_user_email_id FROM project_document";
	private static String SELECT_BY_ID     = SELECT + " WHERE id = ?";
	//private static String SELECT_BY_DOMAIN = SELECT + " WHERE domain_instance_name=? order by name ";
	
	private static String SELECT_WITHOUT_CONTENTS = "SELECT id,domain_instance_name, name, status,'', date_created, " +
            " date_updated, created_by_user_email_id, updated_by_user_email_id FROM project_document";	
	private static String SELECT_WITHOUT_CONTENTS_BY_DOMAIN = SELECT_WITHOUT_CONTENTS + " WHERE domain_instance_name=? order by name ";
	
	private static String SELECT_BY_DOMAIN_AND_NAME = SELECT + " WHERE domain_instance_name=? and name = ? ";
	private static String DELETE_BY_ID     = "DELETE FROM project_document WHERE id=? ";
	private static String DELETE_BY_DOMAIN = "DELETE FROM project_document WHERE domain_instance_name=? ";
	private static String INSERT_RECORD    = "INSERT INTO project_document (id,domain_instance_name, name, status, contents, date_created, " + 
	                                                              "         date_updated, created_by_user_email_id, updated_by_user_email_id) " + 
			                                                      "VALUES (?,?,?,?,?,?,?,?,?)";
	
    private static String UPDATE_RECORD    = "UPDATE project_document set name = ?, status = ?, contents = ?,  " + 
                                                               " date_updated = ?, updated_by_user_email_id= ? where id = ?"; 
    private static String UPDATE_RECORD_STATUS = "UPDATE project_document set status = ?, date_updated = ?, updated_by_user_email_id= ? where id = ?"; 

	public static class ProjectDocumentRowMapper implements RowMapper<ProjectDocument> {
		public ProjectDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new ProjectDocument (( java.util.UUID ) rs.getObject(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), 
					            rs.getTimestamp(6), rs.getTimestamp(7), rs.getString(8), rs.getString(9));
		}
	}

	public ProjectDocument retrieve(UUID indexID) {
		List<ProjectDocument> indexes =  this.getJDBCTemplate().query(SELECT_BY_ID, new ProjectDocumentRowMapper(), indexID);
		if (indexes.size() == 0) { return null;}
		else { return indexes.get(0);}
	}

	public List<ProjectDocument> selectByDomain(String domain) {
		return this.getJDBCTemplate().query(SELECT_WITHOUT_CONTENTS_BY_DOMAIN, new ProjectDocumentRowMapper(), domain);
	}
	
	public List<ProjectDocument> selectByDomainAndName(String domain, String name) {
		return this.getJDBCTemplate().query(SELECT_BY_DOMAIN_AND_NAME, new ProjectDocumentRowMapper(), domain, name);
	}
		
	public boolean delete(UUID uuid){
		return (this.getJDBCTemplate().update(DELETE_BY_ID,uuid) == 1);
	}

	public int deleteByDomain(String domain){
		return this.getJDBCTemplate().update(DELETE_BY_DOMAIN,domain);
	}

	
    
	public boolean insert(ProjectDocument p) {
		int numRows = this.getJDBCTemplate().update(INSERT_RECORD, p.getID(),p.getDomainInstanceName(), p.getName(), p.getStatus(), p.getContents(),
				                                                   p.getDateCreated(), p.getdateUpdated(), p.getCreateUserID(), p.getLastUpdateUserID());
		return (numRows == 1);
	}

	public boolean update(ProjectDocument p) {
		int numRows = this.getJDBCTemplate().update(UPDATE_RECORD, p.getName(), p.getStatus(), p.getContents(),
				                                                   p.getdateUpdated(), p.getLastUpdateUserID(), p.getID());
		return (numRows == 1);
	}

	public boolean updateStatus(UUID projectDocumentID, String newStatus, String userID) {
		int numRows = this.getJDBCTemplate().update(UPDATE_RECORD_STATUS, newStatus, new java.sql.Timestamp(System.currentTimeMillis()), userID, projectDocumentID );
		return (numRows == 1);
	}
	
}