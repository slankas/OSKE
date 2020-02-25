/**
 *
 * Create date - 20180820
 * Description:Provides CRUD access to "documentBuckets"
 * 
 *              All functions (except setDefault*) are instrumented (need to initialize the currentPage)
 *              
 * Dependencies:
 *   jQuery
 *   LASLogger
 * 
 * Usage:
 *    include script
 *    
 *    On the page initialization code, call (these are examples only - change the page name and default data appropriately)
 *        openke.model.ProjectDocument.setDefaultInstrumentationPage("application.pageName.");
 */

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.model == 'undefined') {	openke.model = {}  }
openke.model.DocumentBucket = (function () {
	"use strict";
 
	var userDocumentBuckets;     // Json array containing documentBuckets for currently logged in user
	var userDocumentBucketsByID; //  indexing the documentBuckets by their ID
	
	// For instrumentation events, this will the be basic information sent to the service
	var defaultInstrumentationData = {};
	
	// Used to identify which page the event occurred when sending an instrumentation event
	var instrumentationPage = "application.unknown.";
		
	function setDefaultInstrumentationData(newData) {
		defaultInstrumentationData = newData;
	}

	function setDefaultInstrumentationPage(page) {
		if (instrumentationPage == "application.unknown.") {
			if (page.endsWith(".")==false) {
				page = page + ".";
			}
			
			instrumentationPage = page;
		}
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
		
	// loads all of the available documents.  callback is optional and can be used to call another function/continue processing after the data is loaded
	function loadAll(callback) {
	    jQuery.ajax({
	    	url:  openke.global.Common.getRestURLPrefix()+"/documentbucket",
	        success: function(data) {
	        	userDocumentBuckets = data;
	        	userDocumentBucketsByID = {}
	        	for (var j = 0; j < userDocumentBuckets.length; j++) {
	        		userDocumentBucketsByID[userDocumentBuckets[j].id] = userDocumentBuckets[j];
	    	    }
	        	if (typeof callback !== "undefined") {
	        		callback();
	        	}
	        }
	    });
	}
	
	function getDocumentBucket(bucketUUID) {
		return userDocumentBucketsByID[bucketUUID];
	}

    // Does the passed in tag already exist
	function doesTagExist(tagNameToCheck) {
        for (var j = 0; j < userDocumentBuckets.length; j++) {
            if (userDocumentBuckets[j].tag===tagNameToCheck){
            	return true;
            }
        }
        return false;
	}
	
	// returns all of the document buckets.  
	function getDocumentBuckets() {
		return userDocumentBuckets;
	}
	
	function removeDocumentFromBucket(documentBucketID, documentID,callback) {
		var dataObject = {
			documentBucketUUID : documentBucketID,
			documentUUID: documentID
		}
		
		sendInstrumentationEvent("removeDocumentFromBucket", dataObject);

		var url = openke.global.Common.getRestURLPrefix()+"/documentbucket/"+documentBucketID+"/document/"+documentID;
	    $.ajax({
			type : "DELETE",
			url : url,
			success : function(data) {
				callback(dataObject);
			},
			error : function(data) {
				//TODO - provide better messages.  probably need to fix return codes and use JSON mwssages
				alert("error");
			},
			dataType : "text",
		});	
		return false;
	}
	return {
		setDefaultInstrumentationData : setDefaultInstrumentationData, 
		setDefaultInstrumentationPage : setDefaultInstrumentationPage,
		
		loadAll : loadAll,
		getDocumentBuckets : getDocumentBuckets,
		getDocumentBucket : getDocumentBucket,
		doesTagExist : doesTagExist,
		removeDocumentFromBucket : removeDocumentFromBucket
	};
}());