package edu.ncsu.las.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.ncsu.las.model.collector.type.MimeType;

/**
 * This class expands a zip file, putting the file(s) back into the document router
 * for further processing.
 * 
 */
public class ZipHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.APPLICATION_ZIP };

	public void processDocument() {
		 byte[] buffer = new byte[1024];

		try {
	    	//get the zip file content
	    	ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(this.getCurrentDocument().getContentData()));
	    	
	    	//get the zipped file list entry
	    	ZipEntry ze;
	    		
	    	while( (ze = zis.getNextEntry()) !=null){
	    			
	    	   String fileName = ze.getName();
	    	   if (ze.isDirectory() == false) {
		    	   //System.out.println(fileName);
		    	   
		    	   int size = (int) ze.getSize();
		    	   if (size <1024) size = 1024;
		    	   
		    	   ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
		           int len;
		           while ((len = zis.read(buffer)) > 0) {
		        	   baos.write(buffer, 0, len);
		           }
		           baos.flush();
		           baos.close();
		           byte[] data = baos.toByteArray();
		    	   
		           logger.log(Level.FINE, "zip forwarding - "+fileName);
		    	   this.forwardFile(fileName,data, ze.getTime());
	    	   }
	    	}
	    	
	        zis.closeEntry();
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
		return "Extracts files from zip files, feeding the files back into the document router";
	}

	@Override
	public int getProcessingOrder() {
		return 10;
	}
}
