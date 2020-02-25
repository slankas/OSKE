package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.storage.citation.PubMedProcessor;

public class TextLengthAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "Text Length Annotation";   	}
	public String getCode() {	return  "textLength"; 	}
	public String getDescription() { return "records the length of the textField."; }
	public int getOrder() {	return 23; 	}
	public String[] getRequiredAnnotations() { return TEXT_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		record.put(this.getCode(),  record.getString("text").length());
		logger.log(Level.INFO, "Extracted text length: "+record.getString("PMID"));
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new TextLengthAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new TextLengthAnnotation()).getCode()));
	}



	
}
