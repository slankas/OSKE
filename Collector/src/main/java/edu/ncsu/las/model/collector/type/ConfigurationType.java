package edu.ncsu.las.model.collector.type;

/**
 * Represents the different configuration parameters in the application_properties / local_properties files
 *  
 */
public enum ConfigurationType implements ConfigurationTypeInterface {
	COLLECTOR("collector",true,null,"",ConfigurationLocation.FILE,false),
	COLLECTOR_ID("id",true,COLLECTOR,"",ConfigurationLocation.FILE,false),
	COLLECTOR_JOB_POOLSIZE("jobPoolSize",true,COLLECTOR,"number of concurrent threads that process jobs (collectors) on a particular machine.",ConfigurationLocation.FILE,false),
	COLLECTOR_TASK_POOLSIZE("taskPoolSize",true,COLLECTOR,"number of threads that are used for misc. tasks such as sending emails or exporting search results.",ConfigurationLocation.FILE,false),
	COLLECTOR_SEARCH_POOLSIZE("searchPoolSize",true,COLLECTOR,"number of threads that are used to crawl web pages for the search sandbox / domain discovery.",ConfigurationLocation.FILE,false),
	COLLECTOR_IDLEKILLTIMESEC("idleKillTimeSec",true,COLLECTOR,"",ConfigurationLocation.FILE,false),
	COLLECTOR_SLEEPTIMESEC("sleepTimeSec",true,COLLECTOR,"",ConfigurationLocation.FILE,false),
	COLLECTOR_WORKINGDIRECTORY("workingDirectory",true,COLLECTOR,"",ConfigurationLocation.FILE,false),
	COLLECTOR_ALLOW_SERVICES("allowServices", true, COLLECTOR,"For an instance of the collector, should services (e.g., DirectoryWatcher) be executed?",ConfigurationLocation.FILE,false),
	COLLECTOR_REQUIRE_USERAGREEMENT("requireUserAgreement", true, COLLECTOR,"Does the collector require a user agreement for human-based users?",ConfigurationLocation.FILE,false),

	CONCEPT_CACHESEC("conceptCacheSec",true,null,"length (in seconds) concepts are cached per domain.",ConfigurationLocation.SYSTEM,false),
	CONCEPT_PARSE("conceptParse",true,null,"Should concepts be parse by 'sentence' or by 'document'.  Default is 'document'.",ConfigurationLocation.SYSTEM,true,true, false, SourceParameterType.STRING, false),
	CONCEPT_TIMEOUTSEC("conceptTimeoutSec",true,null,"how long can a document attempt to parse concepts in seconds.  Default is 600 seconds.",ConfigurationLocation.SYSTEM,true,true, false, SourceParameterType.INT, false),
	
	DOMAIN_PURGE("allowOnlineDomainPurge",false,null,"Can the current domain be purged(emptied) interactively?",ConfigurationLocation.SYSTEM,true),
	DOMAIN_DUPLICATE_TEXT("allowDuplicateText",false,null,"can multiple records with the same text be stored into ElasticSearch? true/false",ConfigurationLocation.SYSTEM,true),
	DOMAIN_COLLECTION_ACCESS_REQUIRED("collectionAccessRequired",false,null,"Do users need to be assigned to collections to access them? true/false",ConfigurationLocation.SYSTEM,true,false, false, SourceParameterType.BOOLEAN, false),
	
	
	BUILD_DAEMON_TIMESTAMP("daemonBuild",true,null,"Timestamp or version number of the daemon process",ConfigurationLocation.MANIFEST,false),
	BUILD_WEB_TIMESTAMP("webBuild",true,null,"Timestamp or version number of the web application.",ConfigurationLocation.MANIFEST,false),

	
	DATABASE("database",true, null,"",ConfigurationLocation.FILE,false),
	
	DATABASE_DATASOURCENAME("datasourcename",true, DATABASE,"",ConfigurationLocation.FILE,false),
	DATABASE_DRIVER("driver",true, DATABASE,"",ConfigurationLocation.FILE,false),
	DATABASE_URL("url",true, DATABASE,"",ConfigurationLocation.FILE,false),
	DATABASE_USER("user",true, DATABASE,"",ConfigurationLocation.FILE,false),
	DATABASE_PASSWORD("password",true, DATABASE,"",ConfigurationLocation.FILE,false, false, true, SourceParameterType.STRING, false),
	DATABASE_MAXCONNECTIONS("maxconnections",true, DATABASE,"",ConfigurationLocation.FILE,false),
	
	EMAIL("email",true,null,"",ConfigurationLocation.SYSTEM,true),
	EMAIL_SERVER("server",true,EMAIL,"what host does outgoing email",ConfigurationLocation.SYSTEM,true),
	EMAIL_PORT("port",true, EMAIL, "what port does the smtp listen on?",ConfigurationLocation.SYSTEM,true),
	EMAIL_USER("user",true, EMAIL, "what user authenticates to the SMTP server?",ConfigurationLocation.SYSTEM,true),
	EMAIL_PASSWORD("password", true, EMAIL, "password to authenticate to the smtp server",ConfigurationLocation.SYSTEM,true,false,true,SourceParameterType.STRING,false),	

	ACCESS_AUTHENTICATED_SITES("accessAuthenticatedSites",true, null,"Should jobs be allowed to use authentication.  If set to false, authentication configuration will be block.",ConfigurationLocation.SYSTEM,true, true, true, SourceParameterType.BOOLEAN, false),
	BLOCKED_TLDS("blockedTLDs",true, null,"if any top-level domains are listed, then those domains will be blocked from being crawled as well as from domain discovery searches.",ConfigurationLocation.SYSTEM,true,false,false, SourceParameterType.STRING, true),
	

	ELASTIC("elastic",true,null,"Contains the \"base\" information for ElasticSearch",ConfigurationLocation.SYSTEM,true),
	ELASTIC_REST("restEndPoint",true,ELASTIC,"what is the starting point for the rest interface. should include protocol(http) and port (9200)",ConfigurationLocation.SYSTEM,true),
	ELASTIC_REST_INTERNAL("restEndPointInternal",true,ELASTIC,"what is the starting point for the rest interface. should include protocol(http) and port (9200).  Used for server to server communication within a docker-compose/swarm configruation.",ConfigurationLocation.SYSTEM,true),
	
	ELASTIC_DEFAULT("default",true,ELASTIC,"Defaults for the index settings and mappings when creating new indexs",ConfigurationLocation.SYSTEM,true),
	ELASTIC_DEFAULT_MAPPINGS("mappings",true,ELASTIC_DEFAULT,"what mapping should be put in place when creating a new index",ConfigurationLocation.SYSTEM,true),
	ELASTIC_DEFAULT_SETTINGS("settings",true,ELASTIC_DEFAULT,"what settings should be used when creating a new index",ConfigurationLocation.SYSTEM,true),
	
	
	ELASTIC_TYPE_COLLECTION("elasticTypeCollection","collection","what type stores collections in ElasticSearch. does not change"),	
	
	ELASTIC_STOREJSON("storeJSON",true,ELASTIC, "Should the system store JSON content in ElasticSearch?",ConfigurationLocation.SYSTEM,true),
	ELASTIC_STOREJSON_NORMAL("normal",true,ELASTIC_STOREJSON,"", ConfigurationLocation.SYSTEM,true),
	ELASTIC_STOREJSON_SANDBOX("sandbox",true,ELASTIC_STOREJSON,"", ConfigurationLocation.SYSTEM,true),
	ELASTIC_STOREJSON_ARCHIVE("archive",true,ELASTIC_STOREJSON,"", ConfigurationLocation.SYSTEM,true),	
	ELASTIC_STOREJSON_LOCATION("elasticStoreJSONLocation","{ELASTIC_REST}{domain}_{area}/","creates a full url to the appropriate elasticsearch index"),	
	ELASTIC_STOREJSON_LOCATION_INTERNAL("elasticStoreJSONLocation","{ELASTIC_REST_INTERNAL}{domain}_{area}/","creates a full url to the appropriate elasticsearch index"),	
	
	FILE_ROOT("fileRoot",true,null,"Contains the \"base\" information for the local filesystem",ConfigurationLocation.FILE,true),
	
	KAFKA("kafka",true,null, "",ConfigurationLocation.SYSTEM,true),
	KAFKA_PROPERTIES("properties",true,KAFKA,"",ConfigurationLocation.SYSTEM,true),
	KAFKA_REPLICATION_FACTOR("replicationFactor",true,KAFKA,"",ConfigurationLocation.SYSTEM,true),
	KAFKA_NUM_PARTITIONS("numPartitions",true,KAFKA,"",ConfigurationLocation.SYSTEM,true),
	KAFKA_ZOOKEEPER("zookeeper",true,KAFKA,"",ConfigurationLocation.SYSTEM,true),
	KAFKA_SEND("send",true,KAFKA, "",ConfigurationLocation.SYSTEM,true),
	KAFKA_SEND_NORMAL("normal",true,KAFKA_SEND,"", ConfigurationLocation.SYSTEM,true),
	KAFKA_SEND_SANDBOX("sandbox",true,KAFKA_SEND,"", ConfigurationLocation.SYSTEM,true),
	KAFKA_SEND_ARCHIVE("archive",true,KAFKA_SEND,"", ConfigurationLocation.SYSTEM,true),
	
	HDFS("hdfs", true,null,"",ConfigurationLocation.SYSTEM,true),
	HDFS_USER("hdfsUser", true, HDFS,"",ConfigurationLocation.SYSTEM,true),
	HDFS_BASE_URI("hdfsBaseURI", true, HDFS,"",ConfigurationLocation.SYSTEM,true),
	HDFS_STOREJSON("storeJSON",true, HDFS, "Should the system store JSON content in HDFS?",ConfigurationLocation.SYSTEM,true),
	HDFS_STOREJSON_NORMAL("normal",true,HDFS_STOREJSON,"", ConfigurationLocation.SYSTEM,true),
	HDFS_STOREJSON_SANDBOX("sandbox",true,HDFS_STOREJSON,"", ConfigurationLocation.SYSTEM,true),
	HDFS_STOREJSON_ARCHIVE("archive",true,HDFS_STOREJSON,"", ConfigurationLocation.SYSTEM,true),	
	HDFS_STOREJSON_LOCATION("hdfsStoreJSONLocation","{HDFS_BASE_URI}/openke/{domain}/json/{area}/","where does the system place JSON files in HDFS?"),
	
	FILE("file",true,null, "",ConfigurationLocation.SYSTEM,true),
	FILE_STORERAW("storeRaw",true,FILE, "",ConfigurationLocation.SYSTEM,true),
	FILE_STORERAW_NORMAL("normal",true,FILE_STORERAW,"", ConfigurationLocation.SYSTEM,true),
	FILE_STORERAW_SANDBOX("sandbox",true,FILE_STORERAW,"", ConfigurationLocation.SYSTEM,true),
	FILE_STORERAW_ARCHIVE("archive",true,FILE_STORERAW,"", ConfigurationLocation.SYSTEM,true),
	FILE_STORE("fileStore","{FILE_ROOT}/{domain}/files_{area}/","where does the system place raw files?"),
	
	
	ACCUMULO("accumulo",true,null, "",ConfigurationLocation.SYSTEM,true),
	ACCUMULO_STORERAW("storeRaw",true,ACCUMULO, "Should the system store raw content in Accumulo?",ConfigurationLocation.SYSTEM,true),
	ACCUMULO_STORERAW_NORMAL("normal",true,ACCUMULO_STORERAW,"", ConfigurationLocation.SYSTEM,true),
	ACCUMULO_STORERAW_SANDBOX("sandbox",true,ACCUMULO_STORERAW,"", ConfigurationLocation.SYSTEM,true),
	ACCUMULO_STORERAW_ARCHIVE("archive",true,ACCUMULO_STORERAW,"", ConfigurationLocation.SYSTEM,true),	
	
	ACCUMULO_INSTANCE_NAME("instanceName",true,ACCUMULO, "",ConfigurationLocation.SYSTEM,false),
	ACCUMULO_ZOOKEEPERS("zooKeepers",true,ACCUMULO, "",ConfigurationLocation.SYSTEM,false),
	ACCUMULO_USERNAME("userName",true,ACCUMULO, "",ConfigurationLocation.SYSTEM,false),
	ACCUMULO_PASSWORD("password",true,ACCUMULO, "",ConfigurationLocation.SYSTEM,false,false,true,SourceParameterType.STRING,false),
	
	//NOTE: based upon the settings, the ElasticSearch and Accumulo instances host all domains...
	
	
	ANNOTATIONS("annotations",true, null,"array of annotations that should be used for additional fields in the JSON document",ConfigurationLocation.SYSTEM,true,true,false, SourceParameterType.STRING,true),
	SOURCE_HANDLERS("sourceHandlers",true, null,"What source handlers may be used in this domain?",ConfigurationLocation.SYSTEM,true,true,false, SourceParameterType.STRING,true),
	
	USER_AGENT("userAgent",true, null,"array of strings to be randomly used by web crawlers",ConfigurationLocation.SYSTEM,true,true,false, SourceParameterType.STRING,true),
	
	/*
	FULLTEXT("fulltext",true,null,"used to connect to a full-text search engine (ie, elasticsearch)"),
	FULLTEXT_RESTENDPOINT("restEndPoint",true,FULLTEXT,"specifies the endpoint the appropriate ElasticSearch Index"),
	*/
	
	EXPORT("export",true,null,"",ConfigurationLocation.SYSTEM,true),
	EXPORT_TIME_TO_LIVE_HOURS("timeToLiveHours", true, EXPORT, "specifies how long export files should be kept prior to deletion",ConfigurationLocation.SYSTEM,true),
	EXPORT_MAX_RECORD_COUNT("maxRecordCount", true, EXPORT, "maximum number of records that can be exported",ConfigurationLocation.SYSTEM,true),
	EXPORT_DOWNLOAD_PATH("exportDownload","{FILE_ROOT}/download/","where does the system place exported files?"),
	EXPORT_EXTERNAL_SYSTEM_PATH("exportExternalSystem","{FILE_ROOT}/externalExport/","where does the system place exported files meant for other systems (e.g., IBM, SAS, etc.) ?"),
	
	EXPORT_VOYANT("voyant",true,EXPORT, "Configuration for sending results to voyant.",ConfigurationLocation.SYSTEM,true),
	EXPORT_VOYANT_POST_URL("postURL", true, EXPORT_VOYANT, "what is the url to send documents to voyant",ConfigurationLocation.SYSTEM,true),
	EXPORT_VOYANT_ACCESS_URL("accessURL", true, EXPORT_VOYANT, "what is the url to access voyant.  The corpus ID from an upload will be appended to this and sent to users.",ConfigurationLocation.SYSTEM,true),
	EXPORT_VOYANT_MAX_RECORD_COUNT("maxRecordCount", true, EXPORT_VOYANT, "maximum number of records that can be exported to voyant",ConfigurationLocation.SYSTEM,true),
	

	
	IMPORT("import",true,null,"",ConfigurationLocation.SYSTEM,true),
	IMPORT_SLEEPTIMESEC("sleepTimeSec",true, IMPORT,"", ConfigurationLocation.SYSTEM,true),
	//These are values derived automatically from other configuration values
	IMPORT_DIRECTORY("importDirectory","{FILE_ROOT}/{domain}/import/","where does the system watch for new files to import?"),
		
	WEBCRAWLER("webCrawler",true,null,"",ConfigurationLocation.SYSTEM,true),
	//WEBCRAWLER_USERAGENTSTRING("userAgentString",true,WEBCRAWLER,"",ConfigurationLocation.SYSTEM,true),  // the agent is defined in its own top-level listing now. 
	WEBCRAWLER_MAX_CRAWL_DEPTH("maxDepthOfCrawling",true,WEBCRAWLER,"",ConfigurationLocation.SYSTEM,true),
	WEBCRAWLER_MAX_FETCH_PAGES("maxPagesToFetch",true,WEBCRAWLER,"",ConfigurationLocation.SYSTEM,true),
	WEBCRAWLER_INCLUDE_BINARY_CONTENT("includeBinaryContentInCrawling",true,WEBCRAWLER,"",ConfigurationLocation.SYSTEM,true),
	WEBCRAWLER_MAX_DOWNLOAD_SIZE("maxDownloadSize",true,WEBCRAWLER,"",ConfigurationLocation.SYSTEM,true),
	
	FAROO("faroo",true,null,"",ConfigurationLocation.SYSTEM,true),
	FAROO_KEY("key",true,FAROO,"What is the API token (key) used by Faroo?",ConfigurationLocation.SYSTEM,true,false,true,SourceParameterType.STRING,false),
	
	AZURE("azure",true,null,"",ConfigurationLocation.SYSTEM,true),
	AZURE_ACADEMIC_KEY("academicAPI_key",true, AZURE,"key for microsoft academic API",ConfigurationLocation.SYSTEM,true,false,true,SourceParameterType.STRING,false),	
	
	AWS("aws",true,null,"",ConfigurationLocation.SYSTEM,true),
	AWS_KEY("key", true, AWS, "Amazon Web Services Translate key", ConfigurationLocation.SYSTEM, true, false, true, SourceParameterType.STRING, false),
	AWS_SECRETKEY("secretKey", true, AWS, "Amazon Web Services Translate secret key", ConfigurationLocation.SYSTEM, true, false, true, SourceParameterType.STRING, false),
	AWS_REGION("region", true, AWS, "Amazon Web Services region", ConfigurationLocation.SYSTEM, true, false, false, SourceParameterType.STRING, false),
	AWS_TRANSLATE_MAX_TPS("translateMaxTPS", true, AWS, "Amazon Web Services region", ConfigurationLocation.SYSTEM, true, false, false, SourceParameterType.INT, false),
	
	WORDNET("wordnetAPI",true, null,"docker image REST interface to WordNet search",ConfigurationLocation.SYSTEM,true),
	WORDNET_GENERALIZED("generalizedUrl", true, WORDNET, "hypernym", ConfigurationLocation.SYSTEM, true, false, true, SourceParameterType.STRING, false),
	WORDNET_SPECIALIZED("specializedUrl", true, WORDNET, "hyponym", ConfigurationLocation.SYSTEM, true, false, true, SourceParameterType.STRING, false),
	
	NLEXPANSION("nlExpansion",true, null,"docker image Flask REST interface to nlExpansion",ConfigurationLocation.SYSTEM,true),
	NLEXPANSION_SYNTACTIC("syntacticUrl", true, NLEXPANSION, "syntactic", ConfigurationLocation.SYSTEM, true, false, true, SourceParameterType.STRING, false),
	NLEXPANSION_SEMANTIC("semanticUrl", true, NLEXPANSION, "semantic", ConfigurationLocation.SYSTEM, true, false, true, SourceParameterType.STRING, false),

	NLDEFINITIONEXPANSION_API("nlDefExpansionAPI",true, null, "api end point for word expansion based upon definitions", ConfigurationLocation.SYSTEM,true),

	
	GRAPH_API("graphAPI",true, null,"what is the location at the graph collector API.  Should include web app name (context)",ConfigurationLocation.SYSTEM,true),
	TEXTRANK_API("textrankAPI",true, null,"what is the location at the textrank-based APIs",ConfigurationLocation.SYSTEM,true),
	TOPICMODEL_API("topicModelAPI",true, null, "Where is the location of the topic-modeling APIs", ConfigurationLocation.SYSTEM,true),
	GEOCODE_API("geoCodeAPI ",true, null, "Where is the location of the geo coding APIs? API must follow the same format as that at https://nominatim.openstreetmap.org/", ConfigurationLocation.SYSTEM,true),
	GEOTAG_API("geoTagAPI",true, null, "Where is the location of the geo tagging APIs?", ConfigurationLocation.SYSTEM,true),
	NLP_API("nlpAPI",true, null, "Where is the location of the natural language processing APIs?", ConfigurationLocation.SYSTEM,true),
	WHOIS_API("whoisAPI",true, null, "calls the whois service to get basic information about a domain", ConfigurationLocation.SYSTEM,true),
	MICROFORMAT_API("microformatAPI",true, null, "API that extracts microformat data from HTML content", ConfigurationLocation.SYSTEM,true),
	DBPEDIA_SPOTLIGHT_API("dbpediaSpotlightAPI",true, null, "API that finds indentified resources for DBPedia within text.  See https://www.dbpedia-spotlight.org/", ConfigurationLocation.SYSTEM,true),
	SPACY_API("spacyAPI",true, null, "REST-based service for getting named entities (and possibly other items) from spaCy. See https://spacy.io/usage/linguistic-features", ConfigurationLocation.SYSTEM,true),
	TEMPORAL_API("temporalAPI",true, null, "REST-based service for getting time annotations from a text document.  Uses HeidelTime", ConfigurationLocation.SYSTEM,true),

	PASTEBIN_SEARCH_URL("pastebinSearchURL",true, null, "URL to use for the pastebin search.  Value to search for should be 'REPLACEME'.  Can determine the URL by accessing existing custom Google search engine sites at either https://inteltechniques.com/osint/menu.pastebins.html or https://netbootcamp.org/pastesearch.html.  From one of those search pages, open up the browser's developer console, then search for 'REPLACEME'.  Within the network traffic look for a URL to v1ement.  Copy that link address and use as the value for this parameter.", ConfigurationLocation.SYSTEM,true),
	
	SECRET_PHRASE("secretPhrase",true, null, "A text based phrase that will be combined with an internal phrase to encrypt passwords.  This value is stored in plaintext as it forms the key.",ConfigurationLocation.FILE,true),
	
	APPLICATION_LOGO("applicationLogo",true,null, "What logo should be used for the application ?  Standard Value: resources/images/LAS_Logo.png",ConfigurationLocation.SYSTEM,true),
		
	INSTRUMENTATION("instrumentation",true, null,"",ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_PROJECTID("projectID",true, INSTRUMENTATION,"Project ID used to identify the collector system/application.  Typically: LAS/Collector",ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_API("api",true, INSTRUMENTATION,"REST API end point for adding new events to the instrumentation system",ConfigurationLocation.SYSTEM,true),	
	INSTRUMENTATION_TOKEN("token",true, INSTRUMENTATION,"What is the token to identify the collector app to the instrumentation service",ConfigurationLocation.SYSTEM,true,false,true,SourceParameterType.STRING,false),
	INSTRUMENTATION_SEND_EVENTS("sendEvents",true,INSTRUMENTATION,"Should the application send events to instrumentation.  true/false", ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_USE_ELASTICSEARCH("useElastic",true,INSTRUMENTATION,"Is the REST API under \"api\" serviced by ElasticSearch?  If so, then the application will check existance and mappings.  true/false", ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_USE_SKYLR("useSkylr",true,INSTRUMENTATION,"Should events be sent to the skylr instrumentation service.  true/false", ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_ELASTIC("elastic",true,INSTRUMENTATION,"JSON Object with index name, default settings and default mappings.  The server identfied under elastic configuration will be used.", ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_ELASTIC_INDEX("index",true,INSTRUMENTATION_ELASTIC,"index to use for instrumentation.", ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_ELASTIC_MAPPINGS("mappings",true,INSTRUMENTATION_ELASTIC,"mappings to be put in place when creating an instrumentation index",ConfigurationLocation.SYSTEM,true),
	INSTRUMENTATION_ELASTIC_SETTINGS("settings",true,INSTRUMENTATION_ELASTIC,"settings to be used when creating an instrumentation index",ConfigurationLocation.SYSTEM,true),
	
	KIBANA("kibana",true, null,"",ConfigurationLocation.SYSTEM,true),
	KIBANA_UTILIZE_DASHBOARD("utilizeDashboard",true, KIBANA,"Should the Kibana dashboard be shown or not? defined in the system domain configuration",ConfigurationLocation.SYSTEM,true,  true, false, SourceParameterType.BOOLEAN, false),
	KIBANA_HOME_DASHBOARD("homeDashboard",true, KIBANA,"Link to a Kibana visualization/dashboard used to show status on the home page.  Only needs to be defined at the system level.",ConfigurationLocation.SYSTEM,true),
	KIBANA_HOME_TITLE("homeTitle",true, KIBANA,"Title to display above a kibana dashboard. Defined in the system domain configuration",ConfigurationLocation.SYSTEM,true,  true, false, SourceParameterType.STRING, false),
	
	
	SYSTEMCOMPONENT("systemComponent",true, null,"JSON objects that specifies whether or not certain components of the system are used are not.",ConfigurationLocation.SYSTEM,true),
	SYSTEMCOMPONENT_ACCUMULO("accumulo",true, SYSTEMCOMPONENT,"Does this installation use Accumulo?",ConfigurationLocation.SYSTEM,true),
	SYSTEMCOMPONENT_HDFS("hdfs",true, SYSTEMCOMPONENT,"Does this installation use HDFS?",ConfigurationLocation.SYSTEM,true),	
	SYSTEMCOMPONENT_KAFKA("kafka",true, SYSTEMCOMPONENT,"Does this installation use Kafka?",ConfigurationLocation.SYSTEM,true),
	
	PUBMEDIMPORTER("pubMedImporter",true,null,"Configuration options for the PubMedImporter",ConfigurationLocation.FILE,false),
	PUBMEDIMPORTER_FTPSERVER("ftp_server",true, PUBMEDIMPORTER,"FTP server that hosts the pub med data",ConfigurationLocation.FILE,true),
	PUBMEDIMPORTER_BASELINE("baseline_dir",true, PUBMEDIMPORTER,"directory that contains the files that form the yearly basis of the pubmed data",ConfigurationLocation.FILE,true),
	PUBMEDIMPORTER_UPDATES("update_dir",true, PUBMEDIMPORTER,"directory of updates to the PUBMED database that are published regularly",ConfigurationLocation.FILE,true),
	PUBMEDIMPORTER_SLEEPTIME("sleepTimeSec",true, PUBMEDIMPORTER,"How much time to sleep in seconds in iterations of the pubmed processor?",ConfigurationLocation.FILE,true),
	PUBMEDIMPORTER_BASEDIRECTORY("baseDirectory",true, PUBMEDIMPORTER,"where should the pubmed working area be placed?",ConfigurationLocation.FILE,true),

	NEWS_FEED("newsFeed",true, null,"Allows for a 'breaking news' panel to be displayed on a domain's home page",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.BOOLEAN,false),
	NEWS_FEED_UTILIZE("utilize",true, NEWS_FEED,"Should this domain utilize the news feed or not?",ConfigurationLocation.SYSTEM,true),
	NEWS_FEED_TITLE("title",true, NEWS_FEED,"Label to use for the section on the domain's main / home page",ConfigurationLocation.SYSTEM,true,false,false, SourceParameterType.STRING, false),	
	NEWS_FEED_CACHE_TIME_MINUTES("cacheTimeMinutes",true, NEWS_FEED,"How many minutes should the results of the RSS feed be valide before a refresh?",ConfigurationLocation.SYSTEM,true,false,false, SourceParameterType.INT, false),	
	NEWS_FEED_URLS("urls",true, NEWS_FEED,"list of complete URLs that are RSS feeds",ConfigurationLocation.SYSTEM,true,false,false, SourceParameterType.STRING, true),
	NEWS_FEED_KEYWORDS("keywords",true, NEWS_FEED,"if this array of strings is non-empty, then one of the keywords must match part of the news feed item to be displayed. Keywords must be entered as lower-case - otherwise they will never match.",ConfigurationLocation.SYSTEM,true,false,false, SourceParameterType.STRING, true),
	
	
	JOB_ADJUDICATION("adjudicationQuestions",true, null,"Configuration for FIPP questions and answers for configured jobs within the domain",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	JOB_ADJUDIDCATION_UTILIZE("utilize",true, JOB_ADJUDICATION,"Should this domain utilize FIPP questions and answers while adjudicating jobs?",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.BOOLEAN,false),	
	JOB_ADJUDIDCATION_REQUIRE("require",true, JOB_ADJUDICATION,"Must the FIPP answers be completed for all questions?",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.BOOLEAN,false),	
	JOB_ADJUDIDCATION_TITLE("title",true, JOB_ADJUDICATION,"What title should be shown to the user while reviewing and answering the questions?",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.STRING,false),
	JOB_ADJUDIDCATION_OVERVIEWTEXT("overviewText",true, JOB_ADJUDICATION,"additional explanatory text for completing the section",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.STRING,false),
	JOB_ADJUDIDCATION_QUESTIONS("questions",true, JOB_ADJUDICATION,"The actual questions.  Each JSON object contains the following elements: category, subcategory, question ",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,true),
	
	
	VIDEO("video",true, null,"Configuration for support within the domain",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	VIDEO_UTILIZE("utilize",true, VIDEO,"Is video utilized within this domain?",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.BOOLEAN,false),
	VIDEO_URL_REGEXES("url_regex",true, VIDEO,"Array of regular expressions, which is a URL matches will be considered a link to the a video and passed to the VideoHandler for downloading.",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.REGEX,true),
	
	WEBAPP("webapp",true, null,"Parameters for the web application",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	WEBAPP_AUTH("authorization",true, WEBAPP,"parameters to configuration the web application authorization (Note: this should have been called authentication)",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	WEBAPP_AUTH_METHOD("method",true, WEBAPP_AUTH,"method to use to authenticate users.  Should be \"http_header\"(default) or \"oauth2\",\"singleuser\", or \"local\".",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),	
	WEBAPP_AUTH_HEADER("http_header",true, WEBAPP_AUTH,"array of http_headers to check for user IDs, which should be email addresses.",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, true),
	WEBAPP_AUTH_OAUTH_CLIENTID("oauth_clientid",true, WEBAPP_AUTH,"What is the Google OAUTH2 Client ID?",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	WEBAPP_AUTH_SIGNOUT_TEXT("signout_text",true, WEBAPP_AUTH,"text to place on the logout button",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	WEBAPP_AUTH_SIGNOUT_URL("signout_url",true, WEBAPP_AUTH,"destination for the user when leaving openke",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	WEBAPP_AUTH_LOCAL("local",true, WEBAPP_AUTH,"parameters used for local/internal authnetication",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	WEBAPP_AUTH_LOCAL_MIN_PASSWORD_LENGTH("minPasswordLength",true, WEBAPP_AUTH_LOCAL,"minimum length of a password",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.INT, false),
	WEBAPP_AUTH_LOCAL_MAX_PASSWORD_LENGTH("maxPasswordLength",true, WEBAPP_AUTH_LOCAL,"maximum length of a password",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.INT, false),
	WEBAPP_AUTH_LOCAL_REQUIRED_LOWERCASE("lowerCaseRequired",true, WEBAPP_AUTH_LOCAL,"are lower case letters required",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	WEBAPP_AUTH_LOCAL_REQUIRED_UPPERCASE("upperCaseRequired",true, WEBAPP_AUTH_LOCAL,"are upper case letters required",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	WEBAPP_AUTH_LOCAL_REQUIRED_DIGIT("digitRequired",true, WEBAPP_AUTH_LOCAL,"are number(0-9) required",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	WEBAPP_AUTH_LOCAL_REQUIRED_SPECIAL("specialCharRequired",true, WEBAPP_AUTH_LOCAL,"are other characters required.",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	WEBAPP_AUTH_LOCAL_MAX_PASSWORD_AGE("maxPasswordAgeDays",true, WEBAPP_AUTH_LOCAL,"How long are passwords good for until they must be changed?",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.INT, false),
	WEBAPP_AUTH_LOCAL_MAX_FAILURES("maxFailures",true, WEBAPP_AUTH_LOCAL,"How many failures can be made before locking accounts?",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.INT, false),

	WEBAPP_AUTH_LDAP("ldap",true, WEBAPP_AUTH,"parameters used for LDAP authnetication.  Users are expected to enter their email address.  Their user ID should just be the portion before the '@' symbol.",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	WEBAPP_AUTH_LDAP_DN_FORMAT("dnFormat",true, WEBAPP_AUTH_LDAP,"format used for distinquishing name.  The USERID will be replaced with the users ID they enter. For active directory, USERID@domainName.  For other LDAP servers, the dnFormat (implementation specific) will be similar to uid=USERID,ou=accounts,dc=ncsu,dc=edu",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	WEBAPP_AUTH_LDAP_EMAIL_DOMAIN("emailDomain",true, WEBAPP_AUTH_LDAP,"",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	WEBAPP_AUTH_LDAP_SERVER("server",true, WEBAPP_AUTH_LDAP,"LDAP URL used by JNDI.  Typically, this will be in the format of ldaps://hostname:636 or ldap://hostname:389",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	WEBAPP_AUTH_LDAP_ACTIVE_DIRECTORY_DOMAIN("activeDirectoryDomain",true, WEBAPP_AUTH_LDAP,"if this value is set, then attempt to discover the active directory domain controllers through DNS searches by prepending _ldap._tcp.dc._msdcs and then searching for SRV records. Discovered controllers must support LDAPS (SSL) on port 636.  See https://technet.microsoft.com/pt-pt/library/cc759550(v=ws.10).aspx for more details",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),

	
	LDAP("ldap",true, null,"Parameters for the LDAP - used to search for users.  ldap results must include mail and displayName fields.",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	LDAP_UTILIZE("utilize",true, LDAP,"Should person searches be allowed / used?",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	LDAP_URL("url",true, LDAP,"LDAP server to utilize.  Should be in the format of ldap://fullyQualifiedDomainName:port",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	LDAP_BASE_DN("baseDN",true, LDAP,"container to search for people entries.  Example: ou=people,dc=ncsu,dc=edu",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	LDAP_SEARCH_FIELDS("searchFields",true, LDAP,"array of field names to search for individuals.  Search fields must be indexed to support wildcarding, which is added automatically during a search. [\"displayName\",\"mail\"]",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, true),

	HEADER_LINKS("headerLinks",true, null,"Parameters for hyperlinks displayed in the page header",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	HEADER_LINKS_UTILIZE("utilize",true, HEADER_LINKS,"should the system display links in the header",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	HEADER_LINKS_OBJECTS("hyperlinks",true, HEADER_LINKS,"Array of json objects containing displayText and link properties",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),

	
	HYPERLINKS("hyperlinks",true,null,"Array of json objects containing displayText and link properties",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_ARRAY,false),
	
	RESOURCE("resourceSection",true, null,"Parameters for resource box on a domain dashboard",ConfigurationLocation.SYSTEM,true,false,false,SourceParameterType.JSON_OBJECT,false),
	RESOURCE_UTILIZE("utilize",true, RESOURCE,"should the system show the resource box on the dashboard page",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.BOOLEAN, false),
	RESOURCE_TITLE("title",true, RESOURCE,"should the system display links in the header",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),
	RESOURCE_OBJECTS("hyperlinks",true, RESOURCE,"Array of json objects containing displayText and link properties",ConfigurationLocation.SYSTEM,false,false,false, SourceParameterType.STRING, false),

	
	;
	
	
	
	private String _label;
	private boolean _required;
	private ConfigurationTypeInterface _parentConfiguration;
	private String _description;
	private ConfigurationLocation _location;
	private boolean _overrideable;
	private boolean _derived;  // if set, then the configuration needs to replace values enclosed in curly braces with the value of that string.
	private String  _derivationPattern;
	
	private boolean _isArray = false;
	
	/** what is the underlying type for this parameter? */
	private SourceParameterType _type = SourceParameterType.UNKNOWN;

	/** should this value be encrypted */
	private boolean _encrypted = false;
	
	
	private String _fullLabel; // includes all of the parent labels, separated by periods.
	
	
	private boolean _exposeToUserInterface = false; // Should this parameter be exposed to the ui (or other aspect).  The UI will create a json object to get the settings
	
	private ConfigurationType(String label, boolean required, ConfigurationTypeInterface parent, String description, ConfigurationLocation location, boolean overrideable) {
		this(label,required,parent,description,location,overrideable,false);
	}
	
	private ConfigurationType(String label, boolean required, ConfigurationTypeInterface parent, String description, ConfigurationLocation location, boolean overrideable, boolean exposeToUserInterface) {
		this(label,required,parent,description,location,overrideable,exposeToUserInterface, false, SourceParameterType.STRING, false);
	}

	private ConfigurationType(String label, boolean required, ConfigurationTypeInterface parent, String description, ConfigurationLocation location, boolean overrideable, boolean exposeToUserInterface, boolean encryptValue, SourceParameterType type, boolean isArray) {
		_label = label;
		_required = required;
		_parentConfiguration = parent;
		_description = description;
		_location = location;
		_overrideable = overrideable;
		_exposeToUserInterface = exposeToUserInterface;
		_encrypted = encryptValue;
		_type      = type;
		_isArray   = isArray;
	}	
	
	
    
	private ConfigurationType(String label, String pattern, String description) {
		_label = label;
		_required = false;
		_parentConfiguration = null;
		_description = description;
		_location = null;
		_overrideable = false;
		_derived = true;
		_derivationPattern = pattern;
		_exposeToUserInterface = false;
		_encrypted = false;
		_type      = SourceParameterType.STRING;
		_isArray   = false;
	}
	
	
	
	public String toString() { return _label; }
	
	public String getLabel() { return _label; }
	
	public boolean isRequired() { return _required; }
	
	public ConfigurationTypeInterface getParentConfiguration() {
		return _parentConfiguration;
	}
	
	
	public String getDescription() {
		return _description;
	}
	
	public ConfigurationLocation getLocation() {
		return _location;
	}
	
	public boolean isOverrideable() {
		return _overrideable;
	}
	
	public boolean isDerived() {
		return _derived;
	}
	
	public boolean exposeToUserInterface() {
		return _exposeToUserInterface;
	}
	
	public String getDerivationPattern() {
		return _derivationPattern;
	}
	
	public boolean isEncrypted() {
		return _encrypted;
	}

	public SourceParameterType getType() {
		return _type;
	}
	
	public boolean isArray() {
		return _isArray;
	}
		
	
	
	public String getFullLabel() {
		if (_fullLabel == null) {
			if (_parentConfiguration == null) {
				_fullLabel = _label;
			}
			else {
				_fullLabel = this.getParentConfiguration().getFullLabel()+"."+_label;
			}
		}
		return _fullLabel;
	}
	
	
	public static ConfigurationType getEnum(String name) {
		return ConfigurationType.valueOf(name.toUpperCase());
	}	
	
}
