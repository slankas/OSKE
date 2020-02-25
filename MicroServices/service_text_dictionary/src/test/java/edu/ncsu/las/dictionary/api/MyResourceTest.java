package edu.ncsu.las.dictionary.api;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.dictionary.Main;


public class MyResourceTest {

    private WebTarget target;

    @BeforeClass
    public void setUp() throws Exception {
    	
		Main.initialize("APPLICATION_PROPERTIES");
		Main.startServer(); 
        // create the client
        Client c = ClientBuilder.newClient(new ClientConfig());

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        target = c.target("http://0.0.0.0:9001/geo/");
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
    			"North Carolina State University, Raleigh, NC, 27695",
    			"Washington D.C.",
    			"University of Miami, Florida",
    			"Atlanta, GA",
    			"Paris, France",
    			"North Carolina State University, Raleigh, NC, 27695",
    			"Atlanta, GA",
    			"Paris, France",
    	};
    	
    	for (String location: testLocations) {
	    	try {
		        Response response = target.path("v1/geoCode").queryParam("location", location).request().get();
		        System.out.println(response.getStatus() + " " + response.getStatusInfo() + " " + response);
		        String output = response.readEntity(String.class);
		        System.out.println( (new JSONObject(output)).toString(4));
	    	}
	    	catch (Exception e) {
	    		System.err.println(e);
	    	}
    	}
    	System.out.println(System.currentTimeMillis()-start);
    	try {
	        Response response = target.path("v1/statistics").request().get();
	        System.out.println(response.getStatus() + " " + response.getStatusInfo() + " " + response);
	        String output = response.readEntity(String.class);
	        System.out.println( (new JSONObject(output)).toString(4));
    	}
    	catch (Exception e) {
    		System.err.println(e);
    	}
        
    }
}
