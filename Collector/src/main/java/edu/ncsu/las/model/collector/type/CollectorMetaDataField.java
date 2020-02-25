package edu.ncsu.las.model.collector.type;

/**
 * Represents the different meta-data fields that are used for internal processing within the router.
 * 
 *
 */
public enum CollectorMetaDataField {
	URL("sourceURL"),               // what is the source URL of the record being processed.  String
	DOMAIN("domain"),               // job has processed successfully and can run again.  String
	SOURCE_UUID("sourceUUID"),      // what was the UUID used to store the file on the file system/HDFS?   String
	REFERRING_URL("referringURL"),  // What URL was the link to the current file.  String
	HTTP_HEADERS("HTTP_HEADERS");   // Stores of HashMap<String,String>
	
	private String _label;

	private CollectorMetaDataField(String label) {
		_label = label;
	}
	
	public String toString() { return _label; }
	
	public static CollectorMetaDataField getEnum(String label) {
		return CollectorMetaDataField.valueOf(label.toUpperCase());
	}
}
