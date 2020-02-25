package edu.ncsu.las.model.collector;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.type.ConfigurationLocation;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.ConfigurationTypeInterface;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.util.json.JSONUtilities;
import edu.ncsu.las.util.json.JSONUtilities.JSONType;

/**
 * 
 * Special areas:
 *   base:  file-level configuration, 
 *   system:  overall configuration that domains could build from/override ...
 *
 */
public class Configuration {
	private static Logger logger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	private static HashMap<String, Configuration> _configurations = new HashMap<>();
	
	private JSONObject _properties;
	private String _domain;
	
	public static final String SAMPLE_DOMAIN_CONFIGURATION = "{\"allowOnlineDomainPurge\":\"true\",\"textrankAPI\":\"http://MICROSERVICES:8000/textrank/\",\"import\":{\"sleepTimeSec\":300},\"annotations\":[\"referrer\",\"http_headers\",\"structured_data\",\"html_title\",\"html_meta\",\"open_graph\",\"html_outlinks\",\"provenance\",\"dataHeader\",\"language\",\"tikaMetaData\",\"published_date\",\"concepts\"],\"graphAPI\":\"http://MICROSERVICES:9001/GraphAPI/\",\"secretPhrase\":\"a deep, really deep, dark secret\",\"webCrawler\":{\"maxPagesToFetch\":-1,\"politenessDelay\":500,\"includeBinaryContentInCrawling\":true,\"maxDepthOfCrawling\":-1,\"userAgentString\":\"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36\",\"maxDownloadSize\":20000000},\"accumulo\":{\"password\":\"ACCUMULO_PASSWORD\",\"instanceName\":\"hdp-accumulo-instance\",\"zooKeepers\":\"ZOOKEEPER_SERVER:ZOOKEEPER_PORT\",\"userName\":\"ACCUMULO_USER\",\"storeRaw\":{\"normal\":true,\"sandbox\":false,\"archive\":false}},\"file\":{\"storeRaw\":{\"normal\":true,\"sandbox\":false,\"archive\":false}},\"elastic\":{\"default\":{\"settings\":{\"settings\":{\"index\":{\"mapping\":{\"ignore_malformed\":\"true\"},\"number_of_shards\":5,\"number_of_replicas\":1}}},\"mappings\":{\"web\":{\"properties\":{\"open_graph\":{\"type\":\"object\",\"enabled\":false},\"domainDiscovery\":{\"properties\":{\"relevant\":{\"type\":\"boolean\"},\"relevantUser\":{\"type\":\"string\"},\"description\":{\"type\":\"string\"},\"retrievals\":{\"type\":\"nested\",\"properties\":{\"executionNumber\":{\"type\":\"long\"},\"sessionID\":{\"type\":\"string\",\"fields\":{\"raw\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}},\"userID\":{\"type\":\"string\"},\"uuid\":{\"type\":\"string\"}}},\"title\":{\"type\":\"string\"},\"url\":{\"type\":\"string\",\"fields\":{\"raw\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}}}},\"provenance\":{\"properties\":{\"configuration\":{\"properties\":{\"allowSingleHopFromReferrer\":{\"type\":\"string\"}}},\"job\":{\"properties\":{\"url\":{\"type\":\"string\",\"fields\":{\"raw\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}}}}}},\"concepts\":{\"type\":\"nested\",\"properties\":{\"name\":{\"type\":\"string\"},\"fullName\":{\"type\":\"string\"},\"position\":{\"type\":\"long\"},\"category\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}},\"http_headers\":{\"type\":\"object\",\"enabled\":false},\"user_collection\":{\"type\":\"nested\",\"properties\":{\"collection_id\":{\"index\":\"not_analyzed\",\"type\":\"string\"},\"collection_name\":{\"type\":\"string\"}}},\"structured_data\":{\"type\":\"object\",\"enabled\":false},\"html_meta\":{\"type\":\"object\",\"enabled\":false},\"url\":{\"type\":\"string\",\"fields\":{\"raw\":{\"index\":\"not_analyzed\",\"type\":\"string\"}}}}}}},\"port\":9300,\"clusterName\":\"elasticsearch\",\"host\":\"ELASTIC_PORT\",\"restEndPoint\":\"http://ELASTIC_SERVER:ELASTIC_PORT/\",\"storeJSON\":{\"normal\":true,\"sandbox\":true,\"archive\":false}},\"hdfs\":{\"hdfsUser\":\"hdfs\",\"hdfsBaseURI\":\"hdfs://HDFS_SERVER/\",\"storeJSON\":{\"normal\":true,\"sandbox\":false,\"archive\":false},\"storeRaw\":{\"normal\":true,\"sandbox\":false,\"archive\":false}},\"faroo\":{\"key\":\"FAROO_KEY\"},\"export\":{\"maxRecordCount\":1000000,\"voyant\":{\"maxRecordCount\":1000,\"url\":\"http://VOYANT_SERVER:VOYANT_IP\"},\"timeToLiveHours\":25},\"email\":{\"server\":\"smtp.gmail.com\",\"password\":\"EMAIL_PASSWORD\",\"port\":587,\"user\":\"EMAIL_USER\"},\"hyperlinks\":[{\"displayText\":\"Kibana\",\"link\":\"http://KIBANA_SERVER:KIBANA_PORT\"},{\"displayText\":\"Voyant\",\"link\":\"http://VOYANT_SERVER:VOYANT_IP\"},{\"displayText\":\"HDP\",\"link\":\"http://HDP_MASTER_SERVER:HDP_MASTER_PORT\"}],\"instrumentation\":{\"projectID\":\"LAS/CollectorDev\",\"api\":\"SKYLR_LITE_API\",\"token\":\"INSTUMENTATION_TOKEN\",\"sendEvents\":true},\"userAgent\":[\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36\",\"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:64.0) Gecko/20100101 Firefox/64.0\",\"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36\",\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36\",\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36\",\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0.2 Safari/605.1.15\",\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134\",\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:64.0) Gecko/20100101 Firefox/64.0\"]}";
	
	
	public static void initializeConfiguration(Configuration baseConfiguration, java.util.Map<String,Domain> domains ){
		_configurations = new java.util.HashMap<String,Configuration>();
		
		Configuration systemConfiguration = baseConfiguration.overrideSetProperties(Domain.DOMAIN_SYSTEM, domains.get(Domain.DOMAIN_SYSTEM).getConfiguration());
		java.util.List<ConfigurationType> missingProperties = systemConfiguration.findMissingConfigurationElements(ConfigurationLocation.SYSTEM);
		if (missingProperties.size() > 0) {
			for (ConfigurationType ct: missingProperties) {
				logger.log(Level.SEVERE, "System missing parameter - "+ct.getFullLabel()+": "+ct.getDescription());
			}
		}
		
		_configurations.put(Domain.DOMAIN_SYSTEM, systemConfiguration);
		logger.log(Level.INFO, "Configuration complete: "+Domain.DOMAIN_SYSTEM);
		for (String domainName: domains.keySet()) {
			if (domainName.equals(Domain.DOMAIN_SYSTEM)) { continue; }
			
			Configuration domainConfig = systemConfiguration.overrideSetProperties(domainName, domains.get(domainName).getConfiguration());
			
			_configurations.put(domainName, domainConfig);
			logger.log(Level.INFO, "Configuration complete: "+domainName);
		}
	}
	
	/**
	 * 
	 * @param domain
	 * @return
	 */
	public static Configuration getDomainConfiguration(String domain) {
		if (_configurations.containsKey(domain) == false) {
			throw new IllegalStateException("No such domain exists: "+domain);
		}
	
		return _configurations.get(domain);
	}
	
	
	private static String _daemonTimeStamp = null;
	public static String getCollectorDaemonBuildTimestamp() {
		if (_daemonTimeStamp == null) {
			try {
				_daemonTimeStamp = "notFound";
				Enumeration<URL> resources = Configuration.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
				while (resources.hasMoreElements()) {
				
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					if (manifest.getMainAttributes().getValue("CollectorBuildTime") != null) {
						_daemonTimeStamp = manifest.getMainAttributes().getValue("CollectorBuildTime");
						break;
					}
				}
				
			}
			catch (IOException ex) {
				logger.log(Level.SEVERE, "Unable to get build timestamp", ex);
				_daemonTimeStamp = "exception";
			}
			
		}
		return _daemonTimeStamp;
}



	

	
	public static JSONObject getConfigurationObject(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationObject(property);
	}	
	
	public static String getConfigurationProperty(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		try {
			return conf.getConfigurationProperty(property,null);
		} catch(Exception e) {
			logger.severe("Unable to get config property.  Domain = " + domain 
					               + ", Property = " + property.getFullLabel());
			throw e;		      
		}
	}	
	
	public static String getConfigurationProperty(String domain, FileStorageAreaType area, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationProperty(property,area);
	}	
	
	public static int getConfigurationPropertyAsInt(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationPropertyAsInt(property);
	}	
	
	public static long getConfigurationPropertyAsLong(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationPropertyAsLong(property);
	}	
	
	public static double getConfigurationPropertyAsDouble(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationPropertyAsDouble(property);
	}	
	
	public static boolean getConfigurationPropertyAsBoolean(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationPropertyAsBoolean(property);
	}	
	
	
	public static java.util.Properties getConfigurationPropertyAsProperties(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationPropertyAsProperties(property);
	}	
	
	public static JSONArray getConfigurationPropertyAsArray(String domain, ConfigurationTypeInterface property) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.getConfigurationPropertyAsArray(property);	}		
	
	public static boolean storeJSONInElasticSearch(String domain, FileStorageAreaType area) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.storeJSONInElasticSearch(area);
	}	
	
	public static boolean storeJSONInHDFS(String domain, FileStorageAreaType area) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.storeJSONInHDFS(area);
	}		
	
	public static boolean sendJSONToKafka(String domain, FileStorageAreaType area) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.sendJSONToKafka(area);
	}		
	
	
	
	
	public static boolean storeRawInFile(String domain, FileStorageAreaType area) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.storeRawInFile(area);
	}		
	
	
	public static boolean storeRawInAccumulo(String domain, FileStorageAreaType area) {
		Configuration conf = Configuration.getDomainConfiguration(domain);
		return conf.storeRawInAccumulo(area);
	}			
	
	/**
	 * this provides the KAFKA queue name, the Elasticsearch index name, and the accumulo table name
	 * 
	 * @param domain
	 * @param fsat
	 * @return
	 */
	public static String getDomainAndArea(String domain,FileStorageAreaType fsat) {
		return domain + "_" + fsat.getLabel();
	}
	
	public Configuration(String domain, JSONObject properties) {
		_domain = domain;
		_properties = properties;
		
		_configurations.put(domain, this);
	}
	
	/**
	 * Used by the static method to check encryption properties...
	 * Not to be used externally....
	 * 
	 * @param properties
	 */
	private Configuration(JSONObject properties) {
		_properties = properties;
	}

	/**
	 * From the current configuration, create a new configuration by overriding any values set in 
	 * the override properties.
	 * 
	 * This will be useful to use the "system" configuration as the starting point and then apply
	 * domain-specific configuration to the changes.
	 * 
	 * @param overrideProperties
	 * @return
	 */
	public Configuration overrideSetProperties(String domain,JSONObject overrideProperties) {
		JSONObject result = new JSONObject(_properties.toString());  //force a deep copy 
		
		Configuration.override(result, overrideProperties);
		
		return new Configuration(domain, result);
	}
	
	private static void override(JSONObject base, JSONObject override) {
		
		java.util.Set<String> examinedProperties = new java.util.HashSet<String>();
		
		for (String baseKey: base.keySet()) {
			examinedProperties.add(baseKey);
			if (override.has(baseKey) == false) { continue; }
			
			JSONType baseType = JSONUtilities.getJSONType(base.get(baseKey));
			JSONType overType = JSONUtilities.getJSONType(override.get(baseKey));
			
			if (baseType != overType) {
				base.put(baseKey, override.get(baseKey)); 
				continue;
			}
			if (baseType == JSONType.JSON_OBJECT) {
				override(base.getJSONObject(baseKey),override.getJSONObject(baseKey));
				continue;
			}
			if (baseType == JSONType.JSON_ARRAY) {
				//TODO: Figure this shit out ....
			}
			
			if (base.get(baseKey).equals(override.get(baseKey)) == false) {  // this should work for the other types and unknowns
				base.put(baseKey, override.get(baseKey));
			}
		}

		for (String overrideKey: override.keySet()) {
			if (examinedProperties.contains(overrideKey)) { continue; } // we've already checked this key once, no need to repeat
			
			// since we are here, the base didn't have this key, so add it.
			base.put(overrideKey, override.get(overrideKey));
		}
	}
	
	public JSONObject getConfiguration() { return _properties;	}
	
	
	public JSONObject getConfigurationObject(ConfigurationTypeInterface property){
		if (property == null) { return _properties; }
		else {
			JSONObject parentConf = this.getConfigurationObject(property.getParentConfiguration());
			if (parentConf == null || !parentConf.has(property.toString())) {
				return null;
			}
			
			return parentConf.getJSONObject(property.toString());
		}
	}
	
	private static final Pattern replacementPattern = Pattern.compile("\\{.*?\\}");
	
	public String getConfigurationProperty(ConfigurationTypeInterface property, FileStorageAreaType area) {
		if (property.isDerived()) {
			String result = property.getDerivationPattern();
			
			Matcher m = replacementPattern.matcher(result);
			while (m.find()) {
				String value = result.substring(m.start()+1, m.end()-1);
				String replacement = "";
				if (value.equals("domain")) {
					replacement = _domain;
				}
				else if (value.equals("area")) {
					if (area != null) { replacement = area.getLabel(); }
					else { replacement = "AREANOTPRESENT"; }
				}
				else {
					// Note: This assumes that the ConfigurationTypeInterface property is an enum in ConfigurationType.
					// Other instances of ConfigurationTypeInterfaces should not allows derivable values
					replacement = this.getConfigurationProperty(ConfigurationType.valueOf(value),area); 
				}
				
				result = result.substring(0,m.start()) + replacement + result.substring(m.end());
				m = replacementPattern.matcher(result);
			}
			return result;
		}
		else {
			return this.getConfigurationObject(property.getParentConfiguration()).optString(property.toString());	
		}
	}
	
	public boolean hasConfigurationProperty(ConfigurationTypeInterface property) {
		JSONObject parentConf = this.getConfigurationObject(property.getParentConfiguration());
		return parentConf != null && parentConf.has(property.toString());	
	}

	/**
	 * 
	 * @param property
	 * @return 0 if it doesn't exist
	 */
	public long getConfigurationPropertyAsLong(ConfigurationTypeInterface property) {
		JSONObject configurationObject = this.getConfigurationObject(property.getParentConfiguration());
		if (configurationObject == null) {
			return 0L;
		}
		return configurationObject.optLong(property.toString());
	}
	
	/**
	 * 
	 * @param property
	 * @return 0 if it doesn't exist
	 */
	public int getConfigurationPropertyAsInt(ConfigurationTypeInterface property) {
		JSONObject configurationObject = this.getConfigurationObject(property.getParentConfiguration());
		if (configurationObject == null) {
			return 0;
		}
		return configurationObject.optInt(property.toString());
	}
		

	/**
	 * 
	 * @param property
	 * @return 0 if it doesn't exist
	 */
	public double getConfigurationPropertyAsDouble(ConfigurationTypeInterface property) {
		JSONObject configurationObject = this.getConfigurationObject(property.getParentConfiguration());
		if (configurationObject == null) {
			return 0.0;
		}
		return configurationObject.optDouble(property.toString());
	}
	
	/**
	 * Get a boolean value from the property system with conversion to false for
	 * cases where the property does not exist.
	 * 
	 * @param property
	 * @return false if it doesn't exist
	 */
	public boolean getConfigurationPropertyAsBoolean(ConfigurationTypeInterface property) {
		JSONObject configurationObject = this.getConfigurationObject(property.getParentConfiguration());
		if (configurationObject == null) {
			return false;
		}
		return configurationObject.optBoolean(property.toString());
	}

	/**
	 * 
	 * @param property
	 * @return 
	 */
	public JSONArray getConfigurationPropertyAsArray(ConfigurationTypeInterface property) {
		return this.getConfigurationObject(property.getParentConfiguration()).getJSONArray(property.toString());	
	}
	
	/**
	 * 
	 * @param property
	 * @return 
	 */
	public java.util.Properties getConfigurationPropertyAsProperties(ConfigurationTypeInterface property) {
		JSONObject jo = this.getConfigurationObject(property);	
		java.util.Properties prop = new java.util.Properties();
		for (String key: jo.keySet()) {
			prop.setProperty(key, jo.getString(key));
		}
		return prop;
	}

	
	public boolean storeJSONInElasticSearch(FileStorageAreaType area) {
		switch(area) {
		case REGULAR: return this.getConfigurationPropertyAsBoolean(ConfigurationType.ELASTIC_STOREJSON_NORMAL);
		case ARCHIVE: return this.getConfigurationPropertyAsBoolean(ConfigurationType.ELASTIC_STOREJSON_ARCHIVE);
		case SANDBOX: return this.getConfigurationPropertyAsBoolean(ConfigurationType.ELASTIC_STOREJSON_SANDBOX);
		default: logger.log(Level.SEVERE, "Invalid area passed to sendToElasticSearch: "+area.getLabel());
		         return false;
		}
	}
	
	public boolean storeJSONInHDFS(FileStorageAreaType area) {
		switch(area) {
		case REGULAR: return this.getConfigurationPropertyAsBoolean(ConfigurationType.HDFS_STOREJSON_NORMAL);
		case ARCHIVE: return this.getConfigurationPropertyAsBoolean(ConfigurationType.HDFS_STOREJSON_ARCHIVE);
		case SANDBOX: return this.getConfigurationPropertyAsBoolean(ConfigurationType.HDFS_STOREJSON_SANDBOX);
		default: logger.log(Level.SEVERE, "Invalid area passed to sendToHDFS: "+area.getLabel());
		         return false;
		}
	}
	
	public boolean sendJSONToKafka(FileStorageAreaType area) {
		switch(area) {
		case REGULAR: return this.getConfigurationPropertyAsBoolean(ConfigurationType.KAFKA_SEND_NORMAL);
		case ARCHIVE: return this.getConfigurationPropertyAsBoolean(ConfigurationType.KAFKA_SEND_ARCHIVE);
		case SANDBOX: return this.getConfigurationPropertyAsBoolean(ConfigurationType.KAFKA_SEND_SANDBOX);
		default: logger.log(Level.SEVERE, "Invalid area passed to sendToKafka: "+area.getLabel());
		         return false;
		}
	}
	
	public boolean storeRawInFile(FileStorageAreaType area) {
		switch(area) {
		case REGULAR: return this.getConfigurationPropertyAsBoolean(ConfigurationType.FILE_STORERAW_NORMAL);
		case ARCHIVE: return this.getConfigurationPropertyAsBoolean(ConfigurationType.FILE_STORERAW_ARCHIVE);
		case SANDBOX: return this.getConfigurationPropertyAsBoolean(ConfigurationType.FILE_STORERAW_SANDBOX);
		default: logger.log(Level.SEVERE, "Invalid area passed to storeRawInFile: "+area.getLabel());
		         return false;
		}
	}	

	public boolean storeRawInAccumulo(FileStorageAreaType area) {
		switch(area) {
		case REGULAR: return this.getConfigurationPropertyAsBoolean(ConfigurationType.ACCUMULO_STORERAW_NORMAL);
		case ARCHIVE: return this.getConfigurationPropertyAsBoolean(ConfigurationType.ACCUMULO_STORERAW_ARCHIVE);
		case SANDBOX: return this.getConfigurationPropertyAsBoolean(ConfigurationType.ACCUMULO_STORERAW_SANDBOX);
		default: logger.log(Level.SEVERE, "Invalid area passed to storeRawInAccumulo: "+area.getLabel());
		         return false;
		}
	}
	
	public java.util.List<ConfigurationType> findMissingConfigurationElements(ConfigurationLocation configLocale) {
		java.util.ArrayList<ConfigurationType> results = new java.util.ArrayList<ConfigurationType>();
		
		for (ConfigurationType ct: ConfigurationType.values()) {
			if (ct.isRequired() == false || ct.getLocation() == null || configLocale.equals(ct.getLocation()) == false) {continue;}
			
			if (this.hasConfigurationProperty(ct) == false) {
				results.add(ct);
			}
		}
		
		return results;
	}
	
	/**
	 * Looks through the configuration to check if the fields should be encrypted.
	 * If a field should be encrypted, then its value is check to see whether or not it starts with "{AES}"
	 * If it doesn't currently start with "{AES}, the value is encrypted and placed back into the json object with "{AES}encryptedValue"
	 *   
	 * @param parameters
	 * @param configuration
	 */
	public boolean checkConfigurationForEncryptedFields() {
		boolean changesMade = false;
		for (ConfigurationType ct: ConfigurationType.values()) {
			if (ct.isEncrypted() == false) { continue; }
			
			JSONObject immediateParent = getConfigurationObject(ct.getParentConfiguration());
			if (immediateParent == null) { continue; } // object isn't present.
						
			String fieldValueThatBeEncrypted = immediateParent.optString(ct.getLabel(),null);
			if (fieldValueThatBeEncrypted == null) { continue; } // object isn't present.

			if (fieldValueThatBeEncrypted.startsWith("{AES}") == false) {
				String encryptedValue = Collector.getTheCollecter().encryptValueWithFileKey(fieldValueThatBeEncrypted);
				immediateParent.put(ct.getLabel(), encryptedValue);
				changesMade = true;
			}

		}
		return changesMade;
	}		

	
	/**
	 * Looks through the configuration to check if the fields should be encrypted.
	 * If a field should be encrypted, then its value is check to see whether or not it starts with "{AES}"
	 * If it doesn't currently start with "{AES}, the value is encrypted and placed back into the json object with "{AES}encryptedValue"
	 *   
	 * @param parameters
	 * @param configuration
	 */
	public static boolean checkConfigurationForEncryptedFields(JSONObject configurationData) {
		Configuration c  = new Configuration(configurationData);
		return c.checkConfigurationForEncryptedFields();
	}		
	
}
