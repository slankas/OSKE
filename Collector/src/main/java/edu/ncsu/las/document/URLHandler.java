package edu.ncsu.las.document;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.source.WebSourceHandler;


/**
 * The 
 * 
 * TODO complete this 
 *      Need to include possible configuration.
 * 
 */
public class URLHandler extends DocumentHandler {
	private static String[] MIME_TYPE = {MimeType.URL } ;
	
	@Override
	protected void processDocument() {
		if (this.getRouter().getSourceHandler() != null && this.getRouter().getSourceHandler() instanceof WebSourceHandler) { 
			String url = new String(this.getCurrentDocument().getContentData(), StandardCharsets.UTF_8);
			
			logger.log(Level.FINE, "Added URL back to a source handler: "+url);
			
			WebSourceHandler wsh = (WebSourceHandler) this.getRouter().getSourceHandler();
			wsh.addURLToCrawl(url,this.getCurrentDocument().getCrawlDepth());
		}
	}

	@Override
	public String[] getMimeType() {
		return MIME_TYPE;
	}
	@Override
	public String getDocumentDomain() {
		return "";
	}
	@Override
	public String getDescription() {
		return "Processes URLs by feeding them back into the current webcrawler for processing";
	}
	@Override
	public int getProcessingOrder() {
		return 0;
	}
}
