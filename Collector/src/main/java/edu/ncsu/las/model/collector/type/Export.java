package edu.ncsu.las.model.collector.type;


public class Export {
	
	public static enum Format {	
		JSON_OBJ_LINE("JSON Line Separation","jsonObj"), // each line is its own individual JSON object
		JSON_ARRAY("JSON Array","jsonArray"),              // Export format should be a full JSON array 
		CSV("CSV","csvFile"),  // output type is a CSV file
		TAB("tab","tabFile"),  // output type is a tab-delimited file
		IND_TEXT_ONLY("Text Only Files","indTextOnly"),
		IND_TEXT_EXPANDED("Expanded Text files","indTextExp"),
		JSON_FILE("JSON File","indJSON"); // 
		
		private String _label;
		private String _webPageParam;
	
		private Format(String label, String webPageParam) {
			_label = label;
			_webPageParam = webPageParam;
		}
		
		public String toString() { return _label; }
		public String getWebPageParamValue() { return _webPageParam;}
		
		public static Format getEnum(String label) {
			return Format.valueOf(label.toUpperCase());
		}
		
		public static Format getEnumByWebPageParameter(String paramValue) {
			for (Format f: Format.values()) {
				if (f.getWebPageParamValue().equals(paramValue)) {
					return f;
				}
			}
			
			return null;
		}

		
	}
}
