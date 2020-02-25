package edu.ncsu.las.elastic.collection;




import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.fail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.collector.Collector;


/**
 * 
 *
 */
public class CreateCollectionTest {

	private static Logger logger =Logger.getLogger(Collector.class.getName());

    @BeforeClass
    public void setUp() {
    	try {
    		File currentDirectory = new File(new File(".").getAbsolutePath());			
    		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
    		
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true,false);
    	}
    	catch (Exception e) {
    		System.err.println("ElasticSearchExportTest setup failed: "+e);
    		
    		fail("unable to iniatilized Collector");
    	}
    }	
	
	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testExportQueryToFile() {
		logger.log(Level.SEVERE, "ElasticSearch - test collection");
		logger.setLevel(Level.ALL);
/*
		String host = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_STORAGEHOST);
		int    port = (int) Collector.getTheCollecter().getConfigurationPropertyAsLong(ConfigurationType.JSONSTORE_ELASTIC_STORAGEPORT);
		String index = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_NORMAL);
		String type = Collector.getTheCollecter().getConfigurationProperty(ConfigurationType.JSONSTORE_ELASTIC_COLLECTION_TYPE);
		
		List<JSONObject> shared_with = new ArrayList<JSONObject>();
		
		
		
		JSONObject document = new JSONObject();
		UUID id = UUID.randomUUID();
		document.put("id", id);
		document.put("name", "Test collection");
		document.put("owner", owner);
		document.put("owner_name", owner_name);
		document.put("description", "Description for collection" );
		document.put("date_created", Instant.now().toString());
		document.put("personal_notes", "");
		
		document.put("shared_with", shared_with);
//		document.put("shared_with", java.util.Arrays.asList("shared1@ncsu.edu", "shared2@ncsu.edu"));
		
		Client client;
		try {
			logger.log(Level.FINER, "Push to ElasticSearch  ");
			client = getClient(host, port);
								
			IndexResponse response = client.prepareIndex(index, type, id.toString()).setSource(document.toString()).get();
			logger.log(Level.FINER, "ElasticSearch saved: "+ response.toString());
			System.out.println("Successfully created collection: " + response.toString());
			
			GetResponse record = client.prepareGet(index, type, id.toString()).execute().actionGet();
			System.out.println("Record: " + response.getId());
			System.out.println(record.getSourceAsMap());

		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Unable to save to ElasticSearch", e);
			logger.log(Level.SEVERE,document.toString(4));
		} finally {
			destroyConnection(host, port);			
		}
	 */
	}

}
