package edu.ncsu.las.source;

import edu.ncsu.las.document.Document;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.collector.type.JobHistoryStatus;
import edu.ncsu.las.model.collector.type.MimeType;
import edu.ncsu.las.model.collector.type.SourceParameter;
import edu.ncsu.las.source.util.StopRequestedException;
import edu.ncsu.las.util.InternetUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;


/**
 * Video handler provides a systematic / code driven interface to download a single video through videograbber.net
 * 
 */
public class VideoHandler extends AbstractHandler {


	private static final JSONObject SOURCE_HANDLER_CONFIGURATION = new JSONObject("{ 'webCrawler': {}, 'excludeFilter' : '.*(\\\\.(css|js))$' }");
	
	private static final String SOURCE_HANDLER_NAME = "video";
	private static final String SOURCE_HANDLER_DISPLAY_NAME = "Video Downloader";
	


	private static final java.util.TreeMap<String, SourceParameter> SOURCE_HANDLER_PARAM_CONFIG = new java.util.TreeMap<String, SourceParameter>() {
		private static final long serialVersionUID = 1L;
		{	
			// Need to construct an instance of our parent to get parent configuration as we are in a static context
			WebSourceHandler wsh = new WebSourceHandler();
			java.util.TreeMap<String, SourceParameter> parentParameters = wsh.getConfigurationParameters();
			for (String key: parentParameters.keySet()) {
				put(key, parentParameters.get(key));
			}

	}};
	
	
	@Override
	public JSONObject getHandlerDefaultConfiguration() {
		return SOURCE_HANDLER_CONFIGURATION;
	}

	@Override
	public String getSourceHandlerName() {
		return SOURCE_HANDLER_NAME;
	}
	
	@Override
	public String getSourceHandlerDisplayName() {
		return SOURCE_HANDLER_DISPLAY_NAME;
	}
	
	@Override
	public String getDescription() {
		return "Provides an interface to download videos from the internet.  The URL should provide the link to the specified video.";
	}
	
	@Override
	public ParameterLabelType getPrimaryLabel() {
		return ParameterLabelType.URL;
	}
	
	@Override
	public java.util.TreeMap<String, SourceParameter> getConfigurationParameters() {
		return SOURCE_HANDLER_PARAM_CONFIG;
	}
	
	public java.util.List<String> validateConfiguration(String domainName, String primaryFieldValue, JSONObject configuration) {
		java.util.ArrayList<String> errors = new java.util.ArrayList<String>();	
		
		errors.addAll(super.validateConfiguration(domainName, primaryFieldValue, configuration));

		return errors;
	}
	
	
	
	
	// higher is better
	private static HashMap<String, Integer> formatExtensionPriorities = new HashMap<String,Integer>();
	static {
		formatExtensionPriorities.put("m4a", 10);
		formatExtensionPriorities.put("webm", 15);
		formatExtensionPriorities.put("3gp", 15);
		formatExtensionPriorities.put("mp4", 20);
	}
	
	
	public static int compareFormats(JSONObject bestFormat, JSONObject currFormat) {

		if (formatExtensionPriorities.getOrDefault(currFormat.getString("ext"), 15) >
		    formatExtensionPriorities.getOrDefault(bestFormat.getString("ext"), 15)) {
			return 1;
		}
		
		int bestFormatSize = bestFormat.optInt("width", 1) *  bestFormat.optInt("height", 1);
		int currFormatSize = currFormat.optInt("width", 1) *  currFormat.optInt("height", 1);

		if (currFormatSize >= bestFormatSize) { return 1; }

		return 0;
	}
	
	/**
	 * 
	 * @param formats
	 * @return
	 */
	public static int findBestFormat(JSONArray formats) {
		if (formats.length() == 0) { return -1; }
		
		int bestIndex = 0;
		JSONObject bestFormat = formats.getJSONObject(0);
		for (int i=1; i< formats.length(); i++) {
			JSONObject currFormat = formats.getJSONObject(i);
			if (currFormat.getString("format").contains("audio only")) {
				continue;
			}
		
			int result = compareFormats(bestFormat,currFormat);
			if (result > 0) {
				bestFormat = currFormat;
				bestIndex = i;
			}
		}
		return bestIndex;
	}
		

	public Document downloadFile(String url, File destinationFile, String uuid) {
		
		JSONObject result = this.generateDownloadObject(url);
		
		while (result.getJSONArray("formats").length() > 0) {
			int i = findBestFormat(result.getJSONArray("formats"));
			JSONObject currentRecord = result.getJSONArray("formats").getJSONObject(i);
			try {
				InternetUtilities.HttpContent content = InternetUtilities.retrieveURLToFile(currentRecord.getString("url"), this.getUserAgent(), 0,	destinationFile);
				srcLogger.log(Level.FINEST,"Downloaded video("+url+"), content-type: "+content.contentType);
				if (content.contentType.contains("url")) {
					srcLogger.log(Level.FINER,"Unable to download video("+url+"), found URL content");
					result.getJSONArray("formats").remove(i);
					continue;
				}
				result.put("format", currentRecord);
				result.remove("formats");
				
				Document currentDocument = new Document(uuid, "video", content, destinationFile, this.getJobConfiguration(), this.getJob().getSummary(),this.getDomainInstanceName(), result, this.getJobHistory());

				currentDocument.setExtractedTextFromTika(result.optString("title", ""));
				currentDocument.setExtractedText(result.optString("title", ""),true);
				
				return currentDocument;
			}
			catch (IOException|JSONException e) {
				srcLogger.log(Level.FINER,"Unable to download video("+url+"), trying another format: "+e.toString());
				result.getJSONArray("formats").remove(i);
			}
		}		
		
		return null;
	}

	/**
	 * Determines what URL should be used to download the video.  If the URL links to a video (as identified by the suffix), then that is returned.
	 * Otherwise, the system queries videograbber.net to grab potential download locations for the file
	 * 
	 * @param url
	 * @return
	 */
	public JSONObject generateDownloadObject(String url) {
		
		if (MimeType.doesSuffixIndicateVideo(url)) {
			// the video handler was provided a direct link to a video, just download that.  Create a record for that.
			JSONArray formatArray = new JSONArray();
			formatArray.put(new JSONObject().put("url", url));
			JSONObject result = new JSONObject().put("formats", formatArray);
			return result;
			
		}
		
			
		JSONObject result;
		
		try {
			String userAgent = this.getUserAgent();
			String videoGrabberURL = "https://www.videograbber.net/api/video?action=get-info&url=" + URLEncoder.encode(url, "UTF-8");
			srcLogger.log(Level.INFO, "videograbber URI: " + videoGrabberURL);
				
			
			JSONObject responseObject;
			HttpResponse<JsonNode> jsonResponse = Unirest.get(videoGrabberURL)
						          .header("accept", "application/json, text/javascript, */*; q=0.01")
						          .header("accept-encoding", "gzip, deflate, br")
						          .header("accept-language", "en-US,en;q=0.8")
						          .header("cache-control", "no-cache")
						          .header("pragma", "no-cache")
						          .header("referer", "https://www.videograbber.net/")
						          .header("user-agent", userAgent)
						          .header("x-requested-with","XMLHttpRequest")			          
						          .asJson();
			responseObject = jsonResponse.getBody().getObject();
			
			if (responseObject.getInt("state") != 1) {
				return null;
			}
			result = responseObject.getJSONObject("data");
		} catch (Exception e) {
			srcLogger.log(Level.SEVERE, "Vi exception: " + e.toString());
			return null;
		}
		
		return result;
	}

	public Document processURL(String url) {
		String uuid = edu.ncsu.las.util.UUID.createTimeUUID().toString();
		
		String basePath = Configuration.getConfigurationProperty(this.getDomainInstanceName(), FileStorageAreaType.REGULAR, ConfigurationType.FILE_STORE);
		Path path = Paths.get(basePath,uuid);
		
		Document retrievedVideo = this.downloadFile(url,path.toFile(),uuid);	
		return retrievedVideo;
	}
	
	
	@Override
	public void process() {
		List<String> errors = this.validateConfiguration(this.getDomainInstanceName(), this.getJob().getPrimaryFieldValue(), this.getJob().getConfiguration());
		if (errors.size() > 0) {
			srcLogger.log(Level.SEVERE, "Unable to process "+this.getSourceHandlerName()+" - invalid parameters: "+this.getJob().getName());
			for (String error: errors) {
				srcLogger.log(Level.SEVERE, "        "+error);
			}
			this.getJobCollector().sourceHandlerCompleted(this, JobHistoryStatus.INVALID_PARAMS, "Unable to process "+this.getSourceHandlerName()+" - invalid parameters: "+this.getJob().getName());
			this.setJobHistoryStatus(JobHistoryStatus.INVALID_PARAMS);
			return;
		}
		
		srcLogger.log(Level.INFO, "Job processing starting: " + this.getJob().getName());
		JobHistoryStatus status;
		String message;
		try {
			this.prepareForProcessing();
			
			this.doProcessing();

			srcLogger.log(Level.INFO, "Job processing completed: " + this.getJob().getName());

			status = JobHistoryStatus.COMPLETE;
			if (this.isManuallyStopped()) {
				status = JobHistoryStatus.STOPPED;
				message = "Job stopped upon request.";
			} else {
				message = "Completed " + this.getNumberOfActivities() + " activities";
			}
			this.finalizeProcessing();
			this.updateConfiguration();
		} catch (StopRequestedException sre) {
			status = JobHistoryStatus.STOPPED;
			message = "Job stopped upon request.";
		} catch (Throwable t) {
			status = JobHistoryStatus.ERRORED;
			message = t.getMessage();
			srcLogger.log(Level.SEVERE, "Job processing failed: " + this.getJob().getName(), t);
		}
		this.getJobCollector().sourceHandlerCompleted(this, status, message);
		this.setJobHistoryStatus(status);
		return;
		
	}

	private void doProcessing() throws Exception {
		String url = this.getJob().getPrimaryFieldValue();
		
		Document retrievedVideo = this.processURL(url); // putting this into a separate method so that we can call it externally with a URL from the web source handler.
		if (retrievedVideo == null) { // we weren't able to download
			throw new Exception ("unable to  download video");
		}
		
		this.getDocumentRouter().processPage(retrievedVideo,"noSaveRaw  ignoreDuplicates");
	}

	private void prepareForProcessing() {
		return; // no activity necessary in this handler
		
	}

	private void updateConfiguration() {
		return; // no activity necessary in this handler
		
	}

	private void finalizeProcessing() {
		return; // no activity necessary in this handler
		
	}

	@Override
	public void forceShutdown() {
		return; // no activity necessary in this handler.  We probably could set a flag to interrupt a download
		
	}
	
	
	public static void main(String args[]) throws IOException {
		File currentDirectory = new File(new File(".").getAbsolutePath());
		String currentWorkingDirectory  = currentDirectory.getCanonicalPath();
					
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
				
		srcLogger.log(Level.INFO, "Application Started");
		srcLogger.log(Level.INFO, "Configuration directory: "+currentWorkingDirectory);

		//Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",false,false);		
		
		VideoHandler vh = new VideoHandler();
		vh.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
		JSONObject jo = vh.generateDownloadObject("https://youtu.be/kDtkFeH8icQ");
		System.out.println(jo.toString(4));
		//"
	}
	
}
