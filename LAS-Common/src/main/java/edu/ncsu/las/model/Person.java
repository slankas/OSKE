package edu.ncsu.las.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import edu.ncsu.las.model.Person;
import edu.ncsu.las.persist.PersonLDAP;

/**
 * Represents a "person" for the system. This can be retrieved the NCSU LDAP
 * directory. Preference is given to "employee" records over "student" records.
 * 
 *
 */
@XmlRootElement(name = "person")
public class Person implements Comparable<Person> {

	private HashMap<String, String> _attributes;

	private Person() {

	}
	
	public boolean equals(Object val) {
		if (val instanceof Person) {
			Person p2 = (Person) val;
			return p2.getUserID().equals(this.getUserID());
		}
		return false;
	}
	
	public Person(HashMap<String,String> attributes) {
		this._attributes = attributes;
	}

	public static Person createPerson(String emailID) {

		Person p = new Person();
		p._attributes = PersonLDAP.getPersonAttributes(emailID);
		if (p._attributes == null) {
			return null; // couldn't locate record
		}

		return p;
	}

	public String getAttribute(String attributeName) {
		return _attributes.get(attributeName);
	}

	public String getGivenName() {
		if (_attributes.get("displayName") != null) {
			return _attributes.get("displayName");
		} else {
			return _attributes.get("givenName");
		}
	}

	public String getDisplayName() {
		if (_attributes.get("displayName") != null) {
			return _attributes.get("displayName");
		} else {
			return getGivenName();
		}
	}

	public String getEmailAddress() {
		return this.getUserID();

	}

	public String getUserID() {
		return _attributes.get("mail");
	}
	
	public String getOrganization() {
		if (_attributes.get("ou") == null) { return ""; }
		else {return _attributes.get("ou"); }
	}
	
	public String getDepartmentNumber() {
		if (_attributes.get("departmentNumber") == null) { return ""; }
		else {return _attributes.get("departmentNumber"); }
	}

	/*
	public HashMap<String,String> getAttributes() {
		return _attributes;
	}
	*/
	
	

	public int hashCode() {
		return this.getUserID().hashCode();
	}
	
	public static PersonList getPeople(String searchTerm, List<String> searchFields, String baseDN){
		
		List<Person> people =  PersonLDAP.getMatchingPeople(searchTerm, searchFields, baseDN);
		HashSet<Person> peopleSet = new HashSet<Person>(people);
		people = new ArrayList<Person>(peopleSet);
		
		Collections.sort(people);
		
		PersonList result = new PersonList(people.toArray(new Person[0]));

		return result;	
	}
	
	
	@XmlRootElement(name="people")
	public static class PersonList {
		
		@XmlElement (name="person")
		public Person[] People;
		
		public PersonList() { }

		public PersonList(Person[] values) {
			People = values;
		}
	}


	@Override
	public int compareTo(Person o) {
		return this.getGivenName().compareTo(o.getGivenName());
	}
	
	/**
	 * Attempts to create a person account based up the based
	 * email address
	 * 
	 * @param emailID
	 * @return person object if it exists.  Null if not found.
	 */
	public static Person getPersonByEmailAddress(String emailID) {
		return createPerson(emailID);
	}
}
