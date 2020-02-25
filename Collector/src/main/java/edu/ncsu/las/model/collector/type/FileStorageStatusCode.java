package edu.ncsu.las.model.collector.type;


public enum FileStorageStatusCode {
		
	SUCCESS("success"),                 // 
	UNKNOWN_FAILURE("unknown failure"), // 
	FILE_NOT_FOUND("file not found"),   // 
	INVALID_CONTENT("invalid file content"),
	NOT_EXECUTED("not exectued"),
	SKIPPED("skipped"),
    ALREADY_PRESENT("content exists"); 
	
    
	
	private String _label;

	private FileStorageStatusCode(String label) {
		_label = label;
	}
	
	public String toString() { return _label; }
	
	public static FileStorageStatusCode getEnum(String label) {
		return FileStorageStatusCode.valueOf(label.toUpperCase());
	}
}
