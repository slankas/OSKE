package edu.ncsu.las.annotator;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.MimeType;

/**
 * Checks if there a <title> set for the current document.  If so, saves that string as 
 * an annotation for the document.
 * 
 *
 */
public class HTMLTitleAnnotator extends Annotator {

	@Override
	public String getName() {
		return "HTML Title";
	}	
	
	@Override
	public String getCode() {
		return "html_title";
	}

	
	@Override
	public String getDescription() {
		return "Puts the HTML title if it exists into document as an annotation";
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
		return 20;
	}

	
	@Override
	public void process(Document doc) {
		if (doc.getMimeType().equals(MimeType.TEXT_HTML) && doc.hasAnnotation(this.getCode()) == false) {
			String htmlText = new String(doc.getContentData(), StandardCharsets.UTF_8);	
			
			org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlText);
			String title = jsoupDoc.title();
			
			doc.addAnnotation(this.getCode(),title);
		}
	}

	@Override
	public JSONObject getSchema() {
		return null;
	}


}
