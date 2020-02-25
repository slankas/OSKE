package edu.ncsu.las.util.json;

import static edu.ncsu.las.util.json.JSONMinify.*;

import org.testng.annotations.Test;

public class JSONMinifyTest {
	
	@Test
	public void testMinify() {
		System.out.println("!! Assertions will only be executed if the -ea vm argument is set !!");
	
		System.out.println("Running test1...");
	    String test1 = "// this is a JSON file with comments\n" +
	    		"{\n" +
	    		"\"foo\": \"bar\",    // this is cool\n" +
	    		"\"bar\": [\n" +
	    		"\"baz\", \"bum\", \"zam\"\n" +
	    		"],\n" +
	    		"/* the rest of this document is just fluff\n" + 
	    		"in case you are interested. */\n" + 
	    		"\"something\": 10,\n" + 
	    		"\"else\": 20\n" +
	    		"}\n" +
		    	"/* NOTE: You can easily strip the whitespace and comments\n" +
		    	"   from such a file with the JSON.minify() project hosted \n"+
		    	"   here on github at http://github.com/getify/JSON.minify \n"+
		    	"*/";
	     
	    String test1_res = "{\"foo\":\"bar\",\"bar\":[\"baz\",\"bum\",\"zam\"],\"something\":10,\"else\":20}";
	    assert(minify(test1).equals(test1_res));
	    
		System.out.println("Running test2..."); 
	    String test2 = "{\"/*\":\"*/\",\"//\":\"\",/*\"//\"*/\"/*/\"://\n" +
	    		       "\"//\"}" +
			           "";
	    
	    String test2_res = "{\"/*\":\"*/\",\"//\":\"\",\"/*/\":\"//\"}";
	    assert(minify(test2).equals(test2_res));
	    
		System.out.println("Running test3...");	    
	    String test3 =  "/*\n" +
					    "this is a\n" +
					    "multi line comment */{\n" +
				        "\n" +
					    "\"foo\"\n" +
					    ":" +
					    "    \"bar/*\"// something\n" +
					    "    ,    \"b\\\"az\":/*\n" +
					    "something else */\"blah\"\n" +
				        "\n" +
					    "}";
	    
	    String test3_res = "{\"foo\":\"bar/*\",\"b\\\"az\":\"blah\"}";
	    assert(minify(test3).equals(test3_res));

		System.out.println("Running test4...");
	    String test4 = "{\"foo\": \"ba\\\"r//\", \"bar\\\\\": \"b\\\\\\\"a/*z\", \n" + 
	    	    	   "\"baz\\\\\\\\\": /*  yay */ \"fo\\\\\\\\\\\"*/o\"\n" + 
	    	    		"}"; 
	    String test4_res = "{\"foo\":\"ba\\\"r//\",\"bar\\\\\":\"b\\\\\\\"a/*z\",\"baz\\\\\\\\\":\"fo\\\\\\\\\\\"*/o\"}";
	    assert(minify(test4).equals(test4_res));

		System.out.println("Running test5...");
	    String test5 = 	"// this is a comment //\n" +
	    		"{ // another comment\n" +
	    		"   true, \"foo\", // 3rd comment\n" +
	    		"   \"http://www.ariba.com\" // comment after URL\n" +
	    		"   \n" +
	    		"}";
	    String test5_res = "{true,\"foo\",\"http://www.ariba.com\"}";
	    assert(minify(test5).equals(test5_res));
	    System.out.println(test5);
	    System.out.println(minify(test5));
	}

}
