package edu.ncsu.las.document;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import edu.ncsu.las.model.collector.type.MimeType;


/**
 * This class expands a TAR file, putting the files back into the document router
 * for further processing.
 * 
 */
public class TARHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.APPLICATION_TAR, MimeType.APPLICATION_GTAR };

	public void processDocument() {
		try {		
			TarArchiveInputStream tais = new TarArchiveInputStream(new ByteArrayInputStream(this.getCurrentDocument().getContentData()));
	    	
            TarArchiveEntry entry;

            /* Create a loop to read every single entry in TAR file */
            while (( entry = tais.getNextTarEntry()) != null) {
            	if (entry.isDirectory()) {
            		continue;
            	}
            	
            	String fileName = entry.getName();
            	byte[] data = new byte[(int) entry.getSize()];
            	tais.read(data, 0, data.length);
                logger.log(Level.FINE, "tar forwarding - "+fileName);
 		    	this.forwardFile(fileName,data,entry.getModTime().getTime()); 

            }               
            tais.close();			
			
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
		return "Extracts files from TAR files, feeding the files back into the document router";
	}

	@Override
	public int getProcessingOrder() {
		return 10;
	}
}
