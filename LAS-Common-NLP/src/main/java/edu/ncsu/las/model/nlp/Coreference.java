package edu.ncsu.las.model.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * Coreference provides a static method to replaces co-references in a text document with their representative mention.
 * (e.g., a pronoun is replaced with the person's name
 * 
 */
public class Coreference {

	private static StanfordCoreNLP pipeline;
	
	public static String createSentenceTextFromParts(List<String> parts) {
		StringBuilder result = new StringBuilder();
		
		boolean nospace = false;
		
		for (int i=0; i< parts.size(); i++) {
			String part = parts.get(i);
			if      (part.equals("-LRB-")) { part = "("; }
			else if (part.equals("-LSB-")) { part = "["; }
			else if (part.equals("-RRB-")) { part = ")"; }
			else if (part.equals("-RSB-")) { part = "]"; }

			if (!nospace && i > 0 && !UtilityNLP.NO_PRIOR_SPACE_PUNCTUATION.contains(part) && !part.startsWith("'") && !part.startsWith( "’")) { result.append(" "); }
			if (part.equals("(") || part.equals("[")) {
				nospace = true;
			}
			else { nospace =false;}	
			
			result.append(part);
		}
		
		return result.toString();	
	}
	
	/**
	 * Replaces items with their co-referenced text.
	 * 
	 * Code adapated from https://stackoverflow.com/questions/30182138/how-to-replace-a-word-by-its-most-representative-mention-using-stanford-corenlp/30185771
	 * 
	 * @param document use a pre-annotated document
	 * @return
	 */
	public static List<String> resolveCoreferences(Annotation document) {
		List<String> resolvedSentences = new ArrayList<String>();
				
	    Map<Integer, CorefChain> corefs = document.get(CorefChainAnnotation.class);
	    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    
	    for (CoreMap sentence : sentences) {
	    	List<String> resolved = new ArrayList<String>();
	        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

	        for (CoreLabel token : tokens) {
	            Integer corefClustId= token.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
	            CorefChain chain = corefs.get(corefClustId);
	            if (chain == null) { // curent token did not have an associated coreference.  add and move on ..
	                resolved.add(token.word());
	            }
	            else {
	                int sentenceIndex = chain.getRepresentativeMention().sentNum -1;

	                CoreMap corefSentence = sentences.get(sentenceIndex);
	                List<CoreLabel> corefSentenceTokens = corefSentence.get(TokensAnnotation.class);
	               
	                CorefMention representative = chain.getRepresentativeMention();

	                if (token.index() <= representative.startIndex || token.index() >= representative.endIndex) {
	                	for (int i = representative.startIndex; i < representative.endIndex; i++) {
	                		CoreLabel matchedLabel = corefSentenceTokens.get(i - 1); 
	                		resolved.add(matchedLabel.word().replace("'s", ""));
	                	}
	                }
	                else {
	                	resolved.add(token.word());
	                }
	            }
	        }
	        resolvedSentences.add(Coreference.createSentenceTextFromParts(resolved));
	    }
		
		return resolvedSentences;
	}	
	
	
	/**
	 * Convenience methods to annotate the text string (document) with the full Stanford CoreNLP pipeline prior 
	 * to resolving the co-reference text.
	 * 
	 * @param text
	 * @return
	 */
	public static List<String> resolveCoreferences(String text) {
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		
		return resolveCoreferences(document);
	}
}
