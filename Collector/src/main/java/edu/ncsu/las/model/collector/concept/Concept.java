package edu.ncsu.las.model.collector.concept;

import edu.ncsu.las.collector.util.TikaUtilities;
import edu.ncsu.las.model.collector.Configuration;
import edu.ncsu.las.model.collector.type.ConfigurationType;
import edu.ncsu.las.model.nlp.SentenceSegmenter;
import edu.ncsu.las.persist.collector.ConceptDAO;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class Concept {
	private static Logger logger = Logger.getLogger(Concept.class.getName());
	
	private UUID id;
	private UUID categoryId;
	private String name;
	private String type;
	private String regex;
	private String domainInstanceName;
	
	private Pattern _regexPattern = null;
	
	public Concept(){
		
	}
	
	public Concept(UUID id,String domainInsatnceName,UUID categoryId,String name,String type,String regex){
		this.id=id;
		this.domainInstanceName = domainInsatnceName;
		this.categoryId=categoryId;
		this.name=name;
		this.type=type;
		this.regex=regex;
	}
	
	public Concept(JSONObject jo){
		this.id         = UUID.fromString(jo.getString("id"));
		this.categoryId = UUID.fromString(jo.getString("categoryid"));
		this.name  = jo.getString("name");
		this.type  = jo.getString("type");
		this.regex = jo.getString("regex");
		this.domainInstanceName = jo.getString("domainInstanceName");
		
		
		
	}

	public Concept(UUID categoryIDForRecord, String conceptName, String conceptType, String conceptRegEx, String domainInstanceName) {
		this.id = edu.ncsu.las.util.UUID.createTimeUUID();
		this.categoryId=categoryIDForRecord;
		this.name=conceptName;
		this.type=conceptType;
		this.regex=conceptRegEx;
		this.domainInstanceName=domainInstanceName;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(UUID categoryId) {
		this.categoryId = categoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}
	
	
	public String getDomainInstanceName() {
		return domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		this.domainInstanceName = domainInstanceName;
	}
	
	
	public Pattern getRegexPattern() {
		if (_regexPattern == null) {
			_regexPattern = Pattern.compile(this.getRegex());
		}
		return _regexPattern;
	}
	

	public boolean insertConcept(){
		return (new ConceptDAO()).insert(this);
	}
	
	public static List<Concept> getAllConcepts(String domainInstanceName)
	{
		ConceptDAO d = new ConceptDAO();
		return d.selectAll(domainInstanceName);
	}	
	
	public static List<Concept> getConceptWithCategoryId(UUID uuid)
	{
		ConceptDAO d = new ConceptDAO();
		return d.selectConceptsByCategoryID(uuid);
	}
	
	public static void deleteConceptByCategoryID(UUID uuid){
		ConceptDAO d = new ConceptDAO();
		d.deleteByCategoryID(uuid);
	}
	
	public static void deleteConcept(UUID uuid){
		ConceptDAO d = new ConceptDAO();
		d.delete(uuid);
	}
	
	public  JSONObject toJSONObject() {
		JSONObject jo = new JSONObject().put("id",this.getId().toString())
                                        .put("name", this.getName())
                                        .put("type", this.getType())
                                        .put("regex", this.getRegex())
                                        .put("categoryid", this.getCategoryId().toString())
                                        .put("domainInstanceName", this.getDomainInstanceName());
		return jo;
	}
	
	public String toString() {
		return toJSONObject().toString();
	}
	
	public static boolean validateRegex(String regex){
		try{
			Pattern.compile(regex) ;
		}
		catch(java.util.regex.PatternSyntaxException pse){
			logger.log(Level.FINER,"Invalid regex: "+regex);
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param text
	 * @param domain
	 * @param domainCategoryTable
	 * @param concepts
	 * @return
	 */
	public static JSONArray annotateConcepts(String text, String domain, ConceptCategoryTable domainCategoryTable, java.util.ArrayList<Concept> concepts){
		JSONArray foundConcepts = new JSONArray();
		
		// The following is necessary because a regular expression can appear to take an arbitrarily long period due to backtracking...
		final ExecutorService service = Executors.newSingleThreadExecutor();
		
		JSONObject lastConcept = new JSONObject();
		
		try {
			final Future<Object> f = service.submit(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					List<String> sentences = null;
					boolean bySentence = false;
					if ("sentence".equals(Configuration.getConfigurationProperty(domain, ConfigurationType.CONCEPT_PARSE))) {
						bySentence = true;
						sentences = SentenceSegmenter.segment(text, TikaUtilities.detectLanguage(text));
					}
					for (Concept concept: concepts) {
						lastConcept.put("lastConcept",concept.toJSONObject());
						//logger.log(Level.FINEST, "Concept: "+ concept.getName() +", pattern: "+ concept.getRegexPattern());
						if (bySentence) {
							parseBySentence(concept,sentences);
						}
						else {
							parseByDocument(concept,text);
						}
					}
					return null;
				}
				
				private void parseBySentence(Concept concept, List<String> sentences) {
					for (int i = 0; i < sentences.size(); i++) {
						Matcher m = concept.getRegexPattern().matcher( sentences.get(i)); // where in document to search?
						while (m.find()) {
							JSONObject jo = new JSONObject();
							jo.put("category", domainCategoryTable.getConceptCategory(concept.getCategoryId()).getFullCategoryName());
							jo.put("fullName", domainCategoryTable.getConceptCategory(concept.getCategoryId()).getFullCategoryName()+"."+concept.getName());
							jo.put("name", concept.getName());
							jo.put("type", concept.getType());
							jo.put("value", m.group(0).replace("\n", " ").trim());
							jo.put("sentencePosition", m.start());
							jo.put("sentenceIndex", i);
							foundConcepts.put(jo);
						}
					}
					
				}

				public void parseByDocument(Concept concept, String text) {
					Matcher m = concept.getRegexPattern().matcher(text); // where in document to search?
					while (m.find()) {
						JSONObject jo = new JSONObject();
						ConceptCategory cc = domainCategoryTable.getConceptCategory(concept.getCategoryId());
						if (cc == null) {
							logger.log(Level.WARNING, "Unable to find concept category: "+concept.toJSONObject());
							jo.put("category", concept.getName());
							jo.put("fullName", concept.getName());
						}
						else {
							jo.put("category", cc.getFullCategoryName());
							jo.put("fullName", cc.getFullCategoryName()+"."+concept.getName());
						}
						
						jo.put("name", concept.getName());
						jo.put("type", concept.getType());
						jo.put("value", m.group(0).replace("\n", " ").trim());
						jo.put("position", m.start());
						foundConcepts.put(jo);
					}
				}
				
			});

			int timeoutSeconds = Configuration.getConfigurationPropertyAsInt(domain, ConfigurationType.CONCEPT_TIMEOUTSEC);
			if (timeoutSeconds == 0) { timeoutSeconds = 600; }
			
			f.get(Math.max(concepts.size(), timeoutSeconds), TimeUnit.SECONDS);
			f.cancel(true);
				
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Unable to annotate concepts: "+ex);
			logger.log(Level.SEVERE, "Unable to annotate concepts - last concept: "+lastConcept.toString());
			ex.printStackTrace();
			//logger.log(Level.SEVERE, "Unable to annotate concepts - text: "+data);  // this can print out a lot of garbage

		}
		service.shutdownNow();

		return foundConcepts;
	}		
	
}