package edu.ncsu.las.model.collector.concept;

import java.util.UUID;

public class ConceptCategoryTable {

	java.util.Map<UUID, ConceptCategory> _categoryTableByUUID;
	java.util.Map<String, ConceptCategory> _categoryTableByName;

	
	public ConceptCategoryTable(String domainName){
		_categoryTableByUUID = ConceptCategory.getAllConceptsWithFullName(domainName); 
		this.createTableByName();
	}

	public ConceptCategoryTable(java.util.Map<UUID, ConceptCategory> table) {
		_categoryTableByUUID = table;
		this.createTableByName();
	}
	
	private void createTableByName() {
		_categoryTableByName = new java.util.HashMap<String, ConceptCategory>();
		for (ConceptCategory cc: _categoryTableByUUID.values()) {
			_categoryTableByName.put(cc.getFullCategoryName().toLowerCase(), cc);
		}
	}
	
	public ConceptCategory getConceptCategory(UUID categoryID) {
		return _categoryTableByUUID.get(categoryID);
	}
	
	public ConceptCategory getConceptCategory(String categoryID) {
		return _categoryTableByUUID.get(UUID.fromString(categoryID));
	}	
	
	/**
	 * 
	 * @param originalName
	 * @return
	 */
	public ConceptCategory getConceptCategoryByFullName(String domain, String originalName) {
		String name = originalName.toLowerCase();
		if (_categoryTableByName.containsKey(name) == false) {
			String names[] = name.split("\\.");
			int counter = 0;
			String catName = "";
			UUID parent = ConceptCategory.ROOT_UUID;
			
			while (counter < names.length) {
				if (counter > 0) { catName = catName +"."; }
				catName = catName + names[counter];
				if (_categoryTableByName.containsKey(catName)) {
					parent = _categoryTableByName.get(catName).getCategoryID();
				}
				else {
					ConceptCategory newCategory = new ConceptCategory(edu.ncsu.las.util.UUID.createTimeUUID(), domain, originalName.split("\\.")[counter],parent);
					newCategory.createConceptCategory();
					_categoryTableByUUID.put(newCategory.getCategoryID(), newCategory);
					_categoryTableByName.put(catName,newCategory);
					parent = newCategory.getCategoryID();
				}
				counter++;
			}
		}
		
		return _categoryTableByName.get(name);
	}
	
}