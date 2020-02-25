package edu.ncsu.las.model.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.persist.collector.SearchAlertDAO;
import edu.ncsu.las.persist.collector.SearchAlertNotificationDAO;
import edu.ncsu.las.source.DomainDiscoveryInterface;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.CronExpression;
import edu.ncsu.las.util.DateUtilities;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.ValidationException;

/**
 * Represents a
 * 
 */

public class SearchAlert {
    private static Logger logger = Logger.getLogger(Job.class.getName());

    public static final String STATE_WAITING = "waiting";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_PAUSED  = "paused";
    
    
    private UUID _alertId;
    private String _alertName;
    private String _sourceHandler;
    private String _searchTerm;
    private int _numberOfResult;
    private String _ownerEmailId;
    private String _cronSchedule;
    private Timestamp _cronNextRun;
    private Timestamp _dateCreated;
    private Timestamp _dateLastRun;
    private String _state;
    private String _currentCollector;
    private int _numTimesRun;
    private String _domain;

    public SearchAlert(UUID alertId, String alertName, String sourceHandler, String searchTerm, int numberOfResult, String ownerEmailId, String cronSchedule, Timestamp cronNextRun, Timestamp dateCreated, Timestamp dateLastRun, String state, String currentCollector, int numTimesRun, String domain) {

        _alertId = alertId;
        _alertName = alertName;
        _sourceHandler = sourceHandler;
        _searchTerm = searchTerm;
        _numberOfResult = numberOfResult;
        _ownerEmailId = ownerEmailId;
        _cronSchedule = cronSchedule;
        _cronNextRun = cronNextRun;
        _dateCreated = dateCreated;
        _dateLastRun = dateLastRun;
        _state = state;
        _currentCollector = currentCollector;
        _numTimesRun = numTimesRun;
        _domain = domain;

    }

    public SearchAlert() {
    }

    public void setAlertId(UUID alertId){ this._alertId = alertId; }

    public void setAlertName(String alertName){ this._alertName = alertName; }

    public void setSourceHandler(String sourceHandler){ this._sourceHandler = sourceHandler; }

    public void setSearchTerm(String searchTerm){ this._searchTerm = searchTerm; }

    public void setNumberOfResult(int numberOfResult){ this._numberOfResult = numberOfResult; }

    public void setOwnerEmailId(String ownerEmailId){ this._ownerEmailId = ownerEmailId; }

    public void setCronSchedule(String cronSchedule){ this._cronSchedule = cronSchedule; }

    public void setCronNextRun(Timestamp cronNextRun){ this._cronNextRun = cronNextRun; }

    public void setDateCreated(Timestamp dateCreated){ this._dateCreated= dateCreated; }

    public void setDateLastRun(Timestamp dateLastRun){ this._dateLastRun = dateLastRun; }

    public void setState(String state){ this._state = state; }

    public void setCurrentCollector(String currentCollector){ this._currentCollector = currentCollector; }

    public void setNumTimesRun(int numTimesRun){ this._numTimesRun = numTimesRun; }

    public void setDomain(String domain){ this._domain = domain; }

    public UUID getAlertID( ){ return _alertId ; }

    public String getAlertName( ){ return _alertName ; }

    public String getSourceHandler( ){ return _sourceHandler ; }

    public String getSearchTerm( ){ return _searchTerm ; }

    public int getNumberOfResult( ){ return _numberOfResult ; }

    public String getOwnerEmailId( ){ return _ownerEmailId ; }

    public String getCronSchedule( ){ return _cronSchedule ; }

    public Timestamp getCronNextRun( ){ return _cronNextRun ; }

    public Timestamp getDateCreated( ){ return _dateCreated ; }

    public Timestamp getDateLastRun( ){ return _dateLastRun ; }

    public String getState(){ return _state; }

    public String getCurrentCollector(){ return _currentCollector; }

    public int getNumTimesRun(){ return _numTimesRun; }

    public String getDomain(){ return _domain; }

    public JSONObject toJSON()  {
        JSONObject result = new JSONObject().put("alertId", _alertId)
                .put("alertName", _alertName)
                .put("sourceHandler", _sourceHandler)
                .put("searchTerm", _searchTerm)
                .put("numberOfResult", _numberOfResult)
                .put("ownerEmailId", _ownerEmailId)
                .put("cronSchedule", _cronSchedule)
                .put("cronNextRun", DateUtilities.getDateTimeISODateTimeFormat(_cronNextRun.toInstant()) )
                .put("dateCreated", DateUtilities.getDateTimeISODateTimeFormat(_dateCreated.toInstant()))
                .put("dateLastRun", DateUtilities.getDateTimeISODateTimeFormat(_dateLastRun.toInstant()))
                .put("domain", _domain)
                .put("state", _state);

        return result;
    }

    public boolean insert(){
        logger.log(Level.FINEST, "Alert: inserting alert created by user.");
        SearchAlertDAO alertDao = new SearchAlertDAO();
        return alertDao.insertAlert(this);
    }

    public static java.util.List<SearchAlert> getAllWaitingAlerts() {
        logger.log(Level.INFO, "Alert: getting all Alerts in waiting state.");
        SearchAlertDAO alertDao = new SearchAlertDAO();
        return alertDao.getAllAlertsReadyToRun();
    }


    public int markAlertRunComplete() throws ValidationException {
        logger.log(Level.INFO, "run complete, changing status to waiting: "+this.getAlertID());
        
		int numTimesRun = this.getNumTimesRun()+1;
		Timestamp dateLastRun = new Timestamp(System.currentTimeMillis());
		Timestamp cronNextRun;
		String cronSchedule = this.getCronSchedule();

		try {
			CronExpression ce = new CronExpression(cronSchedule);
			ce.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
			java.util.Date d = ce.getNextValidTimeAfter(new java.util.Date());
			cronNextRun = Timestamp.from(Instant.ofEpochMilli(d.getTime()));
			SearchAlertDAO alertDao = new SearchAlertDAO();
	        return alertDao.changeAlertStatusToWaiting(cronNextRun,dateLastRun,numTimesRun,this.getAlertID());
		}
		catch (Exception pe) {
			logger.log(Level.SEVERE, "Unable to generate the run time: "+this.getAlertID());
			
			return 0;
		}
    }  
    
    public int claimAlertToRun(String collectorID) {
        logger.log(Level.INFO, "Alert: changing alert status to running.");
        SearchAlertDAO alertDao = new SearchAlertDAO();
        return alertDao.claimAlert(this.getAlertID(),collectorID);
    }    
    
    public boolean changeAlertState(String newState) {
        logger.log(Level.INFO, "Alert: changing alert status to "+ newState);
        SearchAlertDAO alertDao = new SearchAlertDAO();
        return (alertDao.changeAlertStatus(this.getAlertID(),newState) == 1);
    }
    
    public int acknowledgeAllAlerts() {
    	logger.log(Level.FINE, "set all alerts to acknowledged true");
    	SearchAlertNotificationDAO sanDao = new SearchAlertNotificationDAO();
        return sanDao.acknowledgeAllAlerts(this.getAlertID());
    }
    
    public boolean deleteAlert() {
    	logger.log(Level.FINE, "deleting alert: "+ this.getAlertID());
    	( new SearchAlertNotificationDAO()).deleteAllAlertNotificationsForAlertID(this.getAlertID());
    	return (new SearchAlertDAO()).delete(this.getAlertID());
    }
    
    public List<SearchAlertNotification> getAllAlertNotifications(boolean showAll) {
        logger.log(Level.INFO, "AlertNotification: getting all alerts notifications: "+showAll);
        
        if (showAll) {
        	return (new SearchAlertNotificationDAO()).getAlertNotificationsForAlert(this.getAlertID());
        }
        else {
        	ArrayList<Object> alertIDs = new ArrayList<Object>();
        	alertIDs.add(this.getAlertID());
        	return (new SearchAlertNotificationDAO()).getUnacknowledgedAlertNotifications(alertIDs);
        }
    }       
    

    /**
     * Retreives the specific alert ID record from the database based upon the alert ID
     * 
     * @param alertID
     * @return appropriate Alert recrod, null if not found
     */
    public static SearchAlert retrieveAlert(UUID alertID) {
        logger.log(Level.INFO, "getting alert with alert id");
        SearchAlertDAO alertDao = new SearchAlertDAO();
        return alertDao.retrieveByAlertID(alertID);
    }
    
    public static List<SearchAlertNotification> getAllAlertNotifications(String domain, String userEmailID, boolean showAll) {
        logger.log(Level.INFO, "AlertNotification: getting all alerts notifications.");
        SearchAlertDAO saDAO = new SearchAlertDAO();
        
        List<Object> alertIDs;
        
        if (showAll) {
        	alertIDs = null; //TODO, need to implement when adding a show all feature
        }
        else {
        	List<SearchAlert> alerts = saDAO.getAllAlertsByDomainAndUser(domain, userEmailID);
        	alertIDs =  alerts.stream().map(searchAlert -> searchAlert.getAlertID()).collect(Collectors.toList());
        }
        
        return (new SearchAlertNotificationDAO()).getUnacknowledgedAlertNotifications(alertIDs);
        

    }   
    
    

	/**
	 * Checks for alerts which are in 'waiting' state for user and executes them.
	 */
	public static void executeWaitingAlerts() throws ValidationException, org.springframework.dao.DuplicateKeyException {
		logger.log(Level.INFO, "Executing Waiting Alerts");
		
		String collectorID = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID);

		for(SearchAlert alert: SearchAlert.getAllWaitingAlerts()) {
			
			if (Collector.getTheCollecter().getDomain(alert.getDomain()).isOffline()) {
				logger.log(Level.INFO, "Domain offline("+alert.getDomain()+"), not running search alert: "+alert.getAlertName());
				continue;
			}
			
			
			int numOfAlertsUpdated = alert.claimAlertToRun(collectorID);
			if (numOfAlertsUpdated == 0) {
				logger.log(Level.INFO, "Another process claimed alert: "+alert.getAlertID().toString());
				continue;
			}
			alert.run();
			alert.markAlertRunComplete();
		}
		logger.log(Level.INFO, "Processed Alerts");
	}

	/**
	 * 
	 * @return returns the number of search alerts created, -1 on an error, -2 if no results are returned by the underlying search handler
	 */
	public int run() {
		long startTime = System.currentTimeMillis();
		int numResults = 0;
		
		String searchAPI   = this.getSourceHandler();
		String domain      = this.getDomain();
		String searchTerms = this.getSearchTerm();

		java.util.List<SearchRecord> records;

		int numberOfSearchResults = this.getNumberOfResult();

		JSONObject configuration = new JSONObject();
		JSONObject advConfigObj = new JSONObject();

		DomainDiscoveryInterface ddi = DomainDiscoveryInterface.getSourceHandler(searchAPI);
		
		ddi.setUserAgent(SourceHandlerInterface.getNextUserAgent(domain));
		//ddi.setUserAgent(Configuration.getConfigurationProperty(domain, ConfigurationType.WEBCRAWLER_USERAGENTSTRING));
		records = ddi.generateSearchResults(domain, searchTerms, configuration, numberOfSearchResults, advConfigObj);			
		

		if (records == null || records.size() == 0) {
			logger.log(Level.WARNING, "No search results found for for alert("+this.getAlertID()+"): "+this.getSourceHandler());
			return -2;
		}

		//remove duplicate entries - conversion to the LinkedHashSet does this while maintaining the insert order...
		records = new ArrayList<SearchRecord>(new LinkedHashSet<SearchRecord>(records));

		if (records.size() > numberOfSearchResults) {
			records = new ArrayList<SearchRecord>(records.subList(0,  numberOfSearchResults));
		}

		for(SearchRecord record : records){
			String resultUrl = record.getUrl();
			String resultTitle = record.getName();
			String resultDescription = record.getDescription();

			SearchAlertNotification an = new SearchAlertNotification(this.getAlertID(), resultUrl, resultTitle, resultDescription , Boolean.FALSE,null);

			try {
				an.insert();
				numResults++;
			}
			catch (org.springframework.dao.DuplicateKeyException e){
				; // ignore duplicate key exception, we've already seen this record
			}
		}
		
		//Send instrumentation event
		long endTime = System.currentTimeMillis();
		Instrumentation.createAndSendEvent(this.getDomain(), "daemon.searchAlert.process", "searchAlert", startTime, endTime, this.toJSON(), null);

		return numResults;

	}

	public static List<SearchAlert> retrieveAlerts(String domain, String emailID, boolean showAll) {
        logger.log(Level.FINE, "Alert: getting alerts");
        SearchAlertDAO saDAO = new SearchAlertDAO();

        if (showAll) {
        	return saDAO.getAllAlertsByDomain(domain);
        }
        else {
        	return saDAO.getAllAlertsByDomainAndUser(domain, emailID);
        
        }
	}


  
}
