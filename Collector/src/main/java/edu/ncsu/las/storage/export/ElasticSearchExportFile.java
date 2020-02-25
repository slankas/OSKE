package edu.ncsu.las.storage.export;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.Export;
import edu.ncsu.las.model.collector.type.Export.Format;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.storage.ElasticSearchREST;

public class ElasticSearchExportFile implements ElasticSearchRecordProcessor {
	private static Logger logger =Logger.getLogger(ElasticSearchExportFile.class.getName());
	
	public static final long ERROR_UNKNOWN_QUERY_TYPE = -10;
	
	private String _domain;
	private FileStorageAreaType _area; 
	private JSONObject _queryClause; 
	private String _outputFile;
	private Export.Format _format; 
	private long _maxRecords;
	
	private long _numRecordsProcessed = 0;
	private PrintWriter _pw = null;
	private CSVPrinter _csvFilePrinter = null;
	
	public ElasticSearchExportFile (String domain, FileStorageAreaType area, JSONObject queryClause, String outputFile, Export.Format format, long maxRecords) throws IllegalArgumentException {
		if (format != Export.Format.CSV  && format != Export.Format.TAB  && format != Export.Format.JSON_ARRAY && format != Export.Format.JSON_OBJ_LINE) {
			logger.log(Level.WARNING,"Invalid export format : " + format);
			throw new IllegalArgumentException("Invalid export format : " + format);
		}
		
		_domain = domain;
		_area = area; 
		_queryClause = queryClause; 
		_outputFile  = outputFile;
		_format      = format; 
		_maxRecords  = maxRecords;
	}
	
	/**
	 * Performs the export based upon the criteria established through the constructor.
	 * 
	 * @return -1 on error.  
	 */
	public long export() {
		logger.log(Level.INFO, "Export to file, writing to " + _outputFile);
		
		
		try {
			
			if (_format == Export.Format.JSON_ARRAY || _format == Export.Format.JSON_OBJ_LINE ) {
				_pw = new PrintWriter(_outputFile);
			}
			else if (_format == Export.Format.CSV) {
				CSVFormat csvFileFormat = CSVFormat.RFC4180;
				FileWriter fw = new FileWriter(_outputFile);
				_csvFilePrinter = new CSVPrinter(fw, csvFileFormat);
			}
			else if (_format == Export.Format.TAB) {
				CSVFormat csvFileFormat = CSVFormat.TDF;
				FileWriter fw = new FileWriter(_outputFile);
				_csvFilePrinter = new CSVPrinter(fw, csvFileFormat);
			}
			
			logger.log(Level.INFO, "Starting export - query: "+ _queryClause.toString());
			printHeader(_pw,_csvFilePrinter,_format);
			ElasticSearchREST.searchQueryForAllResultsUsingScroll(_domain, _area, _queryClause, _maxRecords, this);
			printFooter(_pw,_csvFilePrinter,_format);
			
			if (_pw != null) {
				_pw.flush();
				_pw.close();
			}
			if (_csvFilePrinter != null) {
				_csvFilePrinter.flush();
				_csvFilePrinter.close();
			}
		}
		catch (Throwable t) {
			logger.log(Level.SEVERE, "File Export issue: ",t);
		}
		logger.log(Level.INFO, "Number of export query results processed: "+_numRecordsProcessed);
		return _numRecordsProcessed;
	}

	@Override
	public void processRecord(ElasticSearchRecord record) throws IOException {

		_numRecordsProcessed++;
		if (_numRecordsProcessed > 1) {
			printRecordSeparator(_pw,_csvFilePrinter, _format);
		}
		JSONObject recordObj = record.getSource();
		printRecord(_pw,_csvFilePrinter,_format,recordObj);		
	}

	
	public void printRecord(PrintWriter pw, CSVPrinter csvFilePrinter, Format format, JSONObject document) throws IOException {
		if (format == Format.JSON_ARRAY || format == Format.JSON_OBJ_LINE) {
			pw.println(document.toString());
		}
		else if (format == Format.CSV || format == Format.TAB) {
	        Object[] row = new Object[7];
	        row[0] = document.getString("source_uuid");
	        try {
	        	row[1] = document.getJSONObject("http_headers").getString("CRAWL_PARENT_URL");
	        }
	        catch (org.json.JSONException ex) {
	        	row[1] = "";
		    }
	        row[2] = document.getString("mime_type");
	        row[3] = document.getString("crawled_dt");
	        row[4] = document.getString("domain");
	        row[5] = document.getString("url");
	        row[6] = document.getString("text").replace("\n", " ").replace("\r", " ").replace("\t", " ");

	        csvFilePrinter.printRecord(row);
		}
		else {
			// Do nothing
		}
	}

	private void printHeader(PrintWriter pw, CSVPrinter csvFilePrinter, Format format) throws IOException {
		if (format == Format.JSON_ARRAY) {
			pw.println("[");
		}
		else if (format == Format.CSV || format == Export.Format.TAB) {
			Object [] FILE_HEADER = {"id","referrer","mime_type","crawled_dt","domain","url","text"};
			csvFilePrinter.printRecord(FILE_HEADER);
		}
		else {
			// Do nothing
		}
		
	}
	
	private void printRecordSeparator(PrintWriter pw, CSVPrinter csvFilePrinter, Format format) {
		if (format == Format.JSON_ARRAY) {
			pw.println(",");
		}
		else if (format == Format.CSV) {
			// do nothing
		}
		else  if (format == Export.Format.TAB) { 
			// Do nothing
		}
		else {
			// Do nothing
		}
		
	}	
	
	private void printFooter(PrintWriter pw, CSVPrinter csvFilePrinter, Format format) {
		if (format == Format.JSON_ARRAY) {
			pw.println("]");
		}
		else if (format == Format.CSV) {
			// do nothing
		}
		else  if (format == Export.Format.TAB) { 
			// Do nothing
		}
		else {
			// Do nothing
		}
		
	}




}
