package edu.ncsu.las.model.collector;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

/**
 * 
 *
 */
public class UserPasswordTest {

	/** 
	 * Test that we are generating random salts...
	 * 
	 * produce 256 and make sure that they are different
	 */
	@org.testng.annotations.Test
	public void testSaltGeneration()  {
		int numSalts = 1024;
		byte[][] salts = new byte[numSalts][];
		for (int i=0; i < numSalts; i++) {
			salts[i] = UserPassword.getNextSalt();
		}
		for (int i=0; i < numSalts; i++) {
			for (int j=i+1; j < numSalts; j++) {
				assertEquals(Arrays.equals(salts[i], salts[j]), false);
			}
		}
	}
	
	@org.testng.annotations.Test
	public void testPasswordMatches()  {
		byte[] salt = UserPassword.getNextSalt();
		String clearTextPassword = "ThisIsTheBestThingEver!2004@test";
		
		byte[] password = UserPassword.hash(clearTextPassword, salt);
		assertEquals(UserPassword.isExpectedPassword(clearTextPassword, salt, password),true);
		
		assertEquals(password.length *8,256);
	}
	
	@org.testng.annotations.Test
	public void testPasswordFails()  {
		byte[] salt = UserPassword.getNextSalt();
		String clearTextPassword = "ThisIsTheBestThingEver!2004@test";
		
		byte[] password = UserPassword.hash(clearTextPassword, salt);
		assertEquals(UserPassword.isExpectedPassword("not the same", salt, password),false);
	}
}
