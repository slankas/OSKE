package edu.ncsu.las.util.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Specialized JSONObject class that handles the case when a JSON object exists that has the same field defined multiple times,
 * which is not supposed to happen.
 * 
 * This class was taken from http://stackoverflow.com/questions/19001238/remove-duplicates-from-a-json-string-in-java
 * by Menelaos Bakopoulos and Asaf Bartov
 * 
 * Code Usage:
 * JSONObject contentJSON = new JSONObjectIgnoreDuplicates(new JSONObjectIgnoreDuplicates.JsonDupTokener(content));
 * 
 * 
 *
 */
public class JSONObjectIgnoreDuplicates extends JSONObject {
    public JSONObjectIgnoreDuplicates(JSONTokener x) throws JSONException {
        super(x);
    }

    @Override
    public JSONObject putOnce(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            this.put(key, value);
        }
        return this;
    }
    
    public static class JsonDupTokener extends JSONTokener {

        public JsonDupTokener(String s) {
            super(s);
        }

        @Override
        public Object nextValue() throws JSONException {
            char c = this.nextClean();
            switch (c) {
                case '\"':
                case '\'':
                    return this.nextString(c);
                case '[':
                    this.back();
                    return new JSONArray(this);
                case '{':
                    this.back();
                    return new JSONObjectIgnoreDuplicates(this);
                default:
                    StringBuffer sb;
                    for (sb = new StringBuffer(); c >= 32 && ",:]}/\\\"[{;=#".indexOf(c) < 0; c = this.next()) {
                        sb.append(c);
                    }

                    this.back();
                    String string = sb.toString().trim();
                    if ("".equals(string)) {
                        throw this.syntaxError("Missing value");
                    } else {
                        return JSONObject.stringToValue(string);
                    }
            }
        }
    }
}


