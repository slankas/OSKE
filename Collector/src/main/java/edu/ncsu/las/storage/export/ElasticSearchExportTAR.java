package edu.ncsu.las.storage.export;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.Export;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.Export.Format;
import edu.ncsu.las.storage.ElasticSearchREST;

public class ElasticSearchExportTAR implements ElasticSearchRecordProcessor  {
	private static Logger logger =Logger.getLogger(ElasticSearchExportTAR.class.getName());
	
	public static final long ERROR_UNKNOWN_QUERY_TYPE = -10;
	
	private String _domain;
	private FileStorageAreaType _area; 
	private JSONObject _queryClause;
	private JSONObject _optionObject; 
	private String _outputFile;
	private Format _format;
	private long _maxRecords; 
	private boolean _removeNonASCII;
	private boolean _stemText;
	
	private long _numRecordsProcessed = 0;
	private TarArchiveOutputStream _tarOS;
	
	public ElasticSearchExportTAR(String domain, FileStorageAreaType area, JSONObject queryClause, JSONObject optionObject, String outputFile, Format format, long maxRecords, boolean removeNonASCII) throws IllegalArgumentException {
		if (format != Export.Format.IND_TEXT_EXPANDED && format != Export.Format.IND_TEXT_ONLY && format != Export.Format.JSON_FILE) {
			logger.log(Level.SEVERE,"Invalid export format : " + format);
			throw new IllegalArgumentException("Invalid export format : " + format);
		}
		
		_domain         = domain;
		_area           = area; 
		_queryClause    = queryClause;
		_optionObject   = optionObject; 
		_outputFile     = outputFile;
		_format         = format;
		_maxRecords     = maxRecords; 
		_removeNonASCII = removeNonASCII;		
	}
	
	/**
	 * 
	 * @return  the number of files exported.
	 */
	public long export() {
		logger.log(Level.INFO, "Export to tar, writing to " + _outputFile);
		
		
		if (_optionObject.optString("stem", "false").equalsIgnoreCase("true")) { _stemText = true;}

		try {
			_tarOS = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(_outputFile)));
			_tarOS.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			
			ElasticSearchREST.searchQueryForAllResultsUsingScroll(_domain, _area, _queryClause, _maxRecords, this);
			
			_tarOS.flush();
			_tarOS.close();
			
		}
		catch (Throwable t) {
			logger.log(Level.SEVERE, "TAR Export issue: ",t);
		}
		logger.log(Level.INFO, "Number of export query results found: "+_numRecordsProcessed);
		return _numRecordsProcessed;
	}



	@Override
	public void processRecord(ElasticSearchRecord recordESR) throws IOException {
		
		_numRecordsProcessed++;

		JSONObject record =  recordESR.getSource(); 
		OpenKETarArchiveEntry tae = OpenKETarArchiveEntry.createTarArchiveEntry(record, _format, _optionObject.getString("grouping"), _optionObject.getString("naming"), _optionObject.optBoolean("eliminateNonSentences",false),_removeNonASCII,_stemText);
			        
		if (tae != null) {
			_tarOS.putArchiveEntry(tae);
			_tarOS.write(tae.getContent());
		    _tarOS.closeArchiveEntry();
		}			        

	}	
	



}
