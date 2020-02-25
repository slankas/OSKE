package edu.ncsu.las.document;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;

/**
 * Used to process Office 2003 and old excel files
 * 
 * TODO complete this Need to include possible configuration.
 * 
 */
public class ExcelHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { "application/vnd.ms-excel" };

	protected void processDocument() {

		try (InputStream input = new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())) {

			HSSFWorkbook workbook = new HSSFWorkbook(input);

			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				HSSFSheet sheet = workbook.getSheetAt(i);
				this.addTable(LASTable.createLASTable(sheet));
			}

			workbook.close();

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
		return "Extracts a table for each sheet in the Excel file";
	}

	@Override
	public int getProcessingOrder() {
		return 100;
	}

}
