package edu.ncsu.las.storage.citation;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ncsu.las.util.Serializer;
import edu.ncsu.las.util.json.JSONUtilities;

/**
 * This class adapts the work from "A new method for automatically constructing domain-oriented term taxonomy based upon
 * weighted word-occurence analysis" by Li, Sun, and Soergel.   DOI 10.1007/s11192-015-1571-0
 * 
 *
 */
public class TermTaxonomy implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger logger =Logger.getLogger(TermTaxonomy.class.getName());

	public static class AppearanceCounts implements Serializable{
		private static final long serialVersionUID = 1L;
		int numAbstract  = 0; // number of times a keyphrase appears in a document's abstract
		int numFullText  = 0; // number of times a keyphrase appears in a document's full text.  This may be the title / abstract / etc.
		int numTitle     = 0; // number of times a keyphrase appears in a document's title
		int numKeyword   = 0; // number of times a keyphrase appears in a document's author keyword list
		double weightTwo = 0.0;
	}
	
	public static class KeyphraseMetrics implements Serializable{
		private static final long serialVersionUID = 1L;
		int documentFrequncy = 0;  // how many documents does a keyword appear?
		double weightOne     = 0.0;
		HashMap<String, AppearanceCounts> documentCount = new HashMap<String, AppearanceCounts>();  // key=DocumentID a keyphrase was not in a document, this is zero
	}
	
	private ArrayList<String> uniqueDocumentIDs = new ArrayList<String>();
	private HashSet<String> uniqueKeyphrases = new HashSet<String>();
	private HashMap<String, KeyphraseMetrics> counts = new HashMap<String, KeyphraseMetrics>();    //key = keyphrase
	private HashMap<String, Double> wordWeights = new HashMap<String,Double>();  // indexed by keyphrase:documentID
	private HashMap<String, Double> relationWeights = new HashMap<String,Double>();  // indexed by keyphrase_i:keyphrase_j
	private HashMap<String, Double> relationWeightsHierarchy  = new HashMap<String,Double>();
	private TermTaxonomy() {
		
	}
	
	/**
	 * Determines the list of unique keyphrases and documents.  Also established the counts Hashmap 
	 * That will be completed in countKeyphraseAppearances
	 * 
	 * @param directory
	 * @param keywordField
	 */
	private void determineUniqueKeyphrases(java.io.File directory, String keywordField) {
		int files =0;
		for (java.io.File f: directory.listFiles()) {
			files++;
			try {
				JSONObject record = PubMedProcessor.loadRecord(f);
				if (record.has("text")) { 
					JSONArray keyphrases = record.optJSONArray(keywordField);  if(keyphrases == null) { keyphrases = new JSONArray(); }
					uniqueDocumentIDs.add(record.getString("PMID"));
					keyphrases.toList().stream().forEach(e -> { this.uniqueKeyphrases.add(e.toString().toLowerCase());});
				}
			} catch (JSONException | IOException e) {
				logger.log(Level.WARNING, "Unable to read record file("+f.getName()+"), ignoring: "+ e.toString());
			}
			//if (files > 300) break;
		}
		
		for (String keyphrase: this.uniqueKeyphrases) {
			counts.put(keyphrase, new KeyphraseMetrics());
		}
	}	

	private void countKeyphraseAppearances(File directory) {
		int files =0;
		for (java.io.File f: directory.listFiles()) {
			files++;
			System.out.println(f.toString());
			try {
				JSONObject record = PubMedProcessor.loadRecord(f);
				if (record.has("text") == false) { continue; }
				
				String recordID = record.getString("PMID");
				
				// setup the document fields.  all analysis performed in lower case
				String title        = record.optString("title","").toLowerCase();
				String abstractText = JSONUtilities.getAsString(record, "Article.Abstract.AbstractText", "").toLowerCase();
				JSONArray authorKeywordArray = record.optJSONArray("keywordMinor");  if(authorKeywordArray == null) { authorKeywordArray = new JSONArray(); }
				HashSet<String> authorKeywordSet = new HashSet<String>();
				for (int i=0; i < authorKeywordArray.length(); i++) { authorKeywordSet.add(authorKeywordArray.getString(i).toLowerCase());}
				String fullText         = record.getString("text").toLowerCase();
				
				for (String keyPhrase: this.uniqueKeyphrases) {
					AppearanceCounts ac = new AppearanceCounts();
					ac.numTitle    = StringUtils.countMatches(title, keyPhrase);
					ac.numAbstract = StringUtils.countMatches(abstractText, keyPhrase);
					ac.numKeyword  = authorKeywordSet.contains(keyPhrase) ? 1: 0;
					ac.numFullText = StringUtils.countMatches(fullText, keyPhrase);
					
					int inDocument = (ac.numAbstract + ac.numFullText + ac.numKeyword + ac.numTitle) >0 ? 1 : 0;
					if (inDocument == 1) {
						// don't process metrics if the keyPhrase wasn't in this document
						ac.weightTwo   = ac.numAbstract*2 + ac.numFullText*1 + ac.numKeyword*4 + ac.numTitle*8;
						
						KeyphraseMetrics km = counts.get(keyPhrase);
						km.documentFrequncy += inDocument;
						km.documentCount.put(recordID, ac);
					}
				}
			} catch (JSONException | IOException e) {
				logger.log(Level.WARNING, "Unable to read record file("+f.getName()+"), ignoring: "+ e.toString());
			}
			//if (files > 300) break;
		}
	}
	
	private void computeWeightOneValues() {
		double numDocuments = uniqueDocumentIDs.size();

		// compute weightOne
		for (String keyphrase: uniqueKeyphrases) {
			KeyphraseMetrics km = counts.get(keyphrase);
			km.weightOne = Math.log(numDocuments / km.documentFrequncy);
		}
		
	}
	
	/**
	 * 
	 */
	public void computeWordWeights() {
		double maxWeightValue = Double.MIN_VALUE;
		
		for(String keyphrase: counts.keySet()) {
			KeyphraseMetrics km = counts.get(keyphrase);
			for (String docID: km.documentCount.keySet()) {
				String wordWeightKey = keyphrase+":"+docID;
				double value = km.weightOne * km.documentCount.get(docID).weightTwo;
				wordWeights.put(wordWeightKey, value);
				maxWeightValue = Math.max(maxWeightValue, value);
			}
		}
		
		logger.log(Level.INFO, "Number of Word Weights: "+ wordWeights.size());
		
		// normalize against the maximum value
		for (String key: wordWeights.keySet()) {
			wordWeights.put(key, wordWeights.get(key)/maxWeightValue);
		}
		
		logger.log(Level.INFO, "Word Weights normalized");
	}
	
	public void computeRelationWeights() {
		//relationWeights
		for (String keyphrase_i: uniqueKeyphrases) {
			for (String keyphrase_j: uniqueKeyphrases) {
				if (keyphrase_i.equals(keyphrase_j)) { continue; }
				 
				double numerator = 0;   //sum of (WordWeight_i * WordWeight_j) for all docs
				double denominator = 0; //sum of WordWeight_i for all docs
				
				for (String docID: uniqueDocumentIDs) {
					String wordWeight_i_key = keyphrase_i+":"+docID;
					String wordWeight_j_key = keyphrase_j+":"+docID;
					
					numerator += (wordWeights.getOrDefault(wordWeight_i_key, 0.0) * wordWeights.getOrDefault(wordWeight_j_key, 0.0));
					denominator += wordWeights.getOrDefault(wordWeight_i_key, 0.0);
				}
				double relationWeight = numerator / denominator;
				if (relationWeight > 0.0) {
					relationWeights.put(keyphrase_i+":"+keyphrase_j, relationWeight);
				}
			}
		}
		logger.log(Level.INFO, "relation weights computed");
	}
	
	public void computeHierarchy() {
		for (String keyphrase_i: uniqueKeyphrases) {
			for (String keyphrase_j: uniqueKeyphrases) {
				if (keyphrase_i.equals(keyphrase_j)) { continue; }
				String key1 = keyphrase_i +":" + keyphrase_j;
				String key2 = keyphrase_j +":" + keyphrase_i;
				
				double value1 = this.relationWeights.getOrDefault(key1, Double.MIN_VALUE);
				double value2 = this.relationWeights.getOrDefault(key2, Double.MIN_VALUE);
				if (value1 == Double.MIN_VALUE || value2 == Double.MIN_VALUE) { continue;}
				if (value1 > value2) {
					relationWeightsHierarchy.put(key1,value1);
				}
				else if (value2 > value1) {
					relationWeightsHierarchy.put(key2,value2);
				}
				else {
					relationWeightsHierarchy.put(key1,value1);
					relationWeightsHierarchy.put(key2,value2);
				}
			}
		}
		logger.log(Level.INFO, "relation weights computed");
	}	
	
	public static TermTaxonomy analyze(java.io.File directory)  {
		logger.log(Level.INFO, "analyzing "+directory.toString());
		TermTaxonomy result = new TermTaxonomy();
		
		String keywordField = "keywordMinor"; // "keyphrases"

		logger.log(Level.INFO, "finding unique keyphrases: "+keywordField);
		result.determineUniqueKeyphrases(directory,  keywordField);
		logger.log(Level.INFO, "counting appearances");
		result.countKeyphraseAppearances(directory);
		logger.log(Level.INFO, "computing weights");
		result.computeWeightOneValues();  // weightTwo metrics are computed in countKeyphrase appearances.
		
		logger.log(Level.INFO, "Initial analysis complete");
		System.out.println(result.uniqueKeyphrases.size());
		
		
		return result;
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//TermTaxonomy tt = TermTaxonomy.analyze(new java.io.File("C:\\pubmed\\pubmed\\extractedRecords"));
		
		//byte[] data = Serializer.serialize(tt);
		//Files.write(Paths.get("c:\\pubmed\\taxonomy_terms.ser"), data);
		
		
		byte[] data = Files.readAllBytes(Paths.get("c:\\pubmed\\taxonomy_terms.ser"));
		TermTaxonomy tt = (TermTaxonomy) Serializer.deserialize(data);
		
		tt.computeWordWeights();
		
		System.out.println(tt.uniqueKeyphrases.size());
		for (String keyphrase: tt.uniqueKeyphrases) {
			System.out.println(keyphrase+"\t"+ tt.counts.get(keyphrase).documentFrequncy+"\t"+tt.counts.get(keyphrase).weightOne);
		}
		
		tt.computeRelationWeights();
		tt.computeHierarchy();
		

		System.out.println("===================");
		java.util.ArrayList<String> pairs = new java.util.ArrayList<>(tt.relationWeightsHierarchy.keySet());
		
		pairs.sort(new Comparator<String>() {

			@Override
			public int compare(String p1, String p2) {
				return Double.compare(tt.relationWeightsHierarchy.get(p2),tt.relationWeightsHierarchy.get(p1));
			}
		});
		
		for (String key: pairs) {
			System.out.println(key+":"+tt.relationWeightsHierarchy.get(key));
		}
		
	}
}
