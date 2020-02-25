package edu.ncsu.las.geo.api;

/**
 * Represents the different configuration parameters in the application_properties
 * 
 * TODO: we need to look at how to better provide this configuration from a docker perspective....
 * 
 */
public enum Configuration {
		
	GEO_API("geo_api",true,null,"",false),
	GEO_API_SERVICE_URL("service_url",true,GEO_API,"what is the URL used to service client requests.  Use a format of http://0.0.0.0:9001/geo/  to listen on all network hosts, otherwise specify a name or IP address",false),
	GEO_API_CACHE_SIZE("cacheSize",true,GEO_API,"What are the maximum number of responses that can be cached?", false),
	GEO_API_MAX_SLEEP_TIME("maxSleepTime",true,GEO_API,"What is the maximum number of milliseconds to sleep in between the requests.  This should be related to the fatest request rate", false),
	GEO_PROVIDER("providers",true,null,"JSON array that lists the different providers this service may use to return geocoding results",true),
	GEO_PROVIDER_MAX_PER_DAY("maxPerDay",true,GEO_PROVIDER,"Maximum number of requests that can be sent to this provider daily",false),
	GEO_PROVIDER_NAME("name",true,GEO_PROVIDER,"name of the current provider",false),
	GEO_PROVIDER_RESTAPI("restEndPoint",true,GEO_PROVIDER,"What is the rest endpoint for this provider.  This should include the api key.  Assume that the query string will be appended to this.",false),
	GEO_PROVIDER_REQUEST_RATE("requestRate",true,GEO_PROVIDER,"Request rate per second.  Specify as a double value", false),
	GEO_PROVIDER_USER_AGENT("userAgent",true,GEO_PROVIDER,"String value to use the user-agent field when making a request", false),
	GEO_PROVIDER_PARENT_ARRAY("parentArray",true, GEO_PROVIDER,"We assume the results are returned in an array.  If a field is necessary to access this array, specify it here.  Otherwise, leave blank.",false),
	GEO_PROVIDER_LATITUDE_FIED("latitudeField",true, GEO_PROVIDER,"What field contains the latitude?",false),
	GEO_PROVIDER_LONGITUDE_FIED("longitudeField",true, GEO_PROVIDER,"What field contains the longitude?",false);
	
	private String _label;
	private boolean _required;
	private Configuration _parentConfiguration;
	private String _description;
	private boolean _jsonArray;

	private Configuration(String label, boolean required, Configuration parent, String description, boolean jsonArray) {
		_label = label;
		_required = required;
		_parentConfiguration = parent;
		_description = description;
		_jsonArray   = jsonArray;
	}
	
	public String getLabel() { return _label; }
	
	public String toString() { return _label; }
	
	public boolean isRequired() { return _required; }
	
	public Configuration getParentConfiguration() {
		return _parentConfiguration;
	}
	
	public boolean isJSONArray() { return _jsonArray; }
	
	public String getDescription() {
		return _description;
	}
	
	public static Configuration getEnum(String label) {
		return Configuration.valueOf(label.toUpperCase());
	}
}
