package edu.ncsu.las.time.model;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;

/**
 * 
 * Wrapper to use HeidelTime and return results in a JSON-based format
 * 
 */
public class HeidelTime {
	private static Logger logger =Logger.getLogger(HeidelTime.class.getName());
	
	private HeidelTimeStandalone heidelTime;
	private static final HeidelTimePool HTP = new HeidelTimePool(new HeidelTimePoolFactory());
	
	private static String CONFIG_PATH ;
	
	public static void initialize(String configurationPath) {
		CONFIG_PATH = configurationPath;
	}
	
	public static HeidelTimePool getTheHeidelTimePool() {
		return HTP;
	}
	
	HeidelTime() {
		heidelTime = new HeidelTimeStandalone(Language.ENGLISH,DocumentType.COLLOQUIAL,
                OutputType.TIMEML,
                CONFIG_PATH, 
                POSTagger.STANFORDPOSTAGGER, true);		
	}
	
	public JSONArray processDocument(String text, Instant date) throws Exception {
		JSONArray results = new JSONArray();
		
		// HeidelTime appears to process indefinitely for some results.
		final ExecutorService service = Executors.newSingleThreadExecutor();
		
		try {
			final Future<Object> f = service.submit(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					JSONArray tempResults = processDocumentInternal(text, date); 
					for (int i=0;i < tempResults.length(); i++) {
						results.put(tempResults.get(i));
					}
					return null;
				}
			});

			f.get(120, TimeUnit.SECONDS);
			f.cancel(true);
				
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Unable to process text for time annotations - took too long: "+ex);
		}
		service.shutdownNow();
		
		return results;
	}
	
	JSONArray processDocumentInternal(String text,Instant date) throws Exception {
		
		String taggedText = heidelTime.process(text, Date.from(date));

		JSONArray timexObjectArray = new JSONArray();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		DocumentBuilder dBuilder = dbf.newDocumentBuilder();
		org.w3c.dom.Document doc = null;
		try {
			doc = dBuilder.parse(new ByteArrayInputStream(taggedText.getBytes(StandardCharsets.UTF_8)));
		}
		catch (org.xml.sax.SAXParseException spe) {
			logger.log(Level.WARNING, "Unable to parse document from HeidelTime - most likely spurious & in text, attempting to correct and re-process");
			try {
				taggedText = taggedText.replaceAll("&", "&amp;");
				doc = dBuilder.parse(new ByteArrayInputStream(taggedText.getBytes(StandardCharsets.UTF_8)));
			}
			catch (org.xml.sax.SAXParseException sp) {
				logger.log(Level.WARNING, "Unable to parse document from HeidelTime",spe);
				logger.log(Level.WARNING,taggedText);
				return new JSONArray();
			}
			
		}
		
		
		NodeList nList = doc.getElementsByTagName("TIMEX3");

		int originalTextPos = 0;		
		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;
				String nodeText = nNode.getTextContent();
				int pos = text.indexOf(nodeText, originalTextPos);
		        originalTextPos = pos + nodeText.length();
		        
		        JSONObject timexObjectDetails = new JSONObject().put("text", nodeText).put("position", pos);
		        
				NamedNodeMap nnm = eElement.getAttributes();
				for (int j=0; j<nnm.getLength();j++) {
					String attributeName = nnm.item(j).getNodeName();
					String value = eElement.getAttribute(attributeName);
					timexObjectDetails.put(attributeName, value);
				}
				timexObjectArray.put(timexObjectDetails);
			}
		}

		return timexObjectArray;
	}
}
