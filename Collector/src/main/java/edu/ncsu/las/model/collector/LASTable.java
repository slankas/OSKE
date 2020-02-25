package edu.ncsu.las.model.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xwpf.usermodel.ICell;
import org.apache.poi.xwpf.usermodel.XWPFSDTCell;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import edu.ncsu.las.util.Serializer;

/*
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
*/

//TODO: Add columns
//TODO: Figure out column types

/**
 * 
 * @
 *
 */
public class LASTable implements Serializable {
	private static Logger logger = Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<List<String>> rows = new ArrayList<>();

	private String tableName;

	public List<List<String>> getRows() {
		return rows;
	}

	public void setRows(List<List<String>> rows) {
		this.rows.addAll(rows);
	}

	public void addRow(List<String> row) {
		this.rows.add(row);
	}

	@Override
	public String toString() {
		return rows.toString();
	}

	/**
	 * Create our table representation from a Excel sheet (.xls)
	 * 
	 * @param e
	 * @return
	 */
	public static LASTable createLASTable(HSSFSheet sheet) {
		LASTable lasTable = new LASTable();
		//System.out.println("\n" + sheet.getSheetName());

		// Header(s), if present
		// System.out.println(sheet.getHeader());
		// Rows and cells
		for (Object rawR : sheet) {
			List<String> tableCells = new ArrayList<>();
			Row row = (Row) rawR;
			//System.out.println("");
			for (Iterator<Cell> ri = row.cellIterator(); ri.hasNext();) {
				Cell cell = ri.next();
				//System.out.print(cell.toString() + "\t");
				tableCells.add(cell.toString());
			}
			lasTable.addRow(tableCells);
		}

		lasTable.setTableName(sheet.getSheetName());
		return lasTable;
	}

	/**
	 * Create our table representation from a Excel sheet (.xlsx)
	 * 
	 * @param e
	 * @return
	 */
	public static LASTable createLASTable(XSSFSheet sheet) {
		LASTable lasTable = new LASTable();
		//System.out.println("\n" + sheet.getSheetName());

		// Header(s), if present
		// System.out.println(sheet.getHeader());
		// Rows and cells
		for (Object rawR : sheet) {
			List<String> tableCells = new ArrayList<>();
			Row row = (Row) rawR;
			//System.out.println("");
			for (Iterator<Cell> ri = row.cellIterator(); ri.hasNext();) {
				Cell cell = ri.next();
				//System.out.print(cell.toString() + "\t");
				tableCells.add(cell.toString());
			}
			lasTable.addRow(tableCells);
		}

		lasTable.setTableName(sheet.getSheetName());
		return lasTable;
	}

	/**
	 * Create our table representation from a word document table. (doc)
	 * 
	 * @param e
	 * @return
	 */
	public static LASTable createLASTable(org.apache.poi.hwpf.usermodel.Table table) {
		LASTable lasTable = new LASTable();
		for (int rowIdx = 0; rowIdx < table.numRows(); rowIdx++) {
			List<String> tableCells = new ArrayList<>();
			TableRow row = table.getRow(rowIdx);
			// System.out.println("row " + (rowIdx + 1) + ",is table header: " +
			// row.isTableHeader());
			for (int colIdx = 0; colIdx < row.numCells(); colIdx++) {
				TableCell cell = row.getCell(colIdx);
				// System.out.println("column " + (colIdx + 1) + ",text= " +
				// cell.getParagraph(0).text());
				tableCells.add(cell.getParagraph(0).text());
			}
			lasTable.addRow(tableCells);
		}

		return lasTable;
	}

	/**
	 * Create our table representation from a ppt document table.
	 * 
	 * @param e
	 * @return
	 */
	public static LASTable createLASTable(org.apache.poi.hslf.usermodel.HSLFTable table) {
		LASTable lasTable = new LASTable();

		for (int i = 0; i < table.getNumberOfColumns(); i++) {
			List<String> tableCells = new ArrayList<>();
			for (int j = 0; j < table.getNumberOfRows(); j++) {
				tableCells.add(table.getCell(i, j).getText());
			}
			lasTable.addRow(tableCells);
		}

		return lasTable;
	}

	/**
	 * Create our table representation from a pptx document table.
	 * 
	 * @param e
	 * @return
	 */
	public static LASTable createLASTable(XSLFTable table) {
		LASTable lasTable = new LASTable();

		for (XSLFTableRow row : table.getRows()) {
			List<String> tableCells = new ArrayList<>();
			for (XSLFTableCell cell : row.getCells()) {
				tableCells.add(cell.getText());
			}
			lasTable.addRow(tableCells);
		}

		return lasTable;
	}

	/**
	 * Create our table representation from a word document table. (docx)
	 * 
	 * @param e
	 * @return
	 */
	public static LASTable createLASTable(XWPFTable e) {
		LASTable lasTable = new LASTable();
		for (XWPFTableRow row : (e).getRows()) {
			List<String> tableCells = new ArrayList<String>();
			List<ICell> cells = row.getTableICells();
			for (int i = 0; i < cells.size(); i++) {
				ICell cell = cells.get(i);
				if (cell instanceof XWPFTableCell) {
					tableCells.add(((XWPFTableCell) cell).getTextRecursively());
				} else if (cell instanceof XWPFSDTCell) {
					tableCells.add(((XWPFSDTCell) cell).getContent().getText());
				}
			}
			lasTable.addRow(tableCells);
		}
		return lasTable;
	}

	
	/**
	 * Create our table representation from a tabula table.
	 * 
	 * @param e
	 * @return
	 */
	/*
	public static LASTable createLASTable(Table table) {

		LASTable lasTable = new LASTable();
		for (List<RectangularTextContainer> row : table.getRows()) {
			List<String> cells = new ArrayList<String>(row.size());
			for (RectangularTextContainer tc : row) {
				cells.add(tc.getText());
			}
			lasTable.addRow(cells);
		}

		return lasTable;
	}
	*/

	public byte[] toByteArray() {
		try {
			return Serializer.serialize(this);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to serialize LASTable: " + e);
			return null;
		}
	}

	public static LASTable fromByteArray(byte[] data) {
		try {
			return (LASTable) Serializer.deserialize(data);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to de-serialize LASTable: " + e);
			return null;
		}
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

}
