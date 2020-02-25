package edu.ncsu.las.model.collector.type;

import java.util.TreeMap;


/**
 * Represents the different parameters that source handlers can utilize.
 * 
 * Needed because a simple key/value pair of name and description wasn't sufficiently rich...
 * 
 */
public class SourceParameter {
	/** what is the name of the parameter*/
	private String _name;
	
	/** description */
	private String _description;
	
	/** Does the source handler require this parameter? */
	private boolean _required = false;
	
	/** String with what a typical parameter may look like ... */
	private String _example;
	
	private boolean _isArray = false;
	
	/** what is the underlying type for this parameter? */
	private SourceParameterType _type = SourceParameterType.UNKNOWN;
	
	/** should this value be encrypted */
	private boolean _encrypted = false;
	
	/** should this parameter in the templates when creating jobs */
	private boolean _useInTemplate = false;
	
	private SourceParameter(String name, String description, boolean required, String example) {
		this._name = name;
		this._description = description;
		this._required = required;
		this._example = example;
	}
	
	private SourceParameter(String name, String description, boolean required, String example, boolean isArray, SourceParameterType type) {
		this(name, description, required, example);
		
		_isArray = isArray;
		_type    = type;
	}	
	
	public SourceParameter(String name, String description, boolean required, String example, boolean isArray, SourceParameterType type, boolean encryptValue, boolean useInTemplate) {
		this(name, description, required, example,isArray,type);
		_encrypted = encryptValue;
		_useInTemplate = useInTemplate;
	}	

	public String getName() {
		return _name;
	}

	public String getDescription() {
		return _description;
	}
		
	public boolean isRequired() {
		return _required;
	}

	public boolean isEncrypted() {
		return _encrypted;
	}
	
	public String getExample() {
		return _example;
	}

	public SourceParameterType getType() {
		return _type;
	}
	
	public boolean isArray() {
		return _isArray;
	}
	
	public boolean useInTemplate() {
		return _useInTemplate;
	}
	/*
	public static void putSourceParameter(TreeMap<String, SourceParameter> map, String name, String description, boolean required, String defaultValue, boolean isArray, SourceParameterType type) {
		putSourceParameter(map, name, description, required, defaultValue, isArray, type, false, false);
	}
	*/

	public static void putSourceParameter(TreeMap<String, SourceParameter> map, String name, String description, boolean required, String defaultValue, boolean isArray, SourceParameterType type, boolean encrypt, boolean useInTemplate) {
		SourceParameter parameter = new SourceParameter(name, description, required, defaultValue, isArray, type, encrypt, useInTemplate);
		map.put(parameter.getName(), parameter);
	}
}
