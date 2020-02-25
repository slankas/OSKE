package edu.ncsu.las.collector.util;



import static org.testng.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * 
 *
 */
public class ElasticSearchExportTest {

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
	 * Test method for {exportQueryToFile}.
	 */
	@Test
	public void testExportQueryToFile() {
		
		try {
			Path tempPath = Paths.get(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.EXPORT_DOWNLOAD_PATH));

			if (!Files.exists(tempPath)) {
				Files.createDirectory(tempPath);
			}

			//String outputFile = Paths.get(tempPath.toString(), UUID.randomUUID().toString()).toString() + ".json";

						
		    String queryString = "{\"query\":{\"match\":{\"text\":\"alien bestgear\"}},\"filter\":{\"bool\":{\"must\":[],\"should\":[],\"must_not\":[{\"match\":{\"domain\":\"amazon.com\"}}]}}}";
		    queryString = "{\"query\":{\"match\":{\"text\":\"alien bestgear\"}},\"filter\":{\"bool\":{\"must\":[]}}}";
		   // queryString ="{\"query\":{\"match\":{\"text\":\"alien bestgear\"}},\"highlight\":{\"fields\":{\"text\":{\"fragment_size\":400,\"number_of_fragments\":1}}},\"from\":0,\"size\":20,\"filter\":{\"bool\":{\"must\":[{\"range\":{\"crawled_dt\":{\"gte\":1457283677000}}}],\"should\":[],\"must_not\":[{\"match\":{\"domain\":\"amazon.com\"}}]}}}";
			//queryString = "{\"query\":{\"match_phrase\":{\"text\":\"return to home\"}},\"highlight\":{\"fields\":{\"text\":{\"fragment_size\":400,\"number_of_fragments\":1}}},\"from\":0,\"size\":20,\"filter\":{\"bool\":{\"must\":[{\"term\":{\"domain\":\"amazon.com\"}}],\"should\":[],\"must_not\":[]}}}";
		    
		    
		    JSONObject q = new JSONObject(queryString);			

			//JSONObject query = q.getJSONObject("query");
			//query.put("bool", q.getJSONObject("filter").getJSONObject("bool"));

			System.out.println(q.toString(4));

			//SearchExport.exportQueryToFile(q, outputFile);
	
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}		
	}
	
}
