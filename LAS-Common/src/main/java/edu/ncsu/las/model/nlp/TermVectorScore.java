package edu.ncsu.las.model.nlp;

public class TermVectorScore {

	public String getWord() {
		return word;
	}

	public int getDoc_freq() {
		return doc_freq;
	}

	public int getTtf() {
		return ttf;
	}

	public int getTerm_freq() {
		return term_freq;
	}

	public double getScore() {
		return score;
	}

	private String word;
	private int doc_freq;
	private int ttf;
	private int term_freq;
	private double score;
	
	public TermVectorScore(String _word, int _doc_freq, int _ttf, int _term_freq, double _score) {
		word = _word;
		doc_freq = _doc_freq;
		ttf = _ttf;
		term_freq = _term_freq;
		score = _score;
	}

	public void addScores(int _doc_freq, int _ttf, int _term_freq, double _score) {
		doc_freq += _doc_freq;
		ttf += _ttf;
		term_freq += _term_freq;
		score += _score;
	}

}
