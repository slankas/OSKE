package edu.ncsu.las.annotator;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;

/**
 * 
 * 
 * 
 *
 */
public class DataHeaderAnnotator extends Annotator {

	@Override
	public String getName() {
		return "dataHeader";
	}	
	
	@Override
	public String getCode() {
		return "dataHeader";
	}

	
	@Override
	public String getDescription() {
		return "'LAS' version of an enterprise data header";
	}

	@Override
	public String getContentType() {
		return "";
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.PRE_DOCUMENT;
	}
	
	@Override
	public int getPriority() {
		return 100;
	}

	@Override
	public void process(Document doc) {
		JSONObject security = new JSONObject();
		security.put("access", new JSONObject().put("ownerProducer", "USA").put("requires", new JSONObject().put("accessIndividual", new JSONArray())));
		security.put("noticeList", new JSONObject());
		
		JSONObject header = new JSONObject();
		header.put("usAgency","urn:us:las");
		header.put("icid", "las://openke/"+doc.getDomainInstanceName()+"/"+doc.getUUID());
		header.put("responsibleEntity", new JSONObject().put("country", "USA").put("organization", "org"));
		header.put("DataItemCreateDateTime", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
		header.put("ExternalSecurity", security);
		
		doc.addAnnotation(this.getCode(),header);
	}

	@Override
	public JSONObject getSchema() {
		return null;
	}
}
