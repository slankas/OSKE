package edu.ncsu.las.topic.model;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import edu.ncsu.las.topicmodel.api.Main;

import java.util.concurrent.TimeUnit;

public class LDATest {

    private static HttpServer server;
    private static WebTarget target;

    @BeforeClass
    public void setUp() throws Exception {
        // start the server
    	//Main.main(new String[1]);
        server = Main.startServer("http://0.0.0.0:9001/topicmodel/");
        // create the client
        Client c = ClientBuilder.newClient((new ClientConfig()));
        target = c.target("http://0.0.0.0:9001/topicmodel/");
    }

    @AfterClass
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    /**
     * Test sending a bad session ID to get status
     *
     */
    @Test
    public void testSessionDoesNotExist() {
    	try {
	        Response response = target.path("v1/LDA/19cb1423-cf10-4ce4-bcd1-5cac84f2dd9f").request().accept(MediaType.APPLICATION_JSON).get();
	        //System.out.println(response.getStatus() + " " + response.getStatusInfo() + " " + response);
	        String output = response.readEntity(String.class);
	        JSONObject result = new JSONObject(output);
	        Assert.assertEquals("failure", result.getString("status"));
	        Assert.assertEquals(1, result.getJSONArray("errors").length());
	        Assert. assertEquals("19cb1423-cf10-4ce4-bcd1-5cac84f2dd9f",result.getString("sessionUUID"));
    	}
    	catch (Exception e) {
    		System.err.println(e);
    	}
    }


    /**
     * Test to see that the message
     */
    @Test
    public void testGetIt() {
    	try {
    		JSONArray documents = new JSONArray();

    		documents.put(new JSONObject().put("url", "http://www.test1.com").put("uuid", "uuid1").put("text", "text for document 1"));
    		documents.put(new JSONObject().put("url", "http://www.test2.com").put("uuid", "uuid2").put("text", "text for document 2"));
    		documents.put(new JSONObject().put("url", "http://www.test3.com").put("uuid", "uuid3").put("text", "text for document 3"));
    		documents.put(new JSONObject().put("url", "http://www.test4.com").put("uuid", "uuid4").put("text", "text for document 4"));

    		JSONObject request =  new JSONObject().put("documents", documents);
    		request.put("numTopics",2);
        	request.put("numKeywords",10);
        	request.put("maxIterations",2);
        	request.put("stemWords",false);


	        Response response = target.path("v1/LDA").request().accept(MediaType.APPLICATION_JSON).post(Entity.entity(request.toString(), "application/json; charset=UTF-8"));

	        //System.out.println(response.getStatus() + " "   + response.getStatusInfo() + " " + response);
	        String output = response.readEntity(String.class);
	        JSONObject jsonResponse = new JSONObject(output);
	        System.out.println( jsonResponse.toString(4));
	        String sessionID = jsonResponse.getString("sessionUUID");

	        while (true) {
	        	 Response statusResponse = target.path("v1/LDA/"+sessionID).request().accept(MediaType.APPLICATION_JSON).get();
	 	        //System.out.println(response.getStatus() + " " + response.getStatusInfo() + " " + response);
	        	String textResponse = statusResponse.readEntity(String.class);
	        	System.out.println(textResponse);
	 	        JSONObject statusResult = new JSONObject(textResponse);
	 	        if (statusResult.getString("message").contains("failure")) {
	 	        	break;
	 	        }
	 	        if (statusResult.getString("message").contains("complete")) {
	 	        	Assert.assertEquals(sessionID,statusResult.getString("sessionUUID"));
	 	        	Assert.assertEquals(2,statusResult.getJSONArray("topics").length());
	 	        	System.out.println(statusResult.toString(4));
	 	        	break;
	 	        }
	 	        System.out.println(statusResult.getString("status"));
	 	        TimeUnit.SECONDS.sleep(5);
	        }
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}


    }
}
