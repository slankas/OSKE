package edu.ncsu.las.util.json;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class JavaxJsonUtilities {

	/**
	 * Parses a JSON object represented in a string to a javax.json.JsonObject
	 * 
	 * @param jsonObjectStr
	 * @return
	 */
	public static JsonObject toJsonObject(String jsonObjectStr) {

	    JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
	    JsonObject result = jsonReader.readObject();
	    jsonReader.close();

	    return result;
	}
}
