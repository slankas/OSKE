package edu.ncsu.las.controller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.ncsu.las.controller.AbstractController;
import edu.ncsu.las.model.Person;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.rest.collector.AbstractRESTController;
import edu.ncsu.las.util.json.JSONUtilities;

/**
 * Provides people data when searching for users
 */
@Controller
@RequestMapping("/rest/person")
public class PeopleController extends AbstractController {

	protected static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "/query", method = RequestMethod.GET, headers = "Accept=application/xml, application/json")
	public @ResponseBody Person.PersonList queryForPeople(@RequestParam(value = "searchString", required = true) String SearchString,
														  @RequestParam(value = "limit", required=false ) Integer limit) throws JsonGenerationException, JsonMappingException, IOException {

		logger.log(Level.FINE,"PersonController got a search request: " + SearchString);
		
		JSONArray jFields = Configuration.getConfigurationPropertyAsArray(Domain.DOMAIN_SYSTEM, ConfigurationType.LDAP_SEARCH_FIELDS);
		
		List<String> fields = JSONUtilities.toStringList(jFields);
		
    	Person.PersonList result = Person.getPeople(SearchString, fields,Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.LDAP_BASE_DN));
    
    	if (limit != null) {
    		//truncate array to limit if neeeded
    	}
		return result;
	}
}