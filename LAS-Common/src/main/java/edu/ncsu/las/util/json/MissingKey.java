package edu.ncsu.las.util.json;

/**
 * Discrepancy class for when a JSONObject or JSONArray is missing a key or item.
 * 
 *
 */
public class MissingKey extends JSONMismatch {
    /**
     * Constructor.
     *
     * @param path Path of key that is missing
     */
    public MissingKey(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Missing Key: " + path;
    }
}
