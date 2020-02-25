package edu.ncsu.las.document;

import java.util.logging.Level;

import edu.ncsu.las.model.collector.type.MimeType;


/**
 * This class expands a TAR file, putting the files back into the document router
 * for further processing.
 * 
 */
public class ImageHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.IMAGE_GIF, MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG, MimeType.IMAGE_TIFF };
	
	public void processDocument() {
		logger.log(Level.INFO, "Image handler - no action taking");
		


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
		return "Processes image files.";
	}

	@Override
	public int getProcessingOrder() {
		return 10;
	}
}
