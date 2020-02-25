package edu.ncsu.las.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class DateUtilities {
	private static Logger logger =Logger.getLogger(InternetUtilities.class.getName());
	
	private static ArrayList<java.time.format.DateTimeFormatter> knownPatterns = new ArrayList<java.time.format.DateTimeFormatter>();
	
	static {   //probably duplication exists, especially giving the formatters at the bottom
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd' 'HH:mm:ss"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm.ssX"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm.ss.SSSX"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd' 'HH':'mm':'ss"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd' 'HH':'mm':'ss z"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
		knownPatterns.add(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);  //	'Tue, 3 Jun 2008 11:05:30 GMT'
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'Z"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));		
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss ZZZ yyyy",Locale.ENGLISH)); 
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("EEE, yyyy-MM-dd HH:mm"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("EEE, yyyy-MM-dd HH:mm:ss"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-d"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy"));
		knownPatterns.add(
	            new java.time.format.DateTimeFormatterBuilder().appendPattern("dd/MM/yyyy[ [HH][:mm][:ss][.SSS]][X]")
	            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
	            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
	            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
	            .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
	            .toFormatter()); 
		//"2016-10-05T11:44:29.713-04:00[America/New_York]"
		knownPatterns.add(
	            new java.time.format.DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd['T'[HH][:mm][:ss][.SSS]][X]['['VV']']")
	            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
	            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
	            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
	            .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
	            .toFormatter()); 
		
		knownPatterns.add(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME);
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss xxxx zzz")); // alternate version of RFC1123 without leading date
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss zzzZZZ (zzz)")); // alternate version of RFC1123 minus comma after day of week
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("M/dd/yyyy h:mm:ss a"));  //"9/23/2018 9:08:47 PM"
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z"));  //"Aug 14, 2018 3:00 AM PT"
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));  //"Aug 14, 2018 3:00 AM"
		knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a"));
	}

	/**
	 * 
	 * @param text
	 * @return
	 */
	public static ZonedDateTime getFromString(String text) {
		return getFromString(text,true);
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	private static ZonedDateTime getFromString(String text, boolean checkInternetFormat) {
		ZonedDateTime result = null;

		for (java.time.format.DateTimeFormatter pattern : knownPatterns) {
		    try {
		    	result = ZonedDateTime.parse(text, pattern);
		    	break; // we were able to successfully parse the pattern.
		    } catch (DateTimeParseException  pe) {
		    	//System.out.println(pattern.toString()+":"+pe);
		        // Loop on
		    }
		}		
		
		if (result == null) {
			for (java.time.format.DateTimeFormatter pattern : knownPatterns) {
			    try {
			    	result = LocalDateTime.parse(text, pattern).atZone(ZoneOffset.UTC);
			    	break;
			    } catch (DateTimeParseException  pe) {
			    	//System.out.println(pe);
			        // Loop on
			    }
			}
		}
		
		if (result == null) {
			for (java.time.format.DateTimeFormatter pattern : knownPatterns) {
			    try {
					TemporalAccessor ta = pattern.parse(text);
					result = LocalDate.from(ta).atStartOfDay().atZone(ZoneId.systemDefault());
			    	break;
			    } catch (Exception  pe) {
			    	//System.out.println(pattern.toString()+":"+pe);
			        // Loop on
			    }
			}
		}
		
		
		if (result != null) {  //Convert to UTC/Zulu/GMT Timezone if necessary
			return result.withZoneSameInstant(ZoneOffset.UTC);
		}
		
		try {
			if (checkInternetFormat) {
				String isoString = InternetUtilities.convertHTTPBasedDateStringToISO(text,false);
				return getFromString(isoString, false);
			}
		}
		catch (Throwable t) {
			//System.err.println(t);
			; //ignore any issue;
		}
		
		
		logger.log(Level.WARNING,"No known Date format found: " + text);
		return null;
	}

	/**
	 * Does the field name specify a date field? Looks for the presence of "date", "time", and "last-modified"
	 * 
	 * @param name
	 * @return true / false
	 */
	public static boolean isDateField(String name) {
		name = name.toLowerCase();
		return ( name.contains("validate") == false)  &&
			   ( name.contains("date") || name.contains("time") || name.contains("last-modified")  );
	}
	
	
	public static String getCurrentDateTimeISODateTimeFormat() {
    	ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS);
		String resultDate = zdt.format(DateTimeFormatter.ISO_INSTANT);
		return resultDate;
	}
	
	public static String getDateTimeISODateTimeFormat(Instant i) {
		String resultDate = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(i.truncatedTo(ChronoUnit.SECONDS));
		return resultDate;
	}
	
	
	/** 
	 * Returns the number of days in particular month, based upon the based in year
	 * @param month 1-12
	 * @param year
	 * @return
	 */
	public static int numDaysInMonth(int month, int year) {
		switch (month) {
	        case 2 : return ( (year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0) ? 29 : 28;
	        case 9 : 
	        case 4 : 
	        case 6 : 
	        case 11 : return 30;
	        default : return 31;
		}
	}
	
	public static boolean isValid(int d, int m, int y) {
		return m > 0 && m <= 12 && d > 0 && d <= numDaysInMonth(m, y);
	}	
	
	
	
	
	
	public static void main(String[] args) {
		/*
		System.out.println(numDaysInMonth(2, 2000));
		System.out.println(numDaysInMonth(2, 2001));
		System.out.println(numDaysInMonth(2, 2004));
		System.out.println(numDaysInMonth(2, 2100));

		
		
		java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
		System.out.println(ts);
//		 DateUtilities.getDateTimeISODateTimeFormat(_statusTimestamp.toInstant(Z)
	
		System.out.println(getDateTimeISODateTimeFormat(Instant.now()));
		System.out.println(getCurrentDateTimeISODateTimeFormat());
		
		
		java.time.format.DateTimeFormatter alternateRFC1123 = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss xxxx zzz");
		//System.out.println(ZonedDateTime.parse("22 Jul 2017 04:25:33 +0100 GMT", alternateRFC1123));

		System.out.println(ZonedDateTime.parse("25 Apr 2017 13:03:00 -0400 GMT", alternateRFC1123).toInstant().toString());
		
		ZonedDateTime zdt = ZonedDateTime.now();
		System.out.println(zdt.format(alternateRFC1123));
		System.out.println(zdt.toInstant().toString());
		*/
		
		//System.out.println(getFromString("Feb. 12, 2004 GMT"));
		
		//knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern();  //""
		//knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z"));  //"Aug 14, 2018 3:00 AM PT"
		//knownPatterns.add(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));  //"Aug 14, 2018 3:00 AM"
		
		java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a");
		System.out.println(ZonedDateTime.parse("Sep 21, 2018, 8:52 AM", dtf));
	}

}
