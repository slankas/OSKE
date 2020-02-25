package edu.ncsu.las.annotator;

import java.util.logging.Logger;

import org.testng.annotations.Test;


public class GeoTagAnnotatorTest {
	static final Logger srcLogger =Logger.getLogger(GeoTagAnnotatorTest.class.getName());



    @Test
    public void test1(){
    	GeoTagAnnotator gta = new GeoTagAnnotator();
    	gta.getSchema(); // make sure the schema is legit
    }


}
