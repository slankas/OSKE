package edu.ncsu.las.model.collector;


import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import java.sql.Timestamp;

public class DocumentCollectionTest {

	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testGoodCollection() {
		DocumentBucket dc = new DocumentBucket("","myName","","","","",new Timestamp(System.currentTimeMillis()));
		java.util.ArrayList<String> errors = dc.validate();
		assertEquals(0,errors.size());
	}
	
	/**
	 * Test method to Create a collection.
	 */
	@Test
	public void testBadName() {
		DocumentBucket dc = new DocumentBucket("","myNa$me","","","","",new Timestamp(System.currentTimeMillis()));
		java.util.ArrayList<String> errors = dc.validate();
		assertEquals(1,errors.size());
	}
	
}
