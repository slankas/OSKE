package edu.ncsu.las.document;

import java.time.Instant;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.util.json.JSONUtilities;

public class ForumUserMerger implements JSONDocumentMerger {
	
	private void priorValueFieldObject (String fieldName, JSONArray priorValues, String current, String previous, String previousDate) {
		if (current.equals(previous) == false) {
			JSONObject prevObject = new JSONObject().put(fieldName, previous)
					                                .put("crawled_dt", previousDate);
			priorValues.put(prevObject);
		}
	}
	
	private JSONArray mergeMemberLists(JSONArray currentArray, JSONArray priorArray) {
		if (currentArray == null) {
			return priorArray;
		}
		if (priorArray == null) {
			return currentArray;
		}
		//If in the prior list, copy the firstSeenDt to the current record
		//if not in current list, copy that record across 
		JSONObject hashedPrior   = JSONUtilities.toMappedJSONObject(priorArray, "user_id");
		JSONObject hashedCurrent = JSONUtilities.toMappedJSONObject(currentArray, "user_id");
		
		for (int i=0;i<currentArray.length();i++) {
			JSONObject currMember = currentArray.getJSONObject(i);
			if (hashedPrior.has(currMember.getString("user_id"))) {
				JSONObject priorMember = hashedPrior.getJSONObject(currMember.getString("user_id"));
				currMember.put("firstSeen_dt", priorMember.getString("firstSeen_dt"));
			}
		}
		
		for (int i=0;i<priorArray.length();i++) {
			JSONObject priorMember = priorArray.getJSONObject(i);
			if (hashedCurrent.has(priorMember.getString("user_id")) == false) {
				currentArray.put(priorMember);
			}
		}
		return currentArray;
	}

	@Override
	public JSONObject mergeMemberRecords(JSONObject current, JSONObject previous) {
		JSONObject result = new JSONObject(current.toString());
		
		JSONArray priorValues = new JSONArray();
		if (previous.has("priorValues")) {
			priorValues = previous.getJSONArray("priorValues");
		}
		result.put("priorValues", priorValues);
		String priorCrawlDate = previous.has("latest_crawled_dt") ? previous.getString("latest_crawled_dt") : Instant.now().toString();
		
		if (previous.has("first_crawled_dt")) { result.put("first_crawled_dt", previous.get("first_crawled_dt")); }
		
		this.priorValueFieldObject("user_name", priorValues, result.optString("user_name",""), previous.optString("user_name", ""), priorCrawlDate);
		this.priorValueFieldObject("user_title", priorValues, result.optString("user_title",""), previous.optString("user_title", ""), priorCrawlDate);
		
		this.priorValueFieldObject("avatar_image", priorValues, result.optString("avatar_image",""), previous.optString("avatar_image", ""), priorCrawlDate);
		this.priorValueFieldObject("reputation", priorValues, result.optString("reputation",""), previous.optString("reputation", ""), priorCrawlDate);
		this.priorValueFieldObject("visitCount", priorValues, result.optString("visitCount",""), previous.optString("reputation", ""), priorCrawlDate);
		
		JSONObject priorProfile = previous.has("profile") ? previous.getJSONObject("profile") : new JSONObject();
		JSONObject currentProfile = previous.has("profile") ? previous.getJSONObject("profile") : new JSONObject();
		for (String key: priorProfile.keySet()) {
			this.priorValueFieldObject("profile_"+key, priorValues, currentProfile.optString(key,""), priorProfile.optString(key, ""), priorCrawlDate);
		}
		
		//photostream.  if previous contains values not in current add them...
		if (result.has("photoStream")) {
			java.util.HashSet<String> currentPhotos = JSONUtilities.toStringHashSet(result.getJSONArray("photoStream"));
			if (previous.has("photoStream")) {
				JSONArray priorPhotoStream = previous.getJSONArray("photoStream");
				for (int i=0;i<priorPhotoStream.length();i++) {
					if (currentPhotos.contains(priorPhotoStream.getString(i)) == false) {
						result.getJSONArray("photoStream").put(priorPhotoStream.getString(i));
					}
				}
			}
		}
		else {
			if (previous.has("photoStream")) {
				current.put("photoStream", previous.getJSONArray("photoStream"));
			}
		}
	
		JSONArray mergedList = this.mergeMemberLists(result.optJSONArray("friends"),previous.optJSONArray("friends"));
		if (mergedList != null) {
			result.put("friends", mergedList);
		}

		JSONArray mergedVisitorList = this.mergeMemberLists(result.optJSONArray("recentVisitors"),previous.optJSONArray("recentVisitors"));
		if (mergedVisitorList != null) {
			result.put("recentVisitors", mergedVisitorList);
		}
				
		
		if (result.has("albums")) {
			JSONObject hashCurrentAlbums = JSONUtilities.toMappedJSONObject(result.getJSONArray("albums"), "albumID");
			if (previous.has("albums")) {
				JSONArray priorAlbums = previous.getJSONArray("albums");
				for (int i=0; i<priorAlbums.length(); i++) {
					JSONObject priorAlbum = priorAlbums.getJSONObject(i);
					if (hashCurrentAlbums.has(priorAlbum.getString("albumID")) == false) {
						result.getJSONArray("albums").put(priorAlbum);
					}
				}
			} // nothhing to do if we currently have albums, but the past has none...
		}
		else {
			if (previous.has("albums")) {
				current.put("albums", previous.getJSONArray("albums"));
			}
		}
			
		return result;

		
	}

}
