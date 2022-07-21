package edu.ncsu.las.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;



/**
 * set of internet related utilities to be used by various objects.
 *
 */
public class InternetUtilities {
	private static Logger logger = Logger.getLogger(InternetUtilities.class.getName());

	// adapted from the actual java source code..  Used because conflicts may exist between the day of the week stated, and that derived from the date.
	
	public static final DateTimeFormatter RFC_1123_DATE_TIME_MINUS_INITIAL_DAY_OF_WEEK;
    static {
        java.util.Map<Long, String> moy = new java.util.HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        RFC_1123_DATE_TIME_MINUS_INITIAL_DAY_OF_WEEK = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, moy)
                .appendLiteral(' ')
                .appendValue(java.time.temporal.ChronoField.YEAR, 4)  // 2 digit year not handled
                .appendLiteral(' ')
                .appendValue(java.time.temporal.ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(java.time.temporal.ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(java.time.temporal.ChronoField.SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendOffset("+HHMM", "GMT")  // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
                .toFormatter();
    }
    

	
	/**
	 * Returns just machine name from the URL.
	 * 
	 * @param url
	 * @return
	 */
	public static String getDomain(String url) {
		int domainStartIdx = url.indexOf("//") + 2;
		int domainEndIdx = url.indexOf('/', domainStartIdx);
		domainEndIdx = domainEndIdx > domainStartIdx ? domainEndIdx : url.length();
		String domain = url.substring(domainStartIdx, domainEndIdx);
		return domain;
	}	
	
	/**
	 * Checks whether or not the expiration heade 
	 * @param h
	 * @return
	 */
	public static boolean isDateHTTPHeader(String h) {
		return (h.equalsIgnoreCase("last-modified") || h.equalsIgnoreCase("expires") ||h.equalsIgnoreCase("date"));
	}
	
	/**
	 * Converts an HTTP Based date representation into the standard ISO format.
	 * If the string equals "-1", then "1900-01-01T00:00:00:000Z" is returned. 
	 * (This would typically happens for expiration headers)
	 * 
	 * @param s string to convert
	 * @return string in an ISO format of YYYY-MM-DDTHH:mm:SS.sssZ
	 */
	public static String convertHTTPBasedDateStringToISO(String s) {
		return convertHTTPBasedDateStringToISO(s,true);
	}	
	
	
	/**
	 * Converts an HTTP Based date representation into the standard ISO format.
	 * If the string equals "-1", then "1900-01-01T00:00:00:000Z" is returned. 
	 * (This would typically happens for expiration headers)
	 * 
	 * @param s string to convert
	 * @return string in an ISO format of YYYY-MM-DDTHH:mm:SS.sssZ
	 */
	public static String convertHTTPBasedDateStringToISO(String s,  boolean tryDateUtilities) {
		try {
			int offset = 0;
			if      (s.contains("EST")) { s=s.replace("EST","GMT");	offset = 5; }
			else if (s.contains("PST")) { s=s.replace("PST","GMT");	offset = 8; }
			else if (s.contains("UTC")) { s=s.replace("UTC","GMT");	offset = 0; }
			
			if (s.contains("+0000 ")) { s= s.replace("+0000 ", "");}
			if (s.endsWith("+0000")) { s= s.substring(0, s.indexOf("+0000")); }
			
			if (s.trim().equals("-1") || s.trim().equals("0")|| s.trim().equals("GMT")) { // invalid date times should be marked in the past no matter what
				return ZonedDateTime.parse("1900-01-01T00:00:00+00:00[GMT]").format(DateTimeFormatter.ISO_INSTANT);
			}
			
			
			if (s.contains("GMT") == false) {
				s = s.trim() + " GMT";
			}
			ZonedDateTime zdt = null;
			try {
				zdt = DateTimeFormatter.RFC_1123_DATE_TIME.parse(s,ZonedDateTime::from);
			}
			catch (Exception e) {
				zdt = RFC_1123_DATE_TIME_MINUS_INITIAL_DAY_OF_WEEK.parse(s.substring(s.indexOf(",")+2),ZonedDateTime::from);
			}
			zdt = zdt.minusHours(offset);  // this can be a null exception, which is handled by the caller 
			return zdt.format(DateTimeFormatter.ISO_INSTANT);
		}
		catch (java.time.format.DateTimeParseException ex) {
			//logger.log(Level.WARNING, "Datetime parse exception: "+s);
			if (tryDateUtilities) {
				return DateUtilities.getFromString(s).format(DateTimeFormatter.ISO_INSTANT);
			}
			return null;
		}
		
	}
		
	/**
	 * This method looks for special format tags within a URL and then replaces the text with given ranges.
	 * 
	 * Support format tags: {label:integerFormat[startNum-endNum]}
	 * Example
	 *   {a:%d[1-3]}  will by replaced by "1","2", and "3"
	 *   {a:%03d[1-2]} will by replaced by "001" and "002"
	 *   
	 * String url="http://www.amazon.com/s/?node=11608080011&page={a:%03d[1-5]}&subpage={b:%d[1-2]}";
	 * 
	 * @param originalURL
	 * @return list of the URL with the format tags expanded
	 */
	public static List<String> expandURLs(String originalURL) {
		
		Pattern p2 = Pattern.compile("\\{(.*?):(.*?)\\}");
		Matcher m = p2.matcher(originalURL);

		ArrayList<String> parameters = new ArrayList<String>();
		while(m.find()) {
		    parameters.add(m.group());
		}
		ArrayList<String> URLs = new ArrayList<String>();
		URLs.add(originalURL);
		
		for (String param: parameters) {
			int colonIndex        = param.indexOf(":");
			int openBracketIndex  = param.indexOf("[");
			int closeBracketIndex = param.indexOf("]");
			int dashIndex         = param.indexOf("-");
			
			int startNumber = Integer.parseInt(param.substring(openBracketIndex+1,dashIndex));
			int endNumber   = Integer.parseInt(param.substring(dashIndex+1,closeBracketIndex));
			
			String format = param.substring(colonIndex+1,openBracketIndex);

			for (int j=URLs.size()-1;j>=0;j--) {
				String urlToModify = URLs.remove(j);
				
				for (int i=startNumber; i<= endNumber; i++) {
					String number = String.format(format, i);
					URLs.add(urlToModify.replace(param, number));
				}
			}
		}
		//URLs.stream().forEach(System.out::println);
		
		return URLs;
	}
	
	/**
	 * Converts the headers in the Page object to a HashMap. 
	 * Date fields are converted from the HTTP Date format to ISO
	 * 
	 * @param headers
	 * @return hashmap of the HTTP headers
	 */
	public static java.util.HashMap<String, String> convertHeadersToHashMap(org.apache.http.Header[] headers) {
		java.util.HashMap<String, String> result = new java.util.HashMap<String, String>();
		for (org.apache.http.Header head: headers) {
			String name = head.getName();
			String value = head.getValue();
			if (InternetUtilities.isDateHTTPHeader(name)) {
				try {
					value = InternetUtilities.convertHTTPBasedDateStringToISO(value);
				}
				catch (Throwable t) {
					logger.log(Level.WARNING, "Unable to convert date field - "+value, t);
					continue;
				}
			}
			result.put(name,value);
		}
		return result;
	}
	
	public static String read(java.net.URL url, String userAgent) throws IOException {
		URLConnection conn = url.openConnection();
		
		conn.addRequestProperty("User-Agent", userAgent);
		
		return FileUtilities.read(conn.getInputStream());
	}
	/*
	public static String read(String url, String userAgent) throws IOException {
		java.net.URL u = new java.net.URL(url);
		return read(u, userAgent);
	}
	*/
	
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

	/**
	 * Parse out a charset from a content type header.
	 * 
	 * @param contentType
	 *            e.g. "text/html; charset=EUC-JP"
	 * @return "EUC-JP", or null if not found. Charset is trimmed and
	 *         uppercased.
	 */
	public static String getCharsetFromContentType(String contentType) {
		if (contentType == null)  return null;
		  
		Matcher m = charsetPattern.matcher(contentType);
		if (m.find()) {
			return m.group(1).trim().toUpperCase();
		}
		return null;
	}
	
	/**
	 * Find the passed in URL, find the "base" url for the given host.
	 * 
	 * @param initialURL
	 * @return
	 * @throws MalformedURLException 
	 */
	public static String getBaseURL(String initialURL) throws MalformedURLException {
		int doubleSlashPosition = initialURL.indexOf("//");
		if (doubleSlashPosition < 0) {
			throw new MalformedURLException("URL does not contain a protocol");
		}
		//String protocol = initialURL.substring(0,doubleSlashPosition);
		
		int slashPosition = initialURL.indexOf("/",doubleSlashPosition +2);
		if (slashPosition < 0) {slashPosition = initialURL.length(); }
		
		String resultURL = initialURL.substring(0, slashPosition) + "/";
		
		return resultURL;
	}
	
	public static HttpContent retrieveURL(String targetURL, String userAgent, int numRedirects) throws IOException {
		return retrieveURL(targetURL, userAgent, numRedirects, false);
	}
	
	public static HttpContent retrieveURL(String targetURL, String userAgent, int numRedirects, boolean includeJSoupDocument) throws IOException {
		if (numRedirects > 5) { throw new IllegalStateException("Processed to many redirects for "+targetURL); }
		
		java.net.URL url;
		java.net.URLConnection conn;
		HttpURLConnection httpConn;
		try {
			url = new java.net.URL(targetURL);
			conn = url.openConnection();
			conn.setRequestProperty("User-Agent", userAgent);
			conn.setConnectTimeout(15 * 1000);
			conn.setReadTimeout(15 * 1000);
			conn.connect();
			
		} catch (java.net.SocketTimeoutException e1) {
			e1.printStackTrace();
			return null;
		}
		
		httpConn = (HttpURLConnection) conn;
		if (httpConn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM ||
			httpConn.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP ||
			httpConn.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
			String reDirectURL = httpConn.getHeaderField("Location");
			return retrieveURL(reDirectURL,userAgent,numRedirects+1, includeJSoupDocument);
		}	
		
		HttpContent result = new HttpContent();
		result.responseCode = httpConn.getResponseCode();
		
		result.contentType = conn.getContentType();
		result.charset     = InternetUtilities.getCharsetFromContentType(result.contentType);
		if (result.charset  == null) { result.charset  = StandardCharsets.UTF_8.name();		}
		if (result.contentType == null) { result.contentType = "text/plain"; }
		
		result.contentData = FileUtilities.readAllBytes(conn.getInputStream()); conn.getInputStream().close();

		
		result.outgoingURLs = new HashSet<String>();
		if (result.contentType.startsWith("text") || result.contentType.equals("")) {
			try {
				org.jsoup.nodes.Document jDoc = Jsoup.parse( result.getContentDataAsString(), targetURL);
				
				if (includeJSoupDocument) {
					result.jsoupDocument = jDoc;
				}
				
				for (Element link: jDoc.select("a[href]")) {
					result.outgoingURLs.add(link.attr("abs:href"));
				}
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Unable to parse outgoing URLS in creating jsoup document: content-type="+result.contentType+", exception: "+e);
			}
		}
		
		result.url = targetURL;
		result.domain = (url).getHost();
				
		java.util.Map<String, java.util.List<String>> headerMap = conn.getHeaderFields();
		result.httpHeaders = new org.apache.http.Header[headerMap.size()];

		int pos=0;	
		for (java.util.Map.Entry<String, java.util.List<String>> entry : headerMap.entrySet()) {
			String headerName = entry.getKey();
			if (headerName == null) { headerName = "StatusHeader"; }
			String headerValue = entry.getValue().get(0);
			result.httpHeaders[pos] = new org.apache.http.message.BasicHeader(headerName, headerValue);
			pos++;
		}
		return result;
	}
	
	public static HttpContent retrieveURLToFile(String targetURL, String userAgent, int numRedirects, File storageLocation) throws IOException {
		if (numRedirects > 5) { throw new IllegalStateException("Processed to many redirects for "+targetURL); }
		
		java.net.URL url = new java.net.URL(targetURL);
		java.net.URLConnection conn = url.openConnection();
		conn.setRequestProperty("User-Agent", userAgent);
		conn.setConnectTimeout(15 * 1000);
		conn.setReadTimeout(15 * 1000);
		conn.connect();
		
		HttpURLConnection httpConn = (HttpURLConnection) conn;
		if (httpConn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM ||
			httpConn.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP ||
			httpConn.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
			String reDirectURL = httpConn.getHeaderField("Location");
			return retrieveURLToFile(reDirectURL,userAgent,numRedirects+1, storageLocation);
		}	
		
		HttpContent result = new HttpContent();
		
		result.contentType = conn.getContentType();
		result.charset     = InternetUtilities.getCharsetFromContentType(result.contentType);
		if (result.charset  == null) { result.charset  = StandardCharsets.UTF_8.name();		}
		if (result.contentType == null) { result.contentType = "text/plain"; }

		
		
		InputStream is = conn.getInputStream();
		FileOutputStream fos = new FileOutputStream(storageLocation);
		BufferedOutputStream buffer = new BufferedOutputStream(fos, 16384);
		
	    int nRead;
	    byte[] data = new byte[16384];

	    while ((nRead = is.read(data, 0, data.length)) != -1) {
	      buffer.write(data, 0, nRead);
	    }
	    buffer.flush();
	    buffer.close();
	    is.close();
		
		result.outgoingURLs = new HashSet<String>();
		if (result.contentType.startsWith("text") || result.contentType.equals("")) {
			try {
				org.jsoup.nodes.Document jDoc = Jsoup.parse(storageLocation, targetURL);
				
				result.jsoupDocument = jDoc;
				
				for (Element link: jDoc.select("a[href]")) {
					result.outgoingURLs.add(link.attr("abs:href"));
				}
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Unable to parse outgoing URLS in creating jsoup document: content-type="+result.contentType+", exception: "+e);
			}
		}
		
		
		result.url = targetURL;
		result.domain = (url).getHost();
				
		java.util.Map<String, java.util.List<String>> headerMap = conn.getHeaderFields();
		result.httpHeaders = new org.apache.http.Header[headerMap.size()];

		int pos=0;	
		for (java.util.Map.Entry<String, java.util.List<String>> entry : headerMap.entrySet()) {
			String headerName = entry.getKey();
			if (headerName == null) { headerName = "StatusHeader"; }
			String headerValue = entry.getValue().get(0);
			result.httpHeaders[pos] = new org.apache.http.message.BasicHeader(headerName, headerValue);
			pos++;
		}
		return result;
	}	
	
	
	public static HttpContent createHttpContent(String targetURL, CloseableHttpResponse httpResponse) throws IOException {
		return createHttpContent(targetURL, httpResponse, false);
	}
	
	public static HttpContent createHttpContent(String targetURL, CloseableHttpResponse httpResponse, boolean includeJSoupDocument) throws IOException {
		HttpContent result = new HttpContent();
		java.net.URL url = new java.net.URL(targetURL);
		
		Header contentHeader = httpResponse.getFirstHeader("Content-Type"); 
		if (contentHeader != null) {
			result.contentType = contentHeader.getValue();
			result.charset     = InternetUtilities.getCharsetFromContentType(result.contentType);
		}
		if (result.charset  == null) { result.charset  = StandardCharsets.UTF_8.name();		}
		if (result.contentType == null) { result.contentType = "text/plain"; }
		
		result.contentData = EntityUtils.toByteArray(httpResponse.getEntity()); 
		
		result.outgoingURLs = new HashSet<String>();
		if (result.contentType.startsWith("text") || result.contentType.startsWith("application/xhtml+xml") || result.contentType.equals("")) {
			try {
				org.jsoup.nodes.Document jDoc = Jsoup.parse( result.getContentDataAsString(), targetURL);
				if (includeJSoupDocument) {
					result.jsoupDocument = jDoc;
				}
				
				for (Element link: jDoc.select("a[href]")) {
					result.outgoingURLs.add(link.attr("abs:href"));
				}
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Unable to parse outgoing URLS in creating jsoup document: content-type="+result.contentType+", exception: "+e);
			}
		}
		
		result.url = targetURL;
		result.domain = (url).getHost();			
		result.httpHeaders = httpResponse.getAllHeaders();

		return result;
	}

	public static HttpContent createHttpContent(byte data[], String contentType) throws IOException {
		HttpContent result = new HttpContent();

		result.contentType = contentType;
		result.charset  = StandardCharsets.UTF_8.name();
		result.contentData = data;
		
		result.outgoingURLs = new HashSet<String>();
		if (result.contentType.startsWith("text") || result.contentType.equals("")) {
			try {
				org.jsoup.nodes.Document jDoc = Jsoup.parse( result.getContentDataAsString());
				
				for (Element link: jDoc.select("a[href]")) {
					result.outgoingURLs.add(link.attr("abs:href"));
				}
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Unable to parse outgoing URLS in creating jsoup document: content-type="+result.contentType+", exception: "+e);
			}
		}
		
		result.url = "notApplicable";
		result.domain = "localhost";			
		result.httpHeaders = new org.apache.http.Header[0];

		return result;
	}	
	
	
	public static class HttpContent {
		public String url;
		public String domain;
		public String contentType;
		public String charset;
		public byte[] contentData;
		public Set<String> outgoingURLs;
		public org.apache.http.Header[] httpHeaders; 
		public org.jsoup.nodes.Document jsoupDocument;
		public int responseCode;
		
		public File localFileLocation; /* if we downloaded the file to a file store, this points to that file.  Needed for large content such as movies. */
		
		public String getContentDataAsString() {
			try {
				return new String(contentData,charset);
			} catch (UnsupportedEncodingException e) {
				return new String(contentData);
			}
		}
	}
	
	 /**
     * Get an absolute URL from a URL value that may be relative (i.e. an <code>&lt;a href></code> or
     * <code>&lt;img src></code>).
     * <p/>
     * E.g.: <code>String absUrl = linkEl.absUrl("href");</code>
     * <p/>
     * If the attribute value is already absolute (i.e. it starts with a protocol, like
     * <code>http://</code> or <code>https://</code> etc), and it successfully parses as a URL, the attribute is
     * returned directly. Otherwise, it is treated as a URL relative to the element's {@link #baseUri}, and made
     * absolute using that.
     * <p/>
     * As an alternate, you can use the {@link #attr} method with the <code>abs:</code> prefix, e.g.:
     * <code>String absUrl = linkEl.attr("abs:href");</code>
     *
     * @param attributeKey The attribute key
     * @return An absolute URL if one could be made, or an empty string (not null) if the attribute was missing or
     * could not be made successfully into a URL.
     * @see #attr
     * @see java.net.URL#URL(java.net.URL, String)
     */
    public static String absUrl(String relativeURL, String baseURI) {
        URL base;
        try {
            try {
                base = new URL(baseURI);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute may be abs on its own, so try that
                URL abs = new URL(relativeURL);
                return abs.toExternalForm();
            }
            // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
            if (relativeURL.startsWith("?"))
            	relativeURL = base.getPath() + relativeURL;
            URL abs = new URL(base, relativeURL);
            return abs.toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }
    }	
	
    /**
     * Extracts parameters from a get URL
     * 
     * @param url
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String, List<String>> getQueryParams(String url) throws UnsupportedEncodingException {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length < 2) {
            return params;
        }

        String query = urlParts[1];
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            String key = URLDecoder.decode(pair[0], "UTF-8");
            String value = "";
            if (pair.length > 1) {
                value = URLDecoder.decode(pair[1], "UTF-8");
            }

            // skip ?& and &&
            if ("".equals(key) && pair.length == 1) {
                continue;
            }

            List<String> values = params.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                params.put(key, values);
            }
            values.add(value);
        }

        return params;
    }
    
}
