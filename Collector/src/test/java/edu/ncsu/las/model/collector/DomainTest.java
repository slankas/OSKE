package edu.ncsu.las.model.collector;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class DomainTest {
	
	/**
	 * Test method blocking top level domains
	 */
	@Test
	public void testTLDBlock() {
		Domain d= new Domain("domainInstanceName", "domainStatu", new java.sql.Timestamp(System.currentTimeMillis()), "Testing",
	             "description", "contanct", 1, "{}", "",new java.sql.Timestamp(System.currentTimeMillis()),false);
		d._blockedTopLevelDomains = new java.util.HashSet<String>();
		d._blockedTopLevelDomains.add(".gb");
		d._blockedTopLevelDomains.add(".us");
		assertEquals(d.isTopLevelDomainBlocked("www.somedomain.com"),false);
		assertEquals(d.isTopLevelDomainBlocked("www.somedomain.au"),false);
		assertEquals(d.isTopLevelDomainBlocked("www.somedomain.us") ,true);
		assertEquals(d.isTopLevelDomainBlocked("www.somedomain.gb"),true);
		assertEquals(d.isTopLevelDomainBlocked("stuff"),false);
	}
	
}
