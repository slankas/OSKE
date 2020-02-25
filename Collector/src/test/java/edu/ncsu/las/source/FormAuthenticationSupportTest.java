package edu.ncsu.las.source;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;


public class FormAuthenticationSupportTest {
	
	@Test
	public void testMD5() {
		String password = "password";
		String md5Hash  = "5f4dcc3b5aa765d61d8327deb882cf99";
		
		String hashPassword  = FormAuthenticationSupport.getMD5HashForPassword(password);
		String hashPassword2 = FormAuthenticationSupport.getMD5HashForPassword(FormAuthenticationSupport.HASH_MD5_INDICATOR+password);
		assertEquals(hashPassword, hashPassword2);
		assertEquals(md5Hash,hashPassword);
	}
}
