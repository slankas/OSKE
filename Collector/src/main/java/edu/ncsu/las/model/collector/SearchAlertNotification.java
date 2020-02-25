package edu.ncsu.las.model.collector;

import edu.ncsu.las.model.collector.Job;
import edu.ncsu.las.persist.collector.SearchAlertNotificationDAO;
import edu.ncsu.las.util.DateUtilities;

import org.json.JSONObject;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * 
 */
public class SearchAlertNotification {

    private static Logger logger = Logger.getLogger(Job.class.getName());

    private UUID _alertId;
    private String _resultUrl;
    private String _resultTitle;
    private String _resultDescription;
    private boolean _acknowledgement;
    private java.sql.Timestamp _resultTimestamp;

    public SearchAlertNotification(UUID alertId, String resultUrl, String resultTitle, String resultDescription, boolean acknowledgement, java.sql.Timestamp resultTimestamp) {

        _alertId = alertId;
        _resultUrl = resultUrl;
        _resultTitle = resultTitle;
        _resultDescription = resultDescription;
        _acknowledgement = acknowledgement;
        _resultTimestamp = resultTimestamp;
        
        if (_resultDescription == null) { _resultDescription = ""; }

    }

    /*
    public void setAlertId(UUID alertId) { this._alertId = alertId; }

    public void setResultUrl(String resultUrl) { this._resultUrl = resultUrl; }

    public void setResultTitle(String resultTitle) { this._resultTitle = resultTitle; }

    public void setResultDescription(String resultDescription) { this._resultDescription = resultDescription; }

    public void setAcknowledgement(boolean acknowledgement) { this._acknowledgement = acknowledgement; }
    */

    public UUID getAlertId() { return _alertId;}

    public String getResultUrl() {return _resultUrl;}

    public String getResultTitle() {return _resultTitle;}

    public String getResultDescription() {return _resultDescription;}

    public boolean getAcknowledgement() {return _acknowledgement;}

    public JSONObject toJSON()  {
        JSONObject result = new JSONObject().put("alertID", _alertId)
                .put("resultURL", _resultUrl)
                .put("resultTitle", _resultTitle)
                .put("resultDescription", _resultDescription)
                .put("acknowledgement", _acknowledgement)
                .put("resultTimestamp", DateUtilities.getDateTimeISODateTimeFormat(_resultTimestamp.toInstant()));

        return result;
    }

    public boolean insert() throws org.springframework.dao.DuplicateKeyException {
        logger.log(Level.FINER, "AlertNotification: inserting alert notification for user.");
        SearchAlertNotificationDAO andao = new SearchAlertNotificationDAO();
        return andao.insertAlertNotification(this);
    }

    /*
    public static List<SearchAlertNotification> getAlertNotificationsForAlert(UUID alertId) {
        logger.log(Level.INFO, "AlertNotification: getting alert notifications for alertId: "+String.valueOf(alertId));
        SearchAlertNotificationDAO alertNotificationDao = new SearchAlertNotificationDAO();
        return alertNotificationDao.getAlertNotificationsForAlert(alertId);
    }

    public static List<SearchAlertNotification> getAllAlertNotificationsToShow() {
        logger.log(Level.INFO, "AlertNotification: getting all alerts notifications to be shown.");
        SearchAlertNotificationDAO alertNotificationDao = new SearchAlertNotificationDAO();
        return alertNotificationDao.getAllAlertNotificationsToShow(new UUID[0]); //TODO: FIXME
    }

	*/

    /**
     * Used to acknowledge a particular SearchAlert notificaiton
     * 
     * @param alertID
     * @param url
     * @return returns true if the SearchAlertNotification has been set to true, false otherwise.
     *         if previously set to true it will return true again.
     */
    public static boolean acknowledge(UUID alertID, String url) {
        logger.log(Level.FINE, "acknowledging SearchAlertNotification: "+alertID+", "+url);
        SearchAlertNotificationDAO anDao = new SearchAlertNotificationDAO();
        return (anDao.updateAcknowledgement(alertID, url, true) == 1);
    }
   

}
