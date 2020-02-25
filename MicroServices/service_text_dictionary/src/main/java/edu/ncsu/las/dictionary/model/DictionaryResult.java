package edu.ncsu.las.dictionary.model;

import java.util.HashSet;
import java.util.List;

import org.json.JSONObject;

/**
 * 
 *
 *
 */
public class DictionaryResult {
	private String _source;
	private String _word;
	private String _pos;
	private String _definition;
	private String _exampleSentence;
	private List<String> _synonyms; 
	
	public DictionaryResult(String source, String word, String pos, String definition, String exampleSentence, List<String> synonyms) {
		_source = source;
		_word   = word;
		_pos    = pos;
		_definition     = definition;
		_exampleSentence = exampleSentence;
		_synonyms       = synonyms;
	}
	
	public String getSource() { return _source;	}
	public String getWord()   { return _word;	}
	public String getPartOfSpeech() { return _pos;	}
	public String getDefinition() { return _definition;	}

	public JSONObject toJSONObject() {
		return new JSONObject().put("source", _source)
				               .put("word", _word)
				               .put("pos", _pos)
				               .put("definition", _definition)
				               .put("example", _exampleSentence)
				               .put("synonyms", _synonyms);
	}
	
	public String toString() {
		return toJSONObject().toString();
	}
}
