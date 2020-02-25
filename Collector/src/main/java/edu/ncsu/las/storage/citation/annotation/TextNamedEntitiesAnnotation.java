package edu.ncsu.las.storage.citation.annotation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.json.JSONArray;
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
public class TextNamedEntitiesAnnotation extends Annotation {
	private static final String[] TEXT_ARRAY = {"text" };
	
	public String getName() {	return "NLP Named Entity Annotation";   	}
	public String getCode() {	return "namedEntities" ; 	}
	public String getDescription() { return "Extracts named entities from the full text"; }
	public int getOrder() {	return 45; 	}
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
		Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true, false);
    	
    	
        File jsonFile = new File("C:\\pubmed\\pubmed\\extractedRecords\\20236513.json");
        
        TextNamedEntitiesAnnotation tra = new TextNamedEntitiesAnnotation();
        tra.process(jsonFile, "", "");
        
    }
	
	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) { 
		HashMap<String, JSONObject> uniqueEntities = new HashMap<String,JSONObject>();
   		
   		String lines[] = record.getString("text").split("\n");
 		for (String line: lines) {
   			logger.log(Level.INFO, "Processing line: "+line);
   			JSONObject textRequest =  new JSONObject().put("text", line);
   			
   	   		try {
   	   			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.NLP_API)+"v1/process?filterRelations=max")  // use the same call to get cache benefits w/ relation extraction
   	   					                                     .header("accept", "application/json")
   	   					                                     .header("Content-Type", "application/json")
   	   					                                     .body(textRequest).asJson();
   	   			JSONObject result = jsonResponse.getBody().getObject();
  	   			
   	   			//System.out.println(result.toString(4));
   	   			
   	   			JSONArray newEntities = new JSONArray();
   	   			JSONArray parsedDataArray = result.getJSONArray("parseData");
   	   			for (int i=0;i< parsedDataArray.length(); i++) {
   	   				String lastType = "O";
				
   	   				JSONArray tokenArray = parsedDataArray.getJSONObject(i).getJSONArray("tokens");
   	   				for (int j=0; j< tokenArray.length(); j++) {
   	   					String namedType = tokenArray.getJSONObject(j).getString("namedEnitity");  //TODO FIX TYPO!!!
   	   					if (namedType.equals("O")) { lastType="O"; continue;}

   	   					String word = tokenArray.getJSONObject(j).getString("word");
   	   					
   	   					if (lastType.equals(namedType)) {
   	   						JSONObject neRecord= newEntities.getJSONObject(newEntities.length()-1);
   	   						neRecord.put("entity", neRecord.getString("entity") + " " + word);
   	   					}
   	   					else {
   	   						JSONObject neRecord = new JSONObject().put("entity",word).put("namedType",namedType).put("count",1);
							newEntities.put(neRecord);
   	   					}
   	   					lastType = namedType;
   	   				}
					
   	   			}
   	   			
   				// loop though results and add to hash 
   				for (int i=0; i < newEntities.length(); i++) {
   					JSONObject neRecord = newEntities.getJSONObject(i);
   					String key = neRecord.getString("entity")+";"+neRecord.getString("namedType");
   					
   					if (uniqueEntities.containsKey(key)) {
   					    neRecord.put("count", neRecord.getInt("count")+1);
   					}
   					else {
   						uniqueEntities.put(key, neRecord);
   					}				

   				}
   	   			
   	   		}
   	   		catch (UnirestException ure) {
   	   			logger.log(Level.WARNING, "Unable to process remote request", ure);
   	   			return;
   	   		}
 		}	

			
   		JSONArray result = new JSONArray();
   		uniqueEntities.values().stream().forEach( e-> {result.put(e);}  );
   		
   		record.put(this.getCode(), result);
	}

}
