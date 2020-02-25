/**
 * Create date - 20170424
 * Description: Used to create document indexes
 * 
 * Dependencies:
 *   jQuery
 *   LASLogger
 * 
 * Usage:
 *    include script
 *    
 *    On the page initialization code, call (these are examples only - change the page name and default data appropriately)
 *        RecordLevelAnalytics.setDefaultInstrumentationPage("application.pageName.");
 *        setDefaultInstrumentationData( { sessionID : sessionId,  executionNumber: enumber} );
 *        
 *   HTML Dependencies: needs btShowIndex and btCreateIndex  
 */

// NOTE: This file is depracated, use openke.model.DocumentIndex instead. (this file can only support a single index on a page)

var DocumentIndex = (function () {
	"use strict";
 
	var indexStatusTimer = null;
	
	// For instrumentation events, this will the be basic information sent to the service
	var defaultInstrumentationData = {};
	
	// Used to identify which page the event occurred when sending an instrumentation event
	var instrumentationPage = "application.unknown.";
	
	function setDefaultInstrumentationData(newData) {
		defaultInstrumentationData = newData;
	}

	function setDefaultInstrumentationPage(page) {
		if (page.endsWith(".")==false) {
			page = page + ".";
		}
		
		instrumentationPage = page;
	}

	function sendInstrumentationEvent(eventName, eventDataJSON) {
		var eventData = JSON.parse(JSON.stringify(defaultInstrumentationData));
		
		if (typeof eventDataJSON != "undefined") {
			for (var key in eventDataJSON) {
			    if (eventDataJSON.hasOwnProperty(key)) {
			        eventData[key] = eventDataJSON[key];
			    }
			}			
		}	
	    LASLogger.instrEvent(instrumentationPage+eventName, eventData);
	}
	
	function sendResultLogEvent(dataURL, dataSourceUUID,name) {
		var dataJSON = { 
				documentUUID:  dataSourceUUID,
	            documentURL:   dataURL};
		sendInstrumentationEvent("result."+name,dataJSON);	
		
	}
	

	function checkCreateIndexStatus(documentArea, documentIndex) {
		var indexStatus = false;

		$.ajax({	
			url: openke.global.Common.getRestURLPrefix()+"/documentIndex/"+documentArea+"/"+documentIndex+"/status",
			type:"GET",
			contentType: "application/json; charset=utf-8",
			dataType : "text JSON",
			success: function(data) {
				indexStatus = data['status']
				
				if (indexStatus == "True") {	
					$('#btShowIndex').show()
					$('#btCreateIndex').replaceWith('<a style="cursor:default" id="btCreateIndex">Re-create</a>')		
					$('#btCreateIndex').click(createIndex);
					clearInterval(indexStatusTimer)
				}
			}
		})
	}

	
	/**
	 * Checks if the index already exists for the session.  If so, change links to let user visit & re-create
	 * @returns
	 */
	function checkIndexExists(documentArea, documentIndex) {
		$.ajax({	
			url : openke.global.Common.getRestURLPrefix()+"/documentIndex/"+documentArea+"/"+documentIndex,
			type:"HEAD",
			contentType: "application/json; charset=utf-8",
			dataType : "text JSON",
			success: function() {
					$('#btShowIndex').show()
					$('#btCreateIndex').text("Re-create")
			}
		})
	}


	function createIndex(documentArea, documentIndexID, query, title, maxNumResults, checkCreationStatus=true) {
		
		if (checkCreationStatus) {
			$('#btCreateIndex').replaceWith("<span id='btCreateIndex'>creating</span>")
		}
		var queryData = {
				documentArea: documentArea,
			documentIndexID : documentIndexID,
			title : title,
			query : query,
			maxNumResults : maxNumResults
		}

		$.ajax({
			url:openke.global.Common.getRestURLPrefix()+"/documentIndex",
			type:"POST",
			data:JSON.stringify(queryData),
			contentType: "application/json; charset=utf-8",
			dataType : "text JSON",
			success: function(data) {
				var documentArea    = data.documentArea;
				var documentIndexID = data.documentIndexID; 
				
				if (checkCreationStatus) {
					indexStatusTimer = setInterval(function(){ checkCreateIndexStatus(documentArea, documentIndexID) }, 5000);
					bootbox.alert ("Index creation in progress.");
				}
			}
		})
	}

	//creates a new window and calls the the application to bring the ViewIndexer in that page
	function showIndexView(area, documentIndexID) {
		window.open(openke.global.Common.getPageURLPrefix()+"/viewIndex?documentArea="+area+"&documentIndexID="+documentIndexID)
	}	
	
	
	return {
		setDefaultInstrumentationData : setDefaultInstrumentationData, 
		setDefaultInstrumentationPage : setDefaultInstrumentationPage,

		createIndex            : createIndex,
		checkCreateIndexStatus : checkCreateIndexStatus,
		checkIndexExists       : checkIndexExists, 
		showIndexView          : showIndexView
		
	};
}());