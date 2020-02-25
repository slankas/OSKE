package edu.ncsu.las.util.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JSONUtilities {

	public static enum JSONType {
		NUMBER, STRING, BOOLEAN, JSON_OBJECT, JSON_ARRAY, UNKNOWN;
	}

	public static JSONType getJSONType(Object o) {
		if (o.equals(Boolean.FALSE) || o.equals(Boolean.TRUE) ||
		        (o instanceof String && (((String) o).equalsIgnoreCase("false") || ((String) o).equalsIgnoreCase("true")))) {
			return JSONType.BOOLEAN;
		}
		if (o instanceof Number) {
			return JSONType.NUMBER;
		}
		if (o instanceof JSONObject) {
			return JSONType.JSON_OBJECT;
		}
		if (o instanceof JSONArray) {
			return JSONType.JSON_ARRAY;
		}
		if (o instanceof String) {
			return JSONType.STRING;
		}

		return JSONType.UNKNOWN;
	}

	public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
		Map<String, Object> retMap = new HashMap<String, Object>();

		if (json != JSONObject.NULL) {
			retMap = toMap(json);
		}
		return retMap;
	}

	public static Map<String, Object> toMap(JSONObject object) throws JSONException {
		Map<String, Object> map = new HashMap<String, Object>();

		Iterator<String> keysItr = object.keys();
		while (keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = object.get(key);

			if (value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			map.put(key, value);
		}
		return map;
	}

	public static List<Object> toList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if (value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if (value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			list.add(value);
		}
		return list;
	}

	public static List<String> toStringList(JSONArray array) throws JSONException {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < array.length(); i++) {
			list.add(array.getString(i));
		}
		return list;
	}

	public static HashSet<String> toStringHashSet(JSONArray array) throws JSONException {
		HashSet<String> set = new HashSet<String>();
		for (int i = 0; i < array.length(); i++) {
			set.add(array.getString(i));
		}
		return set;
	}

	public static List<Long> toLongList(JSONArray array) throws JSONException {
		List<Long> list = new ArrayList<Long>();
		for (int i = 0; i < array.length(); i++) {
			list.add(array.getLong(i));
		}
		return list;
	}

	public static JSONArray toJSONArray(java.util.List<Long> list) throws JSONException {
		JSONArray result = new JSONArray();
		for (Long l : list) {
			result.put(l.longValue());
		}
		return result;
	}		
	
	public static JSONArray toJSONArray(Object[] list) throws JSONException {
		JSONArray result = new JSONArray();
		for (Object o: list) {
			result.put(o.toString());
		}
		return result;
	}		
	
	
	/**
	 * Converts the passed in list to a JSONArray by converting the members to a string
	 * @param <E>
	 * 
	 * @param list
	 * @return
	 * @throws JSONException
	 */
	public static <E> JSONArray toJSONArrayAsString(java.util.List<E> list) throws JSONException {
		JSONArray result = new JSONArray();
		list.stream().forEach(e -> result.put(e.toString()));
		return result;
	}	
	
	
	/**
	 * maintains a JSON array that keeps track of historical values of a field within a JSONObject.  If the fieldValue is not defined,
	 * no change is made to the history, or a blank array is returned if the array didn't already exist.
	 * 
	 * The array is arranged in ascending historical order, so that most recent
	 * item will be at the end of the list.
	 * 
	 * @param historicalValues
	 *            current history array
	 * @param fieldName
	 * @param fieldValue
	 * @param dateTimeStamp
	 * @return the updated (which is done in place, version of historicalValues.
	 */
	public static JSONArray maintainHistoryArray(JSONArray historicalValues, String fieldName, String fieldValue, String dateTimeStamp) {
		if (historicalValues == null) {
			historicalValues = new JSONArray();
		}

		if (fieldValue.length() > 0) {
			if (historicalValues.length() == 0 || historicalValues.getJSONObject(historicalValues.length() - 1).getString(fieldName).equals(fieldValue) == false) {
				JSONObject entry = new JSONObject().put(fieldName, fieldValue).put("date", dateTimeStamp);
				historicalValues.put(entry);
			}
		}

		return historicalValues;
	}

	/**
	 * replaces any "." in a field name with an underscore -> elasticsearch can
	 * not store field names with a "." The original object is modified in
	 * place.
	 * 
	 * @param result
	 */
	public static void correctFieldNamesWithPeriods(JSONObject result) {
		java.util.Set<String> keys = new java.util.HashSet<String>(result.keySet());

		for (String field : keys) {
			Object o = result.get(field);
			if (o instanceof JSONObject) {
				correctFieldNamesWithPeriods((JSONObject) o);
			} else if (o instanceof JSONArray) {
				JSONArray a = (JSONArray) o;
				for (int i = 0; i < a.length(); i++) {
					Object o1 = a.get(i);
					if (o1 instanceof JSONObject) {
						correctFieldNamesWithPeriods((JSONObject) o1);
					}
				}
			}
			if (field.contains(".")) {
				String newFieldName = field.replace('.', '_');
				result.remove(field);
				result.put(newFieldName, o);
			}
		}
	}

	/**
	 * 
	 * 
	 * @param result
	 */
	public static java.util.List<String> listAllFields(JSONObject jo) {
		return listAllFields(jo, "");

	}

	private static java.util.List<String> listAllFields(JSONObject jo, String prefix) {
		java.util.List<String> result = new java.util.ArrayList<String>();
		if (prefix.equals("") == false) {
			prefix += ".";
		}

		for (String field : jo.keySet()) {
			Object o = jo.get(field);
			if (o instanceof JSONObject) {
				result.addAll(listAllFields((JSONObject) o, prefix + field));
			} else if (o instanceof JSONArray) {
				result.add(prefix + field);
				JSONArray a = (JSONArray) o;
				for (int i = 0; i < a.length(); i++) {
					Object o1 = a.get(i);
					if (o1 instanceof JSONObject) {
						result.addAll(listAllFields((JSONObject) o1, prefix + field));
					}
				}
			} else {
				result.add(prefix + field);
			}
		}
		return result;
	}

	/**
	 * 
	 * 
	 * @param result
	 */
	public static java.util.HashMap<String,Integer> countAllFields(JSONObject jo) {
		return countAllFields(jo, new HashMap<String,Integer>() );

	}

	public static java.util.HashMap<String, Integer> countAllFields(JSONObject jo, HashMap<String, Integer> priorCounts) {
		return countAllFields(jo,priorCounts,"");
	}

	private static java.util.HashMap<String, Integer>  countAllFields(JSONObject jo, HashMap<String, Integer> counts, String prefix) {
		// count ourself, first
		counts.put(prefix, counts.getOrDefault(prefix, 0) +1);
		
		if (prefix.equals("") == false) {
			prefix += ".";
		}

		for (String field : jo.keySet()) {
			Object o = jo.get(field);
			if (o instanceof JSONObject) {
				countAllFields((JSONObject) o, counts, prefix + field); //counting for the current key is at the start of the method
			} else if (o instanceof JSONArray) {
				counts.put(prefix+field, counts.getOrDefault(prefix+field, 0) +1);
				JSONArray a = (JSONArray) o;
				for (int i = 0; i < a.length(); i++) {
					Object o1 = a.get(i);
					if (o1 instanceof JSONObject) {
						countAllFields((JSONObject) o1, counts, prefix + field);
					}
				}
			} else {
				counts.put(prefix+field, counts.getOrDefault(prefix+field, 0) +1);
			}
		}
		return counts;
	}
	
	/**
	 * Recursively gets all of the field values for a particular JSON array and
	 * places into a set so that the values are unique.
	 * 
	 * @param jo
	 * @return
	 */
	public static java.util.HashSet<String> getAllFieldValues(JSONObject jo) {
		java.util.HashSet<String> result = new java.util.HashSet<String>();

		for (String field : jo.keySet()) {
			Object o = jo.get(field);
			if (o instanceof JSONObject) {
				result.addAll(getAllFieldValues((JSONObject) o));
			} else if (o instanceof JSONArray) {
				JSONArray a = (JSONArray) o;
				for (int i = 0; i < a.length(); i++) {
					Object o1 = a.get(i);
					if (o1 instanceof JSONObject) {
						result.addAll(getAllFieldValues((JSONObject) o1));
					} else {
						result.add(o.toString());
					}
				}
			} else {
				result.add(o.toString());
			}
		}
		return result;
	}

	/**
	 * Converts an array of similar JSON objects that have the same key field
	 * name into a hashmap indexed by those keys.
	 * 
	 * @param array
	 * @param keyFieldName
	 * @return JSONObject that is a "hashmap" of objects.
	 */
	public static JSONObject toMappedJSONObject(JSONArray array, String keyFieldName) {
		JSONObject result = new JSONObject();

		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			result.put(obj.getString(keyFieldName), obj);
		}

		return result;
	}

	public static String convertInvertedIndexToString (JSONObject index, int length) {
		String words[] = new String[length];
		
		for (String word: index.keySet()) {
			JSONArray positions = index.getJSONArray(word);
			for (int i=0;i<positions.length();i++) {
				words[positions.getInt(i)] = word;
			}
		}
		
		return String.join(" ", words);
	}
	
	/**
	 * Two JSON Objects are considered to be equivalent if the gson parsed
	 * representations match.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isEquivalent(JSONObject a, JSONObject b) {
		if ((a == null && b != null) || (b == null && a != null)) {
			return false;
		}
		JsonParser parser = new JsonParser();
		JsonElement e1 = parser.parse(a.toString());
		JsonElement e2 = parser.parse(b.toString());
		return e1.equals(e2);
	}

	/**
	 * Copy a JSON object to create a new instance that has an equivalent value.  
	 * Performs a shallow copy
	 * 
	 * @param a an object to copy
	 * @return New Instance
	 */
	public static JSONObject copy(JSONObject a) {
		String[] names = JSONObject.getNames(a);
		return new JSONObject(a, names == null ? new String[] {} : names);
	}
	
	/**
	 * Copy a JSON object to create a new instance that has an equivalent value.
	 * Works by converting to a string and parsing that value.
	 * 
	 * @param a an object to copy
	 * @return New Instance
	 */
	public static JSONObject copyDeep(JSONObject a) {
		return new JSONObject(a.toString());
	}	
	

	/**
	 * Get an nested object from a JSONObject using a qualified name.
	 */
	public static Object get(JSONObject json, String qualifedKey) {
		String[] nameParts = StringUtils.split(qualifedKey, ".");
		for (int i = 0; i < nameParts.length; i++) {
			if (json == null) {
				return null;
			}
			if (i == nameParts.length - 1) {
				return json.opt(nameParts[i]);
			}
			json = json.optJSONObject(nameParts[i]);
		}
		return null;
	}
	
	/**
	 * Get a String from a JSONObject using a qualified name.
	 * 
	 * If not found, returns null
	 */
	public static String getAsString(JSONObject json, String qualifedKey, String defaultValue) {
		String[] nameParts = StringUtils.split(qualifedKey, ".");
		for (int i = 0; i < nameParts.length; i++) {
			if (json == null) {
				return defaultValue;
			}
			if (i == nameParts.length - 1) {
				return json.optString(nameParts[i],defaultValue);
			}
			json = json.optJSONObject(nameParts[i]);
		}
		return defaultValue;
	}	
	

	/**
	 * Get an nested object from a JSONObject using a qualified name and a
	 * default value.
	 */
	public static Object get(JSONObject json, String qualifedKey, Object defaultValue) {
		Object value = get(json, qualifedKey);
		return value == null ? defaultValue : value;
	}

	public static JSONArray asJsonArray(Object object) {
		if (object instanceof JSONArray) {
			return (JSONArray) object;
		}
		JSONArray result = new JSONArray();
		result.put(object);
		return result;
	}

	public static void put(JSONObject json, String qualifedKey, Object value) {
		String[] nameParts = StringUtils.split(qualifedKey, ".");
		for (int i = 0; i < nameParts.length; i++) {
			if (i == nameParts.length - 1) {
				json.put(nameParts[i], value);
				return;
			} else {
				JSONObject x = json.optJSONObject(nameParts[i]);
				if (x == null) {
					x = new JSONObject();
					json.put(nameParts[i], x);
				}
				json = x;
			}
		}
	}

}
