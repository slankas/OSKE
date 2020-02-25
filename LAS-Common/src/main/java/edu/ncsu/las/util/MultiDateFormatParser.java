package edu.ncsu.las.util;

/**
 * MultiDateFormatParser is a utility class with static methods to parse strings into Date objects.
 * (The date object is not deprecated, only methods to manipulate it are.)
 *
 * It is necessary to use a library of these classes because the DateFormat and
 * SimpleDateFormat classes are not thread-safe.
 *
 * Version History:
 * ----------------------------------------------------------------
 *
 * 
 */
public class MultiDateFormatParser {
	/** library of parse objects */
	private static java.util.Vector<MultiDateFormatParser> _multiDateFormatParsers = new java.util.Vector<MultiDateFormatParser>();
	
	/** different formats to try to parse */
	private java.text.SimpleDateFormat[] _sdfFormat = { 
		new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
		new java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss"),
		new java.text.SimpleDateFormat("yyyyMMddHHmmss"),
		new java.text.SimpleDateFormat("yyyyMMdd"),
		new java.text.SimpleDateFormat("yyyy'W'wwF"),
		new java.text.SimpleDateFormat("yyyy-'W'ww-F"),
		new java.text.SimpleDateFormat("yyyyDDD"),
		new java.text.SimpleDateFormat("yyyy-DDD"),
		new java.text.SimpleDateFormat("yyyy/MM/dd"),		
		new java.text.SimpleDateFormat("MM/dd/yyyy h:mm:ss aa"),
		new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
		new java.text.SimpleDateFormat("MM/dd/yy"),	
		new java.text.SimpleDateFormat("MM/dd/yyyy"), 
		new java.text.SimpleDateFormat("MM-dd-yy"),
		new java.text.SimpleDateFormat("MM-dd-yyyy"),
		new java.text.SimpleDateFormat("MM.dd.yy"),
		new java.text.SimpleDateFormat("MM.dd.yyyy"),
		new java.text.SimpleDateFormat("MM dd yy"),
		new java.text.SimpleDateFormat("MM dd yyyy"),
		
		new java.text.SimpleDateFormat("dd/MMM/yy"), 
		new java.text.SimpleDateFormat("dd/MMM/yyyy"),
		new java.text.SimpleDateFormat("dd-MMM-yy"),
		new java.text.SimpleDateFormat("dd-MMM-yyyy"),
		new java.text.SimpleDateFormat("dd.MMM.yy"),
		new java.text.SimpleDateFormat("dd.MMM.yyyy"),
		new java.text.SimpleDateFormat("dd MMM yy"),
		new java.text.SimpleDateFormat("dd MMM yyyy"),
															   
		new java.text.SimpleDateFormat("MMM-dd-yy"),
		new java.text.SimpleDateFormat("MMM-dd-yyyy"),
		new java.text.SimpleDateFormat("MMM/dd/yy"),
		new java.text.SimpleDateFormat("MMM/dd/yyyy"),
		new java.text.SimpleDateFormat("MMM.dd.yy"),
		new java.text.SimpleDateFormat("MMM.dd.yyyy"),
		new java.text.SimpleDateFormat("MMM dd yy"),
		new java.text.SimpleDateFormat("MMM dd yyyy"),
		new java.text.SimpleDateFormat("MMM dd,yy"),
		new java.text.SimpleDateFormat("MMM dd,yyyy"),
		new java.text.SimpleDateFormat("MMM dd, yy"),
		new java.text.SimpleDateFormat("MMM dd, yyyy") 
	};

/**
 * MultiDateFormatParser constructor comment.
 */
private MultiDateFormatParser() {
	for (int i=0;i<_sdfFormat.length;i++) {
		_sdfFormat[i].setLenient(false);
	}
}
/**
 * Get a crypt object from the available pool.  If none available, create a new one,
 *
 * @returns com.fub.its.util.Crypt for use.
 */
private static MultiDateFormatParser getMultiDateFormatParser() {

	synchronized (_multiDateFormatParsers) {
		if (_multiDateFormatParsers.size() >0) {
			MultiDateFormatParser mdfp = _multiDateFormatParsers.remove(_multiDateFormatParsers.size()-1);
			return mdfp;
		}
	}
	return new MultiDateFormatParser();
}
public static java.util.Date getValidDate(String dateString) {
	java.util.Date result = null;

	MultiDateFormatParser mdfp = MultiDateFormatParser.getMultiDateFormatParser();
	result = mdfp.getValidDateInternal(dateString);
	MultiDateFormatParser.returnMultiDateFormatParser(mdfp);
	
	return result;

}
private java.util.Date getValidDateInternal(String dateString) {
	java.util.Date result = null;
	
	
	for (int i=0;i<_sdfFormat.length;i++){
		try {
			result = _sdfFormat[i].parse(dateString);
		}
		catch (Exception e){
			; // no action required.  if the string is not valid, it tries the next format until one is found.
			  // if none are found, null is returned.
		}
		if (result != null) {
			break;
		}
	}

	return result;

}
/**
 * Return a crypt object back to the pool
 *
 * @param c com.fub.its.util.Crypt
 */
private static void returnMultiDateFormatParser(MultiDateFormatParser mdfp) {
	_multiDateFormatParsers.addElement(mdfp);
}
}
