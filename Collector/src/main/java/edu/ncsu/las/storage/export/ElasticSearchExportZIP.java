package edu.ncsu.las.storage.export;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.json.JSONObject;

import edu.ncsu.las.model.collector.type.Export;
import edu.ncsu.las.model.collector.type.Export.Format;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.storage.ElasticSearchREST;

public class ElasticSearchExportZIP implements ElasticSearchRecordProcessor  {
	private static Logger logger =Logger.getLogger(ElasticSearchExportZIP.class.getName());
	
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
	private ZipOutputStream _zipStream;
	
	public ElasticSearchExportZIP(
			String domain, 
			FileStorageAreaType area, 
			JSONObject queryClause, 
			JSONObject optionObject, 
			String outputFile, 
			Format format, 
			long maxRecords, 
			boolean removeNonASCII
			) throws IllegalArgumentException {
		
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
		logger.log(Level.INFO, "Export to zip, writing to " + _outputFile);
		
		try {
			_zipStream = new ZipOutputStream(new FileOutputStream(_outputFile));
			
			ElasticSearchREST.searchQueryForAllResultsUsingScroll(_domain, _area, _queryClause, _maxRecords, this);
			_zipStream.close();
			
		}
		catch (Throwable t) {
			logger.log(Level.SEVERE, "ZIP Export issue: ",t);
		}
		
		logger.log(Level.INFO, "Number of export query results found: "+_numRecordsProcessed);
		return _numRecordsProcessed;
	}



	@Override
	public void processRecord(ElasticSearchRecord recordESR) throws IOException {
		
		_numRecordsProcessed++;

		JSONObject record =  recordESR.getSource(); 
		
		
		Map<String, byte[]> zae =  OpenKEZipArchiveEntry.createZipEntry(record, _format, "noGroup", "uuid", false,false,false);
			        
		if (zae != null) {
			for (Map.Entry<String, byte[]> entry : zae.entrySet())
			{
				String name = entry.getKey();
				byte[] content = (byte[]) entry.getValue();
			    System.out.println("name / content=" + name + "/" + content);
			    ZipEntry ze = new ZipEntry(name);
			    ze.setComment("crawled_dt:"+record.getString("crawled_dt"));
			    _zipStream.putNextEntry(ze);
			    _zipStream.write(content);
			}
			
		}			        

	}	
	



}
