/**
 * 
 * Create date - 20180820
 * Description: Provides CRUD access to "documents" / "scratchpads"
 *              as well as the ability to append notes and images to existing scratchpads
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
openke.model.ProjectDocument = (function () {
	"use strict";
 
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
	
	function getScratchPadDoc(documentUUID, callback) {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/document/"+documentUUID+"/scratchpadDoc", callback);
	}
	
	function loadAvailableDocuments(callback) {
		sendInstrumentationEvent("loadDocumentList", {})
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/projectdocument/", callback);	
	}
	
	function loadDocument(documentUUID, callback) {
		var eventObj = { documentUUID: documentUUID };
		sendInstrumentationEvent("loadDocumentList", eventObj)
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/projectdocument/" + documentUUID, callback);
	}
	
	function getExportURL(documentUUID) {
		return openke.global.Common.getRestURLPrefix()+"/projectdocument/" + documentUUID+"/export";
	}
	
	function createDocument(docObj,callback,errorCallback) {		
		var eventObj = { documentUUID: documentUUID,  name: name};
		sendInstrumentationEvent("updateDocument", eventObj);
		
		var url = openke.global.Common.getRestURLPrefix()+"/projectdocument"
		
		$.ajax({
				type : "POST",
				url : url,
				data: JSON.stringify(docObj),
			    contentType: "application/json; charset=utf-8",
				success : function(data) {
					if (data.status == "success") {
						callback(data);
					}
					else {
						LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to updateDocument: " + JSON.stringify(data.message))
						LASLogger.logObject(LASLogger.LEVEL_ERROR, docObj)
						errorCallback();
					}
				},
				error : function(data) {
					LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to updateDocument: " + JSON.stringify(data))
					LASLogger.logObject(LASLogger.LEVEL_ERROR, docObj)
					errorCallback();				},
			});
		
	}
	
	/**
	 * docObj should contain name, contents, and status
	 */
	function updateDocument(documentUUID, docObj,callback,errorCallback) {
		var eventObj = { documentUUID: documentUUID,
				         name: name};
		sendInstrumentationEvent("updateDocument", eventObj)

		var url = openke.global.Common.getRestURLPrefix()+"/projectdocument/" +documentUUID 
			
	    $.ajax({
			type : "PUT",
			url : url,
			data: JSON.stringify(docObj),
		    contentType: "application/json; charset=utf-8",
			success : function(data) {
				if (data.status == "success") {
					callback(data);
				}
				else {
					LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to updateDocument: " + JSON.stringify(data.message))
					LASLogger.logObject(LASLogger.LEVEL_ERROR, docObj)
					errorCallback();
				}
			},
			error : function(data) {
				LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to updateDocument: " + JSON.stringify(data))
				LASLogger.logObject(LASLogger.LEVEL_ERROR, docObj)
				errorCallback();					
				}
		});		
	}
	
	function deleteDocument(documentUUID, callback, errorCallback) {
    	var eventObj = {	documentUUID: documentUUID        	}
    	sendInstrumentationEvent("deleteDocument", eventObj)

        $.ajax({
    		type : "DELETE",
    		url : openke.global.Common.getRestURLPrefix()+"/projectdocument/"+documentUUID,
    	    contentType: "application/json; charset=utf-8",
    		success : function(data) {
    			if (data.status == "success") {
    				callback()
    			}
    			else {
					LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to append note: " + JSON.stringify(data.message))
					LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
					errorCallback();
    			}
    		},
    		error : function(data) {
				LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to append note: " + JSON.stringify(data))
				LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
				errorCallback();
    		}
        });
		
	}
	
	function updateDocumentStatus(documentUUID, status, callback, errorCallback) {
		var eventObj = { documentUUID: documentUUID, status: status 	}
    	sendInstrumentationEvent("updateDocumentStatus", eventObj)
		
		var statusObj = {
				status : status
		}
		
	    $.ajax({
			type : "PUT",
			url : openke.global.Common.getRestURLPrefix()+"/projectdocument/"+documentUUID+"/status",
			data: JSON.stringify(statusObj),
		    contentType: "application/json; charset=utf-8",
			success : function(data) {
				if (data.status == "success") {
					callback()
				}
				else {
					LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to update status: " + JSON.stringify(data.message))
					LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
					errorCallback();
					
				}
			},
			error : function(data) {
				LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to update status: " + JSON.stringify(data.message))
				LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
				errorCallback();
				
			},
		});
	}

	function appendSourceTextAndNote(sourceText, noteText, sourceUUID, sourceTitle, sourceURL, sourceCrawledDate, callback, errorCallback) {
		var documentFragment = sourceText.trim();
		
		if (noteText.trim() != '') {
    		if (documentFragment != '') {
    			documentFragment = documentFragment +"<br>"
    		}
    		documentFragment = documentFragment + "<i>Note:</i> " + noteText.trim()
		}
		if (documentFragment == '') {
			LASLogger.log(LASLogger.LEVEL_INFO, "No source text or notes passed to append, not appending")
			return;
		}
		
    	var reference = "URL: "+ sourceURL
    	if (sourceURL != sourceTitle) { sourceText = "Title: " + sourceTitle + ", " + reference}
    	reference = reference +", Crawled Date: "+sourceCrawledDate
    	
    	var appendObj = { 
				content : documentFragment + "<br><small style='color: #778899;'>"+reference+"</small>",
				source : sourceUUID
    	}
    	var eventObj = {
        		sourceText : sourceText, 
        		noteText : noteText, 
        		sourceUUID: sourceUUID,
        		sourceTitle: sourceTitle,
        		sourceURL: sourceURL,
        		sourceCrawledDate: sourceCrawledDate
        	}
    	sendInstrumentationEvent("appendNote", eventObj)

		$.ajax({
			type : "PUT",
			url : openke.global.Common.getRestURLPrefix()+"/projectdocument/"+LASHeader.getCurrrentScratchpadUUID()+"/append",
			data: JSON.stringify(appendObj),
		    contentType: "application/json; charset=utf-8",
			success : function(data) {
				if (data.status == "success") {
					callback();
				}
				else {
					LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to append note: " + JSON.stringify(data.message))
					LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
					errorCallback();
				}
			},
			error : function(data) {
				LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to append note: " + JSON.stringify(data))
				LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
				errorCallback();
			},
		});
	}
	
	function appendImage(imageData, imageReferenceText, callback, errorCallback) {
		var eventObj = {
        		referenceText : imageReferenceText 
        }
    	sendInstrumentationEvent("appendImage", eventObj)
		var appendObj = {
				content : '<img src="'+imageData+'">',
				source  : imageReferenceText 
			  }
				
		$.ajax({
			type : "PUT",
			url : openke.global.Common.getRestURLPrefix()+"/projectdocument/"+LASHeader.getCurrrentScratchpadUUID()+"/append",
			data: JSON.stringify(appendObj),
		    contentType: "application/json; charset=utf-8",
			success : function(data) {
				if (data.status == "success") {
					callback();
				} else {
					LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to append note: " + JSON.stringify(data.message))
					LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
					errorCallback();
				}},
			error: function(data) {
				LASLogger.log(LASLogger.LEVEL_ERROR, "Unable to append note: " + JSON.stringify(data))
				LASLogger.logObject(LASLogger.LEVEL_ERROR, eventObj)
				errorCallback();
			}
		});
	}
	
	return {
		setDefaultInstrumentationData : setDefaultInstrumentationData, 
		setDefaultInstrumentationPage : setDefaultInstrumentationPage,
		
		loadAvailableDocuments : loadAvailableDocuments,
		loadDocument : loadDocument, 
		createDocument: createDocument,
		updateDocument: updateDocument,
		deleteDocument : deleteDocument,
		updateDocumentStatus : updateDocumentStatus,
		appendSourceTextAndNote : appendSourceTextAndNote,
		appendImage : appendImage,
		getExportURL : getExportURL
	};
}());