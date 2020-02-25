package edu.ncsu.las.model.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;

import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

/**
 * Runs the Stanford CoreNLP (https://stanfordnlp.github.io/CoreNLP/).
 * Two processing pipelines are available:
 * 1) Simply splits a document into sentences
 * 2) Runs the full annotation on the passed in text and returns an edu.stanford.nlp.pipeline.Annotation 
 *    object. 
 * 
 *
 */
public class StanfordNLP {
	//private static Logger logger = Logger.getLogger(StanfordNLP.class.getName());


    //props.put("depparse.model", "edu/stanford/nlp/models/parser/nndep/english_SD.gz");

	
	private static final StanfordCoreNLP fullPipeline;
	private static final StanfordCoreNLP splitPipeline;
	private static final StanfordCoreNLP miniePipeline = CoreNLPUtils.StanfordDepNNParser();
	static {
	    Properties props = PropertiesUtils.asProperties(
	    		"annotators", "tokenize,ssplit,pos,lemma,ner,regexner,parse,coref,natlog,openie,sentiment",
	    		//"ssplit.isOneSentence", "true",
	    		//"parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz",
	    		//"openie.resolve_coref","true",
	    		//"tokenize.language", "en"
	    		//"openie.triple.all_nominals", "true",
	    		"openie.triple.strict","true"
	    		);
	    Properties splitProps = PropertiesUtils.asProperties(
	    		"annotators", "tokenize,ssplit"
	    		);
	    fullPipeline = new StanfordCoreNLP(props);
	    splitPipeline = new StanfordCoreNLP(splitProps);
	}

	/**
	 * splitSentences runs an abbreviated pipeline (tokens and sentence splitting) to
	 * split a document into its sentences for more detail processing.  (The Stanford CoreNLP
	 * does not like long, complex documents - increased memory and CPU loads).
	 * 
	 * @param text original text of a document
	 * @return List of sentences (with the original text) from the document.
	 */
	public static List<String> splitSentences(String text) {
		List<String> result = new ArrayList<String>();
		
		Annotation document = new Annotation(text);
		splitPipeline.annotate(document);
		
	    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
	    for (CoreMap sentence : sentences) {
	    	result.add(sentence.get(CoreAnnotations.TextAnnotation.class));
	    }
		
		return result;
	}
	
	
	/**
	 * Runs the full pipeline on the text.
	 * Annotators: tokenize,ssplit,pos,lemma,ner,regexner,parse,coref,natlog,openie,sentiment
	 * 
	 * @param text
	 * @param maxTokensPerSentence, use -1 for unlimited size
	 * @return
	 */
	public static Annotation annotateText(String text) {
		
		Annotation document = new Annotation(text);
		fullPipeline.annotate(document);
		
		return document;		
	}
	
	public static List<AnnotatedProposition> extractMinIETriples(String text) {
		MinIE minie = new MinIE(text, miniePipeline, MinIE.Mode.SAFE);
		return minie.getPropositions();
		
	}
	
	
	public static void main(String args[]) {
		String text = "IBM announced their quarterly earnings on Friday.  The company will move to New York City. CEO Bill Smith declared that the move save money. You can email him at smith@ibm.com or visit his home page at https://ceo.ibm.com.";
		text = "Hans-Georg Maassen was the guest of honor. He warned of the dangers of what’s known as “white propaganda”.";
		text = "The government has directed significant resources towards cyber security; in addition to the BSI and the BfV, the military has also added a cyber command team.";
		Document myDoc = Document.parse(text,2, new JSONArray(), new JSONArray());
		System.out.println(myDoc.toJSONObject().toString(4));
		System.out.println("================================");
		
		for (Sentence s:myDoc.getSentences()) {
			System.out.println(s.getCorefText());
			for (TripleRelation tr: s.getTripleRelations()) {
				System.out.println(tr);
			}
			System.out.println("================================");			
		}
		
		/*
		for (Sentence s:myDoc.getSentences()) {
	        // Print the extractions
	        System.out.println("\nInput sentence: " + s.getCorefText());
	        System.out.println("=============================");
	        System.out.println("Extractions:");
	        for (AnnotatedProposition ap: extractMinIETriples(s.getCorefText())) {
	        	
	        	//ap.getRelation().getWordCoreLabelList().get(0).index();
	        	
	            System.out.println("\tTriple: " + ap.getTripleAsString());
	            System.out.print("\tFactuality: " + ap.getFactualityAsString());
	            if (ap.getAttribution().getAttributionPhrase() != null) 
	                System.out.print("\tAttribution: " + ap.getAttribution().toStringCompact());
	            else
	                System.out.print("\tAttribution: NONE");
	            System.out.println("\n\t----------");
	        }

		}
		*/
        
        
        System.out.println("\n\nDONE!");
	}
}
