package edu.ncsu.las.storage;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.fail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.collector.JobCollector;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;

/**
 * 
 *
 */
public class HDFSStorageTest {

	private static Logger logger =Logger.getLogger(HDFSStorageTest.class.getName());

    @BeforeClass
    public void setUp() {
    	try {
    		File currentDirectory = new File(new File(".").getAbsolutePath());			
    		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
    		
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false,false);
			JobCollector.getTheCollecter();
			
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
			logger.log(Level.INFO, "HDFSStorageTest configured");
    	}
    	catch (Exception e) {
    		System.err.println("HDFSStorageTest setup failed: "+e);
    		
    		fail("unable to iniatilized Collector");
    	}
    }	
	
	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testPurgeJob() {
		logger.log(Level.SEVERE, "ElasticSearch - test collection");
		logger.setLevel(Level.ALL);
		HDFSStorage.purgeJobRecords("testscada", FileStorageAreaType.REGULAR, null,null);
	}

}
