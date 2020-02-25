package edu.ncsu.las.annotator;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.InternetUtilities;

/**
 * Puts the "meta" tags for an HTML document into a hashmap.
 * Duplicate entires have an "_" appended.
 *
 */
public class HTMLMetaAnnotator extends Annotator {
	private static Logger logger =Logger.getLogger(InternetUtilities.class.getName());
	
	@Override
	public String getName() {
		return "HTML Meta Tags";
	}	
	
	@Override
	public String getCode() {
		return "html_meta";
	}

	
	@Override
	public String getDescription() {
		return "Puts any meta tags for the HTML document into a JSONObject, where each element is a JSON array containing strings. Stores that as an annotation";
	}

	@Override
	public String getContentType() {
		return MimeType.TEXT_HTML;
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.PRE_DOCUMENT;
	}
	
	@Override
	public int getPriority() {
		return 10;
	}


	@Override
	public void process(Document doc) {
		if (doc.getMimeType().equals(MimeType.TEXT_HTML)) {
			String htmlText = new String(doc.getContentData(), StandardCharsets.UTF_8);	
			
			JSONObject metaResult = new JSONObject();
			
			org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlText);
			Elements metaTags = jsoupDoc.select("meta");
			for (Element e: metaTags) {
				if (e.attr("property").startsWith("og:")) { continue; }  //ignore OpenGraph data - handled by a different annotator
				if (e.attr("charset").equals("") == false) { continue; } // we don't track the character set
				if (e.attr("itemprop").equals("") == false) { continue; } // we don't track these as they'll be picked up by the StructuredData Extractor
			
				String name = e.attr("name");
				if (name.equals("")) { name = e.attr("http-equiv"); }
				if (name.equals("")) { name = e.attr("property"); }
				String content = e.attr("content");
				content = content.replace("yyyy-MM-dd HH:mm:ss z","");
				
				String storageName = name.replace('.', '_').trim();
				if (storageName.equals("")) {
					logger.log(Level.WARNING,"found empty named meta field, value: "+content);
					continue;
				}
				
				//need to check for date fields.				
				if (DateUtilities.isDateField(storageName)) {
					ZonedDateTime zdt = DateUtilities.getFromString(content);
					if (zdt != null) {
						content = zdt.format(DateTimeFormatter.ISO_INSTANT);
					}
					else {
						logger.log(Level.WARNING,"Not storing html meta date field - field name: " + storageName+", value: "+content);
						continue;
					}
					
					
				}
				
				if (metaResult.has(storageName)) {
					metaResult.getJSONArray(storageName).put(content);
				}
				else {
					metaResult.put(storageName, (new JSONArray()).put(content));
				}
			}
			if (metaResult.has("")) {
				logger.log(Level.WARNING,"Removing html meta date field with empty key, possible value " + metaResult.get(""));
				metaResult.remove("");
			}
			
			doc.addAnnotation(this.getCode(),metaResult);
		}
	}

	@Override
	public JSONObject getSchema() {
		JSONObject ret = new JSONObject();
		
		ret.put("type", "object");
		ret.put("enabled", false);
		
		return ret;
	}


}
