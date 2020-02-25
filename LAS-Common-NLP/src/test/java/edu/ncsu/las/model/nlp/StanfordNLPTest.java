package edu.ncsu.las.model.nlp;


import java.util.List;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNLPTest {
	
    @BeforeClass
    public void setUp() throws Exception {


    }

    @AfterClass
    public void tearDown() throws Exception {

    }

    /**
     * Test 
     */
    @Test
    public void testSplit() {
		String text = "IBM announced their quarterly earnings on Friday.  The company will move to New York City.";
		
		List<String> splitSentences = StanfordNLP.splitSentences(text);
		assertEquals(2, splitSentences.size());
    }
    
    /**
     * Test 
     */
    @Test
    public void testFullAnnotation() {
		String text = "IBM announced their quarterly earnings on Friday.  The company will move to New York City. CEO Smith declared that the move save money. You can email smith at smith@ibm.com or visit his home page at https://ceo.ibm.com.";
	
		Annotation doc = StanfordNLP.annotateText(text);
	    List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
	    
	    int sentenceCount = 0;
	    for (CoreMap sentence : sentences) {
	    	String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
	    	
	    	java.util.List<Token> sentenceTokens = Token.extractTokens(sentence, ++sentenceCount);
	    	
	    	java.util.List<NamedEntity> namedEntities = NamedEntity.extractEntities(sentenceTokens); 
	    	
	    	for (NamedEntity ne: namedEntities) {
	    		System.out.println (ne.toJSONObject().toString(4));
	    	}
	    	
	    }
    }    
}
