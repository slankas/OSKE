package edu.ncsu.las.storage.citation.annotation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.util.crypto.AESEncryption;

/**
 * Performs 
 * 
 */
public class TextRelationsAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "NLP Relation Annotation";   	}
	public String getCode() {	return "relations" ; 	}
	public String getDescription() { return "Extracts relations (triples) from the full text"; }
	public int getOrder() {	return 40; 	}
	public String[] getRequiredAnnotations() { return TEXT_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}
	
    public static void main( String[] args ) throws InvalidPasswordException, IOException     {
    	
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
		
		logger.log(Level.INFO,"PubMed Processeor starting....");
		logger.log(Level.INFO,"Validating unlimited strength policy files....");
		if (AESEncryption.hasUnlimitedStrengthPolicy() == false) {
			logger.log(Level.SEVERE, "JobCollector halting: Unlimited Strength Jurisdiction Policy Files are not installed for Java.");
			System.exit(1);
		}
		
		logger.log(Level.INFO,"Starting initialization....");
		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true,false);
    	
    	
        File jsonFile = new File("C:\\pubmed\\pubmed\\extractedRecords\\20236513.json");
        
        TextRelationsAnnotation tra = new TextRelationsAnnotation();
        tra.process(jsonFile, "", "");
        
    }
	
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		HashMap<String, JSONObject> uniqueTriples = new HashMap<String,JSONObject>();
   		
   		String lines[] = record.getString("text").split("\n");
   		for (String line: lines) {
   			logger.log(Level.INFO, "Processing line: "+line);
   			JSONObject textRequest =  new JSONObject().put("text", line);
   			
   	   		try {
   	   			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.NLP_API)+"v1/process?filterRelations=max")
   	   					                                     .header("accept", "application/json")
   	   					                                     .header("Content-Type", "application/json")
   	   					                                     .body(textRequest).asJson();
   	   			JSONObject result = jsonResponse.getBody().getObject();
  	   			
   	   			//System.out.println(result.toString(4));
   	   			
   	   			JSONArray resultTriples = result.getJSONArray("triples");
   	   			for (int i=0; i < resultTriples.length(); i++) {
   	   				JSONObject tripleRecord = resultTriples.getJSONObject(i);
   	   			
   	   				tripleRecord.put("count", 1);
   	   				
   	   				//make everything lowercase
   	   				tripleRecord.put("subj",  tripleRecord.getString("subj").toLowerCase());
   	   				tripleRecord.put("obj",   tripleRecord.getString("obj").toLowerCase());
   	   				tripleRecord.put("rel",   tripleRecord.getString("rel").toLowerCase());

				
   	   				// check that subject and objects are not pronouns
   	   				try {
	   	   				String subPOS = result.getJSONArray("parseData").getJSONObject(tripleRecord.getInt("sentIndex")-1).getJSONArray("tokens").getJSONObject(tripleRecord.getInt("subjIdxStart")-1).getString("partOfSpeech");
	   	   				String objPOS = result.getJSONArray("parseData").getJSONObject(tripleRecord.getInt("sentIndex")-1).getJSONArray("tokens").getJSONObject(tripleRecord.getInt("objIdxStart")-1).getString("partOfSpeech");
	   	   				//String subPOS = data.parseData[tripleRecord.sentIndex-1].tokens[tripleRecord.subjIdxStart-1].partOfSpeech;
	   	   				//String objPOS = data.parseData[tripleRecord.sentIndex-1].tokens[tripleRecord.objIdxStart-1].partOfSpeech;
	   	   				if (subPOS.equals("PRP") || subPOS.equalsIgnoreCase("DT") || objPOS.equals("PRP") || objPOS.equals("DT")) { 
	   	   					continue; 
	   	   				}
   	   				}
   	   				catch (JSONException e) {
   	   					logger.log(Level.WARNING, "JSON Indexing exception: "+ tripleRecord.toString(4));
   	   				}
   	   				String key = tripleRecord.getString("subj")+";"+tripleRecord.getString("rel")+";"+tripleRecord.getString("obj");
   	   				if (uniqueTriples.containsKey(key) == false) {
   	   					uniqueTriples.put(key, tripleRecord);
   	   				}
   	   				else {
   	   					tripleRecord = uniqueTriples.get(key);
   	   					tripleRecord.put("count", tripleRecord.getInt("count")+1);
   	   				}
   	   			}
   	   		}
   	   		catch (UnirestException ure) {
   	   			logger.log(Level.WARNING, "Unable to process remote request", ure);
   	   			return;
   	   		}
   			
   			
   		}
   		JSONArray result = new JSONArray();
   		uniqueTriples.values().stream().forEach( e-> {result.put(e);}  );
   		
   		record.put(this.getCode(), result);
	}

}
