package edu.ncsu.las.storage.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.User;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.Export;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;

/**
 * Factory-type class to assist with exporting data from ElasticSEarch to a variety of different
 * formats.
 * 
 * In early February, 2017, this code was refactored to use sroll API rather than the native java
 * API due to significant dependency conflicts from ElasticSearch to other parts of the code
 * (logging frameworks and netty).
 * 
 *
 */
public class ExportAssistant {
	private static Logger logger = Logger.getLogger(ExportAssistant.class.getName());
	
	/**
	 * Deletes temporary search result download files which are older than their 'time to live'
	 * 
	 */
	public static void deleteTempExportFiles() {
		String path = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.EXPORT_DOWNLOAD_PATH); 
		int timeToLiveHours = (int) Configuration.getConfigurationPropertyAsLong(Domain.DOMAIN_SYSTEM, ConfigurationType.EXPORT_TIME_TO_LIVE_HOURS);

		File directory = new File(path);
		
		if (directory.exists()) {
			File[] listFiles = directory.listFiles();
			long purgeTime = System.currentTimeMillis() - (1 * timeToLiveHours * 60 * 60 * 1000);
			for (File listFile : listFiles) {
				if (listFile.lastModified() < purgeTime) {
					if (!listFile.delete()) {
						logger.log(Level.SEVERE, "Unable to delete temporary export file: "+listFile);
					}
				}
			}
		}
	}
	
	public static void processDownload(String domain, FileStorageAreaType area, User u, JSONObject queryClause, JSONObject optionObject, String downloadURL) throws IOException {
		// Actual file name
		String fileName = u.getUserID() + "_" + UUID.randomUUID().toString();
		
		// set type for fileName
		Export.Format exportFormat = Export.Format.getEnumByWebPageParameter(optionObject.getString("format"));
		switch (exportFormat) {
		    case JSON_OBJ_LINE:  fileName += ".json"; break;
			case JSON_ARRAY: fileName += ".json"; break;
			case CSV:        fileName += ".csv";  break;
			case TAB:        fileName += ".dat";  break;
			case IND_TEXT_EXPANDED:
			case IND_TEXT_ONLY:
			case JSON_FILE:  fileName += ".zip";  break;
		}
				
	    // Create a path to temp export directory
	    Path tempPath = Paths.get(Configuration.getConfigurationProperty(domain, ConfigurationType.EXPORT_DOWNLOAD_PATH));
		if (!Files.exists(tempPath)) {
			Files.createDirectories(tempPath);
			logger.log(Level.INFO,"Creating: " + tempPath);
		}

		String outputFile = Paths.get(tempPath.toString(), fileName).toString();
		
		long maxRecords = Configuration.getConfigurationPropertyAsLong(domain, ConfigurationType.EXPORT_MAX_RECORD_COUNT);	   
		if (exportFormat == Export.Format.CSV || exportFormat == Export.Format.TAB || exportFormat == Export.Format.JSON_ARRAY || exportFormat == Export.Format.JSON_OBJ_LINE ) {
			ElasticSearchExportFile esef = new ElasticSearchExportFile(domain, area, queryClause, outputFile, exportFormat, maxRecords);
			esef.export();
		}
		else {
			ElasticSearchExportZIP esez = new ElasticSearchExportZIP(domain, area, queryClause, optionObject, outputFile, exportFormat, maxRecords, false);
			esez.export();
		}
		/*else {
			ElasticSearchExportTAR eset = new ElasticSearchExportTAR(domain, area, queryClause, optionObject, outputFile, exportFormat, maxRecords, false);
			eset.export();
		}*/
		
		downloadURL += "/rest/"+domain+"/search/download/" + fileName;
		logger.log(Level.INFO,"download URL: " + downloadURL);

		try {
			String body = "<p> Your search query to '" + Collector.getTheCollecter().getDomain(domain).getFullName() + "' is ready for download at <br>" +
					downloadURL + " </p>" +
					"<p>The link will be active for 24 hours.</p>";

			Collector.getTheCollecter().getEmailClient().sendMessage(u.getEmailID(), "OpenKE: Search Export", body);
		} catch (MessagingException e) {
			logger.log(Level.SEVERE, "Failed to send email",e);
		}
	}	

	public static void processVoyant(String domain, FileStorageAreaType area, User u, JSONObject queryClause, JSONObject optionObjectFromUser) throws IOException {
		// Actual file name
		String fileName = u.getUserID() + "_" + UUID.randomUUID().toString() + ".tar";
		
	    // Create a path to temp export directory
	    Path tempPath = Paths.get(Configuration.getConfigurationProperty(domain, ConfigurationType.EXPORT_DOWNLOAD_PATH)); // usee the download location as a temporary holding spot for sending files to voyant

		if (!Files.exists(tempPath)) {
			Files.createDirectories(tempPath);
			logger.log(Level.INFO,"Creating: " + tempPath);
		}

		String outputFile = Paths.get(tempPath.toString(), fileName).toString();
	   
		boolean stemWords = false;
		if (optionObjectFromUser.optString("stem", "false").equalsIgnoreCase("true")) { stemWords = true;}
		
		JSONObject optionObject = new JSONObject().put("naming", "voyant")
				                                  .put("destination", "voyant")
				                                  .put("format","indTextOnly")
				                                  .put("grouping", "noGroup")
				                                  .put("exportName","na")
				                                  .put("eliminateNonSentences",true)
				                                  .put("stem", Boolean.toString(stemWords));
		long maxRecords = Configuration.getConfigurationPropertyAsLong(domain,ConfigurationType.EXPORT_VOYANT_MAX_RECORD_COUNT);
		ElasticSearchExportTAR eset = new ElasticSearchExportTAR(domain, area, queryClause, optionObject, outputFile, Export.Format.IND_TEXT_ONLY, maxRecords, true);
		eset.export();
		
		String downloadURL = null;
		
		try {
	        String url = Configuration.getConfigurationProperty(domain,ConfigurationType.EXPORT_VOYANT_POST_URL); // trombone is part of this now... + "/trombone";
	        
	        HttpClient client = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(url);
			
			// Set input file
			File inputFile = new File(outputFile);

			// Add file
			MultipartEntityBuilder mpEntity = MultipartEntityBuilder.create().addBinaryBody("upload", inputFile,ContentType.create("application/octet-stream"), inputFile.getName());
			
			// Add tool name. Required at server to process request
			mpEntity.addTextBody("tool", "corpus.CorpusCreator");
			
			org.apache.http.HttpEntity httpEntity = mpEntity.build();
			request.setEntity(httpEntity);

			HttpResponse response = null;
			response = client.execute(request);

			if (response.getStatusLine().getStatusCode() == 200) {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				
				//System.out.println("Test with zip:");
				//System.out.println("Response: " + response.getStatusLine().getStatusCode());
				//System.out.println(result);   //{  "duration": 5112,  "stepEnabledCorpusCreator": {    "storedId": "65086189cda777676e517d28fbf5cae5"  }}
				
				JSONObject voyantResult = new JSONObject(result.toString());
				String corpusID = voyantResult.getJSONObject("stepEnabledCorpusCreator").getString("storedId");
				downloadURL = Configuration.getConfigurationProperty(domain,ConfigurationType.EXPORT_VOYANT_ACCESS_URL) + "?corpus=" + corpusID;

				//delete the temporary working file
				new File(outputFile).delete();
			} else {
				downloadURL = "Voyant Error: "+ response.getStatusLine().getStatusCode();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		
		logger.log(Level.INFO,"download URL: " + downloadURL);

		try {
			String body;
			if (downloadURL.startsWith("Voyant Error") == false) {
				body = "<p>Voyant text analysis for  '" + Collector.getTheCollecter().getDomain(domain).getFullName() + "' is ready to view at <br>" +
						downloadURL + "</p>";
			}
			else {
				body = "<p>Unable to process Voyant Text Analysis</p><p>" + downloadURL + "</p>";
			}
			Collector.getTheCollecter().getEmailClient().sendMessage(u.getEmailID(), "OpenKE: Search Export", body);
		} catch (MessagingException e) {
			logger.log(Level.SEVERE, "Failed to send email",e);
		}
	}	

	public static void processDirectory(String domain, FileStorageAreaType area, User u,  JSONObject queryClause, JSONObject optionObject) throws IOException {

	    // Create a path to the export directory
	    //Path exportPath = Paths.get(Configuration.getConfigurationProperty(domain,ConfigurationType.EXPORT_EXTERNAL_SYSTEM_PATH), optionObject.getString("exportName"));
	    Path exportPath = Paths.get(Configuration.getConfigurationProperty(domain,ConfigurationType.EXPORT_EXTERNAL_SYSTEM_PATH), u.getUserID());

		if (!Files.exists(exportPath)) {
			Files.createDirectories(exportPath);
			logger.log(Level.INFO,"Creating: " + exportPath);
		}
		
		String notificationLocation;
		//which page did we come from? gets something like domain.discover.session or domain.manage.search
		String callingPage = optionObject.getString("currPage");
		String exportType;
		if (callingPage.indexOf("search") != -1) {
			exportType = "_exp_searchHoldings_";
		} else if (callingPage.indexOf("discover") != -1) {
			exportType = "_disc_discSessionName_";
		} else {
			exportType = "_job_";
		}
		
		long maxRecords = Configuration.getConfigurationPropertyAsLong(domain,ConfigurationType.EXPORT_MAX_RECORD_COUNT);
		Export.Format exportFormat = Export.Format.getEnumByWebPageParameter(optionObject.getString("format"));
		if (exportFormat == Export.Format.CSV || exportFormat == Export.Format.TAB || exportFormat == Export.Format.JSON_ARRAY || exportFormat == Export.Format.JSON_OBJ_LINE) {
			// Actual file name
			//String fileName = u.getUserID() + "_" + UUID.randomUUID().toString();
			String fileName = "openke_"+domain+exportType+ nowAsISO();
			
			System.out.println("exportFormat = "+exportFormat);
			// set type for fileName
			switch (exportFormat) {
				case JSON_OBJ_LINE: fileName += ".json"; break;
				case JSON_ARRAY: 	fileName += ".json"; break;
				case CSV:        	fileName += ".csv";  break;
				case TAB:        	fileName += ".dat";  break;
				default:  break; // this option shouldn't occur because of the enclosing if
			}
				
			String outputFile = Paths.get(exportPath.toString(), fileName).toString();
			
			ElasticSearchExportFile esef = new ElasticSearchExportFile(domain, area, queryClause, outputFile, exportFormat, maxRecords);
			esef.export();
			
			notificationLocation = outputFile;
		}
		else {
			String fileName = "openke_"+domain+exportType+ nowAsISO() + ".zip";
			String outputFile = Paths.get(exportPath.toString(), fileName).toString();
			ElasticSearchExportZIP esez = new ElasticSearchExportZIP(domain, area, queryClause, optionObject, outputFile, exportFormat, maxRecords, false);
			esez.export();
			
			//ElasticSearchExportDirectory esed = new ElasticSearchExportDirectory(domain, area, queryClause, optionObject, outputFile, exportFormat, maxRecords);
			//esed.exportToDirectory();
			
			notificationLocation = exportPath.toString();
		}
		
		logger.log(Level.INFO,"Export to filesystem directory: " + notificationLocation);

		try {
			String body = "<p> Your export of a search query for '" + Collector.getTheCollecter().getDomain(domain).getFullName() + "' has been complete.  The files are located at <br>" +
					notificationLocation + " </p>";

			Collector.getTheCollecter().getEmailClient().sendMessage(u.getEmailID(), "OpenKE: Search Export to directory", body);
		} catch (MessagingException e) {
			logger.log(Level.SEVERE, "Failed to send email",e);
		}
	}
	
	private static String nowAsISO() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		
		return nowAsISO;
	}
	
	public static void initiateDownload(String domain, JSONObject exportObject, JSONObject queryClauseObject,	JSONObject optionObject, User user, String downloadURL) {
		Runnable runTask = new Runnable() {
			@Override
			public void run() {				
				try {
					switch (optionObject.getString("destination")) {
						case "download":  ExportAssistant.processDownload(domain,  FileStorageAreaType.REGULAR, user, queryClauseObject, optionObject, downloadURL); break;
						case "voyant":    ExportAssistant.processVoyant(domain,    FileStorageAreaType.REGULAR, user, queryClauseObject, optionObject);              break;
						case "directory": ExportAssistant.processDirectory(domain, FileStorageAreaType.REGULAR, user, queryClauseObject, optionObject);              break;
						default: logger.log(Level.WARNING,"Invalid export destination: " + exportObject.toString(4));
					}
					
					
				} catch (IOException e) {
					logger.log(Level.WARNING, "Unable to execute export",e);
				}

			}
		};
		Collector.getTheCollecter().runTask(runTask);
	}	
	
}
