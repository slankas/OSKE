package edu.ncsu.las.document;

import edu.ncsu.las.model.collector.LASTable;
import edu.ncsu.las.model.collector.type.MimeType;

/**
 * 
 */
public class TableHandler extends DocumentHandler {
	private static String[] MIME_TYPE = { MimeType.TABLE } ;
	
	/**
	 * 
	 * @param _data LASTable that has been serialize to a byte array
	 * @param metaData
	 */
	protected void processDocument() {
		LASTable table = LASTable.fromByteArray(this.getCurrentDocument().getContentData());
		
		//TODO: implement me


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
		return "Places information in tables into the graph database";
	}
	@Override
	public int getProcessingOrder() {
		return 0;
	}	
}
