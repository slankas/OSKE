package edu.ncsu.las.util.json;

/**
 * Discrepancy for when a value is not of the expected type.
 *
 * 
 *
 */
public class TypeMismatch extends JSONMismatch {
    /**
     * Expected type at path.
     */
    protected Object valid;

    /**
     * Actual type at path.
     */
    protected Object current;

    /**
     * Accessor.
     *
     * @return Type at path.
     */
    public Object getCurrent() {
        return current;
    }

    /**
     * Accessor.
     *
     * @return Expected type at path.
     */
    public Object getValid() {
        return valid;
    }

    /**
     * Constructor.
     *
     * @param path Path discrepancy occurs at
     * @param valid Expected type
     * @param current Actual type
     */
    public TypeMismatch(final String path, final Object valid, final Object current) {
        this.path = path;
        this.valid = valid;
        this.current = current;
    }

    @Override
    public String toString() {
        return "Type Mismatch: " + path + " is " + valid.toString() + ", is " + current.toString();
    }
}
