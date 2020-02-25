package edu.ncsu.las.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.pdfbox.pdmodel.PDDocument;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;
import edu.ncsu.las.util.FileUtilities;
/*
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
*/
/**
 * The PDFHandler
 * 
 */
public class PDFHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { "application/pdf" };
	
	protected void processDocument() {

		// Extract Text
		this.appendText(this.getCurrentDocument().getExtractedTextFromTika());

		//Extract URLs
		this.addURL(TikaUtilities.getURLs(this.getCurrentDocument().getContentData()));
		
		// Extract tables
		try {
			// TODO: fix this code - only procesing tables should be left.
/*
 * Note: Apache Tika and Tabula have dependency conflicts with regards to apache pdf box.  
 * need to make a separate service for Tabula to create and return table information.
 * 
 * 
			ByteArrayInputStream stream = new ByteArrayInputStream(this.getCurrentDocument().getContentData());
			ObjectExtractor oe = new ObjectExtractor(PDDocument.load(stream));
			FileUtilities.closeQuietly(stream);

			BasicExtractionAlgorithm extractor = new BasicExtractionAlgorithm();

			PageIterator it = oe.extract();
			List<Table> tables = new ArrayList<Table>();

			// The following is necessary because the Tabula code can enter
			// an infinite loop.  Limit the possible timing of a file to 10 seconds and
			// then break out if it is not complete.
			final ExecutorService service = Executors.newSingleThreadExecutor();
			try {
				while (it.hasNext()) {
					final Future<Object> f = service.submit( new Callable<Object>() {

						@Override
						public Object call() throws Exception {
							Page page = it.next(); // Page of type technology.tabula not crawler4
							tables.addAll(extractor.extract(page));
							return null;
						}
					});
					
			        f.get(10, TimeUnit.SECONDS);
					
				}
				oe.close();
			} catch (Exception ex) {
				logger.log(Level.WARNING, "Unable to process tables from PDF file: "+ex);
			}
			service.shutdown();

			for (Table t : tables) {
				this.addTable(LASTable.createLASTable(t));
			}
*/
			
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Unable to process PDF file: "+e);
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
		return "Extracts text, tables, and URLs from PDF documents";
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public int getProcessingOrder() {
		return 100;
	}

}
