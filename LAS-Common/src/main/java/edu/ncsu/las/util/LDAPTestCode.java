package edu.ncsu.las.util;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import edu.ncsu.las.model.Person;
import edu.ncsu.las.persist.DBConstants;
import edu.ncsu.las.persist.DataSourceManager;


public class LDAPTestCode {
	
	
	private static void testPersonRetrieval(String uid) {
		
		
		Person p = Person.createPerson(uid);
		if (p != null) {
			System.out.println("dn: "+p.getAttribute("dn"));
			System.out.println("First Name: "+p.getGivenName());
			System.out.println("E-mail: "+p.getEmailAddress());
		}
		else {
			System.out.println(uid+": not found!");
		}
	}
	
	
	public static void main(String args[]) throws Exception {  // since I'm lazy
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://ldap.ncsu.edu:389");
		env.put("com.sun.jndi.ldap.connect.pool", "true");
		DataSourceManager.getTheDataSourceManager().addLDAPEnvironments(DBConstants.PERSON_LDAP, env);

	    testPersonRetrieval("trash");


	}
	
	
	
	
	
	
    static void printAttrs(Attributes attrs) {
	if (attrs == null) {
	    System.out.println("    No attributes");
	} else {
	    /* Print each attribute */
	    try {
		for (NamingEnumeration<? extends Attribute> ae = attrs.getAll();
		     ae.hasMore();) {
		    Attribute attr = (Attribute)ae.next();
		    System.out.print("    " + attr.getID() +": ");

		    /* print each value */
		    for (NamingEnumeration<?> e = attr.getAll();
			 e.hasMore();
			 System.out.print(e.next() +" " ));
			;
			System.out.println();
		}
	    } catch (NamingException e) {
		e.printStackTrace();
	    }
	}
    }	
	
}
