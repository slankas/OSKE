package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.json.JSONException;
import org.json.JSONObject;
import org.reflections.Reflections;


import edu.ncsu.las.storage.citation.PubMedProcessor;

/**
 * 
 * 
 * 
 */
public abstract class Annotation implements Comparable<Annotation>{
	static Logger logger =Logger.getLogger(PubMedProcessor.class.getName());
	
	
	/** 
	 * What is the name of this annotation
	 *  
	 * @return name
	 */
	public abstract String getName();

	/**
	 * Used to represent a short code that defines whether the annotator should be used.
	 * The codes are used by ElasticSearch or graph processor to feed data..
	 * 
	 * @return
	 */
	public abstract String getCode();
	
	
	/**
	 * Define the ElasticSearch schema used by the annotator
	 * @return
	 */
	public abstract JSONObject getSchema();
	
	/**
	 * What does this annotator do?
	 * @return description of the annotator
	 */
	public abstract String getDescription();
	  
	
	/**
	 * What is the ordering of this annotation?
	 * 
	 * Order based based upon a low to high scheme. (ie, so
	 * Integer.MIN_VALUE would always be checked first). 
	 * 
	 * @return
	 */
	public abstract int getOrder();
	
	/**
	 * Compares based upon the ordering
	 */
	@Override
	public final int compareTo(Annotation o) {
		int thisVal = this.getOrder();
		int anotherVal = o.getOrder();
		return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
	}
	
	/**
	 * Returns a list of annotations (ie, top-level entries in the JSON record) that
	 * must be present to perform the processing.
	 * 
	 * @return
	 */
	public abstract String[] getRequiredAnnotations();
	
    public final boolean hasRequiredAnnotations(JSONObject record) {
    	String required[] = this.getRequiredAnnotations();
    	
    	for (String a: required) {
    		if (record.has(a) == false) {
    			return false;
    		}
    	}

		return true;
	}
	
	
    public final boolean process(java.io.File recordFile, String htmlFileLocation, String pdfFileLocation) {
    	try {
	    	JSONObject record = PubMedProcessor.loadRecord(recordFile);
	    	if (record != null) {
	    		if (record.has(this.getCode()) == false) {  // make true to re-reprocess...
	    			if (this.hasRequiredAnnotations(record)) {
	    				this.doProcessing(record, htmlFileLocation, pdfFileLocation);
	    				PubMedProcessor.writeRecord(recordFile, record);
	    			}
	    			else {
	    				logger.log(Level.WARNING, "Missing required annotations, skipping "+recordFile);
	    			}
	    		}
	    		else {
	    			logger.log(Level.INFO, "Already processed "+this.getCode()+" on "+recordFile);
	    		}
	    	}
	    	else {
	    		logger.log(Level.WARNING, "No record loaded, skipping "+recordFile);
	    	}
	    	return true;
    	} catch (JSONException | IOException e) {
    		logger.log(Level.SEVERE, "Unable to process annotation "+recordFile,e);
    		return false;
    	}
    }
	


	public abstract void doProcessing(JSONObject record, String htmlFile, String pdfFile);

    private static java.util.List<Annotation> _allAnnotations;
    
	public static java.util.List<Annotation> getAllAnnotations() {
		if (_allAnnotations == null) {

			
			Reflections reflections = new Reflections("edu.ncsu.las");    
			java.util.Set<Class<? extends Annotation>> classes = reflections.getSubTypesOf(Annotation.class);
		
			java.util.List< Annotation> result = new java.util.ArrayList<Annotation>();
			for (Class<? extends Annotation> c: classes) {
				try {
					Annotation a = c.newInstance();
					result.add(a);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
					logger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
				}
			}
			Collections.sort(result);
			_allAnnotations = result;
		}
		return _allAnnotations;
	}	
	

	public static void performAllAnnotations(java.io.File recordFile, String htmlFile, String pdfFile) {
		java.util.List<Annotation> annotations = getAllAnnotations();
		
		for (Annotation a: annotations) {
			a.process(recordFile, htmlFile, pdfFile);
		}
	}    
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject()
				.put("description", this.getDescription())
				.put("code", this.getCode())
				.put("name", this.getName())
				.put("order", this.getOrder());
		return result;
	}

}
