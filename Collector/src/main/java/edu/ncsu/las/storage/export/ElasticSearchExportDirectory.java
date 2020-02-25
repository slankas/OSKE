package edu.ncsu.las.storage.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.Export;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.Export.Format;
import edu.ncsu.las.storage.ElasticSearchREST;


public class ElasticSearchExportDirectory implements ElasticSearchRecordProcessor {
	private static Logger logger =Logger.getLogger(ElasticSearchExportDirectory.class.getName());
	
	public static final long ERROR_UNKNOWN_QUERY_TYPE = -10;
	
	private String _domain;
	private FileStorageAreaType _area; 
	private JSONObject _queryClause;
	private JSONObject _optionObject; 
	private String _exportPath;
	private Format _format;
	private long _maxRecords; 

	
	private long _numRecordsProcessed = 0;
	
	public ElasticSearchExportDirectory(String domain, FileStorageAreaType area, JSONObject queryClause, JSONObject optionObject, String exportPath, Format format, long maxRecords) throws IllegalArgumentException {
		if (format != Export.Format.IND_TEXT_EXPANDED && format != Export.Format.IND_TEXT_ONLY && format != Export.Format.JSON_FILE && format != Export.Format.JSON_OBJ_LINE) {
			logger.log(Level.SEVERE,"Invalid export format : " + format);
			throw new IllegalArgumentException("Invalid export format : " + format);
		}
		
		_domain         = domain;
		_area           = area; 
		_queryClause    = queryClause;
		_optionObject   = optionObject; 
		_exportPath     = exportPath;
		_format         = format;
		_maxRecords     = maxRecords; 	
	}	
	

	/**
	 * 
	 *
	 * @return the number of files exported.
	 */
	public long exportToDirectory() {
		logger.log(Level.INFO, "Export to Directory, writing to " + _exportPath);
				
		
		try {
			ElasticSearchREST.searchQueryForAllResultsUsingScroll(_domain, _area, _queryClause, _maxRecords, this);
			
		}
		catch (Throwable t) {
			logger.log(Level.SEVERE, "Directory Export issue: ",t);
		}
		logger.log(Level.INFO, "Number of export query results found: "+_numRecordsProcessed);
		return _numRecordsProcessed;
	}

	
	@Override
	public void processRecord(ElasticSearchRecord recordESR) throws IOException {
		
		_numRecordsProcessed++;

		JSONObject record =  recordESR.getSource(); 
		
	    OpenKETarArchiveEntry tae = OpenKETarArchiveEntry.createTarArchiveEntry(record, _format, "noGroup", "uuid",false,false,false);
	        
	    if (tae != null) { 	        
	        Path outputpath = Paths.get(_exportPath, tae.getName());
	        Files.createDirectories(outputpath.getParent());
	        while (outputpath.toFile().exists()) {
	        	String name = outputpath.toString()+ "_" + record.getString("source_uuid");
	        	outputpath = Paths.get(name);
	        }
	        java.nio.file.Files.write(outputpath, tae.getContent());

	    }		
	}	
	
	

}
