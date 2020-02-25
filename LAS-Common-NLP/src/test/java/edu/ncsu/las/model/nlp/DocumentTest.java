package edu.ncsu.las.model.nlp;

import edu.ncsu.las.model.nlp.Document;
import edu.ncsu.las.model.nlp.Sentence;
import edu.ncsu.las.model.nlp.TripleRelation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DocumentTest {
	
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
    public void testInitialAnnotations() {
		String text = "IBM announced their quarterly earnings on Friday.  The company will move to New York City.";
		
		/*
		java.util.UUID documentID  = java.util.UUID.randomUUID();
		Document myDocument = new Document(documentID,text,-1,1);

		//System.out.println(myDocument.toJSONObject().toString());

    	java.util.List<TripleRelation> relations = myDocument.getTriples();
    	assertEquals(3, relations.size());
    	TripleRelation tr = relations.get(2);
    	assertEquals("company",tr.getSubject());
    	assertEquals("will move to",tr.getRelation());
    	assertEquals("New York City",tr.getObject());
    	
    	String tabFileContents = TripleRelation.toTabDelimitedString(relations,documentID.toString());
    	//System.out.println(tabFileContents);
    	assertEquals(4,tabFileContents.split("\n").length);
    	
    	String tokenFileContents = Sentence.toTabDelimitedString(documentID.toString(), myDocument.getSentences());
    	//System.out.println(tokenFileContents);
    	assertEquals(18,tokenFileContents.split("\n").length);
    	
    	byte[] bytes = tabFileContents.getBytes(StandardCharsets.UTF_8);
    	ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    	InputStreamReader isr = new InputStreamReader(bais);
    	
   		try {
			relations = TripleRelation.fromTabDelimitedStreamReader(isr,true);
			isr.close();
			assertEquals(3,relations.size());
			tr = relations.get(2);
	    	assertEquals("company",tr.getSubject());
	    	assertEquals("will move to",tr.getRelation());
	    	assertEquals("New York City",tr.getObject());
		} catch (IOException e) {
			fail(e.toString());
		}

    	byte[] sentenceBytes = tokenFileContents.getBytes(StandardCharsets.UTF_8);
    	ByteArrayInputStream baisSentence = new ByteArrayInputStream(sentenceBytes);
    	InputStreamReader isrSentence = new InputStreamReader(baisSentence);
    	
   		try {
   			java.util.List<Sentence> sentences = Sentence.fromTabDelimitedStreamReader(isrSentence,true);
			isr.close();
			assertEquals(2,sentences.size());
			Sentence s = sentences.get(1);
	    	//System.out.println(s.getText());
	    	assertEquals("The company will move to New York City .",s.getText());
	    	assertEquals(9,s.getTokens().size());
	    	
		} catch (IOException e) {
			fail(e.toString());
		}   		
    	*/
    }
}
