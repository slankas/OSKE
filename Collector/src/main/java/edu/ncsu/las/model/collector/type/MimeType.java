package edu.ncsu.las.model.collector.type;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.Tika;

import edu.ncsu.las.document.DocumentCreatorInterface;
import edu.ncsu.las.util.FileUtilities;
import edu.uci.ics.crawler4j.crawler.Page;

/**
 * Represents a content-type / mime-type for a file.
 * 
 * 
 * Implementation note: Choose not to use an enum for the initial release as we would need to have
 * a substantial number of the content-types handled. New types can be handled regularly.
 * Could work around this by providing a default/unknown handler...
 *
 */
public class MimeType {
	private static Logger logger =Logger.getLogger(MimeType.class.getName());

	
	public static final String TABLE = "collector/table";
	public static final String URL = "collector/url";
	public static final String TEXT_HTML  = "text/html";
	public static final String TEXT_PLAIN = "text/plain";
	public static final String TEXT_XML   = "text/xml";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String APPLICATION_XGZIP = "application/x-gzip";
	public static final String APPLICATION_GZIP = "application/gzip";
	public static final String APPLICATION_TAR  = "application/x-tar";
	public static final String APPLICATION_GTAR  = "application/x-gtar";
	public static final String APPLICATION_RSS_XML  = "application/rss+xml";
	public static final String APPLICATION_ATOM_XML  = "application/atom+xml";
	public static final String APPLICATION_XML = "application/xml";
	public static final String APPLICATION_PDF = "application/pdf";
	public static final String APPLICATION_EPUB = "application/epub+zip";
	public static final String APPLICATION_HTML = "application/xhtml+xml";

	public static final String IMAGE_GIF  = "image/gif";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_PNG  = "image/png";
	public static final String IMAGE_TIFF = "image/tiff";
	
	public static final String JAVAX_MAILMESSAGE = "javax/mailmessage";	
		
	public static final String FORUM_THREAD  = "forum/thread";	
	public static final String FORUM_POST    = "forum/post";	
	public static final String FORUM_USER    = "forum/user";	

	
	public static final String APPLICATION_OCTECT_STREAM = "application/octet-stream";
	public static final String UNKOWN = "unknownn";
	
	/**
	 * Checks whether or not the given MIME_TYPE is a compressed file format.
	 * 
	 * @param type mimeType to be checked
	 * @return boolean. true if compressed
	 */
	public static boolean isCompressedType(String s) {
		return (s.equalsIgnoreCase(APPLICATION_ZIP)  || 
				s.equalsIgnoreCase(APPLICATION_GZIP) ||
				s.equalsIgnoreCase(APPLICATION_TAR) || 
				s.equalsIgnoreCase(APPLICATION_GTAR));
	}
	
	/**
	 * checks whether or not we were able to determine if the mime was ambiguous (i.e., file format unknown)
	 * 
	 * @param mimeType
	 * @return
	 */
	public static boolean isAmbiguousMimeType(String mimeType) { 
		if (mimeType.equals(APPLICATION_OCTECT_STREAM) || mimeType.equals(UNKOWN)) {
			return true;
		}
		
		return false;
	}
	
	
	
	/** 
	 * Attempts to determine the content-type of a crawled web page
	 * 
	 * Precedence order: HTTPHeader, Data, weburl path
	 * @param p
	 * @return
	 */
	public static String getMimeType(Page p) {
		String contentTypeHeader = p.getContentType();
		Tika tika = new Tika();
		
		if (contentTypeHeader != null) {
			if (contentTypeHeader.indexOf(';') >0) {
				contentTypeHeader = contentTypeHeader.substring(0, contentTypeHeader.indexOf(';'));
			}
			if (!MimeType.isAmbiguousMimeType(contentTypeHeader)) {
				return MimeType.checkMimeTypeBeforeReturn(contentTypeHeader,p.getContentData(),p.getWebURL().getURL());
			}
		}
		
		String mimeType = tika.detect(p.getContentData());
		if (! MimeType.isAmbiguousMimeType(mimeType)) {
			return MimeType.checkMimeTypeBeforeReturn(mimeType,p.getContentData(),p.getWebURL().getURL());
		}
		
		return MimeType.checkMimeTypeBeforeReturn(tika.detect(p.getWebURL().getPath()), p.getContentData(),p.getWebURL().getURL()); // return content type by file name	
	}

	/** 
	 * Attempts to determine the content-type with a given mimetype.
	 * 
	 * Precedence order: HTTPHeader, Data
	 * 
	 * @param contentData
	 * @param headerMimeType
	 * @return
	 */
	public static String getMimeType(byte[] contentData, String headerMimeType, String fileOrURL) {
		String contentTypeHeader = headerMimeType;
		Tika tika = new Tika();
		
		if (contentTypeHeader != null) {
			if (contentTypeHeader.indexOf(';') >0) {
				contentTypeHeader = contentTypeHeader.substring(0, contentTypeHeader.indexOf(';'));
			}
			if (!MimeType.isAmbiguousMimeType(contentTypeHeader)) {
				return MimeType.checkMimeTypeBeforeReturn(contentTypeHeader,contentData,fileOrURL);
			}
		}
		
		String mimeType = tika.detect(contentData);
		return MimeType.checkMimeTypeBeforeReturn(mimeType,contentData,fileOrURL);	
	}	
	
	
	/** 
	 * Attempts to determine the content-type of a file.  Utilizes Tika.detect(File) to get an answer
	 * 
	 * 
	 * @param f file to check
	 * @return
	 */
	public static String getMimeType(java.io.File f) {
		Tika tika = new Tika();
		
		try {
			String mimeType = tika.detect(f);  
			byte[] data = FileUtilities.readAllBytesFromFile(f);
			return MimeType.checkMimeTypeBeforeReturn(mimeType,data,f.getName());
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to determine mime type: "+f);
			logger.log(Level.SEVERE, "   exception: "+e);
			return MimeType.UNKOWN;
		}
	}
	
	/**
	 * First attempts to the detect the mime-type from the actual data.
	 * If that is ambiguous, then the filename is used.
	 * 
	 * @param filename
	 * @param data
	 * @return
	 */
	public static String getMimeType(String filename, byte[] data) {
		Tika tika = new Tika();
		String mimeType = tika.detect(data,filename);
		if (! MimeType.isAmbiguousMimeType(mimeType)) {
			return MimeType.checkMimeTypeBeforeReturn(mimeType, data, filename);
		}
		
		return MimeType.checkMimeTypeBeforeReturn(tika.detect(filename),data,filename);
	}	
	
	/**
	 * Many sites don't send the proper content-type for RSS feeds.  This methods
	 * checks to see if a text/xml (or application/xml) file contains "<rss" or "<feed".  If so, the 
	 * content type is changed to rss or atom as appropriate.
	 * 
	 * All mimeType detection methods in this class, call this function prior to return
	 * their value.
	 * 
	 * This is also used to see if just "application/vnd.openxmlformats" has been set as the mimeType, if so tika.detect is used.
	 * 
	 * @param mimeType  mimeType to check for any local overrrides
	 * @param data data in the file
	 * @return original mimeType unless an override was necessary.
	 */
	private static String checkMimeTypeBeforeReturn(String mimeType, byte[] data, String fileName) {
		if (mimeType.equalsIgnoreCase(TEXT_XML) || mimeType.equalsIgnoreCase(APPLICATION_XML)) {
			String content = new String(data,StandardCharsets.UTF_8);
			if (content.contains("<RSS") || content.contains("<rss") | content.contains("<Rss")) {
				mimeType = APPLICATION_RSS_XML;
			}
			else if (content.contains("<feed") || content.contains("<FEED") || content.contains("<Feed")) {
				mimeType = APPLICATION_ATOM_XML;
			}
		}
		else if (mimeType.equals("application/vnd.openxmlformats")) {
				String tempType = (new Tika()).detect(data,fileName);
				if (!MimeType.isAmbiguousMimeType(tempType)) {
					mimeType = tempType;
				}
		}
		return mimeType;
		
	}
	
	/**
	 * Does the suffix is the given name (which can be a file name or URL)
	 * indicate that the file is a video?
	 * 
	 * @param name
	 * @return true is the suffix can be detected, and has a mime type containing video.  
	 *         false otherwise.
	 */
	public static boolean doesSuffixIndicateVideo(String name) {
		String suffix = "";
		if (name.startsWith("http") ) {
			suffix = MimeTypeMap.getFileExtensionFromUrl(name);
		}
		else {
			int lastIndex = name.lastIndexOf(".");
			if (lastIndex < 0) { return false; }
			
			suffix = name.substring(lastIndex +1);
		}
		
		String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
		if (contentType == null) { return false;}
		
		return contentType.contains("video");
	}
	
	//TODO: Comment this!!
	
	
	public static final HashMap<String, edu.ncsu.las.document.DocumentCreatorInterface> _extendedMimeTypes = new HashMap<String, edu.ncsu.las.document.DocumentCreatorInterface>();
	public static void registerExtendedType(String mimeType,  edu.ncsu.las.document.DocumentCreatorInterface creator) {
		_extendedMimeTypes.put(mimeType,creator);
	}
	public static boolean isExtendedType(String mimeType) { return _extendedMimeTypes.containsKey(mimeType); }
	public static DocumentCreatorInterface getExtendedTypeDocumentCreator(String mimeType) { return _extendedMimeTypes.get(mimeType); }
	
}
