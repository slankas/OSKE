/**
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var ChoroplethMap = (function () {
	"use strict";

	// variables to support Leaflet / Open Street Map Visualization
	var map
	var markerGroup
	var infoWindow
	var myDateSlider
	
	function loadMapData(startDateMillis,endDateMillis) {
		//TODO: adjust this to bring in most recent data first (by published_dt.date(?)) 
		var query=
		{
		  "query": {
		    "bool": {
		      "filter": [ {  "range": { "published_date.date": { "gte": startDateMillis, "lte": endDateMillis   }   }   }  ]
		    }
		  },
		  "aggs": {
			    "geotag_root": {
			      "nested": {
			        "path": "geotag"
			      },
			      "aggs": {
			        "geotag": {
			          "terms": {
			            "field": "geotag.geoData.primaryCountryName.keyword",
			            "size" : 250
			          },
			          "aggs": {
			            "country_to_document": {
			              "reverse_nested": {}, 
			              "aggs": {
			                "country_count": {
			                  "terms": {
			                    "field": "geotag"
			                  }
			                }
			              }
			            }
			          }
			        }
			      }
			    }
			  }
		}
		
		openke.model.Analytics.getAnalyticFilterOptions().forEach(function(element) {
			query.query.bool.filter.push(element);
		})
		
		$(".overlay").show();
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			async: false,
			success: function(data) {
				populateMap(data, query, startDateMillis, endDateMillis);
			},
			error: function() {
				$(".overlay").hide();
				Snackbar.show({text: "Error in getting results.", duration: 3500})
			}
		});
	
	}

	
	function populateMap(results, query, startDateMS, endDateMS) {
		var buckets = results.aggregations.geotag_root.geotag.buckets; 
		var totalHits = results.hits.total.value;
		var docHits = 0;
		var seenCountries = new Map();
		
		for (var i=0; i< buckets.length; i++) {
			seenCountries.set(buckets[i].key, buckets[i].country_to_document.doc_count);
			docHits += buckets[i].country_to_document.doc_count;
		}
		
		var layout = {
				title: 'Document Count by Country',
				geo: {
					projection: {
						type: 'robinson'
					}
				}
		};
		var data = [ {
			type: 'choropleth',
			locationmode: 'country names',
			locations: getKeys(seenCountries),
			z: getValues(seenCountries),
			autocolorscale: true
		}];

		Plotly.newPlot('choro', data,layout).then(graphChoro => {
			graphChoro.on('plotly_click', data => {
				var pt = (data.points || [])[0];
				var filter = ElasticSupport.createMustGeotag(pt.location);
				openke.view.Analyze.searchWithFiltersClean(startDateMS,  endDateMS, filter);
			});
		});
		
		$(".overlay").hide();
				
	 }
	
	
	function getKeys(map) {
		//plotly needs an array
		var keys = [...map.keys()]
		return keys;
	}
	
	function getValues(map) {
		//plotly needs an array
		var values = [...map.values()]
		return values;
	}


	

	
	function initialize() {
		LASLogger.instrEvent('application.analytics.geovisualization');
		
		$(".overlay").hide();		
		
		//Callback method for the filter window.
		window.filterData = function(filterClauses) {
			var startDateMS = myDateSlider.getDateRange().startValue;
			var endDateMS = myDateSlider.getDateRange().endValue;
					
			openke.view.Analyze.displayFilters();
			loadMapData(startDateMS,endDateMS)
		}
		
		openke.model.Analytics.loadDates(intializeAfterDateLoad)
	}
	
	function intializeAfterDateLoad(dates) {
			
		// Only do one  update per .2 seconds
		var debounceSlideUpdate = openke.global.Common.debounce(function(data) {
			loadMapData(data.startValue,data.endValue)
		},500);
				
	    var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
		myDateSlider = new DateSlider('timeSlider', debounceSlideUpdate, dates.aggregations.minPublishedDate.value_as_string,dates.aggregations.maxPublishedDate.value_as_string, step, "YYYYMMDD", true, true, openke.view.Analyze.defaultSliderPlayOptions )
		
		//creating myDateSlide initiates an event which calls debounceSlide/update (which calls loadMapData)
	}	
	
	
	return {
		initialize : initialize,
	};
}());


$(document).ready(function() {
	ChoroplethMap.initialize();
});

