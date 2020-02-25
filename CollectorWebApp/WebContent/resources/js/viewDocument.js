/**
 * 
 * 
 * Dependencies:
 *   jQuery
 * 
 * Usage:
 * 
 * Notes:

 */

var ViewDocument = (function () {
	"use strict";
 
	var documentObject; 	
		
	/**
	 * Initializes the component
	 * Sets up the generic document analytic menus
	 * Initializes any HTML components (e.g., sliders) not dependent on data
	 * Defines the event handlers for the various HTML components 
	 * Begins the process to load data
	 * 
	 */
	function initialize() {		
		OKAnalyticsManager.defineStandardDocumentMenu("application.viewDocument.", false,true); 
		openke.model.DocumentBucket.setDefaultInstrumentationPage('application.viewDocument');
		openke.view.DocumentBucketSupport.setDefaultInstrumentationPage('application.viewDocument');
	}
		
    var displayBlock = "#textBlock";
	
	function displayText(docRecord, additionalData) {
		documentObject = docRecord;
		
		var url  = docRecord.url;
		var uuid = docRecord.source_uuid;
		var title = docRecord.url;
		if (docRecord.hasOwnProperty("html_title")) { title = docRecord.html_title; }				
					
					
		var rec = new openke.component.ResultObject(uuid, title, url, "", docRecord, true, true);	
		$(displayBlock).html("<table style='width:100%'></table>");
		$(displayBlock + ' table').append(rec.getRecordDOM());
		rec.displayRecord();
		var domMenu = OKAnalyticsManager.produceObjectAnalyticsMenu(uuid, "", docRecord ,url, additionalData,rec);  //note, not all of these need to be defined.  The called analytic will check
		var collectionDOM = openke.view.DocumentBucketSupport.createCollectionSelect(docRecord.source_uuid,rec)

		rec.displayMenu(domMenu);
		rec.appendToMenu(collectionDOM);
		if (typeof(docRecord.user_collection) != 'undefined') {
			openke.view.DocumentBucketSupport.populateResultObject(rec,docRecord.user_collection)
		}

		rec.establishFullTextToggle("",true);
					
	}	
			
	
	return {
		initialize  : initialize,
		displayText : displayText
	};
}());



$(document).ready(function() {
	ViewDocument.initialize();
	openke.model.DocumentBucket.loadAll(function() {
		var currentDocument = opener.RecordLevelAnalytics.getDocumentToExpand();
		var additionalData  = opener.RecordLevelAnalytics.getAdditionalDataForExpandedDocument();
		ViewDocument.displayText(currentDocument, additionalData);		
	});
})



