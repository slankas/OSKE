package edu.ncsu.las.model.nlp;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * 
 * 
 */
public class Sentence {
	
	/** what was the text for this sentence orginally? */
	private String _originalText; 

	/** what was the text for this sentence after coreference resolution? */
	private String _corefText; 
	
	/** What is the starting position of this sentence in the original document */
	private int _startPosition;

	/** What is the sentence index in the document?  This starts at 1 */
	private int _sentenceIndex;	
	
	/** What is the sentiment of this document?  0 to 4, 0 = negative, 1= mildly neg, 2 neutral, 3 mildly positive, 4 positive*/
	private int _sentimentScore;
	
	/** */
	private java.util.List<NamedEntity> _namedEntities;
	
	private java.util.List<TripleRelation> _triples;

	private java.util.List<Token> _sentenceTokens;
	
	public Sentence() {
	}
	
	public Sentence(String text, int documentStartPosition, int sentenceIndex, java.util.List<Token> sentenceTokens) {
		_originalText = text;
		_startPosition = documentStartPosition;
		_sentenceIndex = sentenceIndex;
		_sentenceTokens = sentenceTokens;
		_namedEntities = NamedEntity.extractEntities(sentenceTokens);
	}
	
	public void setSentiment(int newValue) {
		_sentimentScore = newValue;
	}
	
	public void setCorefText(String text) {
		_corefText = text;
	}
	
	public void setTripleRelations(java.util.List<TripleRelation> triples) {
		_triples = triples;
	}
	

	public static java.util.List<Sentence> extractSentences(Annotation doc) {
		java.util.ArrayList<Sentence> result = new java.util.ArrayList<Sentence>();
		
	    int sentenceNumber = 0;
	    for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) { 
	    	sentenceNumber++;
	    	
	    	Sentence s = new Sentence();
	    	s._sentenceTokens = Token.extractTokens(sentence,sentenceNumber);
	    	s._originalText = sentence.get(edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation.class).toString();
	    	s._namedEntities = NamedEntity.extractEntities(s._sentenceTokens);
	    	
	    	result.add(s);
	    }
	    return result;
	}
	
	/** where does this sentence start in the overall document */
	public int getStartPosition() { return _startPosition; }
	
	public String getOriginalText() { return _originalText; }
	
	public String getCorefText() { return _corefText; }
	
	public int getSentimentScore() { return _sentimentScore; }
	
	public java.util.List<NamedEntity> getNamedEntities() { return _namedEntities;}
	
	public java.util.List<TripleRelation> getTripleRelations() { return _triples; }
		
	public java.util.List<Token> getTokens() { return _sentenceTokens; }
	
	
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject().put("text", _originalText)
				                         .put("corefText", _corefText)
				                         .put("sentiment", _sentimentScore)
				                         .put("startPosition", _startPosition)
				                         .put("sentenceIndex", _sentenceIndex)
				                         .put("entities", NamedEntity.toJSONArray(_namedEntities))
				                         .put("triples",  TripleRelation.toJSONArray(_triples))
				                         .put("tokens", Token.toJSONArray(_sentenceTokens));
		
	    return obj;
	}
	
	public static JSONArray toJSONArray(java.util.List<Sentence> sentences) {
		JSONArray records = new JSONArray();
		for (Sentence tr: sentences) {
			records.put(tr.toJSONObject());
		}
		return records;
	}
	
	public static String toTabDelimitedString(String documentID, java.util.List<Sentence> sentences) {
		java.util.ArrayList<Token> allTokens = new java.util.ArrayList<Token>();
		for (Sentence s: sentences) {
			allTokens.addAll(s.getTokens());
		}
		return Token.toTabDelimitedString(allTokens, true,documentID);		
	}


	

	
}
