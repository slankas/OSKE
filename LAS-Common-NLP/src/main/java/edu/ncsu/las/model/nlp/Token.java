package edu.ncsu.las.model.nlp;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.ncsu.las.util.Export;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.ud.CoNLLUUtils;
import edu.stanford.nlp.util.CoreMap;


/**
 * Token reflects a single word / item (e.g., punctuation) within a parsed sentence.
 * The attributes equate to those in the CoNLL-U format (http://universaldependencies.org/format.html),
 * along with additional attributes tied to where the token occurs within a document (sentence, text position).
 *
 *
 */
public class Token {
	private static final String LRB_PATTERN = "(?i)-LRB-";
	private static final String RRB_PATTERN = "(?i)-RRB-";
	
	
	private int _sentenceIndex;
	private int _tokenIndex;
	private String _word;
	private String _lemma;
	private String _universalPartOfSpeech;
	private String _partOfSpeech;
	private String _features;
	private int _governingIndex;
	private String _relationshipName;
	private String _additionalRelationshipNames;
	private String _miscellaneous;
	private String _namedEntity;
	private int _textPositionStart;
	private int _textPositionEnd;
	

	public int getSentenceIndex() {
		return _sentenceIndex;
	}

	public void setSentenceIndex(int sentenceIndex) {
		_sentenceIndex = sentenceIndex;
	}

	public int getTokenIndex() {
		return _tokenIndex;
	}

	public void setTokenIndex(int tokenIndex) {
		_tokenIndex = tokenIndex;
	}

	public String getWord() {
		return _word;
	}

	public void setWord(String word) {
		_word = word;
	}

	public String getLemma() {
		return _lemma;
	}

	public void setLemma(String lemma) {
		_lemma = lemma;
	}

	public String getUniversalPartOfSpeech() {
		return _universalPartOfSpeech;
	}

	public void setUniversalPartOfSpeech(String universalPartOfSpeech) {
		_universalPartOfSpeech = universalPartOfSpeech;
	}

	public String getPartOfSpeech() {
		return _partOfSpeech;
	}

	public void setPartOfSpeech(String partOfSpeech) {
		_partOfSpeech = partOfSpeech;
	}

	public String getFeatures() {
		return _features;
	}

	public void setFeatures(String features) {
		_features = features;
	}

	public int getGoverningIndex() {
		return _governingIndex;
	}

	public void setGoverningIndex(int governingIndex) {
		_governingIndex = governingIndex;
	}

	public String getRelationshipName() {
		return _relationshipName;
	}

	public void setRelationshipName(String relationshipName) {
		_relationshipName = relationshipName;
	}

	public String getAdditionalRelationshipNames() {
		return _additionalRelationshipNames;
	}

	public void setAdditionalRelationshipNames(String additionalRelationshipNames) {
		_additionalRelationshipNames = additionalRelationshipNames;
	}

	public String getMiscellaneous() {
		return _miscellaneous;
	}

	public void setMiscellaneous(String miscellaneous) {
		_miscellaneous = miscellaneous;
	}

	public String getNamedEntity() {
		return _namedEntity;
	}

	public void setNamedEnitity(String namedEnitity) {
		_namedEntity = namedEnitity;
	}

	public int getTextPositionStart() {
		return _textPositionStart;
	}

	public void setTextPositionStart(int textPositionStart) {
		_textPositionStart = textPositionStart;
	}

	public int getTextPositionEnd() {
		return _textPositionEnd;
	}

	public void setTextPositionEnd(int textPositionEnd) {
		_textPositionEnd = textPositionEnd;
	}

	/**
	 * Given a parsed sentence from an annotated document through Stanford's NLP processor, 
	 * extract the token and their correspending information.
	 * 
	 * @param sentence
	 * @param sentenceIndex
	 * @return List of the tokens in the sentence, ordered by their position.
	 */
	public static java.util.List<Token> extractTokens(CoreMap sentence, int sentenceIndex) {
		java.util.ArrayList<Token> result = new java.util.ArrayList<Token>();
		
		SemanticGraph sg = sentence.get(BasicDependenciesAnnotation.class);
			
        for (IndexedWord token : sg.vertexListSorted()) {
	            /* Check for multiword tokens. */
	            /*
	        	if (token.containsKey(CoreAnnotations.CoNLLUTokenSpanAnnotation.class)) {
	                IntPair tokenSpan = token.get(CoreAnnotations.CoNLLUTokenSpanAnnotation.class);
	                if (tokenSpan.getSource() == token.index()) {
	                    String range = String.format("%d-%d", tokenSpan.getSource(), tokenSpan.getTarget());
	                    System.out.print(String.format("%s\t%s\t_\t_\t_\t_\t_\t_\t_\t_%n", range, token.originalText()));
	                }
	            }
	            */

            /* Try to find main governor and additional dependencies. */
            int govIdx = -1;
            GrammaticalRelation reln = null;
            HashMap<String, String> additionalDeps = new HashMap<>();
            for (IndexedWord parent : sg.getParents(token)) {
                SemanticGraphEdge edge = sg.getEdge(parent, token);
                if ( govIdx == -1 && ! edge.isExtra()) {
                    govIdx = parent.index();
                    reln = edge.getRelation();
                } else {
                    additionalDeps.put(Integer.toString(parent.index()), edge.getRelation().toString());
                }
            }
	            
            String additionalDepsString = CoNLLUUtils.toExtraDepsString(additionalDeps);
            String word = token.word();
            String featuresString = CoNLLUUtils.toFeatureString(token.get(CoreAnnotations.CoNLLUFeats.class));
            String pos = token.getString(CoreAnnotations.PartOfSpeechAnnotation.class, "_");
            String upos = token.getString(CoreAnnotations.CoarseTagAnnotation.class, "_");
            String misc = token.getString(CoreAnnotations.CoNLLUMisc.class, "_");
            String lemma = token.getString(CoreAnnotations.LemmaAnnotation.class, "_");
            String relnName = reln == null ? "_" : reln.toString();
	            
	            
            /* Root. */
            if (govIdx == -1 && sg.getRoots().contains(token)) {
                govIdx = 0;
                relnName = GrammaticalRelation.ROOT.toString();
            }

            word = word.replaceAll(LRB_PATTERN, "(");
            word = word.replaceAll(RRB_PATTERN, ")");
            lemma = lemma.replaceAll(LRB_PATTERN, "(");
            lemma = lemma.replaceAll(RRB_PATTERN, ")");

            Token t = new Token();
            t.setSentenceIndex(sentenceIndex);
            t.setTokenIndex(token.index());
            t.setWord(word);
            t.setLemma(lemma);
            t.setUniversalPartOfSpeech(upos);
            t.setPartOfSpeech(pos);
            t.setFeatures(featuresString);
            t.setGoverningIndex(govIdx);
            t.setRelationshipName(relnName);
            t.setAdditionalRelationshipNames(additionalDepsString);
            t.setMiscellaneous(misc);
            t.setNamedEnitity(token.getString(CoreAnnotations.NamedEntityTagAnnotation.class,"_"));
            t.setTextPositionStart(token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class));
            t.setTextPositionEnd( token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
            
            result.add(t);
	    }
	    
	    return result;
	}

	public static java.util.List<Token> fromTabDelimitedStreamReader(java.io.InputStreamReader isr, boolean hasHeader, boolean hasDocumentID) throws IOException {
		java.util.ArrayList<Token> result = new java.util.ArrayList<Token>();
		
		LineNumberReader lnr = new LineNumberReader(isr);
		String line;
		if (hasHeader) {line = lnr.readLine(); } // skip header
		
		while ( (line = lnr.readLine()) != null) {
    		result.add(fromTabDelimitedLine(line, hasDocumentID));
		}
		
	    return result;
	}
	
	public static Token fromTabDelimitedLine(String line, boolean hasDocumentID) throws IOException {
		String[] elements = line.split("\t");
		Token t = new Token();
		
		int index = 0;
		if (hasDocumentID) { index++; }
		
        t.setSentenceIndex(Integer.parseInt(elements[index++]));
        t.setTokenIndex(Integer.parseInt(elements[index++]));
        t.setWord(elements[index++]);
        t.setLemma(elements[index++]);
        t.setUniversalPartOfSpeech(elements[index++]);
        t.setPartOfSpeech(elements[index++]);
        t.setFeatures(elements[index++]);
        t.setGoverningIndex(Integer.parseInt(elements[index++]));
        t.setRelationshipName(elements[index++]);
        t.setAdditionalRelationshipNames(elements[index++]);
        t.setMiscellaneous(elements[index++]);
        t.setNamedEnitity(elements[index++]);
        t.setTextPositionStart(Integer.parseInt(elements[index++]));
        t.setTextPositionEnd(Integer.parseInt(elements[index++]));
           		
	    return t;
	}	
	
	
	public JSONObject toJSONObject() {
		JSONObject tokenObject = new JSONObject().put("sentenceIndex",_sentenceIndex)
				                                 .put("tokenIndex", _tokenIndex)
				                                 .put("word", _word)
				                                 .put("lemma", _lemma)
				                                 .put("universalPartOfSpeech", _universalPartOfSpeech)
				                                 .put("partOfSpeech", _partOfSpeech)
				                                 .put("features", _features)                
				                                 .put("governingIndex", _governingIndex)
				                                 .put("relationshipName", _relationshipName)
				                                 .put("additionalRelationshipNames", _additionalRelationshipNames)
				                                 .put("miscellaneous",_miscellaneous)
				                                 .put("namedEntity", _namedEntity)
				                                 .put("textPositionStart", _textPositionStart)
				                                 .put("textPositionEnd", _textPositionEnd);		
	    return tokenObject;
	}
	
	public static JSONArray toJSONArray(java.util.List<Token> tokens) {
		JSONArray records = new JSONArray();
		for (Token t: tokens) {
			records.put(t.toJSONObject());
		}
		return records;
	}

	private static final String[] columnNames = {"sentenceIndex", "tokenIndex", "word", "lemma","universalPartOfSpeech", "partOfSpeech",	 "features", "governingIndex", "relationshipName", "additionalRelationshipNames", "miscellaneous", "namedEnitity", "textPositionStart",	 "textPositionEnd",};
	
	public static String toTabDelimitedString(java.util.List<Token> relations, boolean includeHeader, String documentID) {
		String result = Export.convertJSONArrayToTabDelimited(toJSONArray(relations), columnNames, columnNames, includeHeader, documentID);
		
		return result;
	}
	
}
