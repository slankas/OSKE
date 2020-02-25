/**
 * 
 * Create date - 20171101
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var CitationFilter = (function () {
	"use strict";

	var queryFilters = null;
	
	function getSelectedValues(selectFieldID) {
		var selectedValues = [];  
		$("#"+selectFieldID + " :selected").each(function(){
		        selectedValues.push($(this).val()); 
		    });
		return selectedValues;
	}
	
	function sortSelectListNumerically(selectFieldID) {
		var sel = $('#'+selectFieldID);
		var selected = sel.val(); // cache selected value, before reordering
		var opts_list = sel.find('option');
		opts_list.sort(function(a, b) { 
			var strA = $(a).text();
			var strB = $(b).text();
			var valueA = Number.parseInt(strA.substring(1,strA.indexOf(")")))
			var valueB = Number.parseInt(strB.substring(1,strB.indexOf(")")))
			
			return valueA > valueB ? -1 : 1; 
		});
		sel.html('').append(opts_list);
		sel.val(selected); // set cached selected value		
	}
	
	function sortSelectListAlphabetically(selectFieldID) {
		var sel = $('#'+selectFieldID);
		var selected = sel.val(); // cache selected value, before reordering
		var opts_list = sel.find('option');
		opts_list.sort(function(a, b) { return $(a).val() > $(b).val() ? 1 : -1; });
		sel.html('').append(opts_list);
		sel.val(selected); // set cached selected value		
	}	
	
	function sortNumerically() {
		sortSelectListNumerically("authorKeywordList");
		sortSelectListNumerically("meshKeywordList");
		sortSelectListNumerically("journalList");
		sortSelectListNumerically("countryList");
		sortSelectListNumerically("universityList");
		sortSelectListNumerically("authorList");
		sortSelectListNumerically("vendorList");
		sortSelectListNumerically("chemicalList");
		sortSelectListNumerically("actionList");
		sortSelectListNumerically("conceptList");
		sortSelectListNumerically("kitList");
		sortSelectListNumerically("thingList");
		
	}
	
	function sortAlphabetically() {
		sortSelectListAlphabetically("authorKeywordList");
		sortSelectListAlphabetically("meshKeywordList");
		sortSelectListAlphabetically("journalList");
		sortSelectListAlphabetically("countryList");
		sortSelectListAlphabetically("universityList");
		sortSelectListAlphabetically("authorList");
		sortSelectListAlphabetically("vendorList");
		sortSelectListAlphabetically("chemicalList");
		sortSelectListAlphabetically("actionList");
		sortSelectListAlphabetically("conceptList");
		sortSelectListAlphabetically("kitList");
		sortSelectListAlphabetically("thingList");		
	}
	
	function createConceptFilter(selectField, conceptType, category, filters ) {
		var items = getSelectedValues(selectField);
		if (items.length > 0) {
			var filter = ElasticSupport.createFilterConcept(items, null, [ category], [conceptType], null)
			filters.push(filter)
		}					
	}
	
	function createFilter(selectField, termField,filters) {
		var keywords = getSelectedValues(selectField);
		if (keywords.length > 0) {
			var filter = ElasticSupport.createFilterField(termField,keywords);
			filters.push(filter)
		}
	}
	
	function produceFilters() {
		var filters = [];
		
		createFilter("authorKeywordList", "keywordMinor.keyword", filters )
		createFilter("meshKeywordList", "MeshHeading.DescriptorName.content.keyword", filters )
		createFilter("journalList", "Article.Journal.Title.keyword", filters )
		createFilter("countryList", "Article.AuthorList.Author.AffiliationInfo.location.country.keyword", filters )
		createFilter("authorList", "authorFullName.keyword", filters )	
		createFilter("chemicalList", "ChemicalList.Chemical.NameOfSubstance.content.keyword", filters )
		createConceptFilter("actionList", "action", "wolfhunt.technology_regex" ,filters)
		createConceptFilter("conceptList", "concept", "wolfhunt.technology_regex" ,filters)
		createConceptFilter("kitList", "kit", "wolfhunt.technology_regex" ,filters)
		createConceptFilter("thingList", "thing", "wolfhunt.technology_regex" ,filters)
		

		var universities = getSelectedValues("universityList");
		if (universities.length > 0) {
			var universityFilter = ElasticSupport.createFilterConcept(null, null, null, ["University"], universities);
			filters.push(universityFilter)
		}
	
		var vendors = getSelectedValues("vendorList");
		if (vendors.length > 0) {
			var filter = ElasticSupport.createFilterConcept(vendors, null, null, ["vendor"], null)
			filters.push(filter)
		}		
		
		return filters;
	}
	
	
	function applyFilters() {
		var filters = produceFilters();
		opener.filterData(filters)
		
		LASLogger.instrEvent('application.literatureDiscovery.filters.applyFilters', {
			filters : Filters
		});
	}
	
	function clearFilters() {
		$("#authorKeywordList").val([]);
		$("#meshKeywordList").val([]);
		$("#journalList").val([]);
		$("#countryList").val([]);
		$("#universityList").val([]);
		$("#authorList").val([]);
		$("#vendorList").val([]);
		$("#chemicalList").val([]);
		$("#actionList").val([]);
		$("#conceptList").val([]);
		$("#kitList").val([]);
		$("#thingList").val([]);
		
		queryFilters = null;
		
		loadFilters({});
		
		LASLogger.instrEvent('application.literatureDiscovery.filters.clearFilters')
	}
	
	function loadFilters(savedOptions) {
		loadAuthorKeywords(savedOptions);
		loadMESHKeywords(savedOptions);
		loadJournals(savedOptions);
		loadCountries(savedOptions);
		loadUniversities(savedOptions);
		loadAuthors(savedOptions);
		loadVendors(savedOptions);
		loadChemicals(savedOptions);
		loadCRISPRActions(savedOptions);
		loadCRISPRConcepts(savedOptions);
		loadCRISPRKits(savedOptions);
		loadCRISPRThings(savedOptions);		
	}
	
	function updateFilters() {
		// warning this assumes that only filter selects are on the page.  if other selects are added, need to add a specific class to identify ours
		var saveOptions = {}
		$('select').each(
			    function(index){  
			        var input = $(this);
			        var selectID = input.attr('id')
			        saveOptions[selectID] = input.val();
			    }
		);
		
		var filters = produceFilters();
		queryFilters = {"bool": {"filter": filters }}
		loadFilters(saveOptions);
		
		LASLogger.instrEvent('application.literatureDiscovery.filters.updateFilters', {
			criteria : queryFilters,
			savedOption: saveOptions
		});
	}
	
	function searchByFilters() {
		var myFilters = produceFilters();
		
		var query= ElasticSupport.createQueryFilter(myFilters, 20, 0);

		LASLogger.instrEvent('application.literatureDiscovery.filters.searchByFilters', {
			criteria : query
		});
		
		$('#searchQuery').val(JSON.stringify(query));
		$('#searchForm').submit();
		
	}
	
	
	function initialize() {
		LASLogger.instrEvent('application.literatureDiscovery.filters');
		
		loadFilters({});
		
		$("#btnApplyfilter").click(applyFilters);
		$("#btnUpdatefilters").click(updateFilters);
		$("#btnClearfilters").click(clearFilters);
		$("#btnSortAlpha").click(sortAlphabetically);
		$("#btnSortFreq").click(sortNumerically);
		$("#btnSearchByFilters").click(searchByFilters);
		
		
	}
	
	function compare(a,b) {
		  if (a.doc_count < b.doc_count)
		    return 1;
		  if (a.doc_count > b.doc_count)
		    return -1;
		  return 0;
		}
	
	function loadKeywords(query, selectFieldID, termFieldName, savedOptions) {
		
		if (queryFilters) {	query.query = queryFilters; 	}  // used for applying filters on ourselves
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success: function(data) {
				var buckets = data.aggregations[termFieldName].buckets;
				buckets.sort(compare);
				$('#'+selectFieldID).find('option').remove();  // if we have previously loaded data, clear that out
				for (var i=0; i< buckets.length; i++) {
					
					var viewStr = buckets[i].key;
					if (selectFieldID === "countryList" && countryTranslation.hasOwnProperty(viewStr)) {
						viewStr = countryTranslation[viewStr]
					}
					
					$('#'+selectFieldID).append($('<option>', {
					    value: buckets[i].key,
					    text: "("+ buckets[i].doc_count +") "+viewStr 
					}));
				}
				if (savedOptions.hasOwnProperty(selectFieldID)) {
					$('#'+selectFieldID).val(savedOptions[selectFieldID]); // set cached selected value
				}
			}	
		});		
	}
	
	
	function loadAuthorKeywords(savedOptions) {
		var query = ElasticSupport.createQueryAggregationField("keywordMinor.keyword", 2000);
		loadKeywords(query,'authorKeywordList','item',savedOptions);
	}
	
	function loadMESHKeywords(savedOptions) {
		var query = ElasticSupport.createQueryAggregationField("MeshHeading.DescriptorName.content.keyword", 2000);
		loadKeywords(query,'meshKeywordList','item',savedOptions);
	}	
	
	function loadJournals(savedOptions) {
		var query = ElasticSupport.createQueryAggregationField("Article.Journal.Title.keyword", 2000);
		loadKeywords(query,'journalList','item',savedOptions);
	}		
	
	function loadCountries(savedOptions) {
		var query = ElasticSupport.createQueryAggregationField("Article.AuthorList.Author.AffiliationInfo.location.country.keyword", 2000);
		loadKeywords(query,'countryList','item',savedOptions);
	}			

	function loadAuthors(savedOptions) {
		var query = ElasticSupport.createQueryAggregationField("authorFullName.keyword", 2000);
		loadKeywords(query,'authorList','item',savedOptions);
	}	
	function loadChemicals(savedOptions) {
		var query = ElasticSupport.createQueryAggregationField("ChemicalList.Chemical.NameOfSubstance.content.keyword", 2000);
		loadKeywords(query,'chemicalList','item',savedOptions);
	}	

	function loadConcepts(query, selectField, savedOptions) {
		if (queryFilters) {	query.query = queryFilters; 	}  // used for applying filters on ourselves
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success: function(data) {
				var buckets = data.aggregations.concepts_root;  
				while (buckets.hasOwnProperty("items")) { buckets = buckets.items;}
				buckets = buckets.item.buckets;
				
				buckets.sort(compare);
				$('#'+selectField).find('option').remove();  // if we have previously loaded data, clear that out
				for (var i=0; i< buckets.length; i++) {
					$('#'+selectField).append($('<option>', {
					    value: buckets[i].key,
					    text: "("+ buckets[i].doc_count +") "+buckets[i].key 
					}));
				}
				if (savedOptions.hasOwnProperty(selectField)) {
					$('#'+selectField).val(savedOptions[selectField]); // set cached selected value
				}
			}	
		});		
	}
	
	
	function loadUniversities(savedOptions) {
		var query = ElasticSupport.createQueryAggregationConceptsField([ {fieldName: "concepts.type.keyword", fieldValue: "University"} ], "concepts.value.keyword", 2000)
		loadConcepts(query,'universityList',savedOptions)
	}	
	
	
	function loadVendors(savedOptions) {
		var query = ElasticSupport.createQueryAggregationConceptsField([ {fieldName: "concepts.type.keyword", fieldValue: "vendor"} ], "concepts.name.keyword", 2000)
		loadConcepts(query,'vendorList',savedOptions)

	}		
	
	function loadCRISPRActions(savedOptions) {
		var query = ElasticSupport.createQueryAggregationConceptsField([ {fieldName: "concepts.type.keyword", fieldValue: "action"}, {fieldName: "concepts.category.keyword", fieldValue: "wolfhunt.technology_regex"} ], "concepts.name.keyword", 2000)
		loadConcepts(query,'actionList',savedOptions)
	}

		
	function loadCRISPRConcepts(savedOptions) {
		var query = ElasticSupport.createQueryAggregationConceptsField([ {fieldName: "concepts.type.keyword", fieldValue: "concept"}, {fieldName: "concepts.category.keyword", fieldValue: "wolfhunt.technology_regex"} ], "concepts.name.keyword", 2000)
		loadConcepts(query,'conceptList',savedOptions)
	}		
	
	function loadCRISPRKits(savedOptions) {
		var query = ElasticSupport.createQueryAggregationConceptsField([ {fieldName: "concepts.type.keyword", fieldValue: "kit"}, {fieldName: "concepts.category.keyword", fieldValue: "wolfhunt.technology_regex"} ], "concepts.name.keyword", 2000)
		loadConcepts(query,'kitList',savedOptions)
	}			
	
	function loadCRISPRThings(savedOptions) {
		var query = ElasticSupport.createQueryAggregationConceptsField([ {fieldName: "concepts.type.keyword", fieldValue: "thing"}, {fieldName: "concepts.category.keyword", fieldValue: "wolfhunt.technology_regex"} ], "concepts.name.keyword", 2000)
		loadConcepts(query,'thingList',savedOptions)
	}			
	
	return {
		initialize : initialize,
	};
}());


$(document).ready(function() {
	CitationFilter.initialize();
})

