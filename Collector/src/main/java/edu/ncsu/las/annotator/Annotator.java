package edu.ncsu.las.annotator;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.AnnotatorExecutionPoint;
import edu.ncsu.las.model.collector.type.ConfigurationType;

/**
 * Parent class to represent annotations that can be performed on
 * documents to extract additional data from ingested documents
 * 
 */
public abstract class Annotator implements Comparable<Annotator>{
	static Logger logger =Logger.getLogger(Collector.class.getName());
	
	/** codes in use already in the JSON Object.  Don't allow an annotator to use these*/
	public static String[] RESERVED_CODES = { 
		    "mime_type",
		    "domain",
		    "hash",
			"email",
		    "crawlDepth",
		    "url",
		    "textFromTika",
		    "crawled_dt",
		    "source_uuid", "type",
		    "text", "text_length","raw_data_length", "sourceDocument","alternateID"
	};  	
	
	/** 
	 * What is the name of this annotator?
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
	 * 
	 * @return
	 */
	public abstract String getContentType();
	
	/**
	 * What is the priority of this annotator? 
	 * 
	 * Order based based upon a low to high scheme. (ie, so
	 * Integer.MIN_VALUE would always be checked first). Any domain specific
	 * handlers must have a lower processing order such that are processed
	 * first.
	 * 
	 * 
	 * @return
	 */
	public  int getPriority() {
		return 100;
	}
	
	
	@Override
	public int compareTo(Annotator o) {
		int thisVal = this.getPriority();
		int anotherVal = o.getPriority();
		return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
	}
	
	
	/**
	 * when should this annotator occur?
	 * 
	 * @return
	 */
	public abstract AnnotatorExecutionPoint getExecutionPoint();
		
	
    public abstract void process(Document currentDocument);
    
    private static java.util.HashMap<String,java.util.List<Annotator>> _annotators = new java.util.HashMap<>();
    
    private static java.util.List<Annotator> _allAnnotators;
    
	public static java.util.List<Annotator> getDomainAnnotators(String domain) {
		if (_annotators.containsKey(domain) == false) {
			JSONArray annotationList = Configuration.getConfigurationPropertyAsArray(domain,ConfigurationType.ANNOTATIONS);
			java.util.HashSet<String> annotatorCodes = new java.util.HashSet<String>();
			for (int i=0;i< annotationList.length();i++) {
				annotatorCodes.add(annotationList.getString(i));
			}
			
			Reflections reflections = new Reflections("edu.ncsu.las");    
			java.util.Set<Class<? extends Annotator>> classes = reflections.getSubTypesOf(Annotator.class);
		
			java.util.List< Annotator> result = new java.util.ArrayList<Annotator>();
			for (Class<? extends Annotator> c: classes) {
				try {
					Annotator a = c.newInstance();
					if (annotatorCodes.contains(a.getCode())) {
						result.add(a);
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
					logger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
				}
			}
			Collections.sort(result);
			_annotators.put(domain, result);
		}
		
		return _annotators.get(domain);
	}
	
	public static java.util.List<Annotator> getAllAnnotators() {
		if (_allAnnotators == null) {

			
			Reflections reflections = new Reflections("edu.ncsu.las");    
			java.util.Set<Class<? extends Annotator>> classes = reflections.getSubTypesOf(Annotator.class);
		
			java.util.List< Annotator> result = new java.util.ArrayList<Annotator>();
			for (Class<? extends Annotator> c: classes) {
				try {
					Annotator a = c.newInstance();
					result.add(a);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
					logger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
				}
			}
			Collections.sort(result);
			_allAnnotators = result;
		}
		return _allAnnotators;
	}	
	

	public static void annotate(AnnotatorExecutionPoint point, Document currentDocument) {
		java.util.List<Annotator> annotators = Annotator.getDomainAnnotators(currentDocument.getDomainInstanceName());
		
		for (Annotator a: annotators) {
			if (a.getExecutionPoint() == point && currentDocument.hasAnnotation(a.getCode()) == false) {
				logger.log(Level.FINEST, " calling annotator "+ a.getName());
				a.process(currentDocument);
			
			}
		}
	}    
	
	public static void validateCodesNotReserved() {
		boolean stop = false;
		
		List<Annotator> annotators = getAllAnnotators();
		for (Annotator a: annotators) {
			for (String reservedCode: RESERVED_CODES) {
				if (a.getCode().equalsIgnoreCase(reservedCode)) {
					logger.log(Level.SEVERE, "Invalid annotator code.  Class: "+a.getClass().getName()+", code: "+a.getCode());
					stop = true;
				}
			}
		}
		if (stop) {
			System.exit(-2);
		}
	}
	
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject()
				.put("description", this.getDescription())
				.put("mimeType", this.getContentType().toString())
				.put("code", this.getCode())
				.put("executionPoint", this.getExecutionPoint().toString())
				.put("name", this.getName())
				.put("priority", this.getPriority());
		return result;
	}

}
