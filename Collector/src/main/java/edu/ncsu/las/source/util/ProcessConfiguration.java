package edu.ncsu.las.source.util;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.json.JSONObject;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.source.AbstractHandler;

/**
 * Manages configuration for include / exclude parameters ...
 *  
 */
public class ProcessConfiguration {

	Pattern includeText = null;
	Pattern includeURL  = null;
	Pattern excludeText = null;
	Pattern excludeURL  = null;
	HashSet<String> includeMimeTypes = null;
	HashSet<String> excludeMimeTypes = null;
	
	
	Pattern _relevancyPattern = null;
	Pattern _includeFilter = null;
	java.util.List<Pattern> excludeFilters = new java.util.ArrayList<Pattern>();
	
	public ProcessConfiguration(AbstractHandler shi, JSONObject config) {
		
		
		if (config.has("relevantRegExp")) {
			_relevancyPattern = Pattern.compile(config.getString("relevantRegExp"));
		}
		
		if (config.has("includeFilter")) {
			_includeFilter = Pattern.compile(config.getString("includeFilter"));
		}
		
		this.establishExcludeFilters(shi);
		
		
		if (config.has("process") == false) {
			return;  // no process config in place, everything should be null
		}
		config = config.getJSONObject("process");
	
		if (config.has("includeTextRegExp")) { includeText = Pattern.compile(config.getString("includeTextRegExp"));  }
		if (config.has("includeURLRegExp"))  { includeURL  = Pattern.compile(config.getString("includeURLRegExp"));	  }
		if (config.has("excludeTextRegExp")) { excludeText = Pattern.compile(config.getString("excludeTextRegExp"));  }
		if (config.has("excludeURLRegExp"))  { excludeURL  = Pattern.compile(config.getString("excludeURLRegExp"));	  }
		
		if (config.has("includeMimeType")) {
			String[] types = config.getString("includeMimeType").split(",");
			includeMimeTypes = new HashSet<String>();
			for (String t: types) { includeMimeTypes.add(t.trim().toLowerCase()); }
		}
		if (config.has("excludeMimeType")) {
			String[] types = config.getString("excludeMimeType").split(",");
			excludeMimeTypes = new HashSet<String>();
			for (String t: types) { excludeMimeTypes.add(t.trim().toLowerCase()); }
		}
	}
	
	public boolean hasIncludeParameter() {
		return (includeText != null || includeURL != null || includeMimeTypes != null);
	}
	public boolean hasExcludeParameter() {
		return (excludeText != null || excludeURL != null || excludeMimeTypes != null);
	}
	
	
	public void addExcludeFilters(JSONObject config) {
		if (config.has("excludeFilter")) {
			Pattern excludeFilter = Pattern.compile(config.getString("excludeFilter"));
			excludeFilters.add(excludeFilter);
		}
	}

	public void establishExcludeFilters(AbstractHandler shi) {
		this.addExcludeFilters(shi.getHandlerDefaultConfiguration());
		this.addExcludeFilters(shi.getJobConfiguration());
		this.addExcludeFilters(Configuration.getDomainConfiguration(shi.getJob().getDomainInstanceName()).getConfiguration());
	}
	
	public Pattern getRelevancyPattern() {
		return _relevancyPattern;
	}
	
	
}
