package edu.ncsu.las.util;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * Set of components to analyze and extract HTML Components.
 * JSoup library heavily utilized.
 * 
 */
public class HTMLUtilities {
	private static Logger logger =Logger.getLogger(HTMLUtilities.class.getName());
	
	
	/**
	 * Removes the start and end of a CDATA tag.
	 * @param text
	 * @return
	 */
	public static String removeCDATATags(String text) {
		if (text.startsWith("//<![CDATA[")) {
			text = text.substring(11);
			if (text.endsWith("//]]>")) {
				text = text.substring(0, text.length()-5);
			}
		}
		return text;
	}	
	
	public static String extractText(Element e) {
		StringBuilder sb = new StringBuilder();
		List<Node> childNodes = e.childNodes();
		
		for (Node n: childNodes) {
			if ( n instanceof org.jsoup.nodes.TextNode) {
				sb.append( ((org.jsoup.nodes.TextNode) n).getWholeText());
			}
			else if (n instanceof org.jsoup.nodes.Element) {
				//System.out.println(n.nodeName() +": "+ n.getClass().getName());
				if (n.nodeName().equals("ul") || n.nodeName().equals("li")) {
					sb.append("\n");
				}
				sb.append(HTMLUtilities.extractText( (org.jsoup.nodes.Element) n));
			}
			else if (n instanceof org.jsoup.nodes.DataNode) {
				// A data node, for contents of style, script tags etc, where contents should not show in text().
				;  // Don't do anything with these because they do not have text
			}
			else {
				logger.log(Level.WARNING, n.nodeName() +": "+ n.getClass().getName()+ " not using");
			}
		}
		
		return sb.toString();
	}
	
	public static String extractText(Document doc, String selector) {
		StringBuilder sb = new StringBuilder();
		Elements elements = doc.select(selector);
		
		for (Element e: elements) {
			if (sb.length()>0) {sb.append("\n");}
			sb.append(HTMLUtilities.extractText(e)); 
		}
		
		
		return StringUtilities.cleanText(sb.toString());
	}
	
	/**
	 * 
	 * @param text
	 * @param baseURL
	 * @return
	 * @throws MalformedURLException if the based-in baseURL is incorrect
	 */
	public static List<String> extractWindowOpenLinks(String text, String baseURL) throws MalformedURLException  {
		java.util.ArrayList<String> result= new java.util.ArrayList<String>();
		
		URL base = new java.net.URL(baseURL); 
		
		int lastIndex = -1;
		int currentIndex = 0;
		while ( (currentIndex = text.indexOf("window.open",lastIndex)) > 0) {
			currentIndex += 11;  //moves past window.open.
			
			String link = getWindowOpenLink(text, currentIndex);
			if (link != null && link.length() >0) {
				try {
					URL newURL = new URL(base,link);
					result.add(newURL.toString());
				}
				catch (MalformedURLException e) {
					logger.log(Level.WARNING, "unable create url:  baseURL: "+baseURL+", link: "+link+", exception: "+e);
				}			
			}		
			lastIndex = currentIndex;
		}
		
		return result;	
	}
	
	private static String getWindowOpenLink(String text, int startPos) {
		
		int firstQuoteIndex  = text.indexOf("'", startPos);
		int firstDoubleQuoteIndex = text.indexOf("\"", startPos);
		
		int openParenLocation = text.indexOf("(", startPos);
		int closeParenLocation = text.indexOf(")", startPos);
		int commaLocation = text.indexOf(",", startPos);
		
		if (firstQuoteIndex < 0 && firstDoubleQuoteIndex <0) {
			return "";
		}
		if (firstQuoteIndex < 0) { firstQuoteIndex = Integer.MAX_VALUE; }
		if (firstDoubleQuoteIndex < 0) { firstDoubleQuoteIndex = Integer.MAX_VALUE; }
		
		String quoteCharacter = "'";
		int    quotePosition  = firstQuoteIndex;
		if (firstDoubleQuoteIndex < firstQuoteIndex) {
			quoteCharacter = "\"";
			quotePosition  = firstDoubleQuoteIndex;
		}
		
		if (quotePosition > closeParenLocation || quotePosition < openParenLocation || quotePosition > commaLocation) {
			return "";
		}
		
		int endIndex = text.indexOf(quoteCharacter,quotePosition+1);
		if (endIndex <0) {
			return "";
		}
		
		return text.substring(quotePosition+1,endIndex);
	}

	/**
	 * returns a set containing all of linked resources on the given page.  It checks 4 items:
	 * - javascript:window.open()
	 * - a.href
	 * - img sources
     * - css and javascript includes
     * 
	 * @param text HTML text of the document.
	 * @param baseURL Base URL that the document came from
	 * @param excludeKeywords  If certain terms should indicate that the URL not be included, they should be in this array.  This array needs to be lowercased
	 * @return set of the URLs contained in the document
	 */
	public static Set<String> getAllLinks(String text, String baseURL, String[] excludeKeywords) {
		HashSet<String> result = new HashSet<String>();
		
		try {
			result.addAll(HTMLUtilities.extractWindowOpenLinks(text,baseURL));
		} catch (MalformedURLException e) {
			logger.log(Level.WARNING, "Malformed URL: "+e+", ignoring");
		}
		
		Document doc = Jsoup.parse(text, baseURL);
		result.addAll(HTMLUtilities.getHREFLinks(doc, excludeKeywords));
		result.addAll(HTMLUtilities.getSourceLinks(doc));
		result.addAll(HTMLUtilities.getImportLinks(doc));
		
		return result;		
	}

    public static Set<String> getHREFLinks(Document doc, String[] excludeKeywords) {
    	HashSet<String> result = new HashSet<String>();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
        	boolean include = true;
        	
        	String linkText = link.text().toLowerCase();
        	String url      = link.attr("abs:href").toLowerCase();
        	for (String keyword: excludeKeywords) {
        		if (linkText.contains(keyword) || url.contains(keyword)) {
        			include= false;
        			break;
        		}
        	}
        	
        	if (include) {
        		result.add(link.attr("abs:href"));
        	}
        }
		return result;
    }
    
    public static Set<String> getSourceLinks(Document doc) {
    	HashSet<String> result = new HashSet<String>();
    	
        Elements media = doc.select("[src]");
        for (Element src : media) {
           	result.add( src.attr("abs:src"));
        }
		return result;
    }

    public static Set<String> getImportLinks(Document doc) {
    	HashSet<String> result = new HashSet<String>();
        Elements imports = doc.select("link[href]");

        for (Element src : imports) {
           	result.add( src.attr("abs:href"));
        }
		return result;
    }

    /**
     * Extracts all images links from a series of elements  
     * e.g., images will be created with something like doc.select("img");
     * 
     * @param images
     * @return array of JSONObjects.  Each object has "href"  as its member
     */
    public static JSONArray extractImageURLs(Elements images) {
		JSONArray imageArray = new JSONArray();
			
		for (org.jsoup.nodes.Element a: images) {
			String imageURL = a.attr("abs:src");
			JSONObject jo = new JSONObject().put("href",imageURL);
			imageArray.put(jo);
		}
		return imageArray;
	}
	
    /**
     * Extracts all hyperlinks and their associated text from "a" tags
     * hrefs should be create with a selector like doc.select("a");
     * 
     * @param hrefs
     * @return array of JSONObjects.  Each object has "href" and "linkText" as its members.
     */
	public static JSONArray extractHyperlinks(Elements hrefs) {
		JSONArray hrefArray = new JSONArray();
			
		for (org.jsoup.nodes.Element a: hrefs) {
			String linkURL = a.attr("abs:href");
			JSONObject jo = new JSONObject().put("href",linkURL)
					                        .put("linkText",a.text());
			hrefArray.put(jo);
		}
		
		return hrefArray;
	}
}
