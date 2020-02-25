package edu.ncsu.las.document;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import edu.ncsu.las.model.collector.type.MimeType;

/**
 * The
 * 
 */
public class TextHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.TEXT_PLAIN };

	public void processDocument() {
		String text = new String(this.getCurrentDocument().getContentData(), StandardCharsets.UTF_8);

		logger.log(Level.FINE,"Text processing Text size: " + text.length() + ", URL: "+this.getCurrentURL());
			
		// need to set this such that the text will be sent to the full-text search engine if necessary.
		this.setExtractedText(text);
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
		return "Performs any direct processing for text data.  Primary this established the extracted text portion of the record.";
	}

	@Override
	public int getProcessingOrder() {
		return 100;
	}
}
