/**
 * Create date - 20171101
 * Description: 
 *              
 * Usage:
 *  
 * 
 */

var CitationVisualization = (function () {
	"use strict";

	// variables to support Leaflet / Open Street Map Visualization
	var map
	var markerGroup;
	var infoWindow;
	
	var filters = [];  // array of filter clauses that should be added to "filter" when querying for records.  updated by the filters and options window
	
	
    var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
	var playOptions = {
			windowSize : 180,
			windowSizeUnit: "days",
            stepForwardSize: 90,
            stepForwardUnit: "days",
            timeDelay:    3000, 
            allowPlay: true
	}
	
	function openFilterWindw() {
		var screen_width = screen.width * .9;
	    var screen_height = screen.height* .6;
	    var top_loc = screen.height *.15;
	    var left_loc = screen.width *.05;
	
		window.open(openke.global.Common.getPageURLPrefix()+"/citationsFilter", "filterWindow", 'toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
	}
		
	// Only do one map update per 1/2 second
	var debounceSlideUpdate = openke.global.Common.debounce(function(data) {
		loadMapData(data.startValue,data.endValue)
	},500);
	
	
	var myDateSlider = new DateSlider('timeSlider', debounceSlideUpdate, "20091001T000000Z", "20170930T235959Z", step, "YYYYMMDD", true, true, playOptions )	
	

	
	function initialize() {
		LASLogger.instrEvent('application.literatureDiscovery.geovisualization');
	
		$("#btFilters").click(openFilterWindw);
		$("#btViewAsSearch").click(searchByFilters);
		
		$("#clusterResults").change(function() {
		    if (this.checked) {
				LASLogger.instrEvent('application.literatureDiscovery.geovisualization.mapClusters', {	clustering : true	});
		    	markerCluster.setMap(map)
		    }
		    else {
		    	LASLogger.instrEvent('application.literatureDiscovery.geovisualization.mapClusters', {	clustering : false	});
		    	markerCluster.setMap(null)
		    }
		});
		
		window.filterData = function(filterClauses) {
			filters = filterClauses;
			var startDateMS = myDateSlider.getDateRange().startValue;
			var endDateMS = myDateSlider.getDateRange().endValue;
			
			if (filters.length == 0) {
				$("#filterText").text("none");
			}
			else {
				$("#filterText").text(JSON.stringify(filters));
			}
			
			loadMapData(startDateMS,endDateMS)
		}
		map = L.map('map').setView([20.0, 0], 3);
		L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
		    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
		}).addTo(map);
		markerGroup = new L.markerClusterGroup([]);
		infoWindow  = L.popup({minWidth: 600, maxHeight: 400});
	}
	
	
	function loadMapData(startDateMillis,endDateMillis) {
		var query=
		{
			  "query": {
			    "bool": {
			      "filter": [ {  "range": { "DateCreated": { "gte": startDateMillis, "lte": endDateMillis   }   }   }  ]
			    }
			  },
			  "_source": [ "PMID","location"],
			  "size": 10000
			}
		
		filters.forEach(function(element) {
			query.query.bool.filter.push(element);
		})
		
		
		$.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/citations/data",
			type : "POST",
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(query),
			dataType : "JSON",
			success:populateMap	
		});
	
	}
	
	function getRandomArbitrary(min, max) {
		  return Math.random() * (max - min) + min;
		}
	
	function populateMap(data) {
		var recordArray = data.hits.hits;
		
		clearAllMarkers();
		
		var markersToAdd  = []
		recordArray.forEach(function(record){
			if (record._source.hasOwnProperty('location')) {
		        var point = {lat: record._source.location[1] + getRandomArbitrary(-0.005,0.005), lng: record._source.location[0]+ getRandomArbitrary(-0.005,0.005)};
		        var marker = L.marker([point.lat, point.lng])
		        attachListener(marker,record._id)
		        markersToAdd.push(marker);
			}
		});
        markerGroup.addLayers(markersToAdd);
		map.addLayer(markerGroup)
		
	}

	 function attachListener(marker, uuid) {
		 marker.on('click', function(event) {
			 var query = { "query": { "terms": { "_id": [uuid]  }  },
					       "_source": [ "Article","PubmedData","PMID", "authorFullName" ]
						 }
			 $.ajax({
				 url : openke.global.Common.getRestURLPrefix()+"/citations/data",
				 type : "POST",
				 contentType: "application/json; charset=utf-8",
				 data : JSON.stringify(query),
				 dataType : "JSON",
				 success: function (data) {
					 var record = data.hits.hits[0];
					 var abstract = "";
					 if (record._source.Article.hasOwnProperty("Abstract") && record._source.Article.Abstract.hasOwnProperty("AbstractText")) {
							abstract =  record._source.Article.Abstract.AbstractText;
					 }
						
					 var authorsList = "";
					if (record._source.hasOwnProperty("authorFullName")) {
							for (var i=0;i< record._source.authorFullName.length; i++) {
								authorsList += record._source.authorFullName[i] +
								               '&nbsp;&nbsp;[<a target=_blank href="https://www.google.com/search?q='+record._source.authorFullName[i]+'">Google</a>]'+
								               '&nbsp;&nbsp;[<a target=_blank href="https://scholar.google.com/scholar?q='+record._source.authorFullName[i]+'">Google Scholar</a>]'+
								               '&nbsp;&nbsp;[<a target=_blank href="https://academic.microsoft.com/#/search?iq=@'+record._source.authorFullName[i]+'@&q='+record._source.authorFullName[i]+'&filters=&from=0&sort=0">MS Academic</a>]'+
								"<br>";
							}
					}
					var doiString = "";
					if (record._source.PubmedData.id.hasOwnProperty("doi")) {
						doiString = '&nbsp;&nbsp;DOI: <a target=_blank href="https://doi.org/'+record._source.PubmedData.id.doi+'">'+ record._source.PubmedData.id.doi +'</a>'
					}
					
				    var contentString = '<div id="content">'+
				        '<b>'+record._source.Article.ArticleTitle+'</b><p>'+
				        '<a target=_blank href="https://www.ncbi.nlm.nih.gov/pubmed/'+record._source.PMID+'">'+
				        'PubMED</a>&nbsp;&nbsp;'+
				        '<a target=_blank href="https://scholar.google.com/scholar?q='+record._source.Article.ArticleTitle+'">'+
				        'Google Scholar</a>' + doiString +
				        '<br>'+ 
				        '<div id="bodyContent">'+ abstract  +"<p>&nbsp;<p>" +
				        authorsList+
				        '</div>'+
				        '</div>';  
					 
				    infoWindow.setLatLng(L.latLng(marker.getLatLng()));
				    infoWindow.setContent(contentString);
				    infoWindow.openOn(map);
					 
				 }	
				}); 
			 
				   
		});
	    
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
	
		filters.forEach(function(element) {
			query.query.bool.filter.push(element);
		})
		
		LASLogger.instrEvent('application.literatureDiscovery.geovisualization.searchByFilters', {
			criteria : query
		});
		
		$('#searchQuery').val(JSON.stringify(query));
		$('#searchForm').submit();
	}
	
	function clearAllMarkers() {
		map.removeLayer(markerGroup)
		markerGroup.clearLayers();
		map.addLayer(markerGroup)
	}
	
	return {
		initialize : initialize,
	};
}());


$(document).ready(function() {
	CitationVisualization.initialize();
})

