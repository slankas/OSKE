/**
 * 
 */
package edu.ncsu.las.util;

import org.testng.Assert;

import java.text.ParseException;
import java.util.Date;

import org.testng.annotations.Test;



/**
 *  
 *
 */
public class CronExpressionTest {

	public static final long MILLIS_PER_DAY = 86400000;
	/**
	 * Test method
	 */
	@Test
	public void testPeriod() {
		String expresion = "0 0 16 * * ?";
		
		CronExpression ce;
		try {
			ce = new CronExpression(expresion);
			Date nextRunTime = ce.getNextValidTimeAfter(new java.util.Date());
			Date followOnRuntime = ce.getNextValidTimeAfter(ce.getNextInvalidTimeAfter(nextRunTime));
			
			System.out.println(nextRunTime);
			System.out.println(followOnRuntime);
			
			long millis = followOnRuntime.getTime() - nextRunTime.getTime();
			System.out.println(millis);
			Assert.assertEquals(ce.getMilliSecondsBetweenRuns(),MILLIS_PER_DAY);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	@Test
	public void testCE() {
		try {
			CronExpression ce = new CronExpression("0 0 16 ? * 2-6");
			System.out.println(ce.getNextValidTimeAfter(new Date()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	

}
