/**
 * 
 * Create date - 20171101
 * Description:
 *
 * Usage:
 *
 *
 */

var AnalyticFrequencies = (function () {
	"use strict";

	var yAxisField   = null;

	var plotID = 0;  // used to produce a unique ID for each plot

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

	const charRegExpValidLabel = /^\w+(?!\:\(\")(?:\s|\w|\.)+$/;  // validates whether or not to use a particular record

	var myDateSlider;

	function getSizeEntered(fieldID) {
		var size = Number.parseInt($('#'+fieldID).val());
		if (isNaN(size)) {
			size = 20;
		}
		return size;
	}

	function compareByKey(a,b) {
		return a.key.localeCompare(b.key);
	}


	function getFilterQuery(fieldName, fieldValue) {
		var filterQuery = null;
		var cleanedField = fieldName.substring(fieldName.indexOf(":")+1);
		if(fieldName.indexOf("entity") > -1){
			filterQuery = ElasticSupport.createMustEntity(cleanedField, fieldValue);
		}else{
			filterQuery = ElasticSupport.createMustConcept(cleanedField, fieldValue);
		}

		return filterQuery;
	}


	function searchByFilters() {
		var startTimeMS = myDateSlider.getDateRange().startValue;
		var endTimeMS   = myDateSlider.getDateRange().endValue;
		var query= {
				  "query": {
				    "range": { "published_date.date": { "gte": startTimeMS, "lte": endTimeMS  }  }
				  },
				  "size":20, "from":0
		       }

		openke.model.Analytics.getAnalyticFilterOptions().forEach(function(element) {
			query.query.bool.filter.push(element);
		})

		LASLogger.instrEvent('application.analytic.heatmap.searchByFilters', {
			criteria : query
		});

		$('#searchQuery').val(JSON.stringify(query));
		$('#analyzeSearchForm').submit();
	}



	function createDateHistogramChart(data) {
		
		// assign bucket to the start of the actual.  the top level there forms the x-axis
		var bucket = data.aggregations.items_per_day.buckets; 
		
		var labels = [];
		var appearanceDocs  = [];
		var otherDocs       = [];

		for (var i=0; i < bucket.length; i++) {
			var item = bucket[i].key_as_string;
			item = item.substring(0,10)
			labels.push(item);
			appearanceDocs.push(bucket[i].items.items.doc_count)
		}
		
		var yActualFieldName = yAxisField.substring(yAxisField.indexOf(":")+1);		
		if (yAxisField.indexOf("entity") > -1){ yActualFieldName = spacyMapping[yActualFieldName];}
		
		var filterHTML = $("#hdrFilterContents").html()
		if (filterHTML != "") { filterHTML = "<br>(filters applied)" }
		
		var layout = {
			title: 'Count of '+yActualFieldName + ' by Date' +filterHTML,
			xaxis: { title: "Date"  },
			yaxis: { title: "Count"  },
			barmode: 'stack',
			margin: {
			    l: 200,
			    r: 50,
			    b: 100,
			    t: 50,
			    pad: 4
			  }
		};

		var computedHeight = labels.length *10 +200;
		
		var data = [ {
			type: 'bar',
			x: labels,
			y: appearanceDocs,
		    colorscale: 'Bluered' //'Blues',
	    }];

		plotID = plotID + 1;
		$("#plotContainer").append($("<div id='plot_"+plotID+"' style='width: 500px; height: 500px; display: inline-block;' />"))
		
		Plotly.newPlot("plot_"+plotID, data,layout);
		
		/*document.getElementById("plot_"+plotID).on('plotly_click', function(data){
			
			var xValue = data.points[0].x
			var yValue = data.points[0].y 
			
			var startDateMS = myDateSlider.getDateRange().startValue;
			var endDateMS   = myDateSlider.getDateRange().endValue;
			
			var xyFilter = [ getFilterQuery(yAxisField, yValue) ];
			var query = openke.model.Analytics.searchWithFilters(startDateMS,  endDateMS, xyFilter)
			
			var query= {
					  "query": {
					    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startDateMS, "lte": endDateMS  }  } }, getFilterQuery(xAxisField, xValue), getFilterQuery(yAxisField, yValue)  ]      }
					  },
					  "size": 20, "from": 0
			       } 
						
			LASLogger.instrEvent('application.analytic.createDateHistogramChart.searchCell', {
				criteria : query
			});
			
			$('#searchQuery').val(JSON.stringify(query));
			$('#searchForm').submit();
			
			
		});*/
		
	}

function createHorizontalBarChart(data) {
	
	// assign bucket to the start of the actual.  the top level there forms the x-axis
	var bucket = data.aggregations.items.items.items.buckets; 
	
	var labels = [];
	var freqs  = [];

	for (var i=0; i < bucket.length; i++) {
		var item = bucket[i].key;
		if (!charRegExpValidLabel.test(item)) {
			continue;
		}
		labels.push(item);
		freqs.push(bucket[i].doc_count)
	}
	
	var yActualFieldName = yAxisField.substring(yAxisField.indexOf(":")+1);		
	if (yAxisField.indexOf("entity") > -1){ yActualFieldName = spacyMapping[yActualFieldName];}
	
	var filterHTML = $("#hdrFilterContents").html()
	if (filterHTML != "") { filterHTML = "<br>(filters applied)" }
	
	var layout = {
		title: yActualFieldName + ' Frequency' +filterHTML,
		xaxis: { title: "Count"  },
		yaxis: { title: yActualFieldName, dtick: 1  },
		margin: {
		    l: 200,
		    r: 50,
		    b: 100,
		    t: 50,
		    pad: 4
		  }
	};

	var computedHeight = labels.length *10 +200;
	
	var data = [ {
		type: 'bar',
		orientation: 'h',
		x: freqs,
		y: labels,
	    colorscale: 'Bluered' //'Blues',
    }];

	plotID = plotID + 1;
	$("#plotContainer").append($("<div id='plot_"+plotID+"' style='width: 500px; height: "+computedHeight+"px; display: inline-block;' />"))
	
	Plotly.newPlot("plot_"+plotID, data,layout);
	
	var dragLayer = $('#plot_'+plotID).find('.nsewdrag')[0]
	document.getElementById("plot_"+plotID).on('plotly_hover', function(data){
	  dragLayer.style.cursor = 'pointer'
	});
	document.getElementById("plot_"+plotID).on('plotly_unhover', function(data){
	  dragLayer.style.cursor = ''
	});
	
	document.getElementById("plot_"+plotID).on('plotly_click', function(data){
		
		var xValue = data.points[0].x
		var yValue = data.points[0].y 
		
		var startDateMS = myDateSlider.getDateRange().startValue;
		var endDateMS   = myDateSlider.getDateRange().endValue;
		
		var xyFilter = [ getFilterQuery(yAxisField, yValue) ];
		openke.view.Analyze.searchWithFilters(startDateMS,  endDateMS, xyFilter)
		
		
	});
}



	function createDateHistogramQuery(yField, startTimeMS,endTimeMS, appliedFilters) {
		var query= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startTimeMS, "lte": endTimeMS  }  } } ]      }
				  },
				  "size": 0
		       }

		appliedFilters.forEach(function(element) {
			query.query.bool.filter.push(element);
		});

		var aggHeatMapQuery =  ElasticSupport.createAggregationDateFrequencyClause(yField);

		query.aggs = aggHeatMapQuery;
		LASLogger.log(LASLogger.LEVEL_INFO,"createDateHistogramQuery.CreateQuery: "+JSON.stringify(query));
		return query;
    }

	function createBarChartQuery(yField, ySize, startTimeMS,endTimeMS, appliedFilters) {
		var query= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startTimeMS, "lte": endTimeMS  }  } } ]      }
				  },
				  "size": 0
		       }

		appliedFilters.forEach(function(element) {
			query.query.bool.filter.push(element);
		});

		var aggQuery =  ElasticSupport.createAggregationFrequencyClause(yField,ySize);

		query.aggs = aggQuery;
		LASLogger.log(LASLogger.LEVEL_INFO,"createBarChartQuery.CreateQuery: "+JSON.stringify(query));
		return query;
    }


	// also uses  yAxisField.  will load sizes from current size box.  those default to 20 if not completed correctly.  query filters are pulled from the session
	function loadDataForVisualization(displayFormat,startTimeMS,endTimeMS) {
		if (!yAxisField) {return; } //haven't defined those fields, can't plot
		var yAxisSize = getSizeEntered('yAxisSize');


		var dataQuery;
		if (displayFormat == "barchart") {
			dataQuery= createBarChartQuery(yAxisField, yAxisSize, startTimeMS, endTimeMS, openke.model.Analytics.getAnalyticFilterOptions());
		}
		else if (displayFormat == "histogram") {
			dataQuery= createDateHistogramQuery(yAxisField, startTimeMS, endTimeMS, openke.model.Analytics.getAnalyticFilterOptions());
		}
		else {
			bootbox.alert("Display Format: Invalid Choice");
			return;
		}

		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(dataQuery),
			dataType : "JSON",
			success: function(data) {
				if (displayFormat == "barchart") {
					createHorizontalBarChart(data);
				}
				else if (displayFormat == "histogram") {
					createDateHistogramChart(data);
				}

			}
		});

	}


	function viewAction() {
		var localYAxisField = $("#yAxisField").val();

		if (localYAxisField == "") { bootbox.alert("You must select an item to visualization"); 	return false;	}

		// set the "global" variables for y.
		yAxisField = localYAxisField;

		var startDateMS = myDateSlider.getDateRange().startValue;
		var endDateMS   = myDateSlider.getDateRange().endValue;
		var format      = $("input[name='displayFormat']:checked"). val();
		LASLogger.instrEvent('application.analytic.frequencies.view', {
			displayFormat: format,
			yAxis: yAxisField,
			startDate: startDateMS,
			endDate: endDateMS
		});


		loadDataForVisualization(format,startDateMS,endDateMS);
	}


	function loadDropDowns(){
		var query = {
				  "size": 0,
				  "aggs": {
				    "spacy_root": {
				      "nested": {"path": "spacy.entities"},
					     "aggs": {"item": {"terms": {
					            "field": "spacy.entities.type.keyword",
					            "size": 2000
					          }}}
				    },
				    "concepts_root": {
				        "nested": {"path": "concepts"},
				                "aggs": {"item": {"terms": {
				                    "field": "concepts.fullName.keyword",
				                    "size": 2000
				                }}}
				    }
				  }
				}
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success: function(data) {
				var buckets;
				var spacybuckets = data.aggregations.spacy_root;
				var conceptbuckets = data.aggregations.concepts_root;
				while (spacybuckets.hasOwnProperty("items")) { spacybuckets = spacybuckets.items;}
				while (conceptbuckets.hasOwnProperty("items")) { conceptbuckets = conceptbuckets.items;}
				spacybuckets = spacybuckets.item.buckets;
				conceptbuckets = conceptbuckets.item.buckets;
				spacybuckets.sort(compareByKey);
				conceptbuckets.sort(compareByKey);
				buildDropdown(spacybuckets, conceptbuckets, 'Select an option');
			}
		});
	}

	function buildDropdown (spacy, concept, emptyMessage){
    	var idCounter = 0;
        // Remove current options
        $("#yAxisField").html('');

        // Add the empty option with the empty message
        $("#yAxisField").append('<option value="">' + emptyMessage + '</option>');

        // Check result isn't empty
        if(spacy != '' && concept != ''){
            // Loop through each of the results and append the option to the dropdown
            $.each(spacy, function(k, v) {
        		if(v.doc_count > 0){
        			var spacyText = "Entity: "+ spacyMapping[v.key]
        			$("#yAxisField").append('<option value="entity:' + v.key + '"> ' + spacyText + '(' + v.doc_count + ')</option>');
            		idCounter++;
        		}
            });
            $.each(concept, function(k, v) {
        		if(v.doc_count > 0){
        			var pestleText = v.key
        			$("#yAxisField").append('<option value="concept:'+v.key+'"> ' + pestleText + '(' + v.doc_count + ')</option>');
            		idCounter++;
        		}
            });
        }
    }


	function initialize() {
		LASLogger.instrEvent('application.analytic.heatmap.initialize')

		$('#btView').click(function() {viewAction(); return false;})
		$('#btClearAllCharts').click(function() {
			LASLogger.instrEvent('application.analytic.frequencies.clearCharts');
			$("#plotContainer").empty();
		})

		window.filterData = function(filterClauses) {
			//NOTE: updating of existing graphs is not performed on this page
			openke.view.Analyze.displayFilters();
		}

		openke.model.Analytics.loadDates(intializeAfterDateLoad)
	}


	function intializeAfterDateLoad(dates) {

		// Only do one  update per .2 seconds
		var debounceSlideUpdate = openke.global.Common.debounce(function(data) {
			//loadHeatMapData(data.startValue,data.endValue) Note: we are not updating this graphs - will add new ones to compare
		},200);

	    var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
		myDateSlider = new DateSlider('timeSlider', debounceSlideUpdate, dates.aggregations.minPublishedDate.value_as_string,dates.aggregations.maxPublishedDate.value_as_string, step, "YYYYMMDD", true, true, openke.view.Analyze.defaultSliderPlayOptions )

		loadDropDowns();
	}


	return {
		initialize : initialize,
	};
}());


$(document).ready(function() {
	AnalyticFrequencies.initialize();
})
