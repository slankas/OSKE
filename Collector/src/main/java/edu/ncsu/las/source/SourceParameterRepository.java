package edu.ncsu.las.source;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.model.collector.type.SourceParameterType;
import edu.ncsu.las.util.DateUtilities;

public class SourceParameterRepository {
	
	java.util.TreeMap<String, SourceParameter> _parameters;
	
	public SourceParameterRepository(java.util.TreeMap<String, SourceParameter> parameters) {
		_parameters = parameters;
	}
	
	public  String convertParametersToSampleString() {
		JSONObject result = new JSONObject();
		
		for (String paramName: _parameters.keySet()){
			JSONObject jo = result;
			
			SourceParameter sp = _parameters.get(paramName);
			if (sp.useInTemplate() == false) { continue; }
			
			String[] sections = paramName.split("\\.");
			for (int i=0; i< (sections.length-1);i++) {
				String key = sections[i];
				if (jo.has(key) == false) {
					jo.put(key, new JSONObject());
				}
				if (jo.get(key) instanceof JSONObject) {
					jo = jo.getJSONObject(key);
				}
				else if (jo.get(key) instanceof JSONArray) {
					jo = jo.getJSONArray(key).getJSONObject(0);
				}
				else if (jo.get(key) instanceof String) {
					jo.put(key, new JSONObject());
					jo = jo.getJSONObject(key);
					
				}
			}			
			
			//SourceParameter sp = _parameters.get(paramName);
			//if (sp.useInTemplate() == false) { continue; }
			if (sp.isArray()) {
				JSONArray temp = new JSONArray();
				if (sp.getType() == SourceParameterType.JSON_OBJECT) {
					temp.put(new JSONObject());
				}
				else {
					try {
						temp = new JSONArray(sp.getExample());
					}
					catch (org.json.JSONException e) {
						temp.put(sp.getExample());
					}
				}
				jo.put(sections[sections.length-1], temp);
			}
			else {
				this.setAppropriateType(null, sp,jo, sections[sections.length-1]);
			}
		}	
		return result.toString();
	}		
	
	private JSONObject getParentJSONObject(String fieldName, JSONObject obj) {
		String fields[] = fieldName.split("\\.");
		
		JSONObject temp = obj;
		for (int i=0;i <(fields.length-1); i++) {
			temp = temp.optJSONObject(fields[i]);
			if (temp == null) {break;}
		}
		return temp;
	}
	
	private String getLastFieldName(String fieldName) {
		String fields[] = fieldName.split("\\.");
		return fields[fields.length-1];
	}
	
	private Object getField(String fieldName, JSONObject obj) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? null : parent.opt(getLastFieldName(fieldName));
	}
	
	public String getFieldAsString(String fieldName, JSONObject obj) {
		return getFieldAsString(fieldName, obj, null);
	}	

	public String getFieldAsString(String fieldName, JSONObject obj, String defaultValue) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		String result = (parent == null ? defaultValue : parent.optString(getLastFieldName(fieldName),defaultValue));
		
		//check if encrypted
		SourceParameter sp = _parameters.get(fieldName);
		if (result != null && sp != null && sp.isEncrypted()) {
			result = Collector.getTheCollecter().decryptValue(result);
		}
		
		return result;
	}		
	
	
	/**
	 * defaults to false if not present
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public boolean getFieldAsBoolean(String fieldName, JSONObject obj) {
		return getFieldAsBoolean(fieldName,obj,false);
	}	
	
	/**
	 * returns the default if not present
	 * 
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public boolean getFieldAsBoolean(String fieldName, JSONObject obj, boolean defaultValue) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? defaultValue : parent.optBoolean(getLastFieldName(fieldName),defaultValue);
	}		
	
	/**
	 * defaults to -1 if not present
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public int getFieldAsInteger(String fieldName, JSONObject obj) {
		return getFieldAsInteger(fieldName, obj, -1);
	}	
	
	/**
	 * defaults to -1 if not present
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public int getFieldAsInteger(String fieldName, JSONObject obj, int defaultValue) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? defaultValue : parent.optInt(getLastFieldName(fieldName),defaultValue);
	}	
	
	
	/**
	 * default to -1 if not presett
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public long getFieldAsLong(String fieldName, JSONObject obj) {
		return getFieldAsLong(fieldName, obj, -1L);
	}		
	
	/**
	 * default to -1 if not present
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public long getFieldAsLong(String fieldName, JSONObject obj, long defaultValue) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? defaultValue : parent.optLong(getLastFieldName(fieldName),defaultValue);
	}		
	
	
	/**
	 * default to -1.0 if not present
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public double getFieldAsDouble(String fieldName, JSONObject obj) {
		return getFieldAsDouble(fieldName, obj, -1.0);
	}			
	
	/**
	 * default to -1.0 if not present
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public double getFieldAsDouble(String fieldName, JSONObject obj, double defaultValue) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? defaultValue : parent.optDouble(getLastFieldName(fieldName),defaultValue);
	}		
		
	
	
	public JSONObject getFieldAsJSONObject(String fieldName, JSONObject obj) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? null : parent.optJSONObject(getLastFieldName(fieldName));
	}		
	
	public JSONArray getFieldAsJSONArray(String fieldName, JSONObject obj) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		return parent == null ? null : parent.optJSONArray(getLastFieldName(fieldName));
	}		
	
	/**
	 * returns the default if not present
	 * 
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public void accessFieldAsBoolean(String fieldName, JSONObject obj) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		parent.getBoolean(getLastFieldName(fieldName));
	}		
	
	/**
	 * returns the default if not present
	 * 
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public void accessFieldAsInt(String fieldName, JSONObject obj) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		parent.getInt(getLastFieldName(fieldName));
	}		
	
	/**
	 * returns the default if not present
	 * 
	 * @param fieldName
	 * @param obj
	 * @return
	 */
	public void accessFieldAsLong(String fieldName, JSONObject obj) {
		JSONObject parent = getParentJSONObject(fieldName, obj); 
		parent.getLong(getLastFieldName(fieldName));
	}			
		
	
	private boolean checkType(SourceParameter sp, JSONObject configuration, String fieldName) {
		if (sp.getType() == SourceParameterType.BOOLEAN) {
			try {	accessFieldAsBoolean(fieldName, configuration);  return true;	}
			catch (Exception e) {		return false;		}
		}

		if (sp.getType() == SourceParameterType.INT) {
			try {	accessFieldAsInt(fieldName, configuration);  return true;	}
			catch (Exception e) {		return false;	}
		}

		if (sp.getType() == SourceParameterType.LONG) {
			try {	getFieldAsLong(fieldName, configuration);	return true;	}
			catch (Exception e) {		return false;	}
		}		
		
		return true;
	}
	
	/**
	 * 
	 * 
	 * Logic for the types has been copied from org.json.JSONObject 
	 * 
	 * @param type
	 * @param object
	 * @return
	 */
	private boolean checkType(SourceParameterType type, Object object) {
		switch(type) {
		case BOOLEAN: if (object.equals(Boolean.FALSE) || (object instanceof String && ((String) object).equalsIgnoreCase("false"))) {	return true; } 
		              else if (object.equals(Boolean.TRUE) || (object instanceof String && ((String) object).equalsIgnoreCase("true"))) { return true; }
		              return false;
		case DOUBLE:  try {
						  @SuppressWarnings("unused")
						  double d = object instanceof Number ? ((Number) object).doubleValue() : Double.parseDouble((String) object);
						  return true;
					  } catch (Exception e) {
				          return false; 
					  }
		case INT:     try {
						  @SuppressWarnings("unused")
			              int i = object instanceof Number ? ((Number) object).intValue() : Integer.parseInt((String) object);
			              return true;
		              } catch (Exception e) {
	                      return false; 
		              }
		case JSON_OBJECT: return (object instanceof JSONObject);
		case LONG:    try {
			              @SuppressWarnings("unused")
            	          long l = object instanceof Number ? ((Number) object).longValue() : Long.parseLong((String) object);
            	          return true;
            	      } catch (Exception e) {
            	    	  return false; 
            	      }
		case REGEX: return (object instanceof String);
		case STRING: return (object instanceof String);
		case STRING_TIMESTAMP: return (object instanceof String);
		case UNKNOWN:
		case OBJECT:
		default:
			break;
		}
		
		return true;
	}
	
	/**
	 * Checks the passed in configuration to ensure required elements are present and
	 * those elements that are present are of the correct type.
	 * 
	 * @param configuration
	 * @return
	 */
	public java.util.List<String> validateConfiguration(JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();
		
		for (SourceParameter sp: _parameters.values()) {
			Object parameterValue;
			try {
				parameterValue = this.getField(sp.getName(), configuration);
			}
			catch (Throwable t) {
				parameterValue = null; 
			}
			if (sp.isRequired() && parameterValue == null) {
				errors.add(sp.getName() + " is a required field in the configuration.");
				continue;
			}
			if (parameterValue == null) { // not defined in the configuration, and not required, so skip
				continue;
			}
			
			if (sp.isArray()) {
				JSONArray a = getFieldAsJSONArray(sp.getName(),configuration);
				if (a == null) {
					errors.add(sp.getName() + " needs to be an array of values with type "+ sp.getType().toString());						
				}
				else {
					for (int i=0; i< a.length(); i++) {
						if (checkType(sp.getType(),a.get(i)) == false) {
							errors.add(sp.getName() + " members all need to have a type of "+ sp.getType().toString());
							break;
						}
					}
				}
			
				continue;
			}
			if (!checkType(sp,configuration,sp.getName())) {
				errors.add(sp.getName() + " is not of the correct type: "+ sp.getType().toString());
				continue;
			}
			if (sp.getType() == SourceParameterType.REGEX) {
				String value = "";
				try { 
					value = getFieldAsString(sp.getName(), configuration);
					Pattern.compile(value) ; // will throw an exception when invalid
				}
				catch (Exception e) {		
					errors.add(sp.getName() + " is not a valid regular expression: "+ value);
					continue;
				}
				
			}
			if (sp.getType() == SourceParameterType.STRING_TIMESTAMP) {
				String value = "";
				try {
					value = getFieldAsString(sp.getName(), configuration);
					ZonedDateTime zdt = DateUtilities.getFromString(value);
					if (zdt == null) { errors.add(sp.getName() + " is not a valid datetime: "+ value); }
				}
				catch (Exception e) {
					errors.add(sp.getName() + " is not a valid datetime: "+ value);
					continue;
				}
			}
			this.setAppropriateType(parameterValue, sp, this.getParentJSONObject(sp.getName(), configuration), this.getLastFieldName(sp.getName()));
		}
		errors.addAll(validateExtraneousDefinitions(configuration, ""));
		return errors;

	}
	
	/**
	 * Has the side affect of possible changing the underlying field type.
	 * If the field type isn't value, default to zero or false...
	 * 
	 * @param value  Use this value to store if it is not null
	 * @param sp if the value is null, then use the example value to store.
	 * @param jo
	 * @param fieldName
	 */
	private void setAppropriateType(Object value, SourceParameter sp, JSONObject jo, String fieldName) {
		Object valueToSet = value;
		if (valueToSet == null) {
			valueToSet = sp.getExample();
		}
		switch (sp.getType()) {
		case BOOLEAN: try {
						  boolean b = Boolean.valueOf(valueToSet.toString()).booleanValue();
						  jo.put(fieldName,b);
		              }
					  catch(Throwable t) {
						  jo.put(fieldName,false);
					  }
					  break;
		case DOUBLE:  try {
			              double d = Double.valueOf(valueToSet.toString()).doubleValue();
			              jo.put(fieldName,d);
                      }
		              catch(Throwable t) {
			              jo.put(fieldName,0.0);
		              }
		              break;
		case INT: 	  try {
			              int i = Integer.valueOf(valueToSet.toString()).intValue();
			              jo.put(fieldName,i);
	                  }
		              catch(Throwable t) {
			              jo.put(fieldName,0);
		              }
		              break;
		case LONG:    try {
			               long l = Long.valueOf(valueToSet.toString()).longValue();
			               jo.put(fieldName,l);
			          }
		              catch(Throwable t) {
	                      jo.put(fieldName,0);
                      }
                      break;
		default: break; // no action needed
		}
	}
	
	
		
		/**
		 * Recursively checks to make sure there are no extra fields defined.
		 * 
		 * @param configuration
		 * @return
		 */
		public java.util.List<String> validateExtraneousDefinitions(JSONObject configuration, String prefix) {
			java.util.ArrayList<String> extraFields = new java.util.ArrayList<String>();
			
			for (String field: configuration.keySet()) {
				String checkName =  (prefix.length() == 0) ? field : prefix + "." + field;
				if (_parameters.containsKey(checkName) == false) {
					extraFields.add(checkName+" is not a defined field in the configuration.");
					continue;
				}
				
				Object o = configuration.get(field);
				if (o instanceof JSONObject) {
					extraFields.addAll(validateExtraneousDefinitions((JSONObject) o, checkName));
				}
				else if (o instanceof JSONArray) {
					if (_parameters.get(checkName).getType() == SourceParameterType.JSON_OBJECT) {
						JSONArray array = (JSONArray) o;
						for (int i=0;i<array.length();i++) {
							try {
								JSONObject arrayMember = array.getJSONObject(i);
								extraFields.addAll(validateExtraneousDefinitions((JSONObject) arrayMember, checkName));
							}
							catch (JSONException e) {
								extraFields.add("excpected JSON object in "+checkName);
								break;
							}
						}
					}
				}
			}
			return extraFields;
	}
	
	/**
	 * Looks through the configuration to check if the fields should be encrypted.
	 * If a field should be encrypted, then its value is check to see whether or not it starts with "{AES}"
	 * If it doesn't currently start with "{AES}, the value is encrypted and placed back into the json object with "{AES}encryptedValue"
	 *   
	 * @param parameters
	 * @param configuration
	 */
	public void checkConfigurationForEncryptedFields(JSONObject configuration) {
		for (SourceParameter sp: _parameters.values()) {
			if (sp.isEncrypted() == false) { continue; }
			
			JSONObject immediateParent = getParentJSONObject(sp.getName(), configuration);
			if (immediateParent == null) { continue; } // object isn't present.
			
			String fields[] = sp.getName().split("\\.");
			String passwordFieldName = fields[fields.length-1];
			
			String password = immediateParent.optString(passwordFieldName,null);
			if (password == null) { continue; } // object isn't present.

			if (password.startsWith("{AES}") == false) {
				String encryptedPassword = Collector.getTheCollecter().encryptValue(password);
				immediateParent.put(passwordFieldName, encryptedPassword);
			}

		}
	}	
	
	
}
