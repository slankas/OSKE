package edu.ncsu.las.model.collector;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.EmailClient;

/**
 * 
 *
 */


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EmailTester {
	static final Logger srcLogger =Logger.getLogger(EmailTester.class.getName());

    @BeforeClass
    public void initialize() { 
    	try{
   			File currentDirectory = new File(new File(".").getAbsolutePath());
			String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
						
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
			srcLogger.log(Level.INFO, "Forum VBulleting Test Started");
			srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);
	
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false, false);	
    	}
    	catch (Exception e) {
    		System.err.println(e);
    		System.exit(-1);
    	}
    }

    @Test
    public void test1(){
    	try{
	    	String title = "My Test";
	    	String body  = "Hello John, Can you see this?";
	    	
	    	EmailClient ec = Collector.getTheCollecter().getEmailClient();
	    	ec.sendMessage("user@ncsu.edu", title, body);
	    	TimeUnit.SECONDS.sleep(5);
    	}
    	catch (Exception e)  {
    		System.err.println(e);
    	}
    }


}
