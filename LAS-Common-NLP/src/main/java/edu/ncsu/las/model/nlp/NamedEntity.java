package edu.ncsu.las.model.nlp;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * NamedEntity from the Stanford CoreNLP Processor
 * 
 * Types: 
 *   named: PERSON, LOCATION, ORGANIZATION, MISC
 *   numerical: MONEY, NUMBER, ORDINAL, PERCENT
 *   temporal: DATE, TIME, DURATION, SET
 *   additional: EMAIL, URL, CITY, STATE_OR_PROVINCE, COUNTRY, NATIONALITY, RELIGION, 
 *               (job) TITLE, IDEOLOGY, CRIMINAL_CHARGE, CAUSE_OF_DEATH 
 */
public class NamedEntity {

	private String _entity;
	private String _type;
	private int _sentencePosition;
	
	public NamedEntity(String entity, String type, int sentencePosition) {
		_entity = entity;
		_type   = type;
		_sentencePosition = sentencePosition;
	}
	
	public String getEntity() {
		return _entity;
	}
	public String getType() {
		return _type;
	}
	public int getSentencePosition() {
		return _sentencePosition;
	}
	
	public JSONObject toJSONObject() {
		return new JSONObject().put("entity", _entity).put("type",_type).put("pos", _sentencePosition);
	}
	
	public static JSONArray toJSONArray(java.util.List<NamedEntity> entities) {
		JSONArray result = new JSONArray();
		for (NamedEntity ner: entities) {
			result.put(ner.toJSONObject());
		}
		return result;
	}
	
	public static java.util.List<NamedEntity> extractEntities(java.util.List<Token> tokens) {
		java.util.ArrayList<NamedEntity> results = new java.util.ArrayList<NamedEntity>();
		
		String lastEntityType = "O";
		for (Token t: tokens) {
			if (t.getNamedEntity().equals("O")) { continue; }
			
			if (lastEntityType.equals(t.getNamedEntity())) { // continuation token
				NamedEntity record = results.get(results.size()-1);
				record._entity += " " + t.getWord();;
			}
			else {
				results.add(new NamedEntity(t.getWord(), t.getNamedEntity(), t.getTextPositionStart()));
			}
			lastEntityType = t.getNamedEntity();
		}
		
		return results;
	}
}
