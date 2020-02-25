package edu.ncsu.las.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class Export {

	
	public static String convertJSONArrayToTabDelimited(JSONArray jsonData, String[] jsonColumnNames, String[] headers, boolean printHeaderLine) {
		return convertJSONArrayToTabDelimited(jsonData,jsonColumnNames,headers,printHeaderLine,null);
	}
	
	public static String convertJSONArrayToTabDelimited(JSONArray jsonData, String[] jsonColumnNames, String[] headers, boolean printHeaderLine, String documentID) {
		if (jsonColumnNames.length != headers.length) {
			throw new IllegalArgumentException("jsonColumnNames and headers must be the same length");
		}
		
		StringBuilder result = new StringBuilder();
		
		if (printHeaderLine) {
			if (documentID != null) {
				result.append("DocumentID\t");
			}
			
			result.append( String.join("\t", headers));
			result.append("\n");
		}
		
		for (int recordNumber=0; recordNumber<jsonData.length(); recordNumber++) {
			JSONObject row = jsonData.getJSONObject(recordNumber);
			
			if (documentID != null) {
				result.append(documentID.toString());
				result.append("\t");
			}
			
			for (int colNumber=0; colNumber <jsonColumnNames.length; colNumber++) {
				if (colNumber>0) { result.append("\t"); }
				result.append(row.get(jsonColumnNames[colNumber]));
			}
			result.append("\n");
		}
		return result.toString();
	}	
	
}
