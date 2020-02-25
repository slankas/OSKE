package edu.ncsu.las.annotator;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.model.extract.HTMLStructuredDataExtractor;

/**
 * Extract structured data from HTML.  This uses the entire document.
 * (John - I don't think its feasible to extract from portions of a document)
 * 
 */
public class StructuredDataAnnotator extends Annotator {

	@Override
	public String getName() {
		return "Structured Data";
	}	
	
	@Override
	public String getCode() {
		return "structured_data";
	}

	
	@Override
	public String getDescription() {
		return "Extracts structured data from HTML text";
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
		return 35;
	}
	
	
	@Override
	public void process(Document doc) {
		if (doc.getMimeType().equals(MimeType.TEXT_HTML)) {
			String htmlText = new String(doc.getContentData(), StandardCharsets.UTF_8);	
			
			try {
				JSONObject jsonObject = HTMLStructuredDataExtractor.extractAllFormats(htmlText, new java.net.URL(doc.getURL()));
				doc.addAnnotation(this.getCode(),jsonObject);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Unable to extract structured data from HTML document: "+ doc.getURL());
			}
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
