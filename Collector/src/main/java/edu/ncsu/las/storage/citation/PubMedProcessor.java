package edu.ncsu.las.storage.citation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.collector.Collector;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.Instrumentation;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.storage.citation.annotation.AuthorFullNameAnnotation;
import edu.ncsu.las.storage.citation.annotation.ExtractFullTextAnnotation;
import edu.ncsu.las.storage.citation.annotation.LocationAnnotation;
import edu.ncsu.las.storage.citation.annotation.PDFPageCountAnnotation;
import edu.ncsu.las.storage.citation.annotation.RecordURLAnnotation;
import edu.ncsu.las.storage.citation.annotation.RecordTitleAnnotation;
import edu.ncsu.las.storage.citation.annotation.TextGeoTagAnnotation;
import edu.ncsu.las.storage.citation.annotation.TextKeyPhraseAnnotation;
import edu.ncsu.las.storage.citation.annotation.TextLengthAnnotation;
import edu.ncsu.las.storage.citation.annotation.TextNamedEntitiesAnnotation;
import edu.ncsu.las.storage.citation.annotation.TextRelationsAnnotation;
import edu.ncsu.las.storage.citation.annotation.TextSectionAnnotation;
import edu.ncsu.las.util.json.JSONUtilities;
import edu.ncsu.las.util.UUID;
import edu.ncsu.las.util.crypto.AESEncryption;

public class PubMedProcessor implements Runnable {
	private static Logger logger =Logger.getLogger(PubMedProcessor.class.getName());

	public static final Pattern REGEX_EMAIL_ADDRESS = Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE );
	public static final Pattern REGEX_URL           = Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

	private static PubMedProcessor _thePubMedProcessor = null;

	public static PubMedProcessor getThePubMedProcessor() {
		if (_thePubMedProcessor == null) {  // race conditions could possibly exist for this, but since we are starting up the class as main, this should not occur in practice
			_thePubMedProcessor = new PubMedProcessor();
		}

		return _thePubMedProcessor;
	}

	private PubMedProcessor() {}


	public static void main(String[] args) throws Exception {

		//String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
		//String outputFile = "C:\\pubmed\\wolfhunt_20171109.csv";
		//PubMedUtilities.produceCSVFile(jsonRecordLocation, outputFile);
		//System.exit(0);

		//PubMedUtilities.produceMasterJSONFile();
		//PubMedUtilities.produceISIFile();
		//System.exit(0);



		/*
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
		String number = "25855067";

		String file = jsonRecordLocation + number+".json";
		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(file));
		String isi = PubMedUtilities.convertPubMedJSONToISI(record,0);
		System.out.println(isi);
		System.exit(0);
		*/
		/*
		JSONObject jo = PubMedUtilities.convertGZippedXMLFile(new File("C:\\pubmed\\pubmed\\updatefiles\\medline17n1258.xml.gz"));

		JSONArray articleList = jo.getJSONArray("PubmedArticle");
		JSONArray articleRefactordList = new JSONArray();

		String[] keywords = { "crispr", "cas9" }; //, "dna", "gene", "genome" };

		for (int i=0; i < articleList.length(); i++) {
			JSONObject article = articleList.getJSONObject(i);
			JSONObject refactoredArticle = PubMedUtilities.refactorCitation(article);
			articleRefactordList.put(refactoredArticle);

			if (recordHasKeyword(refactoredArticle, keywords)) {
				System.out.println("found");
			}


			//System.out.println(refactoredArticle.toString(4));

			//break;
		}

		JSONObject temp = new JSONObject().put("articles", articleRefactordList);
		Files.write(Paths.get("C:\\pubmed\\1258.json"), temp.toString(4).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		System.exit(0);



		HashMap<String,Integer> counts = JSONUtilities.countAllFields(temp);

		for (String key: counts.keySet()) {
		//	System.out.println(key+"\t"+counts.get(key));
		}
		//Fields to search


		//System.exit(0);
		//List<DeleteCitationRecord> dcrList = PubMedUtilities.extractDeleteRecords(jo);
		//for (DeleteCitationRecord dcr: dcrList) {
		//	System.out.println(dcr);
		//}




		System.exit(0);
		//JSONObject jo = convertGZippedXMLFile(new File("C:\\pubmed\\pubmed\\updatefiles\\medline17n1258.xml.gz"));
		//Files.write(Paths.get("C:\\pubmed\\1258.json"), jo.toString(4).getBytes());
		*/
		try {
			File currentDirectory = new File(new File(".").getAbsolutePath());
			String currentWorkingDirectory  = currentDirectory.getCanonicalPath();

			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");

			logger.log(Level.INFO,"PubMed Processeor starting....");
			logger.log(Level.INFO,"Validating unlimited strength policy files....");
			if (AESEncryption.hasUnlimitedStrengthPolicy() == false) {
				logger.log(Level.SEVERE, "JobCollector halting: Unlimited Strength Jurisdiction Policy Files are not installed for Java.");
				System.exit(1);
			}

			logger.log(Level.INFO,"Starting initialization....");
			Collector.initializeCollector(currentWorkingDirectory,"system_properties.json",true,true,false);

			PubMedProcessor pmp = PubMedProcessor.getThePubMedProcessor();

			logger.log(Level.INFO,"PubMed Processeor initialized");

			//String[] keywords = { "crispr", "cas9" };
			//int[] resultsFull = PubMedUtilities.countJSONRecordsWithKeywords(ConfigurationType.PUBMEDIMPORTER_BASELINE, keywords);
			//int[] resultsUpdates = PubMedUtilities.countJSONRecordsWithKeywords(ConfigurationType.PUBMEDIMPORTER_UPDATES, keywords);

			//System.out.println("Total records:"+ (resultsFull[0]+resultsUpdates[0]));
			//System.out.println("Found records:"+ (resultsFull[1]+resultsUpdates[1]));
			//System.exit(0);
			//String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
			//pmp.storeJSONRecordsInElastic(jsonRecordLocation, "C:\\pubmed\\pubmed\\extractHTMLFiles", "C:\\pubmed\\pubmed\\extractPDFFiles");
			//System.exit(0);

			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "pubmed.start", System.currentTimeMillis(), null, null, null);


			Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(pmp, 0, Configuration.getConfigurationPropertyAsLong(Domain.DOMAIN_SYSTEM,ConfigurationType.PUBMEDIMPORTER_SLEEPTIME), TimeUnit.DAYS); //TODO: Change back to seconds



		}
		catch(Throwable t) {
			logger.log(Level.SEVERE,"Unhandled excpetion in main: ", t);
		}
	}



	/**
	 * Primary application loop/processing.  (loop is driven in the main process through an Executor)
	 */
	public void run() {
		long startTime = System.currentTimeMillis();

		try {
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "pubmed.processLoop.start", startTime, null, null, null);

			logger.log(Level.INFO, "Start process loop");

			downloadFiles(ConfigurationType.PUBMEDIMPORTER_BASELINE);
			downloadFiles(ConfigurationType.PUBMEDIMPORTER_UPDATES);



			//String[] keywords = { "crispr", "cas9" }; //, "dna", "gene", "genome" };
			String[] keywords = { "parental alienation",  "parent-child alienation" }; //"loyalty conflict",

			//Establish the location for the extracted JSON records
			String jsonRecordLocation = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.PUBMEDIMPORTER_BASEDIRECTORY) + "pubmed/extractedRecords/";
			File jsonRecordFileObj = new File(jsonRecordLocation);
			jsonRecordFileObj.mkdirs();

			//extractJSONRecordsWithKeywords(ConfigurationType.PUBMEDIMPORTER_BASELINE, jsonRecordLocation, keywords);
			//extractJSONRecordsWithKeywords(ConfigurationType.PUBMEDIMPORTER_UPDATES, jsonRecordLocation, keywords);

			String htmlDirectory = "C:\\pubmed\\pubmed\\extractHTMLFiles";
			String pdfDirectory  = "C:\\pubmed\\pubmed\\extractPDFFiles";
			new File(htmlDirectory).mkdirs();
			new File(pdfDirectory).mkdirs();

			// Need to remove any deleted records

			for (File f: (new File(jsonRecordLocation)).listFiles() ) {
				if (f.getName().endsWith(".json") == false) {continue;}
				//if (f.getName().compareTo("28782570.json") < 0) {continue;}

				logger.log(Level.INFO, "Processing: "+f.getName());


				String recordNumber = f.getName().substring(0,f.getName().indexOf('.'));

				String htmlFileLocation = htmlDirectory + "\\" + recordNumber + ".html";
				String pdfFileLocation  = pdfDirectory  + "\\" + recordNumber + ".pdf";

				try {
					JSONObject record = new JSONObject(  new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));
					PubMedDownloadFullReport.downloadCitationFullReport(record, htmlFileLocation, pdfFileLocation);
				}
				catch(Exception e) {
					logger.log(Level.WARNING, "Unable to download fulltext: "+f.getName(), e);
				}

				//( new GeoCodeAuthorOrganizationAnnotation()).process(f, htmlFileLocation, pdfFileLocation);

				( new ExtractFullTextAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new PDFPageCountAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new TextLengthAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new TextSectionAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				(new AuthorFullNameAnnotation()).process(f, htmlFileLocation, pdfFileLocation);

				//( new TextConceptAnnotation()).process(f, htmlFileLocation, pdfFileLocation);  //TODO:  Wolfhunt domain is hard coded in this right now
				( new TextGeoTagAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new TextKeyPhraseAnnotation()).process(f, htmlFileLocation, pdfFileLocation);

				( new RecordURLAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new RecordTitleAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new LocationAnnotation()).process(f, htmlFileLocation, pdfFileLocation);

				( new TextRelationsAnnotation()).process(f, htmlFileLocation, pdfFileLocation);
				( new TextNamedEntitiesAnnotation()).process(f, htmlFileLocation, pdfFileLocation);


			}
			System.exit(0);
			storeJSONRecordsInElastic(jsonRecordLocation, "C:\\pubmed\\pubmed\\extractHTMLFiles", "C:\\pubmed\\pubmed\\extractPDFFiles");

			logger.log(Level.INFO, "End process loop");
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "pubmed.processLoop.end", startTime, System.currentTimeMillis(), null, null);
		}
		catch (Throwable t) {
			logger.log(Level.SEVERE, "Unable able to process",t);
			Instrumentation.createAndSendEvent(Domain.DOMAIN_SYSTEM, "pubmed.processLoop.exception", System.currentTimeMillis(), null, (new JSONObject()).put("exception",t.toString()), null);
			//Let's just exit the run-loop.  This may cause the error to appear every x minutes, but this temporary communication
			//problems won't be impacted.
		}
	}


	public void storeJSONRecordsInElastic(String recordDirectory, String htmlDirectory, String pdfDirectory) {

		String url = "http://serverNameOrIP:9200/whpubmed_normal";

		try {
			// create index w/ default settings from the configuration
			HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
					.header("accept", "application/json")
					.body(Configuration.getConfigurationObject("wolfhuntv2", ConfigurationType.ELASTIC_DEFAULT_SETTINGS))
	                .asJson();

			JSONObject esResult = jsonResponse.getBody().getObject();
			if (esResult.optBoolean("acknowledged",false) == false) {
				logger.log(Level.SEVERE, "Unable to create index exists in ElasticSearch (URL: "+url+" ): "+ esResult.optString("error",""));
				//return;
			}

		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE, "Unable to create index in ElasticSearch: "+ url);
		}



		for (File f: (new File(recordDirectory)).listFiles() ) {
			if (f.getName().endsWith(".json") == false) {continue;}
			//if (f.getName().compareTo("26000843.json") < 0) {continue;}


			logger.log(Level.INFO, "Store JSON record in ElasticSearch: "+f.getName());


			String recordNumber = f.getName().substring(0,f.getName().indexOf('.'));



			//String htmlFileLocation = htmlDirectory + "\\" + recordNumber + ".html";
			String pdfFileLocation  = pdfDirectory  + "\\" + recordNumber + ".pdf";

			File pdfFile = new File(pdfFileLocation);

			if ( pdfFile.exists()  ) {
				try {
					JSONObject record = new JSONObject(  new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));

					String uuid = UUID.createTimeUUID().toString();
					record.put("source_uuid", uuid);
					String storageURL = url +"/_doc/"+uuid; // the rest of the openKE application expects this format for the key

					if (record.has("locationCountryCode")) {
						record.put("locationCountryCode", record.getString("locationCountryCode").toUpperCase());
					}

					if (record.getJSONObject("Article").has("AuthorList")) {
						JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");

						for (int i=0; i < authors.length(); i++) {
							JSONObject author = authors.getJSONObject(i);
							if (author.has("AffiliationInfo") && author.getJSONArray("AffiliationInfo").length()>0) {
								JSONArray affiliationArray = author.getJSONArray("AffiliationInfo");
								for (int j=0; j <affiliationArray.length(); j++ ) {
									JSONObject locObj = affiliationArray.getJSONObject(j).optJSONObject("location");
									if (locObj != null) {
										try {
											locObj.put("country", locObj.getString("country").trim());
										}
										catch (org.json.JSONException ex) {
											; //ignore
										}
									}
								}
							}
						}
					}


					HttpResponse<JsonNode> jsonResponse = Unirest.put(storageURL)
							.header("accept", "application/json")
							.body(record)
			                .asJson();

					JSONObject esResult = jsonResponse.getBody().getObject();
					if (esResult.optBoolean("created",false) == false) {
						logger.log(Level.SEVERE, "Unable to store document in ElasticSearch (URL: "+storageURL+" ): "+ esResult.optString("error",""));
						logger.log(Level.SEVERE, esResult.toString(4));
					}


					logger.log(Level.INFO, "ElasticSearch - stored: "+f.getName());
				}
				catch(Exception e) {
					logger.log(Level.WARNING, "Unable to store record in ES: "+f.getName(), e);
				}
			}
		}
	}


	/**
	 * For the given location, download any data files that we do not currently have.
	 *
	 * @param location   Whhat location contains the files to download?  Must be ConfigurationType.PUBMEDIMPORTER_BASELINE  or ConfigurationType.PUBMEDIMPORTER_UPDATES
	 */
	public void downloadFiles(ConfigurationType location) {
		if (location != ConfigurationType.PUBMEDIMPORTER_BASELINE && location != ConfigurationType.PUBMEDIMPORTER_UPDATES) {
			logger.log(Level.SEVERE, "Invalid location passed, skipping download: "+location.getFullLabel());
		}
		logger.log(Level.INFO, "Start download files for " + location.getFullLabel());

		String localCacheDir = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.PUBMEDIMPORTER_BASEDIRECTORY) +
				               Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,location);

		try {
			File localCacheDirFileObject = new File(localCacheDir);
			localCacheDirFileObject.mkdirs();

		    FTPClient f = new FTPClient();
		    f.connect(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.PUBMEDIMPORTER_FTPSERVER));
		    f.login("anonymous", "user@ncsu.edu");
		    f.changeWorkingDirectory(Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,location));
		    FTPFile[] files = f.listFiles(".");
		    for (FTPFile file: files) {
		    	String outputLocation = localCacheDir + file.getName();

		    	if (outputLocation.endsWith(".xml.gz") == false) {  //I'm going to assume that not being able to open a file is a good enough check, skip readm and md5 hashses
		    		continue;
		    	}

		    	if (new File(outputLocation).exists()) {  // we've already downloaded the file, skip
		    		logger.log(Level.INFO, "File exists, skipping download: "+ file.getName());
		    		continue;
		    	}

		    	int bufferSize = 8 * 1024;
		    	OutputStream output = new BufferedOutputStream(
		    	                      new FileOutputStream(outputLocation),bufferSize);

            	logger.log(Level.INFO, "Starting download: "+ file.getName());
                boolean result = f.retrieveFile(file.getName(), output);
                if (result == false) {
                	logger.log(Level.SEVERE, "Unable to download: "+ file.getName());
                }
                output.close();
            	logger.log(Level.INFO, "Completed download: "+ file.getName());
		    }
		    f.disconnect();
		}
		catch (Exception e) {
			e.printStackTrace();
			//System.out.println(e);
		}
		logger.log(Level.INFO, "End download files: " + location.getFullLabel());
	}

	public void extractJSONRecordsWithKeywords(ConfigurationType location, String destination, String keywords[]) throws Exception {
		if (location != ConfigurationType.PUBMEDIMPORTER_BASELINE && location != ConfigurationType.PUBMEDIMPORTER_UPDATES) {
			logger.log(Level.SEVERE, "Invalid location passed, skipping download: "+location.getFullLabel());
		}
		String localCacheDir = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.PUBMEDIMPORTER_BASEDIRECTORY) +
	                           Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,location);



		//Executor parsingPool = Executors.newFixedThreadPool(1);

		for (File f: (new File(localCacheDir)).listFiles() ) {
			if (f.getName().endsWith("xml.gz") == false) {continue;}

			/*
			if (f.getName().compareTo("pubmed18n0941.xml.gz") < 0) {
				logger.log(Level.INFO, "skipping "+f);
				continue;
			}*/


			//parsingPool.execute(new Runnable() {
			//	@Override
			//	public void run() {
			logger.log(Level.INFO, "Extracting from "+f);
			JSONObject jo = null;
			try {
				jo = PubMedUtilities.convertGZippedXMLFile(f);
			}
			catch (Throwable t) {
				logger.log(Level.SEVERE, "Unable to convert XML file: "+f,t);
				return;
			}
			logger.log(Level.INFO, "converted to XML");

			JSONArray articleList = jo.getJSONArray("PubmedArticle");
			for (int i=0; i < articleList.length(); i++) {
				try {
					JSONObject article = articleList.getJSONObject(i);
					JSONObject refactoredArticle = PubMedUtilities.refactorCitation(article);

					if (recordHasKeyword(refactoredArticle, keywords)) {
						String pmid = refactoredArticle.getString("PMID");
						Files.write(Paths.get(destination + pmid + ".json"), refactoredArticle.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
						logger.log(Level.INFO, "Extracted: "+pmid);
					}
				}
				catch (Throwable t) {
					logger.log(Level.SEVERE, "Uncaught exception: "+ t.toString(),t);
				}
			}
			articleList = null; // hints to the garbage collector
			jo = null;
			System.gc();
			logger.log(Level.INFO, "completed "+f);
				//}
			//});
		}

	}



	/**
	 *
	 * @param record
	 * @param keywords array of keywords to search.  should be already lowercased
	 * @return
	 */
	public static boolean recordHasKeyword(JSONObject record, String[] keywords) {
		String title = JSONUtilities.getAsString(record, "Article.ArticleTitle","").toLowerCase();
		for (String keyword: keywords) {
			if (title.contains(keyword)) { return true; }
		}

		String abstractText = JSONUtilities.getAsString(record, "Article.Abstract.AbstractText","").toLowerCase();
		for (String keyword: keywords) {
			if (abstractText.contains(keyword)) { return true; }
		}

		JSONArray minorKeywords = record.optJSONArray("keywordMinor");
		if (minorKeywords != null) {
			for (int i=0; i< minorKeywords.length(); i++) {
				String docKeyword = minorKeywords.getString(i).toLowerCase();
				for (String keyword: keywords) {
					if (docKeyword.contains(keyword)) { return true; }
				}
			}
		}

		JSONArray majorKeywords = record.optJSONArray("keywordMajor");
		if (majorKeywords != null) {
			for (int i=0; i< majorKeywords.length(); i++) {
				String docKeyword = majorKeywords.getString(i).toLowerCase();
				for (String keyword: keywords) {
					if (docKeyword.contains(keyword)) { return true; }
				}
			}
		}


		JSONArray meshArray     = record.optJSONArray("MeshHeading");
		if (meshArray != null) {
			for (int i=0; i< meshArray.length(); i++) {
				JSONObject meshObject = meshArray.getJSONObject(i);
				String descriptor = meshObject.getJSONObject("DescriptorName").getString("content").toLowerCase();
				for (String keyword: keywords) {
					if (descriptor.contains(keyword)) { return true; }
				}

				// now check the qualifiers
				JSONArray qualifierArray = meshObject.optJSONArray("QualifierName");
				if (qualifierArray != null) {
					for (int j=0; j < qualifierArray.length(); j++) {
						JSONObject qualObj = qualifierArray.getJSONObject(j);
						String content = qualObj.getString("content").toLowerCase();
						for (String keyword: keywords) {
							if (content.contains(keyword)) { return true; }
						}
					}
				}
			}
		}

		return false;
	}


	public static final JSONObject loadRecord(java.io.File recordFile) throws JSONException, IOException {
		JSONObject record = new JSONObject(  new String(Files.readAllBytes(recordFile.toPath()),StandardCharsets.UTF_8));
		return record;
	}

	public static final void writeRecord(java.io.File recordFile, JSONObject record) throws IOException {
		Files.write(recordFile.toPath(), record.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

}
