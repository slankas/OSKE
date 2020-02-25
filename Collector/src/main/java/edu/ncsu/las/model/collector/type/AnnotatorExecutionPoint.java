package edu.ncsu.las.model.collector.type;


/**
 * Specifies when a particular annotator should execute.
 * 
 *
 */
public enum AnnotatorExecutionPoint {
	  PRE_DOCUMENT("pre-document"),
	  POST_DOCUMENT("post-document"),
	  SECONDARY("secondary"); // used for long running/time-consuming annotations such as geo-tagging
	
		private String _label;

		private AnnotatorExecutionPoint(String label) {
			_label = label;
		}
		
		public String toString() { return _label; }
		
		public static AnnotatorExecutionPoint getEnum(String label) {
			return AnnotatorExecutionPoint.valueOf(label.toUpperCase());
		}	
	
}
