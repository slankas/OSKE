package edu.ncsu.las.annotator;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.StructuralExtractrionCache;
import edu.ncsu.las.model.collector.StructuralExtractionRecord;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.MimeType;

public class StructuralAnnotator extends Annotator {
	private static Logger logger = Logger.getLogger(StructuralAnnotator.class.getName());
	private static StructuralExtractrionCache structuralExtractionCache = null;
	
	private static StructuralExtractrionCache getContentCache() {
		if (structuralExtractionCache == null) { //possible race condition on the first calls, but not really a problem.  just some extra processing retrieving concepts if it occurs.
			structuralExtractionCache = new StructuralExtractrionCache(Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM, ConfigurationType.CONCEPT_CACHESEC),true);
		}
		return structuralExtractionCache;
	}
		
	@Override
	public String getName() {
		return "Structural Extraction";
	}	
	
	@Override
	public String getCode() {
		return "structuralExtraction";
	}

	@Override
	public String getDescription() {
		return "Allows for user-defined content extraction via CSS selectors from HTML Text. (i.e., the structure of the HTML page is used to extract content.) ";
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
		doc.addAnnotation(this.getCode(),  this.extractContent( doc.getURL(), doc.getContentDataAsString(), doc.getDomainInstanceName()));
	}
	
	public JSONObject extractContent(String url, String htmlText, String domain)  {
		try {
			return StructuralExtractionRecord.annotateForStructuralExtraction(url, htmlText,  getContentCache().retrieveDomainStructuralExtractionRecords(domain),false);
		}
		catch (MalformedURLException mue) {
			logger.log(Level.WARNING, "Unable to extract content from page, bad url: "+url);
			return new JSONObject();
		}
	}

	@Override
	public JSONObject getSchema() {
		JSONObject ret = new JSONObject();
		
		ret.put("type", "nested");
		ret.put("enabled", true);
		return ret;
	}
	
}