package edu.ncsu.las.annotator;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;


/**
 * Checks if there are HTTP headers for current document.
 * If so, converts them into a hash map.
 * 
 *
 */
public class HTTPReferrerAnnotator extends Annotator {

	@Override
	public String getName() {
		return "HTTP Referrer";
	}	
	
	@Override
	public String getCode() {
		return "referrer";
	}

	
	@Override
	public String getDescription() {
		return "Puts the http referrer into the anotations.";
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
		return 25;
	}

	
	@Override
	public void process(Document doc) {
		if (doc.getReferrer() != null) {
			doc.addAnnotation(this.getCode(),doc.getReferrer());
		}
	}

	@Override
	public JSONObject getSchema() {
		return null;
	}


}
