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
public class GetCollectionsTest {

	private static Logger logger =Logger.getLogger(Collector.class.getName());

    @BeforeClass
    public void setUp() {
    	try {
    		File currentDirectory = new File(new File(".").getAbsolutePath());			
    		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
    		
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true, false);
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

	}


}
