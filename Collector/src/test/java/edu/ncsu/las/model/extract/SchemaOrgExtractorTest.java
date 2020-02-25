/**
 * 
 */
package edu.ncsu.las.model.extract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;

import edu.ncsu.las.util.FileUtilities;

/**
 * 
 */
public class SchemaOrgExtractorTest {

	/**
	 * Test method for {@link edu.ncsu.las.model.extract.HTMLStructuredDataExtractor#extractFromJSONLDScript(java.lang.String)}.
	 */
	@Test
	public void testExtractFromJSONLD() {
		
		try {
			//System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
			//byte data[] = FileUtilities.readAllBytesFromFile(new java.io.File("TestFiles\\schema.org_json_ld\\00000151-3898-d689-0a7e-051400007711"));	   // Test Files have been removed.	
			//assertEquals( 114392,data.length);
			String htmlContent; // = new String(data,StandardCharsets.UTF_8);
			
			htmlContent="<script type=\"application/ld+json\">{ \"@context\": \"http://schema.org/\", \"@type\": \"Service\", \"serviceType\": \"Weekly home cleaning\", \"provider\": { \"@type\": \"LocalBusiness\", \"name\": \"ACME Home Cleaning\" }, \"areaServed\": { \"@type\": \"State\", \"name\": \"Massachusetts\" }, \"hasOfferCatalog\": { \"@type\": \"OfferCatalog\", \"name\": \"Cleaning services\", \"itemListElement\": [ { \"@type\": \"OfferCatalog\", \"name\": \"House Cleaning\", \"itemListElement\": [ { \"@type\": \"Offer\", \"itemOffered\": { \"@type\": \"Service\", \"name\": \"Apartment light cleaning\" }}, { \"@type\": \"Offer\", \"itemOffered\": { \"@type\": \"Service\", \"name\": \"House light cleaning up to 2 bedrooms\" }}, { \"@type\": \"Offer\", \"itemOffered\": { \"@type\": \"Service\", \"name\": \"House light cleaning 3+ bedrooms\" }}]}, { \"@type\": \"OfferCatalog\", \"name\": \"One-time services\", \"itemListElement\": [ { \"@type\": \"Offer\", \"itemOffered\": { \"@type\": \"Service\", \"name\": \"Window washing\" }}, { \"@type\": \"Offer\", \"itemOffered\": { \"@type\": \"Service\", \"name\": \"Carpet cleaning\" }}, { \"@type\": \"Offer\", \"itemOffered\": { \"@type\": \"Service\", \"name\": \"Move in/out cleaning\" }}]}]}}</script>";
			
			HTMLStructuredDataExtractor soe = new HTMLStructuredDataExtractor();
			
			
			JSONObject result =soe.extractFromJSONLDScript(htmlContent);
			System.out.println(result.toString(4));
		}
		catch (IOException ex) {
			System.err.println(ex);
			fail("Unable to load file");
		}		
	}

	/**
	 * Test method for {@link edu.ncsu.las.model.extract.HTMLStructuredDataExtractor#extractFromMicroFormat(java.lang.String)}.
	 */
	@Test
	public void testExtractFromMicoformat() {
		try {
			//System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
			//byte data[] = FileUtilities.readAllBytesFromFile(new java.io.File("TestFiles\\schema.org_microformat\\00000151-3af4-c66a-0a7e-051400000454"));	  // Test Files have been removed.		
			//assertEquals( 497016,data.length);
			String htmlContent;// = new String(data,StandardCharsets.UTF_8);
			
			htmlContent = "<div itemscope itemtype=\"http://schema.org/Service\">  <meta itemprop=\"serviceType\" content=\"Home cleaning\" />  <span itemprop=\"provider\" itemscope itemtype=\"http://schema.org/LocalBusiness\">    <span itemprop=\"name\">ACME Home Cleaning</span>  </span>  offers a variety of services in  <span itemprop=\"areaServed\" itemscope itemtype=\"http://schema.org/State\">    <span itemprop=\"name\">Massachusetts</span>, including  </span>  <ul itemprop=\"hasOfferCatalog\" itemscope itemtype=\"http://schema.org/OfferCatalog\">    <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/OfferCatalog\">      <span itemprop=\"name\">House cleaning</span>      <ul itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/OfferCatalog\">        <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/Offer\">          <div itemprop=\"itemOffered\" itemscope itemtype=\"http://schema.org/Service\">            <span itemprop=\"name\">Apartment light cleaning</span>          </div>        </li>        <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/Offer\">          <div itemprop=\"itemOffered\" itemscope itemtype=\"http://schema.org/Service\">            <span itemprop=\"name\">House light cleaning up to 2 bedrooms</span>          </div>        </li>        <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/Offer\">          <div itemprop=\"itemOffered\" itemscope itemtype=\"http://schema.org/Service\">            <span itemprop=\"name\">House light cleaning 3+ bedrooms</span>          </div>        </li>      </ul>    <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/OfferCatalog\">      <span itemprop=\"name\">One-time services</span>      <ul itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/OfferCatalog\">        <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/Offer\">          <div itemprop=\"itemOffered\" itemscope itemtype=\"http://schema.org/Service\">            <span itemprop=\"name\">Window washing</span>          </div>        </li>        <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/Offer\">          <div itemprop=\"itemOffered\" itemscope itemtype=\"http://schema.org/Service\">            <span itemprop=\"name\">Carpet deep cleaning</span>          </div>        </li>        <li itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/Offer\">          <div itemprop=\"itemOffered\" itemscope itemtype=\"http://schema.org/Service\">            <span itemprop=\"name\">Move in/out cleaning</span>          </div>        </li>      </ul>    </li>  </ul></div>";
			
			JSONObject result = HTMLStructuredDataExtractor.extractAllFormats(htmlContent, new java.net.URL("http://blog.example.com/progress-report"));
			//JSONObject result = soe.convertMicroData(htmlContent, );
			System.out.println(result.toString(4));
			
		}
		catch (IOException ex) {
			System.err.println(ex);
			fail("Unable to load file");
		}
	}
	
	/**
	 * Test method for {@link edu.ncsu.las.model.extract.HTMLStructuredDataExtractor#convertRDFa(java.lang.String)}.
	 */
	@Test
	public void testExtractFromRDFa() {
		
		try {
			//System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
			//byte data[] = FileUtilities.readAllBytesFromFile(new java.io.File("TestFiles\\schema.org_json_ld\\00000151-3898-d689-0a7e-051400007711"));		
			//assertEquals( 114392,data.length);
			//String htmlContent = new String(data,StandardCharsets.UTF_8);
			
			String htmlContent="<body vocab=\"http://purl.org/dc/terms/\">   <div vocab=\"http://xmlns.com/foaf/0.1/\" resource=\"#me\">   <ul rel=\"knows\">      <li resource=\"http://example.com/bob/#me\" typeof=\"Person\">        <a property=\"homepage\" href=\"http://example.com/bob/\"><span property=\"name\">Bob</span></a>      </li>      <li resource=\"http://example.com/eve/#me\" typeof=\"Person\">        <a property=\"homepage\" href=\"http://example.com/eve/\"><span property=\"name\">Eve</span></a>      </li>      <li resource=\"http://example.com/manu/#me\" typeof=\"Person\">        <a property=\"homepage\" href=\"http://example.com/manu/\"><span property=\"name\">Manu</span></a>      </li>   </ul></div>            <div resource=\"/alice/posts/trouble_with_bob\">      <h2 property=\"title\">The trouble with Bob</h2>      <p>Date: <span property=\"created\">2011-09-10</span></p>      <h3 property=\"creator\">Alice</h3>      <link property=\"rdfa:copy\" href=\"#ccpattern\"/>    </div>   <div resource=\"/alice/posts/jims_concert\">      <h2 property=\"title\">I was at Jim's concert the other day</h2>      <p>Date: <span property=\"created\">2011-10-22</span></p>      <h3 property=\"creator\">Alice</h3>      <link property=\"rdfa:copy\" href=\"#ccpattern\"/>   </div>   <div resource=\"#ccpattern\" typeof=\"rdfa:Pattern\">      <p vocab=\"http://creativecommons.org/ns#\">All content on this blog item is licensed under        <a property=\"license\" href=\"http://creativecommons.org/licenses/by/3.0/\">          a Creative Commons License</a>. <span property=\"attributionName\">©2011 Alice Birpemswick</span>.</p>   </div></body> ";
			
			JSONObject result = HTMLStructuredDataExtractor.extractAllFormats(htmlContent, new java.net.URL("http://"));
			
			System.out.println(result.toString(4));
		}
		catch (IOException ex) {
			System.err.println(ex);
			fail("Unable to load file");
		}
	}

	
}
