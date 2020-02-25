package edu.ncsu.las.persist.collector;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.persist.DAO;


public class DomainDAO extends DAO {
	
	private static final String INSERT_DOMAIN_ROW = "INSERT INTO domain(domain_instance_name, domain_status, effective_ts, full_name, description, primary_contact, appearance_order, configuration, user_email_id, insert_ts, offline)	VALUES (?, ?,?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP::timestamp,?)"; 

	private static final String SELECT_DOMAIN = "SELECT domain_instance_name, domain_status, effective_ts, full_name, description, primary_contact, appearance_order, configuration, user_email_id, insert_ts, offline FROM domain";
	
	private static final String SELECT_DOMAIN_HISTORY = SELECT_DOMAIN + " where domain_instance_name=? order by effective_ts";
	
	private static final String SELECT_DOMAIN_EFFECTIVE_RECORD = SELECT_DOMAIN + " a  where domain_instance_name = ? and effective_ts = (select max(effective_ts) from domain b where a.domain_instance_name = b.domain_instance_name and b.domain_instance_name = ? and b.effective_ts <= CURRENT_TIMESTAMP::timestamp)";

	private static final String SELECT_DOMAIN_LEAST_EFFECTIVE_RECORD = SELECT_DOMAIN + " a  where domain_instance_name = ? and effective_ts = (select min(effective_ts) from domain b where a.domain_instance_name = b.domain_instance_name and b.domain_instance_name = ? and b.effective_ts <= CURRENT_TIMESTAMP::timestamp)";
	
	
	private static final String SELECT_DOMAIN_ACTIVE_RECORDS = SELECT_DOMAIN + " a  where domain_status = 'active' and effective_ts = (select max(effective_ts) from domain b where a.domain_instance_name = b.domain_instance_name and b.effective_ts <= CURRENT_TIMESTAMP::timestamp)";
	
	/*
	 SELECT domain_instance_name, domain_status, effective_ts, full_name, description, primary_contact, appearance_order, configuration, user_email_id, insert_ts
	FROM public.domain a
    where domain_instance_name = 'test' and
          domain_status = 'Active' and 
          effective_ts = (select max(effective_ts) from domain b where a.domain_instance_name = b.domain_instance_name and
                                                                       b.domain_instance_name = 'test' and
                                                                       b.effective_ts <= CURRENT_TIMESTAMP::timestamp);
    
INSERT INTO public.domain(
	domain_instance_name, domain_status, effective_ts, full_name, description, primary_contact, appearance_order, configuration, user_email_id, insert_ts)
	VALUES ('test', 'inactive', CURRENT_TIMESTAMP::timestamp + (INTERVAL '3 day') ,'Test Dmain','older description', 'user', 1, '{}', 'user@ncsu.edu', CURRENT_TIMESTAMP::timestamp+(INTERVAL '2 day'));    
	 
	 
	 
	 */
	
	public static class DomainRowMapper implements RowMapper<Domain> {
		public Domain mapRow(ResultSet rs,int rowNum) throws SQLException {
			return new Domain(rs.getString(1),rs.getString(2),rs.getTimestamp(3),rs.getString(4),
					          rs.getString(5),rs.getString(6),rs.getInt(7),rs.getString(8),
					          rs.getString(9),rs.getTimestamp(10), rs.getBoolean(11));
		}
		
	}
	
	
	/**
	 * Retrieves the record most recently created for a domain
	 * 
	 * @param domainInstanceName 
	 * @return most effective(recent) record for a specific domain
	 */
	public Domain retrieveEffectiveDomainRecord(String domainInstanceName) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_DOMAIN_EFFECTIVE_RECORD, new DomainRowMapper(), domainInstanceName,domainInstanceName);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	/**
	 * This returns the first record for the domain creation - which is used for the established date
	 * @param domainInstanceName
	 * @return oldest domain record
	 */
	public Domain retrieveLeastEffectiveDomainRecord(String domainInstanceName) {
		try {
			return  this.getJDBCTemplate().queryForObject(SELECT_DOMAIN_LEAST_EFFECTIVE_RECORD, new DomainRowMapper(), domainInstanceName,domainInstanceName);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	public java.util.List<Domain> retrieveActiveDomains() {
		return  this.getJDBCTemplate().query(SELECT_DOMAIN_ACTIVE_RECORDS, new DomainRowMapper());
	
	}
	
	public java.util.List<Domain> retrieveDomainHistory(String domainInstanceName) {
		return  this.getJDBCTemplate().query(SELECT_DOMAIN_HISTORY, new DomainRowMapper(),domainInstanceName);
	}
	
	public boolean insertDomain(Domain d){
		int numRows = this.getJDBCTemplate().update(INSERT_DOMAIN_ROW,
							d.getDomainInstanceName(),d.getDomainStatus(),d.getEffectiveTimestampAsTimeStamp(), d.getFullName(),
							d.getDescription(), d.getPrimaryContact(), d.getAppearanceOrder(), d.getConfiguration().toString(),
							d.getUserEmailAddress(), d.isOffline());

		return (numRows == 1);
	}
	
	
}
