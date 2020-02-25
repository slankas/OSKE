package edu.ncsu.las.collector;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.document.DocumentHandler;
import edu.ncsu.las.model.EmailClient;
import edu.ncsu.las.model.EmailConfiguration;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.SiteCrawlRule;
import edu.ncsu.las.model.collector.type.ConfigurationLocation;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.persist.DBConstants;
import edu.ncsu.las.persist.DBInitializer;
import edu.ncsu.las.persist.TestDAO;
import edu.ncsu.las.source.AbstractHandler;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.storage.AccumuloStorage;
import edu.ncsu.las.storage.ElasticSearch;
import edu.ncsu.las.storage.FileStorage;
import edu.ncsu.las.storage.KafkaQueue;
import edu.ncsu.las.util.crypto.AESEncryption;
import edu.ncsu.las.util.json.JSONMinify;

/**
 * The "Collector" manages the overall "state" of the system...
 * 
 *
 */
public class Collector  {
	private static Logger logger =Logger.getLogger(Collector.class.getName());
	
	private java.util.HashMap<String, AbstractHandler> _sourceHandlers;
	private java.util.List<DocumentHandler> _documentHandlers;

    private HashMap<String,HashMap<String,SiteCrawlRule>> _siteRules;
	
	private Configuration _baseConfiguration; // this is the configuration data that is read from the file. System must be restarted for this to take affect.
	private java.util.Map<String, Domain> _domains;
	private long _lastDomainRefreshTime = -1L; // when was the "domains" object last rebuilt??
	
	
	
	
	private EmailClient _emailClient;
	
	private ExecutorService _taskPool;
	
	private static Collector _theCollector;
	
	/**
	 * 
	 * 
	 * @param directory
	 * @param configurationFile
	 * @param validateProcessingQueues
	 * @param loadSiteRules should the siteRules table be loaded?  generally not needed for tests
	 * @param sleepUntilDatabaseReady if set to true, the program will check if the system domain is available in the system_domains table.  As long as no result is returned,
	 *                                the program will go to sleep for a minute and then check again.
	 * @return
	 * @throws IOException
	 */
	public static synchronized Collector initializeCollector(String directory, String configurationFile, boolean validateProcessingQueues, boolean loadSiteRules, boolean sleepUntilDatabaseReady) throws IOException {
		if (_theCollector == null) {
			_theCollector = new Collector();
			_theCollector.initialize(directory, configurationFile, validateProcessingQueues, loadSiteRules, sleepUntilDatabaseReady);
		} 
		return _theCollector;
	}
	
	/**
	 * 
	 * 
	 * @param directory
	 * @param configurationFile
	 * @return
	 * @throws IOException
	 */
	public static synchronized Collector initializeCollector() throws IOException {
		if (_theCollector == null) {
			_theCollector = new Collector();
			//_theCollector.initialize(directory, configurationFile, validateProcessingQueues, loadSiteRules);
		}
		return _theCollector;
	}	
	
	public static Collector getTheCollecter() {
		if (_theCollector == null) {
			throw new IllegalAccessError("Collector must be created through initializeCollector first.");
		}
		return _theCollector;
	}
	
	// private to make this a singleton.
	private Collector()  {
	}
	
	/**
	 * Loads the configuration file(s) and places them into the _properties object, which can be exposed
	 * to others through the "getConfiguration" call.
	 * 
	 * This method also initializes the database configuration.
	 * 
	 * The collector system requires a configuration file (which are in a specified in json formatted files.)
	 * The minify call exists to remove any comments that exist in those files as comments are not part of the actually 
	 * JSON specification, but do serve a useful pupose to explain the parameters.
	 * 
	 * @param directory where does the configuration files exist?
	 * @param configurationFile
	 * @throws InterruptedException 
	 */
	private void initialize(String directory, String configurationFile, boolean validateProcessingQueues, boolean loadSiteRules, boolean sleepUntilDatabaseReady) {
		
		Annotator.validateCodesNotReserved(); // check to make sure we are not using any invalid codes
		
		try {
			// Load the properties file
			String content = JSONMinify.minify(new String(Files.readAllBytes(this.getActualConfigFile(directory,configurationFile))));
			//System.out.println(content);
			JSONObject fileProperties = new JSONObject(content);			
			_baseConfiguration = new Configuration("base", fileProperties);

		}
		catch (IOException ioe) {
			logger.log(Level.SEVERE, "Unable to read configuration property file",ioe);
			System.exit(-1);
		}
		catch (JSONException je) {
			logger.log(Level.SEVERE, "Unable to read configuration property file - malformed json: " +je);
			logger.log(Level.SEVERE, "  main configuration file: "+configurationFile);

			System.exit(-1);
		}		
		
		// Check all required properties are present:
		java.util.List<ConfigurationType> missingProperties = _baseConfiguration.findMissingConfigurationElements(ConfigurationLocation.FILE);
		if (missingProperties.size() > 0) {
			for (ConfigurationType ct: missingProperties) {
				logger.log(Level.SEVERE, "Missing parameter - "+ct.getFullLabel()+": "+ct.getDescription());
			}
			logger.log(Level.SEVERE, "Exiting System - not all required properties are present.");
			System.exit(-1);
		}
		logger.log(Level.INFO,"Base configuration loaded");
		
		// Encrypt if necessary ...
		if (_baseConfiguration.checkConfigurationForEncryptedFields() == true) {
			try {
				Files.write(this.getActualConfigFile(directory,configurationFile), _baseConfiguration.getConfiguration().toString(4).getBytes(StandardCharsets.UTF_8));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unable to save file parameters after encryption", e);
				System.exit(-5);
			}
			logger.log(Level.WARNING, "Updated system_properties.json - parameter has been encrypted");
		}
		
		// initialize the database connection.  will need to decrypt database password
		JSONObject databaseConfiguration = _baseConfiguration.getConfigurationObject(ConfigurationType.DATABASE);
		databaseConfiguration.put("password", this.decryptValueWithFileKey(databaseConfiguration.getString("password")));	
		DBInitializer.initialize(databaseConfiguration, DBConstants.CONNECTION_AW);
		databaseConfiguration = null;
		
		TestDAO td = new TestDAO();
		logger.log(Level.INFO,"Database status: "+td.getPostgreSQLVersion());
		
		if (sleepUntilDatabaseReady) {
			sleepUntilDatabaseReady();
		}

		this.refreshDomains(validateProcessingQueues, loadSiteRules);
		this.initializeInternalProcessingQueues(validateProcessingQueues);
		Instrumentation.initializeInstrumentation();
	}

	private void sleepUntilDatabaseReady() {
		long sleepTime = 30;
		while(true) {
			try {
				Domain d = Domain.findDomain(Domain.DOMAIN_SYSTEM);
				if (d != null) {
					logger.log(Level.INFO,"SYSTEM domain found in database - system has been initialized.");
					break;
				}
			}
			catch (Exception e) {
				logger.log(Level.INFO,"Initialization check exception: "+e.toString());
			}
			logger.log(Level.INFO,"Initialization check failed - sleeping "+sleepTime + " seconds");
			try {
				TimeUnit.SECONDS.sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.log(Level.INFO,"Initialization check interrupted exception: "+e.toString());
			}
			sleepTime = Math.max(sleepTime+10, 120); // sleep longer next time....
		}
	}
		
	private void initializeInternalProcessingQueues(boolean loadSiteRules) {
		_sourceHandlers = AbstractHandler.getAllSourceHandlers();
		_documentHandlers = DocumentHandler.getDocumentHandlers();		
		
		_emailClient = new EmailClient(new EmailConfiguration(Configuration.getConfigurationObject(Domain.DOMAIN_SYSTEM,ConfigurationType.EMAIL)));
		_taskPool = Executors.newFixedThreadPool(Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.COLLECTOR_TASK_POOLSIZE));
	}
	
	public void refreshDomains() {
		refreshDomains(true,true);
	}
	
	public void refreshDomains( boolean validateDomainEnvironment, boolean loadSiteRules) {
		logger.log(Level.INFO, "Domains - initializing/refreshing");
		
		_domains = Domain.findActiveDomainsAsMap();
		
		if (_domains.containsKey(Domain.DOMAIN_SYSTEM) == false) {
			logger.log(Level.SEVERE, "Exiting System - system domain is not present in the domain table.");
			System.exit(-1);			
		}
		
		//_domains.values().stream().forEach(d -> { d.checkConfigurationForEncryptedFields(); }); // this check needs to come before initialize configuratio as the initialize method will bring down parent values...
		
		Configuration.initializeConfiguration(_baseConfiguration, _domains);
		
		_domains.values().stream().forEach(d -> { d.initializeAfterConfiguration();   });
		
		
		_lastDomainRefreshTime = System.currentTimeMillis();
		
		if (loadSiteRules) {
			_siteRules = SiteCrawlRule.getAllSiteRules();              //Load all site rules
		}
		
		if (validateDomainEnvironment) {
			validateDomainEnvironment();
		}
	}
	
	public void validateDomainEnvironment() {
		KafkaQueue.checkAllQueues(_domains.keySet());             // Create KafkaTopics if necessary
		ElasticSearch.checkAllIndexExistence(_domains.keySet());  // Create ElasticSearch indices as necessary
		AccumuloStorage.validateEnvironment(_domains.keySet());   // Ensure storage tables exist in accummulo
		boolean fileResult = FileStorage.validateEnvironment(_domains.keySet());       // Ensure directory structure exists
		if (fileResult == false) {
			logger.log(Level.SEVERE, "Exiting System - unable to create file storage directories");
			System.exit(-1);							
		}
	}
	
	private Path getActualConfigFile(String directory, String fileName) {
		return Paths.get(directory,fileName);	
	}
	
	/**
	 * Returns when the domains were last refreshed. 
	 * Value generated by System.currentTimeMillis();
	 * 
	 * @return timestamp of when domain weas last refreshed
	 */
	public long getLastDomainRefreshTime() {
		return _lastDomainRefreshTime;
	}
	
	
	public Configuration getBaseConfiguration() {
		return _baseConfiguration;
	}
	
	public EmailClient getEmailClient() {
		return _emailClient;
	}
	
	public java.util.HashMap<String, AbstractHandler> getSourceHandlers() {		return _sourceHandlers; 	}
	public java.util.ArrayList<SourceHandlerInterface> getSourceHandlersForJobs(String domain) {	return AbstractHandler.getSourceHandlersForJobs(domain); 	}
	public java.util.ArrayList<SourceHandlerInterface> getSourceHandlersForDomainDiscovery(String domain) {		return AbstractHandler.getSourceHandlersForDomainDiscovery(domain); 	}
	public SourceHandlerInterface getSourceHandler(String name) {		return AbstractHandler.getSourceHandler(name); 	}
	public java.util.List<DocumentHandler> getDocumentHandlers() { return _documentHandlers; }
	public java.util.List<Annotator> getAnnotators() { return Annotator.getAllAnnotators(); }
	public HashMap<String,SiteCrawlRule> getSiteRules(String domain) {	return _siteRules.get(domain); }

	
	public Domain getDomain(String domainName) {
		return _domains.get(domainName);
	}
	
	public java.util.Map<String, Domain> getAllDomains() {
		return _domains;
	}
	
	public java.util.List<Domain> getDomainsSorted() {
		java.util.List<Domain> result = new java.util.ArrayList<Domain>(_domains.values());
		Collections.sort(result, new Comparator<Domain>() {
			@Override
			public int compare(Domain o1, Domain o2) {
				return o1.getFullName().compareTo(o2.getFullName());
			}
	    });
		return result;
	}
	
	public void runTask(Runnable task) {
		_taskPool.execute(task);
	}
	

	
	
		
	private static String collectorPrivatePhrase = "O beautiful for spacious skies,For amber waves of grain, For purple mountain majesties";
	
	public String encryptValue(String value) {
		String[] keys = {collectorPrivatePhrase, Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.SECRET_PHRASE) };
		
		return encryptValue(value,keys);
	}
	
	public String encryptValueWithFileKey(String value) {
		String[] keys = {collectorPrivatePhrase, _baseConfiguration.getConfigurationProperty(ConfigurationType.SECRET_PHRASE,null) };
		
		return encryptValue(value,keys);
	}
	
	
	public String encryptValue(String value, String[] keys) {
		AESEncryption.Key k = new AESEncryption.Key(keys);
		AESEncryption aes = new AESEncryption(k);
		String encrypyedValue =  "{AES}" + aes.encryptToBase64(value);
		
		return encrypyedValue;
	}
	
	
	
	public String decryptValue(String value) {		
		String[] keys = {collectorPrivatePhrase, Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.SECRET_PHRASE) };
		
		return decryptValue(value,keys);
	}	

	public String decryptValueWithFileKey(String value) {
		String[] keys = {collectorPrivatePhrase, _baseConfiguration.getConfigurationProperty(ConfigurationType.SECRET_PHRASE,null) };
		
		return decryptValue(value,keys);
	}
	
	
	public String decryptValue(String value, String[] keys) {
		if (value.startsWith("{AES}")) { value = value.substring(5); }
		else {
			return value; //assumes that if "AES" is not present, the value is not encrypted.
		}
		
		AESEncryption.Key k = new AESEncryption.Key(keys);
		AESEncryption aes = new AESEncryption(k);
		String decryptedValue =  aes.decryptFromBase64ToString(value);
		
		return decryptedValue;
	}	
	
	
}
