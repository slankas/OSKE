package edu.ncsu.las.util.json;

/**
 * JSONMismatch for when an extra key is present in a JSONArray or JSONObject.
 */
public final class ExtraKey extends JSONMismatch {
    /**
     * Constructor.
     *
     * @param path Backslash delimited path that the extra element is found at
     */
    public ExtraKey(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Extra Key: " + path;
    }
}
