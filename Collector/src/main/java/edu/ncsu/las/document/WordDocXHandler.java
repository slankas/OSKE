package edu.ncsu.las.document;

import java.io.IOException;
import java.io.InputStream;

import java.util.logging.Level;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.IRunElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFSDT;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;

//Reference:
//	https://svn.apache.org/repos/asf/poi/trunk/src/ooxml/java/org/apache/poi/xwpf/extractor/XWPFWordExtractor.java

// For .docx documents

/**
 * 
 */
public class WordDocXHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.template",
			"application/vnd.ms-word.document.macroEnabled.12", "application/vnd.ms-word.template.macroEnabled.12" };

	public void processDocument() {

		try (InputStream input = new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())) {

			XWPFDocument docx = new XWPFDocument(input);

			for (IBodyElement e : docx.getBodyElements()) {
				if (e instanceof XWPFParagraph) {
					// TODO: Extract method
					for (IRunElement run : ((XWPFParagraph) e).getRuns()) {
						// System.out.println(run.toString());
						if (run instanceof XWPFHyperlinkRun) {
							XWPFHyperlink link = ((XWPFHyperlinkRun) run).getHyperlink(docx);
							if (link != null) {
								this.addURL(link.getURL());
							}
						}
					}
					this.appendText(((XWPFParagraph) e).getText());

				} else if (e instanceof XWPFTable) {
					this.addTable(LASTable.createLASTable((XWPFTable) e));
				} else if (e instanceof XWPFSDT) {
					this.appendText(((XWPFSDT) e).getContent().getText());
				}
			}

		} catch (IOException e1) {
			e1.printStackTrace();  // TODO: Change to logger
		}
		
		if (this.getExtractedText().trim().length() ==0 ) {
			this.appendText(TikaUtilities.extractText(this.getCurrentDocument().getContentData()));
		}

		logger.log(Level.FINER, "DOCX complete: " + this.getCurrentDocument().getURL());
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
		return "Extracts text, tables, and URLs from new Microsoft Word documents";
	}

	@Override
	public int getProcessingOrder() {
		return 100;
	}

}
