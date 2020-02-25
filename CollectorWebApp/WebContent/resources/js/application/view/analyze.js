

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.Analyze = (function () {
	"use strict";
	
	function initialize() {
		$('#btnFilters').on('click',openFilterWindow);
		$('#btnClearFilters').on('click',clearFilters);
		displayFilters();		
	}
	
	
		
	// Change to the current page and send an instrumentation event
    function navigateTo(location) {
		var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
		LASLogger.instrEvent('topNavigation.link', {
				link : completeLocation
		}, function() {window.location = completeLocation;	});
	} 
	
	function openFilterWindow() {
		var screen_width = screen.width * .9;
	    var screen_height = screen.height* .8;
	    var top_loc = screen.height *.15;
	    var left_loc = screen.width *.05;
	
		window.open(openke.global.Common.getPageURLPrefix()+"/analyticFilter", "filterWindow", 'toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
	}
	
	function clearFilters() {
		openke.model.Analytics.clearAnalyticFilterData()
		displayFilters(true);
	}
	
	function displayFilters(loadData=false) {
		if (openke.model.Analytics.getAnalyticFilterState() == null) {
			$('#btnClearFilters').hide()
			$('#hdrFilterContents').html("")
		}
		else {
			$('#btnClearFilters').show()
			var filterHTML = "<b>Filters:</b>&nbsp;&nbsp;";
			var savedState = openke.model.Analytics.getAnalyticFilterState();
			
			var cards = savedState.cards;
			var keywords = savedState.keywords;
			var excludeSessions = savedState.excludeSessions;
			var sessions = savedState.sessionNames;
			var crawltime = savedState.crawltime;
			
			
			
			for (var i=0; i < cards.length; i++) {
				var analyticStateItem = cards[i];
				if (analyticStateItem.selectedValues.length >0) {
					filterHTML += "<i>"+analyticStateItem.title+"</i>"+"-" +analyticStateItem.selectedValues.join(",") +";&nbsp;&nbsp;";
				}
			}
			
			if (keywords && keywords.length > 0){
					filterHTML += "<i>Keywords</i>-" +keywords.join(",") +";&nbsp;&nbsp;";
			}
			
			if (excludeSessions && excludeSessions === true) {
				filterHTML += "<i>Discovery Sessions</i>- EXCLUDED;&nbsp;&nbsp;";
			}
			if (sessions && sessions.length > 0){
					filterHTML += "<i>Discovery Sessions</i>-"+sessions.join(",") +";&nbsp;&nbsp;";
			}
			
			if (crawltime ){
				var start, end = '';
				if (crawltime.startime){
					start = crawltime.startime;
				}
				if (crawltime.endtime){
					end = crawltime.endtime;
				}
				if (start && end ){filterHTML += "<i>Crawl Time Start-</i> "+start+", <i>End-</i> "+end+";&nbsp;&nbsp;";}
				else if (start && !end ){filterHTML += "<i>Crawl Time Start-</i> "+start+";&nbsp;&nbsp;";}
				else if (!start && end ){filterHTML += "<i>Crawl Time End-</i> "+end+";&nbsp;&nbsp;";}
			}
			
			
			
			$('#hdrFilterContents').html(filterHTML)
		}
		if (loadData) {
			window.filterData(openke.model.Analytics.getAnalyticFilterOptions())
		}
	}
	
	function submitQuery(query) {
		$('#searchQuery').val(JSON.stringify(query))
		$('#analyzeSearchForm').submit()		
	}
	
	function searchField(fieldName, fieldValue, startDateMS, endDateMS) {
		var query = openke.model.Analytics.createSearchFieldQuery(fieldName, fieldValue, startDateMS, endDateMS)
		submitQuery(query)
	}

	function searchWithFilters(startDateMS, endDateMS, filters) {
		var query = openke.model.Analytics.createSearchWithFiltersQuery(startDateMS, endDateMS, filters);
		submitQuery(query)
	}
	
	function searchWithFiltersClean(startDateMS, endDateMS, filters) {
		var query = openke.model.Analytics.createSearchWithFiltersQueryClean(startDateMS, endDateMS, filters);
		submitQuery(query)
	}
	
	
	function searchNestedField(nestedPath, fieldName, fieldValue, startDateMS, endDateMS) {
		var query = openke.model.Analytics.createSearchNestedFieldQuery(nestedPath, fieldName, fieldValue, startDateMS, endDateMS)
		submitQuery(query)
	}
	
	function searchBySavedFilters(startDateMS, endDateMS) {
		searchWithFilters(startDateMS, endDateMS, [])
	}	
	
	var defaultSliderPlayOptions = {
			windowSize : 120,
			windowSizeUnit: "days",
            stepForwardSize: 60,
            stepForwardUnit: "days",
            timeDelay:    5000, 
            allowPlay: true
	}
	
	var privateMembers = {

	};

	return {
		initialize : initialize,

		openFilterWindow : openFilterWindow,
		clearFilters : clearFilters,
		displayFilters : displayFilters,
		
		searchField : searchField,
		searchWithFilters : searchWithFilters,
		searchWithFiltersClean : searchWithFiltersClean,
		searchNestedField : searchNestedField,
		searchBySavedFilters : searchBySavedFilters,
		
		defaultSliderPlayOptions : defaultSliderPlayOptions,
		
		privateMembers : privateMembers
	};
}());

$(document).ready(function() {
	openke.view.Analyze.initialize();
});

