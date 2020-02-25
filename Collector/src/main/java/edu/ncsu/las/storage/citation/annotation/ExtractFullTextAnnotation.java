package edu.ncsu.las.storage.citation.annotation;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.storage.citation.AcademicPDFUtilities;
import edu.ncsu.las.storage.citation.PubMedProcessor;

public class ExtractFullTextAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};
	
	public String getName() {	return "Full-text Extraction";   	}
	public String getCode() {	return  "text"; 	}
	public String getDescription() { return "Extracts the full text from either the HTML file or the PDF file.  Precedence given to the PDF File"; }
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
				String text = TikaUtilities.extractText(pdfFile);
				text = AcademicPDFUtilities.cleanTextFromLiteraturePDF(text);
				record.put(this.getCode(), text);
				logger.log(Level.INFO, "Extracted fulltext from: "+pdfFileName);
			}
			catch(Exception e) {
				logger.log(Level.WARNING, "Unable to extract fulltext: "+pdfFileName, e);
			}
		}

		
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new ExtractFullTextAnnotation()).doProcessing(record,"",pdfFileLocation);

		String text = record.getString("text");
		String lines[] = text.split("\n");
		for (String line: lines) {
			System.out.println(line);
		}
	}


	
}
