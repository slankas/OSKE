/**
 * 
 */
package edu.ncsu.las.util;

import org.testng.Assert;

import java.time.Instant;
import java.time.ZonedDateTime;


import org.testng.annotations.Test;



/**
 *
 * 
 *
 */
public class DateUtilitiesTest {

	/**
	 * Test method
	 */
	@Test
	public void testDate() {
		String[] sampleDates = { "2015-06-08T12:48:48Z-0400", "Monday, October 03, 2016",  "October 03, 2016", "Sep 19, 2012",  "2015-12-20",
				                 "2015-12-20", "2012-09-19 05:55:59 UTC", "2015-12-15T07:59:51-0700",
				                 "01 October, 2015","01/23/2016","2016-09-15","2016-02-26", "2016-02-20T14:35+11:00","02-03-2016", "12/01/2010 02:27",
				                 "2016-10-05T11:44:29.713-04:00[America/New_York]","Wed, 05 Oct 2016 22:27:47 GMT","20170109","Tue, 2012-01-24 14:32","2015-08-5",
				                 "Thu Jan 04 2018 14:56:33 GMT+0000 (UTC)","Aug 14, 2018 3:00 AM PT","9/23/2018 9:08:47 PM"};

		for (String text: sampleDates) {
			System.out.println(text+": "+DateUtilities.getFromString(text));
		}
	}


	@Test
	public void testOtherDate() {
		String[] sampleDates = { "Sep 21, 2018, 8:52 AM"};

		for (String text: sampleDates) {
			System.out.println(text+": "+DateUtilities.getFromString(text));
		}
	}
	
	
	/**
	 * Test method
	 */
	@Test
	public void testDifferentTimeZones() {
		

		
		//String text = "2016-03-18 07:40:51";
		String[] dateTimeStrings = {"2016-03-18 07:40:51", "2016-08-01T07:26:00-03:00", "Tue, 3 Jun 2008 11:05:30 GMT","2016-07-25T22:01:52+00:00" } ; //,"2016-07-29T22:07:00-03:00","2016-07-08T18:10:00-03:00",,"2016-08-02T02:12:57Z","2016-05-31T03:00:08+00:00","2016-08-02T02:15:37Z","2016-08-01T07:26:00-03:00","2016-08-02T03:49:27Z","2016-08-01T07:26:00-03:00","2016-07-29T22:07:00-03:00","2016-07-31T22:25:00-03:00","2016-08-02T04:57:05Z","2016-07-25T22:01:52+00:00","2016-08-02T06:55:41Z","2016-05-31T03:00:08+00:00","2016-07-29T22:07:00-03:00","2016-07-31T22:25:00-03:00","2016-08-02T09:17:03Z","2016-07-25T22:01:52+00:00","2016-08-02T14:41:08Z","2016-08-02T15:07:01Z","2016-08-02T17:45:55Z","2016-08-02T18:30:49+00:00","2014-08-15T16:14:37+00:00"
		String[] result = {"2016-03-18T07:40:51Z","2016-08-01T10:26Z","2008-06-03T11:05:30Z","2016-07-25T22:01:52Z"};
		
		Assert.assertEquals(dateTimeStrings.length,result.length,"Test configuration error - arrays not of same length");
		
		for (int i=0; i<result.length;i++) {
			ZonedDateTime zdt = DateUtilities.getFromString(dateTimeStrings[i]);
			Assert.assertTrue(result[i].equals(zdt.toString()),"Not equals: ("+zdt.toString()+","+result[i]+")");
		}
		
	}	
	
	
	/**
	 * Test method
	 */
	@Test
	public void testDateFormatting() {
		Instant i = Instant.ofEpochMilli(1448990122000L);
		Assert.assertEquals("2015-12-01T17:15:22Z",DateUtilities.getDateTimeISODateTimeFormat(i));
		Assert.assertNotEquals("2015-12-01T17:15:22Z",DateUtilities.getDateTimeISODateTimeFormat(Instant.ofEpochMilli(1435767322000L)));
		Assert.assertEquals("2015-07-01T16:15:22Z",DateUtilities.getDateTimeISODateTimeFormat(Instant.ofEpochMilli(1435767322000L)));
	}
	
	/**
	 * Test method
	 */
	@Test
	public void testAnotherDateStamp() {
		String[] dateTimeStrings = {"Tue Nov 22 16:24:15 +0000 2016", "Fri Dec 20 11:20:21 +0000 2013"};
		String[] result = {"2016-11-22T16:24:15Z","2013-12-20T11:20:21Z"};
		for (int i=0; i<dateTimeStrings.length;i++) {
			ZonedDateTime zdt = DateUtilities.getFromString(dateTimeStrings[i]);
			Assert.assertEquals(result[i],zdt.toString());
		}
	}
}
