package edu.ncsu.las.storage.citation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.Domain;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.util.DateUtilities;
import edu.ncsu.las.util.FileUtilities;
import edu.ncsu.las.util.json.JSONUtilities;
import edu.ncsu.las.util.json.JSONUtilities.JSONType;
import edu.ncsu.las.util.XMLUtilities;

public class PubMedUtilities {
	private static Logger logger =Logger.getLogger(PubMedUtilities.class.getName());
	
	public static Map<String,Integer> computeCitationCounts() throws JSONException, IOException {
		logger.log(Level.INFO, "producing citation counts");
		java.util.HashMap<String, Integer> result = new java.util.HashMap<String,Integer>();
		
		String citationDirectory =  "C:\\pubmed\\pubmed\\citationRecords\\";
		File citationDir = new File(citationDirectory);
		for (File citationFile: citationDir.listFiles()) {
			JSONObject citation = PubMedProcessor.loadRecord(citationFile);
			if (!citation.has("references")) { continue; }
			JSONArray references = citation.getJSONArray("references");
			for (int i=0;i < references.length();i++ ) {
				String pmid = references.getJSONObject(i).getString("PMID");
				Integer count = result.getOrDefault(pmid, 0)+1;
				result.put(pmid, count);
			}
			
		}
		logger.log(Level.INFO, "produced citation counts");		
		return result;
	}
	
	public static String getFirstAuthorName(JSONObject record) {
		String result = "";
		
		// Include both AU and AF fields for authors
		if (record.getJSONObject("Article").has("AuthorList")) {
			JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");
			JSONObject author = authors.getJSONObject(0);
			if (author.getString("ValidYN").equals("Y")) {
				if (author.has("CollectiveName")) {
					result = author.getString("CollectiveName");
				}
				else {
					result = author.getString("LastName") + ", " + author.optString("ForeName", author.optString("Initials","UnknownFirst"));
				}
			}
		}
		return result;
	}
	
	private static Pattern[] affiliations = { Pattern.compile("([A-Z][a-z]{1,20} ?){1,4} (Medical School|Institute of Technology|Institute of Science and Technology|Hospital)"),
			                                  Pattern.compile("University of ([A-Z][a-z]{1,20} ?){1,4}"),
			                                  Pattern.compile("(([A-Z][']?[^\\s,.]+[.]?\\s(\\(.*?\\))?([a-z]*\\s){0,3})*?(College|Université|Research Center|University|Institute|Law School|School of|Military Academy|Naval Academy|Nursing School|Seminary|Polytechnic)[^,\\d]*(?=,|\\d))")
	};
	
	public static String getFirstAffiliation(JSONObject record) {
		String result = "";
		
		// Include both AU and AF fields for authors
		if (record.getJSONObject("Article").has("AuthorList")) {
			JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");
			JSONObject author = authors.getJSONObject(0);
				
			if (author.has("AffiliationInfo") && author.getJSONArray("AffiliationInfo").length()>0) {
				String affiliationLine = author.getJSONArray("AffiliationInfo").getJSONObject(0).getString("Affiliation");
				
				result = affiliationLine;
				for (Pattern p: affiliations) {
					Matcher m = p.matcher(affiliationLine);
					if (m.find()) {
						String value = m.group();
						result = value;
						
						break;
					}
				}
				
				//System.out.println(result);
				
			}
		}
		return result;
	}	
	
	
	public static String createCitationReferenceLine(String pmid, String source, boolean useFullNames) throws JSONException, IOException {
		String result = "";
		try {
			String citationRecordLocation = "C:\\pubmed\\pubmed\\citationRecords\\"+pmid+".json";
			
			JSONObject citationRecord = PubMedProcessor.loadRecord(new java.io.File(citationRecordLocation));
			
			JSONArray authors = citationRecord.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");
			if (authors.length() > 0) {
				JSONObject author = authors.getJSONObject(0);
				if (author.has("LastName")) {
					String authorName = author.getString("LastName");
					if (useFullNames) {
						if (author.has("ForeName")) { 
							authorName += " " + author.getString("ForeName"); 
						}
						else if (author.has("Initials")) { 
							authorName += " " +author.getString("Initials"); 
						}
					}
					else {
						if (author.has("Initials")) { 
							authorName += " " +author.getString("Initials"); 
						}
					}
					//source = authorName +", " + source;   // this was my initial default.
					result = authorName;
				}
			}
			
			result = result + ", "  + JSONUtilities.getAsString(citationRecord, "DateCreated", "").substring(0,4);
			result = result + ", "  + JSONUtilities.getAsString(citationRecord, "Article.Journal.ISOAbbreviation", "").toUpperCase();
			result = result + ", V" + JSONUtilities.getAsString(citationRecord, "Article.Journal.JournalIssue.Volume", "");
			result = result + ", I" + JSONUtilities.getAsString(citationRecord, "Article.Journal.JournalIssue.Issue", ""); 
			result = result + ", P" + JSONUtilities.getAsString(citationRecord, "Article.Pagination.MedlinePgn", "");
			
			String doi = JSONUtilities.getAsString(citationRecord, "PubmedData.id.doi", "");
			if (doi.length() >0) {
				result = result +", DOI "+ doi;
			}
			
			return result;
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Creating reference line: "+e.toString());
			return null;
		}
		
	}
	
	public static String convertPubMedJSONToISI(JSONObject record, int timesReferenced, boolean useFullNames) throws JSONException, IOException {
		try {
			StringBuilder result = new StringBuilder();
			
			//String publicationType = JSONUtilities.getAsString(record, "Article.PublicationTypeList.PublicationType.content", "");

			result.append("PT J\n");
			
			// Include both AU and AF fields for authors
			if (record.getJSONObject("Article").has("AuthorList")) {
				JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");
				for (int i=0; i < authors.length(); i++) {
					if (i == 0) { result.append("AU "); }
					else        { result.append("   "); }
					
					JSONObject author = authors.getJSONObject(i);
					if (author.getString("ValidYN").equals("N")) { continue; }
					if (author.has("CollectiveName")) {
						result.append(author.getString("CollectiveName"));
					}
					else {
						result.append(author.getString("LastName"));
						if (useFullNames) {
							if (author.has("ForeName")) { 
								result.append(", ");
								result.append(author.getString("ForeName")); 
							}
							else if (author.has("Initials")) { 
								result.append(", ");
								result.append(author.getString("Initials")); 
							}
						}
						else {
							if (author.has("Initials")) { 
								result.append(", ");
								result.append(author.getString("Initials")); 
							}							
						}
					}
					result.append("\n");
				}
				for (int i=0; i < authors.length(); i++) {
					if (i == 0) { result.append("AF "); }
					else        { result.append("   "); }
					
					JSONObject author = authors.getJSONObject(i);
					if (author.getString("ValidYN").equals("N")) { continue; }
					if (author.has("CollectiveName")) {
						result.append(author.getString("CollectiveName"));
					}
					else {
						result.append(author.getString("LastName"));
						if (author.has("ForeName")) { 
							result.append(", "); 
							result.append(author.getString("ForeName")); 
						}	
					}
					result.append("\n");
				}
			}
			result.append("TI ");result.append(JSONUtilities.getAsString(record, "Article.ArticleTitle", ""));            result.append("\n");
			result.append("SO ");result.append(JSONUtilities.getAsString(record, "Article.Journal.Title", ""));           result.append("\n");
			result.append("LA ");result.append(JSONUtilities.getAsString(record, "Article.Language", ""));                result.append("\n");
			result.append("DT Article\n");
			
			// Minor keywords: Author keywords
			JSONArray keywords = record.optJSONArray("keywordMinor");
			if (keywords != null) {
				for (int i=0;i< keywords.length(); i++) {
					if (i == 0) { result.append("DE "); }
					else        { result.append("; ");  }
					result.append(keywords.getString(i));
				}
				result.append("\n");
			}
			
			// MESH Headings
			JSONArray meshHeadings = record.optJSONArray("MeshHeading");
			if (meshHeadings != null) {
				for (int i=0;i< meshHeadings.length(); i++) {
					if (i == 0) { result.append("ID "); }
					else        { result.append("; ");  }
					if (meshHeadings.getJSONObject(i).has("DescriptorName")) {
						result.append(meshHeadings.getJSONObject(i).getJSONObject("DescriptorName").getString("content"));
					}
				}
				result.append("\n");
			}
			
			result.append("AB ");result.append(JSONUtilities.getAsString(record, "Article.Abstract.AbstractText", "").replace("\n","    "));   result.append("\n");
			
					
            // C1 and RP
			if (record.getJSONObject("Article").has("AuthorList")) {
				JSONArray authors = record.getJSONObject("Article").getJSONObject("AuthorList").getJSONArray("Author");
				// Output the affiliation information
				String firstAffiliationLineWithAuthor = "";
				int affiliationLineCount = 0;
				for (int i=0; i < authors.length(); i++) {
					JSONObject author = authors.getJSONObject(i);
					if (author.has("AffiliationInfo") && author.getJSONArray("AffiliationInfo").length()>0) {
						String affiliationLine = author.getJSONArray("AffiliationInfo").getJSONObject(0).getString("Affiliation");
						if (affiliationLineCount == 0) { result.append("C1 ");  }
						else                           { result.append("   ");  }
						affiliationLineCount++;
						result.append("[");
						if (author.has("CollectiveName")) {
							result.append(author.getString("CollectiveName"));
						}
						else {
							result.append(author.getString("LastName"));
							result.append(", ");
							result.append(author.optString("ForeName", author.optString("Initials", "UNK")));
						}
						result.append("]");
						result.append(affiliationLine);
						result.append("\n");
						
						if (affiliationLineCount == 1) {
							String authorString = "";
							if (author.has("CollectiveName")) {
								authorString = author.getString("CollectiveName");
							}
							else {
								authorString = author.getString("LastName") +", " + author.optString("ForeName", author.optString("Initials", "UNK"));
							}
								
							firstAffiliationLineWithAuthor = "RP " + authorString + ", " +affiliationLine+"\n";
						}
					}
					
				}
				result.append(firstAffiliationLineWithAuthor);
			}
			
			// convert CR (references
			JSONArray references = record.optJSONArray("references");
			if (references == null) { references = new JSONArray();}
			for (int i=0; i < references.length(); i++) {
				JSONObject refRecord = references.getJSONObject(i);
				String pmidRef = refRecord.getString("PMID");
				
				String shortRef = createCitationReferenceLine(pmidRef, refRecord.getString("source"), useFullNames);
				if (shortRef == null) { shortRef = refRecord.getString("source"); }
				shortRef = shortRef.replace(";", ",");  // Change this for bibliometrix the semi-colon interferes with field separators in bibliometrix
				
				if (i == 0) { result.append("CR "); }
				else        { result.append("   "); }
				result.append(shortRef);
				result.append("\n");
			}
			result.append("NR "+references.length()+"\n");
			result.append("TC "+timesReferenced+"\n");
			
			
			result.append("JI ");result.append(JSONUtilities.getAsString(record, "Article.Journal.ISOAbbreviation", "")); result.append("\n");
			result.append("PY ");result.append(JSONUtilities.getAsString(record, "DateCreated", "").substring(0,4));  result.append("\n");
			result.append("VL ");result.append(JSONUtilities.getAsString(record, "Article.Journal.JournalIssue.Volume", ""));  result.append("\n");
			result.append("IS ");result.append(JSONUtilities.getAsString(record, "Article.Journal.JournalIssue.Issue", ""));  result.append("\n");
			result.append("PG ");result.append(JSONUtilities.getAsString(record, "Article.Pagination.MedlinePgn", ""));  result.append("\n");
			
			result.append("AN ");result.append(JSONUtilities.getAsString(record, "PubmedData.id.pubmed", ""));  result.append("\n");
			result.append("DI ");result.append(JSONUtilities.getAsString(record, "PubmedData.id.doi", ""));  result.append("\n");
			result.append("JC ");result.append(JSONUtilities.getAsString(record, "MedlineJournalInfo.NlmUniqueID", ""));  result.append("\n");
			result.append("SN ");result.append(JSONUtilities.getAsString(record, "MedlineJournalInfo.ISSNLinking", ""));  result.append("\n");
			result.append("UT ");result.append(JSONUtilities.getAsString(record, "PubmedData.id.doi", ""));  result.append("\n");
			
			
			//result.append("SC MEDICAL\n"); // TODO change this
			
			
			//output ER at the end of each record following by a space
			//result.append("ER\n");
			return result.toString();
		}
		catch (JSONException e) {
			System.out.println(record.toString(4));
			e.printStackTrace();
			System.exit(0);
			return "";
		}
	}
	
	public static void produceISIDirectory() throws JSONException, IOException {
		Map<String,Integer> citationCount = computeCitationCounts();
		
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
		
		//PrintWriter pw = new PrintWriter("C:\\pubmed\\wolfhunt.txt");
		//pw.println("FN OpenKE");
		//pw.println("VR 1.0");

		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			JSONObject record = PubMedProcessor.loadRecord(f);
			PrintWriter pw = new PrintWriter("C:\\pubmed\\individualFileExport\\download"+JSONUtilities.getAsString(record, "DateCreated", "").substring(0,4)+"_"+record.getString("PMID")+".txt");
			pw.println("FN OpenKE: "+ record.getString("PMID"));
			pw.println("VR 1.0");
			//if (citationCount.getOrDefault(record.getString("PMID"), 0) ==0) {	continue;			}
			
			String isiFormat = convertPubMedJSONToISI(record,citationCount.getOrDefault(record.getString("PMID"), 0),false);
			String[] lines = isiFormat.split("\n");
			for (String line: lines) {
				if (line.trim().length() > 2) {
					pw.println(line);
				}
			}
			//pw.print(isiFormat);
			pw.println("ER");
			pw.println("EF");
			pw.close();
		}
		
		
		
		//pw.println("EF");
		//pw.close();
		logger.log(Level.INFO, "ISI file produced");
	}
	
	
	public static void produceISIFile() throws JSONException, IOException {
		Map<String,Integer> citationCount = computeCitationCounts();
		
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
		String isiFileName = "C:\\pubmed\\wolfhunt_20171105_fullname.txt"; 
		PrintWriter pw = new PrintWriter(isiFileName);
		pw.println("FN OpenKE");
		pw.println("VR 1.0");

		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			JSONObject record = PubMedProcessor.loadRecord(f);

			//if (citationCount.getOrDefault(record.getString("PMID"), 0) ==0) {	continue;			}
			
			String isiFormat = convertPubMedJSONToISI(record,citationCount.getOrDefault(record.getString("PMID"), 0),true);
			String[] lines = isiFormat.split("\n");
			for (String line: lines) {
				if (line.trim().length() > 2) {
					pw.println(line);
				}
			}
			//pw.print(isiFormat);
			pw.println("ER");
			pw.println("");
		}
		
		pw.println("EF");
		pw.close();
		logger.log(Level.INFO, "ISI file produced: "+ isiFileName);
	}

	public static void produceCSVFile(String jsonRecordLocation, String outputFile) throws Exception {
		CSVFormat csvFileFormat = CSVFormat.RFC4180;
		
		FileWriter fw = new FileWriter(outputFile);
		CSVPrinter csvFilePrinter = new CSVPrinter(fw, csvFileFormat);
		Object [] FILE_HEADER = {"pmid","createdDate", "firstAuthor", "affiliation", "journalText", "abstractText","text"};
		csvFilePrinter.printRecord(FILE_HEADER);
		
		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			JSONObject record = PubMedProcessor.loadRecord(f);
			if (record.has("text") == false) { continue; }
			String pmid = record.getString("PMID");
			String abstractText = JSONUtilities.getAsString(record, "Article.Abstract.AbstractText", "").replace("\n","    ");
			String text         = record.getString("text").replace("\n","    ").replace("\r","");
			String authorName   = getFirstAuthorName(record);
			String affiliation  = getFirstAffiliation(record);
			String createdDate  = record.getString("DateCreated");
			String journal      = JSONUtilities.getAsString(record, "Article.Journal.Title", "");

			Object row[] = { pmid, createdDate, authorName, affiliation, journal, abstractText,text};
			csvFilePrinter.printRecord(row);
		}
		
		csvFilePrinter.flush();
		csvFilePrinter.close();
		
	}
	
	
	public static void produceTextFiles() throws JSONException, IOException {
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";

		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			JSONObject record = PubMedProcessor.loadRecord(f);
			if (record.has("text") == false) { continue; }
			String pmid = record.getString("PMID");
			String year = JSONUtilities.getAsString(record, "DateCreated", "").substring(0,4);
			String outputFile = "C:\\pubmed\\abstractYearly\\" + year +"\\"+ pmid+".txt";
			
			
			Files.write(Paths.get(outputFile), JSONUtilities.getAsString(record, "Article.Abstract.AbstractText", "").getBytes(StandardCharsets.UTF_8));
			//Files.write(Paths.get(outputFile), record.getString("text").getBytes(StandardCharsets.UTF_8));
		}
				
		logger.log(Level.INFO, "Text files produced");
	}
	
	public static void produceMasterJSONFile() throws JSONException, IOException {
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";

		String outputLocation = "C:\\pubmed\\pubmed\\allRecords.json";
		
		PrintWriter pw = new PrintWriter(new OutputStreamWriter( new FileOutputStream(outputLocation), "UTF-8"));
		pw.println("[");
		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			JSONObject record = PubMedProcessor.loadRecord(f);
			
			if (record.has("relations")) { record.remove("releations"); }
			if (record.has("text")) { record.remove("text"); }
			if (record.has("namedEntities")) { record.remove("namedEntities"); }
			
			
			pw.print(record.toString());
			pw.println(",");
		}
		pw.println("]");
		pw.close();
		logger.log(Level.INFO, "Master JSON file produced");
	}	
	
	
	public static void anaylzePubMedFiles() throws Exception {
		
		File[] files = (new File("C:\\pubmed\\pubmed\\baseline\\")).listFiles();
		HashMap<String, Integer> counts = new HashMap<String,Integer>();
		int count = 0;
		for (int i=files.length-20;i<files.length;i++) {
			File f= files[i];
			System.err.println("Processing: "+f);
			JSONObject jo = convertGZippedXMLFile(f);
			JSONUtilities.countAllFields(jo,counts);
			count++;
			if (count > 10)break;
		}
		for (String key: counts.keySet()) {
			System.out.println(key+"\t"+counts.get(key));
		}	
	}
	
	/**
	 * Converts a pubmed file from XML to JSON
	 * 
	 * @param f  file to be converted
	 * @return JSONObject representation of the file without further changes
	 * @throws IOException
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static JSONObject convertGZippedXMLFile(File f) throws IOException, ParserConfigurationException, SAXException {
		GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(FileUtilities.readAllBytesFromFile(f)));
		return convertXML(zis);
	}

	/**
	 * Converts a pubmed file from XML to JSON
	 * 
	 * @param f  file to be converted
	 * @return JSONObject representation of the file without further changes
	 * @throws IOException
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static JSONObject convertXML(InputStream is) throws IOException, ParserConfigurationException, SAXException {
		JSONObject result = new JSONObject();
		JSONArray  articles = new JSONArray();
		
		

	    //byte[] data = FileUtilities.uncompressGZipStream(zis);
			
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();   
		// no validation is really needed on our files - we can trust they were built properly.  Also eliminates having to download the DTD
		dbFactory.setNamespaceAware(true);
		dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
		dbFactory.setFeature("http://xml.org/sax/features/validation", false);
		dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document doc = dBuilder.parse(is);
		is.close();

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		NodeList dcList = doc.getElementsByTagName("DeleteCitation");
		if (dcList.getLength() > 0) {
			String text = XMLUtilities.nodeToString(dcList.item(0));
			JSONObject deleteCitation = org.json.XML.toJSONObject(text);
			result.put("DeleteCitation", deleteCitation.getJSONObject("DeleteCitation"));
		}
		
		NodeList nList = doc.getElementsByTagName("PubmedArticle");
		for (int i = 0; i < nList.getLength(); i++) {
			org.w3c.dom.Node nNode = nList.item(i);
			String text = XMLUtilities.nodeToString(nNode);
			JSONObject article = org.json.XML.toJSONObject(text);
			
			if(nNode instanceof Element) {
				// Put in copy of AbstractTextXML
				org.w3c.dom.Element docElement = (org.w3c.dom.Element) nNode;
				NodeList abstractList = docElement.getElementsByTagName("Abstract");
				if (abstractList.getLength() >0 && abstractList.item(0) instanceof Element) {
				    NodeList atList = ((Element) abstractList.item(0)).getElementsByTagName("AbstractText");
				    if (atList.getLength() > 0) {
				    	JSONArray atXMLList = new JSONArray();
				    	for (int j=0; j < atList.getLength(); j++ )  {
					    	org.w3c.dom.Node abstractTextNode = atList.item(j);
					    	atXMLList.put(XMLUtilities.nodeToString(abstractTextNode));
				    	}
				    	article.getJSONObject("PubmedArticle").getJSONObject("MedlineCitation").put("abstractTextXML", atXMLList);
				    }
				}
			    
			    // Put in Copy of ArticleTitle
			    NodeList titleList = docElement.getElementsByTagName("ArticleTitle");
			    if (titleList.getLength() > 0) {
			    	org.w3c.dom.Node articleTitleNode = titleList.item(0);
			    	article.getJSONObject("PubmedArticle").getJSONObject("MedlineCitation").put("articleTitleXML", XMLUtilities.nodeToString(articleTitleNode));
			    }
			    
			    // Put in Copy of OtherAbstract.  Find OtherAbstract, then AbstractText
			    NodeList otherAbstractList = docElement.getElementsByTagName("OtherAbstract");
			    if (otherAbstractList.getLength() >0 && otherAbstractList.item(0) instanceof Element) {
			    	NodeList oaList = ((Element) otherAbstractList.item(0)).getElementsByTagName("AbstractText");
			    	if (oaList.getLength() > 0) {
			    		article.getJSONObject("PubmedArticle").getJSONObject("MedlineCitation").put("otherAbstractTextXML", XMLUtilities.nodeToString(oaList.item(0)));
			    	}
			    }
			}
			articles.put(article.getJSONObject("PubmedArticle"));
		}
		result.put("PubmedArticle", articles);

		return result;
	}	
	
	/**
	 * From a JSON object for a pubmed file, return the list of delete citation records
	 * 
	 * @param jo
	 * @return
	 */
	public static List<DeleteCitationRecord> extractDeleteRecords(JSONObject jo) {
		ArrayList<DeleteCitationRecord> result = new ArrayList<DeleteCitationRecord>();
		
		if (jo.has("DeleteCitation")) {
			JSONObject dcObj = jo.getJSONObject("DeleteCitation");
			if (dcObj.has("PMID")) {
				JSONArray records = dcObj.getJSONArray("PMID");
				for (int i=0; i < records.length(); i++) {
					JSONObject rec = records.getJSONObject(i);
					result.add(new DeleteCitationRecord(rec.getLong("content"), rec.getInt("Version")));
				}
			}
		}
		
		return result;
	}
	
	
	public static JSONObject refactorCitation(JSONObject original) {

		
		//Refactor the "MedlineCitation" part of the record. We'll also remove "MedlineCitation level as it provides no real value
		JSONObject result = refactorMedlineCitation(original.getJSONObject("MedlineCitation"));
		
		//Refactor the PubMedData (which is metadata about status changes, ID's and status
		JSONObject pubmedDataNew = refactorPubmedData(original.getJSONObject("PubmedData")); 
		result.put("PubmedData", pubmedDataNew);
		
		//Refactor "DateCreated" which was removed in the 2018 feeds.  This available as the "entrez" field
		if (pubmedDataNew.has("pubStatus")) {
			JSONObject pubStatus = pubmedDataNew.getJSONObject("pubStatus");
			if (pubStatus.has("entrez")) {
				String dateCreated = pubStatus.getString("entrez");
				result.put("DateCreated", dateCreated);
			}
		}
	
		return result;
	}
	
	
	public static JSONObject refactorMedlineCitation(JSONObject mcOriginal) {
		JSONObject result = new JSONObject(mcOriginal.toString()); // forces a deep copy of the object
		
		//Convert date fields
		if (result.has("DateRevised")) { result.put("DateRevised", refactorObjectDateFields(result.getJSONObject("DateRevised"))); }
		if (result.has("DateCreated")) { result.put("DateCreated", refactorObjectDateFields(result.getJSONObject("DateCreated"))); }
		if (result.has("DateCompleted")) { result.put("DateCompleted", refactorObjectDateFields(result.getJSONObject("DateCompleted"))); }
		
		Object o = JSONUtilities.get(result, "Article.Journal.JournalIssue.PubDate");
		if (o != null) {
			JSONObject journalPubDate = (JSONObject) o;
			JSONUtilities.put(result, "Article.Journal.JournalIssue.PubDate",refactorObjectDateFields(journalPubDate));
		}
		
		JSONObject articleObject = result.getJSONObject("Article");
		if (articleObject.has("ArticleDate")) {
			articleObject.put("ArticleElectronicDate", refactorObjectDateFields(articleObject.getJSONObject("ArticleDate")));
			articleObject.remove("ArticleDate");
		}
		
		// simplify keywords into major or minor keyword list
		if (result.has("KeywordList")) {
			if (JSONUtilities.getJSONType(result.get("KeywordList")) == JSONType.JSON_OBJECT) {
				JSONObject keywordListObject = result.getJSONObject("KeywordList");
				
				JSONArray kwList = keywordListObject.optJSONArray("Keyword");
				if (kwList != null) {
					String owner = keywordListObject.optString("Owner", null); 
					JSONArray minorKeywords = new JSONArray();
					JSONArray majorKeywords = new JSONArray();
				
					for (int i=0; i < kwList.length(); i++) {
						JSONObject ko = kwList.getJSONObject(i);
						if (ko.optString("MajorTopicYN", "N").equals("Y")) { 
							String keyword = ko.optString("content",null);
							if (keyword != null) {
								majorKeywords.put(keyword); 
							}						
						}
						else {
							String keyword = ko.optString("content",null);
							if (keyword != null) {
								minorKeywords.put(keyword); 
							}
						}
					}
					result.put("keywordOwner", owner);
					result.put("keywordMinor", minorKeywords);
					result.put("keywordMajor", majorKeywords);
				}
				
				result.remove("KeywordList");
			}
			else if (JSONUtilities.getJSONType(result.get("KeywordList")) == JSONType.JSON_ARRAY) {
				JSONArray keywordList = result.getJSONArray("KeywordList");
				
				JSONArray minorKeywords = new JSONArray();
				JSONArray majorKeywords = new JSONArray();
				for (int i=0; i <keywordList.length(); i++) {
					JSONObject keywordListObject = keywordList.getJSONObject(i);
					String owner = keywordListObject.optString("Owner", null);  // we'll be ignoring these since there is more than one owner, just use the actual keywords themselves 
					
					JSONArray kwList = keywordListObject.optJSONArray("Keyword");
					if (kwList != null) {
						for (int j=0; j < kwList.length(); j++) {
							JSONObject ko = kwList.getJSONObject(j);
							if (ko.optString("MajorTopicYN", "N").equals("Y")) {
								String keyword = ko.optString("content",null);
								if (keyword != null) {
									majorKeywords.put(keyword); 
								}						
							}
							else {
								String keyword = ko.optString("content",null);
								if (keyword != null) {
									minorKeywords.put(keyword); 
								}
							}
						}
					}
	
				}
				
					
				//result.put("keywordOwner", owner);
				result.put("keywordMinor", minorKeywords);
				result.put("keywordMajor", majorKeywords);
				
				result.remove("KeywordList");			
				
			}
			else {
				logger.log(Level.WARNING, "Unexpected value for keyword: "+ result.toString());
			}
		}
		
		// Ensure author and affiliation Info are all arrays for consistency
		if (articleObject.has("AuthorList")) {
			JSONObject al =  articleObject.getJSONObject("AuthorList");
			if (al.has("Author")) {
				if (JSONUtilities.getJSONType(al.get("Author")) == JSONType.JSON_OBJECT ) {
					JSONObject authObj = al.getJSONObject("Author");
					JSONArray  authorArray = new JSONArray();
					authorArray.put(authObj);
					al.put("Author", authorArray);
				}
				JSONArray authorArray = al.getJSONArray("Author");
				for (int i=0;i < authorArray.length(); i++) {
					JSONObject authorObject = authorArray.getJSONObject(i);
					if (authorObject.has("AffiliationInfo")) {
						if (JSONUtilities.getJSONType(authorObject.get("AffiliationInfo")) == JSONType.JSON_OBJECT) {
							JSONObject affObject = authorObject.getJSONObject("AffiliationInfo");
							JSONArray affArray   = new JSONArray();
							affArray.put(affObject);
							authorObject.put("AffiliationInfo", affArray);
						}
					}
				}
			}
		}
			
		// Fix consistency of Investigator list 
		if (result.has("InvestigatorList")) {
			JSONObject investigatorList = result.getJSONObject("InvestigatorList");
			if (investigatorList.has("Investigator") && JSONUtilities.getJSONType(investigatorList.get("Investigator")) == JSONType.JSON_OBJECT) {
				JSONObject investObject = investigatorList.getJSONObject("Investigator");
				JSONArray  investArray  = new JSONArray();
				investArray.put(investObject);
				investigatorList.put("Investigator", investArray);
			}
			JSONArray investigatorArray = investigatorList.getJSONArray("Investigator");
			for (int i=0; i < investigatorArray.length(); i++) {
				JSONObject investigatorObject = investigatorArray.getJSONObject(i);
				if (investigatorObject.has("AffiliationInfo")) {
					if (JSONUtilities.getJSONType(investigatorObject.get("AffiliationInfo")) == JSONType.JSON_OBJECT) {
						JSONObject affObject = investigatorObject.getJSONObject("AffiliationInfo");
						JSONArray affArray   = new JSONArray();
						affArray.put(affObject);
						investigatorObject.put("AffiliationInfo", affArray);
					}
				}
			}
		}
		
		// Fix the Abstract.  The default xml to JSON convert doesn't hand tagged text correctly.
		if (articleObject.has("Abstract")) {
			JSONObject abstractObject = articleObject.getJSONObject("Abstract");
			if (JSONUtilities.getJSONType(abstractObject.get("AbstractText")) != JSONType.STRING) {				
				JSONArray absXMLArray = result.getJSONArray("abstractTextXML");
				if (absXMLArray.length() == 1) {
					String text = XMLUtilities.convertXMLStringToText(absXMLArray.getString(0));
					abstractObject.put("AbstractText", text);		
				}
				else {
					StringBuilder abstractText = new StringBuilder();
					JSONArray structuredAbstract = new JSONArray();
					for (int i=0; i < absXMLArray.length(); i++) {
						if (i>0) { abstractText.append("\n");	}
						
						Element element = XMLUtilities.convertXMLStringToDocument(absXMLArray.getString(i)).getDocumentElement();
						String label    = element.getAttribute("Label");
						String category = element.getAttribute("NlmCategory");
						if (category == null || category.trim().equals("")) { category = "Unknown"; }
						if (label == null || label.trim().equals("")) { label = "Unknown"; }
						String text     = element.getTextContent(); 
						structuredAbstract.put(new JSONObject().put(category, text));
						abstractText.append(label);
						abstractText.append(": ");
						abstractText.append(text);
					}
					abstractObject.put("AbstractText", abstractText.toString());		
					abstractObject.put("AbstractStructured", structuredAbstract);			
				}
			}
			//abstractObject.put("abstractTextXML", result.getJSONArray("abstractTextXML"));
			result.remove("abstractTextXML");
		}
		
		// Fix otherAbstract if it exists
		if (result.has("otherAbstractTextXML") && result.has("OtherAbstract")) {
			if (JSONUtilities.getJSONType(result.get("OtherAbstract")) == JSONType.JSON_ARRAY) {
				JSONObject otherAbstractObject = result.getJSONArray("OtherAbstract").getJSONObject(0);
				String text = XMLUtilities.convertXMLStringToText(result.getString("otherAbstractTextXML"));
				otherAbstractObject.put("AbstractText",text);
				result.put("OtherAbstract", otherAbstractObject);
			}
			else {				
				JSONObject otherAbstractObject = result.getJSONObject("OtherAbstract");
				if (otherAbstractObject.has("AbstractText")) {
					if (JSONUtilities.getJSONType(otherAbstractObject.get("AbstractText")) != JSONType.STRING) {
						String text = XMLUtilities.convertXMLStringToText(result.getString("otherAbstractTextXML"));
						otherAbstractObject.put("AbstractText",text);
					}			
				}
			}
			result.remove("otherAbstractTextXML");
		}
		
		// Fix Article Title if it needs it
		if (articleObject.has("ArticleTitle") && result.has("articleTitleXML")) {
			if (JSONUtilities.getJSONType(articleObject.get("ArticleTitle")) != JSONType.STRING) {
				String text = XMLUtilities.convertXMLStringToText(result.getString("articleTitleXML"));
				articleObject.put("ArticleTitle",text);
			}
			result.remove("articleTitleXML");
		}
		
		
		// Refactor out citations into their own references
		if (result.has("CommentsCorrectionsList") && result.getJSONObject("CommentsCorrectionsList").has("CommentsCorrections")) {
			JSONObject ccl = result.getJSONObject("CommentsCorrectionsList");
			if (JSONUtilities.getJSONType(ccl.get("CommentsCorrections")) == JSONType.JSON_OBJECT ) {
				JSONObject ccObj = ccl.getJSONObject("CommentsCorrections");
				JSONArray  ccArray = new JSONArray();
				ccArray.put(ccObj);
				ccl.put("CommentsCorrections", ccArray);
			}
			
			JSONArray comments = result.getJSONObject("CommentsCorrectionsList").getJSONArray("CommentsCorrections");
			JSONArray references = new JSONArray();
			for (int i=0; i< comments.length(); i++) {
				JSONObject comment = comments.getJSONObject(i);
				if (comment.has("RefType")) {
					if (comment.getString("RefType").equals("Cites")) {
						JSONObject citation = new JSONObject().put("source", comment.getString("RefSource")).put("PMID", comment.getJSONObject("PMID").get("content").toString());
						references.put(citation);
						comments.remove(i);
						i--; // need to process this index again
					}
				}
			}
			result.put("references", references);
		}
		
		// Fix MeshHeadings is not an array, and remove MeshHeadingList level
		if (result.has("MeshHeadingList")) {
			JSONObject mhl = result.getJSONObject("MeshHeadingList");
			if (mhl.has("MeshHeading") && JSONUtilities.getJSONType(mhl.get("MeshHeading")) == JSONType.JSON_OBJECT) {
				JSONObject meshObject = mhl.getJSONObject("MeshHeading");
				JSONArray  meshArray  = new JSONArray();
				meshArray.put(meshObject);
				mhl.put("MeshHeading", meshArray);			
			}
			Object meshHeading = mhl.get("MeshHeading");
			result.put("MeshHeading", meshHeading);
			result.remove("MeshHeadingList");
		}
		
		// Fix qualifiers within MeshHeading to be an array
		if (result.has("MeshHeading")) {
			JSONArray meshHeadingArray = result.getJSONArray("MeshHeading");
			for (int i=0; i < meshHeadingArray.length(); i++) {
				JSONObject meshObject = meshHeadingArray.getJSONObject(i);
				if (meshObject.has("QualifierName") && JSONUtilities.getJSONType(meshObject.get("QualifierName")) != JSONType.JSON_ARRAY) {
					JSONObject qualObject = meshObject.getJSONObject("QualifierName");
					JSONArray  qualArray  = new JSONArray();
					qualArray.put(qualObject);
					meshObject.put("QualifierName", qualArray);
				}
			}
		}
		
		// simplify PMID
		if (result.has("PMID")) {
			try {
				String pmid = result.getJSONObject("PMID").get("content").toString();
				result.put("PMID", pmid);
			}
			catch (Exception e) {
				logger.log(Level.WARNING,"Unable to simplify PMID: "+e.toString()+"\n"+result.toString(4));
			}
		}
		
		
		return result;
	}
	
	/**
	 * Refactors the "PubmedData" portion of a PubmedArticle
	 * Converts the date fields into a single date.  converts the arrays for IDs and status into objects
	 * for easier retrieval.
	 * 
	 * @param pubmedDataOriginal
	 * @return
	 */
	public static JSONObject refactorPubmedData(JSONObject pubmedDataOriginal) {
		JSONObject pubmedDataNew      = new JSONObject();
		pubmedDataNew.put("PublicationStatus", pubmedDataOriginal.get("PublicationStatus"));
		
		JSONObject articleIDList = pubmedDataOriginal.getJSONObject("ArticleIdList");
		if (articleIDList.has("ArticleId")) {
			if (JSONUtilities.getJSONType(articleIDList.get("ArticleId")) == JSONType.JSON_OBJECT) {
				JSONObject jo = articleIDList.getJSONObject("ArticleId");
				JSONObject idObject = new JSONObject().put(jo.getString("IdType"), jo.get("content").toString());
				pubmedDataNew.put("id", idObject);
			}
			else {
				JSONArray idArray = articleIDList.getJSONArray("ArticleId");
				JSONObject idObject = new JSONObject();
				for (int i=0; i< idArray.length(); i++) {
					JSONObject jo = idArray.getJSONObject(i);
					idObject.put(jo.getString("IdType"), jo.get("content").toString());
				}
				pubmedDataNew.put("id", idObject);
			}
		}
		

		JSONObject historyObj = pubmedDataOriginal.optJSONObject("History");
		if (historyObj != null && historyObj.has("PubMedPubDate")) {
			if (JSONUtilities.getJSONType(historyObj.get("PubMedPubDate")) == JSONType.JSON_OBJECT) {
				JSONObject jo = historyObj.getJSONObject("PubMedPubDate");
				JSONObject historyObject = new JSONObject().put(jo.getString("PubStatus"), refactorObjectDateFields(jo));
				pubmedDataNew.put("pubStatus", historyObject);	
			}
			else {
				JSONArray historyArray = historyObj.getJSONArray("PubMedPubDate");
				JSONObject historyObject = new JSONObject();
				for (int i=0; i< historyArray.length(); i++) {
					JSONObject jo = historyArray.getJSONObject(i);
					historyObject.put(jo.getString("PubStatus"), refactorObjectDateFields(jo));
				}
				pubmedDataNew.put("pubStatus", historyObject);	
			}
		}
		
	
		return pubmedDataNew;
	}
	
	
	public static String refactorObjectDateFields(JSONObject jo) {
		int year   = jo.optInt("Year",  0);
		int day    = jo.optInt("Day",   1);
		int hour   = jo.optInt("Hour",  0);
		int minute = jo.optInt("Minute",0);
		int month  = jo.optInt("Month", 1);
		
		if (jo.has("Month") && JSONUtilities.getJSONType(jo.get("Month")) == JSONType.STRING) {
			switch(jo.getString("Month").toLowerCase()) {
			case "jan": month =  1; break;
			case "feb": month =  2; break;
			case "mar": month =  3; break;
			case "apr": month =  4; break;
			case "may": month =  5; break;
			case "jun": month =  6; break;
			case "jul": month =  7; break;
			case "aug": month =  8; break;
			case "sep": month =  9; break;
			case "oct": month = 10; break;
			case "nov": month = 11; break;
			case "dec": month = 12; break;
			}
		}
		
		while (!DateUtilities.isValid(day, month, year)) {
			day = day -1;
			if (day <=0) {
				day = 1;
				month = 1;
			}
		}
		
		ZonedDateTime zdt = ZonedDateTime.of(year, month,day,hour,minute,0,0,ZoneId.of("UTC"));
		
		return DateTimeFormatter.ISO_INSTANT.format(zdt);
	}
	
	
	public static int[] countJSONRecordsWithKeywords(ConfigurationType location, String keywords[]) throws Exception {
		if (location != ConfigurationType.PUBMEDIMPORTER_BASELINE && location != ConfigurationType.PUBMEDIMPORTER_UPDATES) {
			logger.log(Level.SEVERE, "Invalid location passed, skipping download: "+location.getFullLabel());
		}
		String localCacheDir = Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.PUBMEDIMPORTER_BASEDIRECTORY) +
	                           Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,location);
		
		int totalRecords = 0;
		int foundRecords = 0;
		
		//Executor parsingPool = Executors.newFixedThreadPool(1);
		
		for (File f: (new File(localCacheDir)).listFiles() ) {
			if (f.getName().endsWith("xml.gz") == false) {continue;}
			//System.out.println(f.getName() + ":medline17n1099.xml.gz"); 
			/*
			if (f.getName().compareTo("medline17n1122.xml.gz") < 0) {
				logger.log(Level.INFO, "skipping "+f);
				continue;
			}
			*/
			
			//parsingPool.execute(new Runnable() {
			//	@Override
			//	public void run() {
			logger.log(Level.INFO, "Extracting from "+f);
			JSONObject jo = null;
			try {
				jo = PubMedUtilities.convertGZippedXMLFile(f);
			}
			catch (Throwable t) {
				logger.log(Level.SEVERE, "Unable to convert XML file: "+f,t);
			}
			logger.log(Level.INFO, "converted to XML");
			
			JSONArray articleList = jo.getJSONArray("PubmedArticle");
			for (int i=0; i < articleList.length(); i++) {
				totalRecords++;
				try {
					JSONObject article = articleList.getJSONObject(i);
					JSONObject refactoredArticle = PubMedUtilities.refactorCitation(article);
	
					if (PubMedProcessor.recordHasKeyword(refactoredArticle, keywords)) {
						foundRecords++;
					}
				}
				catch (Throwable t) {
					logger.log(Level.SEVERE, "Uncaught exception: "+ t.toString(),t);
				}
			}
			articleList = null; // hints to the garbage collector
			jo = null;
			System.gc();
			logger.log(Level.INFO, "completed "+f);
				//}
			//});
		}
		int[] results = { totalRecords, foundRecords };
		return results;
		
	}	
	
}
