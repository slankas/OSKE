/**
 * 
 * Create date - 20171101
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var AnalyticHeatMapTimeline = (function () {
	"use strict";

	var xAxisField   = null;
	var yAxisField   = null;
	var myDateSlider
	
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
	
	function createHeatMap(data) {
		
		const charRegExpValidLabel = /^\w+(?!\:\(\")(?:\s|\w|\.)+$/;
		
		// assign bucket to the start of the actual.  the top level there forms the x-axis
		var bucket = data.aggregations.item.buckets; 
		
		var xAxisLabels = [];   // list of the keys (column names for the x-axis....
	
		var yAxisLabels = [];		
		
		var z = [];

		for (var i=0; i < bucket.length; i++) {
			var xItem = bucket[i].key_as_string;
			
			xAxisLabels.push(xItem);
			
			
			var subBucket = bucket[i].concepts_root.item.item.buckets;
			for (var j=0; j < subBucket.length; j++) {
				var yItem  = subBucket[j].key;
				var count = subBucket[j].root_document.doc_count;  				
				
				if (yAxisLabels.indexOf(yItem) < 0) {
					z.push(new Array(bucket.length).fill(0));
					
					yAxisLabels.push(yItem);
				}
				
				z[yAxisLabels.indexOf(yItem)][xAxisLabels.indexOf(xItem)] = count;
			}
				
		}
		
		//Check to make sure all of the rows have good labels
		for (var i=yAxisLabels.length-1; i >= 0; i--) {
			var yLabelToTest = yAxisLabels[i];
			//if (!charRegExpValidLabel.test(yLabelToTest)) {
			if (!openke.model.Analytics.isValidLabel(yLabelToTest)){
				yAxisLabels.splice(i, 1)
				z.splice(i,1);
			}
		}
		
		var xMax = xAxisLabels.length;
		var yMax = Math.min(getSizeEntered('yAxisSize'), yAxisLabels.length)
		
		if (yAxisLabels.length > yMax) {yAxisLabels.length = yMax; z.length= yMax}
		
		var xNiceFieldName = $("#xAxisField option:selected").text();
		var xAxisName = xNiceFieldName.substring(xNiceFieldName.indexOf("y")+1).trim();
		var yActualFieldName = yAxisField.substring(yAxisField.indexOf(":")+1);
		
		if(yAxisField.indexOf("entity") > -1){ yActualFieldName = openke.model.Analytics.mapSpacyEntityName(yActualFieldName);}
		
		
		var layout = {
			title: 'Occurrences of '+ yActualFieldName + ' ' + xNiceFieldName,
			xaxis: { title: xAxisName  },
			yaxis: { title: yAxisField  },
			margin: {
			    l: 200,
			    r: 50,
			    b: 100,
			    t: 100,
			    pad: 4
			  }
		};

		var data = [ {
			    z: z,
			    x: xAxisLabels, 
			    y: yAxisLabels,
			    colorscale: 'Bluered', //'Blues',
			    type: 'heatmap'
	    }];

		Plotly.newPlot('tester', data,layout);
		
		var myPlot = document.getElementById('tester')
		var dragLayer = document.getElementsByClassName('nsewdrag')[0]
		myPlot.on('plotly_hover', function(data){
		  dragLayer.style.cursor = 'pointer'
		});
		myPlot.on('plotly_unhover', function(data){
		  dragLayer.style.cursor = ''
		});
		
		myPlot.on('plotly_click', function(data){
			var xValue = data.points[0].x
			var yValue = data.points[0].y
			
			var myMomentStart = moment(xValue, "MM-DD-YYYY")
			var myMomentEnd = myMomentStart.clone();
			if(xAxisName.indexOf("Month") > -1){
				myMomentEnd = myMomentEnd.add(1,'months')
			} 
			else {
				myMomentEnd = myMomentEnd.add(7,'days')
			}
			
			var xyFilter = [ getFilterQuery(yAxisField, yValue) ];
			openke.view.Analyze.searchWithFilters(myMomentStart.valueOf(),  myMomentEnd.valueOf(), xyFilter)
			
		});
	}
	
	
	function createQuery(xField, yField, ySize, startTimeMS,endTimeMS, appliedFilters) {
		var query= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startTimeMS, "lte": endTimeMS  }  } } ]      }
				  },
				  "size": 0
		       }
		
		appliedFilters.forEach(function(element) {
			query.query.bool.filter.push(element);
		});
		
		var aggHeatMapQuery =  ElasticSupport.createQueryHeatMapTimeline(xField,yField,ySize);
				
		query.aggs = aggHeatMapQuery;
		return query;
    }
    
	
	// also uses xAxisField, yAxisField.  will load sizes from current size boxes.  those default to 20 if not completed correctly
	// queryFilters comes from localSession, accessed through Header object
	function loadHeatMapData(startTimeMS,endTimeMS) {
		if (!yAxisField || !xAxisField ) {return; } //haven't defined those fields, can't plot
		var xAxisSize = getSizeEntered('xAxisSize');
		var yAxisSize = getSizeEntered('yAxisSize');
		
		var myQuery = createQuery(xAxisField, yAxisField, yAxisSize, startTimeMS, endTimeMS, openke.model.Analytics.getAnalyticFilterOptions())
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(myQuery),
			dataType : "JSON",
			success:createHeatMap	
		});
		
	}
	

	function viewAction() {
		
		var localXAxisField = $("#xAxisField").val();
		var localYAxisField = $("#yAxisField").val();

		if (localXAxisField == "") { bootbox.alert("You must select a field for the x-axis"); 	return false;	}	
		if (localYAxisField == "") { bootbox.alert("You must select a field for the y-axis"); 	return false;	}
		
		// set the "global" variables for x and y and then go produce the map
		xAxisField = localXAxisField;
		yAxisField = localYAxisField;

		var startDateMS = myDateSlider.getDateRange().startValue;
		var endDateMS   = myDateSlider.getDateRange().endValue;
		
		LASLogger.instrEvent('application.analytic.heatMapTimeline.view', {
			xAxis: xAxisField,
			yAxis: yAxisField,
			startDate: startDateMS,
			endDate: endDateMS
		});
		
		
		loadHeatMapData(startDateMS,endDateMS);
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
				};
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
        			var spacyText = "Entity: "+ openke.model.Analytics.mapSpacyEntityName(v.key)
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
		LASLogger.instrEvent('application.analytic.heatmap.initialize');
				
		$('#btView').click(function() {viewAction(); return false;})

		
		window.filterData = function(filterClauses) {
			var startDateMS = myDateSlider.getDateRange().startValue;
			var endDateMS = myDateSlider.getDateRange().endValue;
			loadHeatMapData(startDateMS,endDateMS)

			openke.view.Analyze.displayFilters();
		}
		openke.model.Analytics.loadDates(intializeAfterDateLoad)
	}
	
	function intializeAfterDateLoad(dates) {
			
		// Only do one  update per .2 seconds
		var debounceSlideUpdate = openke.global.Common.debounce(function(data) {
			loadHeatMapData(data.startValue,data.endValue)
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
	AnalyticHeatMapTimeline.initialize();
})

