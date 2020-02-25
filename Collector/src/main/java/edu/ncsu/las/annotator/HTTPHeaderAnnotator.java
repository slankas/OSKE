package edu.ncsu.las.annotator;

import java.util.HashMap;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.util.InternetUtilities;

/**
 * Checks if there are HTTP headers for current document.
 * If so, converts them into a hash map.
 * 
 *
 */
public class HTTPHeaderAnnotator extends Annotator {

	@Override
	public String getName() {
		return "HTTP Headers";
	}	
	
	@Override
	public String getCode() {
		return "http_headers";
	}

	
	@Override
	public String getDescription() {
		return "Puts the HTTP Headers into a hashmp and stores them in the annotations for the current document.";
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
		return 5;
	}


	@Override
	public void process(Document doc) {
		if (doc.getHTTPHeaders() != null) {
			HashMap<String, String> headers = InternetUtilities.convertHeadersToHashMap(doc.getHTTPHeaders());
			
			doc.addAnnotation(this.getCode(),new JSONObject(headers));
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
