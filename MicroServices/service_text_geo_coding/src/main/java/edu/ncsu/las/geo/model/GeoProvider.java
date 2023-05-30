package edu.ncsu.las.geo.model;

import java.net.URLEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.google.common.util.concurrent.RateLimiter;


import edu.ncsu.las.geo.api.Configuration;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;


/**
 * Provides access to a remote API that implements the nominatum geo-coding API.
 * 
 *
 */
public class GeoProvider {
	private static Logger logger =Logger.getLogger(GeoProvider.class.getName());


	private int maxRequestsPerDay;
	private double requestRatePerSecond;
	private String userAgent;
	//private String parentArrayField;
	private String latitudeField;
	private String longitudeField;
	private String providerName;
	private String restAPI;
	
	private RateLimiter rateLimiter;
	
	private int currentDailyRequestCount = 0;
	private int totalRequestCount = 0;
	
	long minimumResponseTimeMS = Long.MAX_VALUE;
	long maximumResponseTimeMS = Long.MIN_VALUE;
	long totalResponseTimeMS   = 0;
	
	private long nextRequestCheckTime = produceNextRequestCheckTime();
	
	private static long produceNextRequestCheckTime() {
		return Instant.now().plus(1,ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
	}
	
	public GeoProvider(JSONObject configuration) {
		maxRequestsPerDay    = configuration.getInt(Configuration.GEO_PROVIDER_MAX_PER_DAY.getLabel());
		requestRatePerSecond = configuration.getDouble(Configuration.GEO_PROVIDER_REQUEST_RATE.getLabel());
		providerName         = configuration.getString(Configuration.GEO_PROVIDER_NAME.getLabel());
		restAPI              = configuration.getString(Configuration.GEO_PROVIDER_RESTAPI.getLabel());
		userAgent            = configuration.getString(Configuration.GEO_PROVIDER_USER_AGENT.getLabel());
		//parentArrayField     = configuration.getString(Configuration.GEO_PROVIDER_PARENT_ARRAY.getLabel());
		latitudeField        = configuration.getString(Configuration.GEO_PROVIDER_LATITUDE_FIED.getLabel());
		longitudeField       = configuration.getString(Configuration.GEO_PROVIDER_LONGITUDE_FIED.getLabel());
		
		rateLimiter = RateLimiter.create(requestRatePerSecond);
	}
	
	public int getMaxRequestsPerDay() {
		return maxRequestsPerDay;
	}
	
	public int getCurrentDailyRequestCount() {
		return currentDailyRequestCount;
	}

	public int getTotalRequestCount() {
		return totalRequestCount;
	}	
	
	public JSONObject createStatisticsResponse() {
		double averageTime = ((double) totalResponseTimeMS)/  Math.max((double)totalRequestCount, 1.0);

		return new JSONObject().put("provider", providerName)
				               .put("dailyLimit", maxRequestsPerDay)
				               .put("requestRate", requestRatePerSecond)
				               .put("totalRequests", totalRequestCount)
				               .put("currentDailyRequests", currentDailyRequestCount)
				               .put("minimumResponseTimeMS", minimumResponseTimeMS)
				               .put("maximumResponseTimeMS", maximumResponseTimeMS)
				               .put("totalResponseTimeMS", totalResponseTimeMS)
				               .put("averageTimeMS", averageTime);
	}
	
	/**
	 * Returns try if a call can be immediately made to process a request.  This does consume a "token"
	 * from the internal RateLimiter, but doesn't increment the request count
	 * 
	 * @return
	 */
	public boolean canProcess() {
		if (System.currentTimeMillis() > nextRequestCheckTime) {
			logger.log(Level.INFO, "Reseting daily count");
			currentDailyRequestCount = 0;
			nextRequestCheckTime = produceNextRequestCheckTime();
		}
		
		if (currentDailyRequestCount >= maxRequestsPerDay) {
			return false;
		}
		
		return rateLimiter.tryAcquire();
	}
	
	/**
	 * Calls a remote API to geocode the location String. canProcess() should be checked prior to this call.
	 * 
	 * @param location
	 * @return
	 */
	public synchronized JSONObject geocodeLocation(String location) {
		JSONObject result = new JSONObject().put("provider", providerName);
		
		logger.log(Level.INFO, "Querying "+providerName+" for "+location);
		
		currentDailyRequestCount++;
		
		
		try {
			long startTime = System.currentTimeMillis();
			String url = restAPI+URLEncoder.encode(location, "UTF-8");
			logger.log(Level.FINER, "calling URL: "+url);
			HttpResponse<JsonNode> response = Unirest.get(url).header("User-agent", userAgent).asJson();
			JsonNode body = response.getBody();
			
			long processingTime = System.currentTimeMillis() - startTime;
			minimumResponseTimeMS = Math.min(processingTime, minimumResponseTimeMS);
			maximumResponseTimeMS = Math.max(processingTime, maximumResponseTimeMS);
			totalResponseTimeMS  += processingTime;
			totalRequestCount++;
			
			if (body.isArray()) {
				JSONArray array = body.getArray();
				
				if (array.length() > 0) {
					kong.unirest.json.JSONObject record = array.getJSONObject(0);
					
					if (record.has(latitudeField) == false || record.has(longitudeField) == false) {
						logger.log(Level.SEVERE, "Missing latitude/longitude, rest api:"+url+", result: "+array.toString());
						result.put("status", "error");
						result.put("message", "record missing latitude and/or longitude");
					}
					else {
						result.put("status", "success");
						result.put("latitude", record.getDouble(latitudeField));
						result.put("longitude", record.getDouble(longitudeField));
						if (record.has("display_name")) {
							String displayName = record.getString("display_name");
							String[] parts = displayName.split(",");
							result.put("display_name",displayName);
							result.put("country", parts[parts.length-1].trim());
						}
						if (record.has("address"))     { result.put("address",record.get("address"));	}
						if (record.has("osm_id"))      { result.put("osm_id",record.get("osm_id"));		}
						if (record.has("type"))        { result.put("type",record.get("type"));		    }
						if (record.has("boundingbox")) { result.put("boundingbox",record.get("boundingbox"));		}

						result.put("originalRecord", record);
					}
				}
				else {
					result.put("status", "error");
					result.put("message", "no results found");
				}
			}
			else {
				logger.log(Level.SEVERE, "Unexpected response, rest api:"+url+", result: "+body.toString());
				result.put("status", "error");
				result.put("message", "unexpected response");
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to query provider("+providerName+"):", e);
			result.put("status", "error");
			result.put("message", e.toString());
		}
		
		return result;
	}
}
