package edu.ncsu.las.model.collector;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;

/**
 * SearchRecord is data object that contains information
 * about records generate from a search result along with status about their
 * subsequent page retrieval.   
 * 
 * This class is also used to represent data from RSS / ATOM feeds as it provides a name(title) / description in addition to the url
 * 
 */
public class SearchRecord {
	private static Logger logger =Logger.getLogger(SearchRecord.class.getName());
	
	private String _name;
	private String _url;
	private String _description;
	private String _status       = "new" ;  // possible values: new, crawling, crawled, error
	private String _errorMessage = "";
	private int    _resultPosition = -1;         // what position / order was this record in a search result.  -1 = undefined.
	
	//Track original (untranslated names and descriptions) if necessary.
	private String _nativeName        = null;
	private String _nativeDescription = null;
	
	private String _source;    // what was the source of this search record?
	
	private String _mimeType = "_doc";   // What type is this result?  web
	private JSONObject _document;

	private ZonedDateTime _publishedDateTime = ZonedDateTime.now();
	
	private byte[] _uploadedData;
	private String _uploadedContentType;
	
	/**
	 * Tracks what was this search record's position relative to an earlier run.
	 * positive: has moved up that many positions
	 * negative: moved down that many positions
	 * zero: position is unchanged
	 * Integer.MAX_VALUE - new result
	 * Integer.MIN_VALUE - not set
	 */
	private int _positionRelativeToPriorExecution = Integer.MIN_VALUE;
	
	
	
	private UUID   _documentUUID = edu.ncsu.las.util.UUID.createTimeUUID();
	private Job _job;
	
	private long _initialMS      = -1L;
	private long _startDownloadMS = -1L;
	private long _finishDownloadMS = -1L;
	private long _finishProcessing = -1L;
		
	public SearchRecord() {
	}
	
	public SearchRecord(String name, String url, String description, int position, String source) {
		this._name = name;
		this._url = url;
		this._description = description;
		this._resultPosition = position;
		this._source = source;
	}
	
	public SearchRecord(String name, String url, String description, int position, String source, ZonedDateTime zdt) {
		this(name,url,description,position, source);
		
		_publishedDateTime = zdt;
	}	
	
	
	public SearchRecord(String name, String url, String description, int position, String source, String mimeType, JSONObject document, Job job) {
		this(name,url,description,position, source);
		this._mimeType = mimeType;
		this._document = document;
		this._job      = job;
	}

	public String getName() {
		return _name;
	}
	public void setName(String name) {
		this._name = name;
	}
	public String getUrl() {
		return _url;
	}
	public void setUrl(String url) {
		this._url = url;
	}
	public String getDescription() {
		return _description;
	}
	public void setDescription(String description) {
		this._description = description;
	}
	
	public String getSource() { return _source;}
	public void setSource(String source) { _source = source; }
	
	
	public String getStatus() {
		return _status;
	}
	public void setStatus(String status) {
		this._status = status;
	}
	
	public byte[] getUploadedData() {
		return _uploadedData;
	}

	public void setUploadedData(byte[] uploadedData) {
		_uploadedData = uploadedData;
	}

	public String getUploadedContentType() {
		return _uploadedContentType;
	}

	public void setUploadedContentType(String uploadedContentType) {
		_uploadedContentType = uploadedContentType;
	}	
	
	
	public String getMimeType() { return _mimeType;} 
	public void   setMimeType(String newValue) { _mimeType= newValue; }
	
	/** 
	 * document can be set when retreive the complete record as part of the search operation.  This will be checked
	 * during the crawl phase.  If it exists, then we'll create the full "document" at that time from this record.
	 * @return
	 */
	public JSONObject getDocument() { return _document; }
	public void       setDocument(JSONObject newValue) { _document = newValue; }
	
	
	public String getErrorMessage() {
		return _errorMessage;
	}
	public void setErrorMessage(String erroMessage) {
		this._errorMessage = erroMessage;
	}
	
	public int getResultPosition() {
		return _resultPosition;
	}

	public void setResultPosition(int resultPosition) {
		_resultPosition = resultPosition;
	}
	
	public int getPositionRelativeToPriorExecution() {
		return _positionRelativeToPriorExecution;
	}

	public void setPositionRelativeToPriorExecution(int positionRelativeToPriorExecution) {
		_positionRelativeToPriorExecution = positionRelativeToPriorExecution;
	}

	public UUID getDocumentUUID() {
		return _documentUUID;
	}
	public void  setDocumentUUID(UUID newID) {
		_documentUUID = newID;
	}
	
	public ZonedDateTime getPublishedDateTime() {
		return _publishedDateTime;
	}
	
	public void setPublishedDateTime( ZonedDateTime pbt) {
		if (pbt !=null) {
			_publishedDateTime =pbt;
		}
	}
	
	public JSONObject toMinimalJSON() {
		JSONObject result = new JSONObject().put("title", this.getName())
                                            .put("url", this.getUrl())
                                            .put("description", this.getDescription())
                                            .put("status", this.getStatus())
                                            .put("source", this.getSource())
                                            .put("errorMessage", this.getErrorMessage())
                                            .put("publishedDateTime", this.getPublishedDateTime().toString());
		
		if (this.getNativeName() != null) { result.put("nativeName", this.getNativeName()); }
		if (this.getNativeDescription()!= null) { result.put("nativeDescription", this.getNativeDescription()); }
		
		return result;		
	}
	
	public JSONObject toJSON() {
		JSONObject result = this.toMinimalJSON()
				                            .put("resultPosition", this.getResultPosition())
				                            .put("positionRelativeToPriorExecution", this.getPositionRelativeToPriorExecution())
				                            .put("mimeType", this.getMimeType())
				                            .put("document", this.getDocument())		
				                            .put("uuid", this.getDocumentUUID().toString())
				                            .put("waitTimeMillis", this.getWaitingTimeMillis())
				                            .put("downloadTimeMillis", this.getDownloadTimeMillis())
				                            .put("processingTimeMillis", this.getProcessingTimeMillis());
		return result;
	}
	public String toString() {
		return toJSON().toString();
	}

	public void setJobRecord(Job job) {
		_job = job; 
	}
	public Job getJob() { return _job;}
	
	// implementing equals and hashcode so that we can remove duplicate urls in a list
	
	public boolean equals(Object o) {
		if (!(o instanceof SearchRecord)) {return false;} 
		return _url.equals( ((SearchRecord) o).getUrl());
	}
	
	public int hashCode() {
		return _url.hashCode();
	}
	
	
	// collect stats about the download and processing times.
	
	public void setInitialMillis(long currentTimeMillis)          { _initialMS = currentTimeMillis;        }
	public void setStartDownloadMillis(long currentTimeMillis)    { _startDownloadMS = currentTimeMillis;  }
	public void setFinishDownloadMillis(long currentTimeMillis)   { _finishDownloadMS = currentTimeMillis; }
	public void setFinishProcessingMillis(long currentTimeMillis) { _finishProcessing = currentTimeMillis; }
	
	public long getWaitingTimeMillis() { return _startDownloadMS - _initialMS; }
	public long getDownloadTimeMillis() { return _finishDownloadMS - _startDownloadMS; }
	public long getProcessingTimeMillis() { return _finishProcessing - _finishDownloadMS; }
	
	
	// feed utilities ...
    private static List<SearchRecord> processSyndFeed(SyndFeed feed) {
    	java.util.ArrayList<SearchRecord> results = new java.util.ArrayList<SearchRecord>();
    	
    	List<SyndEntry> entries = feed.getEntries();
    	int position = 0;
		for (SyndEntry entry: entries) {
			try {
				String uri = entry.getUri();
				String link = entry.getLink();
				String title = entry.getTitle();
				String description = (entry.getDescription() == null) ? "" : entry.getDescription().getValue();
				ZonedDateTime zdt = ZonedDateTime.now();
				try {
					zdt = ZonedDateTime.ofInstant(entry.getPublishedDate().toInstant(),ZoneOffset.UTC);
				}
				catch (Exception e) {
					logger.log(Level.INFO, "No published date in feed entry, using current date - exception: "+e.getMessage());
				}
				if (uri !=null && (link == null || link.contains("feedproxy"))) {
					SearchRecord sr = new SearchRecord(title, uri, description, ++position,"feed", zdt);
					results.add(sr);
				}
				else {
					if (uri != null && uri.startsWith("tag:google.com")) { // this feed came from a google alert, just extract the direct URL rather than the pass-through of google
						try {
							Map<String, List<String>> params = InternetUtilities.getQueryParams(link);
							String tempURL = params.get("url").get(0);
							link = tempURL;
						}
						catch (Exception e) {
							logger.log(Level.WARNING,"Unable to extract url from Google Link:"+link,e);
						}
					}
					SearchRecord sr = new SearchRecord(title, link, description, ++position,"feed",zdt);
					results.add(sr);
				}
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Error in processing synd feed: "+e.toString());
				logger.log(Level.WARNING, "Current entry: "+entry.toString());

			}
		}
		logger.log(Level.FINEST,"Feed URLs: "+ results.stream().map(i -> i.toString()).collect(Collectors.joining("\n")));
		return results;
    }
   
	/**
	 * Takes a feed's URL and then returns the links within that feed.
	 * 
	 * If a "DOCTYPE" declaration is contained with in the file, all instances are removed.
	 * 
	 * @return list of URLs within the feed, (on error, message printed to log)
	 */
	public static List<SearchRecord> getFeedEntries(String feedURL, String userAgent) throws Exception {
		
		try {
			//String s =  InternetUtilities.read(feedURL, userAgent);
			
			HttpContent hc = InternetUtilities.retrieveURL(feedURL, userAgent, 0);
			String s = hc.getContentDataAsString();
			s = s.replaceAll("<!DOCTYPE.*?>","");
			return getFeedEntries(s.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to feed URL ("+ feedURL +"): "+e);
			throw e;
		}
	}
	
	/**
	 * Takes a feed's URL and then returns the links within that feed.
	 * 
	 * @return list of URLs within the feed, (on error, message printed to log)
	 */
	public static List<SearchRecord> getFeedEntries(byte data[]) throws Exception {	
		SyndFeedInput input = new SyndFeedInput();
		try {
			SyndFeed feed = input.build(new XmlReader(new java.io.ByteArrayInputStream(data),true,"UTF-8"));
			return processSyndFeed(feed);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to process feed data byte array: "+e);
			throw e;
		}
	}

	public void setNativeName(String name) {
		_nativeName        = name;
	}

	public void setNativeDescription(String description) {
		_nativeDescription = description;
	}	
	
	public String getNativeName() { return _nativeName; }
	public String getNativeDescription() { return _nativeDescription;  }

	
}