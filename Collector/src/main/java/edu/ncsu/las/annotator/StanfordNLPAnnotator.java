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
 * Calls a REST API that fronts the Stanford Core NLP tools to get the extract relations, tokens, and named entities
 *
 */
public class StanfordNLPAnnotator extends Annotator {
		
	@Override
	public String getName() {
		return "Stanford NLP Annotator";
	}	
	
	@Override
	public String getCode() {
		return "stanfordNLP";
	}

	@Override
	public String getDescription() {
		return "Tokens, named entities, and relations extract from the text by the Stanford Core NLP service";
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
		doc.addAnnotation(this.getCode(), this.performNLP(doc.getExtractedText(), doc.getDomainInstanceName()));
	}
	
	public JSONObject performNLP(String text, String domainInstanceName) {
		JSONObject result = new JSONObject().put("namedEnities", new JSONArray())
				                            .put("relations", new JSONArray())
				                            .put("tokens", new JSONArray());
		
		String[] sentences = text.split("\n");
		for (String s: sentences) {
			JSONObject sDoc = new JSONObject().put("text",s);
			
			try {
   				HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainInstanceName,ConfigurationType.NLP_API)+"v1/process?filterRelations=max")
   					                                     .header("accept", "application/json")
   					                                     .header("Content-Type", "application/json")
   					                                     .body(sDoc).asJson();
   				JSONObject nlpResult = jsonResponse.getBody().getObject();
   				//TODO: Test result
   				//TODO: add to relations (make unique)
   				//TODO: add to entities -> need to document all of the locations and identified text
   				//TODO: add tokens. Will need to adjust sentence numbers.
   				

				}
   			catch (UnirestException ure) {
   				logger.log(Level.WARNING, "Unable to process remote request", ure);
   			}
   		}
		
		return result;
	}

	@Override
	public JSONObject getSchema() {
		//TODO: MUST IMPLEMENT THIS!!!
		return new JSONObject("{\"type\":\"nested\"}"); //"{\"type\":\"nested\",\"properties\":{\"geoData\":{\"properties\":{\"elevation\":{\"type\":\"long\"},\"geoNameID\":{\"type\":\"long\"},\"parentGeoNameID\":{\"type\":\"long\"},\"latitude\":{\"type\":\"float\"},\"longitude\":{\"type\":\"float\"},\"population\":{\"type\":\"long\"}}}}}");
		
	}
	
	/*
						var newEntities = [];
						for (var i=0;i<data.parseData.length;i++) {
							var lastType = "O";
							
							for (var j=0; j< data.parseData[i].tokens.length; j++) {
								var namedType = data.parseData[i].tokens[j].namedEnitity;  //TODO FIX TYPO!!!
								if (namedType === "O") { lastType="O"; continue;}

								var word      = data.parseData[i].tokens[j].word;
								var record
								if (lastType == namedType) {
									var record = newEntities[newEntities.length-1];
									record.entity = record.entity + " " + word;
								}
								else {
									record = { "entity": word, "namedType" : namedType, "count" :1 };
									
									newEntities.push(record)
								}
								
								lastType = namedType;
									
							}
						}
						
						// loop though results and add to hash 
						var uniqueAdd = []
						for (var i=0; i < newEntities.length; i++) {
							var record = newEntities[i];
							var key = record.entity+";"+record.namedType;
							
							if (entities.hasOwnProperty(key)) {
								entities[key].count = entities[key].count +1 
							}
							else {
								entities[key] = record
								uniqueAdd.push(record)
							}							

						}
						

			 			LASTable.displayJSONInTable(uniqueAdd, columnLabels, columnFields,'entityTable',[[ 0, "asc" ]]);
	 
	 */
	
}