package edu.ncsu.las.model.collector;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.testng.annotations.Test;

public class JobTest {
	
	/**
	 * Test method randomizing next run times.
	 */
	@Test
	public void testRandomizeStartTimes() {
		Job job = new Job();
		for (int percent = 10; percent <= 10; percent += 10) {
			System.out.println("Testing - "+percent);
			job.setRandomPercent(percent);
			
			
			Instant now     = Instant.now();
			Instant nextRun = now.plus(24, ChronoUnit.HOURS);
			job.setNextRun(Timestamp.from(now));
			System.out.println(nextRun);
			
			int minMinutes = 1440 - (int) ( Math.min(job.getRandomPercent(), 50)/100.0 * 1440);
			int maxMinutes = 1440 + (int) ( job.getRandomPercent()/100.0 * 1440);
			
			for (int i=0;i<1000;i++) {
				Instant randomTime = job.randomizeNextRun(nextRun, 1440 * 60 *1000,false);
				long diff = ChronoUnit.MINUTES.between(now,randomTime);
				System.out.println(diff);
				org.testng.Assert.assertTrue( diff >= minMinutes/2, "difference time to low");
				org.testng.Assert.assertTrue( diff <= maxMinutes*2, "difference time to high");
	
			}
		}
		
	}
	
}
