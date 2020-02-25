package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.UserOption;
import edu.ncsu.las.model.collector.UserOptionName;
import edu.ncsu.las.persist.DAO;

/**
 * Maps the UserOption model to the system_user_option table
 * 
 */
public class UserOptionDAO extends DAO {

	private static String SELECT = "select email_id, domain_instance_name, option_name, option_value, status_dt from system_user_option";
	private static String UPSERT = "insert into system_user_option as suo (email_id, domain_instance_name, option_name, option_value, status_dt) values (?,?,?,?, CURRENT_TIMESTAMP::timestamp) ON CONFLICT ON CONSTRAINT system_user_option_pkey DO UPDATE set option_value= EXCLUDED.option_value, status_dt=CURRENT_TIMESTAMP::timestamp where suo.email_id=EXCLUDED.email_id and suo.domain_instance_name=EXCLUDED.domain_instance_name and suo.option_name= EXCLUDED.option_name";	
//	private static String SELECT_BY_USER = SELECT + " where email_id=?";
	private static String SELECT_BY_USER_DOMAIN_OPTION = SELECT + " where email_id=? and domain_instance_name=? and option_name=?";
	//private static String UPDATE_OPTION    = "update system_user_option set option_value=?, status_dt=CURRENT_TIMESTAMP::timestamp where email_id=? and domain_instance_name=? and option_name = ?";
	private static String DELETE_BY_USER_AND_OPTION = "delete from system_user_option where email_id=? and domain_instance_name = ? and option_name=?";
	
	public static class UserOptionMapper implements RowMapper<UserOption> {
		public UserOption mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new UserOption(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getTimestamp(5).toInstant());
		}
	}
		
	public UserOption retrieve(String emailID, String domain, UserOptionName option) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_BY_USER_DOMAIN_OPTION,	new UserOptionMapper(), emailID, domain, option.toString());
		}
		catch (org.springframework.dao.EmptyResultDataAccessException dae) {
			return null;
		}
	}
	
	public boolean save(UserOption record) {
		int numRows = this.getJDBCTemplate().update(UPSERT, record.getEmailID(), record.getDomainInstanceName(), record.getOptionName().toString(), record.getOptionValue());
		return (numRows == 1);
	}

	public boolean delete(String emailID, String domainInstanceName, UserOptionName option) {
		int numRows = this.getJDBCTemplate().update(DELETE_BY_USER_AND_OPTION,emailID, domainInstanceName, option.toString());
		return (numRows == 1);
	}

}
