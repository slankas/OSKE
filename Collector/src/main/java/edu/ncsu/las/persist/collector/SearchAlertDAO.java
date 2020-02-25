package edu.ncsu.las.persist.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.persist.DAO;
import edu.ncsu.las.model.collector.SearchAlert;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Stores data 
 * 
 */
public class SearchAlertDAO extends DAO {

    private static final Logger logger = Logger.getLogger(Collector.class.getName());

    private static final String SELECT_ALERT = "select alert_id, alert_name, source_handler, search_term, number_of_result, owner_email_id, cron_schedule, cron_next_run, date_created, date_last_run, state, current_collector, num_times_run, domain from search_alert ";

    private static final String SELECT_ALERT_BY_ALERT_ID = SELECT_ALERT + " where alert_id=?" ;

    private static final String SELECT_ALERTS_BY_STATE = SELECT_ALERT + " where state=? ";

    private static final String SELECT_ALERTS_READY_TO_RUN = SELECT_ALERT + " where state='waiting' and cron_next_run < now()";
    
    private static final String SELECT_ALERTS_BY_DOMAIN = SELECT_ALERT + " where domain=? ";

    private static final String SELECT_ALERTS_BY_DOMAIN_AND_USER = SELECT_ALERT + " where domain=? and owner_email_id=?";

    
    private static final String INSERT_ALERT = "insert into search_alert"+
            "(alert_id, alert_name, source_handler, search_term, number_of_result, owner_email_id, cron_schedule, cron_next_run, date_created, date_last_run, state, current_collector, num_times_run, domain)"+
            "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_ALERT_STATUS = "update search_alert set state=? where alert_id=?";
    
    private static final String UPDATE_ALERT_TO_CLAIM = "update search_alert set state='"+SearchAlert.STATE_RUNNING+"', current_collector=?  where alert_id=? and state='"+SearchAlert.STATE_WAITING+"'";
    

    private static final String UPDATE_ALERT_STATUS_TO_WAITING = "update search_alert " +
            "set state=? , cron_next_run=? , date_last_run=? , num_times_run=? " +
            "where alert_id=?";

    
    private static final String DELETE_ALERT_BY_ID = "delete from  search_alert where alert_id=?";
    
    private static class AlertRowMapper implements RowMapper<SearchAlert> {

        public SearchAlert mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SearchAlert((java.util.UUID)rs.getObject("alert_id"), rs.getString("alert_name"), rs.getString("source_handler"),rs.getString("search_term"),  rs.getInt("number_of_result"),rs.getString("owner_email_id"),rs.getString("cron_schedule"),rs.getTimestamp("cron_next_run"),rs.getTimestamp("date_created"),rs.getTimestamp("date_last_run"),rs.getString("state"),rs.getString("current_collector"),rs.getInt("num_times_run"),rs.getString("domain"));
        }
    }

    public boolean insertAlert(SearchAlert alert){
        int numRows = this.getJDBCTemplate().update(INSERT_ALERT, alert.getAlertID(), alert.getAlertName(), alert.getSourceHandler(), alert.getSearchTerm(), alert.getNumberOfResult(), alert.getOwnerEmailId(), alert.getCronSchedule(), alert.getCronNextRun(), alert.getDateCreated(), alert.getDateLastRun(), alert.getState(), alert.getCurrentCollector(), alert.getNumTimesRun(), alert.getDomain());
        return (numRows == 1);
    }

    public List<SearchAlert> getAllAlertsReadyToRun() {
        logger.log(Level.FINER, "getting all alerts in waiting state, waiting to run.");
        return this.getJDBCTemplate().query(SELECT_ALERTS_READY_TO_RUN, new AlertRowMapper()); 
    }

    
    public List<SearchAlert> getAllAlertsByState(String state) {
        logger.log(Level.FINER, "getting all alerts in waiting state.");
        return this.getJDBCTemplate().query(SELECT_ALERTS_BY_STATE, new AlertRowMapper(), state); 
    }

    public List<SearchAlert> getAllAlertsByDomain(String domain) {
        return this.getJDBCTemplate().query(SELECT_ALERTS_BY_DOMAIN, new AlertRowMapper(), domain); 
    }    

    public List<SearchAlert> getAllAlertsByDomainAndUser(String domain, String userEmailID) {
        return this.getJDBCTemplate().query(SELECT_ALERTS_BY_DOMAIN_AND_USER, new AlertRowMapper(), domain, userEmailID); 
    }    
    
    public int changeAlertStatus(UUID alertID, String state){
        return this.getJDBCTemplate().update(UPDATE_ALERT_STATUS,state,alertID);
    }

    public int changeAlertStatusToWaiting(Timestamp cronNextRun, Timestamp dateLastRun, int numTimesRun, UUID alertId){
        logger.log(Level.FINER, "changing alert status to running");
        return this.getJDBCTemplate().update(UPDATE_ALERT_STATUS_TO_WAITING,"waiting",cronNextRun,dateLastRun,numTimesRun,alertId);
    }

    public SearchAlert retrieveByAlertID(UUID alertId){
        logger.log(Level.FINER, "getting alert by alert id");
        return this.getJDBCTemplate().queryForObject(SELECT_ALERT_BY_ALERT_ID,new AlertRowMapper(),alertId);
    }

	public int claimAlert(UUID alertID, String collectorID) {
		return this.getJDBCTemplate().update(UPDATE_ALERT_TO_CLAIM,collectorID,alertID);
	}

	public boolean delete(UUID alertID) {
		return (this.getJDBCTemplate().update(DELETE_ALERT_BY_ID,alertID) == 1);
	}

}