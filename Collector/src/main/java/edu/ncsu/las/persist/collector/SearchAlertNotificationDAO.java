package edu.ncsu.las.persist.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.SearchAlertNotification;
import edu.ncsu.las.persist.DAO;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Boolean.TRUE;


/**
 * 
 */
public class SearchAlertNotificationDAO extends DAO {

    private static final Logger logger = Logger.getLogger(Collector.class.getName());

    private static final String SELECT_ALL_ALERT_NOTIFICATIONS = "select alert_id, result_url, result_title, result_description, acknowledgement, result_datetime from search_alert_notification ";

    private static final String SELECT_ALERT_NOTIFICATIONS_FOR_ALERT_ID = SELECT_ALL_ALERT_NOTIFICATIONS + "where alert_id=? order by result_datetime desc";

    private static final String SELECT_ALERT_NOTIFICATIONS_TO_SHOW = SELECT_ALL_ALERT_NOTIFICATIONS + "where acknowledgement=false and alert_id in (:ids) order by result_datetime desc";

    private static final String UPDATE_ALERT_NOTIFICATION_ACKNOWLEDGEMENT = "update search_alert_notification set acknowledgement=? where alert_id = ? and result_url =?";

    private static final String UPDATE_ALL_ALERT_NOTIFICATION_ACKNOWLEDGEMENT = "update search_alert_notification set acknowledgement=? where alert_id = ?";
    
    private static final String DLEETE_ALL_ALERT_NOTIFICATION_FOR_ALERT_ID = "delete from search_alert_notification where alert_id = ?";
    
    private static final String INSERT_ALERT_NOTIFICATION = "insert into search_alert_notification "+
            "(alert_id, result_url, result_title, result_description, result_datetime, acknowledgement) values (?,?,?,?,now(),?)" ;

    private static class AlertNotificationRowMapper implements RowMapper<SearchAlertNotification> {

        public SearchAlertNotification mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SearchAlertNotification(UUID.fromString(rs.getString("alert_id")), rs.getString("result_url"), rs.getString("result_title"), rs.getString("result_description"), rs.getBoolean("acknowledgement"), rs.getTimestamp("result_datetime"));
        }

    }

     public boolean insertAlertNotification(SearchAlertNotification alertNotification) throws org.springframework.dao.DuplicateKeyException {
        int numRows = this.getJDBCTemplate().update(INSERT_ALERT_NOTIFICATION, alertNotification.getAlertId(), alertNotification.getResultUrl(), alertNotification.getResultTitle(), alertNotification.getResultDescription(), alertNotification.getAcknowledgement());
        return (numRows == 1);
    }

    public List<SearchAlertNotification> getAlertNotificationsForAlert(UUID alertId) {
        logger.log(Level.FINE, "getting all alerts notifications for alert.");
        return this.getJDBCTemplate().query(SELECT_ALERT_NOTIFICATIONS_FOR_ALERT_ID, new AlertNotificationRowMapper(), alertId);
    }

    public List<SearchAlertNotification> getUnacknowledgedAlertNotifications( List<Object> alertIDs) {
        logger.log(Level.FINE, "getting all alerts notifications to be shown.");

        if (alertIDs.isEmpty()) {
        	return new ArrayList<SearchAlertNotification>();
        }
        
        Map<String, List<Object>> params = new HashMap<String, List<Object>>();
        params.put("ids", alertIDs);

        NamedParameterJdbcTemplate template = new    NamedParameterJdbcTemplate(this.getJDBCTemplate().getDataSource());
        return template.query(SELECT_ALERT_NOTIFICATIONS_TO_SHOW, params, new AlertNotificationRowMapper());
    }

 

    public int updateAcknowledgement(UUID alertId, String url, boolean value) {
        logger.log(Level.FINE, "changing alert notification acknowledgement to true");
        return this.getJDBCTemplate().update(UPDATE_ALERT_NOTIFICATION_ACKNOWLEDGEMENT, value, alertId, url);
    }

	public int acknowledgeAllAlerts(UUID alertID) {
        logger.log(Level.FINE, "changing alert notification acknowledgement to true for all notifications in a search alert");
        return this.getJDBCTemplate().update(UPDATE_ALL_ALERT_NOTIFICATION_ACKNOWLEDGEMENT, TRUE, alertID);

	}
	
	public int deleteAllAlertNotificationsForAlertID(UUID alertID) {
        logger.log(Level.FINE, "Deleting all alert notifications: " + alertID);
        return this.getJDBCTemplate().update(DLEETE_ALL_ALERT_NOTIFICATION_FOR_ALERT_ID, alertID);
	}
	
	
}
