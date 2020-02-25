package edu.ncsu.las.annotator;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.MimeType;

/**
 * Puts the "meta" tags for an HTML document into a hashmap.
 * Duplicate entires have an "_" appended.
 *
 */
public class HTMLOpenGraphAnnotator extends Annotator {

	@Override
	public String getName() {
		return "Open Graph";
	}	
	
	@Override
	public String getCode() {
		return "open_graph";
	}

	
	@Override
	public String getDescription() {
		return "Parses the open graph tags from an HTML document and put them into a JSON document";
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
		return 30;
	}


	@Override
	public void process(Document doc) {
		if (doc.getMimeType().equals(MimeType.TEXT_HTML)) {
			String htmlText = new String(doc.getContentData(), StandardCharsets.UTF_8);	
			
			JSONObject result = new JSONObject();
			
			org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlText);
			Elements metaTags = jsoupDoc.select("meta");
			
			String currentProperty = "";
			Object currentObject = null;
			for (Element e: metaTags) {
				if (!e.attr("property").startsWith("og:")) { continue; }  // we only care about openGraph data
				String property = e.attr("property");
				String content  = e.attr("content");
	
				if (content.equals("")) {
					continue; // nothing to store.  Assume error
				}				
				
				if (currentObject != null) {
					//test if we are still on the same object.  If not, put the object in the result and reset.
					if (property.startsWith(currentProperty) && !property.equals(currentProperty)) {
						//same object, have an attribute
						
						if (currentObject instanceof String) {
							String value = currentObject.toString();
							currentObject = new JSONObject();
							((JSONObject) currentObject).put("content",value);
						}
						String key = property.substring(currentProperty.length()+1);
						((JSONObject) currentObject).put(key,content);
					}
					else {
						result.append(currentProperty, currentObject);
						currentProperty = property;
						currentObject   = content; 
					}
				}
				else {
					currentProperty = property;
					currentObject   = content; 
				}
			}
			if (currentObject != null && !currentProperty.equals("")) {
				result.append(currentProperty, currentObject);
			}	
			doc.addAnnotation(this.getCode(),result);
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
