package edu.ncsu.las.document;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.util.FileUtilities;

/**
 * The
 * 
 * TODO complete this Need to include possible configuration.
 * 
 */
public class GZipHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.APPLICATION_GZIP , MimeType.APPLICATION_XGZIP};

	public void processDocument() {
		try {
	    	//get the zip file content
			GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(this.getCurrentDocument().getContentData()));
		    byte[] data = FileUtilities.uncompressGZipStream(zis);
		    
		    String url = this.getCurrentDocument().getURL();
		    
		    logger.log(Level.FINE, "gzip forwarding - "+url);
		    if (url.endsWith(".gz")) {
		    	url = url.substring(0, url.lastIndexOf(".gz"));
		    }
		    this.forwardFile(url,data,-1);
			
	    	zis.close();
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to process zip file");
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
		return "Extracts files from g-zip files, feeding the files back into the document router";
	}

	@Override
	public int getProcessingOrder() {
		return 10;
	}
}
