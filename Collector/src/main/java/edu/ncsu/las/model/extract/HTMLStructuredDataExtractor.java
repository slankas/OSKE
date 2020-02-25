package edu.ncsu.las.model.extract;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.HTMLUtilities;
import edu.ncsu.las.util.json.JSONMinify;


/**
 * Extracts strucuted data, such as that from http://schema.org data from content (HTML page)
 *  
 * Results are put into the JSON format specified at http://www.w3.org/TR/microdata/#json
 * 
 *
 */
public class HTMLStructuredDataExtractor {
	private static final Logger logger =Logger.getLogger(Collector.class.getName());
	
	
	/**
	 * Extracts structured data by parsing for JSON-LD (http://www.w3.org/TR/json-ld/), microdata (http://www.w3.org/TR/microdata),
	 * and RDFa (http://www.w3.org/TR/rdfa-syntax/).
	 * 
	 * 
	 * 
	 * @param htmlContent
	 * @return JSONObject following the structure at specified at http://www.w3.org/TR/microdata/#json
	 * @throws IOException 
	 */
	public static JSONObject extractAllFormats(String htmlContent, java.net.URL baseURL) throws IOException {
		
		HTMLStructuredDataExtractor extractor = new HTMLStructuredDataExtractor();
		JSONObject jsonLD = extractor.extractFromJSONLDScript(htmlContent);
		JSONObject rdfa   = extractor.convertRDFa(htmlContent, baseURL);
		JSONObject md     = extractor.convertMicroData(htmlContent, baseURL);
		
		JSONObject result = new JSONObject();
		
		JSONArray ja = jsonLD.getJSONArray("items");
		for (int i=0;i< ja.length();i++) {
			Object o = ja.get(i);
			result.append("items", o);
		}
		ja = rdfa.getJSONArray("items");
		for (int i=0;i< ja.length();i++) {
			Object o = ja.get(i);
			result.append("items", o);
		}
		ja = md.getJSONArray("items");
		for (int i=0;i< ja.length();i++) {
			Object o = ja.get(i);
			result.append("items", o);
		}
		
		normalizeDateFields(result);
		//System.out.println(result.toString(4));
		return result;
	}
	
	
	private static void normalizeDateFields(JSONObject result) {
		for (String field: result.keySet()) {
			Object o = result.get(field);
			if (o instanceof JSONObject) {
				normalizeDateFields( (JSONObject) o); 
			}
			
			if (DateUtilities.isDateField(field)) {
				try {
					JSONArray a = result.getJSONArray(field);
					for (int i=0;i<a.length();i++) {
						Object o1 = a.get(i);
						if (o1 instanceof String) {
							String strDate = o1.toString();
							
							ZonedDateTime zdt = DateUtilities.getFromString(strDate);
							if (zdt != null) {
								zdt.withZoneSameInstant( ZoneId.of("UTC"));
								strDate = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
								if (strDate.endsWith("[UTC]")) {
									strDate = strDate.substring(0, strDate.indexOf("[UTC]"));
								}
								
								a.put(i,strDate);
							}	
						}
					}
				}
				catch (Throwable t) {
					;
				}			
				//Temporarily leaving this in place such that I can grab records of these fields from a variety of sources.
				//System.out.println("normalizeDateFields - "+field+": "+o);
			}
			else if (o instanceof JSONArray) {
				JSONArray a = (JSONArray) o;
				for (int i=0;i<a.length();i++) {
					Object o1 = a.get(i);
					if (o1 instanceof JSONObject) {
						normalizeDateFields( (JSONObject) o1);
					}
				}
			}
		}
		
	}

	private JSONObject extractfromJSONScriptPrivate(String scriptContent) {
		try {
			JSONObject jsonObject = new JSONObject(scriptContent);
			this.normalizeJSONLD(jsonObject,null);
			return jsonObject;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private JSONArray extractfromJSONArrayScriptPrivate(String scriptContent) {
		try {
			JSONArray result = new JSONArray(scriptContent);
			return result;
		}
		catch (Exception e) {
			return null;
		}
	}	

	public JSONObject extractFromJSONLDScript(String htmlContent) throws IOException {
		JSONObject result = new JSONObject();
		result.put("items", new JSONArray());
		
		Document doc = Jsoup.parse(htmlContent);
		Elements elements = doc.select("script[type=\"application/ld+json\"]");
		for (Element e: elements) {
			
			String scriptContent = e.html();
			//System.out.println(scriptContent);
			scriptContent = HTMLUtilities.removeCDATATags(scriptContent);
			scriptContent = scriptContent.trim();
			//scriptContent = scriptContent.replaceAll("\n", "\\n");
			String originalContent = scriptContent;
			
			if (scriptContent.startsWith("{")) {
				scriptContent = JSONMinify.minify(scriptContent);
				JSONObject dataObject = this.extractfromJSONScriptPrivate(scriptContent);
				if (dataObject == null) { 
					scriptContent = scriptContent.replaceAll("\\n", " ");
					dataObject = this.extractfromJSONScriptPrivate(scriptContent);
				}
				if (dataObject == null) { 
					scriptContent = scriptContent.replaceAll("\"\""," ");
					scriptContent = scriptContent.replaceAll("\":\\s*?,","\": \"\","); 
					dataObject = this.extractfromJSONScriptPrivate(scriptContent); 
				}
				if (dataObject == null) { 
					scriptContent = scriptContent.replaceAll("\"ImageObject\",s","\"ImageObject\",");
					dataObject = this.extractfromJSONScriptPrivate(scriptContent); 
				}				
				if (dataObject == null) { 
					scriptContent = JSONMinify.minify(scriptContent);
					dataObject = this.extractfromJSONScriptPrivate(scriptContent); 
				}
				if (dataObject != null) {
					result.append("items", dataObject);
				}
				else {			
					logger.log(Level.WARNING, "Unable to convert JSONLD object: "+originalContent);
				}
			}
			else {
				scriptContent = JSONMinify.minify(scriptContent);
				JSONArray dataObject = this.extractfromJSONArrayScriptPrivate(scriptContent);
				if (dataObject == null) { 
					scriptContent = scriptContent.replaceAll("\\n", " ");
					dataObject = this.extractfromJSONArrayScriptPrivate(scriptContent);
				}
				if (dataObject == null) { 
					scriptContent = scriptContent.replaceAll("\"\""," ");
					scriptContent = scriptContent.replaceAll("\":\\s*?,","\": \"\","); 
					dataObject = this.extractfromJSONArrayScriptPrivate(scriptContent); 
				}
				if (dataObject == null) { 
					scriptContent = JSONMinify.minify(scriptContent);
					dataObject = this.extractfromJSONArrayScriptPrivate(scriptContent); 
				}				
				if (dataObject != null) {
					for (int i=0;i<dataObject.length();i++) {
						JSONObject jo = dataObject.optJSONObject(i);
						if (jo != null) {
							this.normalizeJSONLD(jo,null);
							result.append("items", jo);
						}
					}
				}
				else {			
					logger.log(Level.WARNING, "Unable to convert JSONLD object from array: "+originalContent);
				}				
			}
			
		}
		return result;
	}

	private String getContextTypeString(String context,String type) {
		if (context == null || context.length() == 0) {
			return type;
		}
		if (context.endsWith("/") == false) {
			context = context+"/";
		}
		return context+type;
		
	}
	
	private void normalizeJSONLD(JSONObject jsonObject, String context) {
		JSONObject properties = new JSONObject();
		if (jsonObject.has("@context")) {
			if (jsonObject.get("@context") instanceof JSONObject) {
				JSONObject contextObject = jsonObject.getJSONObject("@context");
				if (contextObject.has("@vocab")) {
					context = contextObject.getString("@vocab");
				}
				else {
					context="";
				}
			}
			else {
				context = jsonObject.getString("@context").trim();
			}
			jsonObject.remove("@context");
		}

		for (String key: new java.util.HashSet<String>(jsonObject.keySet())) {
			if (key.equals("@type")) {
				String[] types;
				Object o = jsonObject.get("@type");
				if (o instanceof JSONArray) {
					JSONArray ja = (JSONArray) o;
					types = new String[ja.length()];
					for (int i=0;i<ja.length();i++) {
						types[i] = ja.getString(i);
					}
				}
				else {
					types = o.toString().split("\\s+");
				}
				jsonObject.remove("@type");
				jsonObject.put("type",new JSONArray());
				for (String type: types) {
					jsonObject.append("type",this.getContextTypeString(context, type));
				}
			}
			else if (key.equals("@id")) {
				String id = jsonObject.getString("@id").trim();
				jsonObject.remove("@id");
				jsonObject.put("id", id);
			}
			else {
				Object o = jsonObject.get(key);
				if (o instanceof JSONObject) {
					JSONObject jo = (JSONObject) o;
					this.normalizeJSONLD(jo, context);
				}
				if (o instanceof JSONArray) {
					JSONArray a = (JSONArray) o;
					for (int i=0;i<a.length();i++) {
						Object arrayMember = a.get(i);
						if (arrayMember instanceof JSONObject) {
							this.normalizeJSONLD((JSONObject) arrayMember, context);
						}
						properties.append(key, arrayMember);
					}
				}
				else {
					properties.append(key, o);
				}
				jsonObject.remove(key);
			}
		}
		jsonObject.put("properties", properties);
		
	}

	private void recursivelyWalkForProperties(Element e, JSONObject properties, java.net.URL baseURL, java.util.HashSet<Element> visited){
		if (visited.contains(e)) { return;}
		visited.add(e);
		if (e.hasAttr("itemprop")){
			String propertyName=e.attr("itemprop");
			
			if (!properties.has(propertyName)) {
				properties.put(propertyName, new JSONArray());
			}
			
			if (e.hasAttr("itemscope") ) {
				properties.getJSONArray(propertyName).put(this.extractJSONFromElement(e,baseURL, visited));
			}
			else {
				properties.getJSONArray(propertyName).put(this.getMicroDataPropertyValue(e,baseURL));
			}			
		}
		
		for (Element child: e.children()) {
			this.recursivelyWalkForProperties(child, properties,baseURL,visited);
		}

	}

	private JSONObject extractJSONFromElement(Element e, java.net.URL baseURL, java.util.HashSet<Element> visited) {
		JSONObject result = new JSONObject();
		JSONObject properties = new JSONObject();
		
		if (e.hasAttr("itemtype")) {
			String[] itemTypes = e.attr("itemtype").split("\\s+");
			
			JSONArray types = new JSONArray();
			for (String s:itemTypes) {
				types.put(s);
			}
			result.put("type", types);
		}
		if (e.hasAttr("itemid")) {
			try {
				java.net.URL resolvedURL = new java.net.URL(baseURL,e.attr("itemid"));
				String id = resolvedURL.toString();
				result.put("id", id);
			} catch (MalformedURLException e1) {
				logger.log(Level.FINEST,"unable to resolve baseURL");
				result.put("id", e.attr("itemid"));
			}
		}
		
		for (Element child: e.children()) {
			this.recursivelyWalkForProperties(child, properties,baseURL, visited );
		}
		
		if (properties.length() >0) {
			result.put("properties", properties);
		}
		
		return result;
	}
	

	
	public JSONObject convertMicroData(String htmlContent, java.net.URL baseURL) throws IOException {
		JSONObject result = new JSONObject();
		result.put("items", new JSONArray());
		
		Document doc = Jsoup.parse(htmlContent);
		Elements elements = doc.select("[itemscope]");
		
		for (Element e: elements) {
			if (this.hasParentWithItemScope(e)) {continue; } //Check if any of e anscestors are in parentElements.  If so, skip.
			
			JSONObject obj = this.extractJSONFromElement(e,baseURL, new java.util.HashSet<Element>());
			result.getJSONArray("items").put(obj);
		}
		return result;
	}
	
	/**
	 * Goes up the tree to see if a parent has itemScope.  
	 * If so then this element would have a parent object
	 * and we would include this object within that.
	 * 
	 * @param e node to check
	 * @return
	 */
	private boolean hasParentWithItemScope(Element e) {
		while (e.parent() != null) {
			e = e.parent();
			if (e.hasAttr("itemscope")) { return true; }
		}
		return false;		
	}
	

	/**
	 * http://www.w3.org/TR/microdata/#concept-property-value
	 * 
	 * @param e
	 * @return
	 * @throws MalformedURLException 
	 */
	public String getMicroDataPropertyValue(Element e, java.net.URL baseURL) {
		switch (e.nodeName()) {
			case "meta": return e.attr("content"); 
			case "audio":
			case "img":
			case "source":
			case "track":
			case "video":	return e.attr("src"); 
			case "a":
			case "area":
			case "link": 	try {
								java.net.URL resolvedURL = new java.net.URL(baseURL,e.attr("href"));
								return resolvedURL.toString();
							}
							catch (MalformedURLException mue) {
								logger.log(Level.FINEST,"unable to resolve baseURL");
								return e.attr("href");
							}
			case "embed":
			case "iframe":			                
			case "object":  return e.attr("data"); 
			case "data":    return e.attr("value"); 
			case "meter":   return e.attr("value"); 
			case "time":    if (e.hasAttr("datetime")) {
				                return e.attr("datetime");
			                }
			                else {
			                	return e.ownText();
			                }
			default:		return e.ownText();
		}
	}
		
	// CODE BELOW IS FOR RDFa Extraction	
	
	
	/**
	 * Generates a hashmap of any prefixes used from the current point in the document
	 * structure up to the root.
	 * 
	 * @param e
	 * @return
	 */
	private java.util.HashMap<String,String> getPrefixMappings(Element e) {
		java.util.HashMap<String,String> result = new java.util.HashMap<String,String>();
		
		while (e != null) {
			if (e.hasAttr("prefix")) {
				String[] prefixes = e.attr("prefix").split("\\s+");
				for (int i=0;i<prefixes.length; i += 2) {
					if (result.containsKey(prefixes[i])) { continue;}
					result.put(prefixes[i], prefixes[i+1]);
				}
			}
			e = e.parent();
		}
		
		return result;
	}
	
	private String resolvePrefixedName(java.util.HashMap<String,String> prefixMappings, String value) {
		for (String prefix: prefixMappings.keySet()) {
			if (value.startsWith(prefix)) {
				return value.replace(prefix, prefixMappings.get(prefix));
			}
		}
		return value;
	}
	
	private void recursivelyWalkForPropertiesRDFa(Element e, JSONObject properties, java.net.URL baseURL, 
			                                      java.util.HashSet<Element> visited, String context,
			                                      java.util.HashMap<String,String> prefixMappings){
		if (visited.contains(e)) { return;}
		visited.add(e);
		
		if (e.hasAttr("prefix")) {
			prefixMappings = new java.util.HashMap<String,String>(prefixMappings);
			String[] prefixes = e.attr("prefix").split("\\s+");
			for (int i=0;i<prefixes.length; i += 2) {
				prefixMappings.put(prefixes[i], prefixes[i+1]);
			}
		}
		
		
		if (e.hasAttr("property")){
			String propertyName= this.resolvePrefixedName(prefixMappings, e.attr("property"));
			
			if (!properties.has(propertyName)) {
				properties.put(propertyName, new JSONArray());
			}
			
			if (e.hasAttr("typeof") || e.hasAttr("resource") || e.hasAttr("about") ) {
				properties.getJSONArray(propertyName).put(this.extractJSONFromElementRDFa(e,baseURL, visited,context,prefixMappings));
			}
			else {
				properties.getJSONArray(propertyName).put(this.getRDFaDataPropertyValue(e,baseURL));
			}			
		}
		if (e.hasAttr("rel")) {
			String propertyName= this.resolvePrefixedName(prefixMappings, e.attr("rel"));
			if (!properties.has(propertyName)) {
				properties.put(propertyName, new JSONArray());
			}
			
			Elements elements = e.select("[typeof],[resource]");
			for (Element child: elements) {
				JSONObject obj = this.extractJSONFromElementRDFa(child,baseURL, new java.util.HashSet<Element>(),context,prefixMappings);
				properties.getJSONArray(propertyName).put(obj);
			}
			
			
			return;
		}
		
		for (Element child: e.children()) {
			this.recursivelyWalkForPropertiesRDFa(child, properties,baseURL,visited, context,prefixMappings);
		}
	}

	private JSONObject extractJSONFromElementRDFa(Element e, java.net.URL baseURL, java.util.HashSet<Element> visited, 
			                                      String context, java.util.HashMap<String,String> prefixMappings) {
		JSONObject result = new JSONObject();
		JSONObject properties = new JSONObject();
		
		if (e.hasAttr("vocab")) {
			context = this.resolvePrefixedName(prefixMappings, e.attr("vocab").trim());
			if (!context.endsWith("/")) { context += "/"; }
		}
		
		
		if (e.hasAttr("typeof")) {
			String[] itemTypes = e.attr("typeof").split("\\s+");
			
			JSONArray types = new JSONArray();
			for (String s:itemTypes) {
				String preFixedString = this.resolvePrefixedName(prefixMappings, s);
				if (preFixedString == s) {
					types.put( context+s);
				}
				else {
					types.put(preFixedString);
				}
			}
			result.put("type", types);
		}
		if (e.hasAttr("resource")) {
			String[] itemTypes = e.attr("resource").split("\\s+");
			
			JSONArray types = new JSONArray();
			for (String s:itemTypes) {
				String preFixedString = this.resolvePrefixedName(prefixMappings, s);
				if (preFixedString == s) {
					try {
						java.net.URL resolvedURL = new java.net.URL(baseURL,s);
						types.put( resolvedURL.toString());
					} catch (MalformedURLException e1) {
						logger.log(Level.FINEST,"unable to resolve baseURL");
						types.put( s);
					}
				}
				else {
					types.put(preFixedString);
				}
			}
			result.put("resource", types);
		}
		if (e.hasAttr("about")) {
			String[] itemTypes = e.attr("about").split("\\s+");
			
			JSONArray types = new JSONArray();
			for (String s:itemTypes) {
				String preFixedString = this.resolvePrefixedName(prefixMappings, s);
				if (preFixedString == s) {
					try {
						java.net.URL resolvedURL = new java.net.URL(baseURL,s);
						types.put( resolvedURL.toString());
					} catch (MalformedURLException e1) {
						logger.log(Level.FINEST,"unable to resolve baseURL");
						types.put( s);
					}
				}
				else {
					types.put(preFixedString);
				}
			}
			result.put("about", types);
		}
		if (e.hasAttr("id")) {
			try {
				java.net.URL resolvedURL = new java.net.URL(baseURL,e.attr("id"));
				String id = resolvedURL.toString();
				result.put("id", id);
			} catch (MalformedURLException e1) {
				logger.log(Level.FINEST,"unable to resolve baseURL");
				result.put("id", e.attr("itemid"));
			}
		}
		
		for (Element child: e.children()) {
			this.recursivelyWalkForPropertiesRDFa(child, properties,baseURL, visited, context, prefixMappings );
		}
		
		if (properties.length() >0) {
			result.put("properties", properties);
		}
		
		return result;
	}
	
	public String getContextForRDFa(Element e) {
		String result = "";
		
		while (e != null) {
			if (e.hasAttr("vocab")) {
				result = e.attr("vocab");
				if (!result.endsWith("/")) {result += "/";
				return result;
				}
			}
			e = e.parent();
		}
		
		return result;
	}

	
	public JSONObject convertRDFa(String htmlContent, java.net.URL baseURL) throws IOException {
		JSONObject result = new JSONObject();
		result.put("items", new JSONArray());
		
		Document doc = Jsoup.parse(htmlContent);
		
		
		
		
		Elements elements = doc.select("[typeof],[resource],[about]");
		if (elements.size() == 0) {
			if (doc.select("[vocab]").size() >0 || doc.select("[property]").size() >0) {
				doc.select("html").first().attr("typeof","");
				elements = doc.select("[typeof],[resource],[about]");
			}
		}
		
		for (Element e: elements) {
			if (this.hasParentWithTypeOf(e)) {continue; } //Check if any of e anscestors are in parentElements.  If so, skip.
			
			java.util.HashMap<String,String> prefixes = this.getPrefixMappings(e);
			
			JSONObject obj = this.extractJSONFromElementRDFa(e,baseURL, new java.util.HashSet<Element>(),this.getContextForRDFa(e),prefixes);
			result.getJSONArray("items").put(obj);
		}
		return result;
	}
	
	/**
	 * Goes up the tree to see if a parent has itemScope.  
	 * If so then this element would have a parent object
	 * and we would include this object within that.
	 * 
	 * @param e node to check
	 * @return
	 */
	private boolean hasParentWithTypeOf(Element e) {
		while (e.parent() != null) {
			e = e.parent();
			if (e.hasAttr("typeOf") || e.hasAttr("resource")) { return true; }
		}
		return false;		
	}
	

	/**
	 * 
	 * 
	 * @param e
	 * @return
	 * @throws MalformedURLException 
	 */
	public String getRDFaDataPropertyValue(Element e, java.net.URL baseURL) {
		switch (e.nodeName()) {
			case "meta": return e.attr("content"); 
			case "audio":
			case "embed":
			case "iframe":
			case "img":
			case "source":
			case "track":
			case "video":	return e.attr("src"); 
			case "a":
			case "area":
			case "link": 	try {
								java.net.URL resolvedURL = new java.net.URL(baseURL,e.attr("href"));
								return resolvedURL.toString();
							}
							catch (MalformedURLException mue) {
								logger.log(Level.FINEST,"unable to resolve baseURL");
								return e.attr("href");
							}
			                
			case "object":  return e.attr("data"); 
			case "data":    return e.attr("value"); 
			case "meter":   return e.attr("value"); 
			case "time":    if (e.hasAttr("datetime")) {
				                return e.attr("datetime");
			                } else if (e.hasAttr("content")) {
                                return e.attr("content");
				            }
			                else {
			                	return e.ownText();
			                }
			default:		if (e.hasAttr("content")) {
                                return e.attr("content");
				            }
				            else {
				            	return e.ownText();
				            }
		}
	}		
	
	
}
