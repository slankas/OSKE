package edu.ncsu.las.internet.api;



import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.api.WhoIsMain;

public class WhoIsTester {

    private WebTarget target;

    @BeforeClass
    public void setUp() throws Exception {
    	
    	WhoIsMain.startServer("http://0.0.0.0:9001/whois/","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36");
		
        // create the client
        Client c = ClientBuilder.newClient(new ClientConfig());

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        target = c.target("http://0.0.0.0:9001/whois/");
    }

    @AfterClass
    public void tearDown() throws Exception {
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void testGetIt() {
    	long start = System.currentTimeMillis();
        

    	
    	String[] testLocations = { 
    			"newsday.co.tt"
    			//"welt.de:80"
    			//"www.draqkhan.com.pk"
    			//"www.telegraph.co.uk"
    			//"wikipedia.org" 
    			//"paperpkads.pk"
    			//"www.draqkhan.com.pk"
    			//"www.amazon.com"
    			//"www.corriere.it"//"santiagotimes.cl"////"research.omicsgroup.org"//"topics.revolvy.com"//"washingtonpost.com"//"ncsu.edu" //"washingtonpost.com"// "ncsu.edu"// ,"washingtonpost.com", "nytimes.com"
    	};
    	
    	
    	for (String location: testLocations) {
	    	try {
		        Response response = target.path("v1/find/"+location).request().get();
		        System.out.println(response.getStatus() + " " + response.getStatusInfo() + " " + response);
		        String output = response.readEntity(String.class);
		        JSONObject result = new JSONObject(output);
		        Assert.assertEquals(result.getJSONObject("whois").getString("Domain Name"),"newsday.co.tt"); // need to create a parallel array for answers, and loop explicitly with indexes
		        System.out.println(result.toString(4));
	    	}
	    	catch (Exception e) {
	    		System.err.println(e);
	    	}
    	}
 
    	System.out.println(System.currentTimeMillis()-start);
    }
  

}
