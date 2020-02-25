package edu.ncsu.las.document;


import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.util.HTMLUtilities;


/**
 * The HTMLHandler
 * 
 * 
 */
public class HTMLHandler extends DocumentHandler {
	static final Logger logger =Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	private static final String[] MIME_TYPE = {MimeType.TEXT_HTML, MimeType.APPLICATION_HTML };
	
	
	private String getTextExtractionMethod(JSONObject config) {
		String textExtractionMethod = config.has("textExtractionMethod") ? config.getString("textExtractionMethod") : "boilerpipe";
		if (  !(textExtractionMethod.equals("tika") || textExtractionMethod.equals("jsoup") || textExtractionMethod.equals("boilerpipe")) ) {
			 textExtractionMethod = "boilerpipe";			
		}
		return textExtractionMethod;
	}
	
	protected void processDocument() {			
		logger.log(Level.FINE,"DocumentHandler: "+this.getCurrentDocument().getUUID());

		JSONObject config = this.getCurrentDocument().getRelatedJobConfiguration();
		if (config.has("extractArea") == false) { 
			String textExtractionMethod = this.getTextExtractionMethod(config);
			switch (textExtractionMethod) {
			case "tika": this.appendText(this.getCurrentDocument().getExtractedTextFromTika());
						 try {
						       String bpText = com.kohlschutter.boilerpipe.extractors.ArticleExtractor.INSTANCE.getText(this.getCurrentDocument().getContentDataAsString());
						       this.getCurrentDocument().addAnnotation("textFromBoilerpipe", bpText);
						       this.getCurrentDocument().addAnnotation("textFromTika", this.getCurrentDocument().getExtractedTextFromTika());
			             }
			             catch (Throwable t) {
			          	   logger.log(Level.WARNING, "Unable to extract text from url ("+this.getCurrentURL()+") with boilerpipe: "+t.toString());
			          	   this.appendText(this.getCurrentDocument().getExtractedTextFromTika());
			             }
				         break;
			case "jsoup": String htmlFile = this.getCurrentDocument().getContentDataAsString();
			              String jsoupText = Jsoup.parse(htmlFile).text();
						  this.appendText(jsoupText);
				          break;
			case "boilerpipe": try {
				 			       String bpText = com.kohlschutter.boilerpipe.extractors.ArticleExtractor.INSTANCE.getText(this.getCurrentDocument().getContentDataAsString());
				 			       this.appendText(bpText);
				 			       this.getCurrentDocument().addAnnotation("textFromBoilerpipe", bpText);
				 			       this.getCurrentDocument().addAnnotation("textFromTika", this.getCurrentDocument().getExtractedTextFromTika());
			                   }
			                   catch (Throwable t) {
			                	   logger.log(Level.WARNING, "Unable to extract text from url ("+this.getCurrentURL()+") with boilerpipe: "+t.toString());
			                	   this.appendText(this.getCurrentDocument().getExtractedTextFromTika());
			                   }
			                   break;
			}
			
			//TODO: Extract and send tables onward
			//NOTE: URLS are not extracted as the crawling process handles those.
			
			return;
		}
		
		String htmlText = new String(this.getCurrentDocument().getContentData(), StandardCharsets.UTF_8);		
		processHTMLPage(config, Jsoup.parse(htmlText, this.getCurrentURL()));
		
	}
	
	/**
	 * Goes through the passed HTML page, extracting any content that has been configured for the job.
	 * 
	 * @param config
	 * @param htmlDoc
	 */
	private void processHTMLPage(JSONObject config, Document htmlDoc) {
		
		JSONArray areas = config.getJSONArray("extractArea");
		for (int i=0; i< areas.length();i++) {
			JSONObject pageArea = areas.getJSONObject(i);
			
			String text = HTMLUtilities.extractText(htmlDoc, pageArea.getString("selector"));
			if (text != null && text.length() >0) {
				this.appendText(pageArea.getString("title"));
				this.appendText(text);
			}
			
			//TODO: Extract and send tables onward
		}		
	}	

	/**
	 * 
	 * @return "text/html" always.
	 */
	public String[] getMimeType() { return MIME_TYPE; }

	/**
	 * 
	 * @return an empty string.  This component 
	 */
	public String getDocumentDomain() {
		return "";
	}


	/**
	 * 
	 * @return returns the description of this component
	 */
	public String getDescription() {
		return "This component extracts the content (as a text) and tables. It then forwards those back into the Document Router for further processing.";
	}


	/**
	 * 
	 * @return 100
	 */
	public int getProcessingOrder() {
		return 100;
	}
	


}
