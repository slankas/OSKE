package edu.ncsu.las.annotator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.util.DateUtilities;


/**
 * Checks if there are HTTP headers for current document.
 * If so, converts them into a hash map.
 * 
 */
public class DatePublishedAnnotator extends Annotator {

	@Override
	public String getName() {
		return "Published Date";
	}	
	
	@Override
	public String getCode() {
		return "published_date";
	}

	
	@Override
	public String getDescription() {
		return "Analyzes extracted date fields in the json record to determin when the content was published.  Future work should look to add regular expressions to see if it can be found within the text";
	}

	@Override
	public String getContentType() {
		return "";
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.PRE_DOCUMENT;
	}

	@Override
	public int getPriority() {
		return 45;
	}

	@Override
	public void process(Document doc) {
		JSONObject docObject = doc.createJSONDocument();
		
    	JSONObject headerDate         = getHeaderDate(docObject);
    	JSONObject headerLastModified = getLastModifiedDate(docObject);
    	JSONObject metaDate           = getMetaDate(docObject);
    	JSONObject structuredDate     = getStructuredDate(docObject);
    	JSONObject fileModificationDt = getFileModificationDate(doc);
    	
    	if      (structuredDate     != null) { doc.addAnnotation(this.getCode(), structuredDate     ); }
    	else if (metaDate           != null) { doc.addAnnotation(this.getCode(), metaDate           ); }
    	else if (headerLastModified != null) { doc.addAnnotation(this.getCode(), headerLastModified ); }
    	else if (headerDate         != null) { doc.addAnnotation(this.getCode(), headerDate         ); }
    	else if (fileModificationDt != null) { doc.addAnnotation(this.getCode(), fileModificationDt ); }
    	else                                 { doc.addAnnotation(this.getCode(), new JSONObject().put("date",ensureDateFieldInISODateTimeFormat(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)))
    			                                                                                 .put("source","crawlSource")
    			                                                                                 .put("field", "system time"));   }
	}

	private static String ensureDateFieldInISODateTimeFormat(String dateValue) {
		try { 
	    	ZonedDateTime zdt = DateUtilities.getFromString(dateValue);
			
			zdt = zdt.withZoneSameInstant( ZoneId.of("UTC"));
			String publishDate = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
			if (publishDate.endsWith("[UTC]")) {
				publishDate = publishDate.substring(0, publishDate.indexOf("[UTC]"));
			}
			return publishDate;
		}
		catch (Exception e) { // ignore error, return null and let caller handle ..
			return null;
		}
	}
	
	private static String[] META_FIELDS = {	"DC_date_modified","article:modified_time","DC_date_created","article:published_time"};
	
	//private static String[] OG_FIELDS = {			"og:updated_time"	};  //TOD: Need to add a check for this
	
	private static String[] STRUCTURED_FIELDS = {	"article:modified_time","dateModified",	"og:updated_time","article:published_time","dateCreated",	"datePublished","uploadDate"};

	private static JSONObject getStructuredDate(JSONObject jo) {
		try {
			if (jo.has("structured_data") == false || jo.getJSONObject("structured_data").has("items") == false) {
				return null;
			}
			JSONArray itemsArray = jo.getJSONObject("structured_data").getJSONArray("items");
			
			for (int i=0;i<itemsArray.length();i++) {
				
				if (!itemsArray.getJSONObject(i).has("properties")) {continue;}
				
				JSONObject propObject = itemsArray.getJSONObject(i).getJSONObject("properties");
				for (String key: STRUCTURED_FIELDS) {
					if (propObject.has(key)) {
						String value =  propObject.getJSONArray(key).getString(0);
						String dateValue = ensureDateFieldInISODateTimeFormat(value);
						if (dateValue != null) {
							JSONObject result = new JSONObject().put("date", dateValue)
                                                                .put("source","structured_data")
                                                                .put("field", key);
                            return result;
						}
					}
				}
			}
		}
		catch (Exception e) {
			System.err.println("");
			System.err.println(e);
			System.err.println(jo.toString(4));
		}
		
		return null;
	}	
	
	
	private static JSONObject getMetaDate(JSONObject jo) {
		
		if (jo.has("html_meta") == false) { return null;}
		
		try {
			JSONObject metaObject = jo.getJSONObject("html_meta");
			
			for (String key: META_FIELDS) {
				if (metaObject.has(key)) {
					String value =  metaObject.getJSONArray(key).getString(0);
					String dateValue = ensureDateFieldInISODateTimeFormat(value);
					if (dateValue != null) {
						JSONObject result = new JSONObject().put("date", dateValue)
                                                            .put("source","metadata")
                                                            .put("field", key);
                        return result;
					}
				}
			}
		}
		catch (Exception e) {
			System.err.println(e);
		}
		
		return null;
	}
	

	private static JSONObject getLastModifiedDate(JSONObject jo) {
				
		try {
			JSONObject headerObject = jo.getJSONObject("http_headers");
			
			if (headerObject.has("Last-Modified")) {
				String lastModifiedValue = headerObject.getString("Last-Modified");
				String dateValue = ensureDateFieldInISODateTimeFormat(lastModifiedValue);
				if (dateValue != null) {
					JSONObject result = new JSONObject().put("date", dateValue)
                                                        .put("source","header")
                                                        .put("field", "last-modified");
                    return result;
				}
			}
			else if (headerObject.has("last-modified")) {
				String lastModifiedValue = headerObject.getString("last-modified");
				String dateValue = ensureDateFieldInISODateTimeFormat(lastModifiedValue);
				if (dateValue != null) {
					JSONObject result = new JSONObject().put("date", dateValue)
                                                        .put("source","header")
                                                        .put("field", "last-modified");
                    return result;
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to get http headers (may be document upload)");
		}
		
		return null;
	}

	private static JSONObject getHeaderDate(JSONObject jo) {
		
		try {
			JSONObject headerObject = jo.getJSONObject("http_headers");
			
			if (headerObject.has("Date")) {
				String lastModifiedValue = headerObject.getString("Date");
				String dateValue = ensureDateFieldInISODateTimeFormat(lastModifiedValue);
				if (dateValue != null) {
					JSONObject result = new JSONObject().put("date", dateValue)
                                                        .put("source","header")
                                                        .put("field", "date");
                    return result;
				}				
			}
			else if (headerObject.has("date")) {
				String lastModifiedValue = headerObject.getString("date");
				String dateValue = ensureDateFieldInISODateTimeFormat(lastModifiedValue);
				if (dateValue != null) {
					JSONObject result = new JSONObject().put("date", dateValue)
                                                        .put("source","header")
                                                        .put("field", "date");
                    return result;
				}				
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to get http headers (may be document upload)");
		}
		
		return null;
	}
	
	private static JSONObject getFileModificationDate(Document doc) {
		long modificationTime = doc.getLastModificationTimeEpochMillis();
		if (modificationTime != -1) {
			Instant i = Instant.ofEpochMilli(modificationTime);
			ZonedDateTime z = ZonedDateTime.ofInstant( i,  ZoneId.of("UTC"));
			String dateValue = ensureDateFieldInISODateTimeFormat(ensureDateFieldInISODateTimeFormat(z.format(DateTimeFormatter.ISO_DATE_TIME)));
			JSONObject result = new JSONObject().put("date", dateValue)
                                                .put("source","header")
                                                .put("field", "date");
            return result;						
		}
		else {
			return null;
		}
	}

	@Override
	public JSONObject getSchema() {
		return null;
	}

}
