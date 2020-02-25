package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.UserPassword;
import edu.ncsu.las.persist.DAO;

/**
 * Maps the UserPassword model to the system_user_table
 * 
 *
 */
public class UserPasswordDAO extends DAO {
/*	
system_user_password	
    email_id	  character varying(256) not null,
    password	  character varying(100) not null,
    password_salt character varying(100) not null,
    temporary_access_token character varying (256) not null,
    must_change    boolean not null,
    password_changed_dt timestamp with time zone not null,
    account_locked_until_dt timestamp with time zone not null,
*/

	private static String SELECT = "select email_id, password, password_salt, temporary_access_token, must_change, password_changed_dt, account_locked_until_dt,account_suspended from system_user_password";
	private static String INSERT = "insert into system_user_password (email_id, password, password_salt, temporary_access_token, must_change, password_changed_dt, account_locked_until_dt,account_suspended ) values (?,?,?,?,?, CURRENT_TIMESTAMP::timestamp,?,?)";	
	private static String SELECT_BY_USER = SELECT + " where email_id=?";
	private static String UPDATE_PASSWORD     = "update system_user_password set password=?, password_salt=?, must_change=FALSE, password_changed_dt=CURRENT_TIMESTAMP::timestamp where email_id=?";
	private static String UPDATE_ACCOUNT_LOCK = "update system_user_password set account_locked_until_dt=? where email_id=?";
	private static String UPDATE_TEMP_ACCESS_TOKEN = "update system_user_password set temporary_access_token=? where email_id=?";
	private static String UPDATE_SUSPEND = "update system_user_password set account_suspended=? where email_id=?";
	
	public static class UserPasswordMapper implements RowMapper<UserPassword> {
		public UserPassword mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new UserPassword(rs.getString(1), java.util.Base64.getDecoder().decode(rs.getString(2)), java.util.Base64.getDecoder().decode(rs.getString(3)),
					                rs.getString(4), rs.getBoolean(5), rs.getTimestamp(6).toInstant(), rs.getTimestamp(7).toInstant(), rs.getBoolean(8));
		}
	}
		
	public UserPassword retrieve(String emailID) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_BY_USER,	new UserPasswordMapper(), emailID);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException dae) {
			return null;
		}
	}
	
	public boolean create(UserPassword record) {
		int numRows = this.getJDBCTemplate().update(INSERT, record.getEmailID(), java.util.Base64.getEncoder().encodeToString(record.getPassword()),
				                                            java.util.Base64.getEncoder().encodeToString(record.getsalt()), record.getTemporaryAccessToken(), record.isMustChangePassword(),
				                                            java.sql.Timestamp.from(record.getPasswordAccountLockedUntilDateTime()),record.isAccountSuspended());
		return (numRows == 1);
	}

	/** 
	 * Updates the password using the password and salt values.  The password_changed_dt is set to the current time and the must_change flag becomes false
	 * 
	 * @param record
	 * @return
	 */
	public boolean updatePassword(UserPassword record) {
		int numRows = this.getJDBCTemplate().update(UPDATE_PASSWORD, java.util.Base64.getEncoder().encodeToString(record.getPassword()),
                                                                     java.util.Base64.getEncoder().encodeToString(record.getsalt()),
                                                                     record.getEmailID());
        return (numRows == 1);
	}
	
	public boolean updateAccountLocked(UserPassword record) {
		int numRows = this.getJDBCTemplate().update(UPDATE_ACCOUNT_LOCK, java.sql.Timestamp.from(record.getPasswordAccountLockedUntilDateTime()), record.getEmailID());
        return (numRows == 1);
	}
	
	public boolean updateTemporaryAccessToken(UserPassword record) {
		int numRows = this.getJDBCTemplate().update(UPDATE_TEMP_ACCESS_TOKEN, record.getTemporaryAccessToken(), record.getEmailID());
        return (numRows == 1);
	}
	
	public boolean updateSuspend(UserPassword record) {
		int numRows = this.getJDBCTemplate().update(UPDATE_SUSPEND, record.getEmailID());
        return (numRows == 1);
	}	
		
}
