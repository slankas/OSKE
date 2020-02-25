package edu.ncsu.las.model.nlp;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import edu.ncsu.las.model.nlp.TripleRelation.FilterStyle;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

/**
 * Represents an overall document, which is primarily composed of sentences.
 * 
 */
public class Document {

	/** What was the original text that was used to create this document */
	private String _originaltext;
	
	/** What are the different sentences in the document */
	private java.util.List<Sentence> _sentences;
	
	/** What is the overall sentiment score for this document?  Simply an average of all of the sentence scores*/
	private double _sentimentScore = Double.NaN;
	
	/**
	 * private construct to have the creation performed through the parse factory method
	 */
	private Document() {
	}

	/**
	 * Convenience object to return two values from getTextwindow
	 */
	static class SplitResult {
		String text;
		int sentencePositionToUse;
	}

	/**
	 * Given the current sentence index and the window size (which is the number of sentences before and after, create
	 * a new document with all of the sentences in the window.  If the window size is 1, then only 1 sentence is returned.
	 * If the window size is 2, then 5 sentences would be returned (except at the start and end of the document.
	 * Typically, the sentences will then be processed again for co-reference resolution
	 * 
	 * @param splitSentences what are the differenet sentences available?
	 * @param currSentenceIndex Which sentence is the center/focus?
	 * @param windowSize how many sentences to be included before and after.
	 * 
	 * @return String of sentences along with which index is the sentence that was the focus.
	 */
	static SplitResult getTextWindow(List<String> splitSentences, int currSentenceIndex, int windowSize) {
		SplitResult result = new SplitResult();
		
		int startIndex = Math.max(currSentenceIndex - windowSize, 0);
		int endIndex    = Math.min(currSentenceIndex + windowSize, splitSentences.size()-1);
		
		StringBuilder sentences =  new StringBuilder();
		int count = -1;
		for (int i = startIndex; i<= endIndex;i++) {
			count++;
			if (i == currSentenceIndex) {
				result.sentencePositionToUse = count;
			}
			sentences.append(splitSentences.get(i));
			sentences.append(" ");
		}
		result.text = sentences.toString().trim();
		return result;
	}
	
	
	/**
	 * Create a document from the given text.
	 * TODO: explain the parsing process
	 * 
	 * @param text
	 * @param window
	 * @return
	 */
	public static Document parse(String text, int windowSize, JSONArray spacyEntities, JSONArray dbpediaEntities) {
		Document result = new Document();
	
		result._originaltext = text;
		result._sentences    = new java.util.ArrayList<Sentence>();
		

		List<String> splitSentences = StanfordNLP.splitSentences(text);

		int lastFoundPosition = -1;
		int lastSentenceLength = 1;
		int sentenceCount      = 0;
		
		int runningSentinmentScore = 0; 
		
		// perform the window parsing - used to do coreref, plus get initial named entities, sentiment, and tokens
		for (int sentenceIndex = 0; sentenceIndex < splitSentences.size(); sentenceIndex++) {
			
			SplitResult sr = getTextWindow(splitSentences, sentenceIndex,windowSize);
			
			Annotation sentenceDoc = StanfordNLP.annotateText(sr.text);
		    List<CoreMap> sentences = sentenceDoc.get(CoreAnnotations.SentencesAnnotation.class);
		    CoreMap sentence = sentences.get(sr.sentencePositionToUse);
		    
	    	sentenceCount++;
	    	String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
	    	
	    	int documentPosition = text.indexOf(sentenceText,lastFoundPosition);
	    	if (documentPosition == -1) { 
	    		documentPosition = lastFoundPosition + lastSentenceLength;
	    	}
	    	
	    	java.util.List<Token> sentenceTokens = Token.extractTokens(sentence, sentenceCount);
	    	
	    	Sentence s = new Sentence(sentenceText, documentPosition, sentenceCount, sentenceTokens);
	    	
	    	Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
            s.setSentiment(sentiment);
            runningSentinmentScore += sentiment;
	    	
            List<String> resolvedSentences = Coreference.resolveCoreferences(sentenceDoc);
            s.setCorefText(resolvedSentences.get(sr.sentencePositionToUse));
                        
	    	result._sentences.add(s);
	    	
	    	lastFoundPosition = documentPosition;
	    	lastSentenceLength = sentenceText.length();
		}
		result._sentimentScore =  ((double) runningSentinmentScore) / sentenceCount;
	

		// reparse to get openIE / relations items
		int baseSentenceIndex = -1;
		for (Sentence s: result._sentences) {
			Annotation sentenceDoc = StanfordNLP.annotateText(s.getCorefText());
			baseSentenceIndex++;
			
			java.util.List<Token> sentenceTokens = Token.extractTokens(sentenceDoc.get(CoreAnnotations.SentencesAnnotation.class).get(0), baseSentenceIndex);
			
			java.util.List<TripleRelation> triples = TripleRelation.extractTriples(sentenceDoc, baseSentenceIndex);
			triples = TripleRelation.valiadateTriples(triples, sentenceTokens);
			
			List<AnnotatedProposition> minieTriplesAP =  StanfordNLP.extractMinIETriples(s.getCorefText());
			java.util.List<TripleRelation> minetriples = TripleRelation.extractTriples(minieTriplesAP, baseSentenceIndex);
			minetriples = TripleRelation.valiadateTriples(minetriples, sentenceTokens);
			
			java.util.List<TripleRelation> combinedTriples = TripleRelation.combine(triples, minetriples);
			
			combinedTriples = TripleRelation.reduceTriples(combinedTriples, FilterStyle.FILTER_TO_MAXIMIZE_SUBJ_OBJ_MINIMIZE_RELATION);
			//combinedTriples = TripleRelation.reduceTriples(combinedTriples, FilterStyle.FILTER_TO_MINIMIZE_TERMS);
			
			TripleRelation.augmentSubjectObjectWithStanfordNER(combinedTriples,s.getNamedEntities());
			TripleRelation.augmentSubjectObjectWithSpacyNER(combinedTriples, spacyEntities, s.getStartPosition(), s.getStartPosition() + s.getOriginalText().length());
			TripleRelation.augmentSubjectObjectWithDBPedia(combinedTriples, dbpediaEntities, s.getStartPosition(), s.getStartPosition() + s.getOriginalText().length());
			
			s.setTripleRelations(combinedTriples);
		}
		
		return result;
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public double getSentimentScore() {
		return _sentimentScore;
	}
	
	public java.util.List<Sentence> getSentences() {
		return _sentences;
	}
	
	public JSONObject toJSONObject() {
		double sentimentScore = _sentimentScore;
		if (Double.isNaN(sentimentScore) || Double.isInfinite(sentimentScore) || sentimentScore == Double.MAX_VALUE || sentimentScore == Double.MIN_VALUE || sentimentScore == Double.NEGATIVE_INFINITY || sentimentScore == Double.POSITIVE_INFINITY) {
			sentimentScore = -999;
		}
		return new JSONObject().put("text",_originaltext)
				               .put("sentinment", sentimentScore)
				               .put("sentences", Sentence.toJSONArray(_sentences));
	}	
}
