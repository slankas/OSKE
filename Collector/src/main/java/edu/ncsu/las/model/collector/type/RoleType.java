package edu.ncsu.las.model.collector.type;


public enum RoleType {
		
	ADMINISTRATOR("administrator"),    // 
	ADJUDICATOR("adjudicator"),        // 
	ANALYST("analyst");                // 
	
	private String _label;

	private RoleType(String label) {
		_label = label;
	}
	
	public String toString() { return _label; }
	
	public static RoleType getEnum(String label) {
		return RoleType.valueOf(label.toUpperCase());
	}
}
