package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.JobCollector;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.FileStorageStatusCode;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.persist.collector.DomainDAO;
import edu.ncsu.las.persist.collector.DomainDiscoveryDAO;
import edu.ncsu.las.persist.collector.JobHistoryDAO;
import edu.ncsu.las.persist.collector.VisitedPageDAO;
import edu.ncsu.las.source.AbstractHandler;
import edu.ncsu.las.storage.AccumuloStorage;
import edu.ncsu.las.storage.ElasticSearchREST;
import edu.ncsu.las.storage.FileStorage;
import edu.ncsu.las.storage.HDFSStorage;
import edu.ncsu.las.util.DateUtilities;



/**
 * Represents a particular "domain" that OpenKE collects and analyzes data upon.
 * Generally, domains are an area that is being investigated.
 * 
 * Domains are significant in that they form the "top-level" buckets in which
 * crawled data is stored for ElasticSearch, Accumulo, files (duplicative of accumulo), 
 * graph database, etc....
 * 
 * Change History
 * 
 */
public class Domain {
	private static Logger logger =Logger.getLogger(Domain.class.getName());
	
	public static final String DOMAIN_SYSTEM = "system"; // special domain used to store the "base" level configuration used by all domains...
	
	public static final String DOMAIN_STATUS_ACTIVE   = "active";
	public static final String DOMAIN_STATUS_INACTIVE = "inactive"; // data kept, no jobs may execute.
	public static final String DOMAIN_STATUS_REMOVED  = "removed";  // all data has been removed. domain no longer visible within the application
	
	
	/** array represents domain names that may not be used - they have specific functionality within the system.
	 *  Note: "system" is the only one of these currently in use.  Others listed to avoid naming/semantic conflicts
	 */ 
	public static final String[] RESERVED_NAMES = {"system", "base", "domain", "user"};
	
	/** 
	 * This is a the primary "abbreviated" name that is used to create ElasticSearch Indexes,
	 * AccumuloTables, folders, etc...  15 characters or less.  must be all lowercase.
	 */
	private String _domainInstanceName;
	
	/**
	 * Possible values: "active", "inactive", "removed"  
	 */
	private String _domainStatus;
	private Instant _effectiveTimestamp; 
	private String _fullName;
	private String _description;
	private String _primaryContact;
	private int _appearanceOrder;
	private JSONObject _configuration;
	private String _userEmailAddress;  // who last changed the record
	private Instant _insertTimestamp;  // when was the record last changed
	private boolean _offline;          // is this domain currently offline?  if so, don't allow users to use it, or allow jobs to run ...
	
	/** When was this domain first created/established?  Compute lazily as we need to load the first record.  value is then cached */
	private Instant _establishedTimeStamp = null;
	
	public Domain(String domainInstanceName, String domainStatus, Timestamp effectiveTimestamp, String fullName,
			      String description, String primaryContact, int appearanceOrder, String configuration,
			      String userEmailAddress, Timestamp insertTimestamp, boolean offline) {	
		_domainInstanceName = domainInstanceName;
		_domainStatus = domainStatus;
		_effectiveTimestamp = effectiveTimestamp.toInstant(); 
		_fullName = fullName;
		_description = description;
		_primaryContact = primaryContact; 
		_appearanceOrder = appearanceOrder;
		_configuration = new JSONObject(configuration);
		_userEmailAddress = userEmailAddress;  
		_insertTimestamp  = insertTimestamp.toInstant();  
		_offline         = offline;
	}
	
	public Domain(JSONObject domainJSON) {
		_domainInstanceName = domainJSON.getString("domainInstanceName");
		_domainStatus       = "active";
		_effectiveTimestamp = Instant.now(); 
		_fullName           = domainJSON.getString("fullName");
		_description        = domainJSON.getString("description");
		_primaryContact     = domainJSON.getString("primaryContact"); 
		_appearanceOrder    = domainJSON.getInt("displayOrder");
		_configuration      = domainJSON.getJSONObject("configuration");
		_userEmailAddress   = "";  
		_insertTimestamp    = Instant.now();
		_offline            = domainJSON.getBoolean("offline");
	}

	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		_domainInstanceName = domainInstanceName;
	}

	public String getDomainStatus() {
		return _domainStatus;
	}

	public void setDomainStatus(String domainStatus) {
		_domainStatus = domainStatus;
	}

	public Instant getEffectiveTimestamp() {
		return _effectiveTimestamp;
	}

	public void setEffectiveTimestamp(Instant effectiveTimestamp) {
		_effectiveTimestamp = effectiveTimestamp;
	}
	
	public java.sql.Timestamp getEffectiveTimestampAsTimeStamp() {
		return Timestamp.from(_effectiveTimestamp);
	}	

	public String getFullName() {
		return _fullName;
	}

	public void setFullName(String fullName) {
		_fullName = fullName;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		_description = description;
	}

	public String getPrimaryContact() {
		return _primaryContact;
	}

	public void setPrimaryContact(String primaryContact) {
		_primaryContact = primaryContact;
	}

	public int getAppearanceOrder() {
		return _appearanceOrder;
	}

	public void setAppearanceOrder(int appearanceOrder) {
		_appearanceOrder = appearanceOrder;
	}

	public JSONObject getConfiguration() {
		return _configuration;
	}

	public void setConfiguration(JSONObject configuration) {
		_configuration = configuration;
	}

	public String getUserEmailAddress() {
		return _userEmailAddress;
	}

	public void setUserEmailAddress(String userEmailAddress) {
		_userEmailAddress = userEmailAddress;
	}

	public Instant getInsertTimestamp() {
		return _insertTimestamp;
	}

	public void setInsertTimestamp(Instant insertTimestamp) {
		_insertTimestamp = insertTimestamp;
	}

	public java.sql.Timestamp getInsertTimestampAsTimeStamp() {
		return Timestamp.from(_insertTimestamp);
	}	
	
	public void setOffline(boolean newValue) { _offline = newValue; }
	
	public boolean isOffline() { return _offline; }
	
	/**
	 * Finds the oldest record for this domain and then returns that timestamp as an Instant.
	 * Computed lazily (on demand) from the first created record for a domain.
	 * 
	 * @return
	 */
	public Instant getEstalblishedTimeStamp() {
		if (_establishedTimeStamp == null) {
			_establishedTimeStamp = (new DomainDAO()).retrieveLeastEffectiveDomainRecord(this.getDomainInstanceName()).getEffectiveTimestamp();
		}
		return _establishedTimeStamp;
	}	

	
	public boolean create(String userEmailID) {
		this.setUserEmailAddress(userEmailID);
		return (new DomainDAO()).insertDomain(this);
	}
	
	/**
	 * Runs the initializes and processes any source handlers marked as services.
	 * 
	 * In most cases, these source handlers will initiate their own threads and then return.
	 */
	public void startSourceServices() {
		String domainName = this.getDomainInstanceName();
		if (domainName.equals(Domain.DOMAIN_SYSTEM)) { return; }
		
		for (AbstractHandler shi: AbstractHandler.getAllSourceHandlers().values()) {
			if (shi.isService()) {
				logger.log(Level.INFO, domainName+": starting service source handler: "+shi.getSourceHandlerName());
				
				UUID jobUUID = new UUID(1,0);
				UUID jobHistoryID = edu.ncsu.las.util.UUID.createTimeUUID();
				JobHistory jh = JobHistory.initiateJob(jobHistoryID, jobUUID, shi.getSourceHandlerName(), JobHistoryStatus.PROCESSING, "",
						 Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID),domainName);
				
				Job serviceJob = new Job();
				serviceJob.setDomainInstanceName(domainName);
				serviceJob.setConfig(new JSONObject());
				shi.initialize(jh, serviceJob);
				shi.startService();						
			}	
		}

	}	
	
	
	/**
	 * Runs the initializes and processes any source handlers marked as interactive.
	 * Generally speaking, the daemon process won't call this - only the web application.
	 * 
	 * In most cases, these source handlers will initiate their own threads and then return.
	 */
	public void startInteractiveServices() {
		String domainName = this.getDomainInstanceName();
		if (domainName.equals(Domain.DOMAIN_SYSTEM)) { return; }
		
		for (AbstractHandler shi: AbstractHandler.getAllSourceHandlers().values()) {
			if (shi.isInteractive()) {	
				logger.log(Level.INFO, domainName+": starting interactive source handler: "+shi.getSourceHandlerName());

				UUID jobUUID = new UUID(1,0);
				UUID jobHistoryID = edu.ncsu.las.util.UUID.createTimeUUID();
				JobHistory jh = JobHistory.initiateJob(jobHistoryID, jobUUID, shi.getSourceHandlerName(), JobHistoryStatus.PROCESSING, "",
						 Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_ID),domainName);
				Job interactiveJob = new Job();
				interactiveJob.setDomainInstanceName(domainName);
				interactiveJob.setConfig(new JSONObject());
				shi.initializeInteractiveService(JobCollector.getTheCollecter(), jh, interactiveJob);								
			}
		}
	}	
	

	/**
	 * Deletes all of the crawled / collected data for a domain.  Validates that the domain supoports
	 * 
	 * @return on success returns an empty JSON array.  On failure, returns which error occured
	 */
	public JSONObject purge() {
		JSONObject errorLog = new JSONObject();
		
		String domain = this.getDomainInstanceName();
		if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.DOMAIN_PURGE) == false) {
			errorLog.put( "message", "Domain does not allow interactive purging");
			return errorLog;
		}

		for (FileStorageAreaType fsat: FileStorageAreaType.values()) {
			if (ElasticSearchREST.deleteIndex(domain, fsat) == false) { errorLog.put( "elasticsearch_"+fsat.getLabel(), "failed"); }
			if (AccumuloStorage.deleteTable(domain, fsat) != FileStorageStatusCode.SUCCESS ) { errorLog.put( "accumulo_"+fsat.getLabel(), "failed"); }
			if (HDFSStorage.deleteEntireDomainAndArea(domain, fsat)          != FileStorageStatusCode.SUCCESS ) { errorLog.put( "hdfs_"+fsat.getLabel(), "failed"); }
			if (FileStorage.deleteDirectory(domain, fsat) != FileStorageStatusCode.SUCCESS ) { errorLog.put( "file_"+fsat.getLabel(), "failed"); }
		}	
	
		// clean up database information
		try {
			(new JobHistoryDAO()).deleteByDomain(domain);
			(new DomainDiscoveryDAO()).deleteDiscoverySessionByDomain(domain);
			(new VisitedPageDAO()).deleteByDomain(domain);
			DiscoveryIndex.purgeDomain(domain);
			DocumentBucket.purgeDomain(domain);
			Project.purgeDomain(domain);
			ProjectDocument.purgeDomain(domain);
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "Unable to purge domain records", t);
			errorLog.put("database", "unable to purge - see logs for exception");
		}
		
		Collector.getTheCollecter().validateDomainEnvironment();	// this rebuilds the domain table information	
		this.startInteractiveServices();  // establishes a new job history for the domain discovery handler.  may need more logic to ensure this only occurs in the web app (which as of May 2018 is the only place this method is called from  
		
		return errorLog;
	}
	
	
	public JSONObject toJSON() {
		boolean allowDomainPurge = false;
		try {
			allowDomainPurge = Configuration.getConfigurationPropertyAsBoolean(this.getDomainInstanceName(), ConfigurationType.DOMAIN_PURGE);
		}
		catch (Throwable t) {
			allowDomainPurge = false;
		}
		JSONObject result = new JSONObject().put("domainInstanceName",this.getDomainInstanceName())
				                            .put("domainStatus",this.getDomainStatus())
				                            .put("effectiveTimestamp", DateUtilities.getDateTimeISODateTimeFormat(this.getEffectiveTimestamp()))
				                            .put("fullName",this.getFullName())
				                            .put("description",this.getDescription())
				                            .put("primaryContact",this.getPrimaryContact())
				                            .put("appearanceOrder",this.getAppearanceOrder())
				                            .put("configuration",this.getConfiguration())
				                            .put("userEmailAddress",this.getUserEmailAddress())
				                            .put("allowOnlineDomainPurge", allowDomainPurge)
				                            .put("offline", this.isOffline())
				                            .put("insertTimestamp",DateUtilities.getDateTimeISODateTimeFormat(this.getInsertTimestamp()));
 
		return result;
	}

	
	public static java.util.Map<String,Domain> findActiveDomainsAsMap() {
		java.util.Map<String,Domain> result = new java.util.HashMap<String,Domain>();
		
		java.util.List<Domain> domains = Domain.findAllActiveDomains();
		for (Domain d: domains) {
			result.put(d._domainInstanceName, d);
		}
		
		return result;

	}

	/**
	 * Returns the most effective dated records for all active domains
	 * 
	 * @param domainStr
	 * @return
	 */
	public static java.util.List<Domain> findAllActiveDomains() {
		return (new DomainDAO()).retrieveActiveDomains();
	}

	/**
	 * Returns the most effective dated record for a domain that is not in the future.
	 * 
	 * Note: This call utilizes the database to get this value.  Collector maintains a cache of the domain data
	 * 
	 * @param domainStr
	 * @return
	 */
	public static Domain findDomain(String domainStr) {
		return (new DomainDAO()).retrieveEffectiveDomainRecord(domainStr);
	}

	
	private static Pattern domainInstanceNamePattern = Pattern.compile("[^a-z0-9]");
	private static Pattern fullNamePattern           = Pattern.compile("[<>'\"]");
	private static Pattern primaryContactPattern     = Pattern.compile("[^a-zA-Z0-9 ,\\-\\.]");
	
	public static JSONArray validate(JSONObject domainJSON, boolean checkReservedDomainNames) {
		JSONArray errors = new JSONArray();
		
	
		String domainInstanceName = domainJSON.optString("domainInstanceName","");
		String fullName           = domainJSON.optString("fullName","");
		String description        = domainJSON.optString("description");
		String primaryContact     = domainJSON.optString("primaryContact");
		String displayOrder       = domainJSON.optString("displayOrder");
		JSONObject configuration  = domainJSON.optJSONObject("configuration");
		
		// Domain Identifier
		if ( domainInstanceName == null || domainInstanceName.trim().equals("")) {
			errors.put("No domain identifier presesnt");
		}
		else {
			domainInstanceName = domainInstanceName.trim();
			if (domainInstanceName.length() > 15) {
				errors.put("The domain identifier can only have a maximum of 15 characters.");
			}
			else {
				Matcher m = domainInstanceNamePattern.matcher(domainInstanceName);
				if (m.find()) {
					errors.put("The domain identifier can only contain lowercase characters and numbers.");
				}
			}
			if (checkReservedDomainNames) {
				for (String reservedName: RESERVED_NAMES) {
					if (domainInstanceName.equals(reservedName)) {
						errors.put(reservedName + " is a reserved domain name and cannot be used.");
					}
				}
			}
		}	

		//Full Name
		if ( fullName == null || fullName.trim().equals("") ) {
			errors.put("No full name present");
		}
		else {
			fullName = fullName.trim();
			if (fullName.length() > 100) {
				errors.put("The full name must be less than or equal to 100 characters.");
			}
			else {
				Matcher m = fullNamePattern.matcher(fullName);
				if (m.find()) {
					errors.put("The full name can not contain <,>,', or \".");
				}
			}	
		}
		
		//Description
		if ( description == null || description.trim().equals("") ) {
			errors.put("No description present.");
		}
		else {
			description = description.trim();
			if (description.length() > 1024) {
				errors.put("The description must be less than 1025 characters.");
			}
		}	
		
		//Primary Contact
		if ( primaryContact == null || primaryContact.trim().equals("") ) {
			errors.put("No primary contact present.");
		}
		else {
			primaryContact = primaryContact.trim();
			if (primaryContact.length() > 100) {
				errors.put("The primary contact must be less than 101  characters.");
			}
			else {
				Matcher m = primaryContactPattern.matcher(primaryContact);
				if (m.find()) {
					errors.put("The primary contact can only alphabetical characters, numbers, spaces, dashes, periods, and commas.");
				}
			}
		}
		
		//Display Order
		if ( displayOrder == null || displayOrder.trim() == "" ) {
			errors.put("No display order.");
		}
		else {
			try {
				int dispOrder = Integer.parseInt(displayOrder);
				if (dispOrder < 0) {
					errors.put("The display order must be a non-negative integer.");
				}
			}
			catch (NumberFormatException nfe) {
				errors.put("The display order must be a non-negative integer.");
			}
		}
		
		//Configuration
		if ( configuration == null) {
			errors.put("No configuration present.");
		}

		return errors;
	}

	/**
	 * Perform an initialization code necessary for a domain after the configuration has been established for it.
	 */
	public void initializeAfterConfiguration() {
		// build the list of top level domains that should be blocked.
		JSONArray domains = Configuration.getConfigurationPropertyAsArray(this.getDomainInstanceName(), ConfigurationType.BLOCKED_TLDS);
		_blockedTopLevelDomains = new java.util.HashSet<String>();
		domains.forEach(o -> _blockedTopLevelDomains.add(o.toString()));
	}
	
	Set<String> _blockedTopLevelDomains  = null;

	public boolean isTopLevelDomainBlocked(String domainName) {
		int lastPeriod = domainName.lastIndexOf('.');
		if (lastPeriod < 0 ) {
			logger.log(Level.INFO, "Received invalid domain name: "+domainName); // for some internal sites (and docker networks), this may be valid, allow
			return false;
		}
		String tld = domainName.substring(lastPeriod);
		return _blockedTopLevelDomains.contains(tld);
	}

	/**
	 * Checks the current configuration for encrypted values.
	 */
	public void checkConfigurationForEncryptedFields() {
		// TODO Auto-generated method stub
		
	}
	

}