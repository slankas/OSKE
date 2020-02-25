package edu.ncsu.las.annotator;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.concept.Concept;
import edu.ncsu.las.model.collector.concept.ConceptCache;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;

public class ConceptAnnotator extends Annotator {
	private static ConceptCache concepts = null;
	
	private static ConceptCache getConceptCache() {
		if (concepts == null) { //possible race condition on the first calls, but not really a problem.  just some extra processing retrieving concepts if it occurs.
			concepts = new ConceptCache(Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.CONCEPT_CACHESEC));
		}
		return concepts;
	}
		
	@Override
	public String getName() {
		return "Concepts";
	}	
	
	@Override
	public String getCode() {
		return "concepts";
	}

	@Override
	public String getDescription() {
		return "Concepts found within text";
	}

	@Override
	public String getContentType() {
		return "";
	}

	@Override
	public AnnotatorExecutionPoint getExecutionPoint() {
		return AnnotatorExecutionPoint.POST_DOCUMENT;
	}
	@Override
	public void process(Document doc) {
		doc.addAnnotation(this.getCode(), this.annotateConcepts(doc.getExtractedText(), doc.getDomainInstanceName()));
	}
	
	public JSONArray annotateConcepts(String text,  String domain) {
		return Concept.annotateConcepts(text, domain, getConceptCache().retrieveDomainCategoryTable(domain), getConceptCache().retrieveDomainConcepts(domain));
	}

	@Override
	public JSONObject getSchema() {
		JSONObject ret = new JSONObject();
		
		ret.put("type", "nested");
		
		return ret;
	}
	
}