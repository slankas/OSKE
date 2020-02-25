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
public class DBPediaAnnotator extends Annotator {
		
	@Override
	public String getName() {
		return "DBPedia Annotator";
	}	
	
	@Override
	public String getCode() {
		return "dbpedia";
	}

	@Override
	public String getDescription() {
		return "DBPedia Annotator - finds references in the text to DBPedia";
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
		doc.addAnnotation(this.getCode(), this.annotateText(doc.getExtractedText(), doc.getDomainInstanceName()));
	}
	
	public JSONArray annotateText(String text, String domainInstanceName) {
		if (text == null || text.trim().length() == 0) {
			logger.log(Level.FINE, "detected empty text, not callinng DB Annotator");
			return new JSONArray();
		}
   		try {
   			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainInstanceName,ConfigurationType.DBPEDIA_SPOTLIGHT_API) )
   	                .header("accept", "application/json")
   	                .field("text", text)
   	                .field("confidence", 0.40)
   	                .asJson();
   			
   			JSONObject result = jsonResponse.getBody().getObject();
   			
   			return result.getJSONArray("Resources");
   		}
   		catch (Exception ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
   			logger.log(Level.WARNING, "Sent text: " + text);
			return new JSONArray();
   		}
	}

	@Override
	public JSONObject getSchema() {
		return new JSONObject("{\"type\":\"nested\",\"properties\":{\"@URI\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"@offset\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"@percentageOfSecondRank\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"@similarityScore\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"@support\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"@surfaceForm\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"@types\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}}}}");
		
	}
}