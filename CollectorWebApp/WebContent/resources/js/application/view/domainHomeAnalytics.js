/**
 * 
 */

"use strict"
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

$(document).ready(function() {
	openke.view.HomeAnalytics.initialize();
});

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.HomeAnalytics = (function () {
	
	// page-level variables hidden into our scope	
	var myDateSlider
	
	function initialize() {
		LASLogger.instrEvent('application.analytics');
		
		window.page = "analyze"; // Page name to be stored in window object so that LasHeader.js could access it for logging
	
		window.filterData = function(filterClauses) {
			openke.view.Analyze.displayFilters();
			updatePageDataAfterDateChange();
		}
		
		var tableOptions = {
				"dom": 'tip',
				"pageLength": 5,
				"columns" : [ {
					"data" : "key",
					"width" : "75%"
				}, {
					"data" : "doc_count",
					"width" : "25%",
				} ],
				"order" : [ [ 1, "desc" ] ]
			}
		
		$('#topDomainsTable').DataTable(tableOptions);
		$('#topEntitiesTable').DataTable(tableOptions);
		$('#topLocationsTable').DataTable(tableOptions);
		
		// Only do one  update per .25 seconds
		var debounceSlideUpdate = openke.global.Common.debounce(function(data) {
			updatePageDataAfterDateChange();
		},250);
		
		
	    var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
		var playOptions = {
				windowSize : 120,
				windowSizeUnit: "days",
	            stepForwardSize: 60,
	            stepForwardUnit: "days",
	            timeDelay:    5000, 
	            allowPlay: false
		}
		myDateSlider = new DateSlider('timeSlider', debounceSlideUpdate, "20091001T000000Z", "20181231T235959Z", step, "YYYYMMDD", true, true, playOptions )
		
		
		// event handling
		$("#btAnalyticsFilter").click(openke.view.Analyze.openFilterWindow)
		$('#entityOptions').change(loadEntityTableOnChange);
		openke.model.Analytics.loadDates(loadDatesCallback)
	}
	
	function loadDatesCallback(data) {
		$('#earliestCrawlDate').text(data.aggregations.minCrawlDate.value_as_string.substring(0,10))
		$('#earliestPublishedDate').text(data.aggregations.minPublishedDate.value_as_string.substring(0,10))
		$('#latestCrawlDate').text(data.aggregations.maxCrawlDate.value_as_string.substring(0,10))
	    $('#latestPublishedDate').text(data.aggregations.maxPublishedDate.value_as_string.substring(0,10))

	    // this will result in updatePageDataAfterDateChange() being called
	    myDateSlider.changeRangeAndStep(data.aggregations.minPublishedDate.value_as_string,data.aggregations.maxPublishedDate.value_as_string)
	   
	}
	
	function loadEntityTableOnChange() {
		var entity = $('#entityOptions').val()
		var entityQuery= openke.model.Analytics.createBaseAnalyticQuery(myDateSlider.getDateRange().startValue, myDateSlider.getDateRange().endValue);
		if (entity == "") {
			entityQuery.aggs = {  "spacy_root": { "nested": {"path": "spacy.entities"},    "aggs": {"item": {"terms": {  "field": "spacy.entities.type.keyword", "size": 100  }}} }	}			
		}
		else {
			entityQuery.aggs = {
			    "items": {
			        "nested": {"path": "spacy.entities" },
			        "aggs": {
			          "items": {
			            "filter": { "term": { "spacy.entities.type.keyword": entity } },
			            "aggs": {
			              "items": {
			                "terms": { "field": "spacy.entities.text.keyword", "size": 500  }
			              }
			            }
			          }
			        }
			      }}
		}
		openke.model.Analytics.executeElasticQuery(entityQuery,populateEntityTable)
	}
	
	//we want to use the dates when loading the rest of the data, so get those first....
	function updatePageDataAfterDateChange() {
		var standardQuery= openke.model.Analytics.createBaseAnalyticQuery(myDateSlider.getDateRange().startValue, myDateSlider.getDateRange().endValue);
		
		LASLogger.instrEvent('application.analytic.home.datesUpdated', {
			startDate: myDateSlider.getDateRange().startValue,
			endDate: myDateSlider.getDateRange().endValue
		});		
				
		var crawlDateAggregationQuery = JSON.parse(JSON.stringify(standardQuery));
		crawlDateAggregationQuery.aggs = { "items_per_day" : { "date_histogram" : {  "field" : "crawled_dt",  "interval" : "1d"  }  }  }
		openke.model.Analytics.executeElasticQuery(crawlDateAggregationQuery,createDateCrawledHistogramChart)
		
		var textLengthAggregationQuery = JSON.parse(JSON.stringify(standardQuery));
		textLengthAggregationQuery.query.bool.filter.push({"range" : { "text_length" : { "gte": 0, "lte": 500000}}})
		textLengthAggregationQuery.aggs = { "textLength" : { "histogram" : { "field" : "text_length","interval" : 500, "extended_bounds" : { "min" : 0, "max" : 500000  } }}}
		openke.model.Analytics.executeElasticQuery(textLengthAggregationQuery,createTextLengthHistogramChart)
		
		var topDomainsAggregationQuery = JSON.parse(JSON.stringify(standardQuery));
		topDomainsAggregationQuery.aggs = { "top_domains" : {"terms" : { "field" : "domain.keyword", "size":500 }   } }
		openke.model.Analytics.executeElasticQuery(topDomainsAggregationQuery,populateDomainTable)
		
		var topLocationsAggregationQuery = JSON.parse(JSON.stringify(standardQuery));
		topLocationsAggregationQuery.aggs = {"location": { "nested": {"path": "geotag"}, "aggs": {"item": {"terms": {"field": "geotag.geoData.preferredName.keyword","size": 500   }}}}}
		openke.model.Analytics.executeElasticQuery(topLocationsAggregationQuery,populateLocationTable)
		
		// initial Entity Table setup
		var entityQuery = JSON.parse(JSON.stringify(standardQuery));
		entityQuery.aggs = {  "spacy_root": { "nested": {"path": "spacy.entities"},    "aggs": {"item": {"terms": {  "field": "spacy.entities.type.keyword", "size": 100  }}} }	}
		openke.model.Analytics.executeElasticQuery(entityQuery,populateEntityTable)

	}
	

	

	
	/*
	function navigateTo(location) {
		var completeLocation = openke.global.Common.getPageURLPrefix() +"/" +location
		LASLogger.instrEvent('domain.home.link', {
			link : completeLocation
		});
		
	     window.location = completeLocation;
	    
	}
	*/
	
	function createDateCrawledHistogramChart(data) {
		
		// assign bucket to the start of the actual.  the top level there forms the x-axis
		var bucket = data.aggregations.items_per_day.buckets; 
		
		var labels = [];
		var docCounts  = [];

		for (var i=0; i < bucket.length; i++) {
			var item = bucket[i].key_as_string.substring(0,10)
			labels.push(item)
			docCounts.push(bucket[i].doc_count)
		}
				
		var filterHTML = $("#hdrFilterContents").html()
		if (filterHTML != "") { filterHTML = "<br>(filters applied)" }
		
		var layout = {
			title: 'Retrieved Pages by Date' +filterHTML,
			xaxis: { title: "Date"  },
			yaxis: { title: "Count"  },
			barmode: 'stack',
			margin: {
			    l: 200,
			    r: 50,
			    b: 60,
			    t: 50,
			    pad: 4
			  }
		};
		
		var data = [ {
			type: 'bar',
			x: labels,
			y: docCounts,
		    colorscale: 'Bluered' //'Blues',
	    }];
		
		Plotly.newPlot("retrievedPagesChart", data,layout);
		
		document.getElementById("retrievedPagesChart").on('plotly_click', function(data){
			//Search by date from bucket clicked on cell
			var xValue = data.points[0].x // date in format of 2018-12-04
			var startDateTime = xValue+"T00:00:00";
			var endDateTime   = xValue+"T23:59:59";
			var filters =  [ {"range": { "crawled_dt": { "gte": moment.utc(startDateTime).valueOf(), "lte": moment.utc(endDateTime).valueOf()  }  } }]
			openke.view.Analyze.searchWithFilters(myDateSlider.getDateRange().startValue,  myDateSlider.getDateRange().endValue, filters)	
		});
		
	}
	
	function createTextLengthHistogramChart(data) {
		
		// assign bucket to the start of the actual.  the top level there forms the x-axis
		var bucket = data.aggregations.textLength.buckets; 
		
		var labels = [];
		var docCounts  = [];

		for (var i=0; i < bucket.length; i++) {
			labels.push(bucket[i].key)
			docCounts.push(bucket[i].doc_count)
		}
				
		// TODO: change logic to use filterOptions length from LASHeader rather than HTML
		var filterHTML = $("#hdrFilterContents").html()
		if (filterHTML != "") { filterHTML = "<br>(filters applied)" }
		
		var layout = {
			title: 'Text Length Distribution' +filterHTML,
			xaxis: { title: "Page Length"  },
			yaxis: { title: "Count"  },
			barmode: 'stack',
			margin: {
			    l: 200,
			    r: 50,
			    b: 60,
			    t: 50,
			    pad: 4
			  }
		};
		
		var data = [ {
			type: 'bar',
			x: labels,
			y: docCounts,
		    colorscale: 'Bluered' //'Blues',
	    }];
		
		Plotly.newPlot("textLengthChart", data,layout);
		
		document.getElementById("textLengthChart").on('plotly_click', function(data){
			//Search by page length from bucket clicked on cell
			var xValue = data.points[0].x
			var filters =  [ {"range":{"text_length":{"gte":xValue-500,"lt":xValue}}}]
			openke.view.Analyze.searchWithFilters(myDateSlider.getDateRange().startValue,  myDateSlider.getDateRange().endValue, filters)			
		});
		
	}
	 
	function populateDomainTable(data) {
		var table = $('#topDomainsTable').DataTable();
		var startDateMS = myDateSlider.getDateRange().startValue;
		var endDateMS   = myDateSlider.getDateRange().endValue;
		
		var bucketArray = data.aggregations.top_domains.buckets;
		table.clear();
		for (var i = 0; i < bucketArray.length; i++) {
			var newRow =bucketArray[i];
			newRow.key= "<a href='javascript:openke.view.Analyze.searchField(\"domain.keyword\",\""+newRow.key+"\",\""+startDateMS+"\",\""+endDateMS+"\");'>"+newRow.key+"</a>"
			table.row.add(newRow);
		}
		table.draw();			
	}
	
	function populateEntityTable(data) {
		var table = $('#topEntitiesTable').DataTable();
		var startDateMS = myDateSlider.getDateRange().startValue;
		var endDateMS   = myDateSlider.getDateRange().endValue;
		
		if (data.aggregations.hasOwnProperty("spacy_root")) {
			var bucketArray = data.aggregations.spacy_root.item.buckets;
			table.clear();
			var selectList = $('#entityOptions')
			for (var i = 0; i < bucketArray.length; i++) {
				var newRow =bucketArray[i];
				var displayValue = openke.model.Analytics.mapSpacyEntityName(newRow.key)
				selectList.append($("<option></option>").attr("value",newRow.key).text(displayValue)); 
				newRow.key= "<a href='javascript:openke.view.Analyze.searchNestedField(\"spacy.entities\", \"spacy.entities.type.keyword\",\""+newRow.key+"\",\""+startDateMS+"\",\""+endDateMS+"\");'>"+displayValue+"</a>"
				table.row.add(newRow);
			}
			table.draw();
		}
		else {
			var bucketArray = data.aggregations.items.items.items.buckets;
			table.clear();
			var selectList = $('#entityOptions')
			for (var i = 0; i < bucketArray.length; i++) {
				var newRow =bucketArray[i];
				if (!openke.model.Analytics.isValidLabel(newRow.key)) {continue;}
				var displayValue = openke.model.Analytics.mapSpacyEntityName(newRow.key)
				selectList.append($("<option></option>").attr("value",newRow.key).text(displayValue)); 
				newRow.key= "<a href='javascript:openke.view.Analyze.searchNestedField(\"spacy.entities\", \"spacy.entities.text.keyword\",\""+newRow.key+"\",\""+startDateMS+"\",\""+endDateMS+"\");'>"+displayValue+"</a>"
				table.row.add(newRow);
			}
			table.draw();
		}
		
		
	}
	
	function populateLocationTable(data) {
		var table = $('#topLocationsTable').DataTable();
		var startDateMS = myDateSlider.getDateRange().startValue;
		var endDateMS   = myDateSlider.getDateRange().endValue;
		
		var bucketArray = data.aggregations.location.item.buckets;
		table.clear();
		for (var i = 0; i < bucketArray.length; i++) {
			var newRow =bucketArray[i];
			newRow.key= "<a href='javascript:openke.view.Analyze.searchNestedField(\"geotag\", \"geotag.geoData.preferredName.keyword\",\""+newRow.key+"\",\""+startDateMS+"\",\""+endDateMS+"\");'>"+newRow.key+"</a>"
			table.row.add(newRow);
		}
		table.draw();	
		
		$('#entityOptions')
	}	

	var privateMembers = {
	
	}
	
	return {
		initialize : initialize,
	
	};
}());
