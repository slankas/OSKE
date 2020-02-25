package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationTypeInterface;
import edu.ncsu.las.model.collector.type.RoleType;



/**
 * Retrieves configuration parameters if exposable
 */
@RequestMapping(value = "rest/{domain}/configuration")
@Controller
public class ConfigurationController extends AbstractRESTController{
	
	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());
	
	
	@RequestMapping(value = "/{full_label:.+}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody byte[] getExposableConfiguration(HttpServletRequest request,  @PathVariable("domain") String domainStr, @PathVariable("full_label") String fullLabel) throws IOException, ValidationException {
		logger.log(Level.INFO, "NLPController - process");
		this.validateAuthorization(request, domainStr, RoleType.ANALYST);
		
		this.instrumentAPI("edu.ncsu.las.rest.collector.ConfigurationController.getExposableConfiguration", new JSONObject(), System.currentTimeMillis(), null, request,domainStr);
		
		JSONObject result;
		
		ConfigurationTypeInterface ct = ConfigurationTypeInterface.getConfigurationType(fullLabel);
		if (ct == null) {
			result = new JSONObject().put("status", "failed")
					                            .put("message", "no such configuration type: "+fullLabel);
		}
		else {
			if (ct.exposeToUserInterface()) {			
				String value = Configuration.getConfigurationProperty(domainStr, ct);
				if (value == null) {
					result = new JSONObject().put("status", "failed")
	                                         .put("message", "no value for configuration type: "+fullLabel);
				}
				else {
					result = new JSONObject().put("status", "success")
	                                         .put("value", value)
	                                         .put("configuration", fullLabel);
				}
			}
			else {
				result = new JSONObject().put("status", "failed")
                                         .put("message", "configuration type cannot be exposed: "+fullLabel);
			}
		}
		
		return result.toString().getBytes("UTF-8");			
	}		

		
}	
