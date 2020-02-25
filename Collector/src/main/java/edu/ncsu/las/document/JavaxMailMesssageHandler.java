package edu.ncsu.las.document;

import edu.ncsu.las.model.collector.type.MimeType;

/**
 * Processes documents that are javax messages
 * As the IMapMessage is not serializable, most of the necessary processing (feeding attachments, getting text, adding URLs to follow
 * occurs in the EmailSourceHandler class.
 * 
 */
public class JavaxMailMesssageHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.JAVAX_MAILMESSAGE };
	

	

	public void processDocument() {
		this.appendText(this.getCurrentDocument().getExtractedTextFromTika());		
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
		return "processes a java mail message.";
	}

	@Override
	public int getProcessingOrder() {
		return 10;
	}
}
