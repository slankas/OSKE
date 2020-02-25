package edu.ncsu.las.model.collector;


import java.net.MalformedURLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import edu.ncsu.las.persist.collector.StructuralExtractionDAO;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.json.JSONUtilities;

/**
 * 
 * 
 * extractBy: text, html, text:regex, html:regex
 * 
 *
 */
public class StructuralExtractionRecord {
	private static Logger logger = Logger.getLogger(StructuralExtractionRecord.class.getName());
	
	private UUID _id;
	private String _domainInstanceName;
	private String _hostname;
	private String _pathregex;
	private String _recordName;
	private String _recordSelector;
	private String _recordExtractBy;
	private String _recordExtractRegex;
	private UUID   _recordParentID;
	private String _userEmailID;
	private java.sql.Timestamp _lastDatabaseChange;

	private Pattern _pathRegexPattern;
	private Pattern _extractRegexPattern;
	
	private java.util.ArrayList<StructuralExtractionRecord> _childRecords = new java.util.ArrayList<StructuralExtractionRecord>();
	
	public static final String EXTRACT_BY_TEXT = "text";
	public static final String EXTRACT_BY_HTML = "html";
	public static final String EXTRACT_BY_TEXT_REGEX = "text:regex";
	public static final String EXTRACT_BY_HTML_REGEX = "html:regex";
	
	public StructuralExtractionRecord() {
		
	}
	
	public StructuralExtractionRecord(UUID id,String domainInstanceName,  String hostname, String pathregex, String recordName, String recordSelector, String recordExtractBy, String recordExtractRegex, UUID recordParentID, String userEmailID, java.sql.Timestamp lastDatabaseChange){
		_id                  = id;
		_domainInstanceName = domainInstanceName;
		_hostname           = hostname;
		_pathregex          = pathregex;
		_recordName         = recordName;
		_recordSelector     = recordSelector;
		_recordExtractBy    = recordExtractBy;
		_recordExtractRegex = recordExtractRegex;
		_recordParentID     = recordParentID;
		_userEmailID        = userEmailID;
		_lastDatabaseChange = lastDatabaseChange;
	}
	
	public StructuralExtractionRecord(JSONObject o) {
		this.setId(UUID.fromString(o.getString("id")));
		this.setDomainInstanceName(o.getString("domainInstanceName"));
	    this.setHostname(o.getString("hostname"));
	    this.setPathRegex(o.getString("pathRegex"));
	    this.setRecordName(o.getString("recordName"));
	    this.setRecordSelector(o.getString("recordSelector"));
	    this.setRecordExtractBy(o.getString("recordExtractBy"));
	    this.setRecordExtractRegex(o.getString("recordExtractRegex"));
	    
	    try {
	    	this.setRecordParentID(UUID.fromString(o.getString("recordParentID")));
	    }
	    catch (IllegalArgumentException ex) {
	    	this.setRecordParentID(null);
	    }
	    this.setUserEmailID(o.getString("userEmailID"));
	}
	

	public UUID getId() {
		return _id;
	}

	public void setId(UUID id) {
		_id = id;
	}

	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		_domainInstanceName = domainInstanceName;
	}

	public String getHostname() {
		return _hostname;
	}

	public void setHostname(String hostname) {
		_hostname = hostname;
	}
	
	public String getPathRegex() {
		return _pathregex;
	}
	public void setPathRegex(String newValue) {
		_pathregex = newValue;
		_pathRegexPattern = null;
	}
	
	public Pattern getPathRegexPattern() {
		if (_pathRegexPattern == null) {
			if (_pathregex.trim().equals("") == false) {
				_pathRegexPattern = Pattern.compile(_pathregex);
			}
		}
		return _pathRegexPattern;
	}
	

	public String getRecordName() {
		return _recordName;
	}

	public void setRecordName(String recordName) {
		_recordName = recordName;
	}

	public String getRecordSelector() {
		return _recordSelector;
	}

	public void setRecordSelector(String recordSelector) {
		_recordSelector = recordSelector;
	}

	public String getRecordExtractBy() {
		return _recordExtractBy;
	}

	public void setRecordExtractBy(String recordExtractBy) {
		this._recordExtractBy = recordExtractBy;
	}

	public String getRecordExtractRegex() {
		return _recordExtractRegex;
	}

	public void setRecordExtractRegex(String recordExtractRegex) {
		_recordExtractRegex = recordExtractRegex;
		_extractRegexPattern = null;
	}
	
	public Pattern getExtractRegexPattern() {
		if (_extractRegexPattern == null) {
			if (_recordExtractRegex.trim().equals("") == false) {
				_extractRegexPattern = Pattern.compile(_recordExtractRegex);
			}
		}
		return _extractRegexPattern;
	}	
	
	public UUID getRecordParentID() {
		return _recordParentID;
	}

	public void setRecordParentID(UUID recrodParentID) {
		_recordParentID = recrodParentID;
	}

	public String getUserEmailID() {
		return _userEmailID;
	}

	public void setUserEmailID(String userEmailID) {
		_userEmailID = userEmailID;
	}

	public java.sql.Timestamp getLastDatabaseChangeTS() {
		return _lastDatabaseChange;
	}
 
	public Instant getLastDatababaseChangeInstant() {
		return _lastDatabaseChange.toInstant();
	}
	
	public void setLastDatabaseChangeTS(java.sql.Timestamp lastDatabaseChange) {
		_lastDatabaseChange = lastDatabaseChange;
	}
	
	public void addChild(StructuralExtractionRecord child) {
		_childRecords.add(child);
	}
	
	public int getNumberChildRecords() {
		return _childRecords.size();
	}

	public java.util.ArrayList<StructuralExtractionRecord> getChildRecords() {
		return _childRecords;
	}
		
	public java.util.List<String> validate() {
		java.util.ArrayList<String> result = new java.util.ArrayList<String>();
		
		if (this.getHostname().trim().length() == 0) {
			result.add("Hostname must have a value");
		}

		if (this.getRecordName().trim().length() == 0) {
			result.add("Recordname must have a value");
		}

		if (this.getRecordSelector().trim().length() == 0) {
			result.add("the record selector must have a value");
		}

		if (this.getPathRegex() != null && this.getPathRegex().trim().length() > 0) {
			try {
				Pattern.compile(this.getPathRegex());
			}
			catch (Exception e) {
				result.add("Invalid path regular expression: "+this.getPathRegex());
			}
		}
		
		if ( this.getRecordExtractBy().equals(EXTRACT_BY_TEXT) == false &&
			 this.getRecordExtractBy().equals(EXTRACT_BY_HTML) == false &&
			 this.getRecordExtractBy().equals(EXTRACT_BY_TEXT_REGEX) == false &&
			 this.getRecordExtractBy().equals(EXTRACT_BY_HTML_REGEX) == false) {
			result.add("Invalid value for extract by.  Must be - text,html,text:regex,html:regex");
		}
		if (this.getRecordExtractBy().equals(EXTRACT_BY_TEXT_REGEX) ||  this.getRecordExtractBy().equals(EXTRACT_BY_HTML_REGEX) ) {
			if (this.getRecordExtractRegex() == null || this.getRecordExtractRegex().trim().length() == 0) {
				result.add("ExtractRegex must have a value.");
			}
			else {
				try {
					Pattern.compile(this.getRecordExtractRegex());
				}
				catch (Exception e) {
					result.add("Invalid path regular expression for extract regex: "+this.getRecordExtractRegex());
				}				
			}
		}
				
		
		return result;
	}
	
	
	
	public boolean store() {
		_lastDatabaseChange = new java.sql.Timestamp(System.currentTimeMillis());
		return (new StructuralExtractionDAO()).insert(this);
	}
	
	public boolean update() {
		_lastDatabaseChange = new java.sql.Timestamp(System.currentTimeMillis());
		return (new StructuralExtractionDAO()).update(this);
	}
	
	public boolean delete() {
		return ((new StructuralExtractionDAO()).delete(this.getId()) == 1);
	}
	
	public String toString() {
		return _hostname+":"+_pathregex+":"+_recordName+":"+_recordSelector;
	}
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.put("id", _id)
		      .put("domainInstanceName", _domainInstanceName)
		      .put("hostname", _hostname)
		      .put("pathRegex", _pathregex)
		      .put("recordName", _recordName)
		      .put("recordSelector", _recordSelector)
		      .put("recordExtractBy", _recordExtractBy)
		      .put("recordExtractRegex", _recordExtractRegex)
		      .put("recordParentID", _recordParentID)
		      .put("userEmailID", _userEmailID)
		      .put("lastDatabaseChange", DateUtilities.getDateTimeISODateTimeFormat(_lastDatabaseChange.toInstant()));
	
		return result;
	}
	
	
	public static List<StructuralExtractionRecord> getRecordsForDomain(String domainInstanceName) {
		StructuralExtractionDAO ced = new StructuralExtractionDAO();
		return ced.selectAll(domainInstanceName);
	}
	
	public static List<StructuralExtractionRecord> getRecordsForAnnotation(String domainInstanceName) {
		return getRecordsForAnnotation(getRecordsForDomain(domainInstanceName));
	}
	
	public static List<StructuralExtractionRecord> getRecordsForAnnotation(List<StructuralExtractionRecord> initialList) {	
		List<StructuralExtractionRecord> resultList = initialList.stream().filter(e -> e.getRecordParentID() == null).collect(Collectors.toList());  //create list and hashmap of just the parents
		HashMap<UUID, StructuralExtractionRecord> records = new HashMap<UUID, StructuralExtractionRecord>();
		resultList.stream().forEach(e-> records.put(e.getId(), e));
		initialList.stream().filter(e -> e.getRecordParentID() != null).forEach( e-> { records.get(e.getRecordParentID()).addChild(e);}   );  // add the child records to their respective parents
		
		return resultList;
	}	

	public static StructuralExtractionRecord retrieve(UUID id) {
		StructuralExtractionDAO ced = new StructuralExtractionDAO();
		return ced.retrieve(id);
	}

	
	public static JSONObject annotateForStructuralExtraction(InternetUtilities.HttpContent content, List<StructuralExtractionRecord> cerList, boolean produceLog) throws MalformedURLException {
		return annotateForStructuralExtraction(content.jsoupDocument,content.url,cerList,produceLog);
	}
	
	public static JSONObject annotateForStructuralExtraction(String url, String htmlDocument, List<StructuralExtractionRecord> cerList, boolean produceLog) throws MalformedURLException {
		return annotateForStructuralExtraction(Jsoup.parse(htmlDocument,url),url, cerList,produceLog);
	}

	/**
	 * 
	 * @param e
	 * @return null if regex not found or bad extract by value. 
	 */
	public String extractValue(org.jsoup.nodes.Element e) {
		switch(this.getRecordExtractBy()) {
		case EXTRACT_BY_TEXT : return e.text();
		case EXTRACT_BY_HTML : return e.outerHtml();
		case EXTRACT_BY_TEXT_REGEX : Matcher m = this.getExtractRegexPattern().matcher(e.text());
		                             if (m.find()) {
		                            	 if (m.groupCount() > 0) {
		                            		 return m.group(1);
		                            	 }
		                            	 else {
		                            		 return m.group();
		                            	 }
		                            	 
		                             }
		                             return null;
		case EXTRACT_BY_HTML_REGEX : Matcher m2 = this.getExtractRegexPattern().matcher(e.outerHtml());
							         if (m2.find()) {
							       	   if (m2.groupCount() > 0) {
							       		 return m2.group(1);
							           }
							       	   else {
							       		 return m2.group();
							       	   }
							       	 
							         }
							         return null;
		default: logger.log(Level.WARNING, "Invalid extract by value: "+this.getRecordExtractBy());
		         return null;
		}
		
		
	}
	
	
	/**
	 * Give the list of StructuralExtractRecords, produces the extracted data
	 * 
	 * @param jsoupDocument
	 * @param urlString
	 * @param cerList
	 * @param produceLog if set to true, will produce a log show the steps ...
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public static JSONObject annotateForStructuralExtraction(org.jsoup.nodes.Document jsoupDocument, String urlString, List<StructuralExtractionRecord> cerList, boolean produceLog) throws MalformedURLException {
		logger.log(Level.FINEST, "Extracting structural content");
		
		final java.util.ArrayList<String>  logMessages = new java.util.ArrayList<String>();
		
		JSONObject foundContent = new JSONObject();
		
		if (produceLog) { logMessages.add("extracting content: "+urlString); }
		
		java.net.URL url = null;
		String host = null;
		if (urlString.length() >0 ) {
			url = new java.net.URL(urlString);
			host = url.getHost().toLowerCase();
		}
		
		for (StructuralExtractionRecord cer: cerList) {
			// check whether the host and path match.  if not, skip this record
			if (url !=null && host.endsWith(cer.getHostname().toLowerCase()) == false) {
				if (produceLog) { logMessages.add("skipping ContentExtraction Record(hostname) - "+ cer); }
				continue;
			}
			if (url !=null && cer.getPathRegexPattern() != null) {
				if (cer.getPathRegexPattern().matcher(url.getPath()).matches() == false) {
					if (produceLog) { logMessages.add("skipping ContentExtraction Record(pathRegex) - "+ cer);	}
					continue;
				}
			}
			
			JSONArray localResult = new JSONArray();
			Elements items = jsoupDocument.select(cer.getRecordSelector());
			if (produceLog) { logMessages.add("Testing - "+ cer); logMessages.add("    found "+items.size()+" items");}
			for (org.jsoup.nodes.Element e: items) {
				if (produceLog) { logMessages.add("    examining "+e.toString());}	
				if (cer.getNumberChildRecords() == 0) {
					String value = cer.extractValue(e);
					if (produceLog) { logMessages.add("    no children, found value: "+value);}
					if (value != null) {
						localResult.put(value);
					}
				}
				else {
					JSONObject childResult = new JSONObject();
					if (produceLog) { logMessages.add("    children records present");}
					for (StructuralExtractionRecord child: cer.getChildRecords()) {
						JSONArray localChildResult = new JSONArray();
						Elements childItems = e.select(child.getRecordSelector());
						if (produceLog) { logMessages.add("Testing - "+ child); logMessages.add("    found "+childItems.size()+" items");}
						childItems.stream().forEach(ci -> { 
							if (produceLog) { logMessages.add("    examining child item - "+ci.toString());}
							String value = child.extractValue(ci);
							if (produceLog) { logMessages.add("    child record, found value: "+ value);}
							if (value != null) {localChildResult.put(value);} 
						} );
					
						if (localChildResult.length() > 0) {
							childResult.put(child.getRecordName(), localChildResult);
						}
					}
					
					if (childResult.length() >0) { localResult.put(childResult); }
				}
			}

			if (localResult.length() > 0) {
				foundContent.put(cer.getRecordName(), localResult);
			}
		}
		
		if (produceLog) {
			foundContent.put("_logMessages", JSONUtilities.toJSONArrayAsString(logMessages));
		}
		
		return foundContent;
	}			

	
}