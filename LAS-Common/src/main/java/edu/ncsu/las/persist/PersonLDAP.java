package edu.ncsu.las.persist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import edu.ncsu.las.model.Person;


/**
 * Retrieves records from an LDAP server
 * 
 *
 */
public class PersonLDAP  {
	private static final Logger logger =Logger.getLogger(PersonLDAP.class.getName());
	
	/**
	 * 
	 * @param uid
	 * @param recordType  "employees" or "students"
	 * @return
	 */
	public static HashMap<String,String> getPersonAttributes(String emailID) {
		
		SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		
        DirContext ctx = DataSourceManager.getTheDataSourceManager().getLDAPContext(DBConstants.PERSON_LDAP);
       
        if (ctx == null) {
        	return null;
        }
        
        try {
        	NamingEnumeration<SearchResult> e = ctx.search("ou=people,dc=ncsu,dc=edu", "(mail="+emailID+")",searchControls);

		    if (e.hasMore()) {
		    	 HashMap<String,String> result = new HashMap<String,String>();
		    	 SearchResult entry =  e.next();
		    	 result.put("dn", entry.getNameInNamespace());
		    	 	    	 
		    	 Attributes attributes = entry.getAttributes();
		    	 NamingEnumeration<?> ae = attributes.getAll();
		    	 
		    	 while (ae.hasMore()) {
		    		 Attribute attr = (Attribute)ae.next();
		    		 result.put(attr.getID(), attr.get().toString());
		    	 }
		    	 
		    	 return result;
		    }
        }
        catch (NamingException ne) {
        	logger.log(Level.SEVERE,ne.toString());
        }
        finally {
        	try {
        		if (ctx != null) {ctx.close(); } // returns the ldap connection back to the pool
        	}
        	catch (NamingException ne2) {
        		logger.log(Level.SEVERE,"unable to close ldap connection");
        	}
        }
        return null;
	}	   

	
	/**
	 * Returns an array 
	 * 
	 * @param uid
	 * @param recordType  "employees" or "students"
	 * @return
	 */
	public static List<Person> getMatchingPeople(String searchTerm, List<String> searchFields, String dnBase) {
		ArrayList<Person> results = new ArrayList<Person>();
		
		SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		
        DirContext ctx = DataSourceManager.getTheDataSourceManager().getLDAPContext(DBConstants.PERSON_LDAP);
       
        if (ctx == null) {
        	return results;
        }
        String wildSearchTerm = "*" + searchTerm.replace(" ","*") + "*";
        
        String searchString="";
        if (searchFields.size() == 0) { return results; }
        else if (searchFields.size() == 1) {
        	searchString = "(" + searchFields.get(0) +"="+wildSearchTerm+")";
        }
        else {
        	StringBuilder sb = new StringBuilder();
        	sb.append("(|");
        	for (String field: searchFields) {
        		sb.append("(");
        		sb.append(field);
        		sb.append("=");
        		sb.append(wildSearchTerm);
        		sb.append(")");
        	}
        	sb.append(")");
        	searchString = sb.toString();
        }
        
        
        try {
        	NamingEnumeration<SearchResult> e = ctx.search(dnBase, searchString,searchControls);

		    while (e.hasMore()) {
		    	 HashMap<String,String> result = new HashMap<String,String>();
		    	 SearchResult entry =  e.next();
		    	 result.put("dn", entry.getNameInNamespace());
		    	 	    	 
		    	 Attributes attributes = entry.getAttributes();
		    	 NamingEnumeration<?> ae = attributes.getAll();
		    	 
		    	 while (ae.hasMore()) {
		    		 Attribute attr = (Attribute)ae.next();
		    		 result.put(attr.getID(), attr.get().toString());
		    	 }
		    	 if (result.containsKey("mail")) {  // mail is a required field for our system.
		    		 results.add(new Person(result));
		    	 }
		    }
        }
        catch (NamingException ne) {
        	logger.log(Level.SEVERE,ne.toString());
        }
        finally {
        	try {
        		if (ctx != null) {ctx.close(); } // returns the ldap connection back to the pool
        	}
        	catch (NamingException ne2) {
        		logger.log(Level.SEVERE,"unable to close ldap connection");
        	}
        }
        return results;
	}	   
	
}
