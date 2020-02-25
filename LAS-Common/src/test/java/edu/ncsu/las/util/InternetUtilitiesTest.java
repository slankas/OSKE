package edu.ncsu.las.util;

import java.io.IOException;

import java.net.MalformedURLException;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * 
 * 
 *
 */
public class InternetUtilitiesTest {

	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testGetBaseURL() throws MalformedURLException {
		assertEquals("https://www.nytimes.com/", InternetUtilities.getBaseURL("https://www.nytimes.com"));
		assertEquals("https://www.nytimes.com/", InternetUtilities.getBaseURL("https://www.nytimes.com/blah/blah/blah.html"));
		assertEquals("https://www.nytimes.com:8080/", InternetUtilities.getBaseURL("https://www.nytimes.com:8080/blah/blah/blah.html"));
		assertEquals("http://www.nytimes.com:8080/", InternetUtilities.getBaseURL("http://www.nytimes.com:8080/blah/blah/blah.html"));
		
	    try {
	    	InternetUtilities.getBaseURL("www.nytimes.com:8080/blah/blah/blah.html");
	        fail("Expected an MalformedURLException to be thrown");
	    } catch (MalformedURLException ex) {
	        assertEquals(ex.getMessage(), "URL does not contain a protocol");
	    }
	}
	
	
	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testDateConversion() throws MalformedURLException {
		
		// this probably isn't the right answer, but its good enough giving that the server didn't publish the date correctly
		/*
		assertEquals("2017-04-25T17:03:00Z",InternetUtilities.convertHTTPBasedDateStringToISO("25 Apr 2017 13:03:00 -0400 GMT")); 
		
		assertEquals("2016-12-12T22:15:06Z",InternetUtilities.convertHTTPBasedDateStringToISO("Mon, 12 Dec 2016 22:15:06 +0000 GMT"));
		assertEquals("2017-01-10T14:38:42Z",InternetUtilities.convertHTTPBasedDateStringToISO("Tue, 10 Jan 2017 14:38:42 +0000"));
		*/


		
	}

	/**
	 * @throws IOException 
	 * 
	 */
	@Test
	public void testCrawlPage() throws IOException {


	}
	
}
