package edu.ncsu.las.topicmodel.model;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class StringUtilities {
	private static String[] words = {"&","a","an","the","above","across","after","at","around","before","behind","below","beside","between","by","down","during","for","from","in","inside","onto","of","off","on","out","through","to","under","up","with","and","or","but"};
	
	private static Pattern punctuationRegEx  = Pattern.compile("[.,!\\?;:]");
	private static Pattern improperNameRegex = Pattern.compile("[^a-zA-Z_ \\-,0-9\\.\\(\\)]+");
	
	private static HashSet<String> ignoreWordList = new HashSet<String>();
	static {
		ignoreWordList.addAll(Arrays.asList(words));
	}
	
	public static boolean hasPunctuation(String text) {
		return punctuationRegEx.matcher(text).find();
	}

	
	public static String[] getWords(String text) {
		return text.split("\\s+");
	}
	
	public static int getNumberOfWords(String text) {
		return getWords(text).length;
	}
	
	public static boolean allWordsHaveUpperCaseLetter(String text) {
		String[] words= getWords(text);
		if (words.length == 0) { return false; }
		
		for (String w: words) {
			if (ignoreWordList.contains(w)) { continue; }
			if (w.equals(w.toLowerCase())) { return false;}
		}
		return true;
	}
	
	public static String eliminateNonSentences(String text) {
		String[] lines = text.split("\n");
		java.util.ArrayList<String> result = new java.util.ArrayList<String>();
		for (String line: lines) {
		    if (getNumberOfWords(line) > 0 && !allWordsHaveUpperCaseLetter(line)) {
		      result.add(line);
		    }
		}
		
		return result.stream().collect(Collectors.joining("\n"));
	}
	
	public static String removeNonASCII(String text, String replaceWith) {
		return text.replaceAll("[^\\x00-\\x7F]", replaceWith);
	}
	
	/**
	 * Removes extraneous newlines and spaces
	 * 
	 * @param text
	 * @return
	 */
	public static String cleanText(String text) {
		String previous = "";
		while (text.equals(previous) == false) {
			previous = text;
			text = text.replaceAll("\t", " ");
			text = text.replaceAll("\n\n", "\n");
		    text = text.trim().replaceAll(" +", " ");
		    text = text.replaceAll("\n \n", "\n");
		    text = text.replaceAll("\n ", "\n");
		}
		return text;
	}
	
	public static String stemText(String text) {
		StringBuilder result = new StringBuilder();
		PorterStemmer ps = new PorterStemmer();
		
		String separators = " \t\n\r\f,.:;?![]'";
		
		StringTokenizer tokenizer = new StringTokenizer(text, separators, true);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			
			if (token.length() == 1 && separators.indexOf(token) > -1) {
				result.append(token);
			}
			else {
				String word = ps.stem(token);
				if (!isStringNumeric(word)) {
					result.append(word);
				}
			}
		}
		
		return result.toString();
	}
	
	public static boolean isStringNumeric( String str )
	{
	    DecimalFormatSymbols currentLocaleSymbols = DecimalFormatSymbols.getInstance();
	    char localeMinusSign = currentLocaleSymbols.getMinusSign();

	    if ( !Character.isDigit( str.charAt( 0 ) ) && str.charAt( 0 ) != localeMinusSign ) return false;

	    boolean isDecimalSeparatorFound = false;
	    char localeDecimalSeparator = currentLocaleSymbols.getDecimalSeparator();

	    for ( char c : str.substring( 1 ).toCharArray() )
	    {
	        if ( !Character.isDigit( c ) )
	        {
	            if ( c == localeDecimalSeparator && !isDecimalSeparatorFound )
	            {
	                isDecimalSeparatorFound = true;
	                continue;
	            }
	            return false;
	        }
	    }
	    return true;
	}
	
	public static String decapitalize(String string) {
	    if (string == null || string.length() == 0) {
	        return string;
	    }
	    char c[] = string.toCharArray();
	    c[0] = Character.toLowerCase(c[0]);
	    return new String(c);
	}
	
	public static boolean isAlphaNumberWithLimitedSpecialCharacters(String text) {
			return !(improperNameRegex.matcher(text).find());
	}
	
}
