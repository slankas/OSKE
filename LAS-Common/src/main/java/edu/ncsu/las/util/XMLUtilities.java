package edu.ncsu.las.util;

import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common set of XML utilities
 * 
 */
public class XMLUtilities {
	private static Logger logger =Logger.getLogger(XMLUtilities.class.getName());
	
	/**
	 * Converts the passed in node to a string
	 * @param node
	 * @return
	 */
	public static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();
	}
	
	public static org.w3c.dom.Document convertXMLStringToDocument(String xml)  {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = dBuilder.parse( new ByteArrayInputStream(xml.getBytes("UTF-8")));
	
			doc.getDocumentElement().normalize();		
			
			return doc;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to convert xml to text - ", e);
			return null;
		}
	}

	
	/**
	 * For a string that represents an XML document (or a complete fragment), get it's text
	 * 
	 * @param xml
	 * @return String
	 */
	public static String convertXMLStringToText(String xml)  {
		org.w3c.dom.Document doc = convertXMLStringToDocument(xml);
		if (doc != null) {
			return doc.getDocumentElement().getTextContent();
		}
		else {
			return null;
		}
	}
	
}
