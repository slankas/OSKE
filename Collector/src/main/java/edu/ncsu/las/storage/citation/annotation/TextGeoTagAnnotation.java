package edu.ncsu.las.storage.citation.annotation;

import java.util.logging.Level;

import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * Performs geo-tagging on the full-text of a citation record.
 * 
 */
public class TextGeoTagAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "Geo-tag Text Field";   	}
	public String getCode() {	return "geotag" ; 	}
	public String getDescription() { return "Geo-tags the text field to find additional geo-locations present."; }
	public int getOrder() {	return 25; 	}
	public String[] getRequiredAnnotations() { return TEXT_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
   		JSONObject geoRequest =  new JSONObject().put("text", record.getString("text"));
		
   		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.GEOTAG_API)+"v1/process")
					                                     .header("accept", "application/json")
					                                     .header("Content-Type", "application/json")
					                                     .body(geoRequest).asJson();
			JSONObject result = jsonResponse.getBody().getObject();
			
			if (result.getString("status").equals("success")) {
				record.put(this.getCode(), result.getJSONArray("result"));
			}
			else {
				logger.log(Level.WARNING, "GeoTagging API returned an error: "+ result.optString("message"));
			}
   		}
   		catch (UnirestException ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
   		}	
	}



}
