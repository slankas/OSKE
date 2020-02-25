package edu.ncsu.las.rest.collector;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.ValidationException;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.SiteCrawlRule;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.User.RoleDomainAccess;
import edu.ncsu.las.model.collector.type.RoleType;


/**
 * Handles requests for the application home page, managing domains, and editing(adding) domains
 */
@RequestMapping(value = "rest/domain/")
@Controller
public class DomainController extends AbstractRESTController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	
	/**
	 * Returns all active domains that the user can access
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getAllAccesibleDomains(HttpServletRequest httpRequest, @RequestParam(value="all", required=false,defaultValue="false") String allDomains)	throws IOException, ValidationException {
		logger.log(Level.FINER,	"DomainController: get all domains");
		this.validateAnyAuthorization(httpRequest);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.getAllAccesibleDomains", new JSONObject(), System.currentTimeMillis(), null, httpRequest,Domain.DOMAIN_SYSTEM);

		JSONObject result = new JSONObject();
		JSONArray domains = new JSONArray();
		
		if (allDomains.equalsIgnoreCase("true")) {
			//TODO: implement routine to get all possible domains, not just those that are active
		}
		else {
			User u = this.getUser(httpRequest);
			
			for (Domain d: Domain.findAllActiveDomains()) {
				if (u.hasDomainAccess(d.getDomainInstanceName())) {
					domains.put(d.toJSON());		
				}
			}
		}
			
		result.put("domains", domains);
		return result.toString();		

	}
	
	
	/**
	 * validates and creates a new domain as appropriate
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, headers = "Accept=application/json")
	public @ResponseBody String createDomain(HttpServletRequest httpRequest, @RequestBody String domainStr) throws IOException, ValidationException {
		logger.log(Level.FINER, "DomainController - create new domain");
		this.validateAuthorization(httpRequest, Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR);
		User u = this.getUser(httpRequest);
		
		JSONObject domainJSON = new JSONObject(domainStr);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.createDomain", domainJSON, System.currentTimeMillis(), null, httpRequest,Domain.DOMAIN_SYSTEM);

		
		JSONArray errors = Domain.validate(domainJSON,true);
		if (errors.length() >0 ) {
			JSONObject result = new JSONObject().put("status", "error").put("message", errors);
			throw new ValidationException(org.json.simple.JSONObject.escape(result.toString()));
		}
		
		Domain newDomain = new Domain(domainJSON);
		if (newDomain.create(u.getEmailID())) {
			
			RoleDomainAccess rda = u.new RoleDomainAccess(RoleType.ADMINISTRATOR, newDomain.getDomainInstanceName(), "active", Timestamp.from(Instant.now()), u.getEmailID());
			u.createUser(rda);
			u.addAccess(rda);
			
			SiteCrawlRule.copySiteRules(Domain.DOMAIN_SYSTEM, newDomain.getDomainInstanceName());
			this.refreshDomains(newDomain);
			return newDomain.toJSON().toString();
		}
		else {
			throw new ValidationException("unable to create domain record");
		}
		
	}

	/**
	 * Returns the sample configuration for domains.
	 * 
	 * @param request
	 * @param httpRequest
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "system/config", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getExampleConfigruation(HttpServletRequest request, HttpServletRequest httpRequest) throws  IOException, ValidationException {
		logger.log(Level.FINER,	"DomainController: received request to get example configuration");
		this.validateAnyAuthorization(request);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.getExampleConfigruation", new JSONObject(), System.currentTimeMillis(), null, httpRequest,Domain.DOMAIN_SYSTEM);


		return Configuration.SAMPLE_DOMAIN_CONFIGURATION;
	}	
	
	/**
	 * Returns the base system configruation for the system
	 * 
	 * @param request
	 * @param httpRequest
	 * @return
	 * @throws IOException
	 * @throws ValidationException
	 */
	@RequestMapping(value = "system/systemConfig", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getSystemConfigruation(HttpServletRequest request, HttpServletRequest httpRequest) throws  IOException, ValidationException {
		logger.log(Level.FINER, "DomainController - view System configuration");
		this.validateAuthorization(request, Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.getSystemConfigruation", new JSONObject(), System.currentTimeMillis(), null, httpRequest,Domain.DOMAIN_SYSTEM);

		return 	Collector.getTheCollecter().getBaseConfiguration().getConfiguration().toString();
	}		
	
	
	/**
	 * Simple check if a domain exists.  Return HttpStatus "OK" (200) if so, otherwise a 404, if it does not exist.
	 */
	@RequestMapping(value = "{domain}", method = RequestMethod.HEAD, headers = "Accept=application/json")
	public @ResponseBody  ResponseEntity<String> checkDomainExists(HttpServletRequest httpRequest,@PathVariable("domain") String domainStr)  {
		logger.log(Level.FINER, "Domain Controller: checks if domain exists");
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.checkDomainExists", new JSONObject().put("checkDomain",domainStr), System.currentTimeMillis(), null, httpRequest,Domain.DOMAIN_SYSTEM);
		
		Domain domain = Domain.findDomain(domainStr);
		if (domain != null) {
			return new ResponseEntity<String>(HttpStatus.OK);
		}
		else {
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND); 
		}
	}		
	

	/**
	 * Returns a specific domain.  In this case, we utilize the latest value from the database rather than 
	 * relying upon the collector's cache of domain in case a change has occured and the system cache was not refreshed.
	 * 
	 */
	@RequestMapping(value = "{domain}", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getSpecificDomain(HttpServletRequest request, @PathVariable("domain") String domainStr, HttpServletRequest httpRequest) throws  IOException, ValidationException {
		logger.log(Level.FINER,	"DomainController: received request to get a specific domain");
		this.validateAuthorization(request, Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.getDomain", new JSONObject(), System.currentTimeMillis(), null, httpRequest,domainStr);

		Domain domain = Domain.findDomain(domainStr);
		if (domain == null) {
			throw new ValidationException("Domain not found: "+domainStr);
		}
		
		return domain.toJSON().toString();
	}

	/**
	 * Returns the established date for the specified domain. Date uses ISO-8601 representation (e.g., 2018-05-22T15:23:39Z)
	 */
	@RequestMapping(value = "{domain}/establishedDate", method = RequestMethod.GET, headers = "Accept=application/json")
	public @ResponseBody String getSpecificDomainEstablishedDate(HttpServletRequest request, @PathVariable("domain") String domainStr, HttpServletRequest httpRequest) throws  IOException, ValidationException {
		logger.log(Level.FINER,	"DomainController: received request to get a specific domain");
		this.validateAuthorization(request, Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.getDomainEstablishedDate", new JSONObject(), System.currentTimeMillis(), null, httpRequest,domainStr);

		Domain domain = Collector.getTheCollecter().getDomain(domainStr);
		if (domain == null) {
			throw new ValidationException("Domain not found: "+domainStr);
		}
		JSONObject result = new JSONObject().put("establishedDate", domain.getEstalblishedTimeStamp().toString());
		
		return result.toString();
	}
	
	
	@RequestMapping(value = "{domain}", method = RequestMethod.PUT, headers = "Accept=application/json")
	public @ResponseBody String updateDomain(@PathVariable("domain") String domainStr, @RequestBody String requestBodyStr, HttpServletRequest httpRequest) throws IOException, ValidationException {
		this.validateAuthorization(httpRequest, Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR);
		logger.log(Level.FINER, "Edit domain: "+ domainStr);
		
		JSONObject domainJSON = new JSONObject(requestBodyStr);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.updateDomain", domainJSON, System.currentTimeMillis(), null, httpRequest,domainStr);

		JSONArray errors = Domain.validate(domainJSON, !domainStr.equals(Domain.DOMAIN_SYSTEM));
		if (errors.length() >0 ) {
			JSONObject result = new JSONObject().put("status", "error").put("message", errors);
			throw new ValidationException(org.json.simple.JSONObject.escape(result.toString()));
		}
		
		Domain newDomain = new Domain(domainJSON);
		if (newDomain.create(this.getEmailAddress(httpRequest))) {
			this.refreshDomains(null);
			return newDomain.toJSON().toString();
		}
		else {
			throw new ValidationException("unable to create domain record (for update)");
		}		
	}
	
	/**
	 * Called to completely erase all crawled / collected data from a domain.  Concepts and Jobs are still maintained.
	 * Must be explicitly enabled in a configuration setting to allow this to occur.
	 * 
	 * @param request
	 * @param domain
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "purgeDomain/{domain}", method = RequestMethod.POST)
	public @ResponseBody String purgeDomain(HttpServletRequest httpRequest,  @PathVariable("domain") String domainStr) throws Exception {		
		logger.log(Level.FINER, "Purge domain: "+domainStr);
		this.validateAuthorization(httpRequest, Domain.DOMAIN_SYSTEM, RoleType.ADMINISTRATOR);
		this.instrumentAPI("edu.ncsu.las.rest.collector.DomainController.purgeDomain", new JSONObject(), System.currentTimeMillis(), null, httpRequest,domainStr);

		JSONObject errors = Collector.getTheCollecter().getDomain(domainStr).purge();		
		if (errors.length() == 0) {
			return new JSONObject().put("status", "Success").put("message", "domain Successfully Purged").toString();
		}
		else {
			return new JSONObject().put("status", "incomplete").put("message", errors).toString();
		}

	}	

	private void refreshDomains(Domain newDomain) {
		Collector.getTheCollecter().runTask(new Runnable() {
		    @Override 
		    public void run() {
		    	try {
			    	logger.log(Level.FINER, "DomainController - pausing before refreshing domains");
			    	TimeUnit.SECONDS.sleep(5);  
			    	Collector.getTheCollecter().refreshDomains();
			    	if (newDomain != null) {
			    		newDomain.startInteractiveServices();
			    	}
			    	logger.log(Level.FINER, "DomainController - domains refreshed");
		    	}
		    	catch (InterruptedException ie) {
		    		logger.log(Level.FINER, "DomainController - InterruptedException received while refresehing domains");
		    	}
		    }
		});
	}
	
}