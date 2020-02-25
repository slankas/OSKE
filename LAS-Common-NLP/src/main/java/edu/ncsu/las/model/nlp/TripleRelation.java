package edu.ncsu.las.model.nlp;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.minie.annotation.Quantity;
import edu.ncsu.las.util.Export;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Represents a triple extract through Stanford's OpenIE Annotator
 * http://stanfordnlp.github.io/CoreNLP/openie.html
 * 
 */
public class TripleRelation {

	private int _sentenceIndex;
	private boolean _prefixBe;
	private boolean _suffixBe;
	private boolean _suffixOf;
	private boolean _timeModifier;
	private double  _confidenceScore;
	private String _subject;
	private String _relation;
	private String _object;
	private int _subjectIndexStart = -1;
	private int _subjectIndexEnd = -1;
	private int _relationIndexStart = -1;
	private int _relationIndexEnd = -1;
	private int _objectIndexStart = -1;
	private int _objectIndexEnd = -1;
	private HashSet<String> _source = new HashSet<String>();  // where did this triple originate?  corenlp, minie
	
	private Set<String> _subjectSynonyms = new HashSet<String>();
	private Set<String> _relationSynonyms = new HashSet<String>();
	private Set<String> _objectSynonyms = new HashSet<String>();

	
	private HashSet<String> _subjectNamedEntities = new HashSet<String>();  // contains the type of any named entity attached to the subject
	private HashSet<String> _objectNamedEntities = new HashSet<String>();	// contains the type of any named entity attached to the object
	
	private JSONObject _subjectDBPedia = null;
	private JSONObject _objectDBPedia  = null;
	
	public TripleRelation() {
	}
	
	public TripleRelation(TripleRelation previous) {
		_sentenceIndex = previous.getSentenceIndex();
		_prefixBe = previous.isPrefixBe();
		_suffixBe = previous.isSuffixBe();
		_suffixOf = previous.isSuffixOf();
		_timeModifier = previous.isTimeModifier();
		_confidenceScore = previous.getConfidenceScore();
		_subject = previous.getSubject();
		_relation = previous.getRelation();
		_object = previous.getObject();
		_subjectIndexStart = previous.getSubjectIndexStart();
		_subjectIndexEnd = previous.getSubjectIndexEnd();
		_relationIndexStart = previous.getRelationIndexStart();
		_relationIndexEnd = previous.getRelationIndexEnd();
		_objectIndexStart = previous.getObjectIndexStart();
		_objectIndexEnd = previous.getObjectIndexEnd();		
		_source = new HashSet<String>(previous._source); // don't share the object pointer
	}
	
	public int getSentenceIndex() {
		return _sentenceIndex;
	}


	public void setSentenceIndex(int sentenceIndex) {
		_sentenceIndex = sentenceIndex;
	}

	public boolean isPrefixBe() {
		return _prefixBe;
	}



	public void setPrefixBe(boolean prefixBe) {
		_prefixBe = prefixBe;
	}



	public boolean isSuffixBe() {
		return _suffixBe;
	}



	public void setSuffixBe(boolean suffixBe) {
		_suffixBe = suffixBe;
	}



	public boolean isSuffixOf() {
		return _suffixOf;
	}



	public void setSuffixOf(boolean suffixOf) {
		_suffixOf = suffixOf;
	}



	public boolean isTimeModifier() {
		return _timeModifier;
	}



	public void setTimeModifier(boolean timeModifier) {
		_timeModifier = timeModifier;
	}



	public double getConfidenceScore() {
		return _confidenceScore;
	}



	public void setConfidenceScore(double confidenceScore) {
		_confidenceScore = confidenceScore;
	}

	public String getSubject() {
		return _subject;
	}
	
	public void setSubject(String subject) {
		subject = subject.trim();
		_subject = subject;
		_subjectSynonyms.add(subject);
	}
	
	public HashSet<String> getSubjectNamedEntities() {
		return new HashSet<String>(_subjectNamedEntities);
	}
	
	public HashSet<String> getSubjectSynonyms() {
		return new HashSet<String>(_subjectSynonyms);
	}
	public HashSet<String> getObjectSynonyms() {
		return new HashSet<String>(_objectSynonyms);
	}
		
	public JSONObject getSubjectDBPedia() {
		if (_subjectDBPedia != null) {
			return new JSONObject(_subjectDBPedia.toString());
		}
		else {
			return null;
		}
	}
	
	public String getRelation() {
		return _relation;
	}

	public void setRelation(String relation) {
		relation = relation.trim();
		_relation = relation;
	}

	public String getObject() {
		return _object;
	}

	public void setObject(String object) {
		object = object.trim();
		_object = object;
		_objectSynonyms.add(object);
	}
	
	public HashSet<String> getObjectNamedEntities() {
		return new HashSet<String>(_objectNamedEntities);
	}

	public JSONObject getObjectDBPedia() {
		if (_objectDBPedia != null) {
			return new JSONObject(_objectDBPedia.toString());
		}
		else {
			return null;
		}
	}	
	
	
	public int getSubjectIndexStart() {
		return _subjectIndexStart;
	}

	public void setSubjectIndexStart(int subjectIndexStart) {
		_subjectIndexStart = subjectIndexStart;
	}

	public int getSubjectIndexEnd() {
		return _subjectIndexEnd;
	}

	public void setSubjectIndexEnd(int subjectIndexEnd) {
		_subjectIndexEnd = subjectIndexEnd;
	}

	public int getRelationIndexStart() {
		return _relationIndexStart;
	}

	public void setRelationIndexStart(int relationIndexStart) {
		_relationIndexStart = relationIndexStart;
	}

	public int getRelationIndexEnd() {
		return _relationIndexEnd;
	}

	public void setRelationIndexEnd(int relationIndexEnd) {
		_relationIndexEnd = relationIndexEnd;
	}

	public int getObjectIndexStart() {
		return _objectIndexStart;
	}

	public void setObjectIndexStart(int objectIndexStart) {
		_objectIndexStart = objectIndexStart;
	}

	public int getObjectIndexEnd() {
		return _objectIndexEnd;
	}

	public void setObjectIndexEnd(int objectIndexEnd) {
		_objectIndexEnd = objectIndexEnd;
	}

	
	public void changePassiveToActive() {
		String relation = this.getRelation();
		if (!(relation.startsWith("be ") && relation.endsWith(" by"))) {
			return; // simply passive check not found...
		}
		try {
			relation = relation.substring(3, relation.length()-3).trim();
			this.setRelation(relation);
			
			String holdSubject = this.getSubject();
			int    holdSubjectIndexStart = this.getSubjectIndexStart();
			int    holdSubjectIndexEnd   = this.getSubjectIndexEnd();
			
			this.setSubject(this.getObject());
			this.setSubjectIndexStart(this.getObjectIndexStart());
			this.setSubjectIndexEnd(this.getObjectIndexEnd());
			
			this.setObject(holdSubject);
			this.setObjectIndexStart(holdSubjectIndexStart);
			this.setObjectIndexEnd(holdSubjectIndexEnd);
		}
		catch (Exception e) {
			System.err.println("Unable to convert passive to active: "+ this.toString());
		}
		
		return;
	}
	
	public static java.util.List<TripleRelation> extractTriples(Annotation doc, int baseSentenceIndex) {
		java.util.ArrayList<TripleRelation> result = new java.util.ArrayList<TripleRelation>();
		
	    int sentenceNumber = -1;
	    for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) { 
	    	sentenceNumber++;
	    	
	    	Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

	    	for (RelationTriple triple : triples) {
	    		TripleRelation tr = new TripleRelation();
	    		tr.setSentenceIndex(baseSentenceIndex + sentenceNumber);
	    		tr.setPrefixBe(triple.isPrefixBe());
	    		tr.setSuffixBe(triple.isSuffixBe());
	    		tr.setSuffixOf(triple.isSuffixOf());
	    		tr.setTimeModifier(triple.istmod());
	    		tr.setConfidenceScore(triple.confidence);
	    		tr.setSubject(triple.subjectGloss().toLowerCase()); //LemmaGloss()
	    		tr.setRelation(triple.relationLemmaGloss().toLowerCase());
	    		tr.setObject(triple.objectGloss().toLowerCase());
	    		tr.setSubjectIndexStart(triple.subjectTokenSpan().first+1);   //Added plus one as the span starts before, not on.
	    		tr.setSubjectIndexEnd(triple.subjectTokenSpan().second);
	    		tr.setRelationIndexStart(triple.relationTokenSpan().first+1);
	    		tr.setRelationIndexEnd(triple.relationTokenSpan().second);
	    		tr.setObjectIndexStart(triple.objectTokenSpan().first+1);
	    		tr.setObjectIndexEnd(triple.objectTokenSpan().second);
	    		tr.addSource("corenlp");
	    		
	    		tr.setSubject(replaceTerms(tr.getSubject()));
	    		tr.setRelation(replaceTerms(tr.getRelation()));
	    		tr.setObject(replaceTerms(tr.getObject())); 
	    		
	    		tr.changePassiveToActive();

	    		if (tr.getSubject().contains("quant_s")) {
	    			System.out.println("@@@@@@  found quant - Stanford");
	    		}
	    		
	    		
	    		result.add(tr);
	    	}
	    }		
	    return result;
	}

	/** add the source value to our set of sources */
	private void addSource(String string) {
		_source.add(string);	
	}
	private void addSource(Collection<String> c) {
		_source.addAll(c);	
	}	
	public Set<String> getSources() {
		return _source;
	}
	
	public static String replaceTerms(String value) {
		String result = value.replaceAll("thus", "");
		result = result.replaceAll("also", "");
		result = result.replaceAll(" '", "'");
		return result;
	}

	public static List<TripleRelation> extractTriples(List<AnnotatedProposition> minieTriplesAP, int sentenceIndex) {
		java.util.ArrayList<TripleRelation> result = new java.util.ArrayList<TripleRelation>();
		
		for (AnnotatedProposition ap: minieTriplesAP) {
			try {
	    		TripleRelation tr = new TripleRelation();
	    		tr.setSentenceIndex(sentenceIndex);
	    		//tr.setPrefixBe(ap.       triple.isPrefixBe());
	    		//tr.setSuffixBe(triple.isSuffixBe());
	    		//tr.setSuffixOf(triple.isSuffixOf());
	    		//tr.setTimeModifier(triple.istmod());
	    		//tr.setConfidenceScore(ap.);
	    		tr.setSubject(ap.getSubject().getWordsLowercase()); //LemmaLowercase());
	    		tr.setRelation(ap.getRelation().getWordsLemmaLowercase()); 
	    		tr.setObject(ap.getObject().getWordsLowercase()); //LemmaLowercase());
	    		
	    		    		
	    		if (ap.getSubject().getWordCoreLabelList().size() > 0) {
	    			tr.setSubjectIndexStart(ap.getSubject().getWordCoreLabelList().get(0).index());   //Added plus one as the span starts before, not on.
	    			tr.setSubjectIndexEnd(ap.getSubject().getWordCoreLabelList().get(ap.getSubject().getWordCoreLabelList().size()-1).index());
	    		}
	    		if (ap.getRelation().getWordCoreLabelList().size() > 0) {
	    			tr.setRelationIndexStart(ap.getRelation().getWordCoreLabelList().get(0).index());
	        		tr.setRelationIndexEnd(ap.getRelation().getWordCoreLabelList().get(ap.getRelation().getWordCoreLabelList().size()-1).index());
	    		}
	    		if (ap.getObject().getWordCoreLabelList().size() >0) {
		    		tr.setObjectIndexStart(ap.getObject().getWordCoreLabelList().get(0).index());
		    		tr.setObjectIndexEnd(ap.getObject().getWordCoreLabelList().get(ap.getObject().getWordCoreLabelList().size()-1).index());
	    		}
	    		
	    		tr.setSubject(replaceTerms(tr.getSubject()));
	    		tr.setRelation(replaceTerms(tr.getRelation()));
	    		tr.setObject(replaceTerms(tr.getObject())); 
	    		tr.changePassiveToActive();
	    		
	    		List<Quantity> quantities = ap.getAllQuantities();
	    		
	    		for (Quantity q: quantities) {
	    			//System.out.println(q.getId());
	    			
	    			String id = q.getId().toLowerCase();
	    			String full_id = "quant_"+id;
	    			
	    			StringBuffer replacementWord = new StringBuffer();
	    	        for (int i = 0; i < q.getQuantityWords().size(); i++){
	    	        	replacementWord.append(q.getQuantityWords().get(i).word());
	    	            if (i < q.getQuantityWords().size() - 1)
	    	            	replacementWord.append(CHARACTER.SPACE);
	    	        }
	    			tr.setSubject(tr.getSubject().replaceAll(full_id, replacementWord.toString()));
	    			tr.setObject(tr.getObject().replaceAll(full_id, replacementWord.toString()));	    			
	    		}
	    		/*
	    		if (tr.getSubject().contains("quant_s")) {
	    			System.out.println("@@@@@@  found quant - MINIE: "+ ap.toString());
	    		}
	    		*/
	    		
	    		tr.addSource("minie");
	    		result.add(tr);			
			}
			catch (Exception e) {
				System.err.println("Unable to convert from minIE to TR: "+ ap.toStringAllAnnotations());
				System.err.println("    exception: "+ e);
			}
			
		}
		
		return result;
	}	
	
	public static HashSet<String> RELATION_PARTS_OF_SPEECH = new HashSet<String>(Arrays.asList("IN","MD","RB","RBR","RBS","TO","VB","VBD","VBG","VBN","VBP","VBZ","WRB"));
	public static HashSet<String> PRONOUNS = new HashSet<String>(Arrays.asList("I","we","you","he","she","it","they","me","us","her","him","it","them","mine","ours","yours","hers","his","theirs","their","her","my","our","myself","yourself","herself","himself","itself","ourselves","yourselves","themselves"));
	public static HashSet<String> INDEFINITE_PRONOUNS = new HashSet<String>(Arrays.asList("all","another","any","anybody","anyone","anything","both","each","either","everybody","everyone","everything","few","many","most","neither","nobody","none","no one","nothing","one","other","others","several","some","somebody","someone","something","such"));
	public static HashSet<String> DEMONSTRATIVE_PRONOUNS = new HashSet<String>(Arrays.asList("such","that","these","this","those"));
	public static HashSet<String> SUBJECT_OBJECT_INVALID_SINGLE_POS = new HashSet<String>(Arrays.asList("JJ","JJR","JJS","RB","RBR","RBS","VB","VBD","VBG","VBN","VBP","VBZ","WRB"));
	
	public static List<TripleRelation> valiadateTriples(List<TripleRelation> triples, java.util.List<Token> sentenceTokens) {
		java.util.ArrayList<TripleRelation> result = new java.util.ArrayList<TripleRelation>();
		
		triple:
		for (TripleRelation tr: triples) {
			// Subject != object
			if (tr.getSubject().equals(tr.getObject())) {
				//System.out.println("subject and object are the same, DROPPING: "+tr.toString());
				continue triple;
			}
			
			// validate sizing
			if (tr.getSubject().equals("")) {
				//System.out.println("no subject, DROPPING: "+tr.toString());
				continue triple;
			}
			
			if (tr.getSubject().equals("\\")) {
				//System.out.println("subject is a backslash (\\), DROPPING: "+tr.toString());
				continue triple;
			}

			if (tr.getRelation().equals("")) {
				//System.out.println("no relation, DROPPING: "+tr.toString());
				continue triple;
			}

			if (tr.getObject().equals("")) {
				//System.out.println("no object, DROPPING: "+tr.toString());
				continue triple;
			}
			if (tr.getObject().equals("\\")) {
				System.out.println("object is a backslash (\\), DROPPING: "+tr.toString());
				continue triple;
			}
			
			
			if (tr.getRelation().length() < 2) {
				//System.out.println("Dropping zero/single character relation " + tr.getRelation() +": " + tr.toString());
				continue triple;
			}			
			if (tr.getSubject().length() < 2 && tr.getSubjectNamedEntities().size() == 0) {
				//System.out.println("Dropping zero/single character subject " + tr.getSubject() +": " + tr.toString());
				continue triple;
			}			
			if (tr.getObject().length() < 2 && tr.getObjectNamedEntities().size() == 0) {
				//System.out.println("Dropping zero/single character object " + tr.getObject() +": " + tr.toString());
				continue triple;
			}			
			
			// Validate relations: 1) no named entities present.  2) must be verbs / adverbs / prepositions 3) length > 1
			for (int i=tr.getRelationIndexStart(); i<=tr.getRelationIndexEnd(); i++) {
				if (i < 0 ) {
					//System.out.println("INDEX BELOW Zero/relation: "+tr.toString());
					break;
				}
				Token t = sentenceTokens.get(i-1);
				if (!t.getNamedEntity().equals("O") && !tr.getRelation().equals("be")) {
					//System.out.println("FOUND NAMED ENTITY, DROPPING " + t.getNamedEntity() +"   "+tr.toString());
					continue triple;
				}
				
				if (!RELATION_PARTS_OF_SPEECH.contains(t.getPartOfSpeech()) && !tr.getRelation().equals("be")) {
					//System.out.println("FOUND bad POS in relation, DROPPING " + t.getPartOfSpeech() +"   "+tr.toString());
					continue triple;
				}
				
				if (tr.getRelationIndexStart() == tr.getRelationIndexEnd() && t.getPartOfSpeech().equals("TO")) {
					//System.out.println("Dropping singlar 'to' in relation - " + t.getPartOfSpeech() +": " + tr.toString());
					continue triple;
				}
				if (tr.getRelationIndexStart() == tr.getRelationIndexEnd() && t.getPartOfSpeech().equals("IN")) {
					//System.out.println("Dropping singlar preposition in relation - " + t.getPartOfSpeech() +": " + tr.toString());
					continue triple;
				}
			}
						
			
			// Validate Subjects: cannot contain pronounds
			if (PRONOUNS.contains(tr.getSubject())) {
				//System.out.println("Found pronoun in subject, DROPPING " + tr.getSubject()+"   "+tr.toString());
				continue triple;
			}
			if (PRONOUNS.contains(tr.getObject())) {
				//System.out.println("Found pronoun in object, DROPPING " + tr.getObject()+"   "+tr.toString());
				continue triple;
			}
			
			// subject / object can not simply be an indefinite pronoun
			if (INDEFINITE_PRONOUNS.contains(tr.getSubject())) {
				//System.out.println("Found indefinite pronoun for subject, DROPPING " + tr.getSubject()+"   "+tr.toString());
				continue triple;
			}
			if (INDEFINITE_PRONOUNS.contains(tr.getObject())) {
				//System.out.println("Found indefinite pronoun for object, DROPPING " + tr.getObject()+"   "+tr.toString());
				continue triple;
			}			
			

			// subject / object can not simply be a demonstrative pronoun
			if (DEMONSTRATIVE_PRONOUNS.contains(tr.getSubject())) {
				//System.out.println("Found demonstrative pronoun for subject, DROPPING " + tr.getSubject()+"   "+tr.toString());
				continue triple;
			}
			if (DEMONSTRATIVE_PRONOUNS.contains(tr.getObject())) {
				//System.out.println("Found demonstrative pronoun for object, DROPPING " + tr.getObject()+"   "+tr.toString());
				continue triple;
			}			

			
			for (int i=tr.getSubjectIndexStart(); i<=tr.getSubjectIndexEnd(); i++) {
				if (i < 0) {
					//System.out.println("INDEX BELOW Zero/subject: "+tr.toString());
					break;
				}

				Token t = sentenceTokens.get(i-1);
				if (t.getPartOfSpeech().equals("PRP") || t.getPartOfSpeech().equals("WP")) {
					//System.out.println("FOUND bad POS in subject, DROPPING " + t.getPartOfSpeech() +"   "+tr.toString());
					continue triple;
				}
				
				if (tr.getSubjectIndexStart() == tr.getSubjectIndexEnd() && SUBJECT_OBJECT_INVALID_SINGLE_POS.contains(t.getPartOfSpeech())) {
					//System.out.println("Droping from bad POS in subject - " + t.getPartOfSpeech() +": " + tr.toString());
					continue triple;
				}
				
			}
			// Validate Objects: cannot contain pronouns
			for (int i=tr.getObjectIndexStart(); i<=tr.getObjectIndexEnd(); i++) {
				if (i < 0) {
					//System.out.println("INDEX BELOW Zero/object: "+tr.toString());
					break;
				}

				Token t = sentenceTokens.get(i-1);
				if (t.getPartOfSpeech().equals("PRP") || t.getPartOfSpeech().equals("WP")) {
					//System.out.println("FOUND bad POS in object, DROPPING " + t.getPartOfSpeech() +"   "+tr.toString());
					continue triple;
				}
				
				if (tr.getObjectIndexStart() == tr.getObjectIndexEnd() && SUBJECT_OBJECT_INVALID_SINGLE_POS.contains(t.getPartOfSpeech())) {
					//System.out.println("Droping from bad POS in object - " + t.getPartOfSpeech() +": " + tr.toString());
					continue triple;
				}

			}
			
			//private HashSet<String> _subjectSynonyms = new HashSet<String>();
			//private HashSet<String> _relationSynonyms = new HashSet<String>();
			//private HashSet<String> _objectSynonyms = new HashSet<String>();
			
			// validate that the relation synonyms have length > 1
			tr._relationSynonyms = tr._relationSynonyms.stream().filter(synonym -> synonym.length() > 1).collect(Collectors.toSet());		
			tr._subjectSynonyms  = tr._subjectSynonyms.stream().filter(synonym -> synonym.length() > 1).collect(Collectors.toSet());
			tr._objectSynonyms   = tr._objectSynonyms.stream().filter(synonym -> synonym.length() > 1).collect(Collectors.toSet());
			result.add(tr);
		}
		return result;
	}
	
	
	
	public static java.util.List<TripleRelation> fromTabDelimitedStreamReader(java.io.InputStreamReader isr, boolean hasDocumentID) throws IOException {
		java.util.ArrayList<TripleRelation> result = new java.util.ArrayList<TripleRelation>();
		
		LineNumberReader lnr = new LineNumberReader(isr);
		String line = lnr.readLine(); // skip header
		
		while ( (line = lnr.readLine()) != null) {
			String[] elements = line.split("\t");
			TripleRelation tr = new TripleRelation();
			
			int index = 0;
			if (hasDocumentID) { index++; }
			
    		tr.setSentenceIndex(Integer.parseInt(elements[index++]));
    		tr.setPrefixBe(Boolean.parseBoolean(elements[index++]));
    		tr.setSuffixBe(Boolean.parseBoolean(elements[index++]));
    		tr.setSuffixOf(Boolean.parseBoolean(elements[index++]));
    		tr.setTimeModifier(Boolean.parseBoolean(elements[index++]));
    		tr.setConfidenceScore(Double.parseDouble(elements[index++]));
    		tr.setSubject(elements[index++]);
    		tr.setRelation(elements[index++]);
    		tr.setObject(elements[index++]);
    		tr.setSubjectIndexStart(Integer.parseInt(elements[index++]));   //Added plus one as the span starts before, not on.
    		tr.setSubjectIndexEnd(Integer.parseInt(elements[index++]));
    		tr.setRelationIndexStart(Integer.parseInt(elements[index++]));
    		tr.setRelationIndexEnd(Integer.parseInt(elements[index++]));
    		tr.setObjectIndexStart(Integer.parseInt(elements[index++]));
    		tr.setObjectIndexEnd(Integer.parseInt(elements[index++]));
    		result.add(tr);
		}
		
	    return result;
	}
	
	public JSONObject toJSONObject() {
		JSONObject tripleObject = new JSONObject().put("sentIndex",_sentenceIndex)
	    			  									.put("prefixBe",_prefixBe)
	    			  									.put("suffixBe",_suffixBe)
	    			  									.put("suffixOf",_suffixOf)
	    			                                    .put("tMod", _timeModifier)
	    			                                    .put("conf",_confidenceScore)
	    			                                    .put("subj",_subject)
	    			                                    .put("rel",_relation)
	    			                                    .put("obj",_object)
	    			                                    .put("subjIdxStart",_subjectIndexStart)   //Added plus one as the span starts before, not on.
	    			                                    .put("subjIdxEnd",_subjectIndexEnd)
	    			                                    .put("relIdxStart",_relationIndexStart)
	    			                                    .put("relIdxEnd",_relationIndexEnd)
	    			                                    .put("objIdxStart",_objectIndexStart)
	    			                                    .put("objIdxEnd",_objectIndexEnd);
	    return tripleObject;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(_subject);
		
		if (_subjectNamedEntities.size() > 0) {
			sb.append("[");
			sb.append(String.join(",", _subjectNamedEntities));
			sb.append("]");
		}
		
		sb.append(",");
		sb.append(_relation);
		sb.append(",");
		sb.append(_object);
		
		if (_objectNamedEntities.size() > 0) {
			sb.append("[");
			sb.append(String.join(",", _objectNamedEntities));
			sb.append("]");
		}

		
		sb.append(",");
		sb.append(_confidenceScore);
		sb.append(",");
		sb.append(_sentenceIndex);
		
		if (_subjectDBPedia != null) {
			sb.append(", Subject DBPedia: "+ _subjectDBPedia.toString());
		}

		if (_objectDBPedia != null) {
			sb.append(", ObjectDBPedia: "+ _objectDBPedia.toString());
		}
	
		
		sb.append(")");
		
		/*
		if (_subjectDBPedia != null) {
			sb.append("\n\t");
			sb.append("Subject DBPedia: ");
			sb.append(_subjectDBPedia.toString());
		}
		
		if (_objectDBPedia != null) {
			sb.append("\n\t");
			sb.append("Object DBPedia: ");
			sb.append(_objectDBPedia.toString());
		}
		*/
		
		return sb.toString();
	}
	
	public static JSONArray toJSONArray(java.util.List<TripleRelation> relations) {
		JSONArray records = new JSONArray();
		for (TripleRelation tr: relations) {
			records.put(tr.toJSONObject());
		}
		return records;
	}

	private static final String[] jsonObjectNames = { "sentIndex","prefixBe","suffixBe","suffixOf","tMod", "conf","subj","rel","obj","subjIdxStart","subjIdxEnd","relIdxStart","relIdxEnd","objIdxStart","objIdxEnd" };
	private static final String[] columnNames = { "sentenceIndex","prefixBe","suffixBe","suffixOf","tMod", "confidence","subject","relation","object","subjectIndexStart","subjectIndexEnd","relationIndexStart","relationIndexEnd","objectIndexStart","objIndexEnd" };
	
	public static String toTabDelimitedString(java.util.List<TripleRelation> relations, String documentID) {
		String result = Export.convertJSONArrayToTabDelimited(toJSONArray(relations), jsonObjectNames, columnNames, true,documentID.toString());
		
		return result;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TripleRelation)) { return false;}
		TripleRelation other = (TripleRelation) o;
		
		return this.getSubject().equals(other.getSubject()) && this.getRelation().equals(other.getRelation()) && this.getObject().equals(other.getObject());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = result * prime + this.getSubject().hashCode();
		result = result * prime + this.getRelation().hashCode();
		result = result * prime + this.getObject().hashCode();
		
		return result;
	}
	
	public static enum FilterStyle {
			FILTER_TO_MAXIMIZE_TERMS,
			FILTER_TO_MINIMIZE_TERMS,
			FILTER_TO_MAXIMIZE_SUBJ_OBJ_MINIMIZE_RELATION
	}
	
	private static class WordLists {
		HashSet<String> setA;
		HashSet<String> setB;
		HashSet<String> intersection;
		
		public WordLists(String a, String b) {
			setA = new HashSet<String>(Arrays.asList(a.split(" ")));
			setB = new HashSet<String>(Arrays.asList(b.split(" ")));
			intersection = new HashSet<String>(setA);
			intersection.retainAll(setB);
		}
		
		public int compare(FilterStyle style) {
			if (intersection.size() == 0 || setA.size() == setB.size()) { return 0; }
			
			if (style == FilterStyle.FILTER_TO_MAXIMIZE_TERMS || style == FilterStyle.FILTER_TO_MAXIMIZE_SUBJ_OBJ_MINIMIZE_RELATION) {
				if (setA.size() > setB.size()) { return 1;}
				else {return -1; }
			}
			else { //style must be minimize
				if (setA.size() > setB.size()) { return -1;}
				else {return 1; }				
			}
		}
		
	}
	
	public static java.util.List<TripleRelation> reduceTriples(java.util.List<TripleRelation> receivedTriples, FilterStyle style) {
		java.util.ArrayList<TripleRelation> incomingTriples = new java.util.ArrayList<TripleRelation> ();
		
		for (TripleRelation tr: receivedTriples) {  // copy triples so that we don't have unattended side affects from the modification
			incomingTriples.add(new TripleRelation(tr));
		}
		
				
		
		for (int i = 0; i < incomingTriples.size()-1; i++) {
			for (int j = i+1; j < incomingTriples.size(); j++) {
				TripleRelation a = incomingTriples.get(i);
				TripleRelation b = incomingTriples.get(j);
				
				// only combine relations from the same sentence
				if (a.getSentenceIndex() != b.getSentenceIndex()) { continue; }
			
				WordLists wlSubject = new WordLists(a.getSubject(),b.getSubject());
				int subjectComparison = wlSubject.compare(style);
				if (subjectComparison == 1) {
					a._subjectSynonyms.add(b.getSubject());
					b.setSubject(a.getSubject());
					b.setSubjectIndexStart(a.getSubjectIndexStart());
					b.setSubjectIndexEnd(a.getSubjectIndexEnd());
				}
				else if (subjectComparison == -1) {
					b._subjectSynonyms.add(a.getSubject());
					a.setSubject(b.getSubject());
					a.setSubjectIndexStart(b.getSubjectIndexStart());
					a.setSubjectIndexEnd(b.getSubjectIndexEnd());											
				}
				
				WordLists wlRelation = new WordLists(a.getRelation(),b.getRelation());
				FilterStyle relationStyle = (style == FilterStyle.FILTER_TO_MAXIMIZE_SUBJ_OBJ_MINIMIZE_RELATION) ? FilterStyle.FILTER_TO_MINIMIZE_TERMS : style;
				int relationComparison = wlRelation.compare(relationStyle);
				if (relationComparison == 1) {
					b.setRelation(a.getRelation());
					b.setRelationIndexStart(a.getRelationIndexStart());
					b.setRelationIndexEnd(a.getRelationIndexEnd());				
				}
				else if (relationComparison == -1) {
					a.setRelation(b.getRelation());
					a.setRelationIndexStart(b.getRelationIndexStart());
					a.setRelationIndexEnd(b.getRelationIndexEnd());												
				}				
				
				WordLists wlObject = new WordLists(a.getObject(),b.getObject());
				int objectComparison = wlObject.compare(style);
				if (objectComparison == 1) {
					a._objectSynonyms.add(b.getObject());
					b.setObject(a.getObject());
					b.setObjectIndexStart(a.getObjectIndexStart());
					b.setObjectIndexEnd(a.getObjectIndexEnd());				
				}
				else if (objectComparison == -1) {
					b._objectSynonyms.add(a.getObject());
					a.setObject(b.getObject());
					a.setObjectIndexStart(b.getObjectIndexStart());
					a.setObjectIndexEnd(b.getObjectIndexEnd());										
				}				

			}			
		}
		
		HashSet<TripleRelation> unique = new HashSet<TripleRelation>(incomingTriples);

		return new ArrayList<TripleRelation>(unique);
	}


	/**
	 * Merges two lists of Triple Relations together.  If the same triple exists in both lists, the sources are combined together
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static List<TripleRelation> combine(List<TripleRelation> a, List<TripleRelation> b) {
		HashMap<TripleRelation, TripleRelation> result = new HashMap<TripleRelation, TripleRelation>();
		
		for (TripleRelation tr: a) {
			TripleRelation newTR = new TripleRelation(tr);
			result.put(newTR,newTR);
		}
		for (TripleRelation tr: b) {
			if (result.containsKey(tr)) {
				TripleRelation existing = result.get(tr);
				existing.addSource(tr.getSources());
			}
			else {
				TripleRelation newTR = new TripleRelation(tr);
				result.put(newTR,newTR);
			}
		}
		
		return new ArrayList<TripleRelation>(result.values());
		
	}
	
	
	//private HashSet<String> _subjectNamedEntities = new HashSet<String>();  // contains the type of any named entity attached to the subject
	//private HashSet<String> _objectNamedEntities = new HashSet<String>();	// contains the type of any named entity attached to the object
	//	public java.util.List<NamedEntity> getNamedEntities() { return _namedEntities;}
	
	public static void augmentSubjectObjectWithStanfordNER(List<TripleRelation> triples, List<NamedEntity> entities) {
		for (NamedEntity ne: entities) {
			String text = ne.getEntity().toLowerCase();
			if (text.contains(" ,")) {
				text = text.replaceAll(" ,", ",");
			}
			
			for (TripleRelation tr: triples) {
				if (tr.getSubject().toLowerCase().contains(text)) {
					//System.out.println("**** FOUND STANFORD NER IN SUBJECT  *********");
					tr._subjectNamedEntities.add(ne.getType());
				}
				if (tr.getObject().toLowerCase().contains(text)) {
					//System.out.println("**** FOUND STANFORD NER IN OBJECT  *********");
					tr._objectNamedEntities.add(ne.getType());
				}
			}
		}
	}
	
	public static void augmentSubjectObjectWithSpacyNER(List<TripleRelation> triples, JSONArray entities, int sentenceStartPos, int sentenceEndPos) {
		for (int i = 0; i < entities.length(); i++) {
			JSONObject jo = entities.getJSONObject(i);
			String text = jo.getString("text").toLowerCase();
			if (text.contains(" ,")) {
				text = text.replaceAll(" ,", ",");
			}
			int startPosition = jo.getInt("startPos");
			
			if (startPosition < sentenceStartPos) { continue; }
			if (startPosition > sentenceEndPos)   { break; }
 			
			
			for (TripleRelation tr: triples) {
				if (tr.getSubject().toLowerCase().contains(text)) {
					//System.out.println("**** FOUND SPACYNER IN SUBJECT  *********");
					tr._subjectNamedEntities.add(jo.getString("type"));
				}
				if (tr.getObject().toLowerCase().contains(text)) {
					//System.out.println("**** FOUND SPACYNER IN SUBJECT  *********");
					tr._objectNamedEntities.add(jo.getString("type"));
				}
			}
		}
	}

	public static void augmentSubjectObjectWithDBPedia(List<TripleRelation> triples, JSONArray dbpediaEntities, int sentenceStartPos, int sentenceEndPos) {
		for (int i = 0; i < dbpediaEntities.length(); i++) {
			JSONObject jo = dbpediaEntities.getJSONObject(i);
			String text = jo.getString("@surfaceForm");
			int startPosition = jo.getInt("@offset");
			
			if (startPosition < sentenceStartPos) { continue; }
			if (startPosition > sentenceEndPos)   { break; }
 			
			
			for (TripleRelation tr: triples) {
				if (tr.getSubject().equalsIgnoreCase(text)) {
					tr._subjectDBPedia = jo;
				}
				if (tr.getObject().equalsIgnoreCase(text)) {
					tr._objectDBPedia = jo;
				}
			}
		}
		
	}	
	
}
