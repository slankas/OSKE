/**
 *
 * Create date - 20181128
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var AnalyticVisualization = (function () {
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
		  "_source": [ "source_uuid","geotag","geoData"],
		  "size": 2000,
		  "sort": [ { "published_date.date": { "order": "desc" } } ]
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
			success:populateMap,
			error: function() {
				$(".overlay").hide();
			}
		});
	
	}
	
	function getRandomArbitrary(min, max) {
		  return Math.random() * (max - min) + min;
		}
	
	function populateMap(data) {
		var recordArray = data.hits.hits;
		var totalHits = data.hits.total.value;
		
		clearAllMarkers();
		
		var markersToAdd  = [];
		recordArray.forEach(function(record) {
			if (markersToAdd.length > 10000) {return;} // put a maximum limit on the markers that appear
			if (record._source.hasOwnProperty('geotag')) {
				var seenTags = {};
				
				record._source.geotag.forEach(function(geoRecord,index) {
					
					if (!seenTags.hasOwnProperty(geoRecord.geoData.geoNameID)) { // don't duplicate geo locations for the same document
						seenTags[geoRecord.geoData.geoNameID] = geoRecord.geoData.geoNameID;
						var point = {lat: geoRecord.geoData.latitude + getRandomArbitrary(-0.005,0.005), lng: geoRecord.geoData.longitude+ getRandomArbitrary(-0.005,0.005)};
				        var marker = L.marker([point.lat, point.lng])
				        attachListener(marker,record._id)
				        markersToAdd.push(marker);
					}
					
				})
			}
		});
		
        markerGroup.addLayers(markersToAdd);
		map.addLayer(markerGroup)
		$(".overlay").hide();
		
		if (totalHits > 2000) {
			Snackbar.show({text: "More than 2,000 records exist for the current settings. Consider additional filters to see more relevant results.", duration: 8000});
		}else if (markersToAdd.length > 10000) {
			Snackbar.show({text: "More than 10,000 map markers exist for the current settings. Consider additional filters to see more relevant results.", duration: 8000});
		}
		
	}

	 function attachListener(marker, uuid) {
		 //show html_title, text
		 
		 marker.on('click', function( event) {
			 var query = { "query": { "term": { "source_uuid.keyword": uuid  }  } }
			 
			 $.ajax({
				 url : openke.global.Common.getRestURLPrefix()+"/citations/data",
				 type : "POST",
				 contentType: "application/json; charset=utf-8",
				 data : JSON.stringify(query),
				 dataType : "JSON",
				 success: function (data) {
					if (data.hits.hits.length == 0 ) {
						bootbox.alert("Unable to find detailed record")
						return;
					}
					var contentString = '<div id="geoSpatialInfoWindow" style="width: 750px; max-height: 350px; overflow: auto;"></div>';  
					infoWindow.setLatLng(L.latLng(marker.getLatLng()));
				    infoWindow.setContent(contentString);
				    infoWindow.openOn(map);
				    
					var docRecord = data.hits.hits[0]._source;
					
					var url  = docRecord.url;
					var uuid = docRecord.source_uuid;
					var title = docRecord.url;
					if (docRecord.hasOwnProperty("html_title")) { title = docRecord.html_title; }				
					
					var text = escapeHtml(docRecord.text).replace(/\n/g, "<p />");
				
					docRecord.text = text;
										
					var rec = new openke.component.ResultObject(uuid, title, url, "", docRecord, true, true);	
					$("#geoSpatialInfoWindow").html("<table style='width:100%'></table>");
					$("#geoSpatialInfoWindow" + ' table').append(rec.getRecordDOM());
					rec.displayRecord();

					var additionalData = {
							domain : openke.global.Common.getDomain(),
							storageArea : "normal",
							type : "_doc",
							title: title
					}
					var domMenu = OKAnalyticsManager.produceObjectAnalyticsMenu(uuid, "", docRecord ,url, additionalData,rec);  //note, not all of these need to be defined.  The called analytic will check
					rec.displayMenu(domMenu);	
					
					var collectionDOM = openke.view.DocumentBucketSupport.createCollectionSelect(docRecord.source_uuid,rec)
					rec.appendToMenu(collectionDOM);
					if (typeof(docRecord.user_collection) != 'undefined') {
						openke.view.DocumentBucketSupport.populateResultObject(rec,docRecord.user_collection)
					}
					
					rec.establishFullTextToggle("",true,false);
				 }	
				}); 
			 
				   
		});
	    
	}
	
	function searchByFilters() {
		var startTimeMS = myDateSlider.getDateRange().startValue;
		var endTimeMS   = myDateSlider.getDateRange().endValue;
		var query= {
				  "query": {
				    "bool": {  "filter": [ {"range": { "published_date.date": { "gte": startTimeMS, "lte": endTimeMS  }  } } ]      }
				  },
				  "size":20, "from":0 
		       }
	
		openke.model.Analytics.getAnalyticFilterOptions().forEach(function(element) {
			query.query.bool.filter.push(element);
		})
		
		LASLogger.instrEvent('application.analytics.geovisualization.searchByFilters', {
			criteria : query
		});
		
		$('#searchQuery').val(JSON.stringify(query));
		$('#analyzeSearchForm').submit();
	}
	
	function clearAllMarkers() {
		map.removeLayer(markerGroup)
		markerGroup.clearLayers();
		map.addLayer(markerGroup)
	}
	
	function initialize() {
		LASLogger.instrEvent('application.analytics.geovisualization');
		
		$(".overlay").hide();
		
		OKAnalyticsManager.defineStandardDocumentMenu("application.viewIndex.", false, true);
		openke.model.DocumentBucket.setDefaultInstrumentationPage('application.analytics.geovisualization');
		openke.view.DocumentBucketSupport.setDefaultInstrumentationPage('application.analytics.geovisualization');
		openke.model.DocumentBucket.loadAll();
		
		$("#btViewAsSearch").click(searchByFilters);
		
		$("#clusterResults").change(function() { // this code was from the GoogleMap view: TODO: https://github.com/Leaflet/Leaflet.markercluster  - turnoff clustering??
		    if (this.checked) {
				LASLogger.instrEvent('application.analytics.geovisualization.mapClusters', {	clustering : true	});
		    	markerCluster.setMap(map)
		    }
		    else {
		    	LASLogger.instrEvent('application.analytics.geovisualization.mapClusters', {	clustering : false	});
		    	markerCluster.setMap(null)
		    }
		});
		
		map = L.map('map').setView([20.0, 0], 3);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
		    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
		}).addTo(map);
		markerGroup = new L.markerClusterGroup([]);
		infoWindow  = L.popup({minWidth: 770, maxHeight: 400, maxWidth: 770});
		
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
	AnalyticVisualization.initialize();
})

