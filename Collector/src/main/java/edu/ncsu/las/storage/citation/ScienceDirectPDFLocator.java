package edu.ncsu.las.storage.citation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.jsoup.select.Elements;

import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.source.SourceHandlerInterface;
import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;



public class ScienceDirectPDFLocator {
	
	public static String findURL(String url) throws IOException {
		int indexHostStart = url.indexOf("//") +2;
		int indexHostEnd   = url.indexOf("/",indexHostStart);
		
		String host = url.substring(indexHostStart,indexHostEnd);
		//String page = url.substring(indexHostEnd);
		
		return findURL(host,url);
	}
	
	public static String findURL(String host, String page) throws IOException {

		String userAgent = SourceHandlerInterface.getNextUserAgent(Domain.DOMAIN_SYSTEM);
		//String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";
		
		//Socket s = new Socket(InetAddress.getByName("www.sciencedirect.com"), 80);
		
	   // System.setProperty("javax.net.ssl.trustStore", "clienttrust");

	    SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
	    Socket s = ssf.createSocket(InetAddress.getByName("www.sciencedirect.com"), 443);
		
		
		PrintStream pw = new PrintStream(s.getOutputStream());
		pw.println("GET "+page+" HTTP/1.1");
		pw.println("Connection: keep-alive");
		pw.println("User-Agent: "+ userAgent+"");
		pw.println("Upgrade-Insecure-Requests: 1");
		pw.println("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		pw.println("Accept-Language: en-US,en;q=0.8");
		pw.println("Host: www.sciencedirect.com");
		
		//pw.println("Cookie: EUID=57900d4a-0c5e-4437-bf09-79bc238edb89; Search_AB_20170605=528; utt=87a5d4192c0f5118ded762-177e0eb205ba5b30-T440; AMCVS_4D6368F454EC41940A4C98A6%40AdobeOrg=1; sid=2e6565bf-34a0-4cb8-9751-d7bd6463bc54; acw=9724-4f7eff93f51c4b47bc352b27281b5a0b40c%7C%24%7C08A8680BC83DAEFB948D8CA60AF211B86DE2086E49D31B375BA49BAA2683BBAC4B234D212EDCB280B6E92CDE03F9DEA6E63D434AB32AF2D4A2F71CEDB4D144125F5B61073AFFCEE81EED741A422FEF400F08A4D9889A7A743C383D65C82D0651D74FDB647D9EF376E3B463751237AB4CC092E8A2BC3619D47A5C3B162CEE5B3F; USER_STATE_COOKIE=346fa8c434beaa18dbb50e2b7e6f20803c2c344c39e22674; sd_scs=3a2440b2-b5c9-11e7-9eb8-00000aacb361; s_sq=%5B%5BB%5D%5D; TARGET_URL=0bcb3afe4c678b54c70979827f7c38f36cdf67436621297559e865e4b18684e9831685d6e229158959534a30d2250dde1e930c26c18955c495c47e7d36fa618f9fd95c358af92feb322507fc22faf9a52bd16cb6efc667b50ccd2b5b67276ccfe4944849d44d0e810d45b271cfb7f18eabb76edda84468ad15050f0798360399f2299a3a52419001eefc47b0c9287f81c1aacfb34fe2091e8564d208802dceaac39c60c19b32ad052550dc80fb59a7d0; s_pers=%20v8%3D1508528008698%7C1603136008698%3B%20v8_s%3DLess%2520than%25201%2520day%7C1508529808698%3B%20c19%3Dsd%253Apdfft%253Apdf%253Aurl%7C1508529808705%3B%20v68%3D1508528008261%7C1508529808715%3B; s_cc=true; s_sess=%20v31%3D1507738963493%3B%20s_cpc%3D0%3B%20e41%3D1%3B%20s_ppvl%3Dsd%25253Apdfft%25253Apdf%25253Aurl%252C100%252C4400%252C924%252C1823%252C924%252C1920%252C1080%252C1%252CP%3B%20s_ppv%3Dsd%25253Apdfft%25253Apdf%25253Aurl%252C100%252C4400%252C924%252C1823%252C924%252C1920%252C1080%252C1%252CP%3B; CARS_COOKIE=763f41674d6f6bc3c23da21d18c3431fe9c3bc4d4c2f508ee3dc196025f06cfd834d9e48ec88b1814fd49b33c73110099a96d9ad0f1646d8; sd_session_id=a724-4f7eff93f51c4b47bc352b27281b5a0b40c; DEFAULT_SESSION_SUBJECT=; SD_ART_LINK_STATE=%3Ce%3E%3Cq%3Escience%3C%2Fq%3E%3Cenc%3EN%3C%2Fenc%3E%3C%2Fe%3E; RETURN_URL=; __cp=1508528963338; MIAMISESSION=09a72195-8548-4a29-bea3-3cf818075f4d:3685996760; ANONRA_COOKIE=465DFBA3C2007EED1E36A06BCBC77431D2BA7E6574A118E8952F998D2F5864EB50E69FEACD93E88A929A4513233FDFB64153B0685B2BD4A9; ak_bmsc=1A24DDF17FC630D2E944EF4C13E97A9ACC02F37CEB230000D88DEA59E8BAD122~plRbGybcMHAJ3z6W0KqPq6oLpLJLGV9yBDRhhesxc1gFe/s6Xg+llLm+a3qEhfE5E8hJsT+MZAFuwrGAfgIKKyf9YmkfEoGlGsUkqqBv4RqhbNOFOJRPKAWTnmIEKBe9Z88F2qkZ5cDRNOJIb10Hrb7mX1zp7WI6tTDFP1D9ZZ3AUy1agwD+o8fhiMBz4oqZR4MHLzbpPZBbgCCwrk5SmsHUWMQklkRWPKpv5UWAcbt+o=; AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg=2096510701%7CMCIDTS%7C17460%7CMCMID%7C34689980440615356141151262900344748962%7CMCAID%7CNONE%7CMCOPTOUT-1508551162s%7CNONE%7CvVersion%7C2.0.0; fingerPrintToken=d305b937117b0ae11b12671dd8eb68d5; RT=\"sl=1&ss=1508543961435&tt=1719&obo=0&sh=1508543963157%3D1%3A0%3A1719&dm=sciencedirect.com&si=8e140f4e-8390-49dd-84af-fc6eff859e12&bcn=%2F%2F36ebc1fb.akstat.io%2F&ld=1508543963157&r=https%3A%2F%2Fwww.sciencedirect.com%2Fscience%2Farticle%2Fpii%2FS1096717616301240&ul=1508543973946\"");
		pw.println();
		pw.flush();
		
		String location="";
		String cookie="";
		String contentType="";
		String statusLine="";
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String line;
		
		while((line = br.readLine()) != null) {
			//System.out.println(line);
			if (line.equals("")) {
				break;
			}
			if (line.startsWith("HTTP")) {
				statusLine = line.substring(line.indexOf(" ")+1);
			}
			if (line.startsWith("Content-Type")) {
				contentType = line.substring(line.indexOf(" ")+1);
			}
			if (line.startsWith("Location")) {
				location = line.substring(line.indexOf(" ")+1);
			}
			if (line.startsWith("Set-Cookie")) {
				cookie = cookie + line.substring(line.indexOf(" ")+1,line.indexOf(";")+1) + " ";
			}
		}
		
		if (statusLine.startsWith("3")) {  //eg, 301, 302 etc
			pw.println("GET "+page+" HTTP/1.1");
			pw.println("Host: www.sciencedirect.com");
			pw.println("Accept-Language: en-US,en;q=0.8");
			pw.println("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			pw.println("User-Agent: "+ userAgent+"");
			pw.println("Cookie: "+cookie);
			pw.println();
			pw.flush();			
			
			while((line = br.readLine()) != null) {
				//System.out.println(line);
				if (line.equals("")) {
					break;
				}
				if (line.startsWith("HTTP")) {
					statusLine = line.substring(line.indexOf(" ")+1);
				}
				if (line.startsWith("Content-Type")) {
					contentType = line.substring(line.indexOf(" ")+1);
				}
				if (line.startsWith("Location")) {
					location = line.substring(line.indexOf(" ")+1);
				}
				if (line.startsWith("Set-Cookie")) {
					cookie = cookie + line.substring(line.indexOf(" ")+1,line.indexOf(";")+1) + " ";
				}
			}			
			
		}
		
		
		String downloadURL=""; 
		statusLine = statusLine.trim();
		if (statusLine.startsWith("200")) {
			while((line = br.readLine()) != null) {
				//System.out.println(line);
				if (line.contains("<meta HTTP-EQUIV=\"Refresh\"")) {
					int indexOfURL = line.indexOf("URL")+4;
					int lastIndex  = line.lastIndexOf("\"");
					downloadURL= line.substring(indexOfURL,lastIndex);
					break;
				}
				if (line.contains("</html>")) {
					downloadURL = "ShoppingCartURL";
					break;
				}
			}
		}
		s.close();
		System.out.println(downloadURL);
	
		return downloadURL;
	}
	

	public static String noScript(HttpContext context, CloseableHttpClient httpClient, RequestConfig rc, URI finalUrl, String urlPDF) throws IOException, ClientProtocolException {
		HttpGet getSDRequest = new HttpGet(urlPDF);
		getSDRequest.setConfig(rc);
		
		CloseableHttpResponse sdResponse = httpClient.execute(getSDRequest,context);
		HttpContent sdContent = InternetUtilities.createHttpContent(finalUrl.toString(), sdResponse,true);
		sdResponse.close();

		Elements sdLinks = sdContent.jsoupDocument.select("div#redirect-message a");
		if (sdLinks.size() == 0 ) {
			urlPDF = ScienceDirectPDFLocator.findURL(urlPDF);
		}
		else {			
			urlPDF = sdLinks.first().attr("abs:href");
		}
		return urlPDF;
	}	
	
}
