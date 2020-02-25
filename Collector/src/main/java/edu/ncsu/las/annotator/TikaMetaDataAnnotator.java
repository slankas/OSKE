package edu.ncsu.las.annotator;


import org.json.JSONObject;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;

/**
 * 
 * 
 * 
 *
 */
public class TikaMetaDataAnnotator extends Annotator {

	@Override
	public String getName() {
		return "tikaMetaData";
	}	
	
	@Override
	public String getCode() {
		return "tikaMetaData";
	}

	
	@Override
	public String getDescription() {
		return "Tika meta data extraction";
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
	public void process(Document doc) {
		if (doc.getContentData() == null) { return; }
		java.util.Map<String,String> metaData = TikaUtilities.extractMetaData(doc.getContentData());	
		
		metaData.keySet().removeIf( key -> key.trim().length() == 0);
						
		doc.addAnnotation(this.getCode(),new JSONObject(metaData));
	}

	@Override
	public JSONObject getSchema() {
		JSONObject ret = new JSONObject();
		
		ret.put("type", "object");
		ret.put("enabled", false);
		
		return ret;
	}
}
