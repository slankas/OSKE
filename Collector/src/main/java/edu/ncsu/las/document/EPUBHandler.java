package edu.ncsu.las.document;


import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;


import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.type.MimeType;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * EPUB Handler
 * Extracts text and URLs from epub documents using the epublib - http://www.siegmann.nl/epublib
 * 
 * all of the text is extracted in this handler.  Images are extracted individually and processed as separate 
 * documents
 * 
 */
public class EPUBHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.APPLICATION_EPUB };
		
	protected void processDocument() {
		
		
		try {
			EpubReader epubReader = new EpubReader();
			Book book = epubReader.readEpub(new ByteArrayInputStream(this.getCurrentDocument().getContentData()));
			
			List<String> titles = book.getMetadata().getTitles();
			StringBuilder textContent = new StringBuilder(String.join("\n", titles));
			
			Collection<Resource> resources = book.getResources().getAll();
			for (Resource r: resources) {
				MediaType mt = r.getMediaType();
				if (mt.getName().equals("image/jpeg")) {
					this.forwardFile(r.getHref(), r.getData(), -1);
				}
				else if (mt.getName().equals("image/png")) {
					this.forwardFile(r.getHref(), r.getData(), -1);
				}
			}
			Spine spine = book.getSpine();
			for (int i=0; i< spine.size(); i++) {
				Resource r = spine.getResource(i);
				MediaType mt = r.getMediaType();
				if (mt.getName().equals("application/xhtml+xml")) {
					String htmlText = new String(r.getData(), r.getInputEncoding());
					
					textContent.append("\n");
					textContent.append(TikaUtilities.extractText(htmlText.getBytes()));
					
					//Extract URLs
					this.addURL(TikaUtilities.getURLs(htmlText.getBytes()));
					
					// Extract tables
					try {

						
					} catch (Throwable e) {
						logger.log(Level.WARNING, "Unable to process epub file for tables: "+e);
					}
				}
				else {
					
				}
			}
			// Extract Text
			this.appendText(textContent.toString());
			
		} catch (IOException e) {
			logger.log(Level.WARNING, "Unable to process epub file: "+e);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	@Override
	public String[] getMimeType() {
		return MIME_TYPE;
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public String getDocumentDomain() {
		return "";
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public String getDescription() {
		return "Extracts text, tables, and URLs from epub documents";
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public int getProcessingOrder() {
		return 100;
	}

	public static void main(String args[]) {
		// sample code to view epub contents
		try {
			EpubReader epubReader = new EpubReader();
			Book book = epubReader.readEpub(new FileInputStream("epub to read"));
			
			List<String> titles = book.getMetadata().getTitles();
			String title = String.join("\n", titles);
			System.out.println(title);
			
			Collection<Resource> resources = book.getResources().getAll();
			for (Resource r: resources) {
				System.out.println(r);
			}
			Spine spine = book.getSpine();
			for (int i=0; i< spine.size(); i++) {
				Resource r = spine.getResource(i);
				MediaType mt = r.getMediaType();
				if (mt.getName().equals("application/xhtml+xml")) {
					String htmlText = new String(r.getData(), r.getInputEncoding());
					System.out.println(TikaUtilities.extractText(htmlText.getBytes()));
					
					//Extract URLs
					System.out.println(TikaUtilities.getURLs(htmlText.getBytes()));
				}
			}
			
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		

	}
	
}
