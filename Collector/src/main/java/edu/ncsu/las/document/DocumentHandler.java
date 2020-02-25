package edu.ncsu.las.document;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.LASTable;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.translate.OpenKEAmazonTranslate;

/**
 * The collector system can route documents based upon the document's mime
 * content type and, possibly, by the domain of the document originated from.
 *
 * Apache Tika determines the mime type through a combination of looking at file
 * names and the actual content. The domain is used to allow for specific
 * handlers customized to a site. For instance, on Amazon.com, there are several
 * parts of a product page we may wish process differently. (product information
 * at the top of the page, Q&As, reviews, product specifications, product
 * details) By default we may implicitly trust everything but q&a's and reviews.
 * For reviews, we may decide to trust based upon the user ranking.
 * 
 *
 */
public abstract class DocumentHandler {
	static Logger logger = Logger.getLogger(edu.ncsu.las.collector.Collector.class.getName());
	
	public Document _currentDocument;
	private DocumentRouter _router;

	private StringBuilder _text = new StringBuilder();
	
	private String _extractedText = null;
	
	private ArrayList<Document> _documentsToRoute = new ArrayList<Document>();

	/**
	 * Gets the document router for the handler. Used if further processing is
	 * need based upon content extraction of the current handler. This method is
	 * called whenever a new instance of DocumentHandlerInterface is created.
	 * (can't create/specific constructors in interfaces)
	 * 
	 * @param _router
	 *            link to the current document router.
	 */
	public DocumentRouter getRouter() {
		return _router;
	}


	public String getCurrentURL() {
		return this.getCurrentDocument().getURL();
	}
    
	public Document getCurrentDocument() {
		return _currentDocument;
	}


	/**
	 * TODO: add description
	 * 
	 * @param document
	 * @param router
	 */
	public final void  process(Document document, DocumentRouter router) {
		this._currentDocument = document;
		this._router = router;
		
		logger.log(Level.FINER, this.getClass().getName() + " processing for : " + this.getCurrentURL());

		this.processDocument();	
		this.processTextAfterExtractions();
		this.routeResults();

		logger.log(Level.FINER, this.getClass().getName() + " complete: " + this.getCurrentURL());	
	}
	
	public final void processTextAfterExtractions() {
		String text = this.getExtractedText();
		String sourceLang = this.getCurrentDocument().getTranslateTargetLanguage();
		logger.log(Level.FINE, "LANGUAGE: " + this.getCurrentDocument().getLanguage());
		String destLang = "en";
		
		//check to see if we need to translate
		if (sourceLang != null  && sourceLang.length() == 2) {  // may want to 
			logger.log(Level.FINE, "source Lang: "+sourceLang);  //TODO CHANGE TO FINEST when translating work complete  //TODO: change variable name??
			logger.log(Level.FINE, "target Lang: "+destLang);        //TODO CHANGE TO FINEST when translating work complete
			
			this.getCurrentDocument().addAnnotation("nativeText", text);

			
			OpenKEAmazonTranslate translator = OpenKEAmazonTranslate.getTheAmazonTranslator();
			text = translator.getTranslation(sourceLang, destLang, text);
			logger.log(Level.FINE, "Translated text: "+text);    //TODO Change to FINEST when translating work complete. 
		}
		
		this.getCurrentDocument().setExtractedText(text);  // must do this so that the full text search can get to do it.  (Does no good in the new document as we don't save that one)

	}

	
	private void routeResults() {	
		if (_text.length() > 0) {
			Document documentToRoute = new Document(this.getCurrentDocument(),this.getExtractedText().getBytes(StandardCharsets.UTF_8),MimeType.TEXT_PLAIN,-1);
			this.getRouter().processPage(documentToRoute, false,false);
		}

		for (Document d: _documentsToRoute) {
			this.getRouter().processPage(d, false,false);
		}

		
	}

	/**
	 * Handlers should call this method when their document has another content object that
	 * should be processed separately.  The addTable() and addURL() methods are specific 
	 * convenience methods that can be used in place of this method.
	 * 
	 * @param d
	 */
	public void addDocumentToRoute(Document d) {
		_documentsToRoute.add(d);
	}

	public void addTable(LASTable t) {
		Document documentToRoute = new Document(this.getCurrentDocument(),t.toByteArray(), MimeType.TABLE,-1);
		_documentsToRoute.add(documentToRoute);
	}
	public void addURL(String url) {
		if (url == null || url.contains(":") == false || url.contains("//") == false) {
			logger.log(Level.WARNING, "invalid URL: " + url);
			return;
		}
		Document documentToRoute = new Document(this.getCurrentDocument(),url.getBytes(StandardCharsets.UTF_8), MimeType.URL,-1);
		_documentsToRoute.add(documentToRoute);
		this.getCurrentDocument().addOutgoingURL(url);
	}
	
	public void addURL(List<String> urls) {
		for (String url: urls) {
			this.addURL(url);
		}
	}
	
	public void forwardFile(String fileName, byte data[], long lastModificationTimeEpochMillis) {
		Document documentToRoute = new Document(this.getCurrentDocument(), data, MimeType.getMimeType(fileName, data), lastModificationTimeEpochMillis);
		documentToRoute.setURL(documentToRoute.getURL()+"/"+fileName);
		if (this.getCurrentDocument().hasAnnotation("html_title")) {
			String htmlTitle = this.getCurrentDocument().getAnnotation("html_title") + "/"+fileName;
			documentToRoute.addAnnotation("html_title", htmlTitle);
		}
		
		this.getRouter().processPage(documentToRoute, true,false);
	}
	
	/**
	 * Adds any text to the buffer.  Automatically adds a new line
	 * @param newText
	 */
	public void appendText(String newText){
		_text.append(newText);
		_text.append("\n");
	}
	
	/** 
	 * Used by a handler to explicitly set a value to be used as the extracted text. 
	 * (otherwise the "appendedText" values are used)
	 * 
	 * @param text
	 */
	public void setExtractedText(String text) {
		_extractedText = text;
	}
	
	/**
	 * Gets the current text that's been processed.
	 * If the extractedText property has been explicitly set by the handler, then this value
	 * is returned.  Otherwise the appended text is returned.
	 * 
	 * The primary use of this method will be for creating the text that is sent on to the Full-Text search box.
	 * 
	 * @return text
	 */
	public String getExtractedText() {
		if (_extractedText != null) {
			return _extractedText;
		}
		else {
			return _text.toString();
		}
	}
	
	/**
	 * When a DocumentHandler receives this message, the handler will either
	 * process the data into knowledge base and/or split the data into different
	 * parts (such as text, tables, links, other files) to be sent back into the
	 * DocumentRouter for additional processing.
	 * 
     */
	protected abstract void processDocument();

	/**
	 * What is the mime content type that this handler responds to? Generally
	 * speaking, this is a fixed value. mime types should follow commmon
	 * practices used on the internet. See
	 * http://hul.harvard.edu/ois/systems/wax/wax-public-help/mimetypes.htm as
	 * an example.
	 * 
	 * @return
	 */
	public abstract String[] getMimeType();

	/**
	 * If a processing shuold be limited to documents of a particular domain,
	 * specify the domain through this property. Return blank if all domains
	 * should be processed by this handler.
	 * 
	 * @return domainName (or an empty string) used by this handler.
	 */
	public abstract String getDocumentDomain();

	/**
	 * Overview of the processing this handler performs. This may be used in
	 * human interfaces for users to see what capabilities currently exist.
	 * 
	 * @return
	 */
	public abstract String getDescription();

	/**
	 * Processing order in which handlers are checked for which one should be
	 * called. Each mime content type has its own processing order defined. (ie,
	 * multiple components can have the same priority provided their content
	 * type is different.).
	 * 
	 * Handlers are checked based upon a low to high scheme. (ie, so
	 * Integer.MIN_VALUE would always be checked first). Any domain specific
	 * handlers must have a lower processing order such that are processed
	 * first.
	 * 
	 * @return
	 */
	public abstract int getProcessingOrder();
	
	public JSONObject toJSON() {
		JSONArray mimeTypes = new JSONArray();
		for (String mt: this.getMimeType()) {
			mimeTypes.put(mt);
		}
		
		JSONObject result = new JSONObject()
			.put("description", this.getDescription())
			.put("mimeType", mimeTypes)
			.put("processingOrder", this.getProcessingOrder())
			.put("documentDomain", this.getDocumentDomain())
			.put("name", this.getClass().getSimpleName());
		return result;
	}
	
	
	public static java.util.List<DocumentHandler> getDocumentHandlers() {
		Reflections reflections = new Reflections("edu.ncsu.las");    
		Set<Class<? extends DocumentHandler>> classes = reflections.getSubTypesOf(DocumentHandler.class);
	
		java.util.List<Class<? extends DocumentHandler>> tempResult = new ArrayList<Class<? extends DocumentHandler>>(classes);
		java.util.ArrayList<DocumentHandler> result = new java.util.ArrayList<DocumentHandler>();
		for (Class<? extends DocumentHandler> c: tempResult) {
			try {
				result.add(c.newInstance());
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unable to create instance of class: "+ c.getName());
				logger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
			}
		}
		
		Collections.sort(result, new Comparator<DocumentHandler>() {
			@Override
			public int compare(DocumentHandler o1, DocumentHandler o2) {
				return Integer.compare(o1.getProcessingOrder(),o2.getProcessingOrder());
			}
	    });

		return result;
	}
	
	/**
	 * 
	 * @param handlers
	 * @param mimeType
	 * @param domain
	 * @return
	 */
	public static DocumentHandler getHandler(String contentMimeType, String contentDomain)  {
		for (DocumentHandler dhd: Collector.getTheCollecter().getDocumentHandlers()) {   // use this since it is cached from the above method
			for (String handlerMimeType: dhd.getMimeType()) {
				if ( (handlerMimeType.equals("") || handlerMimeType.equalsIgnoreCase(contentMimeType)) &&
					 (dhd.getDocumentDomain().equals("") || dhd.getDocumentDomain().equals(contentDomain)) ) {
					try {
						DocumentHandler result = dhd.getClass().newInstance();
						
						if (result instanceof DefaultHandler) {
							System.out.println("NOT CONFIGURED: "+contentMimeType);
						}
						
						return result;
					}
					catch (Exception e) {
						logger.log(Level.SEVERE, "Unable to create instance of class: "+ dhd.getClass().getName());
						logger.log(Level.SEVERE, "Unable to create instance of class - exception: "+ e);
					}
				}
			}
		}
		logger.log(Level.SEVERE, "Unable to getHandler for content (mimeType: "+contentMimeType+", domain: "+contentDomain+" )");
		return null;
		
	}	
	
}
