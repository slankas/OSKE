package edu.ncsu.las.model.collector.type;

/**
 * Rating of a source's reliability
 * 
 * When storing, using the name() field
 * 
 *
 */

public enum SourceReliabilityCode {
		
	//These are table 3.2 in Scientific methods of inquiry for intelligence analysis
	
	A("A","Completely Reliable",1),
	B("B","Usually Reliable",0.8),
	C("C","Fairly Reliable",0.6),
	D("D","Not Usually Reliable",0.4),
	E("E","Unreliable",0.2),
	F("F","Unintentionally Misleading",0),
	G("G","Deliberately Deceptive",0),
	H("H","Cannot be Judged",.5);
	
	
	private String _code;
	private String _description;
	private double _estimatedTruthRating;
	private String _label;

	private SourceReliabilityCode(String code, String description, double estimatedTruthRating) {
		_code = code;
		_description = description;
		_estimatedTruthRating = estimatedTruthRating;
		_label = _code +" - " + _description;
	}
	
	public String toString() { return _label; }
	
	public String getCode() { return _code; }
	
	public String getDescription() { return _description; }
	
	public double getEstimatedTruthRating() { return _estimatedTruthRating; }
	
	public static SourceReliabilityCode getEnum(String label) {
		return SourceReliabilityCode.valueOf(label.toUpperCase());
	}
}
