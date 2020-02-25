package edu.ncsu.las.model.collector.type;


/**
 * Rating of how true a piece of information is.
 * 
 * When storing, using the name() field
 * 
 *
 */
public enum InformationAccuracyCode {
		
	//These are table 3.2 in Scientific methods of inquiry for intelligence analysis
	
	IA_1("1","Confirmed",1),
	IA_2("2","Probably True",0.8),
	IA_3("3","Fossibly True",0.6),
	IA_4("4","Doubtful",0.4),
	IA_5("5","Improbable",0.2),
	IA_6("6","Misinformation",0),           // unintentional, just not right
	IA_7("7","Disinformation",0),           // intentional, outright deception, deliberate
	IA_8("8","Cannot be Judged",.5);
	
	
	private String _code;
	private String _description;
	private double _estimatedTruthRating;
	private String _label;

	private InformationAccuracyCode(String code, String description, double estimatedTruthRating) {
		_code = code;
		_description = description;
		_estimatedTruthRating = estimatedTruthRating;
		_label = _code +" - " + _description;
	}
	
	public String toString() { return _label; }
	
	public String getCode() { return _code; }
	
	public String getDescription() { return _description; }
	
	public double getEstimatedTruthRating() { return _estimatedTruthRating; }
	
	public static InformationAccuracyCode getEnum(String label) {
		return InformationAccuracyCode.valueOf(label.toUpperCase());
	}
}
