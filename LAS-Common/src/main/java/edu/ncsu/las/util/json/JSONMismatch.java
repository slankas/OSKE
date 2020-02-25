package edu.ncsu.las.util.json;

/**
 * Parent class for JSONObject discrepancies.
 * 
 *
 */
public class JSONMismatch {
    /**
     * Backslash delimited string showing path.
     */
    protected String path;

    /**
     * Gets the path that the discrepancy occurs at.
     * @return Path
     */
    public String getPath() {
        return path;
    }
}
