package edu.ncsu.las.storage.export;

import org.json.JSONObject;

/**
 * Represents an ElasticSearch record from a search query
 * 
 */
public class ElasticSearchRecord {

	private JSONObject _record;
	
	public ElasticSearchRecord(JSONObject record) {
		_record = record;
	}
	
	public String getIndex() {
		return _record.getString("_index");
	}
	
	public String getType() {
		return _record.getString("_type");
	}
	
	public String getID() {
		return _record.getString("_id");
	}
	
	public double getScore() {
		return _record.getDouble("_scoure");
	}

	/** 
	 * This is the primary record that is stored in ElasticSerach.  Maintaining their language
	 * rather than calling this a record, or primary record.
	 * 
	 * @return
	 */
	public JSONObject getSource() {
		return _record.getJSONObject("_source");
	}

}
