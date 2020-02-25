package edu.ncsu.las.collector.util;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.http.HttpHeaders;



/**
 *
 */
public class VoyantPostTest {

	@BeforeClass
	public void setUp() {

	}

	/**
	 * Test method for {Creating a trombone corpus for Voyant plugin with text input}.
	 */
	@Test
	public void testInputAsText() {

		// Uncomment to test for string input.

//		try {
//			String url = "http://127.0.0.1:8888/trombone";
//
//			String text = "So far in this article, we’ve discussed about how to implement a command line program in Java which is capable of upload files to any URL that can handle multipart request, without implementing an HTML upload form. This would be very useful in case we want to upload files to a web server programmatically.";
//
//			String params = "?tool=corpus.CorpusMetadata&input=" + URLEncoder.encode(text, "UTF-8");
//
//			url += params;
//
//			HttpClient client = HttpClientBuilder.create().build();
//
//			HttpPost request = new HttpPost(url);
//
//			// add request header
//			request.setHeader("Content-Type", "application/x-www-form-urlencoded");
//
//			// File:
//			HttpResponse response = null;
//			response = client.execute(request);
//
//			if (response.getStatusLine().getStatusCode() == 200) {
//				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//
//				StringBuffer result = new StringBuffer();
//				String line = "";
//				while ((line = rd.readLine()) != null) {
//					result.append(line);
//				}
//
//				System.out.println("Test with text:");
//				System.out.println(result);
//			} else {
//				System.out.println("Bad request: " + response.getStatusLine().getStatusCode());
//			}
//
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}

	}

	/**
	 * Test method for {Creating a trombone corpus for Voyant plugin with a zip file as input}.
	 */
	/*
	@Test
	public void testInputAsZip() {

		try {
	        String zipFile = "C:\\Users\\John\\Desktop\\files.zip";

	        String url = "http://serverNameOrIP:8888//trombone";

	        HttpClient client = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(url);

			// Set input file
			File inputFile = new File(zipFile);

			// Add file
			MultipartEntityBuilder mpEntity = MultipartEntityBuilder.create().addBinaryBody("upload", inputFile,ContentType.create("application/octet-stream"), inputFile.getName());

			// Add tool name. Required at server to process request
			mpEntity.addTextBody("tool", "corpus.CorpusCreator");

			HttpEntity httpEntity = mpEntity.build();
			request.setEntity(httpEntity);

			HttpResponse response = null;
			response = client.execute(request);

			if (response.getStatusLine().getStatusCode() == 200) {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

				System.out.println("Test with zip:");
				System.out.println("Response: " + response.getStatusLine().getStatusCode());
				System.out.println(result);
			} else {
				System.out.println("Bad request: " + response.getStatusLine().getStatusCode());
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	*/
}
