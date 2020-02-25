package edu.ncsu.las.collector.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;


import edu.ncsu.las.util.FileUtilities;
import edu.ncsu.las.util.StringUtilities;

public class TikaUtilities {
	private static Logger logger =Logger.getLogger(TikaUtilities.class.getName());
	private static final Tika tika = new Tika();
	
	
	public static String extractText(File f) throws IOException {
		byte[] data = FileUtilities.readAllBytesFromFile(f);
		return extractText(data);
	}
	
	public static String extractText(byte data[]) {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		String text = null;
	
		try {
			text = tika.parseToString(stream);
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unable parse stream with Tika");
		}
	    finally {	
	    	FileUtilities.closeQuietly(stream);
	    }
		
		if (text == null) {
			try {
				String defaultText = new String(data);
				if (defaultText.startsWith("%PDF")) {
					text = "UNPROCESSED PDF FILE";
				}
				else {
					text = Jsoup.parse(defaultText).body().text();
				}
			}
			catch (Throwable t) {
				logger.log(Level.WARNING, "unable parse stream with JSOUP");
			}			
		}
		
		if (text != null) {
		    try {
		        return StringUtilities.cleanText(text);
		    }
		    catch(Throwable e) {
		    	logger.log(Level.WARNING, "unable clean text");
		    }
		}

	    return null;
	}

	public static java.util.Map<String,String> extractMetaData(byte data[]) {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
				
		java.util.HashMap<String,String> results = new java.util.HashMap<String,String>();
	    try {
	        Parser parser = new AutoDetectParser();
	        BodyContentHandler handler = new BodyContentHandler();
	        Metadata metadata = new Metadata();
	        ParseContext context = new ParseContext();
	        parser.parse(stream, handler, metadata, context);
	        //System.out.println(handler.toString());

	        //getting the list of all meta data elements 
	        String[] metadataNames = metadata.names();

	        for(String name : metadataNames) {
	        	results.put(name,  metadata.get(name));
	        }
	    }
	    catch(Throwable e) {
	    	logger.log(Level.WARNING, "unable parse stream for metaData");
	    }
	    finally {	
	    	FileUtilities.closeQuietly(stream);
	    }
	    return results;
	}
	
	
	public static String detectLanguage(String content) {
		
		// The following code is deprecated, but runs ~ 1 ms vs 300-600ms for the newer methods...
		
		LanguageIdentifier li = new LanguageIdentifier(content);
		return li.getLanguage();
		
		/*
		try {
			org.apache.tika.language.detect.LanguageDetector detector = new  org.apache.tika.language.detect.LanguageDetector(); // org.apache.tika.langdetect.OptimaizeLangDetector().loadModels();
	        LanguageResult result = detector.detect(content);
	        
	        return result.getLanguage();
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, "Unable to load OptimaizeLangDetector", ex);
			return "";
		}
		*/
		
	}
	
	public static  List<String> getURLs(byte data[]) {
		List<String> urls = new ArrayList<>();
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		
		try {
			String text = tika.parseToString(stream);
			String commentstr = text;
			String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
			Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
			Matcher m = p.matcher(commentstr);
			int i = 0;
			while (m.find()) {
				// commentstr = commentstr.replaceAll(m.group(i),"").trim();
				// i++;
				try {
					urls.add(m.group(i++));
				}
				catch(java.lang.IndexOutOfBoundsException ie) {
					logger.log(Level.WARNING, "add URLS, group number out of bounds: "+ie);
				}
			}

		} 
		catch (IOException | TikaException e) {
			logger.log(Level.WARNING, "unable parse stream");
		}
		finally {	
			FileUtilities.closeQuietly(stream);
	    }
		return urls;
	}	
	
	
}
