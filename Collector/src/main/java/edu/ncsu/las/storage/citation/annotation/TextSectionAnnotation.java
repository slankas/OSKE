package edu.ncsu.las.storage.citation.annotation;


import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.storage.citation.AcademicPDFUtilities;
import edu.ncsu.las.storage.citation.AcademicPDFUtilities.DocumentSection;
import edu.ncsu.las.storage.citation.PubMedProcessor;

/**
 * Performs geo-tagging on the full-text of a citation record.
 * 
 */
public class TextSectionAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "Text Section Annotation";   	}
	public String getCode() {	return "sections" ; 	}
	public String getDescription() { return "Marks the start position of sections within a document"; }
	public int getOrder() {	return 27; 	}
	public String[] getRequiredAnnotations() { return TEXT_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		JSONArray result = new JSONArray();
		String text = record.getString("text");
		String lines[] = text.split("\n");
		int position = 0;
		for (String line: lines) {
			if (AcademicPDFUtilities.lineContainsSectionHeader(line)) {
				DocumentSection ds = AcademicPDFUtilities.matchingDocumentSectionToline(line);
				JSONObject section = new JSONObject().put("sectionName",ds.getLabel()).put("startPos", position).put("method", ds.isMethodSection());
				result.put(section);
			}
			position += line.length() + 1; // plus one is for the \n separator that was removed during the split
		}
		
		for (int i=0; i<result.length()-1;i++) {
			result.getJSONObject(i).put("endPos", result.getJSONObject(i+1).getInt("startPos")-1);
		
		}
		if (result.length() >0) {
			result.getJSONObject(result.length()-1).put("endPos", text.length());
		}
		record.put(this.getCode(), result);
		
		
	}


	public static void main(String args[]) throws JSONException, IOException {
		//examineAll(); System.exit(0);
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		recordNumber="22531577";  //  //  //
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf"; // not used
		(new ExtractFullTextAnnotation()).doProcessing(record,"",pdfFileLocation);		// needed to make sure we get the latest version
		System.out.println(record.getString("text"));
		(new TextSectionAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getJSONArray("textSections").toString(4));
	}
	
	public static void examineAll() throws JSONException, IOException {
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			if (f.getName().compareTo("27409665") < 0) { continue; }
			JSONObject record = PubMedProcessor.loadRecord(f);
			String recordNumber = f.getName().substring(0,f.getName().indexOf('.'));
			String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
			
			System.out.println("\n\n"+recordNumber);
			(new ExtractFullTextAnnotation()).doProcessing(record,"",pdfFileLocation);
			if (record.has("text") == false) { continue; }
			(new TextSectionAnnotation()).doProcessing(record,"",pdfFileLocation);
			
			System.out.println(record.getJSONArray("sections").toString(4));
		}
	}	
	
}
