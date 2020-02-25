package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.persist.DAO;

/**
 * 
 *
 */
public class UserDAO extends DAO {

	private static String SELECT         = "select email_id, name, role, domain_instance_name, status, status_dt,changed_by_email_id from system_user";
	private static String INSERT_USER    = "insert into system_user (email_id,  name,role, domain_instance_name, status,status_dt,changed_by_email_id) values (?, ?, ?,?, ?, ?,?)";
	private static String UPDATE_ROLE    = "update system_user set role=?, status_dt=CURRENT_TIMESTAMP::timestamp, changed_by_email_id=? where email_id=? and role=? and domain_instance_name=?";
	private static String UPDATE_STATUS  = "update system_user set status=?, status_dt=CURRENT_TIMESTAMP::timestamp, changed_by_email_id=?  where email_id=? and role=? and domain_instance_name=?";
	private static String SELECT_BY_USER = SELECT + " where email_id=?";
	private static String SELECT_BY_USER_AND_ROLE_DOMAIN = SELECT + " where email_id=? and role=? and domain_instance_name = ?";
	private static String SELECT_BY_ACTIVE_ROLE_DOMAIN   = SELECT + " where status='active' and role=? and domain_instance_name = ? ";
	private static String SELECT_BY_DOMAIN   = SELECT + " where  domain_instance_name = ? ";
	
	public static class UserRowMapper implements RowMapper<User> {
		public User mapRow(ResultSet rs, int rowNum) throws SQLException {
			User u =  new User(rs.getString(1), rs.getString(2));
			User.RoleDomainAccess rda = u.new RoleDomainAccess(RoleType.getEnum(rs.getString(3)),	rs.getString(4), rs.getString(5), rs.getTimestamp(6), rs.getString(7));
			u.addAccess(rda);
			return u;
		}
	}

	/**
	 * Gets all users within the system for a particular domain
	 * @param domain 
	 * 
	 * @return
	 */
	public List<User> getUsers(String domain) {
		return this.getJDBCTemplate().query(SELECT_BY_DOMAIN,	new UserRowMapper(), domain);
	}
	
	/**
	 * Gets all users within the system.
	 * 
	 * @return
	 */
	public List<User> getAllUsers() {
		return this.getJDBCTemplate().query(SELECT,	new UserRowMapper());
	}

	public boolean createUser(User user, User.RoleDomainAccess rda) {
		int numRows = this.getJDBCTemplate().update(INSERT_USER, user.getEmailID(),	user.getName(), rda.getRole().toString(), rda.getDomainInstanceName(),rda.getStatus(), Timestamp.from(rda.getStatusDt()), rda.getChangedByEmailID());
		return (numRows == 1);
	}

	public boolean updateRole(String emailid, RoleType previousRole, RoleType newRole, String domainInstanceName, String changedByEmailID) {
		return (this.getJDBCTemplate().update(UPDATE_ROLE,newRole.toString(),changedByEmailID,emailid,previousRole.toString(),domainInstanceName) == 1);
	}
	
	public boolean updateStatus(String emailid, RoleType role, String status, String domainInstanceName, String changedByEmailID) {
		return (this.getJDBCTemplate().update(UPDATE_STATUS,status,changedByEmailID,emailid,role.toString(), domainInstanceName) == 1);
	}
	
	public List<User> getUserRecords(String emailID){
		return this.getJDBCTemplate().query(SELECT_BY_USER,	new UserRowMapper(), emailID);
	}

	public User getUserRecord(String emailID, RoleType role, String domainInstanceName){
		try {
			return this.getJDBCTemplate().queryForObject(SELECT_BY_USER_AND_ROLE_DOMAIN, new UserRowMapper(), emailID, role.toString(),domainInstanceName);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException dae) {
			return null;  //not able to find record, just return null.
		}
	}	

	public List<User> getUsersForRole(RoleType role, String domainInstanceName){
		return this.getJDBCTemplate().query(SELECT_BY_ACTIVE_ROLE_DOMAIN,	new UserRowMapper(), role.toString(),domainInstanceName);
	}
	
}
