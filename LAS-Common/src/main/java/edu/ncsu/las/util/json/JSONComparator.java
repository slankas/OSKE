package edu.ncsu.las.util.json;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to handle comparing two JSON objects and producing
 * a human- & computer-readable list of discrepancies.
 * 
 *
 */
public class JSONComparator {
    /**
     * Entry function.
     *
     * @param valid The valid JSON object to compare against
     * @param current The JSON object under test
     * @return A list of discrepancies found.
     */
    public static List<JSONMismatch> run(final JSONObject valid, final JSONObject current) {
        JSONComparator comparator = new JSONComparator();
        comparator.compareObjs(valid, current);
        return comparator.issues;
    }

    /**
     * List of issues found.
     */
    private List<JSONMismatch> issues;

    /**
     * The current path with the JSON object.
     *
     * '@' followed by a number is an array index.  Anything else
     * is a object key.
     */
    private List<String> curPath;

    /**
     * Internal constructor.
     */
    private JSONComparator() {
        this.issues = new LinkedList<JSONMismatch>();
        this.curPath = new LinkedList<String>();
    }

    /**
     * Transforms current curPath list into a backslash delimited string.
     * @param key Extra level to include in path
     * @return Path as string
     */
    private String getPath(final String key) {
        return String.join("\\", curPath) + "\\" + key;
    }

    /**
     * Transforms current curPath list into a backslash delimited string.
     * @return Path as string
     */
    private String getPath() {
    	return String.join("\\", curPath);
    }

    /**
     * Internal function to compare two elements.
     * @param xObj Valid element
     * @param yObj Element under test
     */
    private void compareObjs(final Object xObj, final Object yObj) {
        //Check type
        Object xType = xObj.getClass();
        Object yType = yObj.getClass();

        if (xType != yType) {
            TypeMismatch issue = new TypeMismatch(getPath(), xType, yType);
            issues.add(issue);
            return;
        }

        // Check values
        if (xObj instanceof JSONObject) {
            // Compare objects
            compare((JSONObject) xObj, (JSONObject) yObj);
        } else if (xObj instanceof JSONArray) {
            // Compare lists
            JSONArray xArray = (JSONArray) xObj;
            JSONArray yArray = (JSONArray) yObj;
            for (int i = 0; i < xArray.length(); i++) {
                curPath.add("@" + String.valueOf(i));
                try {
                    compareObjs(xArray.get(i), yArray.get(i));
                } catch (JSONException e) {
                    issues.add(new MissingKey(getPath()));
                }
                curPath.remove(curPath.size() - 1);
            }

            for (int i = xArray.length(); i < yArray.length(); i++) {
                issues.add(new ExtraKey(getPath("@" + String.valueOf(i))));
            }
        } else {
            // Compare values
            if (!xObj.equals(yObj)) {
                ValueMismatch issue = new ValueMismatch(getPath(), xObj.toString(), yObj.toString());
                issues.add(issue);
            }
        }
    }

    /**
     * Internal function to compare to JSONObjects.
     * @param x Valid object
     * @param y Object under test
     */
    private void compare(final JSONObject x, final JSONObject y) {
        for (Iterator<String> xKeyIter = x.keys(); xKeyIter.hasNext();) {
            String xKey = xKeyIter.next();
            if (!y.has(xKey)) {
                MissingKey issue = new MissingKey(getPath(xKey));
                issues.add(issue);
                continue;
            }

            // Key is present - compare
            Object xObj = x.get(xKey);
            Object yObj = y.get(xKey);

            curPath.add(xKey);
            compareObjs(xObj, yObj);
            curPath.remove(curPath.size() - 1);
        }

        for (Iterator<String> yKeyIter = y.keys(); yKeyIter.hasNext();) {
            String yKey = yKeyIter.next();
            if (!x.has(yKey)) {
                ExtraKey issue = new ExtraKey(String.join("\\", curPath) + "\\" + yKey);
                issues.add(issue);
            }
        }
    }
}
