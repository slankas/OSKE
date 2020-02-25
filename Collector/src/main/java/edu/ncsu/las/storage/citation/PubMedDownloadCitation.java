package edu.ncsu.las.storage.citation;


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.mashape.unirest.http.HttpResponse;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;



public class PubMedDownloadCitation {
	private static Logger logger =Logger.getLogger(PubMedUtilities.class.getName());
	
	public static JSONObject download(String pubmedID) throws IOException, ParserConfigurationException, SAXException {
		
   		try {
			GetRequest response = Unirest.get("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id="+pubmedID);
			HttpResponse<String> text = response.asString();
			
			JSONObject jo = PubMedUtilities.convertXML(text.getRawBody());
			
			JSONArray articleList = jo.getJSONArray("PubmedArticle");
			if (articleList.length() == 0 ) { 
				return null;
			}
			JSONObject article = articleList.getJSONObject(0); 
			JSONObject refactoredArticle = PubMedUtilities.refactorCitation(article);
			
			return refactoredArticle;

   		}
   		catch (UnirestException ure) {
   			logger.log(Level.WARNING, "Unable to process remote request", ure);
			return new JSONObject();
   		}
	}	
	
	public static void downloadAllRelatedCitations() throws IOException, ParserConfigurationException, SAXException {
		
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
		HashSet<String> uniquePubMedReferences = new HashSet<String>();
		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			JSONObject record = PubMedProcessor.loadRecord(f);
			
			if (record.has("references") == false ) {continue;}
			
			JSONArray references = record.getJSONArray("references");
			for (int i=0;i< references.length();i++) {
				JSONObject jo = references.getJSONObject(i);
				uniquePubMedReferences.add(jo.getString("PMID"));
			}
			
		}
		System.out.println(uniquePubMedReferences.size());
			
		//String recordNumber = f.getName().substring(0,f.getName().indexOf('.'));
		String citationRecordLocation = "C:\\pubmed\\pubmed\\citationRecords\\";
		//ClassLoader loader = PubMedDownloadCitation.class.getClassLoader();
		//System.out.println(org.json.JSONObject.class.getProtectionDomain().getCodeSource().getLocation());
        //System.out.println(org.json.XML.class.getProtectionDomain().getCodeSource().getLocation());
		
	
		int count = 0;
		for (String pmid: uniquePubMedReferences) {
			count++;
			//if (count %100 ==0) {System.out.println(count+"/"+uniquePubMedReferences.size()); }

			if ((new java.io.File(citationRecordLocation+pmid+".json")).exists()) {continue;}
			
			JSONObject record = PubMedDownloadCitation.download(pmid);
			if (record == null) {
				System.out.println("Not present: "+pmid);
				continue;
			}
			PubMedProcessor.writeRecord(new java.io.File(citationRecordLocation+pmid+".json"), record);
		}
		
		System.out.println("Complete!");
		
	}
	
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		downloadAllRelatedCitations();
	}
	
}
