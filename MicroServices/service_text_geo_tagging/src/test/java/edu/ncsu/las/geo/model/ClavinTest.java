package edu.ncsu.las.geo.model;

import java.util.List;

import com.bericotech.clavin.resolver.ResolvedLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import edu.ncsu.las.geo.model.Clavin;
import edu.ncsu.las.geo.model.ClavinPool;
import edu.ncsu.las.geo.model.ClavinPoolFactory;


public class ClavinTest {

	public static ArrayNode processDocument(Clavin c, JsonNode jsonDocument) throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		String text = jsonDocument.get("text").asText();

		List<ResolvedLocation> resolvedLocations = c.parseArticle(text);

		ArrayNode locations = mapper.createArrayNode();

		for (ResolvedLocation resolvedLocation : resolvedLocations) {
			JsonNode details =  mapper.createObjectNode().put("textPosition", resolvedLocation.getLocation().getPosition())
					                             .put("textMatched", resolvedLocation.getLocation().getText())
					                             .put("matchedname", resolvedLocation.getMatchedName())
					                             .put("fuzzy", resolvedLocation.isFuzzy())
					                             .put("confidence",resolvedLocation.getConfidence())
					                             .set("geoname", mapper.valueToTree(resolvedLocation.getGeoname()));
			locations.add(details);
		}
		return locations;
	}


	public static void main(String[] args) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode testObject =  mapper.createObjectNode().put("published_date", "2017-04-04T15:22:30Z")
				                                          .put("text", "sample text used for testing.  replace as needed.");

		ClavinPool clavinPool = new ClavinPool(new ClavinPoolFactory());

		Clavin c = clavinPool.borrowObject();
		processDocument(c,testObject);
		clavinPool.returnObject(c);

		c = clavinPool.borrowObject();
		processDocument(c,testObject);

		Clavin c2 = clavinPool.borrowObject();
		processDocument(c2,testObject);

		System.out.println("Active: "+clavinPool.getNumActive());
		System.out.println("Idle: "+clavinPool.getNumIdle());

		clavinPool.returnObject(c);
		clavinPool.returnObject(c2);

		System.out.println("Active: "+clavinPool.getNumActive());
		System.out.println("Idle: "+clavinPool.getNumIdle());

		clavinPool.close();
	}
}
