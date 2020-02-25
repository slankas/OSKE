package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.storage.citation.PubMedProcessor;


public class AuthorFullNameAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};
	
	public String getName() {	return "Author Full Name";   	}
	public String getCode() {	return "authorFullName" ; 	}
	public String getDescription() { return "Creates a JSON array at the top level of the author's full names in LastName, FirstName format"; }
	public int getOrder() {	return 23; 	}
	public String[] getRequiredAnnotations() { return EMPTY_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		JSONArray result = new JSONArray();
		if (record.getJSONObject("Article").has("AuthorList")) {
			JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");

			for (int i=0; i < authors.length(); i++) {
				JSONObject author = authors.getJSONObject(i);
				String name = "";
				if (author.has("LastName")) {
					name = author.getString("LastName");
					
					String firstName = author.optString("ForeName", author.optString("Initials","")).trim();
					if (firstName.length() >0) {
						name = name + ", " + firstName;
					}
					result.put(name);
				}
			}				
		}
		
		record.put(this.getCode(),result);

		logger.log(Level.INFO, "Author full name populated: "+record.getString("PMID"));
		
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new AuthorFullNameAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new AuthorFullNameAnnotation()).getCode()));
	}



	
}
