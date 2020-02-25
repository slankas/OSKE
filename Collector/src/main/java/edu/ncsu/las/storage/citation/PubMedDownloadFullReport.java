package edu.ncsu.las.storage.citation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.xmlbeans.impl.common.Levenshtein;
import org.json.JSONObject;
import org.jsoup.select.Elements;

import com.google.common.util.concurrent.RateLimiter;

import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.SearchRecord;
import edu.ncsu.las.source.AbstractSearchHandler;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.FileUtilities;
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;
import edu.ncsu.las.util.json.JSONUtilities;

/**
 * 
 *
 */
public class PubMedDownloadFullReport {
	private static Logger logger =Logger.getLogger(PubMedProcessor.class.getName());
	
	public static void main(String args[]) {
		String pdfDirectory = "C:\\pubmed\\pubmed\\extractPDFFiles";
		validatePDFFiles(pdfDirectory);
	}
	 
	
	
	
	public static void validatePDFFiles(String pdfDirectory) {
		
		for (File f: (new File(pdfDirectory)).listFiles() ) {
			validatePDFFile(f);
			
		}
	}
	
	public static void validatePDFFile(File pdfFile) {
		try {
			byte[] data = FileUtilities.readAllBytesFromFile(pdfFile);
			String initial = new String(data,0,4);
			if (initial.equals("%PDF") == false) {
				System.out.println(pdfFile.getName() +": "+ initial);
				pdfFile.delete();
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "File: "+pdfFile.getName() +", IOException: " +e.toString() );
			//pdfFile.delete();
		}
	}
	
	private static final RateLimiter rateLimiter = RateLimiter.create(3); 
	

	
	public static void downloadCitationFullReport(JSONObject record, String htmlFileLocation, String pdfFileLocation) {
		
		String pmid = record.getString("PMID");
		logger.log(Level.INFO, "------------------------------------");
		logger.log(Level.INFO, "download processing for "+pmid);

		
		if ( (new File(htmlFileLocation)).exists() ||   (new File(pdfFileLocation)).exists()  ) {
			logger.log(Level.WARNING, "Full content exists: "+ record.getJSONObject("Article").getString("ArticleTitle"));
			return; // have already downloaded the file
		}
		
		boolean result = downloadCitationFullReportViaPubMed(pmid, record, htmlFileLocation, pdfFileLocation);
		if (!result) { result = downloadCitationFullReportViaDOILink(pmid, record,htmlFileLocation,pdfFileLocation); }
		if (!result) { result = downloadCitationFullReportViaSearch(pmid, record,htmlFileLocation,pdfFileLocation); }
	}
	
	
	/**
	 * @param record
	 * @param htmlFileLocation
	 * @param pdfFileLocation 
	 */
	public static boolean downloadCitationFullReportViaPubMed(String pmid, JSONObject record, String htmlFileLocation, String pdfFileLocation) {
		String elinkURL  = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&retmode=ref&cmd=prlinks&id=" + pmid;
		
		// if the article is in PubMed's full-text archive, get it from there ....
		try {
			String pmcID = JSONUtilities.getAsString(record, "PubmedData.id.pmc", null);
			if (pmcID != null) {
				elinkURL = "https://www.ncbi.nlm.nih.gov/pmc/articles/"+pmcID+"/";
			}
		}
		catch (NullPointerException npe) {
			logger.log(Level.WARNING, "Unable to get pmcID record: " + record.toString(4));
			
			return false;
		}
		
		return downloadCitationFullReportWithStartingURL(pmid, elinkURL,record.getJSONObject("Article").getString("ArticleTitle"),htmlFileLocation,pdfFileLocation);
	}	
	
	/**
	 * @param record
	 * @param htmlFileLocation
	 * @param pdfFileLocation 
	 */
	public static boolean downloadCitationFullReportViaDOILink(String pmid, JSONObject record, String htmlFileLocation, String pdfFileLocation) {
		
		try {
			String doi = JSONUtilities.getAsString(record, "PubmedData.id.doi", null);
			if (doi != null) {
				String doiLink = "https://doi.org/" + doi;
				return downloadCitationFullReportWithStartingURL(pmid, doiLink,record.getJSONObject("Article").getString("ArticleTitle"),htmlFileLocation,pdfFileLocation);
			}
			else {
				logger.log(Level.INFO, "Unable to get DOI from record.");
			}
		}
		catch (NullPointerException npe) {
			logger.log(Level.INFO, "Unable to get DOI from record.");
		}
		
		return false;
	}	
		
	
	/**
	 * @param record
	 * @param htmlFileLocation
	 * @param pdfFileLocation 
	 */
	public static boolean downloadCitationFullReportViaSearch(String pmid, JSONObject record, String htmlFileLocation, String pdfFileLocation) {
		
		try {
			String articleTitle = JSONUtilities.getAsString(record, "Article.ArticleTitle", null);
			if (articleTitle.startsWith("[")) { articleTitle = articleTitle.substring(1); }
			if (articleTitle.endsWith("]"))   { articleTitle = articleTitle.substring(0, articleTitle.length()-1); }
			if (articleTitle.endsWith("."))   { articleTitle = articleTitle.substring(0, articleTitle.length()-1); }
			String articleTitleLower = articleTitle.toLowerCase();
			if (articleTitle != null) {
				System.out.println(articleTitle);
				AbstractSearchHandler ash = AbstractSearchHandler.getSourceHandler("google");

				java.util.List<SearchRecord> records = ash.generateSearchResults(Domain.DOMAIN_SYSTEM, articleTitle + " filetype:pdf",new JSONObject(),10, new JSONObject());   
				
				for (SearchRecord sr: records) {
					String resultTitle = sr.getName();
					String resultTitleLower = sr.getName().toLowerCase();
					
					
					if (resultTitleLower.contains(articleTitleLower)) {
						boolean result = downloadCitationFullReportWithStartingURL(pmid, sr.getUrl(),record.getJSONObject("Article").getString("ArticleTitle"),htmlFileLocation,pdfFileLocation);
						if (result) {return true;} 
					}
					double minLength = Math.min(resultTitle.length(), articleTitle.length());
					if ( (Levenshtein.distance(articleTitleLower, resultTitleLower)/minLength) < 0.1) { // only visit the URL if it is pretty similar to what we currently have
						boolean result = downloadCitationFullReportWithStartingURL(pmid, sr.getUrl(),record.getJSONObject("Article").getString("ArticleTitle"),htmlFileLocation,pdfFileLocation);
						if (result) {return true;} 
					}
				}		
			}
		}
		catch (NullPointerException npe) {
			logger.log(Level.INFO, "Unable to DOI from record.");
		}
		
		return false;
	}	
		
	
		
	
	
	
	/**
	 * For the given record, attempt to download the associated PDF document.
	 * 
	 * Approach:
	 * 1. Find the download link
	 * 
	 * 
	 * @param record
	 * @param htmlFileLocation
	 * @param pdfFileLocation 
	 */
	public static boolean downloadCitationFullReportWithStartingURL(String pmid, String startingURL, String  recordTitle, String htmlFileLocation, String pdfFileLocation) {
		
		rateLimiter.acquire();

		
		//String tempURL   ="http://doi.org/10.1016/j.tibs.2009.05.002";
		String userAgent = SourceHandlerInterface.getNextUserAgent(Domain.DOMAIN_SYSTEM);
		//String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36";
		CookieStore httpCookieStore = new BasicCookieStore();
		
		logger.log(Level.INFO, "Download processing: " + startingURL);
		
		HttpContext context = new BasicHttpContext();
		try (CloseableHttpClient httpClient =   HttpClients.custom().setUserAgent(userAgent).setDefaultCookieStore(httpCookieStore).setRedirectStrategy(new DefaultRedirectStrategy() {
            @Override
            protected URI createLocationURI(String location) throws ProtocolException  {
            	location = location.replaceAll(" ", "+");
	
            	return super.createLocationURI(location);
            }
        }).build()) {
			
			RequestConfig rc = RequestConfig.custom().setCircularRedirectsAllowed(true).build();
						
			//httpClient.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
			 
			HttpGet getRequest = new HttpGet(startingURL);
			getRequest.setConfig(rc);
			
			if (startingURL.contains("academia.edu")) {
				getRequest.addHeader("Referer","https://scholar.google.com/");
			}
			
			CloseableHttpResponse response = httpClient.execute(getRequest,context);
			
			// We may have had re-directs to get to the final page.  What is that last URL?
			URI finalUrl = getRequest.getURI();
			RedirectLocations locations = (RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
			if (locations != null) {
			    finalUrl = locations.getAll().get(locations.getAll().size() - 1);
			}
			
			
			//String finalURL = finalUrl.toString();
			System.out.println(finalUrl);
			
			HttpContent content = InternetUtilities.createHttpContent(finalUrl.toString(), response,true);
			response.close();
			
			if (finalUrl.getHost().contains("linkinghub.elsevier.com")) {
				String nextURL = null;
				
				Elements elsevierChoose = content.jsoupDocument.select("input[name=urls['sd']]");
				if (elsevierChoose.size() >0) {
					nextURL = elsevierChoose.first().val();
				}
				else {
					String redirectURL = content.jsoupDocument.select("input[name=redirectURL]").first().val();
					String key         = content.jsoupDocument.select("input[name=key]").first().val();
					String id          = content.jsoupDocument.select("input[name=id]").first().val();
					String resultName  = content.jsoupDocument.select("input[name=resultName]").first().val();
					
					nextURL = "https://"+finalUrl.getHost() + "/retrieve/" + resultName + "?Redirect="+redirectURL+"&key="+key;
				}
				
				getRequest = new HttpGet(nextURL);
				//getRequest.addHeader("Cookie","EUID=57900d4a-0c5e-4437-bf09-79bc238edb89; Search_AB_20170605=528; utt=87a5d4192c0f5118ded762-177e0eb205ba5b30-T440; AMCVS_4D6368F454EC41940A4C98A6%40AdobeOrg=1; sid=2e6565bf-34a0-4cb8-9751-d7bd6463bc54; acw=9724-4f7eff93f51c4b47bc352b27281b5a0b40c%7C%24%7C08A8680BC83DAEFB948D8CA60AF211B86DE2086E49D31B375BA49BAA2683BBAC4B234D212EDCB280B6E92CDE03F9DEA6E63D434AB32AF2D4A2F71CEDB4D144125F5B61073AFFCEE81EED741A422FEF400F08A4D9889A7A743C383D65C82D0651D74FDB647D9EF376E3B463751237AB4CC092E8A2BC3619D47A5C3B162CEE5B3F; USER_STATE_COOKIE=346fa8c434beaa18dbb50e2b7e6f20803c2c344c39e22674; sd_scs=3a2440b2-b5c9-11e7-9eb8-00000aacb361; s_sq=%5B%5BB%5D%5D; TARGET_URL=0bcb3afe4c678b54c70979827f7c38f36cdf67436621297559e865e4b18684e9831685d6e229158959534a30d2250dde1e930c26c18955c495c47e7d36fa618f9fd95c358af92feb322507fc22faf9a52bd16cb6efc667b50ccd2b5b67276ccfe4944849d44d0e810d45b271cfb7f18eabb76edda84468ad15050f0798360399f2299a3a52419001eefc47b0c9287f81c1aacfb34fe2091e8564d208802dceaac39c60c19b32ad052550dc80fb59a7d0; RETURN_URL=; CARS_COOKIE=763f41674d6f6bc3c23da21d18c3431fe9c3bc4d4c2f508ee3dc196025f06cfd834d9e48ec88b1814fd49b33c73110099a96d9ad0f1646d8; sd_session_id=a724-4f7eff93f51c4b47bc352b27281b5a0b40c; s_pers=%20v8%3D1508544250089%7C1603152250089%3B%20v8_s%3DLess%2520than%25201%2520day%7C1508546050089%3B%20c19%3Dsd%253Apdfft%253Apdf%253Aurl%7C1508546050094%3B%20v68%3D1508544249866%7C1508546050099%3B; s_cc=true; s_sess=%20v31%3D1507738963493%3B%20s_cpc%3D0%3B%20e41%3D1%3B%20s_ppvl%3Dsd%25253Apdfft%25253Apdf%25253Aurl%252C100%252C4371%252C918%252C1916%252C918%252C1920%252C1080%252C1%252CP%3B%20s_ppv%3Dsd%25253Apdfft%25253Apdf%25253Aurl%252C100%252C4371%252C918%252C1916%252C918%252C1920%252C1080%252C1%252CP%3B; DEFAULT_SESSION_SUBJECT=; __cp=1508555260316; ANONRA_COOKIE=465DFBA3C2007EED1E36A06BCBC77431D2BA7E6574A118E8952F998D2F5864EB50E69FEACD93E88A929A4513233FDFB64153B0685B2BD4A9; ak_bmsc=884E01C18EC47F2AF1E4D922CF8D55DCA88FF325600A00004B64EB59DEF53F3D~pl9Hh7s7FaIvg98bXCcJqiX/CbyuMzsOhVoIMce/OizPKjqx6LHKe0CytulczA48I5jMXzm1LekPhHw2hhRUGh6X1pe4WWgHoxEA6alYYelevV+jvSqiSCrB6o+UZfJqrY2tnHU5CSx4z0skKFMbvtwa4SzSdCB2UYL87UYVJ7J2Dc+T98sBZTJnXTE+j9HuNV2Uh0FCwbhhFXhIqlf+6hN9DdPymyYcJU4bc0XXR7I/c=; AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg=2096510701%7CMCIDTS%7C17460%7CMCMID%7C34689980440615356141151262900344748962%7CMCAID%7CNONE%7CMCOPTOUT-1508606061s%7CNONE%7CvVersion%7C2.0.0; MIAMISESSION=ec7b478e-c254-4ae2-9a4d-d7c03da47552:3686052563; bm_sv=D79F917F69F3444B32B1A2DC15387498~/nDDi9+HaruygKnp73qScdW68L/49YDmQ4EFGIyh1tEP4acpwo9l2vHP7O1qJ4YjUaCbR0zuKJ2zN4MjmSC1Jxl0AL3VAz1zxlMfj9Ad05cvs/JLqeEviC9T8NAeeDPGJiWYe50030vlfSBVugd1q/dveEJ1oTFwzAhpa1BbQko=; fingerPrintToken=d305b937117b0ae11b12671dd8eb68d5; RT=\"sl=1&ss=1508598859880&tt=1176&obo=0&sh=1508599766127%3D1%3A0%3A1176&dm=sciencedirect.com&si=8e140f4e-8390-49dd-84af-fc6eff859e12&bcn=%2F%2F36cc248b.akstat.io%2F&r=http%3A%2F%2Fwww.sciencedirect.com%2Fscience%2Farticle%2Fpii%2FS1055790310002393%3Fvia%253Dihub&ul=1508599788419\"");
				getRequest.setConfig(rc);
				context = new BasicHttpContext();
				response = httpClient.execute(getRequest,context);
				
				// We may have had re-directs to get to the final page.  What is that last URL?
				finalUrl = getRequest.getURI();
				locations = (RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
				if (locations != null) {
				    finalUrl = locations.getAll().get(locations.getAll().size() - 1);
				}
				
				content = InternetUtilities.createHttpContent(finalUrl.toString(), response,true);
				response.close();
				System.out.println("at "+finalUrl);
			}
			
			
			System.out.println("Full-text page: "+finalUrl);
			
			//InternetUtilities.HttpContent content = InternetUtilities.retrieveURL(elinkURL, userAgent, -5, true);
		
			//System.out.println(pmid);
			//System.out.println(record.toString(4));
			//System.out.println("======================================");
			//System.out.println(content.jsoupDocument);
			
			if (content.contentType.equalsIgnoreCase("application/pdf") ||content.contentType.equalsIgnoreCase("application/x-pdf") ) {
				//we've already got the content
				logger.log(Level.INFO, "Downloaded PDF directly at "+ finalUrl);
				InputStream inputStream = new ByteArrayInputStream(content.contentData);
			    Files.copy(inputStream, Paths.get(pdfFileLocation));
			    return true;
			}
			
			if (finalUrl.toString().contains("ieeexplore")) {
				return IEEEDownloadPDF.download(httpClient, finalUrl, content, pdfFileLocation);
			}
			
			Elements links = content.jsoupDocument.select("a#pdf-link:contains(Download PDF)");
			if (links.size() == 0) { links = content.jsoupDocument.select("meta[name=citation_pdf_url]"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("a:contains(Download PDF)"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("div.format-menu a:contains(PDF)"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("a.full_text_pdf"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("a[rel=view-full-text.pdf]"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("a:contains(Full-Text PDF)"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("a:contains(PDF)"); }
			if (links.size() == 0) { links = content.jsoupDocument.select("a[href$=.pdf]"); }
			if (links.size() == 0 ) {
				System.out.println("NO PDF LINK FOUND: " + finalUrl);
				System.out.println("PMID: "+pmid);
				return false;
			}
			else {			
				String urlPDF = links.first().attr("data-article-url");
				if ((urlPDF == null ||urlPDF.equals("")) && links.first().hasAttr("content")) {
					urlPDF = links.first().attr("content");
				}
				
				if (urlPDF == null || urlPDF.trim().equals("")) {
					urlPDF = links.first().attr("abs:href");
				}
				if (urlPDF.startsWith("/")) {
					urlPDF = finalUrl.getScheme()+"://"+finalUrl.getHost()+urlPDF;
				}
				
				if (urlPDF.contains("onlinelibrary.wiley.com")) {
					if (urlPDF.endsWith("/epdf")) {
						urlPDF = urlPDF.replace("/epdf", "/pdf");
						getRequest = new HttpGet(urlPDF);
						getRequest.setConfig(rc);
						context = new BasicHttpContext();
						response = httpClient.execute(getRequest,context);
						
						HttpContent wileyContent = InternetUtilities.createHttpContent(finalUrl.toString(), response,true);
						Elements frameLinks = wileyContent.jsoupDocument.select("iframe#pdfDocument");
						if (frameLinks.size() > 0) {
							urlPDF = frameLinks.first().attr("abs:src");
							System.out.println("Wiley: "+urlPDF);
						}
						// articles that still need to be purchase will "fall-out" from a bad content type on the download
					}	
				}
				
				
				if ( urlPDF.contains("sciencedirect.com")) {
					System.out.println("ScienceDirect: "+urlPDF);
					if (urlPDF.contains("ShoppingCartURL") == false) {
						urlPDF = ScienceDirectPDFLocator.noScript(context, httpClient, rc, finalUrl, urlPDF);						
					}
					if (urlPDF.contains("ShoppingCartURL")) {
						logger.log(Level.WARNING, "ScienceDirect: cotent most likely requires purchasing, skipping....");
						return false;
					}

					System.out.println("ScienceDirect(actual): "+urlPDF);			
				}
				
				urlPDF = urlPDF.replace("\n", "");
				urlPDF = urlPDF.replace("\t", "");
				urlPDF = urlPDF.replace(" ", "+");
				System.out.println(urlPDF);
				
				if (urlPDF.contains("embopress.org") && urlPDF.endsWith("pdf") == false) {
					urlPDF += ".full.pdf";
				}
				
				if (urlPDF.contains("sciencemag.org") && urlPDF.endsWith("/tab-pdf") ) {
					urlPDF = urlPDF.substring(0,urlPDF.indexOf("/tab-pdf")) + ".full.pdf"; 
				}
				
				
				logger.log(Level.INFO, "Link size "+links.size());
				//download the file.  note, we should loop through all of the found links until we successfully download the file.
				
						
				// At least these following sites have a link to a page ending in ".pdf+html", by removing the html, we get that actual PDF
				// link.  We'll just assume all links that have that ending, follow the pattern:  asm.org, cshlp.org, jbc.org, mcponline.org
				if (urlPDF.endsWith(".pdf+html")) {
					urlPDF = urlPDF.substring(0, urlPDF.indexOf("+html"));
				}
				
				logger.log(Level.INFO, "Downloading "+urlPDF);
				logger.log(Level.INFO, "title "+ recordTitle);
				logger.log(Level.INFO, "record "+ pmid);
				
				HttpGet httpget = new HttpGet(urlPDF);
				httpget.setConfig(rc);
				CloseableHttpResponse pdfRespnse = httpClient.execute(httpget);
				HttpEntity entity = pdfRespnse.getEntity();
				
				// need to check the content and possibly take some action  ...				
				if (entity != null) {
					if (entity.getContentType().getValue().toLowerCase().contains("pdf")) {
					    //long len = entity.getContentLength();
					    InputStream inputStream = entity.getContent();
					    Files.copy(inputStream, Paths.get(pdfFileLocation));
					    return true;
					}
					else {
						System.out.println("INVALID CONTENT: "+ entity.getContentType().getValue());
						return false;
					}
				}

				pdfRespnse.close();
			}
			
			
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Unable to download fulltext: "+e.toString(),e);
			
			RedirectLocations locations = (RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
			if (locations != null) {
			    System.out.println("possible issue: "+ locations.getAll().get(locations.getAll().size() - 1));
			}
		
			return false;
		}
		return false;
	}



		
}
