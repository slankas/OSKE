/**
 * 
 */
package edu.ncsu.las.model.extract;



import java.io.IOException;

import org.json.JSONObject;

import static org.testng.Assert.fail;

import org.testng.annotations.Test;


/**
 * 
 */
public class HTMLStructuredDataExtractTest {

	/**
	 * Test method for {@link edu.ncsu.las.model.extract.HTMLStructuredDataExtractor#extractFromJSONLDScript(java.lang.String)}.
	 */
	@Test
	public void testExtractFromJSONLD() {
		
		try {
			//System.out.println(Paths.get(\".\").toAbsolutePath().normalize().toString());
			//byte data[] = FileUtilities.readAllBytesFromFile(new java.io.File(\"TestFiles\\schema.org_json_ld\\00000151-3898-d689-0a7e-051400007711\"));	//Note: the test files have been removed	
			//assertEquals( 114392,data.length);
			String htmlContent = "";
			
			htmlContent="\"<script type=\"application/ld+json\">" +
					 "{                    \"@context\": \"http://schema.org\",                       \"@type\": \"WebPage\",                     \"headline\": \"Noticias - Noticias Economía y mas\",                       \"url\": \"http://lta.reuters.com/\",             \"thumbnailUrl\": \"\",                      \"dateCreated\": \"\",                      \"articleSection\": \"\"\"\",                         \"creator\": [],                  \"keywords\": [\"Noticias\",\" Mundo\",\" Negocios\",\" Deporte\"]                       }"
					// "{                    \"@context\": \"http://schema.org\",                       \"@type\": \"WebPage\",                     \"headline\": \"\",                         \"url\": \"http://fr.reuters.com/assets/ticker\",                   \"thumbnailUrl\": \"\",                    \"dateCreated\": \"\",                      \"articleSection\": \"Landing \"\"\",                         \"creator\": [],                  \"keywords\": [\"\"]                }"
                    //"{ \n                  \"@context\": \"http://schema.org\", \n                  \"@type\": \"NewsArticle\", \n                  \"headline\": \"NASA Mars Orbiter Views Rover Climbing Mount Sharp\", \n                  //\"alternativeHeadline\": \"\", \n                  \"image\": \"//imagecache.jpl.nasa.gov/images/640x350/PIA21710-16-640x350.jpg\", \n                  \"datePublished\": \"2017-06-20 12:06:00\", \n                  \"description\": \"Using the most powerful telescope ever sent to Mars, NASA&#039;s Mars Reconnaissance Orbiter caught a view of the Curiosity rover this month amid rocky mountainside terrain.\" \n                  //\"articleBody\": \"\" \n}"
					//"{\"@context\":{\"@vocab\":\"http://schema.org\",\"articleId\":{\"@id\":\"Text\",\"@type\":\"@id\"}},\"@type\":\"NewsArticle\",\"headline\":\"The \\\"Queer Eye\\\" cast on the show’s first trans storyline, the French tuck and which one of the Fab Five needs a makeover\",\"description\":\"Tan France and Antoni Porowski, two of the hosts of Netflix's “Queer Eye,” joined NBC OUT and NBC THINK to chat about the show, diversity in the LGBTQ community and which TV characters really need a makeover.\",\"url\":\"https://www.nbcnews.com/think/video/the-queer-eye-cast-on-the-show-s-first-trans-storyline-the-french-tuck-and-which-one-of-the-fab-five-needs-a-makeover-1315262531949\",\"thumbnailUrl\":\"https://media3.s-nbcnews.com/j/MSNBC/Components/Video/201809/d_think_interview_queereye_180907.1024;768;7;70;3.jpg\",\"creator\":\"NBCNews.com\",\"alternativeHeadline\":\"The \\\"Queer Eye\\\" cast on the show’s first trans storyline, the French tuck and which one of the Fab Five needs a makeover\",\"dateCreated\":\"Fri Sep 07 2018 19:40:48 GMT+0000 (UTC)\",\"datePublished\":\"Fri Sep 07 2018 19:40:48 GMT+0000 (UTC)\",\"dateModified\":\"Fri Sep 07 2018 19:50:58 GMT+0000 (UTC)\",\"articleSection\":\"news\",\"articleId\":\"1315262531949\",\"identifier\":{\"@type\":\"PropertyValue\",\"propertyID\":\"uid\",\"value\":\"1315262531949\"},\"image\":{\"@type\":\"ImageObject\",\"url\":\"https://media3.s-nbcnews.com/i/MSNBC/Components/Video/201809/d_think_interview_queereye_180907.jpg\",\"width\":null,\"height\":null},\"author\":{\"@type\":\"Person\",\"name\":\"NBCNews.com\"},\"video\":{\"@type\":\"VideoObject\",\"name\":\"The \\\"Queer Eye\\\" cast on the show’s first trans storyline, the French tuck and which one of the Fab Five needs a makeover\",\"url\":\"https://www.nbcnews.com/think/video/the-queer-eye-cast-on-the-show-s-first-trans-storyline-the-french-tuck-and-which-one-of-the-fab-five-needs-a-makeover-1315262531949\",\"description\":\"Tan France and Antoni Porowski, two of the hosts of Netflix's “Queer Eye,” joined NBC OUT and NBC THINK to chat about the show, diversity in the LGBTQ community and which TV characters really need a makeover.\",\"thumbnailURL\":\"https://media3.s-nbcnews.com/j/MSNBC/Components/Video/201809/d_think_interview_queereye_180907.1024;768;7;70;3.jpg\",\"embedURL\":\"https://www.nbcnews.com/think/embedded-video/mmvo1315262531949\",\"duration\":\"PT5M32S\",\"uploadDate\":\"Fri Sep 07 2018 19:40:48 GMT+0000 (UTC)\"},\"publisher\":{\"@type\":\"Organization\",\"name\":\"NBC News\",\"logo\":{\"@type\":\"ImageObject\",\"url\":\"https://nodeassets.nbcnews.com/cdnassets/projects/site-images/nbcnews-logo-white.png\",\"width\":166,\"height\":24}},\"mainEntityOfPage\":{\"@type\":\"WebPage\",\"@id\":\"https://www.nbcnews.com/think/video/the-queer-eye-cast-on-the-show-s-first-trans-storyline-the-french-tuck-and-which-one-of-the-fab-five-needs-a-makeover-1315262531949\"}}"
					+ "</script>";
			
			HTMLStructuredDataExtractor soe = new HTMLStructuredDataExtractor();
			
			
			JSONObject result = soe.extractFromJSONLDScript(htmlContent);
			System.out.println(result.toString(4));
		}
		catch (IOException ex) {
			System.err.println(ex);
			fail("Unable to load file");
		}
		//fail(\"Not yet implemented\");
		
	}
	
}
