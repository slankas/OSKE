package edu.ncsu.las.model.collector;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.persist.collector.UserDAO;
import edu.ncsu.las.util.DateUtilities;

public class User {

	private String _emailID;
	private String _userID;
	private String _name;
	
	private java.util.ArrayList<RoleDomainAccess> _access = new java.util.ArrayList<RoleDomainAccess>();
	
	public  class RoleDomainAccess {
		private RoleType _role;
		private String _domainInstanceName;
		private String _status;
		private Instant _statusDt;
		private String _changedByEmailID;
		
		public RoleType getRole() {  return _role;  }
		public void setRole(RoleType role) { this._role = role; }
		public String getDomainInstanceName() {	return _domainInstanceName;}
		public void setDomainInstanceName(String domainInstanceName) {		this._domainInstanceName = domainInstanceName;		}
		public String getStatus() {			return _status;}
		public void setStatus(String status) {this._status = status; }
		public Instant getStatusDt() { 	return _statusDt; }
		public void setStatusDt(Instant status_dt) { this._statusDt = status_dt; }
		public String getChangedByEmailID() { return this._changedByEmailID;}
		public void setChangedByEmailID(String newValue) { this._changedByEmailID = newValue;}
		
		public RoleDomainAccess(RoleType role, String domainInstanceName, String status, java.sql.Timestamp statusDt, String changedByEmailID) {
			_role = role;
			_domainInstanceName = domainInstanceName;
			_status = status;
			_statusDt = statusDt.toInstant();
			_changedByEmailID = changedByEmailID;
		}
		
		public JSONObject toJSON() {
			JSONObject result = new JSONObject().put("role", this.getRole())
												.put("domain", this.getDomainInstanceName())
					                            .put("status", this.getStatus())
					                            .put("statusDt", DateUtilities.getDateTimeISODateTimeFormat(_statusDt))
					                            .put("changedByEmailID",this.getChangedByEmailID());
			return result;		
		}
		public boolean isActive(String domain, RoleType role) {
			return _status.equalsIgnoreCase("active") && _role.equals(role) && _domainInstanceName.equals(domain);
		}
		public boolean isActive(String domain) {
			return _status.equalsIgnoreCase("active")  && _domainInstanceName.equals(domain);
		}		
		public boolean updateRole(RoleType newRole,String domain, String changedByEmailID){
			UserDAO dd = new UserDAO();
			if (dd.updateRole(getEmailID(), this.getRole(), newRole,domain, changedByEmailID)) {
				this.setRole(newRole);
				this.setChangedByEmailID(changedByEmailID);
				return true;
			}
			else {
				return false;
			}
		}
		
		public boolean updateStatus(String newStatus, String domain, String changedByEmailID){
			UserDAO dd = new UserDAO();
			if (dd.updateStatus(getEmailID(), this.getRole(), newStatus, domain, changedByEmailID)) {
				this.setStatus(newStatus);
				this.setChangedByEmailID(changedByEmailID);
				return true;
			}
			else {
				return false;
			}
				
		}

		
	}
	

	public User(String emailID, String name) {
		super();
		this.setEmailID(emailID);
		_name = name;
	}
	
	// To create session: just need email addresses.
	public User(String emailID) {
		this.setEmailID(emailID);
	}

	public void addAccess(RoleDomainAccess rda) {
		_access.add(rda);
	}
	private void addAllAccess(ArrayList<RoleDomainAccess> access2) {
		_access.addAll(access2);
		
	}
	
	public String getUserID() {
		return _userID;
	}
	
	public String getEmailID() {
		return _emailID;
	}
	
	public void setEmailID(String email_id) {
		this._emailID = email_id;
		this._userID = _emailID;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}
	
	public boolean hasAccess(String domain, RoleType role) {
		for (RoleDomainAccess rda: _access) {
			if (rda.isActive(domain,role)) { return true; }
		}
		
		return false;
	}

	public boolean hasDomainAccess(String domain) {
		for (RoleDomainAccess rda: _access) {
			if (rda.isActive(domain)) { return true; }
		}
		return false;
	}	
	
	public boolean hasDomainAndRole(String domain, RoleType role) {
		for (RoleDomainAccess rda: _access) {
			if (rda.getDomainInstanceName().equals(domain) && rda.getRole().equals(role)) { return true; }
		}
		return false;
	}
	
	public boolean hasAnyActiveAccess() {
		for (RoleDomainAccess rda: _access) {
			if (rda.getStatus().equals("active")) { return true; }
		}
		return false;
	}	
	
	
	public  boolean createUser(RoleDomainAccess rda) {
		return (new UserDAO()).createUser(this,rda);
	}	
	

	public static java.util.List<User> getUsers(String domain) {
		UserDAO dd = new UserDAO();
		
		if (domain.equals(Domain.DOMAIN_SYSTEM)) {
			return dd.getAllUsers();
		}
		else {
			return dd.getUsers(domain);
		}
	}
	

	
	/**
	 * Gets the user record for a particular user.  
	 * 
	 *  
	 * @param emailID
	 * @return
	 */
	public static User findUser(String emailID){
		UserDAO dd = new UserDAO();
		List<User> userRecords = dd.getUserRecords(emailID);
		
		if(userRecords.size() == 0) {
			return null;
		}
		else {
			User result = userRecords.get(0);
			
			for (int i=1; i< userRecords.size();i++) {
				result.addAllAccess(userRecords.get(i)._access);
				
			}
			return result;
		}
	}	
	


	/**
	 * Gets the user record for a particular user and role 
	 * 
	 * @param emailID
	 * @param role
	 * @return
	 */
	public static User findUser(String emailID, RoleType role, String domain){
		if (emailID.startsWith("las_chrome_")) {
			emailID =  "laschr" +emailID.substring(11);
		}
		
		UserDAO dd = new UserDAO();
		return dd.getUserRecord(emailID,role,domain);		
	}	
	
	/*
	public static String[] getRolesForUser(String emailid){
		UserDAO dd = new UserDAO();
		List<User> userRecords = dd.getUserRecords(emailid);
		
		return userRecords.stream().map(User::getRole).toArray(String[]::new);
	}
	*/

	public static List<User> getUsersForRole(RoleType role,String domain){
		UserDAO dd = new UserDAO();
		List<User> userRecords = dd.getUsersForRole(role,domain);
		return userRecords;
	}		
	
	public static String[] getAdjudicatorEmails(String domain){
		List<User> userRecords = User.getUsersForRole(RoleType.ADJUDICATOR,domain);
		
		return userRecords.stream().map(User::getEmailID).toArray(String[]::new);
	}	
	
	public static String[] getAdministratorEmails(String domain){
		List<User> userRecords = User.getUsersForRole(RoleType.ADMINISTRATOR,domain);
		
		return userRecords.stream().map(User::getEmailID).toArray(String[]::new);
	}	
	
	public String toString() {
		return 	"Email ID: "+ _emailID +", userID: " + _userID +", name: " + _name+",  access: "+ this.getAccessArray().toString();
	}
	
	public JSONArray getAccessArray() {
		JSONArray access = new JSONArray();
		for (RoleDomainAccess rda: _access) {access.put(rda.toJSON());}
		return access;
	}
	
	public JSONObject toJSON() {
		
		JSONObject result = new JSONObject().put("emailID", this.getEmailID())
				                            .put("userID", this.getUserID())
				                            .put("name", this.getName())
				                            .put("access", this.getAccessArray());
		return result;		
	}

	public RoleDomainAccess getFirstAccess() {
		return _access.get(0);
	}

	/** maintains "true" if the system has found a record where the user is approved and active */
	private Boolean _hasApprovedAgreement = null;
	private UserAgreement _userAgreement = null;   // if there is a current and approved agreement, then this is that value..
	
	public boolean hasActiveAndApprovedAgreement() {
		if (_hasApprovedAgreement == null) {
			UserAgreement ua = UserAgreement.getUserAgreementIfCurrentAndApproved(this.getEmailID());
			if (ua != null ) {
				_userAgreement = ua;
				_hasApprovedAgreement = true;
				return true;
			}
			
			// somewhat of a backdoor in that we need a way to bootstrap the process and have someone access the system to 
			// actually approve messages...
			if (hasDomainAndRole(Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR)) {
				UserAgreement tempUA = new UserAgreement();
				_userAgreement = tempUA;
				_hasApprovedAgreement = true;
				return true;
			}
			
			
			return false;
		} else {
			return _hasApprovedAgreement;
		}	
	}
	
	public UserAgreement getActiveAndApprovedUserAgreement() {
		if (this.hasActiveAndApprovedAgreement()) {
			return _userAgreement;
		}
		else {
			return null;
		}
	}

}
 