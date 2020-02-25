package edu.ncsu.las.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.net.whois.WhoisClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.net.InternetDomainName;

import edu.ncsu.las.util.InternetUtilities;
import edu.ncsu.las.util.InternetUtilities.HttpContent;



/**
 * Provides a REST interface to retrieve information from whois
 * 
 *
 */
@Path("v1")
public class WhoIsService {
	private static Logger logger =Logger.getLogger(WhoIsService.class.getName());
    
	private static long minimumProcessingTimeMS = Long.MAX_VALUE;
	private static long maximumProcessingTimeMS = Long.MIN_VALUE;
	
	private static long minimumAcquisitionTimeMS = Long.MAX_VALUE;
	private static long maximumAcquisitionTimeMS = Long.MIN_VALUE;
	
	private static long totalProcessingTimeMS   = 0;
	private static long totalAcquisitionTimeMS   = 0;

	private static long totalRequestCount = 0;
	
	public static final int MAX_CONCURRENT_REQUESTS = 5;
	public static final int MAX_CACHE_SIZE = 10000;
	
	private static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36";
	public static void setUserAgent(String newValue) { userAgent = newValue; }
	
	public static final HashMap<String, String> WHOIS_SERVERS  = new HashMap<String, String>();
	static {
		  // whois server list from 
		  // http://www.nirsoft.net/whois-servers.txt 
		WHOIS_SERVERS.put("ac", "whois.nic.ac");
		WHOIS_SERVERS.put("ad", "whois.ripe.net");
		WHOIS_SERVERS.put("ae", "whois.aeda.net.ae");
		WHOIS_SERVERS.put("aero", "whois.aero");
		WHOIS_SERVERS.put("af", "whois.nic.af");
		WHOIS_SERVERS.put("ag", "whois.nic.ag");
		WHOIS_SERVERS.put("ai", "whois.ai");
		WHOIS_SERVERS.put("al", "whois.ripe.net");
		WHOIS_SERVERS.put("am", "whois.amnic.net");
		WHOIS_SERVERS.put("as", "whois.nic.as");
		WHOIS_SERVERS.put("asia", "whois.nic.asia");
		WHOIS_SERVERS.put("at", "whois.nic.at");
		WHOIS_SERVERS.put("au", "whois.aunic.net");
		WHOIS_SERVERS.put("aw", "whois.nic.aw");
		WHOIS_SERVERS.put("ax", "whois.ax");
		WHOIS_SERVERS.put("az", "whois.ripe.net");
		WHOIS_SERVERS.put("ba", "whois.ripe.net");
		WHOIS_SERVERS.put("bar", "whois.nic.bar");
		WHOIS_SERVERS.put("be", "whois.dns.be");
		WHOIS_SERVERS.put("berlin", "whois.nic.berlin");
		WHOIS_SERVERS.put("best", "whois.nic.best");
		WHOIS_SERVERS.put("bg", "whois.register.bg");
		WHOIS_SERVERS.put("bi", "whois.nic.bi");
		WHOIS_SERVERS.put("biz", "whois.neulevel.biz");
		WHOIS_SERVERS.put("bj", "www.nic.bj");
		WHOIS_SERVERS.put("bo", "whois.nic.bo");
		WHOIS_SERVERS.put("br", "whois.nic.br");
		WHOIS_SERVERS.put("br.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("bt", "whois.netnames.net");
		WHOIS_SERVERS.put("bw", "whois.nic.net.bw");
		WHOIS_SERVERS.put("by", "whois.cctld.by");
		WHOIS_SERVERS.put("bz", "whois.belizenic.bz");
		WHOIS_SERVERS.put("bzh", "whois-bzh.nic.fr");
		WHOIS_SERVERS.put("ca", "whois.cira.ca");
		WHOIS_SERVERS.put("cat", "whois.cat");
		WHOIS_SERVERS.put("cc", "whois.nic.cc");
		WHOIS_SERVERS.put("cd", "whois.nic.cd");
		WHOIS_SERVERS.put("ceo", "whois.nic.ceo");
		WHOIS_SERVERS.put("cf", "whois.dot.cf");
		WHOIS_SERVERS.put("ch", "whois.nic.ch");
		WHOIS_SERVERS.put("ci", "whois.nic.ci");
		WHOIS_SERVERS.put("ck", "whois.nic.ck");
		WHOIS_SERVERS.put("cl", "whois.nic.cl");
		WHOIS_SERVERS.put("cloud", "whois.nic.cloud");
		WHOIS_SERVERS.put("club", "whois.nic.club");
		WHOIS_SERVERS.put("cn", "whois.cnnic.net.cn");
		WHOIS_SERVERS.put("cn.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("co", "whois.nic.co");
		WHOIS_SERVERS.put("co.nl", "whois.co.nl");
		WHOIS_SERVERS.put("co.uk", "whois.nic.uk");
		WHOIS_SERVERS.put("com", "whois.verisign-grs.com");
		WHOIS_SERVERS.put("coop", "whois.nic.coop");
		WHOIS_SERVERS.put("cx", "whois.nic.cx");
		WHOIS_SERVERS.put("cy", "whois.ripe.net");
		WHOIS_SERVERS.put("cz", "whois.nic.cz");
		WHOIS_SERVERS.put("de", "whois.denic.de");
		WHOIS_SERVERS.put("dk", "whois.dk-hostmaster.dk");
		WHOIS_SERVERS.put("dm", "whois.nic.cx");
		WHOIS_SERVERS.put("dz", "whois.nic.dz");
		WHOIS_SERVERS.put("ec", "whois.nic.ec");
		WHOIS_SERVERS.put("edu", "whois.educause.net");
		WHOIS_SERVERS.put("ee", "whois.tld.ee");
		WHOIS_SERVERS.put("eg", "whois.ripe.net");
		WHOIS_SERVERS.put("es", "whois.nic.es");
		WHOIS_SERVERS.put("eu", "whois.eu");
		WHOIS_SERVERS.put("eu.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("eus", "whois.nic.eus");
		WHOIS_SERVERS.put("fi", "whois.fi");
		WHOIS_SERVERS.put("fo", "whois.nic.fo");
		WHOIS_SERVERS.put("fr", "whois.nic.fr");
		WHOIS_SERVERS.put("gb", "whois.ripe.net");
		WHOIS_SERVERS.put("gb.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("gb.net", "whois.centralnic.com");
		WHOIS_SERVERS.put("qc.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("ge", "whois.ripe.net");
		WHOIS_SERVERS.put("gg", "whois.gg");
		WHOIS_SERVERS.put("gi", "whois2.afilias-grs.net");
		WHOIS_SERVERS.put("gl", "whois.nic.gl");
		WHOIS_SERVERS.put("gm", "whois.ripe.net");
		WHOIS_SERVERS.put("gov", "whois.nic.gov");
		WHOIS_SERVERS.put("gr", "whois.ripe.net");
		WHOIS_SERVERS.put("gs", "whois.nic.gs");
		WHOIS_SERVERS.put("gy", "whois.registry.gy");
		WHOIS_SERVERS.put("hamburg", "whois.nic.hamburg");
		WHOIS_SERVERS.put("hiphop", "whois.uniregistry.net");
		WHOIS_SERVERS.put("hk", "whois.hknic.net.hk");
		WHOIS_SERVERS.put("hm", "whois.registry.hm");
		WHOIS_SERVERS.put("hn", "whois2.afilias-grs.net");
		WHOIS_SERVERS.put("host", "whois.nic.host");
		WHOIS_SERVERS.put("hr", "whois.dns.hr");
		WHOIS_SERVERS.put("ht", "whois.nic.ht");
		WHOIS_SERVERS.put("hu", "whois.nic.hu");
		WHOIS_SERVERS.put("hu.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("id", "whois.pandi.or.id");
		WHOIS_SERVERS.put("ie", "whois.domainregistry.ie");
		WHOIS_SERVERS.put("il", "whois.isoc.org.il");
		WHOIS_SERVERS.put("im", "whois.nic.im");
		WHOIS_SERVERS.put("in", "whois.inregistry.net");
		WHOIS_SERVERS.put("info", "whois.afilias.info");
		WHOIS_SERVERS.put("ing", "domain-registry-whois.l.google.com");
		WHOIS_SERVERS.put("ink", "whois.centralnic.com");
		WHOIS_SERVERS.put("int", "whois.isi.edu");
		WHOIS_SERVERS.put("io", "whois.nic.io");
		WHOIS_SERVERS.put("iq", "whois.cmc.iq");
		WHOIS_SERVERS.put("ir", "whois.nic.ir");
		WHOIS_SERVERS.put("is", "whois.isnic.is");
		WHOIS_SERVERS.put("it", "whois.nic.it");
		WHOIS_SERVERS.put("je", "whois.je");
		WHOIS_SERVERS.put("jobs", "jobswhois.verisign-grs.com");
		WHOIS_SERVERS.put("jp", "whois.jprs.jp");
		WHOIS_SERVERS.put("ke", "whois.kenic.or.ke");
		WHOIS_SERVERS.put("kg", "whois.domain.kg");
		WHOIS_SERVERS.put("ki", "whois.nic.ki");
		WHOIS_SERVERS.put("kr", "whois.kr");
		WHOIS_SERVERS.put("kz", "whois.nic.kz");
		WHOIS_SERVERS.put("la", "whois2.afilias-grs.net");
		WHOIS_SERVERS.put("li", "whois.nic.li");
		WHOIS_SERVERS.put("london", "whois.nic.london");
		WHOIS_SERVERS.put("lt", "whois.domreg.lt");
		WHOIS_SERVERS.put("lu", "whois.restena.lu");
		WHOIS_SERVERS.put("lv", "whois.nic.lv");
		WHOIS_SERVERS.put("ly", "whois.lydomains.com");
		WHOIS_SERVERS.put("ma", "whois.iam.net.ma");
		WHOIS_SERVERS.put("mc", "whois.ripe.net");
		WHOIS_SERVERS.put("md", "whois.nic.md");
		WHOIS_SERVERS.put("me", "whois.nic.me");
		WHOIS_SERVERS.put("mg", "whois.nic.mg");
		WHOIS_SERVERS.put("mil", "whois.nic.mil");
		WHOIS_SERVERS.put("mk", "whois.ripe.net");
		WHOIS_SERVERS.put("ml", "whois.dot.ml");
		WHOIS_SERVERS.put("mo", "whois.monic.mo");
		WHOIS_SERVERS.put("mobi", "whois.dotmobiregistry.net");
		WHOIS_SERVERS.put("ms", "whois.nic.ms");
		WHOIS_SERVERS.put("mt", "whois.ripe.net");
		WHOIS_SERVERS.put("mu", "whois.nic.mu");
		WHOIS_SERVERS.put("museum", "whois.museum");
		WHOIS_SERVERS.put("mx", "whois.nic.mx");
		WHOIS_SERVERS.put("my", "whois.mynic.net.my");
		WHOIS_SERVERS.put("mz", "whois.nic.mz");
		WHOIS_SERVERS.put("na", "whois.na-nic.com.na");
		WHOIS_SERVERS.put("name", "whois.nic.name");
		WHOIS_SERVERS.put("nc", "whois.nc");
		WHOIS_SERVERS.put("net", "whois.verisign-grs.com");
		WHOIS_SERVERS.put("nf", "whois.nic.cx");
		WHOIS_SERVERS.put("ng", "whois.nic.net.ng");
		WHOIS_SERVERS.put("nl", "whois.domain-registry.nl");
		WHOIS_SERVERS.put("no", "whois.norid.no");
		WHOIS_SERVERS.put("no.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("nu", "whois.nic.nu");
		WHOIS_SERVERS.put("nz", "whois.srs.net.nz");
		WHOIS_SERVERS.put("om", "whois.registry.om");
		WHOIS_SERVERS.put("ong", "whois.publicinterestregistry.net");
		WHOIS_SERVERS.put("ooo", "whois.nic.ooo");
		WHOIS_SERVERS.put("org", "whois.pir.org");
		WHOIS_SERVERS.put("paris", "whois-paris.nic.fr");
		WHOIS_SERVERS.put("pe", "kero.yachay.pe");
		WHOIS_SERVERS.put("pf", "whois.registry.pf");
		WHOIS_SERVERS.put("pics", "whois.uniregistry.net");
		WHOIS_SERVERS.put("pl", "whois.dns.pl");
		WHOIS_SERVERS.put("pm", "whois.nic.pm");
		WHOIS_SERVERS.put("pr", "whois.nic.pr");
		WHOIS_SERVERS.put("press", "whois.nic.press");
		WHOIS_SERVERS.put("pro", "whois.registrypro.pro");
		WHOIS_SERVERS.put("pt", "whois.dns.pt");
		WHOIS_SERVERS.put("pub", "whois.unitedtld.com");
		WHOIS_SERVERS.put("pw", "whois.nic.pw");
		WHOIS_SERVERS.put("qa", "whois.registry.qa");
		WHOIS_SERVERS.put("re", "whois.nic.re");
		WHOIS_SERVERS.put("ro", "whois.rotld.ro");
		WHOIS_SERVERS.put("rs", "whois.rnids.rs");
		WHOIS_SERVERS.put("ru", "whois.tcinet.ru");
		WHOIS_SERVERS.put("sa", "saudinic.net.sa");
		WHOIS_SERVERS.put("sa.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("sb", "whois.nic.net.sb");
		WHOIS_SERVERS.put("sc", "whois2.afilias-grs.net");
		WHOIS_SERVERS.put("se", "whois.nic-se.se");
		WHOIS_SERVERS.put("se.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("se.net", "whois.centralnic.com");
		WHOIS_SERVERS.put("sg", "whois.nic.net.sg");
		WHOIS_SERVERS.put("sh", "whois.nic.sh");
		WHOIS_SERVERS.put("si", "whois.arnes.si");
		WHOIS_SERVERS.put("sk", "whois.sk-nic.sk");
		WHOIS_SERVERS.put("sm", "whois.nic.sm");
		WHOIS_SERVERS.put("st", "whois.nic.st");
		WHOIS_SERVERS.put("so", "whois.nic.so");
		WHOIS_SERVERS.put("su", "whois.tcinet.ru");
		WHOIS_SERVERS.put("sx", "whois.sx");
		WHOIS_SERVERS.put("sy", "whois.tld.sy");
		WHOIS_SERVERS.put("tc", "whois.adamsnames.tc");
		WHOIS_SERVERS.put("tel", "whois.nic.tel");
		WHOIS_SERVERS.put("tf", "whois.nic.tf");
		WHOIS_SERVERS.put("th", "whois.thnic.net");
		WHOIS_SERVERS.put("tj", "whois.nic.tj");
		WHOIS_SERVERS.put("tk", "whois.nic.tk");
		WHOIS_SERVERS.put("tl", "whois.domains.tl");
		WHOIS_SERVERS.put("tm", "whois.nic.tm");
		WHOIS_SERVERS.put("tn", "whois.ati.tn");
		WHOIS_SERVERS.put("to", "whois.tonic.to");
		WHOIS_SERVERS.put("top", "whois.nic.top");
		WHOIS_SERVERS.put("tp", "whois.domains.tl");
		WHOIS_SERVERS.put("tr", "whois.nic.tr");
		WHOIS_SERVERS.put("travel", "whois.nic.travel");
		WHOIS_SERVERS.put("tw", "whois.twnic.net.tw");
		WHOIS_SERVERS.put("tv", "whois.nic.tv");
		WHOIS_SERVERS.put("tz", "whois.tznic.or.tz");
		WHOIS_SERVERS.put("ua", "whois.ua");
		WHOIS_SERVERS.put("ug", "whois.co.ug");
		WHOIS_SERVERS.put("uk", "whois.nic.uk");
		WHOIS_SERVERS.put("uk.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("uk.net", "whois.centralnic.com");
		WHOIS_SERVERS.put("ac.uk", "whois.ja.net");
		WHOIS_SERVERS.put("gov.uk", "whois.ja.net");
		WHOIS_SERVERS.put("us", "whois.nic.us");
		WHOIS_SERVERS.put("us.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("uy", "nic.uy");
		WHOIS_SERVERS.put("uy.com", "whois.centralnic.com");
		WHOIS_SERVERS.put("uz", "whois.cctld.uz");
		WHOIS_SERVERS.put("va", "whois.ripe.net");
		WHOIS_SERVERS.put("vc", "whois2.afilias-grs.net");
		WHOIS_SERVERS.put("ve", "whois.nic.ve");
		WHOIS_SERVERS.put("vg", "ccwhois.ksregistry.net");
		WHOIS_SERVERS.put("vu", "vunic.vu");
		WHOIS_SERVERS.put("wang", "whois.nic.wang");
		WHOIS_SERVERS.put("wf", "whois.nic.wf");
		WHOIS_SERVERS.put("wiki", "whois.nic.wiki");
		WHOIS_SERVERS.put("ws", "whois.website.ws");
		WHOIS_SERVERS.put("xxx", "whois.nic.xxx");
		WHOIS_SERVERS.put("xyz", "whois.nic.xyz");
		WHOIS_SERVERS.put("yu", "whois.ripe.net");
		WHOIS_SERVERS.put("za.com", "whois.centralnic.com");	
	}
	
	
	
	private static  com.google.common.util.concurrent.RateLimiter rateLimiter = com.google.common.util.concurrent.RateLimiter.create(1);
	
	private static com.google.common.cache.Cache<String,JSONObject> _siteInfoCache;
		
	static {			
		_siteInfoCache = com.google.common.cache.CacheBuilder.newBuilder()
				                   .maximumSize(MAX_CACHE_SIZE)
				                   .recordStats()
				                   .expireAfterAccess(10000, TimeUnit.DAYS)   //basically, we want to keep things, in cache, but using LRU access as the policy
				                   .build();
	}
	
	
	public static String getTLD(String domain) {
	  int pos = domain.indexOf('.'); 
	   
	  if(pos != -1) { 
		  return domain.substring(pos + 1); 
	  } 
	  else  { 
		  return domain;    
	  } 		
	}
	
	public static String getWhoIsServer(String domain) {
		String tld = getTLD(domain); 
		String server =  WHOIS_SERVERS.getOrDefault(tld,WhoisClient.DEFAULT_HOST);
		return server;
	}
	
	public String getRegistrarFromResponse(String data, String priorServer) {
		String lines[] = data.split("\n");
		String newRegistrar = null;
		for (String line: lines) {
			if (line.contains("Registrar WHOIS Server")) {
				newRegistrar = line.split(":")[1].trim();
			}
		}
		if (newRegistrar != null && newRegistrar.equals(priorServer)) { newRegistrar = null; }
		return newRegistrar;
	}
	
	/**
	 * 
	 * 
	 * 
	 *        
	 * @return
	 */
    @GET
    @Path("/find/{domainName:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public String processDocument(String document, @PathParam("domainName") String domainName) {   	
    	JSONObject result;
     	
    	String originalDomainName = domainName;
    	
    	try {
    		
    		if (domainName.contains(":")) {
    			domainName = domainName.substring(0, domainName.indexOf(":"));
    		}
    		
    		domainName= InternetDomainName.from(domainName).topPrivateDomain().toString();
    	}
    	catch (Exception e) {
    		result = new JSONObject();
    		result.put("status", "errored")
    		      .put("message", "Unable to compute top-level domain name")
    		      .put("domainName", originalDomainName);
    		return result.toString();
    	}
    	
    	try {
			result = _siteInfoCache.getIfPresent(domainName);
			
			if (result == null) {
				long acquisitionTime = ((long) (rateLimiter.acquire() *1000) );  // acquire returns time in seconds, convert to millis
				long startTime = System.currentTimeMillis();
	
				result = processRequest(domainName,0);
								
				long processingTime = System.currentTimeMillis() - startTime;
				synchronized (this) {  // an instance is create per request
					minimumProcessingTimeMS = Math.min(processingTime, minimumProcessingTimeMS);
					maximumProcessingTimeMS = Math.max(processingTime, maximumProcessingTimeMS);

					minimumAcquisitionTimeMS = Math.min(acquisitionTime, minimumAcquisitionTimeMS);
					maximumAcquisitionTimeMS = Math.max(acquisitionTime, maximumAcquisitionTimeMS);
					
					totalProcessingTimeMS  += processingTime;
					totalAcquisitionTimeMS  += acquisitionTime;
					totalRequestCount++;
				}
				
				
				_siteInfoCache.put(domainName, result);
			}
    	}
    	catch (Exception e) {
    		logger.log(Level.WARNING, "Unable to process request", e);
			result = new JSONObject().put("message", e.toString())
					                 .put("status", "error");
    	}
    	
		return result.toString();
	}
    
    private JSONObject processRequest(String domainName, int numTries) throws Exception {
    	
    	if (domainName.endsWith(".tt")) {
    		return scrapeTTWhoIs(domainName);
    	}
    	
    	try {
	    	JSONObject result= new JSONObject();
			WhoisClient whoisClient = new WhoisClient();
	
			String server = getWhoIsServer(domainName);
				
			String whoIsData = null;
			while (server != null && !server.trim().equals(""))  {
				whoisClient.connect(server);
				result.put("whoisServer", server);
				whoIsData = whoisClient.query(domainName);
				server = getRegistrarFromResponse (whoIsData, server);
			}
			
			//TODO:  maximum number of requests per second exceeded
			
			result.put("raw", whoIsData);
	
			if (whoIsData.startsWith("No match for ") || (whoIsData.length() < (30 + domainName.length()))) {
				result = scrapeWhoIsData(domainName);
			}
			else {
				JSONObject whoisData = convertWhoIsResponse(whoIsData, domainName);
				result.put("whois", whoisData);
			}
			result.put("status", "success");
	
			return result;
    	}
    	catch (Exception e) {
    		logger.log(Level.WARNING, "Unable to process request: "+e.getMessage());
    		if (numTries < 2) {
    			return processRequest(domainName, numTries+1);
    		}
    		throw e;
    	}
    }
    
    
    private JSONObject scrapeWhoIsData(String domainName) {
		JSONObject result = new JSONObject();

		try {
			String url = "http://viewdns.info/whois/?domain="+domainName;
			HttpContent hc = InternetUtilities.retrieveURL(url, userAgent, 5, true);
			
		
			org.jsoup.nodes.Document doc = hc.jsoupDocument;
			//System.out.println(doc);
			
			String text = doc.select("font[face=Courier]").html();
			text = text.replace("<br>","\n");
			text = text.replaceAll("&nbsp;", " ");
			text = text.substring(text.indexOf("======="));
			text = text.substring(text.indexOf("\n"));
			
			result.put("raw", text);
			JSONObject whoisData = convertWhoIsResponseCOUK(text);
			result.put("whois", whoisData);
		}
		catch (Exception e) {
			result = new JSONObject().put("status", "errored")
					                 .put("message", e.toString());
		}
		
		return result;
	}

    private JSONObject scrapeTTWhoIs(String domainName) {
		JSONObject result = new JSONObject();

		try {
			HttpClient httpClient = HttpClients.custom().setUserAgent(userAgent).build();
			
			HttpEntity entity = MultipartEntityBuilder
				    .create()
				    .addTextBody("name", domainName)
				    .addTextBody("Search","Search")
				    .build();

			HttpPost httpPost = new HttpPost("https://www.nic.tt/cgi-bin/search.pl");
			httpPost.setEntity(entity);
			httpPost.setHeader("Host", "www.nic.tt");
			httpPost.setHeader("Origin", "https://www.nic.tt");
			httpPost.setHeader("Referer", "https://www.nic.tt/cgi-bin/search.pl");
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity resultEntity = response.getEntity();
			
			ContentType ct = ContentType.getOrDefault(resultEntity);
			String content = EntityUtils.toString(resultEntity,ct.getCharset());
			org.jsoup.nodes.Document doc = Jsoup.parse(content,"\"https://www.nic.tt/cgi-bin/search.pl\"");
			
			String text = doc.select("table.data").html();			
			result.put("raw", text);
			
			JSONObject whoisData = new JSONObject();
			Elements items = doc.select("table.data tr");
			for (Element e:items) {
				String key= e.select("td:eq(0)").text();
				String value = e.select("td:eq(1)").text();
				whoisData.put(key, value);
			}
			
			result.put("whois", whoisData);
			
		}
		catch (Exception e) {
			result = new JSONObject().put("status", "errored")
					                 .put("message", e.toString());
		}
		
		return result;
	}    
    
    
    
	private static String[] entryParts = {"Admin", "Tech", "Registrant", "Registrar", "Registry" };
    private static String[] multipleValues = {"Name Server", "Domain Status"};
    
    private JSONObject convertWhoIsResponseEDU(String whoIsData) {
    	String l[] = whoIsData.split("\n");
    	ArrayList<String> lines = new ArrayList<String>(Arrays.asList(l));
    	
    	JSONObject result = new JSONObject();
    	
    	// ignore all of the leading data
    	while (lines.size() > 0 && lines.get(0).startsWith("Domain Name") == false) {
    		lines.remove(0);
    	}
    	if (lines.size() == 0) { return new JSONObject(); }
    	
    	JSONArray area = null;
    	while (lines.size() > 0) {
    		String line = lines.get(0).trim();
    		
    		if (!line.equals("")) {
	    		if (line.contains(":")) {
	    			String[] parts = line.split(":");
	    			if (parts.length ==2) {
	    				result.put(parts[0].trim(), parts[1].trim());
	    			}
	    			else {
	    				area = new JSONArray();
	    				result.put(parts[0].trim(), area);
	    			}
	    		}
	    		else {
	    			if (area != null) {
	    				area.put(line.trim());
	    			}
	    		}
    		}	
    		lines.remove(0);
    	}
    	
    	return result;
    }
    
    
    
    
    
    private JSONObject convertWhoIsResponseCOUK(String whoIsData) {
    	String l[] = whoIsData.split("\n");
    	ArrayList<String> lines = new ArrayList<String>(Arrays.asList(l));
    	
    	JSONObject result = new JSONObject();
    	
    	
    	JSONArray area = null;
    	while (lines.size() > 0) {
    		String line = lines.get(0).trim();
    		
    		if (line.startsWith("--")) {break;}
    		
    		if (!line.equals("")) {
	    		if (line.contains(":")) {
	    			String[] parts = line.split(":");
	    			if (parts.length ==2) {
	    				result.put(parts[0].trim(), parts[1].trim());
	    			}
	    			else {
	    				area = new JSONArray();
	    				result.put(parts[0].trim(), area);
	    			}
	    		}
	    		else {
	    			if (area != null) {
	    				area.put(line.trim());
	    			}
	    		}
    		}	
    		lines.remove(0);
    	}
    	convertSingleValueArraysToValue(result);
    	return result;
    }
    
    private JSONObject convertWhoIsResponse(String whoIsData, String domainName) {
    	whoIsData = whoIsData.replace("\r", "");
    	if (domainName.endsWith(".edu") ) {
    		return convertWhoIsResponseEDU(whoIsData);
    	}
    	else if (domainName.endsWith("co.uk")) {
    		return convertWhoIsResponseCOUK(whoIsData);
    	}
    	JSONObject result = new JSONObject();
    	
    	
    	String[] lines = whoIsData.split("\n");
    	for (String line: lines) {
    		if (!line.contains(":")) {continue;}
    		
    		String[] parts = line.split(":");
    		if (parts.length == 1) {
    			continue;
    		}
    		String key = parts[0].trim();
    		String value = parts[1].trim();
    		
    		
    		
    		if (key.equals("NOTICE") || key.startsWith(">>>") || key.startsWith("For more information") ||
    		    key.startsWith("Register your domain name at http") || 
    		    key.startsWith("URL of the ICANN WHOIS Data Problem Reporting System") ||
    		    key.startsWith("URL of the ICANN Whois Inaccuracy Complaint Form") ||
    		    key.startsWith("Visit MarkMonitor at http") ||
    		    key.startsWith("please go to http") ||
    		    key.startsWith("Please note") ||
    		    key.startsWith("Access to Public Interest Registry WHOIS information") ||
    		    key.contains("more information on Whois status codes") ||
    		    key.contains("under no circumstances will you use this data to")) {continue;}
    		
    		boolean processed = false;
    		for (String part: multipleValues) {
    			if (key.equals(part)) {
    				if (result.has(part) == false) {
    					result.put(part, new JSONArray());
    				}
    				JSONArray values = result.getJSONArray(part);
    				values.put(value);
    				
    				processed = true;
    				break;
    			}			
    		}
    		
    		
    		if (!processed) {
	    		for (String part: entryParts) {
	    			if (key.startsWith(part)) {
	    				if (result.has(part) == false) {
	    					result.put(part, new JSONObject());
	    				}
	    				JSONObject partJSON = result.getJSONObject(part);
	    				
	    				String tempKey = key.substring(part.length()).trim();
	    				if (tempKey.length() == 0) { tempKey = key; }
	    				partJSON.put(tempKey, value);
	    				
	    				processed = true;
	    				break;
	    			}
	    		}
    		}
    		if (!processed) {
    			result.put(key,value);
    		}
    		
    		
    		
    	}
    	
    	
		return result;
	}

	/**
     * Produces monitoring statistics
     */
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public String processStatistics() {
    	Runtime runtime = Runtime.getRuntime();
    	JSONObject vmStats = new JSONObject().put("usedMemory",  (runtime.totalMemory() - runtime.freeMemory()))
                .put("freeMemory",  runtime.freeMemory())
                .put("totalMemory", runtime.totalMemory())
                .put("maxMemory",   runtime.maxMemory());

    	double averageProcessingTime = ((double) totalProcessingTimeMS)/  Math.max((double)totalRequestCount, 1.0);
    	double averageAcquisitionTime = ((double) totalAcquisitionTimeMS)/  Math.max((double)totalRequestCount, 1.0);
		JSONObject processStats = new JSONObject()
				               .put("minimumProcessingTimeMS", minimumProcessingTimeMS)
				               .put("maximumProcessingTimeMS", maximumProcessingTimeMS)
				               .put("minimumAcquisitionTimeMS", minimumAcquisitionTimeMS)
				               .put("maximumAcquisitionTimeMS", maximumAcquisitionTimeMS)
				               .put("totalProcessingTimeMS", totalProcessingTimeMS)
				               .put("averageProcessingTimeMS", averageProcessingTime)
				               .put("averageAcquisitionTimeMS", averageAcquisitionTime)
				               .put("totalRequests", totalRequestCount);

		com.google.common.cache.CacheStats cacheStats = _siteInfoCache.stats();
		JSONObject cStats = new JSONObject().put("averageLoadPenalty",   cacheStats.averageLoadPenalty())
				                            .put("evictionCount",        cacheStats.evictionCount())
				                            .put("hitCount",             cacheStats.hitCount())
				                            .put("hitRate",              cacheStats.hitRate())
				                            .put("missCount",            cacheStats.missCount())
				                            .put("missRate",             cacheStats.missRate())
				                            .put("requestCount",         cacheStats.requestCount())
				                            .put("totalLoadTimeNanoSec", cacheStats.totalLoadTime())
				                            .put("size",                 _siteInfoCache.size());		
				
    	JSONObject result = new JSONObject().put("process", processStats)
                                            .put("cacheStatistics", cStats )
    			                            .put("memory", vmStats );
    	
    	return result.toString();
    }    
 
    
    private void convertSingleValueArraysToValue(JSONObject object) {
    	    Iterator<String> keysItr = object.keys();
    	    while(keysItr.hasNext()) {
    	        String key = keysItr.next();
    	        Object value = object.get(key);

    	        if (value instanceof JSONArray) {
    	            JSONArray a = (JSONArray) value;
    	            if (a.length() == 1) {
    	            	object.put(key, a.get(0));
    	            }
    	        }
    	        else if(value instanceof JSONObject) {
    	        	convertSingleValueArraysToValue((JSONObject) value);
    	        }
    	    }
    }
    
    
}
