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
public class DeleteCollectionTest {

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
		String index = "collection";
		String type = "_doc";
		String collectionID = "0f018b02-4252-456d-8247-73166ab2dd6f";

		Client client;
		try {
			logger.log(Level.FINER, "Push to ElasticSearch  ");
			client = getClient(host, port);

			DeleteResponse response = client.prepareDelete(index, type, collectionID)
			        .execute()
			        .actionGet();
			
			System.out.println("Deleted " + response.isFound());
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Unable to save to ElasticSearch", e);
		} finally {
			destroyConnection(host, port);			
		}
	 */
	}
	

}
