package edu.ncsu.las.model.nlp;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Split the given text by sentences
 * 
 */
public class SentenceSegmenter {
	
	/**
	 * 
	 * @param text
	 * @param lang 2 letter abbreviation for the language (e.g.: en, es, de, etc.)
	 * @return
	 * @throws Exception
	 */
	public static List<String> segment(String text, String lang) throws Exception {
		List<String> res = new ArrayList<>();
		BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(new Locale(lang));
		sentenceIterator.setText(text);
		int prevBoundary = sentenceIterator.first();
		int curBoundary = sentenceIterator.next();
		while (curBoundary != BreakIterator.DONE) {
			String sentence = text.substring(prevBoundary, curBoundary);
			res.add(sentence);
			prevBoundary = curBoundary;
			curBoundary = sentenceIterator.next();
		}
		return res;
	}
}
