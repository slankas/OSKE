package edu.ncsu.las.util;

import java.net.MalformedURLException;
import java.time.Instant;

import org.json.JSONArray;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import edu.ncsu.las.util.json.JSONUtilities;

/**
 * 
 * 
 *
 */
public class JSONUtilitiesTest {

	/**
	 * Test method
	 * @throws MalformedURLException 
	 */
	@Test
	public void testMaintainHistory() throws MalformedURLException {
		
		JSONArray history = JSONUtilities.maintainHistoryArray(null, "description", "Phd Student", Instant.now().toString());
		assertEquals(history.length(), 1);
		history = JSONUtilities.maintainHistoryArray(history, "description", "Phd Student", Instant.now().toString());
		assertEquals(history.length(), 1);
		history = JSONUtilities.maintainHistoryArray(history, "description", "Senior Research Scholar", Instant.now().toString());
		assertEquals(history.length(), 2);
		history = JSONUtilities.maintainHistoryArray(history, "description", "Phd Student", Instant.now().toString());
		assertEquals(history.length(), 3);
		history = JSONUtilities.maintainHistoryArray(history, "description", "Phd Student", Instant.now().toString());
		assertEquals(history.length(), 3);
	}
	
	

	
}
