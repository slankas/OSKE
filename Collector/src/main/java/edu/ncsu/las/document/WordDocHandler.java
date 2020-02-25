package edu.ncsu.las.document;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;

/**
 * 
 */
public class WordDocHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { "application/msword" };

	

	@Override
	protected void processDocument() {

		try (InputStream input = new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())) {

			HWPFDocument doc = new HWPFDocument(input);

			Range range = doc.getRange();

			for (int i = 0; i < range.numParagraphs(); i++) {
				Paragraph par = range.getParagraph(i);

				if (par.isInTable()) {
					try {
						Table table = range.getTable(par);
						this.addTable(LASTable.createLASTable(table));
					} catch (Exception e) {
						// do nothing.
					}
				} else {
					this.appendText(par.text());
				}
			}
			doc.close();

			this.addURL(TikaUtilities.getURLs(this.getCurrentDocument().getContentData()));
		} catch (IOException e1) {
			e1.printStackTrace();  // TODO: Change to logger
		}
		
		if (this.getExtractedText().trim().length() ==0 ) {
			this.appendText(TikaUtilities.extractText(this.getCurrentDocument().getContentData()));
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
		return "Extracts text, tables, and URLs from older Microsoft Word documents";
	}

	@Override
	public int getProcessingOrder() {
		return 100;
	}

}
