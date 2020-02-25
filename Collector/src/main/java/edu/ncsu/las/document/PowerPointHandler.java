package edu.ncsu.las.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFHyperlink;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTable;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;

/**
 * Used to process Office 2003 and old powerpoint files
 * 
 * TODO complete this Need to include possible configuration.
 * 
 */

public class PowerPointHandler extends DocumentHandler {

	public static final String[] MIME_TYPE = { "application/vnd.ms-powerpoint" };

	@Override
	protected void processDocument() {
		try (InputStream bais = new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())) {

			HSLFSlideShow ppt = new HSLFSlideShow(bais);
			java.util.List<HSLFSlide> slides = ppt.getSlides();
			for (HSLFSlide slide : slides) {
				
		        for (List<HSLFTextParagraph> paras : slide.getTextParagraphs()) {
		        	String text = HSLFTextParagraph.getText(paras);
		            this.appendText(text);
		        	
		        	for (HSLFTextParagraph para : paras) {
                        for (HSLFTextRun run : para) {
                            HSLFHyperlink link = run.getHyperlink();
                            this.addURL(link.getAddress());
                        }
		            }
		        }

		        //in PowerPoint you can assign a hyperlink to a shape without text,
		        //for example to a Line object. The code below demonstrates how to
		        //read such hyperlinks
		        for (HSLFShape sh : slide.getShapes()) {
		        	if (sh instanceof HSLFSimpleShape) {
			            HSLFHyperlink link = ((HSLFSimpleShape) sh).getHyperlink();
			            if(link != null)  {
			                //String title = link.getTitle();
			                //String address = link.getAddress();
			                this.addURL(link.getAddress());
			            }
		        	}
		        }	

				for (HSLFShape shape : slide.getShapes()) {
					if (shape instanceof HSLFTable) {
						this.addTable(LASTable.createLASTable((HSLFTable) shape));
					}
				}
			}
			ppt.close();

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if (this.getExtractedText().trim().length() ==0 ) {
			this.appendText(TikaUtilities.extractText(this.getCurrentDocument().getContentData()));
		}

		logger.log(Level.FINER, "PPT complete: " +this.getCurrentDocument().getURL());
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
		return "Extracts text, tables, and URLs from older Powerpoint files (.ppt)";
	}

	@Override
	public int getProcessingOrder() {

		return 100;
	}
}
