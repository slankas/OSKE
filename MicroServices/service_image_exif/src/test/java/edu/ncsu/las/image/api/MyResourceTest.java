package edu.ncsu.las.image.api;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.ncsu.las.image.api.Main;

import java.nio.file.Files;

public class MyResourceTest {

    private HttpServer server;
    private WebTarget target;

    @BeforeClass
    public void setUp() throws Exception {
        // start the server
    	Main.main(new String[1]);
        server = Main.startServer("http://0.0.0.0:9001/exif/");

        Client c = ClientBuilder.newClient((new ClientConfig()).register(MultiPartFeature.class));
        target = c.target("http://0.0.0.0:9001/exif/");
    }

    @AfterClass
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void testGetIt() {
    	try {
	    	java.io.File f = new java.io.File("./testFile/test.jpg");

	    	byte[] data = Files.readAllBytes(f.toPath());
	    	
	    	try (FormDataMultiPart part = new FormDataMultiPart()) {
	    		FormDataMultiPart multipart = part.field("file", data,MediaType.APPLICATION_OCTET_STREAM_TYPE);
	    		Response response = target.path("v1/extract").request().post(Entity.entity(multipart, multipart.getMediaType()));
	    		multipart.close();
	    		System.out.println(response.getStatus() + " "  + response.getStatusInfo() + " " + response);
	    		String output = response.readEntity(String.class);
	    		System.out.println( (new JSONObject(output)).toString(4));
	    	}
    	}
    	catch (Exception e) {
    		System.err.println(e);
    	}
        
        
    }
}
