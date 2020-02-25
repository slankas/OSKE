package edu.ncsu.las.document;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.LASTable;

/**
 * Used to process Office 2007 and new excel files
 * 
 * TODO complete this Need to include possible configuration.
 * 
 */
public class ExcelXHandler extends DocumentHandler {
	
	private static String[] MIME_TYPE = { "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.template",
			"application/vnd.ms-excel.addin.macroEnabled.12", "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
			"application/vnd.ms-excel.sheet.macroEnabled.12", "application/vnd.ms-excel.template.macroEnabled.12" };

	protected void processDocument() {

		try (InputStream input = new java.io.ByteArrayInputStream(this.getCurrentDocument().getContentData())) {

			XSSFWorkbook workbook = new XSSFWorkbook(input);

			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				XSSFSheet sheet = workbook.getSheetAt(i);
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
		return "extracts tables for spreadsheets in new Excel (2007+) files";
	}

	@Override
	public int getProcessingOrder() {
		return 100;
	}

}
