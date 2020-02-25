package edu.ncsu.las.source;


import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.testng.annotations.Test;

public class Forum_VBulletinTest {
	public static String[] sampleDates = { "1 Week Ago", "14 Hours Ago", "1 Hour Ago", "12 Weeks Ago" };
	public static String postHierarchy = "writeLink(9114112, 0, 0, 232538, \"\", \"Cacelled on me as soon as i...\", \"01-24-2017\", \"01:08 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114112#post9114112\");\nwriteLink(9114127, 0, 0, 84103, \"T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"01:54 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114127#post9114127\");\nwriteLink(9114130, 0, 0, 250037, \"I,T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"02:05 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114130#post9114130\");\nwriteLink(9114171, 0, 0, 232538, \"I,I,L\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"05:45 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114171#post9114171\");\nwriteLink(9114172, 0, 0, 103282, \"I,I,1,L\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"05:49 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114172#post9114172\");\nwriteLink(9114325, 0, 0, 0, \"I,I,2,L\", \"\", \"more\", \"\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114325#post9114325\");\nwriteLink(9114168, 0, 0, 232538, \"I,L\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"05:42 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114168#post9114168\");\nwriteLink(9117875, 0, 0, 84103, \"I,1,L\", \"Re: Cacelled on me as soon as...\", \"01-26-2017\", \"03:51 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9117875#post9117875\");\nwriteLink(9119878, 0, 0, 196610, \"I,2,L\", \"Re: Cacelled on me as soon as...\", \"01-27-2017\", \"07:51 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9119878#post9119878\");\nwriteLink(9119913, 0, 0, 0, \"I,3,L\", \"\", \"more\", \"\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9119913#post9119913\");\nwriteLink(9114444, 0, 0, 95667, \"T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"10:35 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114444#post9114444\");\nwriteLink(9114454, 0, 0, 103282, \"I,T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"10:39 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114454#post9114454\");\nwriteLink(9114814, 0, 0, 91492, \"I,I,L\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"02:26 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114814#post9114814\");\nwriteLink(9116141, 0, 0, 232538, \"I,L\", \"Re: Cacelled on me as soon as...\", \"01-25-2017\", \"12:40 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9116141#post9116141\");\nwriteLink(9114840, 0, 0, 206666, \"T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"02:39 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9114840#post9114840\");\nwriteLink(9115141, 0, 0, 177267, \"I,T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"06:01 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9115141#post9115141\");\nwriteLink(9115306, 0, 0, 97654, \"I,I,T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"07:59 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9115306#post9115306\");\nwriteLink(9116156, 0, 0, 232538, \"I,I,L\", \"Re: Cacelled on me as soon as...\", \"01-25-2017\", \"12:48 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9116156#post9116156\");\nwriteLink(9117510, 0, 0, 110168, \"I,I,1,T\", \"Re: Cacelled on me as soon as...\", \"01-26-2017\", \"11:52 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9117510#post9117510\");\nwriteLink(9117595, 0, 0, 177267, \"I,I,1,I,L\", \"Re: Cacelled on me as soon as...\", \"01-26-2017\", \"12:48 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9117595#post9117595\");\nwriteLink(9152536, 0, 0, 241198, \"I,I,1,L\", \"Re: Cacelled on me as soon as...\", \"Yesterday\", \"05:57 AM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9152536#post9152536\");\nwriteLink(9152685, 0, 0, 0, \"I,I,2,L\", \"\", \"more\", \"\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9152685#post9152685\");\nwriteLink(9115303, 0, 0, 105736, \"I,T\", \"Re: Cacelled on me as soon as...\", \"01-24-2017\", \"07:58 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9115303#post9115303\");\nwriteLink(9116153, 0, 0, 232538, \"I,L\", \"Re: Cacelled on me as soon as...\", \"01-25-2017\", \"12:47 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9116153#post9116153\");\nwriteLink(9116313, 0, 0, 95667, \"L\", \"Re: Cacelled on me as soon as...\", \"01-25-2017\", \"02:16 PM\", 1, \"showthread.php?1138341-Cacelled-on-me-as-soon-as-i-got-off-the-exit&p=9116313#post9116313\");\n";

	
	@Test
	public void createHierarchy() {
		//Forum_VBulletin fvb = new Forum_VBulletin();
		//Forum_VBulletin.TreeResult tr = fvb.createTreeFromScriptText(postHierarchy);
		//System.out.println(tr.root.toString(4));

	}
	

	
	@Test
	public void testDates() {
		//ZoneId.of("US/Pacific");
		//for (String date: sampleDates) {
		//	System.out.println(Forum_VBulletin.toISOInstantDate(date, zoneID).toString()+ "   " + date);
		//}
	}
	
	@Test
	public void testDate() {
		String CONFIG_SITE_FULL_DATE_FORMAT = "MMM dd yyyy h:mm a VV";
		String d = "Dec 27 2002 7:04 am US/Pacific";
		DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern(CONFIG_SITE_FULL_DATE_FORMAT);
			
		System.out.println(d);
		System.out.println(fullDateFormatter);
		ZonedDateTime zdt = ZonedDateTime.parse(d, fullDateFormatter);
		System.out.println(zdt.toString());
		
	}
}
