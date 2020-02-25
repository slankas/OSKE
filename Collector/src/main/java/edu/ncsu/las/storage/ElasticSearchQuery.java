package edu.ncsu.las.storage;


import java.util.UUID;

import org.json.JSONObject;



/**
 * Contains miscellaneous queries for use in ElasticSearch
 *
 */
public class ElasticSearchQuery {
	//private static Logger logger =Logger.getLogger(ElasticSearchQuery.class.getName());

	
	
	/**
	 * Search ElasticSearch for records that came from a particular jobID
	 * @param jobID
	 * @return
	 */
	public static JSONObject createSelectByJobUUID(UUID jobID) {
		JSONObject termObject = new JSONObject().put("provenance.job.id.keyword",jobID.toString());
		JSONObject queryObject = new JSONObject().put("term", termObject);
		JSONObject result = new JSONObject().put("query", queryObject);
		
		return result;
		
		
	}
}
