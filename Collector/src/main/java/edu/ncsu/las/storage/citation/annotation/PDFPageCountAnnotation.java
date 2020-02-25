package edu.ncsu.las.storage.citation.annotation;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.storage.citation.AcademicPDFUtilities;
import edu.ncsu.las.storage.citation.PubMedProcessor;

public class PDFPageCountAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};
	
	public String getName() {	return "PDF Page Count Extraction";   	}
	public String getCode() {	return  "pdfPageCount"; 	}
	public String getDescription() { return "Extracts the number of pages in a PDF file."; }
	public int getOrder() {	return 20; 	}
	public String[] getRequiredAnnotations() { return EMPTY_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		File pdfFile = new File(pdfFileName);
		
		if ( pdfFile.exists()  ) {
			try {				
				record.put(this.getCode(),  AcademicPDFUtilities.numPagesInPDF(pdfFile));
				logger.log(Level.INFO, "Extracted page count: "+pdfFileName);
			}
			catch(Exception e) {
				logger.log(Level.WARNING, "Unable to page count: "+pdfFileName, e);
			}
		}

		
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new PDFPageCountAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new PDFPageCountAnnotation()).getCode()));
	}


	
}
