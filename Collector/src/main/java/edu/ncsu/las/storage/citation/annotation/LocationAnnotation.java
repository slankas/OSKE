package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.http.HttpHost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.util.concurrent.RateLimiter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.ncsu.las.storage.citation.PubMedProcessor;
import edu.ncsu.las.util.json.JSONUtilities;

public class LocationAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};
	
	private static final RateLimiter rateLimiter = RateLimiter.create(6); 
	
	public static HashMap<String, JSONObject> osmCachedResults = new HashMap<String, JSONObject>();
	
	public String getName() {	return "Location Field";   	}
	public String getCode() {	return "location" ; 	}
	public String getDescription() { return "Records the primary location for the article"; }
	public int getOrder() {	return 25; 	}
	public String[] getRequiredAnnotations() { return EMPTY_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		
        // Find the first affiliation line that has been geo-coded
		if (record.getJSONObject("Article").has("AuthorList")) {
			JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");
			JSONObject affiliationObject = null;
			
			for (int i=0; i < authors.length(); i++) {
				JSONObject author = authors.getJSONObject(i);
				if (author.has("AffiliationInfo") && author.getJSONArray("AffiliationInfo").length()>0) {
					try {
						JSONObject aff = author.getJSONArray("AffiliationInfo").getJSONObject(0).getJSONObject("location");
						if (aff.has("longitude")) {
							affiliationObject = aff;
							break;
						}
					}
					catch (JSONException e) {
						logger.log(Level.WARNING, "No location object");
					}
				}
			}
			
			if (affiliationObject != null) {
				JSONArray locationArray = new JSONArray();
				locationArray.put(affiliationObject.getDouble("longitude"));
				locationArray.put(affiliationObject.getDouble("latitude"));
				record.put(this.getCode(), locationArray);
				
				String osmID = "R"+affiliationObject.getString("osm_id")+",N"+affiliationObject.getString("osm_id")+",W"+affiliationObject.getString("osm_id");
				System.out.println(osmID);
				if (osmCachedResults.containsKey(osmID) == false) {
					rateLimiter.acquire();

					try {
						Unirest.setProxy(new HttpHost("152.1.13.152", 8888));
						HttpResponse<JsonNode> jsonResponse = Unirest.get("http://nominatim.openstreetmap.org/lookup?format=json&osm_ids="+osmID)
                                   .header("accept", "application/json")
                                   .header("Content-Type", "application/json")                                 
                                   .asJson();
						System.out.println(jsonResponse.getBody());
						JSONArray result = jsonResponse.getBody().getArray();
						if (result.length() > 0) {
							JSONObject placeObject = result.getJSONObject(0);
							String countryCode   = JSONUtilities.getAsString(placeObject, "address.country_code", "").toUpperCase();
							String countryNameEN = JSONUtilities.getAsString(placeObject, "address.country", "");
							
							JSONObject countryResult = new JSONObject().put("countryCode", countryCode).put("countryNameEN", countryNameEN);
							osmCachedResults.put(osmID, countryResult);
						}
					}
					catch (Exception e) {
						logger.log(Level.WARNING, "unable find OSM place name: "+e.toString());
					}
				}
				if (osmCachedResults.containsKey(osmID)) {
					JSONObject countryResult = osmCachedResults.get(osmID);
					record.put("locationCountryCode", countryResult.getString("countryCode"));
					record.put("locationCountryName", countryResult.getString("countryNameEN"));
				}

				logger.log(Level.INFO, "Extracted location: "+record.getString("PMID"));
				return;
			}
		}

		//record.put(this.getCode(),	"");
		logger.log(Level.INFO, "Extracted location failed: "+record.getString("PMID"));
		
	}

	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";
		
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));
		
		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new LocationAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new LocationAnnotation()).getCode()));
	}



	
}
