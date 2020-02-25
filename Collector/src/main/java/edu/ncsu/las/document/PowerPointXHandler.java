package edu.ncsu.las.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFHyperlink;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;

/**
 * Used to process Office 2007 and new powerpoint files
 * 
 */

// power point:
// https://svn.apache.org/repos/asf/poi/trunk/src/ooxml/java/org/apache/poi/xslf/extractor/XSLFPowerPointExtractor.java
public class PowerPointXHandler extends DocumentHandler {
	public static final String[] MIME_TYPE = { "application/vnd.openxmlformats-officedocument.presentationml.template",
			"application/vnd.openxmlformats-officedocument.presentationml.slideshow",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/vnd.openxmlformats-officedocument.presentationml.slide",
			"application/vnd.ms-powerpoint.presentation.macroEnabled.12",
			"application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
			"application/vnd.ms-powerpoint.template.macroEnabled.12",
			"application/vnd.ms-powerpoint.addin.macroEnabled.12",
			"application/vnd.ms-powerpoint.slide.macroEnabled.12" };

	@Override
	protected void processDocument() {
		try (InputStream input = new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())) {

			XMLSlideShow slideshow = new XMLSlideShow(input);
			List<XSLFSlide> slides = slideshow.getSlides();

			for (XSLFSlide slide : slides) {
				for (XSLFShape shape : slide.getShapes()) {
					if (shape instanceof XSLFTextShape) {
						XSLFTextShape txShape = (XSLFTextShape) shape;

						List<XSLFTextParagraph> paragraphs = txShape.getTextParagraphs();
						for (XSLFTextParagraph para : paragraphs) {
							List<XSLFTextRun> txtRuns = para.getTextRuns();
							for (XSLFTextRun txtRun : txtRuns) {
								XSLFHyperlink x = txtRun.getHyperlink();
								if (x != null) {
									this.addURL(x.getAddress());
								}
							}
						}
						for (XSLFTextParagraph xslfParagraph : txShape.getTextParagraphs()) {
							this.appendText(xslfParagraph.getText());
						}
					} else if (shape instanceof XSLFTable) {
						this.addTable(LASTable.createLASTable((XSLFTable) shape));
					}
				}

			}
			slideshow.close();
		} catch (IOException e1) {
			e1.printStackTrace();
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
		return "Extracts text, tables, and URLs from Powerpoint 2007+ files (.pptx)";
	}

	@Override
	public int getProcessingOrder() {

		return 100;
	}
}
