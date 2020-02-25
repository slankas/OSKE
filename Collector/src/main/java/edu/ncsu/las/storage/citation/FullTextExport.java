package edu.ncsu.las.storage.citation;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;



public class FullTextExport {

	public static final HashSet<String> sectionLabels = new HashSet<String>();
	static {
		sectionLabels.add("abstract");
		sectionLabels.add("acknowledgments");
		sectionLabels.add("references");
		sectionLabels.add("supplementary data");
		sectionLabels.add("methods and protocols");
		sectionLabels.add("background");
		sectionLabels.add("introduction");
		sectionLabels.add("results");
		sectionLabels.add("discussion");
		sectionLabels.add("conclusion");
		sectionLabels.add("future work");
		sectionLabels.add("related work");
		sectionLabels.add("materials");
		sectionLabels.add("materials and methods");
		sectionLabels.add("methods and materials");
		sectionLabels.add("methods");
		sectionLabels.add("concluding remarks");
		sectionLabels.add("conclusions");
		sectionLabels.add("competing interests");
	}
 	
	public static void main(String[] args) throws Exception {
		
		String jsonRecordLocation = "C:\\pubmed\\pubmed\\extractedRecords\\";
				
		String extractedTextLocation = "C:\\pubmed\\pubmed\\text.csv";
		CSVFormat csvFileFormat = CSVFormat.RFC4180;
		FileWriter fw = new FileWriter(extractedTextLocation);
		CSVPrinter csvFilePrinter = new CSVPrinter(fw, csvFileFormat);
		
		Object [] FILE_HEADER = {"count","section","junk","text"};
		csvFilePrinter.printRecord(FILE_HEADER);
		
		HashMap<String,Integer> counts = new HashMap<String,Integer>();
		
		for (File f: (new File(jsonRecordLocation)).listFiles() ) {
			if (f.getName().endsWith(".json") == false) {continue;}
		
			JSONObject record = PubMedProcessor.loadRecord(f);
			if (!record.has("text")) {
				continue;
			}
			String text = record.getString("text");
			String lines[] = text.split("\n");
			
			for (String line: lines) {
				line = line.toLowerCase().trim();
				int count = 0;
				if (counts.containsKey(line)) {
					count = counts.get(line);
				}
				count = count +1;
				counts.put(line, count);
			}
			
			
		}
		
		for (String line: counts.keySet()) {
			//System.out.println(line);
				
			Integer count   = counts.get(line);
			Boolean section = sectionLabels.contains(line);
				
			Boolean junk = (line.length() < 4) || StringUtils.isNumeric(line);
				
		        Object[] row = new Object[4];
		        row[0] = count;
		        row[1] = section;
		        row[2] = junk;
		        row[3] = line;


		        csvFilePrinter.printRecord(row);
		}	
		

		
		csvFilePrinter.flush();
		csvFilePrinter.close();
	}

}
