package edu.ncsu.las.util.json;

/**
 * Discrepency where the JSON value does not match the expected value.
 *
 * 
 *
 */
public class ValueMismatch extends JSONMismatch {
    /**
     * Expected value
     */
    protected String valid;

    /**
     * Actual value
     */
    protected String current;

    /**
     * Accessor. Returns expected value.
     * @return expected value
     */
    public String getValid() {
        return valid;
    }

    /**
     * Accessor.  Returns actual value.
     * @return actual value
     */
    public String getCurrent() {
        return current;
    }

    /**
     * Constructor.
     *
     * @param path Path discrepancy occurs at
     * @param valid Expected Value
     * @param current Actual Value
     */
    public ValueMismatch(String path, String valid, String current) {
        this.path = path;
        this.valid = valid;
        this.current = current;
    }

    @Override
    public String toString() {
        return "Value Mismatch: " + path + " is " + valid + ", is " + current;
    }
}
