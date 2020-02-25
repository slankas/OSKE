package edu.ncsu.las.rest.collector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.RoleType;



/**
 * Returns back data for any hyperlinks needed by the application
 * 
 */
@RequestMapping(value = "rest/{domain}/link")
@Controller
public class LinkController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "home", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String pageHyperlinks(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr) throws ValidationException {
		logger.log(Level.INFO,	"LinkController: received request to get page hyperlinks");
		this.validateAuthorization(httpRequest, domainStr, RoleType.ANALYST);
		this.instrumentAPI("edu.ncsu.las.rest.collector.LinkController.pageHyperlinks", new JSONObject(), System.currentTimeMillis(),null, httpRequest,domainStr);

		JSONArray hyperlinks = Configuration.getConfigurationPropertyAsArray(domainStr, ConfigurationType.HYPERLINKS);
		return hyperlinks.toString();
	}
}