/**
 * 
 */

"use strict"
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.model == 'undefined') { openke.model = {}  }
openke.model.Analytics = (function () {
	
	var openkeLocalStore  = window.localStorage; // convenience variable such that we can switch between setting  
	
	function getValueFromStore(fieldName) {
		var fullName = openke.global.Common.getDomain()+'_'+fieldName
		if (openkeLocalStore[fullName] != null && openkeLocalStore[fullName] != "null") {	return  openkeLocalStore[fullName]; 	}
		else {	return null;}				
	}

	function setValueToStore(fieldName, newValue) {
		var fullName = openke.global.Common.getDomain()+'_'+fieldName;
		openkeLocalStore[fullName] = newValue
	}
	
	function removeValueFromStore(fieldName) {
		var fullName = openke.global.Common.getDomain()+'_'+fieldName;
		openkeLocalStore.removeItem(fullName);
	}
		
	/**
	 * 
	 * filterState - json object that can be be used to restore the analytic filter page with the appropriate select boxes(aka cards) and choices)
	 * elasticSearchFilterOptions - json query that can be appended to other queries to limit results based upon the selected filter
	 */
	function setAnalyticFilterData(filterState, elasticSearchFilterOptions) {
		setValueToStore("analyticFilterState",JSON.stringify(filterState));	
		setValueToStore("analyticFilterOptions",JSON.stringify(elasticSearchFilterOptions));
	}
	function clearAnalyticFilterData(){
		removeValueFromStore("analyticFilterState");
		removeValueFromStore("analyticFilterOptions");
	}
	function getAnalyticFilterState() { return JSON.parse(getValueFromStore("analyticFilterState"));}
	
	function getAnalyticFilterOptions() { 
		var result = JSON.parse(getValueFromStore("analyticFilterOptions"));
		if (result == null) { result = []; }
		return result;
	}

	
	
	// page-level variables hidden into our scope	
	var spacyMapping = { "PERSON" : "People",
            "GPE"    : "Places",
            "NORP"   : "Groups",
            "ORG"    : "Organizations",
            "EVENT"  : "Events",
             "FAC"    : "Facilities",
            "LOC"    : "Physical Locations",
            "PRODUCT"     : "Products",
            "WORK_OF_ART" : "Works of Art",
            "LAW"         : "Laws",
            "LANGUAGE"    : "Languages",
            "MONEY"       : "Money"};
	

	
	/**
	 * creates the base query used for many analytic queries. 
	 * Applies any filters set and the current date range passed
	 */
	function createBaseAnalyticQuery(startDateMS, endDateMS) {
		var standardQuery= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startDateMS, "lte": endDateMS  }  } } ]      }
				  },
				  "size": 0
		       }
		var filters =  getAnalyticFilterOptions();
		filters.forEach(function(element) {
			standardQuery.query.bool.filter.push(element);
		});	
		
		return standardQuery;
	}
	
	function createBaseAnalyticQueryNoFilter(startDateMS, endDateMS) {
		var standardQuery= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startDateMS, "lte": endDateMS  }  } } ]      }
				  },
				  "size": 0
		       }
		
		return standardQuery;
	}
		
	function executeElasticQuery(query, callback) {
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success:  function(data) {
				if (data.hasOwnProperty("status") && data.status != 200) { 
					bootbox.alert("Query Failure: view console for details");
					LASLogger.log(LASLogger.LEVEL_ERROR,"Failed Query: "+JSON.stringify(query));
					LASLogger.log(LASLogger.LEVEL_ERROR,"Response: "+JSON.stringify(data));
					return
				}
				callback(data)
			}	
		});
	}
	
	function loadDates(loadDatesCallback) {
		var query = 
			{
			  "query": {  "bool": { "filter": [ { "range": { "published_date.date": { "gte": "1990-01-01" }}} ] }  },
			  "size": 0,
			  "aggs": {
			    "minCrawlDate" : { "min" : { "field" : "crawled_dt" }},
			    "maxCrawlDate" : { "max" : { "field" : "crawled_dt" }},
			    "minPublishedDate" : { "min" : { "field" : "published_date.date" }},
			    "maxPublishedDate" : { "max" : { "field" : "published_date.date" }}
			  }
			}
		executeElasticQuery(query,loadDatesCallback)
	}
	
	function mapSpacyEntityName(spacyCode) {
		return spacyMapping.hasOwnProperty(spacyCode) ? spacyMapping[spacyCode] : spacyCode; 
	}
	
	//const charRegExpValidLabel = /^\w+(?!\:\(\")(?:\s|\w|\.)+$/;
	const charRegExpValidLabel = /^(?:[A-Z0-9]|the)+(?!\:\(\")(?:\s|\w|\.)+$/;
	const allUpperCase = /[A-Z]+$/;
	const allNumbers = /^[0-9]*$/;
	const allFloat = /^[0-9]*\.[0-9]*$/;
	const hexString = /^[0-9A-F]+$/;
	const noObviousBad = /^(?!.*em|.*px).*/;
	
	function isValidLabel(label) {
		var isValid = false;
		var labelToTest = label.trim();
		
		//general test
		if (labelToTest.length > 2 && charRegExpValidLabel.test(labelToTest)){
			//no obvious bad
			if (noObviousBad.test(labelToTest)) {
				//not all numbers or hex
				if (!allNumbers.test(labelToTest) && !hexString.test(labelToTest) && !allFloat.test(labelToTest)){
					isValid = true;
				}
			}
		}

		//if it's just 2 chars, they have to be upper
		if (labelToTest.length === 2 && allUpperCase.test(labelToTest)) {
			isValid = true;
		}
		
		return isValid;
	}
	
	function createSearchFieldQuery(fieldName, fieldValue, startDateMS, endDateMS) {
		var query = createBaseAnalyticQuery(startDateMS, endDateMS)
		query.size = 20
		query.from = 0
		var termObject = {}
		termObject[fieldName] = fieldValue
		query.query.bool.filter.push({"term" : termObject})
		return query;
	}

	function createSearchWithFiltersQuery(startDateMS, endDateMS, filters) {
		var query = createBaseAnalyticQuery(startDateMS, endDateMS)
		query.size = 20
		query.from = 0
		for (var i=0; i<filters.length; i++) {
			query.query.bool.filter.push(filters[i])
		}		
		return query
	}
	
	function createSearchWithFiltersQueryClean(startDateMS, endDateMS, filters) {
		var query = createBaseAnalyticQuery(startDateMS, endDateMS)
		query.size = 20
		query.from = 0
		query.query.bool.filter.push(filters);		
		return query
	}
	
	
	function createSearchNestedFieldQuery(nestedPath, fieldName, fieldValue, startDateMS, endDateMS) {
		var query = createBaseAnalyticQuery(startDateMS, endDateMS)
		query.size = 20
		query.from = 0
		var termObject = {}
		termObject[fieldName] = fieldValue
		var filterObject = 	{
				"nested": {
					"path": nestedPath,
					"score_mode": "max",
					"query": {
						"bool": {
							"must": [{"term": termObject}]
						}
					}
				}
			}
		query.query.bool.filter.push(filterObject)
		return query
	}
	
	var privateMembers = {
	
	}
	
	return {
		setAnalyticFilterData    : setAnalyticFilterData,
		clearAnalyticFilterData  : clearAnalyticFilterData,
		getAnalyticFilterState   : getAnalyticFilterState,
		getAnalyticFilterOptions : getAnalyticFilterOptions,
		
		createBaseAnalyticQuery: createBaseAnalyticQuery,
		executeElasticQuery : executeElasticQuery,
		loadDates : loadDates,
		mapSpacyEntityName : mapSpacyEntityName,
		isValidLabel: isValidLabel,
		createSearchFieldQuery : createSearchFieldQuery,
		createSearchWithFiltersQuery : createSearchWithFiltersQuery,
		createSearchWithFiltersQueryClean : createSearchWithFiltersQueryClean,
		createSearchNestedFieldQuery : createSearchNestedFieldQuery
	};
}());
