package edu.ncsu.las.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class ValidationException extends Exception {
	private static final long serialVersionUID = 1L;

	private static final int SC_BAD_REQUEST = 400;
	private int _statusCode;
	
	private java.util.List<String> _errors = new java.util.ArrayList<String>();
	
	public ValidationException(String message) {
		super(message);
		_statusCode = SC_BAD_REQUEST;
	}

	public ValidationException(String message, java.util.List<String> errors) {
		super(message);
		_statusCode = SC_BAD_REQUEST;
		_errors = errors;
	}

	public ValidationException(String message, int statusCode) {
		super(message);
		_statusCode = statusCode;
	}

	public JSONObject toJSONObject() {
		JSONArray errors = new JSONArray();
		_errors.stream().forEach(errors::put);
		JSONObject result = new JSONObject().put("status","failed").put("statusCode", _statusCode).put("message", super.getMessage()).put("errors",errors);
		return result;
	}

	public int getStatusCode() {
		return _statusCode;
	}
	

}
