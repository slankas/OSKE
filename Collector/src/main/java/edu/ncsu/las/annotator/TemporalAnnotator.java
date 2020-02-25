package edu.ncsu.las.annotator;

import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * Calls a rest-based service to perform gather date and time references from a document.  Uses HeidelTime 
 * through a REST-base service API to get the data
 * 
 */
public class TemporalAnnotator extends Annotator {
		
	@Override
	public String getName() {
		return "Temporal Annotator";
	}	
	
	@Override
	public String getCode() {
		return "temporal";
	}

	@Override
	public String getDescription() {
		return "Time annotations found within the text";
	}

	public  int getPriority() {
		return 90;
	}
	
	@Override
	public String getContentType() {
		return "";
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.SECONDARY;
	}
	@Override
	public void process(Document doc) {
		doc.addAnnotation(this.getCode(), this.annotateText(doc, doc.getDomainInstanceName()));
	}
	
	public JSONArray annotateText(Document doc, String domainInstanceName) {
		
   		JSONObject nlpRequest =  new JSONObject().put("text", doc.getExtractedText()).put("published_date", doc.getAnnotation("published_date"));
		
   		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainInstanceName,ConfigurationType.TEMPORAL_API)+"extract")
					                                     .header("accept", "application/json")
					                                     .header("Content-Type", "application/json")
					                                     .body(nlpRequest).asJson();
			JSONObject result = jsonResponse.getBody().getObject();
			
			if (result.has("status") && result.getString("status").equals("success")) {
				return result.getJSONArray("result");
			}
			else {
				logger.log(Level.WARNING, "Temporal API had an issue. See that log for more details");
				return new JSONArray();
			}
   		}
   		catch (Exception ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
			return new JSONArray();
   		}
	}

	@Override
	public JSONObject getSchema() {
		return new JSONObject("{\"type\":\"nested\",\"properties\":{\"mod\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"position\":{\"type\":\"long\"},\"text\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"tid\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"type\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"value\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}}}}");
		
	}
}