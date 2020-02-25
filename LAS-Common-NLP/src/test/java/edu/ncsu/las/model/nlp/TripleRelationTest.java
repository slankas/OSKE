package edu.ncsu.las.model.nlp;

import edu.ncsu.las.model.nlp.StanfordNLP;
import edu.ncsu.las.model.nlp.TripleRelation;
import edu.stanford.nlp.pipeline.Annotation;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TripleRelationTest {

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
    	/*
		String text = "IBM announced their quarterly earnings on Friday.  The company will move to New York City.";
		
    	Annotation doc = StanfordNLP.annotateText(text);

    	java.util.List<TripleRelation> relations = TripleRelation.extractTriples(doc,0);
    	assertEquals(3, relations.size());
    	TripleRelation tr = relations.get(2);
    	assertEquals("company",tr.getSubject());
    	assertEquals("will move to",tr.getRelation());
    	assertEquals("new york city",tr.getObject());
    	
    	String tabFileContents = TripleRelation.toTabDelimitedString(relations, java.util.UUID.randomUUID().toString());
    	assertEquals(4,tabFileContents.split("\n").length);
    	
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
		*/
    }
    
    /**
     * Test 
     */
    @Test
    public void testLargerText() {
		String text = "Bob is has an Electrical Engineering degree. Bob went to University of Pittsburgh. Bob lives in North Carolina. Bob owns a car. Mazda manufactures cars. Honda manufactures cars. Ford manufactures cars. Lotus manufactures cars. Bob owns a Mazda. Mazda is a Japanese Company. Honda is a Japanese Company. Lotus is a British Company. Ford is an American Company.\r\n" + 
				"Bob lived previously lived in Baltimore and Lancaster. He also worked in Texas, Georgia, and Hawaii.\r\n";
		
    	Annotation doc = StanfordNLP.annotateText(text);

    	java.util.List<TripleRelation> relations = TripleRelation.extractTriples(doc,0);
    	relations.stream().forEach(System.out::println);

    	

    }   
    
}
