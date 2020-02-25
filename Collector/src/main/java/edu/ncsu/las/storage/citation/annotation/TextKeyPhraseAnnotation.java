package edu.ncsu.las.storage.citation.annotation;

import java.util.logging.Level;

import org.json.JSONArray;
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
public class TextKeyPhraseAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "Keyprase Annotation";   	}
	public String getCode() {	return "keyphrases" ; 	}
	public String getDescription() { return "Extracts keyphrases from a document.  Limited to the top 100 keywords.  Maybe less if scores are below a threshold."; }
	public int getOrder() {	return 35; 	}
	public String[] getRequiredAnnotations() { return TEXT_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
   		JSONObject textRequest =  new JSONObject().put("text", record.getString("text"));
		
   		double percentage = 0.25;
   		
   		try {
   			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.TEXTRANK_API)+"keyphrase/"+percentage)
   					                                     .header("accept", "application/json")
   					                                     .header("Content-Type", "application/json")
   					                                     .body(textRequest).asJson();
   			JSONObject result = jsonResponse.getBody().getObject();
   			
   			JSONArray keyphrases = result.getJSONArray("keyphrase");
   			JSONArray resultArray = new JSONArray();
   			
   			double scoreFactor = 1;
   			if (keyphrases.length() > 0) {
   				scoreFactor = 1.0 / keyphrases.getJSONObject(0).getDouble("score");
   			}
   			
   			int maxKeywords = Math.min( keyphrases.length(), 100);
   			for (int i=0; i < maxKeywords; i++) {
   				JSONObject o = keyphrases.getJSONObject(i);
   				if ( (o.getDouble("score")* scoreFactor) > 0.1) {
   					resultArray.put(o.getString("phrase"));
   				}
   			}
   	   			
   			record.put(this.getCode(), resultArray);
   		}
   		catch (UnirestException ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
   		}	
	}

}
