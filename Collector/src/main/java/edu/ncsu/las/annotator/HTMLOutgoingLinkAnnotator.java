package edu.ncsu.las.annotator;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.json.JSONObject;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.util.json.JSONUtilities;

/**
 * Checks if there are HTTP headers for current document.
 * If so, converts them into a hash map.
 *
 */
public class HTMLOutgoingLinkAnnotator extends Annotator {

	@Override
	public String getName() {
		return "HTML Outgoing Links";
	}	
	
	@Override
	public String getCode() {
		return "html_outlinks";
	}

	
	@Override
	public String getDescription() {
		return "Puts a list of the outgoing URLs into an array";
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
	public int getPriority() {
		return 15;
	}


	@Override
	public void process(Document doc) {
		if (doc.getMimeType().equalsIgnoreCase(MimeType.APPLICATION_RSS_XML) || doc.getMimeType().equalsIgnoreCase(MimeType.APPLICATION_ATOM_XML)) {
			try {
				List<SearchRecord> srList = SearchRecord.getFeedEntries(doc.getContentData());
				List<String> stringURLs = srList.stream().map( result -> result.getUrl()).collect(Collectors.toList());
				String[] urls = stringURLs.toArray(new String[0]);
				java.util.Arrays.sort(urls);
				doc.addAnnotation(this.getCode(),JSONUtilities.toJSONArray(urls));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unable to read feed entries in annotation",e);
			}
		}
		else  {  //if (doc.getMimeType().equalsIgnoreCase(MimeType.TEXT_HTML))
			//List<String> myURLs = doc.getOutgoingURLs().stream().sorted().collect(Collectors.toList());
			String[] urls = doc.getOutgoingURLs().toArray(new String[0]);
			java.util.Arrays.sort(urls);			
			doc.addAnnotation(this.getCode(),JSONUtilities.toJSONArray(urls));
		}		
		
	}

	@Override
	public JSONObject getSchema() {
		return null;
	}


}
