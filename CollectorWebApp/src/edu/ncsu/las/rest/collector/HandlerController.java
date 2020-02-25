package edu.ncsu.las.rest.collector;

import java.util.Collection;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.annotator.Annotator;
import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.document.DocumentHandler;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.source.AbstractHandler;
import edu.ncsu.las.source.SourceHandlerInterface;


/**
 * Returns back data for the document and source handlers
 * 
 */
@RequestMapping(value = "rest/{domain}/handler/")
@Controller
public class HandlerController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "annotator", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String annotatorHandlerTable(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr) throws ValidationException {
		logger.log(Level.FINE,"HandlerController: received request to refresh Document Handler Table");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.HandlerController.annotatorHandlerTable", new JSONObject(), System.currentTimeMillis(), null, httpRequest, domainStr);
		
		List<Annotator> annotators = Collector.getTheCollecter().getAnnotators();
		
		JSONArray handlers = new JSONArray();
		for (Annotator a : annotators) {
			handlers.put(a.toJSON());
		}
		return handlers.toString();
	}	
	
	@RequestMapping(value = "document", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String docHandlerTable(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr) throws ValidationException {
		logger.log(Level.FINE,"HandlerController: received request to refresh Document Handler Table");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.HandlerController.documentHandlerTable", new JSONObject(), System.currentTimeMillis(), null, httpRequest, domainStr);

		List<DocumentHandler> dochandlers = Collector.getTheCollecter().getDocumentHandlers();

		JSONArray handlers = new JSONArray();
		for (DocumentHandler h : dochandlers) {
			handlers.put(h.toJSON());
		}
		return handlers.toString();
	}

	@RequestMapping(value = "source", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String srcHandlerTable(HttpServletRequest httpRequest, @PathVariable("domain") String domainStr) throws ValidationException{
		logger.log(Level.FINE, "HandlerController: received request to refresh Source Handler Table");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.HandlerController.sourceHandlerTable", new JSONObject(), System.currentTimeMillis(), null, httpRequest, domainStr);

		Collection<AbstractHandler> srchandlers = Collector.getTheCollecter().getSourceHandlers().values();
		JSONArray array = Configuration.getConfigurationPropertyAsArray(domainStr, ConfigurationType.SOURCE_HANDLERS);
		java.util.HashSet<String> availableHandlers = new java.util.HashSet<String>();
		for (int i=0;i<array.length();i++) { availableHandlers.add(array.getString(i)); }
		
		JSONArray handlers = new JSONArray();
		
		for (SourceHandlerInterface h : srchandlers) {
			if (availableHandlers.contains(h.getSourceHandlerName())) {
				handlers.put(h.toJSON());
			}
		}

		return  handlers.toString();
	}

	@RequestMapping(value = "source/{sourceHandlerName}/defaultConfig", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getDefaultConfiguration(HttpServletRequest httpRequest, @PathVariable("sourceHandlerName") String sourceHandlerName, @PathVariable("domain") String domainStr) throws ValidationException {
		logger.log(Level.FINE,	"HandlerController: received request to getDefaultConfiguration()");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.HandlerController.getDefaultHandlerConfiguration", new JSONObject().put("handler", sourceHandlerName), System.currentTimeMillis(), null, httpRequest, domainStr);
		
		AbstractHandler shi = AbstractHandler.getSourceHandler(sourceHandlerName);
		if (shi != null) {
			return shi.getHandlerDefaultConfiguration().toString();
		}
		else {
			throw new ValidationException("SourceHandlerInterface not found: "+sourceHandlerName, HttpServletResponse.SC_NOT_FOUND);
		}
	}		
	
	@RequestMapping(value = "source/{sourceHandlerName}/param", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String displaySrcConfigParams(HttpServletRequest httpRequest,	@PathVariable("sourceHandlerName") String sourceHandlerName, @PathVariable("domain") String domainStr) throws ValidationException {
		logger.log(Level.FINE,	"HandlerController: received request to displaySrcConfigParams()");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.HandlerController.getHandlerParameters", new JSONObject().put("handler", sourceHandlerName), System.currentTimeMillis(), null, httpRequest, domainStr);

		AbstractHandler shi = AbstractHandler.getSourceHandler(sourceHandlerName);
		if (shi != null) {
			JSONObject result = new JSONObject();
			JSONArray  parameters = new JSONArray();
			
			java.util.TreeMap<String, SourceParameter> params = shi.getConfigurationParameters();
			
			// Sorting on the server side to avoid the client-side sort.
			java.util.ArrayList<String> keys = new java.util.ArrayList<String>(params.keySet());
			keys.sort((k1, k2) -> k1.compareTo(k2));
			
			for (String key: keys) {
				SourceParameter sp = params.get(key);
				JSONObject entry = new JSONObject();
				entry.put("name", sp.getName());
				entry.put("description", sp.getDescription());
				entry.put("required", sp.isRequired());
				entry.put("example", sp.getExample());
				parameters.put(entry);
			}
			result.put("parameters", parameters);
			return result.toString();
		}
		else {
			throw new ValidationException("SourceHandlerInterface not found: "+sourceHandlerName, HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "source/{sourceHandlerName}/config", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getFullConfiguration(HttpServletRequest httpRequest, @PathVariable("sourceHandlerName") String sourceHandlerName, @PathVariable("domain") String domainStr) throws ValidationException {
		logger.log(Level.FINE,	"HandlerController: received request to getFullConfiguration()");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.HandlerController.getFullConfiguration", new JSONObject().put("handler", sourceHandlerName), System.currentTimeMillis(), null, httpRequest, domainStr);

		
		AbstractHandler shi = AbstractHandler.getSourceHandler(sourceHandlerName);
		if (shi != null) {
			return shi.getSampleConfiguration();
		}
		else {
			throw new ValidationException("SourceHandlerInterface not found: "+sourceHandlerName, HttpServletResponse.SC_NOT_FOUND);
		}
	}	
}