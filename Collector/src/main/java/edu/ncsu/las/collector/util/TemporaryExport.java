package edu.ncsu.las.collector.util;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.ncsu.las.storage.citation.PubMedProcessor;
import edu.ncsu.las.util.crypto.SHA256;


public class TemporaryExport {
	private static Logger logger = Logger.getLogger(TemporaryExport.class.getName());

	public static void searchQueryForAllResultsUsingScroll(String elasticURL, String index, String destDirectory, int maxRecords) throws IOException {

		String fullURL = elasticURL + index+ "/_search?scroll=1m";
		String scrollURL =  elasticURL + "_search/scroll";

		long fromValue = 0;
		long totalHits = 1;
		try {
			JSONObject fullQuery = new JSONObject()
					.put("size", 1000)
                    .put("query", new JSONObject("{\"match_all\": {}}"))
                    .put("sort", new JSONArray().put("_doc"));
			HttpResponse<JsonNode> jsonResponse = Unirest.post(fullURL)
					  .header("accept", "application/json")
					  .body(fullQuery)
					  .asJson();
			String scrollID = jsonResponse.getBody().getObject().getString("_scroll_id");

			searchLoop:
			while (fromValue < totalHits ) {

				JSONObject esResult = jsonResponse.getBody().getObject();

				totalHits = esResult.getJSONObject("hits").getJSONObject("total").getLong("value");
				JSONArray hits =  esResult.getJSONObject("hits").getJSONArray("hits");

				for (int i=0;i<hits.length();i++) {
					String destFile = destDirectory+hits.getJSONObject(i).getString("_id");
					PubMedProcessor.writeRecord(new java.io.File(destFile), hits.getJSONObject(i).getJSONObject("_source"));
					//System.out.println(destFile);
					//System.out.println(hits.getJSONObject(i).getJSONObject("_source").toString(4));
					fromValue++;
					if (maxRecords > 0 && fromValue >= maxRecords) {
						break searchLoop;
					}
				}

				if (fromValue < totalHits) {
					JSONObject scrollQuery = new JSONObject().put("scroll", "1m")
	                        .put("scroll_id", scrollID);
					jsonResponse = Unirest.post(scrollURL)
							.header("accept", "application/json")
							.body(scrollQuery)
							.asJson();
				}
			}
			jsonResponse = Unirest.delete(scrollURL)
					.header("accept", "application/json")
					.body(new JSONObject().put("scroll_id", new JSONArray().put(scrollID)))
					.asJson();
			//System.out.println(fromValue);
		}
		catch (UnirestException ure) {
			logger.log(Level.SEVERE,"Unirest Exception: ",ure);
			return;
		}
	}


	public static void processImageFiles(String directory, String destinationURL) throws JSONException, IOException, UnirestException {
		java.io.File dir = new java.io.File(directory);

		for (java.io.File f: dir.listFiles()) {
			JSONObject record = PubMedProcessor.loadRecord(f);

			record.put("originalLocation", SHA256.hashStringToBase64(record.getString("originalLocation")));
			record.put("sourcePage", SHA256.hashStringToBase64(record.getString("sourcePage")));
			String id = record.getString("id");

			HttpResponse<JsonNode> jsonResponse = Unirest.put(destinationURL+id+"/_create")
					.header("accept", "application/json")
					.body(record)
					.asJson();

			//System.out.println (record.toString(4));

			//System.exit(0);
		}

	}

	private static final String user_label = "TNA Username: \\n\\t";
	private static final String ad_label   = "Ad Link: \\n\\t";

	public static void processGenFiles(String directory, String destinationURL) throws JSONException, IOException, UnirestException, InterruptedException {
		java.io.File dir = new java.io.File(directory);

		String[] postfields = { "reviewedUserAdLink","reviewedUserID", "reviewedUserName", "reply_parent_id",  "originalLocation",  "user_name" , "self_id", "forum_url", "thread_id", "id", "user_profile_metadata_id",  "user_id",   "user_profile_url","extracted_text"};
		String[] userfields = {    "originalLocation", "user_name", "user_id", "biography","a_few_words_about_myself","profile_image_link","home_page","phone_number","id","extracted_text"};
		String[] userArrays = { "friends_ids", "friends_profiles",  "imageLinks",  "friends_user_names","past_email_address", "email" };

		int count =0;
		for (java.io.File f: dir.listFiles()) {
			count++;
			if (count < 343000) {continue;}
			JSONObject record = PubMedProcessor.loadRecord(f);

			//System.out.println (record.toString(4));
			String type ="post/";
			if (record.has("join_date") || record.has("friends_user_names") ) { type = "user/";}


			if (type.equals("post/")) {

				String bodyText = record.getJSONObject("raw").getString("body_text");
				if (bodyText.contains("TNA Username") | bodyText.contains("Ad Link")) {
					bodyText = bodyText.replace("\t", "\\t").replace("\n", "\\n");
					//System.out.println (bodyText);

					int indexOFUserLabel = bodyText.indexOf(user_label);
					if (indexOFUserLabel >= 0) {
						String reviewedUserName = bodyText.substring(indexOFUserLabel+user_label.length(), bodyText.indexOf("\\n", indexOFUserLabel+user_label.length()));
						record.put("reviewedUserName", reviewedUserName);
						//System.out.println(reviewedUserName);
					}

					int indexOfAdLink = bodyText.indexOf(ad_label);
					if (indexOfAdLink >= 0) {
						String reviewedUserAdLink = bodyText.substring(indexOfAdLink+ad_label.length(), bodyText.indexOf("\\n", indexOfAdLink+ad_label.length()));
						//System.out.println(reviewedUserAdLink);
						if (reviewedUserAdLink.startsWith("https://www.tnaboard.com/member.php")) {
							try {
								reviewedUserAdLink = reviewedUserAdLink.substring(0, reviewedUserAdLink.indexOf("-"));
								record.put("reviewedUserAdLink", reviewedUserAdLink);
								String reviewedUserID = reviewedUserAdLink.substring(reviewedUserAdLink.indexOf("?")+1).trim();
								record.put("reviewedUserID", reviewedUserID);
							}
							catch (java.lang.StringIndexOutOfBoundsException e) {
								;//System.out.println(reviewedUserAdLink);
							}
						}
					}


				}


				for (String field: postfields) {
					try {
						record.put(field, SHA256.hashStringToBase64(record.getString(field)));
					}
					catch (org.json.JSONException e) {
						; // ignore missing fields
					}
				}

				record.put("title","");
				//record.put("raw", "");

				try {
					JSONArray imageLinksNew = new JSONArray();
					JSONArray imageLinks = record.getJSONArray("imageLinks");
					for (int i=0; i< imageLinks.length(); i++) {
						imageLinksNew.put(SHA256.hashStringToBase64( imageLinks.getString(i) ));
					}
					record.put("imageLinks", imageLinksNew);
				}
				catch (org.json.JSONException e) {
					//System.err.println("ignoring imageLinks not present");
				}

				//System.out.println (record.toString(4));

				//System.exit(0);
			}
			else {
				try {
					String originalLocation =record.getString("originalLocation");
					originalLocation = originalLocation.substring(0,originalLocation.indexOf("&"));
					record.put("originalLocation", originalLocation);
				}
				catch(org.json.JSONException e) {
					;
				}
				for (String field: userfields) {
					try {
						record.put(field, SHA256.hashStringToBase64(record.getString(field)));
					}
					catch (org.json.JSONException e) {
						; // ignore missing fields
					}
				}

				for (String array: userArrays) {
					try {
						JSONArray newArray = new JSONArray();
						JSONArray oldArray = record.getJSONArray(array);
						for (int i=0; i< oldArray.length(); i++) {
							newArray.put(SHA256.hashStringToBase64( oldArray.getString(i) ));
						}
						record.put(array, newArray);
					}
					catch (org.json.JSONException e) {
						//System.err.println("ignoring "+array+" not present");
					}
				}

				record.put("user_title","");
				record.put("raw", new JSONObject());
				record.put("a_few_words_about_myself","");
				record.put("other_attributes", "");
				record.put("regex_fields", "");

			}



			String id = record.getString("id");
			//System.out.println (record.toString(4));  //.getJSONObject("raw")

			try {
				HttpResponse<String> jsonResponse = Unirest.put(destinationURL+type+id+"/_create")
						.header("accept", "application/json")
						.body(record.toString()).asString();
				//				.asJson();

				System.out.println(jsonResponse.getBody());

			}
			catch ( com.mashape.unirest.http.exceptions.UnirestException uri) {
				System.out.println(uri.toString());
				TimeUnit.SECONDS.sleep(10);
				try {
					HttpResponse<String> jsonResponse = Unirest.put(destinationURL+type+id+"/_create")
							.header("accept", "application/json")
							.body(record.toString()).asString();
				}
				catch ( com.mashape.unirest.http.exceptions.UnirestException uri2) {
					System.out.println(f);
				}
			}

		}

	}


	public static void main(String[] args) throws IOException, JSONException, UnirestException, InterruptedException {
    // update as neessary to export data
		String sourceDirectoryGen = "";
		String destURLGen = "";
		processGenFiles(sourceDirectoryGen,destURLGen);

	}

}
