"use strict";

LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


/**
 * Produces a pie chart using plot.ly for displaying the percentage breakdown of concepts.
 * 
 * divID - ID for the component that will get the chart (don't include a leading #)
 * fileStorageArea - which filestorage area to search: normal/sandbox
 * prefix - if making a second call, use this to drill down
 * filter - use to limit to a specific set of documents.  This should either be an object or an array of objects
 * shiftCallback - function to call if clicked with the shift key down
 */
function ConceptChart(divID, fileStorageArea, prefix = "", startDate=null, endDate=null, shiftCallBack) {
	this.divID           = divID;
	this.fileStorageArea = fileStorageArea;
	this.prefix          = prefix;
	this.shiftCallBack   = shiftCallBack;
	this.filters         = [];
	this.startDate       = startDate;
	this.endDate         = endDate;
	
	var that = this;
	
	populateConceptCountChart();
	
	// callback for populate to create the pie chart on screen
	function createChart(data) {
		if (data.hasOwnProperty("error")) {
			$("#"+ that.divID).html("No concepts in this domain")
			return
		}
	
		var counts = {};
		
		var prefixLength = that.prefix.length;
		if (prefixLength > 0 ) { prefixLength = prefixLength +1; }
		
		// Summarize the results by the next level down from the curren prefix.
		var bucketArray = data.aggregations.concepts_root.concepts.attrs.buckets;
		for (var i=0; i < bucketArray.length; i++) {
			var count = bucketArray[i].doc_count;
			var label = bucketArray[i].key;
			
			var lastIndex = label.indexOf(".",prefixLength);
			if (lastIndex == -1) { lastIndex = label.length}
			
			var myCategory = label.substring(prefixLength,lastIndex)
			if(counts.hasOwnProperty(myCategory)) {
				counts[myCategory] = counts[myCategory] + count;
			}
			else {
				counts[myCategory] = count;
			}
			
		}
		
		var pieData = [{
			values : [],
			labels : [],
			type: 'pie'
		}];

		for (var key in counts) {
			if (counts.hasOwnProperty(key) && key != "") {
				pieData[0].labels.push(key);
				pieData[0].values.push(counts[key])
			}
		}
		if (pieData[0].labels.length == 0) {
			if (prefixLength > 0 ) {
				var lastPeriod = that.prefix.lastIndexOf(".");
				if (lastPeriod > 0) {
					that.prefix = that.prefix.substring(0,lastPeriod);
				}
				
				var box = bootbox.alert({title:"Concepts", message: "Categories can not be broken down any further..."});
				setTimeout(function() {
				    box.modal('hide');
				}, 2000);
			}
			else {
				$("#" + that.divID).text("No concepts in this domain")
			}
			return;
		}
			
		var title = "Concept Breakdown" 
		
		if (prefixLength >0) {
			title = title +": "+ that.prefix
		}
		title = title + computeDateFilterDisplay();
			
		var layout = {
			title: title,
			height: 400,
			width: 600
		};
		
		
		Plotly.newPlot(that.divID, pieData, layout);
			
		document.getElementById(that.divID).on('plotly_click', function(data){
			var category = data.points[0].label;
			
			if (prefixLength > 0) {
				that.prefix = that.prefix + "." + category
			}
			else {
				that.prefix = category;
			}
			if (shiftCallBack != null && data.event.shiftKey) {
				var searchObject = {
				    "serchText" : "",
				    "scope"     : "text",
				    "sortField" : "relevance",
				    "sortOrder" : "desc",
				    "searchType": "keyword",
				    "filterField" : "concepts.fullName",
				    "filterValue" : that.prefix,
				    "filters"     : that.filters,
				    "startDate"   : that.startDate,
				    "endDate"     :	that.endDate
				}
						
				LASLogger.instrEvent('application.conceptChart.shiftCallBack', searchObject);
				shiftCallBack(searchObject)
			}
			else {
				LASLogger.instrEvent('application.conceptChart.drillDown', {prefix: that.prefix, fileStorageArea: that.fileStorageArea, filter: that.filter});
				populateConceptCountChart();
			}
		});
	}

	
	function executeElasticQuery(query, callback) {
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/search/"+ that.fileStorageArea,
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success: function(data) { createChart(data); }	
		});
	}	
	
	function populateConceptCountChart() {
		var query = {
			  "size": 0,
			  "query": {"bool": { "filter": [ ] }},
			  "aggs": { "concepts_root": {
			              "nested": {  "path": "concepts" },
			              "aggs": {  "concepts": { "filter": { "prefix": {"concepts.fullName.keyword": that.prefix}   },
			                         "aggs": {"attrs": { "terms": { "field": "concepts.fullName.keyword",  "size": 1000  } } }
			              }}    
			            }
		}	}
		
		if (that.filters != "") {
			if (isArray(that.filters)) {
				for (var i=0; i < that.filters.length; i++) {
					query.query.bool.filter.push(that.filters[i]);
				}
			}
			else {
				query.query.bool.filter.push(that.filters);
			}
		}
		
		if (that.startDate != null) {
			query.query.bool.filter.push({ "range": { "crawled_dt": { "gte": Number(that.startDate) } } })
		}
		if (that.endDate != null) {
			query.query.bool.filter.push({ "range": { "crawled_dt": { "lte": Number(that.endDate) } } })
		}
		
		executeElasticQuery(query,createChart)
	}
	
	// used to put a descriptive value into a title when date filters are set
	function computeDateFilterDisplay() {
		if      (that.startDate == null && that.endDate == null) { return ""; }
		else if (that.startDate != null && that.endDate == null) { return "<br>after "  + (new Date(that.startDate)).toISOString(); }
		else if (that.startDate == null && that.endDate != null) { return "<br>before " + (new Date(that.endDate)).toISOString(); }
		else {			
			return "<br>"+ (new Date(that.startDate)).toISOString() + " to " + (new Date(that.endDate)).toISOString();
		}
	}
	
	// date is represented in milliseconds.. can't use prototype as we need to call populate
	this.setStartDate = function(newStartDate=null) {
		this.startDate = newStartDate;
		populateConceptCountChart()
	}   
	
	this.setEndDate = function(newEndDate=null) {
		this.endDate = newEndDate;
		populateConceptCountChart()
	}   
	
	this.setFilters = function(newFilters=[]) {
		this.filters = newFilters;
		populateConceptCountChart()
	}  	
	
	this.resetToTop = function() {
		this.prefix = "";
		this.endDate = null;
		this.startDate = null;
		populateConceptCountChart();
	}
	
	this.startAtTop = function() {
		this.prefix = "";
		populateConceptCountChart();
	}
}


