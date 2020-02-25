package edu.ncsu.las.dictionary.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class DictionaryManager {
	private static Logger logger =Logger.getLogger(DictionaryManager.class.getName());

	private com.google.common.cache.Cache<String,JSONObject> _dictionaryCache;
	private IRAMDictionary _wordNetDictionary;
	private edu.mit.jwi.morph.WordnetStemmer _stemmer;
	
	public static void main(String[] args) throws IOException {
		DictionaryManager dm = new DictionaryManager("./wordnet",10000);
		java.util.List<DictionaryResult> results = dm.lookupWordNet("bank");
		for (DictionaryResult dr: results) {
			System.out.println(dr.toString());
		}
	}
	
	public DictionaryManager(String wordNetDirectory, int cacheSize) throws IOException {
		File f = new File(wordNetDirectory);
		System.out.println(f.getAbsolutePath());
		_wordNetDictionary = new RAMDictionary(f);
		_wordNetDictionary.open();
		_stemmer = new edu.mit.jwi.morph.WordnetStemmer(_wordNetDictionary);
		_dictionaryCache = com.google.common.cache.CacheBuilder.newBuilder()
				                   .maximumSize(cacheSize)
				                   .recordStats()
				                   .expireAfterAccess(10000, TimeUnit.DAYS)   //basically, we want to keep things, in cache, but using LRU access as the policy
				                   .build();
	}
	
	public List<DictionaryResult> lookupWordNet(String term) {
		java.util.ArrayList<DictionaryResult> results = new java.util.ArrayList<DictionaryResult>(); 
		for (POS pos: POS.values()) {			
			for (String stemmedTerm: _stemmer.findStems(term, pos)) {
				IIndexWord iiw = _wordNetDictionary.getIndexWord(stemmedTerm, pos);
				if (iiw == null) { 
					logger.log(Level.INFO, "Stemmed word not in wordnet: "+ stemmedTerm);
					continue; 
				} 
				for (IWordID wi: iiw.getWordIDs()) {
					IWord word = _wordNetDictionary.getWord(wi);
					String[] glossEntries = word.getSynset().getGloss().split(";");
					String result = null;
					String sampleSentence = "";
					List<String> synonyms = new ArrayList<String>();
					for (String entry: glossEntries) {
						entry = entry.trim();
						if (entry.startsWith("\"") == false) { // not an example sentence
							if (result == null) { result = entry; }
							else { result = result +"; " + entry; }
						}
						else {
							sampleSentence = entry.substring(1,entry.length()-1);
						}
					}
					List<IWord> relatedWords = word.getSynset().getWords();
					for (IWord rw: relatedWords) {
						if (rw.getLemma().contentEquals(term) == false) {
							synonyms.add(rw.getLemma().replace('_', ' '));
						}
					}
					
					DictionaryResult dr = new DictionaryResult("wordnet", stemmedTerm, pos.toString(), result, sampleSentence, synonyms);
					results.add(dr);
				}
			}
		}
		return results;
	}
	

	public synchronized JSONObject lookup(String term) {
		
		JSONObject result = _dictionaryCache.getIfPresent(term);
		
		if (result == null) {
			java.util.List<DictionaryResult> results = this.lookupWordNet(term);
			JSONArray wordNetResults = new JSONArray();
			results.stream().forEach(i -> { wordNetResults.put(i.toJSONObject());});
			
			result = new JSONObject().put("wordnet", wordNetResults);
			_dictionaryCache.put(term, result);
		}
		return result;
	}
	
	public JSONObject generateStatistics() {
		JSONArray providerStats = new JSONArray();

		com.google.common.cache.CacheStats cacheStats = _dictionaryCache.stats();
		JSONObject cStats = new JSONObject().put("averageLoadPenalty",   cacheStats.averageLoadPenalty())
				                            .put("evictionCount",        cacheStats.evictionCount())
				                            .put("hitCount",             cacheStats.hitCount())
				                            .put("hitRate",              cacheStats.hitRate())
				                            .put("missCount",            cacheStats.missCount())
				                            .put("missRate",             cacheStats.missRate())
				                            .put("requestCount",         cacheStats.requestCount())
				                            .put("totalLoadTimeNanoSec", cacheStats.totalLoadTime())
				                            .put("size",                 _dictionaryCache.size());
		
		Runtime runtime = Runtime.getRuntime();
		JSONObject vmStats = new JSONObject().put("usedMemory",  (runtime.totalMemory() - runtime.freeMemory()))
				                             .put("freeMemory",  runtime.freeMemory())
				                             .put("totalMemory", runtime.totalMemory())
				                             .put("maxMemory",   runtime.maxMemory());
				
		
		JSONObject result = new JSONObject().put("providers", providerStats)
				                            .put("cacheStatistics", cStats )
				                            .put("memory", vmStats );
		return result;
	}	
}
