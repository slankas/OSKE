package edu.ncsu.las.model.collector;

import java.time.Instant;

import edu.ncsu.las.persist.collector.UserOptionDAO;

/**
 * Manages user-level options.
 * 
 * 
 */
public class UserOption {
	
	private String _emailID;
    private String _domainInstanceName;
    private UserOptionName _optionName;
    private String _optionValue;
	private Instant _changedDateTime;
	  
	public UserOption(String emailID, String domainInstanceName, String optionName, String optionValue, Instant changedDateTime) {
		_emailID = emailID;
	    _domainInstanceName = domainInstanceName;
	    _optionName = UserOptionName.getOptionName(optionName);
	    _optionValue = optionValue;
		_changedDateTime = changedDateTime;
	}
	
	
	public  UserOption(String emailID, String domainInstanceName, UserOptionName optionName, String optionValue) {
		_emailID = emailID;
	    _domainInstanceName = domainInstanceName;
	    _optionName = optionName;
	    _optionValue = optionValue;
		_changedDateTime = Instant.now();
	}


	public String getEmailID() {return _emailID;	}
	public void setEmailID(String emailID) {_emailID = emailID;	}

	public String getDomainInstanceName() {		return _domainInstanceName; 	}
	public void setDomainInstanceName(String domainInstanceName) { _domainInstanceName = domainInstanceName; }

	public UserOptionName getOptionName() {	return _optionName; 	}
	public void setOptionName(UserOptionName optionName) { _optionName = optionName; 	}

	public String getOptionValue() { return _optionValue; 	}
	public void setOptionValue(String optionValue) { _optionValue = optionValue;	}

	public Instant getChangedDateTime() { return _changedDateTime;	}
	public void setchangedDateTime(Instant changedDateTime) { _changedDateTime = changedDateTime;	}	
	
	public boolean save() {
		return (new UserOptionDAO()).save(this);
	}


	public static UserOption retrieve(String emailID, String domainInstanceName, UserOptionName option) {
		return (new UserOptionDAO()).retrieve(emailID, domainInstanceName, option);
	}


	public static boolean destroy(String emailID, String domainInstanceName, UserOptionName option) {
		return (new UserOptionDAO()).delete(emailID, domainInstanceName, option);
		
	}
}
