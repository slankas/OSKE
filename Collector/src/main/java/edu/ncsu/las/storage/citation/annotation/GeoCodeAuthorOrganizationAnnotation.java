package edu.ncsu.las.storage.citation.annotation;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.ncsu.las.storage.citation.PubMedProcessor;
import edu.ncsu.las.util.json.JSONUtilities;


public class GeoCodeAuthorOrganizationAnnotation extends Annotation {
	private static final String[] EMPTY_ARRAY = {};

	public String getName() {	return "Author Organization GeoCode";   	}
	public String getCode() {	return "authorOrganizationGeoCode" ; 	}
	public String getDescription() { return "Modifies the author records within article.AuthorList to geo-code the author's affiliation"; }
	public int getOrder() {	return 1; 	}
	public String[] getRequiredAnnotations() { return EMPTY_ARRAY; }
	@Override
	public JSONObject getSchema() {
		return null;
	}

	@Override
	public void doProcessing(JSONObject record, String htmlFileName, String pdfFileName) {
		Object o = JSONUtilities.get(record, "Article.AuthorList.Author");
		if (o == null || o instanceof JSONArray == false) {
			record.put(this.getCode(),"errored: no array at Article.AuthorList.Author");
			logger.log(Level.INFO, "Author affiliations geo-code failed (no author array): "+record.getString("PMID"));
			return;
		}
		JSONArray authors = (JSONArray) o;
		for (int i=0;i < authors.length();i++) {
			JSONArray newAffiations = new JSONArray();
			JSONArray affiliations = authors.getJSONObject(i).optJSONArray("AffiliationInfo");
			if (affiliations == null) {continue;}
			for (int j=0;j < affiliations.length(); j++) {
				JSONObject affiliationObject = affiliations.getJSONObject(j);
				String affiliation = affiliationObject.getString("Affiliation");


				Matcher emailMatcher = PubMedProcessor.REGEX_EMAIL_ADDRESS.matcher(affiliation);
				StringBuffer affiliationSB = new StringBuffer();
				while (emailMatcher.find()) {
				    String email = emailMatcher.group();
				    authors.getJSONObject(i).put("email",email);
				   // System.out.println(email);
				    emailMatcher.appendReplacement(affiliationSB, Matcher.quoteReplacement(""));
				}
				emailMatcher.appendTail(affiliationSB);
				affiliation = affiliationSB.toString();

				Matcher urlMatcher = PubMedProcessor.REGEX_URL.matcher(affiliation);
				StringBuffer urlSB = new StringBuffer();
				while (urlMatcher.find()) {
				    String url = urlMatcher.group();
				    authors.getJSONObject(i).put("website", url);

				    //System.out.println(url);
				    urlMatcher.appendReplacement(urlSB, Matcher.quoteReplacement(""));
				}
				urlMatcher.appendTail(urlSB);
				affiliation = urlSB.toString();

				affiliation = affiliation.replaceAll("^\\W+", "");  // gets rid of bad characters at the start of an affiliation
				affiliation = affiliation.replaceAll("\\b[A-Za-z ]+?: [0-9 +-\\\\(\\\\)]+", "");  // gets rid of "tel: +133424332

				affiliation = affiliation.replaceAll("\\b[A-Za-z ]+?:", "");
				affiliation = affiliation.replaceAll("\\W+$", "");  // gets rid of bad characters at the start of an affiliation

				String [] affiliationList = { affiliation };
				if (affiliation.contains(";")) {
					affiliationList = affiliation.split(";");
					for (int alIndex=0; alIndex < affiliationList.length; alIndex++) {
						affiliationList[alIndex] = affiliationList[alIndex].trim();
					}
				}

				for (String a: affiliationList) {
					JSONObject newAffiliation = new JSONObject().put("Affiliation", a);
					JSONObject geoLocation = searchForGeoLocationWithCommaParsing(a);
					if (geoLocation == null) {
						System.out.println("GEO NOT FOUND: " +a);
					}
					else {
						newAffiliation.put("location", geoLocation);
					}
					newAffiations.put(newAffiliation);

				}

			}
			authors.getJSONObject(i).put("AffiliationInfo", newAffiations);
		}



		record.put(this.getCode(),"processed");

		logger.log(Level.INFO, "Author affiliations geo-code: "+record.getString("PMID"));

	}

	private static  JSONObject searchForGeoLocationWithCommaParsing(String name) {
		name = name.trim();
		while (name.length() > 0) {
			JSONObject result = searchForGeoLocation(name);

			if (result != null) { return result; }
			else {
				int pos = name.indexOf(',');
				if (pos == -1) {
					return null;
				} else {
					name = name.substring(pos + 1).trim();
				}
			}
		}
		return null;
	}

	private static JSONObject searchForGeoLocation(String name)  {
		//String url = (String) JSONUtilities.get(_config, "geoLocationURL", "http://localhost:9002/geo/v1/geoCode?location=");
		String url = "http://serverNameOrIP:9002/geo/v1/geoCode?location="; // TODO: get this from configuration

		try {
			String encName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString());
			url = url + encName;
			HttpResponse<JsonNode> json = Unirest.get(url).asJson();
			JSONObject object = json.getBody().getObject();
			if (object.has("message") && object.getString("message").equals("no results found")) {
				return null;
			}
			object.put("matchedText", name);
			return object;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable peform geocoding: "+url);
		}

		return null;
	}


	public static void main(String args[]) throws JSONException, IOException {
		String recordNumber = "28186905";
		//recordNumber = "28733542";
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\"+recordNumber+".json";

		JSONObject record = PubMedProcessor.loadRecord(new java.io.File(jsonRecordLocation));

		String pdfFileLocation  = "C:\\pubmed\\pubmed\\extractPDFFiles\\" + recordNumber + ".pdf";
		(new GeoCodeAuthorOrganizationAnnotation()).doProcessing(record,"",pdfFileLocation);

		System.out.println(record.getString((new GeoCodeAuthorOrganizationAnnotation()).getCode()));
	}




}
