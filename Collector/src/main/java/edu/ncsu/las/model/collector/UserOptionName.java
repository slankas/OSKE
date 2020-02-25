package edu.ncsu.las.model.collector;

import java.util.HashMap;

/**
 * Defines the the options that are available to users to define.
 * 
 *
 */
public enum UserOptionName {
	BreakingNewsURLs("breakingNewsURLs","List of urls that a user can self-select to show in the news feed section of the domain home page");
	
	private String _label;
	private String _description;
	
	private UserOptionName(String label, String description) {
		_label = label;
		_description = description;
	}
	
	public String toString() {return _label;}
	public String getDescription() { return _description; }

	// access the OptionNames types by their full label.  This is lazily created when the first call is made
	private static HashMap<String, UserOptionName> _optionNameByFullName = null;
	
	public static UserOptionName getOptionName(String label) {
		if (_optionNameByFullName == null) {
			synchronized (UserOptionName.class) {
				_optionNameByFullName = new HashMap<String, UserOptionName>();
				for (UserOptionName on: UserOptionName.values()) {
					_optionNameByFullName.put(on.toString(), on);
				}
			}
		}
		return _optionNameByFullName.get(label);
	}
}