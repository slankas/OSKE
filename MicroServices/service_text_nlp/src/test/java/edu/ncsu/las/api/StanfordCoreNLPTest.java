package edu.ncsu.las.api;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;


public class StanfordCoreNLPTest {

	 private WebTarget target;
    
    HttpServer _server = null;

    @BeforeClass
    public void setUp() throws Exception {
    	
		String serviceURI="http://0.0.0.0:9001/nlp/";

		_server = StanfordCoreNLPMain.startServer(serviceURI); 
		
		Client c = ClientBuilder.newClient(new ClientConfig());
		target = c.target("http://0.0.0.0:9001/nlp/");
    }

    @AfterClass
    public void tearDown() throws Exception {
    	_server.shutdown();
    }
    

    /**
     * Send test text and examine the output
     */
    @Test
    public void testMessagee() {
	  	String text = "The government has directed significant resources towards cyber security.  In 2018, the federal government announced a new strategy to fight election fraud.";

	  	JSONObject message = new JSONObject().put("text",text);
	  	
    	long start = System.currentTimeMillis();
    	try {
	        Response response = target.path("v1/process").request().post(Entity.json(message.toString()));
	        assertEquals(response.getStatus(),200);
	        System.out.println(response.getStatus() + " " + response.getStatusInfo() + " " + response);
	        String output = response.readEntity(String.class);
	        JSONObject result = new JSONObject(output);
	        System.out.println( result.toString(4));
	        
	        assertEquals(result.getJSONArray("sentences").length(), 2);
    	}
    	catch (Exception e) {
    		System.err.println(e);
    	}
    	System.out.println(System.currentTimeMillis()-start);
        
    }
}
