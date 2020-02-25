package edu.ncsu.las.time.model;

import static org.testng.Assert.assertEquals;

import java.time.Instant;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;


public class HeidelTimeTest {

	public static JSONArray processDocument(HeidelTime ht, JSONObject jsonDocument) throws Exception {

		String text = jsonDocument.getString("text");
		Instant date = Instant.parse(jsonDocument.getJSONObject("published_date").getString("date"));

		return ht.processDocument(text,date);
	}

	@Test
	public void testHeidelTime() throws Exception {
		HeidelTime.initialize("./config.props");

		JSONObject testObject = new JSONObject().put("published_date", new JSONObject().put("date","2017-04-04T15:22:30Z"))
				                                .put("text", "Yesterday, I bought a cat!.  It was born earlier this year.  Today, at 1:32pm, it went on the internet.  For Christmas 2015, I received a toy.");

		HeidelTimePool htp = new HeidelTimePool(new HeidelTimePoolFactory());

		HeidelTime ht = htp.borrowObject();
		processDocument(ht,testObject);
		htp.returnObject(ht);

		ht = htp.borrowObject();
		processDocument(ht,testObject);

		HeidelTime ht2 = htp.borrowObject();
		JSONArray result = processDocument(ht2,testObject);

		System.out.println(result.toString(4));
		assertEquals(result.length(),6);

		JSONObject firstResult = result.getJSONObject(0);
		assertEquals(firstResult.getString("text"),"Yesterday");


		assertEquals(htp.getNumActive(), 2);
		assertEquals(htp.getNumIdle(),0);

		htp.returnObject(ht);
		htp.returnObject(ht2);

		assertEquals(htp.getNumActive(), 0);
		assertEquals(htp.getNumIdle(),2);

		htp.close();
	}
}
