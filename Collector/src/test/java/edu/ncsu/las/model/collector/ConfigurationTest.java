package edu.ncsu.las.model.collector;

import org.json.JSONObject;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import edu.ncsu.las.model.collector.type.ConfigurationType;



public class ConfigurationTest {


	public String baseConfiguration = "{\"collector\":{\"id\":\"laptop\",\"instrumentationProjectID\":\"LAS/Collector\",\"jobPoolSize\":5,\"taskPoolSize\":5,\"searchPoolSize\":25,\"idleKillTimeSec\":600,\"sleepTimeSec\":60,\"workingDirectory\":\"collector_\",\"domainName\":\"openke\",\"allowServices\":true,\"daemonBuild\":\"20160111.11\",\"webBuild\":\"20160111.11\",},\"database\":{\"datasourcename\":\"CollectorDataSource\",\"servername\":\"192.168.56.101\",\"databasename\":\"openke_dev\",\"portnumber\":5432,\"user\":\"openke\",\"password\":\"openKEdev!\",\"maxconnections\":10},\"import\":{\"directory\":\"C:/Users/user/Desktop/uploadDir\",\"sleepTimeSec\":300},\"storage\":[{\"name\":\"File Storage\",\"fullclasspathname\":\"edu.ncsu.las.storage.FileStorage\",\"normal\":\"C:/collection_files/\",\"archive\":\"C:/collection_files_archive/\",\"sandbox\":\"C:/collection_files_sandbox/\"},{\"name\":\"Accumulo Storage\",\"fullclasspathname\":\"edu.ncsu.las.storage.AccumuloStorage\",\"normal\":\"drone_normal\",\"archive\":\"drone_archive\",\"sandbox\":\"drone_sandbox\",\"instanceName\":\"hdp-accumulo-instance\",\"zooKeepers\":\"serverNameOrIP:2181\",\"userName\":\"openke\",\"password\":\"las2016!\"}],\"jsonStore\":{\"elasticsearch\":{\"normal\":\"openke\",\"archive\":\"drones_archive\",\"sandbox\":\"openke_sandbox\",\"normalRestEndPoint\":\"http://serverNameOrIP:9200/openke/\",\"archiveRestEndPoint\":\"http://serverNameOrIP:9200/drones_archive/\",\"sandboxRestEndPoint\":\"http://serverNameOrIP:9200/openke_sandbox/\",\"storageHost\":\"serverNameOrIP\",\"storagePort\":9300,\"collectionType\":\"collection\"},\"kafka\":{\"normal_queueName\":\"openke\",\"sandbox_queueName\":\"\",\"properties\":{\"bootstrap.servers\":\"serverNameOrIP:6667\",\"key.serializer\":\"org.apache.kafka.common.serialization.StringSerializer\",\"value.serializer\":\"org.apache.kafka.common.serialization.StringSerializer\",\"compression.type\":\"snappy\"},\"replicationFactor\":1,\"numPartitions\":5,\"zookeeper\":\"serverNameOrIP:2181\"},\"hdfs\":{\"user\":\"hdfs\",\"normal_uri\":\"hdfs://serverNameOrIP/drones/\",\"sandbox_uri\":\"hdfs://serverNameOrIP/sandbox_drones/\",}},\"annotations\":[\"referrer\",\"http_headers\",\"structured_data\",\"html_title\",\"html_meta\",\"open_graph\",\"html_outlinks\",\"provenance\",\"dataHeader\",\"language\",\"tikaMetaData\",\"published_date\",\"concepts\"],\"export\":{\"download\":{\"path\":\"C:\\tmp\",\"timeToLiveHours\":25,\"maxRecordCount\":1000000},\"voyant\":{\"path\":\"C:\\tmp\",\"url\":\"http://las-serverNameOrIP:8888\",\"maxRecordCount\":1000},\"directory\":{\"path\":\"C:\\tmp\",\"maxRecordCount\":-1}},\"searchConsumer\":{},\"webCrawler\":{\"politenessDelay\":500,\"maxDepthOfCrawling\":-1,\"maxPagesToFetch\":-1,\"includeBinaryContentInCrawling\":true,\"maxDownloadSize\":20000000,\"userAgentString\":\"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36\"},\"hyperlinks\":[{\"displayText\":\"ElasticSearch: HQ\",\"link\":\"http://serverNameOrIP:9200/_plugin/hq\"},{\"displayText\":\"ElasticSearch: Kopf\",\"link\":\"http://serverNameOrIP:9200/_plugin/kopf\"},{\"displayText\":\"Kibana\",\"link\":\"http://serverNameOrIP:5601\"}],\"email\":{\"server\":\"smtp.gmail.com\",\"port\":587,\"user\":\"las_opensourceke@ncsu.edu\",\"password\":\"CHANGE_PASSWORD\"},\"faroo\":{\"key\":\"faroo_key\"},\"graphAPI\":\"http://serverNameOrIP:9001/GraphAPI/\",\"textrankAPI\":\"http://serverNameOrIP:5000/textrank/\",\"secretPhrase\":\"a deep, really deep, dark secret\"}";

	public String override = "{\"email\":{\"password\":\"password\",\"port\":587,\"user\":\"las_opensourceke@ncsu.edu\"}}";


	public String derivedFileTest = "{\"fileRoot\": \"c:\\\\collector\\\\documentRoot\" }";

	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testCreation() {
		JSONObject baseConfig = new JSONObject(baseConfiguration);
		JSONObject overConfig = new JSONObject(override);

		Configuration conf = new Configuration("system",baseConfig);
		conf.overrideSetProperties("mytest",overConfig);


		//System.out.println(domainConf.getConfiguration().toString(4));
		//java.util.List<String> fields = JSONUtilities.listAllFields(domainConf.getConfiguration());
		//fields.stream().forEach(System.out::println);
	}

	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testDerived() {
		JSONObject deriveConfig = new JSONObject(derivedFileTest);
		JSONObject overConfig = new JSONObject(override);

		Configuration conf = new Configuration("base",deriveConfig);
		Configuration domainConf = conf.overrideSetProperties("mytest",overConfig);

		String testValue = domainConf.getConfigurationProperty(ConfigurationType.IMPORT_DIRECTORY,null);
		assertEquals(testValue,"c:\\collector\\documentRoot/mytest/import/");
		/*
		System.out.println(domainConf.getConfiguration().toString(4));
		java.util.List<String> fields = JSONUtilities.listAllFields(domainConf.getConfiguration());
		fields.stream().forEach(System.out::println);
		*/
	}


	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testGettingObject() {
		JSONObject deriveConfig = new JSONObject(derivedFileTest);
		JSONObject overConfig = new JSONObject(override);

		Configuration conf = new Configuration("base",deriveConfig);
		conf.overrideSetProperties("mytest",overConfig);

		JSONObject testValue = Configuration.getConfigurationObject("mytest", ConfigurationType.EMAIL);
		assertEquals("{\"password\":\"password\",\"port\":587,\"user\":\"las_opensourceke@ncsu.edu\"}", testValue.toString());




	}

}
