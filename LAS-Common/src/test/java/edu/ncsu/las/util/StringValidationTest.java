/**
 * 
 */
package edu.ncsu.las.util;

import org.testng.Assert;

import org.testng.annotations.Test;



/**
 * 
 * 
 *
 */
public class StringValidationTest {

	
	/**
	 * Test method
	 */
	@Test
	public void testContainsHTML() {
		//System.out.println(StringValidation.removeAllHTML("this is a project title"));
		//System.out.println(StringValidation.removeAllHTML("this is <script> title"));
	
		
		Assert.assertEquals(StringValidation.containsHTML("this is a project title"),false);
		Assert.assertEquals(StringValidation.containsHTML("this is a <script>project title"),true);
		Assert.assertEquals(StringValidation.isValidURL("https://www.ncsu.edu/academics/"),true);
		Assert.assertEquals(StringValidation.isValidURL("hasdfttps://www.ncsu.edu/academics/"),false);
		Assert.assertEquals(StringValidation.isValidURL("https://edu/academics/"),false);
		Assert.assertEquals(StringValidation.isValidURL("https://www.ncsu.edu:80/academics/page?param=true"),true);
		Assert.assertEquals(StringValidation.isValidURL("https://www.ncsu.edu:aa/academics/page?param=true"),false);

	}
	
	/**
	 * Test method
	 */
	@Test
	public void isValidEmailAddressTest() {
		Assert.assertEquals(StringValidation.isValidEmailAddress(""),false);
		Assert.assertEquals(StringValidation.isValidEmailAddress("steve"),false);
		Assert.assertEquals(StringValidation.isValidEmailAddress("34234@test"),false);
		Assert.assertEquals(StringValidation.isValidEmailAddress("john.doe@test.io"),true);
		Assert.assertEquals(StringValidation.isValidEmailAddress("doe@test.io"),true);		
	}

}
