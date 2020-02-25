package edu.ncsu.las.source;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.fail;

import java.io.File;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.collector.Collector;

public class RandomUserAgentTest {
	
	@BeforeClass
    public void setUp() {
    	try {
    		File currentDirectory = new File(new File(".").getAbsolutePath());			
    		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
    		
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true,false);
    	}
    	catch (Exception e) {
    		System.err.println("ElasticSearchExportTest setup failed: "+e);
    		
    		fail("unable to init Collector");
    	}
    }
	
	@Test
	public void TestUserAgent() {
		
		String output = "";
		
		for (int i=0; i<6; i++) {
			output = SourceHandlerInterface.getNextUserAgent("sandbox");
			assertNotEquals(output, "");
			
			System.out.println("output "+i+" is: "+output);
		}
		
		
		
		
	}

}
