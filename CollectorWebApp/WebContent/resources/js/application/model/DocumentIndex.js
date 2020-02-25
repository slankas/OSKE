/**
 * 
 * Create date - 20170424
 * Description: Used to interact with the various document indexes that are possible.
 * 
 * Dependencies:
 *   jQuery
 *   LASLogger
 * 
 * Usage:
 *    include script
 *    
 *    
 *    Note: as functions perform an ajax call, we need a callback method for the results back to the calling routines
 *   
 */

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.model == 'undefined') {	openke.model = {}  }

"use strict";

/**
 * indexID - uuid for the index.  this can be a discovery session id, a specific execution, collection, or a user-created one from a search
 * currentPage - string representing the current web page that the user is on.  Used for 
 */
openke.model.DocumentIndex = function(indexID, documentArea, currentPage) {
	this.uuid = indexID;
	this.documentArea = documentArea
	
	if (!currentPage.endsWith(".")) { currentPage = currentPage + "."; }
	this.instrumentationPage = currentPage;   // Used to identify which page the event occurred when sending an instrumentation event
	
	this.indexStatusTimer = null;             // checks the status of index creation on a fixed basis (e.g., 5 seconds)

}

openke.model.DocumentIndex.prototype.sendInstrumentationEvent = function(eventName, eventData) {
	    LASLogger.instrEvent(instrumentationPage+eventName, eventData); 
	}
	
	

/**
 * Checks if the index already exists for the session.  
 * 
 * @returns callback is a function that will be called with a single, boolean value
 */
openke.model.DocumentIndex.prototype.exists = function(callback) {
	$.ajax({	
		url : openke.global.Common.getRestURLPrefix()+"/documentIndex/"+this.documentArea+"/"+this.uuid,
		type:"HEAD",
		contentType: "application/json; charset=utf-8",
		dataType : "text JSON",
		success: function() { callback(true);  },
		error:   function() { callback(false); }
	});
}

/**
 * Called internally when an index create request is submitted - done primary through an interval call
 * When the index is ready, the callback method will be called with a two parameters: UUID and document area.  the timer is also cleared.
 *  
 * @param callback
 * @returns
 */
openke.model.DocumentIndex.prototype.checkCreationStatus = function(callback) {
	var that = this;
	$.ajax({	
		url: openke.global.Common.getRestURLPrefix()+"/documentIndex/"+this.documentArea+"/"+this.uuid+"/status",
		type:"GET",
		contentType: "application/json; charset=utf-8",
		dataType : "text JSON",
		success: function(data) {
			indexStatus = data['status']
			if (indexStatus == "True") {	
				clearInterval(that.indexStatusTimer);
				callback(that.uuid, that.documentArea);
			}
		}
	})
}

/**
 * @param query
 * @param title
 * @param maxNumResults
 * @param callback - What function should be called when the index has been created?  If not set, no check will be made.  
 *                   Once the index has been created the callback method will be called with the UUID and document area.
 */
openke.model.DocumentIndex.prototype.createIndex = function(query, title, maxNumResults, callback) {
	var that = this;
	var queryData = {
		documentArea: this.documentArea,
		documentIndexID : this.uuid,
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
			LASLogger.log(LASLogger.LEVEL_INFO,"Initiated creation of index")
			
			if (callback != null) {
				that.indexStatusTimer = setInterval(function(){ that.checkCreationStatus(callback) }, 5000);
			}
		}
	})
}

/**
 * creates a new window and calls the the application to bring the ViewIndexer in that page
 */
openke.model.DocumentIndex.prototype.showIndexView = function() {
	window.open(openke.global.Common.getPageURLPrefix()+"/viewIndex?documentArea="+area+"&documentIndexID="+documentIndexID)
}	
