package edu.ncsu.las.model.collector.type;


public enum SourceParameterType {
		
	UNKNOWN("unknown"),
	BOOLEAN("boolean"),
	DOUBLE("double"),
	INT("int"),
	JSON_OBJECT("jsonObject"),
	JSON_ARRAY("jsonArray"),
	LONG("long"),
	OBJECT("object"),
	REGEX("regex"),
	STRING("string"),
	STRING_TIMESTAMP("timestamp");
	
	private String _label;

	private SourceParameterType(String label) {
		_label = label;
	}
	
	public String toString() { return _label; }

	
	public static SourceParameterType getEnum(String label) {
		return SourceParameterType.valueOf(label.toUpperCase());
	}
}
