package edu.ncsu.las.storage.citation;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;


public class AcademicPDFUtilities {
	
	public static enum DocumentSection {
		//Authors' contributions
		
		ABSTRACT("abstract",false,false),
		ACKNOWLEDGMENTS("acknowledgments",false,false),
		ADDITIONAL_FILES("additional files",false,false),
		AUTHOR_CONTRIBUTIONS("author contributions",false,false),
		AUTHOR_DETAILS("author details",false,false),
		BACKGROUND("background",false,false),
		CASE_STUDY("case study",false,false),
		CASE_STUDIES("case studies",false,false),
		COMPETING_INTERESTS("competing interests",false,false),
		CONCLUDING_REMARKS("concluding remarks",false,false),
		CONCLUSION("conclusion",false,false),
		CONCLUSIONS("conclusions",false,false),
		DECLARATION_OF_INTEREST("declaration of interest",false,false),
		DISCUSSION("discussion",false,false),
		EXPIREMENTAL_PROCEDURES("experimental Procedures",true,false),
		FUNDING("funding",false,false),
		FUTURE_WORK("future work",false,false),
		INPUT("input",false,false),
		INPUTS("inputs",false,false),
		INPUT_AND_OPTIONS("input and options",false,false),
		INTRODUCTION("introduction",false,false),
		LITERATURE_CITED("literature cited",false,true),
		MATERIALS("materials",true,false),
		MATERIAL_AND_METHODS("material and methods",true,false),
		MATERIALS_AND_METHODS("materials and methods",true,false),
		METHODS("methods",true,false),
		METHODS_AND_PROTOCOLS("methods and protocols",true,false),
		METHODS_AND_MATERIALS("methods and materials",true,false),
		METHOD_DETAILS("method details",true,false),
		OUTPUT("output",false,false),
		OUTPUTS("outputs",false,false),
		REFERENCES("references",false,true),
		RELATED_WORK("related work",false,false),
		RESULTS("results",false,false),
		RESULTS_AND_DISCUSSION("results and discussion",false,false),
		SUMMARY("summary",false,false),
		SUPPLEMENTARY_DATA("supplementary data",false,false);		
		private String _label;
		private boolean _methodSection;
		private boolean _references;
		
		private DocumentSection(String label, boolean methodSection, boolean references) {
			_label         = label;
			_methodSection = methodSection;
			_references    = references;
		}
		
		public String getLabel() { return _label; }
		public String toString() { return _label; }
		public boolean isMethodSection() { return _methodSection; }
		public boolean isReferences()    { return _references;    }
		
		public static DocumentSection getEnum(String label) {
			return DocumentSection.valueOf(label.trim().toUpperCase().replaceAll(" ", "_"));
		}
		
		private static HashSet<String> PDF_SECTION_LABELS = null;
		
		private static HashSet<String> getSectionLabels() {
			if (PDF_SECTION_LABELS == null) {
				HashSet<String> result = new HashSet<String>();
				for (DocumentSection ds : DocumentSection.values()) {
					result.add(ds.getLabel());
				}
				PDF_SECTION_LABELS = result;
			}
			return PDF_SECTION_LABELS;
		}
		
		public static boolean doesStringSignifyHeader(String value) {
			return DocumentSection.getSectionLabels().contains(value.toLowerCase());
		}
	}
	
	
	static Pattern PATTERN_STARTS_WITH_NUMBER = Pattern.compile("^\\d+\\.");
	static Pattern PATTERN_STARTS_WITH_NUMBER_IEEE = Pattern.compile("^\\[\\d+\\]");
	
	/**
	 * Cleans up the text extracted from academic literature PDFs
	 * @param initialText
	 * @return
	 */
	public static String cleanTextFromLiteraturePDF(String initialText) {
		initialText = initialText.replaceAll("\\r", "");
        
        String lines[] = initialText.split("\n");
        java.util.ArrayList<String> joinedLines = new java.util.ArrayList<String>(lines.length);
        boolean inReferences = false;
        boolean firstLineReferenceCheck = false;
        boolean referencesUsesNumbers = false;
        boolean ieeeFormatReferences = false;  //this format uses [number] on the references
        for (String line: lines) {
        	line = line.trim();
        	if (line.length() == 0) { continue; }
        	
        	if (inReferences && firstLineReferenceCheck == false) {
        		firstLineReferenceCheck = true;
        		if (PATTERN_STARTS_WITH_NUMBER.matcher(line).find()) {
        			referencesUsesNumbers = true;
        		}
        		else if (PATTERN_STARTS_WITH_NUMBER_IEEE.matcher(line).find()) {
        			ieeeFormatReferences = true;
        			referencesUsesNumbers = true;
        		}
        	}
        	
        	if ( (java.lang.Character.isUpperCase(line.charAt(0)) || java.lang.Character.isDigit(line.charAt(0))   )&& 
        		(joinedLines.size() == 0  || ! (endsWithPreposition(joinedLines.get(joinedLines.size()-1)) || endsWithComma(joinedLines.get(joinedLines.size()-1))))  
        		) {
        			if (inReferences && referencesUsesNumbers) {
        				if (PATTERN_STARTS_WITH_NUMBER.matcher(line).find() || (ieeeFormatReferences && PATTERN_STARTS_WITH_NUMBER_IEEE.matcher(line).find() )) {
        					joinedLines.add(line);
        				}
        				else {
                			String prevLine = joinedLines.get(joinedLines.size()-1);
	                			             			
	               			String newLine = prevLine +" " + line;
	              			joinedLines.remove(joinedLines.size()-1);
	               			joinedLines.add(newLine);
        				}
        			}
        			else {
        				joinedLines.add(line);
        			}
        	}
        	else if (java.lang.Character.isUpperCase(line.charAt(0)) && lineContainsSectionHeader(line)) {
        		joinedLines.add(line); //if it's uppercase and one of our section labels.
        	}
        	else if (joinedLines.size() >0 && joinedLines.get(joinedLines.size()-1).endsWith(".")) {
    			if (inReferences) {
    				if ((PATTERN_STARTS_WITH_NUMBER.matcher(line).find() || (ieeeFormatReferences && PATTERN_STARTS_WITH_NUMBER_IEEE.matcher(line).find() ) )&& referencesUsesNumbers) {
    					joinedLines.add(line);
    				}
    				else {
    					if (lineContainsSectionHeader(joinedLines.get(joinedLines.size()-1))) {
    						joinedLines.add(line);
    					}
    					else {
                			String prevLine = joinedLines.get(joinedLines.size()-1);
                			
                			
                			String newLine = prevLine +" " + line;
                			joinedLines.remove(joinedLines.size()-1);
                			joinedLines.add(newLine);
    					}
    				}
    			}
    			else {
    				joinedLines.add(line);
    			}
        	}
        	else {
        		if (joinedLines.size() > 0 && !lineContainsSectionHeader(joinedLines.get(joinedLines.size()-1)) ) {
        			String prevLine = joinedLines.get(joinedLines.size()-1);
        			
        			
        			String newLine = prevLine +" " + line;
        			joinedLines.remove(joinedLines.size()-1);
        			joinedLines.add(newLine);
        		}
        		else {
        			joinedLines.add(line);
        		}
        		
        	}
        	if (line.equalsIgnoreCase("references") || line.equalsIgnoreCase("literature cited")) {
        		inReferences = true;
        	}
        	else if (inReferences && lineContainsSectionHeader(line)) {
        		inReferences = false;
        	}
        }
        java.util.ArrayList<String> resultLines = new java.util.ArrayList<String>(joinedLines.size());
        for (String line: joinedLines) {
        	
        	if (line.equals("A uthor M anuscript") || line.equals("Author manuscript") || line.equals("HHS Public Access")) {
        		continue;
        	}
        	if (line.startsWith("A uthor M anuscript")) {
        		line = line.substring("A uthor M anuscript".length()+1);
        	}
        	resultLines.add(line.replaceAll("- ", "")); //remove hyphens that aren't needed any more
        }
        String finalText = String.join("\n", resultLines);
        return finalText;
	}

	
	private static final Pattern SECTION_NUMBER = Pattern.compile("^(X?V?I{1,3}|V|IX|X|\\d+|[A-Za-z])(\\.|\t| )");
	
	/**
	 * Checks if the given line starts a section or not.  
	 * 
	 * Logic:
	 * 1) Is the line a section header by itself?
	 * 2) Is the line prefixed with a numbering system followed by one of our section titles?
	 *    we'll use recursion to deal with things such III.A Results
	 * 
	 * @param line
	 * @return
	 */
	public static boolean lineContainsSectionHeader(String line) {
		//((I[XV]|V?I{0,3})|\d+|[A-Za-z])\.?
		line = line.trim();
		
		if (DocumentSection.doesStringSignifyHeader(line)) {
			return true;
		}
		
		Matcher section = SECTION_NUMBER.matcher(line);
		if (section.find()) {
			int lastPos = section.end();
			if ((lastPos+1) < line.length()) {
				return lineContainsSectionHeader(line.substring(lastPos+1));
			}
		}
		
		return false;
	}
	
	
	/**
	 * Checks if the given line starts a section or not.  
	 * 
	 * Logic:
	 * 1) Is the line a section header by itself?
	 * 2) Is the line prefixed with a numbering system followed by one of our section titles?
	 *    we'll use recursion to deal with things such III.A Results
	 * 
	 * @param line
	 * @return
	 */
	public static DocumentSection matchingDocumentSectionToline(String line) {
		//((I[XV]|V?I{0,3})|\d+|[A-Za-z])\.?
		line = line.trim();
		
		if (DocumentSection.doesStringSignifyHeader(line)) {
			return DocumentSection.getEnum(line);
		}
		
		Matcher section = SECTION_NUMBER.matcher(line);
		if (section.find()) {
			int lastPos = section.end();
			if ((lastPos+1) < line.length()) {
				return matchingDocumentSectionToline(line.substring(lastPos+1));
			}
		}
		
		return null;
	}	
	
	private static boolean endsWithComma(String string) {
		return string.endsWith(",");
	}
	
	
	public static final String[] PREPOSITION_LIST = { "aboard", "about", "above", "absent", "across", "after", "against", "along", "alongside", "amid", "among", "amongst ", "around", "as", "astride", "at", "on", "atop, ontop", "bar", "before", "behind", "below", "beneath", "beside", "besides", "between", "beyond", "but", "by", "circa", "come", "despite", "down", "during", "except", "for", "from", "in", "inside", "into", "less", "like", "minus", "near", "nearer", "nearest", "notwithstanding", "of", "off", "on", "onto", "opposite", "out", "outside", "over", "past", "per", "post ", "pre ", "pro ", "qua ", "re ", "sans ", "save", "sauf ", "short", "since", "sithence ", "than", "through", "thru ", "throughout", "thruout ", "to", "toward, towards", "under", "underneath", "unlike", "until", "up", "upon", "upside", "versus", "via", "vice", "vis-à-vis ", "with", "within", "without", "worth", "according to", "adjacent to", "ahead of", "apart from", "as for", "as of", "as per", "as regards", "aside from", "back to", "because of", "close to", "due to", "except for", "far from", "inside of", "instead of", "left of", "near to", "next to", "opposite of", "opposite to", "out from", "out of", "outside of", "owing to", "prior to", "pursuant to", "rather than", "regardless of", "right of", "subsequent to", "such as", "thanks to", "up to", "as far as", "as opposed to", "as soon as", "as well as" };

	private static boolean endsWithPreposition(String string) {
		for (String prep: PREPOSITION_LIST) {
			if (string.endsWith(prep)) { return true; }
		}
		return false;
	}	
	
	public static int numPagesInPDF(File pdfFile) throws InvalidPasswordException, IOException {
		PDDocument doc = PDDocument.load(pdfFile);
		int count = doc.getNumberOfPages();
		return count;
	}
}
