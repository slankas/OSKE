package edu.ncsu.las.annotator;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;


/**
 * Using Tika, attempts to identify the language for the text.  Will set the two-letter abbreviation (ISO-639-1)
 * 
 * Note: using CLD2 is probably the better alternative - https://github.com/CLD2Owners/cld2
 * 
 *
 */
public class LanguageAnnotator extends Annotator {

	@Override
	public String getName() {
		return "Language";
	}	
	
	@Override
	public String getCode() {
		return "language";
	}

	
	@Override
	public String getDescription() {
		return "Identifies the primary language for the given text.";
	}

	@Override
	public String getContentType() {
		return "";
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.POST_DOCUMENT;
	}

	@Override
	public void process(Document doc) {
		if (doc.getReferrer() != null) {
			doc.addAnnotation(this.getCode(),doc.getLanguage());
		}
	}

	@Override
	public JSONObject getSchema() {
		return null;
	}


}
