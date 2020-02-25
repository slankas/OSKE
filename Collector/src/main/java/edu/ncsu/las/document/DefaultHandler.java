package edu.ncsu.las.document;

import java.util.logging.Level;

/**
 * If no other handlers are available to process the document, this
 * handler will extract the text and send back into the router for 
 * further processing.
 * 
 */
public class DefaultHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { "" };

	protected void processDocument() {
		this.appendText(this.getCurrentDocument().getExtractedTextFromTika());
		logger.log(Level.FINEST, this.getClass().getName() + " extracted text");
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
		return "If no available handlers are available, this process extracts the text for processing";
	}

	@Override
	public int getProcessingOrder() {
		return Integer.MAX_VALUE;
	}
}
