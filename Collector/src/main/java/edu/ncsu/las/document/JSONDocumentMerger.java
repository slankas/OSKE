package edu.ncsu.las.document;

import org.json.JSONObject;

/**
 * Classes can implement this method to signify and have the capability
 * to merge to JSON documents together/
 * 
 */
public interface JSONDocumentMerger {

	/** 
	 * Merge the two records together.  Current is used as the intially record with 
	 * a full copy being made of that document first.
	 * 
	 * @param current
	 * @param previous
	 * 
	 * @return
	 */
	public JSONObject mergeMemberRecords(JSONObject current, JSONObject previous);
}
