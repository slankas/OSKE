package edu.ncsu.las.storage.citation.annotation;


import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.annotator.ConceptAnnotator;

/**
 * Performs geo-tagging on the full-text of a citation record.
 */
public class TextConceptAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "Concept Annotation";   	}
	public String getCode() {	return "concepts" ; 	}
	public String getDescription() { return "Extracts defined concepts from the text"; }
	public int getOrder() {	return 30; 	}
	public String[] getRequiredAnnotations() { return TEXT_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		
		JSONArray concepts = ( new ConceptAnnotator()).annotateConcepts(record.getString("text"), "wolfhuntv2");
		record.put("concepts", concepts);

	}



}
