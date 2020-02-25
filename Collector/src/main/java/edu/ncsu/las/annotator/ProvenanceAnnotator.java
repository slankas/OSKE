package edu.ncsu.las.annotator;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;

/**
 * 
 *
 */
public class ProvenanceAnnotator extends Annotator {

	@Override
	public String getName() {
		return "provenance";
	}	
	
	@Override
	public String getCode() {
		return "provenance";
	}

	
	@Override
	public String getDescription() {
		return "provides basic provenance for the document";
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
		return 50;
	}


	 @Override
	 public void process(Document doc) {
	 JSONObject provObject = new JSONObject();
	 provObject.put("label", "LAS: "+doc.getDomainInstanceName());
	 provObject.put("collectorVersion", Configuration.getCollectorDaemonBuildTimestamp());
	 provObject.put("configuration", doc.getRelatedJobConfiguration());
	 provObject.put("job", doc.getJobSummaryInformation());
	 
	 if (doc.getJobHistory() != null && doc.getJobHistory().getJobHistoryID() != null) {
		 provObject.put("jobHistoryID", doc.getJobHistory().getJobHistoryID().toString());
	 }

	 doc.addAnnotation(this.getCode(),provObject);
	 
	 }

	@Override
	public JSONObject getSchema() {
		return new JSONObject("{\"properties\":{  \"retrievals\": { \"type\": \"nested\" },    \"configuration\":{\"properties\":{\"allowSingleHopFromReferrer\":{\"type\":\"text\"},\"webCrawler\":{\"properties\":{\"respectRobotsTxt\":{\"type\":\"text\"}}}}},\"job\":{\"properties\":{\"url\":{\"type\":\"text\",\"fields\":{\"raw\":{\"index\":true,\"type\":\"keyword\"}}}}}}}");
	}
}