package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.DomainDiscoveryExecution;
import edu.ncsu.las.model.collector.DomainDiscoverySession;
import edu.ncsu.las.persist.DAO;


public class DomainDiscoveryDAO extends DAO {
	
	private static String INSERT_SESSION_ROW = "INSERT INTO domain_discovery_session (session_id,domain_instance_name,session_name,user_id,created_dt,last_activity_dt) "+ "VALUES (?,?,?,?,?,?)";
	private static String INSERT_EXECUTION_ROW = "INSERT INTO domain_discovery_session_execution (session_id,domain_instance_name,execution_number,search_terms,user_id,max_number_search_result,search_api,advanced_configuration,execution_start_dt,document_index_id,shouldtranslate,source_language,search_terms_translated) "+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

	private static String SELECT_SESSION = "SELECT session_id,domain_instance_name, session_name,user_id,created_dt,last_activity_dt from domain_discovery_session ";
	private static String SELECT_SESSION_BY_ID = SELECT_SESSION + " where session_id = ?";
	private static String SELECT_ALL_SESSIONS_IN_DOMAIN = SELECT_SESSION +" where domain_instance_name = ? order by session_name";
	private static String SELECT_SESSIONS_BY_NAME_DOMAIN = SELECT_SESSION +" where domain_instance_name = ? and session_name = ?";
	
	private static String UPDATE_SESSION_LAST_ACTIVITY_DATETIME = "update domain_discovery_session set last_activity_dt=? where session_id=?";
	private static String UPDATE_SESSION_NAME = "update domain_discovery_session set session_name=? where session_id=?";
	
	private static String SELECT_EXECUTION = "SELECT session_id, domain_instance_name, execution_number, search_terms, user_id, max_number_search_result, search_api, advanced_configuration, execution_start_dt, execution_end_dt, document_index_id, shouldtranslate, source_language, search_terms_translated FROM domain_discovery_session_execution";
	private static String SELECT_EXECUTION_BY_SESSION_ID = SELECT_EXECUTION +" where session_id = ? order by execution_number";
	private static String SELECT_EXECUTION_BY_SESSION_ID_AND_NUMBER = SELECT_EXECUTION +" where session_id = ? and execution_number = ?";

	private static String UPDATE_EXECUTION_END_TIME = "update domain_discovery_session_execution set execution_end_dt=? where session_id=? and execution_number = ?";

	private static String DELETE_EXECUTION_BY_SESSION = "delete from domain_discovery_session_execution where session_id=?";
	private static String DELETE_SESSION_BY_SESSION   = "delete from domain_discovery_session where session_id=?";
	private static String DELETE_SESSION_BY_DOMAIN   = "delete from domain_discovery_session where domain_instance_name=?";
	private static String DELETE_SESSION_EXECUTION_BY_DOMAIN   = "delete from domain_discovery_session_execution where domain_instance_name=?";
	
	
	
	public static class DomainDiscoverySessionRowMapper implements RowMapper<DomainDiscoverySession> {
		public DomainDiscoverySession mapRow(ResultSet rs,int rowNum) throws SQLException {
			return new DomainDiscoverySession(( java.util.UUID ) rs.getObject(1),rs.getString(2), rs.getString(3),rs.getString(4),rs.getTimestamp(5),rs.getTimestamp(6));
		}
		
	}
	
	public static class DomainDiscoveryExecutionRowMapper implements RowMapper<DomainDiscoveryExecution> {
		public DomainDiscoveryExecution mapRow(ResultSet rs,int rowNum) throws SQLException {
			String advConfig = rs.getString(8);
			JSONObject advObject = advConfig == null ? new JSONObject() : new JSONObject(advConfig);
			return new DomainDiscoveryExecution(( java.util.UUID ) rs.getObject(1),rs.getString(2), rs.getInt(3),rs.getString(4),rs.getString(5),rs.getInt(6),rs.getString(7), advObject,rs.getTimestamp(9).toInstant(),rs.getTimestamp(10) != null ?rs.getTimestamp(10).toInstant(): null, ( java.util.UUID ) rs.getObject(11), rs.getBoolean(12), rs.getString(13), rs.getString(14));
		}
	}
	
	public DomainDiscoverySession retrieveSession(UUID sessionID) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_SESSION_BY_ID, new DomainDiscoverySessionRowMapper(), sessionID);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	public boolean insertSession(DomainDiscoverySession d){
		int numRows = this.getJDBCTemplate().update(INSERT_SESSION_ROW,d.getSessionID(),d.getDomainInstanceName(),d.getSessionName(), d.getUserID(), d.getCreationDateTimeAsTimeStamp(), d.getLastActivityDateTimeAsTimeStamp());
		return (numRows == 1);
	}
	
	public List<DomainDiscoverySession> retrieveAllSessionsForDomain(String domain){
		return this.getJDBCTemplate().query(SELECT_ALL_SESSIONS_IN_DOMAIN, new DomainDiscoverySessionRowMapper(), domain);
	}

	public List<DomainDiscoverySession> retrieveAllSessionsByDomainAndName(String domain, String name){
		return this.getJDBCTemplate().query(SELECT_SESSIONS_BY_NAME_DOMAIN, new DomainDiscoverySessionRowMapper(), domain,name);
	}

	public boolean insertSessionExecution(DomainDiscoveryExecution d, String userID){
		int numRows = this.getJDBCTemplate().update(INSERT_EXECUTION_ROW,d.getSessionID(),d.getDomainInstanceName(),d.getExecutionNumber(),d.getSearchTerms(), userID,d.getNumberOfSearchResults(), d.getSearchAPI(), d.getAdvancedConfiguration().toString(), Timestamp.from(d.getStartTime()), d.getDocumentIndexID(), d.shouldTranslate(), d.getSourceLanguage(), d.getSearchTermsTranslated());
		return (numRows == 1);
	}

	
	public List<DomainDiscoveryExecution> retrieveAllExecutionsForSession(UUID sessionID) {
		return this.getJDBCTemplate().query(SELECT_EXECUTION_BY_SESSION_ID, new DomainDiscoveryExecutionRowMapper(),sessionID);
	}
	
	public DomainDiscoveryExecution retrieveExecution(UUID sessionID, int executionNumber) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_EXECUTION_BY_SESSION_ID_AND_NUMBER, new DomainDiscoveryExecutionRowMapper(), sessionID, executionNumber);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException e) {
			return null;
		}		
	}
	
	
	public boolean updateExecutionEndTime(DomainDiscoveryExecution dde) {
		int numRows = this.getJDBCTemplate().update(UPDATE_EXECUTION_END_TIME, Timestamp.from(dde.getEndTime()),  dde.getSessionID(),dde.getExecutionNumber());
		return numRows == 1;
	}

	public int deleteDiscoverySessionExecutions(UUID sessionID) {
		int numRows = this.getJDBCTemplate().update(DELETE_EXECUTION_BY_SESSION, sessionID);
		return numRows;
	}	
	
	public int deleteDiscoverySession(UUID sessionID) {
		int numRows = this.getJDBCTemplate().update(DELETE_SESSION_BY_SESSION, sessionID);
		return numRows;
	}	
	
	public int deleteDiscoverySessionByDomain(String domain) {
		this.getJDBCTemplate().update(DELETE_SESSION_EXECUTION_BY_DOMAIN,domain);
		return this.getJDBCTemplate().update(DELETE_SESSION_BY_DOMAIN,domain); 
	}

	public boolean updateLatestActivityDate(UUID sessionID, Timestamp lastActivityDateTime) {
		int numRows = this.getJDBCTemplate().update(UPDATE_SESSION_LAST_ACTIVITY_DATETIME, lastActivityDateTime, sessionID);
		return numRows == 1;
	}
	
	public boolean updateSessionName(UUID sessionID, String newName) {
		int numRows = this.getJDBCTemplate().update(UPDATE_SESSION_NAME, newName, sessionID);
		return numRows == 1;
	}	
}
