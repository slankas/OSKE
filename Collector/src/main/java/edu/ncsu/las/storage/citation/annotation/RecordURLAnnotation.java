package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.storage.citation.PubMedProcessor;

public class RecordURLAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};
	
	public String getName() {	return "URL Annotation";   	}
	public String getCode() {	return  "url"; 	}
	public String getDescription() { return "creates a URL for the pub-med records"; }
	public int getOrder() {	return 23; 	}
	public String[] getRequiredAnnotations() { return EMPTY_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		String pmid = record.getString("PMID");
		
		record.put(this.getCode(),"https://www.ncbi.nlm.nih.gov/pubmed/"+pmid);
		logger.log(Level.INFO, "Extracted added URL: "+record.getString("PMID"));
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new RecordURLAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new RecordURLAnnotation()).getCode()));
	}



	
}
