package edu.ncsu.las.storage.citation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;

public class IEEEDownloadPDF {
	private static Logger logger =Logger.getLogger(PubMedProcessor.class.getName());

	//TODO: to test:  https://www.ncbi.nlm.nih.gov/pubmed/28186905
	
	public static boolean download(CloseableHttpClient httpClient, URI ieeURL, HttpContent content, String pdfFileLocation) throws ClientProtocolException, IOException {
	
		String pdfLink = null;
		String contentString  = content.getContentDataAsString();
		String[] lines = contentString.split("\n");
		for (String line: lines) {
			if (line.contains("global.document.metadata")) {
				line = line.trim();
				String obj = line.substring(line.indexOf("=") +1,line.length()-1);
				try {
					JSONObject metaData = new JSONObject(obj);
					pdfLink = "http://ieeexplore.ieee.org" + metaData.getString("pdfPath");
				}
				catch (org.json.JSONException ex) {
					if (obj.contains("pdfPath")) {
						String pdfPath = obj.substring(obj.indexOf("pdfPath")+8);
						pdfPath = pdfPath.substring(obj.indexOf("\"")+1);
						pdfPath = pdfPath.substring(0,pdfPath.indexOf('"'));
						pdfLink = "https://ieeexplore.ieee.org" +pdfPath;
					}
				}
				break;
			}
		}
		if (pdfLink == null) {
			logger.log(Level.WARNING,"Unable to get pdfPath for IEEE document: "+ieeURL.toString());
			return false;
		}
		
		
		
		//referrer - view-source:http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7845607
		
		//"pdfPath": "/iel7/8857/4359833/07845607.pdf",
		// actual PDF document: http://ieeexplore.ieee.org/ielx7/8857/4359833/07845607.pdf?tp=&arnumber=7845607&isnumber=4359833
		
		int lastSlash  = pdfLink.lastIndexOf("/");
		int lastPeriod = pdfLink.lastIndexOf(".");
		String arsNumberStr = pdfLink.substring(lastSlash+1, lastPeriod);
		int arsNumber = Integer.parseInt(arsNumberStr);
		
		String isnumberString = pdfLink.substring(0,lastSlash-1);
		int lastIsSlash = isnumberString.lastIndexOf("/");
		String isNumber = isnumberString.substring(lastIsSlash+1);
		
		String referrer = "http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber="+arsNumber;
		pdfLink = pdfLink.replace("iel7", "ielx7");
		pdfLink = pdfLink.replace("iel5", "ielx5");
		pdfLink += "?tp=&arsnumber="+arsNumber+"&isnumber="+isNumber;
		//System.out.println(pdfLink);

		HttpContext context = new BasicHttpContext();
		RequestConfig rc = RequestConfig.custom().setCircularRedirectsAllowed(true).build();
		HttpGet getRequest = new HttpGet(pdfLink);
		getRequest.setConfig(rc);
		getRequest.addHeader("Referer", referrer);
		CloseableHttpResponse response = httpClient.execute(getRequest,context);
		
		// We may have had re-directs to get to the final page.  What is that last URL?
		URI url = getRequest.getURI();
		RedirectLocations locations = (RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
		if (locations != null) {
			url = locations.getAll().get(locations.getAll().size() - 1);
		}
		
		content = InternetUtilities.createHttpContent(url.toString(), response,true);
		response.close();
	
		if (content.contentType.equalsIgnoreCase("application/pdf") ||content.contentType.equalsIgnoreCase("application/x-pdf") ) {
			logger.log(Level.INFO, "Downloaded PDF: "+ url);
			InputStream inputStream = new ByteArrayInputStream(content.contentData);
			Files.copy(inputStream, Paths.get(pdfFileLocation));
			return true;
		}
		
		
		return false;
	}

}
