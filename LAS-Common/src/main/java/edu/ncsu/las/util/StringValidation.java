package edu.ncsu.las.util;


import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Contains a series of static methods to validate and cleanse strings.
 * HTML sanitization library: https://github.com/OWASP/java-html-sanitizer
 * 
 * We did create a custom  policy such that we can maintain certain meta-elements in the HTML editor.
 * 
 * 
 */
public class StringValidation {

	//copied from sanitizers for use to use.
	  private static final AttributePolicy INTEGER = new AttributePolicy() {
		    public String apply(
		        String elementName, String attributeName, String value) {
		      int n = value.length();
		      if (n == 0) { return null; }
		      for (int i = 0; i < n; ++i) {
		        char ch = value.charAt(i);
		        if (ch == '.') {
		          if (i == 0) { return null; }
		          return value.substring(0, i);  // truncate to integer.
		        } else if (!('0' <= ch && ch <= '9')) {
		          return null;
		        }
		      }
		      return value;
		    }
		  };
	
	private static PolicyFactory POLICY_LINKS_ONLY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
	
	// data allows us to upload images into an html wyswyg editor
	private static PolicyFactory POLICY_STANDARD_HTML = Sanitizers.FORMATTING.and(Sanitizers.LINKS).and(Sanitizers.STYLES).and(Sanitizers.TABLES).and(
			new HtmlPolicyBuilder().allowElements("div", "span").allowAttributes("dataSource","dataTag").onElements("div","span").toFactory()).and(
			new HtmlPolicyBuilder().allowUrlProtocols("http", "https","data").allowElements("img")
				      .allowAttributes("alt", "src","style","data-filename").onElements("img")
				      .allowAttributes("border", "height", "width").matching(INTEGER)
				          .onElements("img")
				      .toFactory()).and(
					Sanitizers.BLOCKS);
	
	private static PolicyFactory POLICY_NONE = (new HtmlPolicyBuilder()).toFactory();

	private static String[] schemes = {"http","https"};
	private static UrlValidator urlValidator = new UrlValidator(schemes);
	
	public static boolean containsHTML(String untrustedText) {
		return !untrustedText.equals(removeAllHTML(untrustedText));
	}
	
	public static String removeAllHTML(String untrustedText) {
		String safeText = POLICY_NONE.sanitize(untrustedText);
		return safeText;
	}	
	
	public static String removeNonLinksFromString(String untrustedText) {
		String safeText = POLICY_LINKS_ONLY.sanitize(untrustedText);
		return safeText;
	}

	public static String removeNonStandardHTMLFromString(String untrustedText) {
		String safeText = POLICY_STANDARD_HTML.sanitize(untrustedText);
		return safeText;
	}

	public static String removeNonASCII(String text, String replaceWith) {
		return text.replaceAll("[^\\x00-\\x7F]", replaceWith);
	}
	
	public static boolean isValidURL(String url) {
		return urlValidator.isValid(url);
	}
	
	private static String regexEmailValidator= "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
	private static Pattern regexEmailPattern = Pattern.compile(regexEmailValidator);
	
	public static boolean isValidEmailAddress(String address) {
		return regexEmailPattern.matcher(address).matches();
	}
}
