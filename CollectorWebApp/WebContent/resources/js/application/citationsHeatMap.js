/**
 * Create date - 20171101
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var CitationsHeatMap = (function () {
	"use strict";

	var queryFilters = [];
	var xAxisField   = null;
	var yAxisField   = null;
	
	var labelTranslation = {
			"action": "Actions",
			"author": "Authors",
			"chemical": "Chemicals",
			"concept": "Concepts",
			"country": "Countries",
			"journal": "Journals",
			"authorKeyword": "Keywords - Author",
			"meshKeyword": "Keywords - MESH",
			"kit": "Kits",
			"thing": "Things",
			"university": "Universities",
			"vendor": "Vendors"
	}
	
	// Only do one  update per .2 seconds
	var debounceSlideUpdate = openke.global.Common.debounce(function(data) {
		loadHeatMapData(data.startValue,data.endValue)
	},200);
	
	
    var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
	var playOptions = {
			windowSize : 120,
			windowSizeUnit: "days",
            stepForwardSize: 60,
            stepForwardUnit: "days",
            timeDelay:    5000, 
            allowPlay: true
	}
	var myDateSlider = new DateSlider('timeSlider', debounceSlideUpdate, "20091001T000000Z", "20170930T235959Z", step, "YYYYMMDD", true, true, playOptions )
	
	
	function getSizeEntered(fieldID) {
		var size = Number.parseInt($('#'+fieldID).val());
		if (isNaN(size)) {
			size = 20;
		}
		return size;
	}
	
	// also uses "queryFilters", xAxisField, yAxisField.  will load sizes from current size boxes.  those default to 20 if not completed correctly
	function loadHeatMapData(startTimeMS,endTimeMS) {
		if (!yAxisField || !xAxisField ) {return; } //haven't defined those fields, can't plot
		var xAxisSize = getSizeEntered('xAxisSize');
		var yAxisSize = getSizeEntered('yAxisSize');
		
		var myQuery = createQuery(xAxisField, xAxisSize, yAxisField, yAxisSize, startTimeMS, endTimeMS, queryFilters)
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(myQuery),
			dataType : "JSON",
			success:createHeatMap	
		});
		
	}
	
	function compute2dArraySize(ar){
	    var row_count = ar.length;
	    var row_sizes = []
	    for(var i=0;i<row_count;i++){
	        row_sizes.push(ar[i].length)
	    }
	    return [row_count, Math.min.apply(null, row_sizes)]
	}
	
	function getSubBucket(parentBucket) {
		var subBucket = parentBucket;
		while (subBucket.hasOwnProperty("items")) { subBucket = subBucket.items;}
		subBucket = subBucket.item.buckets;
		
		return subBucket;
	}
	
	function createHeatMap(data) {
		
		// assign bucket to the start of the actual.  the top level there forms the x-axis
		var bucket = data.aggregations;  
		while (bucket.hasOwnProperty("items")) { bucket = bucket.items;}
		bucket = bucket.item.buckets;
		
		var xAxis = {}
		var xAxisLabels = [];
	
		var yAxis = {}
		var yAxisLabels = [];		
		
		var z = [];
		
		if (xAxisField === yAxisField) { // if both axis are the same, make them appear that way as well rather than giving precedence the first values seen.
			for (var i=0; i < bucket.length; i++) {
				var xItem = bucket[i].key;
				xAxis[xItem] = i;
				xAxisLabels.push(xItem);
					
				yAxis[xItem] = i;
				yAxisLabels.push(xItem);
				
				z.push([]);
			}
		}
		for (var i=0; i < bucket.length; i++) {
			var xItem = bucket[i].key;
			if (xAxisField !== yAxisField) {
				xAxis[xItem] = i;
				xAxisLabels.push(xItem);
			}
			
			var subBucket = getSubBucket(bucket[i]);
			for (var j=0; j < subBucket.length; j++) {
				var yItem  = subBucket[j].key;
				var count = subBucket[j].doc_count;
				if (yAxis.hasOwnProperty(yItem) == false) {
					z.push([]);
					yAxis[yItem] = j;
					yAxisLabels.push(yItem);
				}
				
				z[yAxis[yItem]][xAxis[xItem]] = count;
			}
				
		}
		
		var xMax = Math.min(getSizeEntered('xAxisSize'), xAxisLabels.length);
		var yMax = Math.min(getSizeEntered('yAxisSize'), yAxisLabels.length)
		
		for (var i=0; i <xMax; i++) {
			for (var j=0; j <yMax; j++) {
				if (typeof z[j][i] === 'undefined') {
					z[j][i]=0;
				}
			}
		}
		
		if (yAxisLabels.length > yMax) {yAxisLabels.length = yMax; z.length= yMax}
		
		var layout = {
			title: 'Co-occurrences of '+ labelTranslation[xAxisField] + ' and ' + labelTranslation[yAxisField],
			xaxis: { title: labelTranslation[xAxisField]  },
			yaxis: { title: labelTranslation[yAxisField]  },
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
		
		document.getElementById('tester').on('plotly_click', function(data){
			var xValue = data.points[0].x
			var yValue = data.points[0].y 
			
			var startDateMS = myDateSlider.getDateRange().startValue;
			var endDateMS   = myDateSlider.getDateRange().endValue;
			var query= {
					  "query": {
					    "bool": {  "filter": [ {"range": { "DateCreated": { "gte": startDateMS, "lte": endDateMS  }  } }, getFilterQuery(xAxisField, xValue), getFilterQuery(yAxisField, yValue)  ]      }
					  },
					  "size": 20, "from": 0
			       } 
						
			LASLogger.instrEvent('application.literatureDiscovery.heatMap.searchCell', {
				criteria : query
			});
			
			$('#searchQuery').val(JSON.stringify(query));
			$('#searchForm').submit();
			
		});
	}
	
	function getFilterQuery(fieldName, fieldValue) {
		switch (fieldName) {
		case "author":        return ElasticSupport.createFilterField("authorFullName.keyword",[fieldValue]);
		case "chemical":      return ElasticSupport.createFilterField("ChemicalList.Chemical.NameOfSubstance.content.keyword",[fieldValue]);
		case "country":       return ElasticSupport.createFilterField("Article.AuthorList.Author.AffiliationInfo.location.country.keyword",[fieldValue]);
		case "journal":       return ElasticSupport.createFilterField("Article.Journal.Title.keyword",[fieldValue]);
		case "authorKeyword": return ElasticSupport.createFilterField("keywordMinor.keyword",[fieldValue]);
		case "meshKeyword":   return ElasticSupport.createFilterField("MeshHeading.DescriptorName.content.keyword",[fieldValue]);
		case "university":    return ElasticSupport.createFilterConcept(null, null, null, ["University"], [fieldValue]);
		case "vendor"    :    return ElasticSupport.createFilterConcept([fieldvalue], null, null, ["vendor"], null);
		case "action" :       return ElasticSupport.createFilterConcept([fieldValue], null, [ "wolfhunt.technology_regex"], ["action"], null);
		case "concept" :      return ElasticSupport.createFilterConcept([fieldValue], null, [ "wolfhunt.technology_regex"], ["concept"], null);
		case "kit":           return ElasticSupport.createFilterConcept([fieldValue], null, [ "wolfhunt.technology_regex"], ["kit"], null);
		case "thing":         return ElasticSupport.createFilterConcept([fieldValue], null, [ "wolfhunt.technology_regex"], ["thing"], null);
				
		default: return null;
		}
	}
	
	
	function openFilterWindow() {
		LASLogger.instrEvent('application.literatureDiscovery.heatmap.openFilters');
		
		var screen_width = screen.width * .9;
	    var screen_height = screen.height* .6;
	    var top_loc = screen.height *.15;
	    var left_loc = screen.width *.05;
	
		window.open(openke.global.Common.getPageURLPrefix()+"/citationsFilter", "filterWindow", 'toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
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
		
		LASLogger.instrEvent('application.literatureDiscovery.heatmap.view', {
			xAxis: xAxisField,
			yAxis: yAxisField,
			startDate: startDateMS,
			endDate: endDateMS
		});
		
		
		loadHeatMapData(startDateMS,endDateMS);
	}
	
	
	
    var aggregationQueries = {
		"action" :       {"aggs":{"items":{"nested":{"path":"concepts"},"aggs":{"items":{"filter":{"term":{"concepts.type.keyword":"action"}},"aggs":{"items":{"filter":{"term":{"concepts.category.keyword":"wolfhunt.technology_regex"}},"aggs":{"item":{"terms":{"field":"concepts.name.keyword","size":"size"}}}}}}}}}},
		"author" :       {"aggs":{"item":{"terms":{"field":"authorFullName.keyword","order":{"_count":"desc"},"size":"size"}}}},
		"chemical" :     {"aggs":{"item":{"terms":{"field":"ChemicalList.Chemical.NameOfSubstance.content.keyword","order":{"_count":"desc"},"size":"size"}}}},
		"concept" :      {"aggs":{"items":{"nested":{"path":"concepts"},"aggs":{"items":{"filter":{"term":{"concepts.type.keyword":"concept"}},"aggs":{"items":{"filter":{"term":{"concepts.category.keyword":"wolfhunt.technology_regex"}},"aggs":{"item":{"terms":{"field":"concepts.name.keyword","size":"size"}}}}}}}}}},
		"country" :      {"aggs":{"item":{"terms":{"field":"Article.AuthorList.Author.AffiliationInfo.location.country.keyword","order":{"_count":"desc"},"size":"size"}}}},
		"journal" :      {"aggs":{"item":{"terms":{"field":"Article.Journal.Title.keyword","order":{"_count":"desc"},"size": "size"}}}},
		"authorKeyword" :{"aggs":{"item":{"terms":{"field":"keywordMinor.keyword","order":{"_count":"desc"},"size": "size"}}}},
		"meshKeyword" :  {"aggs":{"item":{"terms":{"field":"MeshHeading.DescriptorName.content.keyword","order":{"_count":"desc"},"size":"size"}}}},
		"kit" :          {"aggs":{"items":{"nested":{"path":"concepts"},"aggs":{"items":{"filter":{"term":{"concepts.type.keyword":"kit"}},"aggs":{"items":{"filter":{"term":{"concepts.category.keyword":"wolfhunt.technology_regex"}},"aggs":{"item":{"terms":{"field":"concepts.name.keyword","size":"size"}}}}}}}}}},
		"thing" :        {"aggs":{"items":{"nested":{"path":"concepts"},"aggs":{"items":{"filter":{"term":{"concepts.type.keyword":"thing"}},"aggs":{"items":{"filter":{"term":{"concepts.category.keyword":"wolfhunt.technology_regex"}},"aggs":{"item":{"terms":{"field":"concepts.name.keyword","size":"size"}}}}}}}}}},
		"university" :   {"aggs":{"items":{"nested":{"path":"concepts"},"aggs":{"items":{"filter":{"term":{"concepts.type.keyword":"University"}},"aggs":{"item":{"terms":{"field":"concepts.value.keyword","order":{"_count":"desc"},"size":"size"}}}}}}}},
		"vendor" :       {"aggs":{"items":{"nested":{"path":"concepts"},"aggs":{"items":{"filter":{"term":{"concepts.type.keyword":"vendor"}},"aggs":{"item":{"terms":{"field":"concepts.name.keyword","order":{"_count":"desc"},"size":"size"}}}}}}}}
    }
    
    function setSizeInClause(clause,size) {
    	for (var key in clause) {
    	    if (clause.hasOwnProperty(key)) {
    	        if (key == "size") {
    	        	clause[key] = size;
    	        	return;
    	        }
    	        if (typeof clause[key] == 'object') {
    	        	setSizeInClause(clause[key],size)
    	        }
    	    }
    	}
    }
    
    function appendClause(bucket,subBucket) {
    	for (var key in bucket) {
    	    if (bucket.hasOwnProperty(key)) {
    	        if (key == "item") {
    	        	bucket.item.aggs = subBucket.aggs;
    	        	return;
    	        }
    	        if (typeof bucket[key] == 'object') {
    	        	appendClause(bucket[key],subBucket)
    	        }
    	    }
    	}
    }   
    
			
    function createQuery(xField, xSize, yField, ySize, startTimeMS,endTimeMS, appliedFilters) {
		var query= {
					  "query": {
					    "bool": {  "filter": [ {"range": { "DateCreated": { "gte": startTimeMS, "lte": endTimeMS  }  } } ]      }
					  },
					  "size": 0
			       }
		
		appliedFilters.forEach(function(element) {
			query.query.bool.filter.push(element);
		})
		
		var xClause =jQuery.extend(true, {}, aggregationQueries[xField]); // need to clone the object as we'll be modifying it
		setSizeInClause(xClause,xSize);
		
		var yClause =jQuery.extend(true, {}, aggregationQueries[yField]);
		setSizeInClause(yClause,ySize);
		
		appendClause(xClause,yClause);
		
		query.aggs = xClause.aggs;
		
		return query;
    }
    
	function searchByFilters() {
		var startTimeMS = myDateSlider.getDateRange().startValue;
		var endTimeMS   = myDateSlider.getDateRange().endValue;
		var query= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "DateCreated": { "gte": startTimeMS, "lte": endTimeMS  }  } } ]      }
				  },
				  "size":20, "from":0 
		       }
	
		queryFilters.forEach(function(element) {
			query.query.bool.filter.push(element);
		})
		
		LASLogger.instrEvent('application.literatureDiscovery.heatmap.searchByFilters', {
			criteria : query
		});
		
		$('#searchQuery').val(JSON.stringify(query));
		$('#searchForm').submit();
	}
    
    
	function initialize() {
		LASLogger.instrEvent('application.literatureDiscovery.heatmap');
		
		$('#btView').click(function() {viewAction(); return false;})
		$("#btFilters").click(openFilterWindow);
		$("#btnViewAsSearch").click(searchByFilters);
		
		window.filterData = function(filterClauses) {
			queryFilters = filterClauses;
			var startDateMS = myDateSlider.getDateRange().startValue;
			var endDateMS = myDateSlider.getDateRange().endValue;
			loadHeatMapData(startDateMS,endDateMS)

			if (queryFilters.length == 0) {
				$("#filterText").text("none");
			}
			else {
				$("#filterText").text(JSON.stringify(queryFilters));
			}
		}
	}
	
	return {
		initialize : initialize,
	};
}());


$(document).ready(function() {
	CitationsHeatMap.initialize();
})

