package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.storage.citation.PubMedProcessor;
import edu.ncsu.las.util.json.JSONUtilities;

public class RecordTitleAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};
	
	public String getName() {	return "HTML Title Annotation";   	}
	public String getCode() {	return  "html_title"; 	}
	public String getDescription() { return "Copies the Article Title into the html_title field"; }
	public int getOrder() {	return 23; 	}
	public String[] getRequiredAnnotations() { return EMPTY_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {

		record.put(this.getCode(), JSONUtilities.getAsString(record, "Article.ArticleTitle", ""));
		logger.log(Level.INFO, "HTML Title field annotated: "+record.getString("PMID"));
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new RecordTitleAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new RecordTitleAnnotation()).getCode()));
	}



	
}
