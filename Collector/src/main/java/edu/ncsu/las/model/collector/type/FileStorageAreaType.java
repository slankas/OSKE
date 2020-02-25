package edu.ncsu.las.model.collector.type;


public enum FileStorageAreaType {
		
	REGULAR("normal"),    // most files would go here.
	ARCHIVE("archive"),   // no longer want to actively use data, but don't necessary delete the content
	SANDBOX("normal");    // Let the user be able to view data, but don't have it in the regular processing area.
	
	private String _label;

	private FileStorageAreaType(String label) {
		_label = label;
	}
	
	public String toString() { return _label; }
	
	public String getLabel() { return _label; }
	
	public static FileStorageAreaType getEnum(String label) {
		return FileStorageAreaType.valueOf(label.toUpperCase());
	}
}
