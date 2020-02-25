package edu.ncsu.las.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.ncsu.las.collector.util.ApplicationConstants;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.collector.type.FileStorageAreaType;
import edu.ncsu.las.model.nlp.PorterStemmer;
import edu.ncsu.las.model.nlp.StopWords;
import edu.ncsu.las.model.nlp.TermVectorScore;
import edu.ncsu.las.rest.collector.AbstractRESTController;



/**
 * 
 * 
 */
@Controller
public class WordCloudController extends AbstractController {

	private static Logger logger = Logger.getLogger(AbstractRESTController.class.getName());

	@RequestMapping(value = "{domain}/wordCloud", method = RequestMethod.GET)
	public String passThroughToElasticSearchTermvector(HttpServletRequest httpRequest, HttpServletResponse response,
				@RequestParam(value = "type", required = true) String documentType, 
				@RequestParam(value = "uuid", required = true) String uuid,
				@RequestParam(value = "storageArea", required = true) String storageArea,
				@PathVariable("domain") String domainStr)
						throws Exception {
		logger.log(Level.INFO,"Search Controller: Word cloud");
		this.validateUserAndSetPageAttributes(httpRequest);
		this.instrumentAPI("edu.ncsu.las.rest.collector.WordCloudController.getWordCloudData", new JSONObject().put("documentID",uuid), System.currentTimeMillis(), null, httpRequest,domainStr);

		//TODO: add authorization
		
		FileStorageAreaType fsat;
		if      (storageArea.equals("normal"))  { fsat = FileStorageAreaType.REGULAR; }
		else if (storageArea.equals("sandbox")) { fsat = FileStorageAreaType.SANDBOX; }
		else if (storageArea.equals("archive")) { fsat = FileStorageAreaType.ARCHIVE; }
		else {
			throw new ValidationException("Invalid storage area");
		}
		
		String url = Configuration.getConfigurationProperty(domainStr, fsat, ConfigurationType.ELASTIC_STOREJSON_LOCATION) + documentType + "/" + uuid + "/_termvectors";
		HttpResponse<JsonNode> jsonResponse = Unirest.post(url)
				.header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body(getTermVectorQuery()).asJson();
		JSONObject result = jsonResponse.getBody().getObject();
		
		ArrayList<TermVectorScore> lemmatizedResult = scoreResultsWithStemmedWords(result);

		ObjectMapper mapper = new ObjectMapper();

		mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.setDateFormat(new SimpleDateFormat(ApplicationConstants.DATE_FORMAT));

		String resultString = mapper.writeValueAsString(lemmatizedResult);

		httpRequest.setAttribute("words", resultString);
		
	

		logger.log(Level.INFO,"Words: " + resultString);

		return "wordCloud";		
	}	
	
	private JSONObject getTermVectorQuery() {
		JSONObject queryData = new JSONObject().put("fields", new String[] { "text" } )
		                                       .put("offsets", false )
		                                       .put("payloads", false )
		                                       .put("positions", false )
		                                       .put("term_statistics", true )
		                                       .put("field_statistics", true );
		
		JSONObject filter = new JSONObject().put("max_num_terms", 10000)
				                            .put("min_word_length", 3);
		
		queryData.put("filter", filter);
		
		return queryData;
	}

	private ArrayList<TermVectorScore> scoreResultsWithStemmedWords(JSONObject result) {
		try {
			JSONObject words = result.getJSONObject("term_vectors").getJSONObject("text").getJSONObject("terms");
			
			Iterator<?> keys = words.keys();

			HashMap<String, TermVectorScore> lemmatizedList = new HashMap<>();
			
			while( keys.hasNext() ) {
			    String word = (String)keys.next();
			    
			    if (isStopWord(word)) {
			    	continue;
			    }
			    
				PorterStemmer stemmer = new PorterStemmer();
				String stemmedWord = stemmer.stem(word);

			    if (!lemmatizedList.containsKey(stemmedWord)) {
			    	TermVectorScore tvs = new TermVectorScore(stemmedWord, words.getJSONObject(word).getInt("doc_freq"), 
			    			words.getJSONObject(word).getInt("ttf"), words.getJSONObject(word).getInt("term_freq"), 
			    			words.getJSONObject(word).getDouble("score"));
			    	lemmatizedList.put(stemmedWord, tvs);
			    } else {
			    	// (lemmatized) Word is already present in the list. Add the scores.
			    	TermVectorScore scores = lemmatizedList.get(stemmedWord);
			    	scores.addScores(words.getJSONObject(word).getInt("doc_freq"), 
			    			words.getJSONObject(word).getInt("ttf"), words.getJSONObject(word).getInt("term_freq"), 
			    			words.getJSONObject(word).getDouble("score"));
			    	
			    	lemmatizedList.put(stemmedWord, scores);
			    }
			}
			
			return new ArrayList<TermVectorScore>(lemmatizedList.values());
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<TermVectorScore>();
		}
		
	}

	private boolean isStopWord(String key) {
		Set<String> stopWords = StopWords.getStopWords();
		return stopWords.contains(key.toLowerCase());
	}

}