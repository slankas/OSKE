package edu.ncsu.las.annotator;

import java.util.logging.Level;

import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.MimeType;

public class MicroformatAnnotator extends Annotator {
		
	@Override
	public String getName() {
		return "Microformat Annotator";
	}	
	
	@Override
	public String getCode() {
		return "microformat";
	}

	@Override
	public String getDescription() {
		return "Extracts microformat data (http://microformats.org/) from html content";
	}

	@Override
	public String getContentType() {
		return MimeType.TEXT_HTML;
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.POST_DOCUMENT;
	}
	@Override
	public void process(Document doc) {
		
		if (doc.getMimeType().equals(MimeType.TEXT_HTML)) {
			doc.addAnnotation(this.getCode(), this.extractMicroFormatData(doc.getContentDataAsString(), doc.getDomainInstanceName()));
		}
		else {
			doc.addAnnotation(this.getCode(), new JSONObject());
		}
	}
	
	public JSONObject extractMicroFormatData(String htmlContent, String domainInstanceName) {
		
   		try {
			HttpResponse<JsonNode> jsonResponse = Unirest.post(Configuration.getConfigurationProperty(domainInstanceName,ConfigurationType.MICROFORMAT_API)+"v1/extract")
					                                     .header("accept", "application/json")
					                                     .header("Content-Type", "application/json")
					                                     .body(htmlContent).asJson();
			JSONObject result = jsonResponse.getBody().getObject();
			
			return result;
			/* TODO: need better error checking in remote API and handling code here
			if (result.getString("status").equals("success")) {
				return result.getJSONArray("result");
			}
			else {
				logger.log(Level.WARNING, "GeoTagging API returned an error: "+ result.optString("message"));
				return new JSONArray();
			}
			*/
   		}
   		catch (UnirestException ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
			return new JSONObject();
   		}
	}

	@Override
	public JSONObject getSchema() {
		JSONObject ret = new JSONObject();
		
		ret.put("type", "object");
		ret.put("enabled", false);
		
		return ret;
	}
}