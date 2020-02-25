package edu.ncsu.las.model.collector.concept;
import edu.ncsu.las.persist.collector.ConceptCategoryDAO;
import edu.ncsu.las.persist.collector.ConceptDAO;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

public class ConceptCategory {
	private UUID _categoryID;
	private String _categoryName;
	private UUID _parentID;
	private String _domainInstanceName;
	private String _fullCategoryName;  // This includes the full path up to the root node - levels are separated by a "." pestle.social.religion
	
	public static final UUID ROOT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	
	public ConceptCategory(){
		
	}
	
	public ConceptCategory(UUID categoryID,String domainInstanceName, String categoryName,UUID parentID){
		super();
		this._categoryID= categoryID;
		this._categoryName=categoryName;
		this._parentID=parentID;
		this._domainInstanceName = domainInstanceName;
	}
	
	public UUID getCategoryID() {
		return _categoryID;
	}
	
	public void setCategoryID(UUID uuid) {
		this._categoryID = uuid;
	}
	
	public String getCategoryName() {
		return _categoryName;
	}
	public void setCategoryName(String categoryName) {
		this._categoryName = categoryName;
	}
	
	public String getFullCategoryName() {
		return this._fullCategoryName;
	}
	
	public UUID getParentID() {
		return _parentID;
	}
	public void setParentID(UUID parentId) {
		this._parentID = parentId;
	}
	
	public String getDomainInstanceName() {
		return _domainInstanceName;
	}

	public void setDomainInstanceName(String domainInstanceName) {
		this._domainInstanceName = domainInstanceName;
	}

	public boolean createConceptCategory(){
		return (new ConceptCategoryDAO()).createConceptCategory(this);
	}
	
	public List<Concept> getConcepts()
	{
		ConceptDAO d = new ConceptDAO();
		return d.selectConceptsByCategoryID(this.getCategoryID());
	}
	
	public static List<ConceptCategory> getAllConceptCategories(String domain){
		ConceptCategoryDAO d = new ConceptCategoryDAO();
		return d.selectAll(domain);
	}
	
	public static void deleteCategory(UUID uuid){
		ConceptCategoryDAO d = new ConceptCategoryDAO();
		d.deleteCategory(uuid);
	}

	public static java.util.Map<UUID, ConceptCategory> getAllConceptsWithFullName(String domain) {
		List<ConceptCategory> categories = ConceptCategory.getAllConceptCategories(domain);

		HashMap<UUID, ConceptCategory> results = new HashMap<UUID, ConceptCategory>();
		for (ConceptCategory cc: categories) {
			results.put(cc.getCategoryID(), cc);
		}
		// now that all of the entries are in place, go back and update the full category names
		for (ConceptCategory cc: categories) {
			cc._fullCategoryName = ConceptCategory.getFullCategoryName(cc,results);
		}
		
		return results;		
	}
	
	private static String getFullCategoryName(ConceptCategory concept, java.util.Map<UUID, ConceptCategory> categories) {
		ConceptCategory parent = categories.get(concept.getParentID());
		
		if (parent==null) {
			return concept.getCategoryName();
		}
		
		if (parent.getFullCategoryName() == null) { // This method basically allows us to use "memoziation" complete the results, rather than complete regression
			parent._fullCategoryName = ConceptCategory.getFullCategoryName(parent,categories);
		}
		return parent._fullCategoryName +"." + concept.getCategoryName();
	}
	
	public JSONObject toJSON() {
		JSONObject category = new JSONObject()
			.put("id", this.getCategoryID().toString())
			.put("name", this.getCategoryName())
			.put("parentid",this.getParentID().toString());
		return category;
	}
}