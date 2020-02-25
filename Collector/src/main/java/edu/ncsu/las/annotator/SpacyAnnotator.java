package edu.ncsu.las.annotator;

import java.util.logging.Level;


import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * Calls a rest-based service to perform NLP annotatios (currently just named-entities) using
 * the Python spaCy library.  See https://spacy.io/usage/linguistic-features for other possibilities
 * 
 */
public class SpacyAnnotator extends Annotator {
		
	@Override
	public String getName() {
		return "spaCy NLP Annotator";
	}	
	
	@Override
	public String getCode() {
		return "spacy";
	}

	@Override
	public String getDescription() {
		return "Named entities found with text.";
	}

	@Override
	public String getContentType() {
		return "";
	}
	
	public  int getPriority() {
		return 85;
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.SECONDARY;
	}
	@Override
	public void process(Document doc) {
		doc.addAnnotation(this.getCode(), this.annotateText(doc.getExtractedText(), doc.getDomainInstanceName()));
	}
	
	public JSONObject annotateText(String text, String domainInstanceName) {
		
   		JSONObject nlpRequest =  new JSONObject().put("text", text).put("showAll", false);
		
   		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainInstanceName,ConfigurationType.SPACY_API)+"ner")
					                                     .header("accept", "application/json")
					                                     .header("Content-Type", "application/json")
					                                     .body(nlpRequest).asJson();
			JSONObject result = jsonResponse.getBody().getObject();
			
			if (result.has("entities")) {
				return result;
			}
			else {
				logger.log(Level.WARNING, "spaCy API had an issue. See that log for more details");
				return new JSONObject();
			}
   		}
   		catch (Exception ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
			return new JSONObject();
   		}
	}

	@Override
	public JSONObject getSchema() {
		JSONObject result = new JSONObject("{\"properties\":{\"entities\":{\"type\":\"nested\",\"properties\":{\"text\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"ignore_above\":256,\"type\":\"keyword\"}}},\"type\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"ignore_above\":256,\"type\":\"keyword\"}}},\"startPos\":{\"type\":\"long\"},\"endPos\":{\"type\":\"long\"}}}}}");
		return result;
	}
}