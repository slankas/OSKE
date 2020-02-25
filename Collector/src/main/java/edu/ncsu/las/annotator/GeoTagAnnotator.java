package edu.ncsu.las.annotator;

import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * Calls a CLAVIN service to identify possible locations identified in text.
 * 
 */
public class GeoTagAnnotator extends Annotator {
		
	@Override
	public String getName() {
		return "GeoTag Annotator";
	}	
	
	@Override
	public String getCode() {
		return "geotag";
	}

	@Override
	public String getDescription() {
		return "Geolocations found within the text";
	}

	@Override
	public String getContentType() {
		return "";
	}
	
	public  int getPriority() {
		return 70;
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.SECONDARY;
	}
	@Override
	public void process(Document doc) {
		doc.addAnnotation(this.getCode(), this.geoTagText(doc.getExtractedText(), doc.getDomainInstanceName()));
	}
	
	public JSONArray geoTagText(String text, String domainInstanceName) {
		
   		JSONObject geoRequest =  new JSONObject().put("text", text);
		
   		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainInstanceName,ConfigurationType.GEOTAG_API)+"v1/process")
					                                     .header("accept", "application/json")
					                                     .header("Content-Type", "application/json")
					                                     .body(geoRequest).asJson();
			JSONObject result = jsonResponse.getBody().getObject();
			
			if (result.getString("status").equals("success")) {
				JSONArray locations = result.getJSONArray("result");
				
				// Add "location" field to geoData.  This is mapped as geo-point type and can be used within ElasticSearch's geospatial queries
				for (int i=0;i<locations.length();i++) {
					JSONObject obj = locations.getJSONObject(i);
					JSONObject geoData = obj.getJSONObject("geoData");
					JSONObject locObj = new JSONObject().put("lat", geoData.getFloat("latitude")).put("lon", geoData.getFloat("longitude"));
					geoData.put("location",locObj);
				}
				
				return locations;
			}
			else {
				logger.log(Level.WARNING, "GeoTagging API returned an error: "+ result.optString("message"));
				return new JSONArray();
			}
   		}
   		catch (UnirestException ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
			return new JSONArray();
   		}
	}

	@Override
	public JSONObject getSchema() {
		return new JSONObject("{\"type\":\"nested\",\"properties\":{\"geoData\":{\"properties\":{\"elevation\":{\"type\":\"long\"},\"geoNameID\":{\"type\":\"long\"},\"parentGeoNameID\":{\"type\":\"long\"},\"latitude\":{\"type\":\"float\"},\"longitude\":{\"type\":\"float\"},\"location\": {\"type\": \"geo_point\" },\"population\":{\"type\":\"long\"}}}}}");
		
	}
}