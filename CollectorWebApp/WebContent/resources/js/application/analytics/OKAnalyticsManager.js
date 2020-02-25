/**
 * 
 * Create date - 20170417
 * Description: Manages the analytics and produces an appropriate menu that can be called for either a collection
 *              of documents, or a single document.
 *              
 * Usage:
 *   First, the analytics need to be registered with the system...
 *   
 *   To create a specific menu bar for a record:
			var additionalData = {
				domain : openke.global.Common.getDomain(), 
				searchSession: sessionId,
				execution: enumber,
				title: resultRow.title,
				storageArea : "sandbox",
				type : "_doc"
			}
 *   
 *   var domCode = OKAnalyticsManager.produceObjectAnalyticsMenu(uuidValue, urlForFullRecord, fullJSONRecord, sourceURL, additionalData);  //note, not all of these need to be defined.  The called analytic will check
 *   $('#myTest').append(domCode);
 * 
 */

var OKAnalyticsManager = (function () {
	"use strict";

	var collectionAnalytics = [];	
	var objectAnalytics     = [];
	
	var objectMap = {};  //maintains an index by the unique analytic identifier
	
	var fullPageAnalytics = false; // Are we on a page that has the full-text available.  Possibly will have additional functionaly and menu items

	var standardAnalyticsDefined = false;
	
	/**
	 * Register a
	 * 
	 * @param analyticID - unique identifier to represent an analytic.  string value
	 * @param parent - what is the parent name for this object
	 * @param displayName - 
	 * @param callbackFunction - what function to call when event occurs.  
	 */
	function registerObjectAnalytic(analyticID, parent, displayName, callbackFunction) {

		var item = {
				itemType : "analytic",
				analyticID: analyticID,
				name: displayName,
				callBack: callbackFunction
		}
		if (parent === null) {
			objectAnalytics.push(item);
		}
		else {
			var foundParent = false;
			for (var i =0; i<objectAnalytics.length;i++) {			
				var record = objectAnalytics[i];
				if (record.itemType === "analytic") { continue; }
				if (record.name === parent) {
					foundParent = true;
					record.items.push(item);
					break;
				}
			}
			if (foundParent == false) {
				var items = {
						itemType: "menu",
						name: parent,
						items: [item]
				}
				objectAnalytics.push(items);
			}
		}	
	}
	
	/**
	 * Register a label to be shown on a drop down menu
	 * 
	 * @param parent - what is the parent name for this object
	 * @param displayName - 
	 */
	function registerObjectLabel(parent, displayName) {

		var item = {
				itemType : "label",
				name: displayName
		}
		
		var foundParent = false;
		
		for (var i =0; i<objectAnalytics.length;i++) {			
			var record = objectAnalytics[i];
			if (record.itemType === "analytic") { continue; }
			if (record.name === parent) {
				foundParent = true;
				record.items.push(item);
				break;
			}
		}
		if (foundParent == false) {
			LASLogger.log(LASLogger.LEVEL_FATAL, "unable to find parent for label: "+parent);
		}	
	}	
	
	
	
	// private function used to create the appropriate callback and closure
	function createFunction(callBack,args) {
		return function(event) { 
			event.preventDefault(); 
			callBack(...args); 
			$(this).parent().removeClass("show"); //$(this).parents(".open").find('div.dropdown-menu').removeClass("show"); //.dropdown('toggle');//
			return false;
			};
	} 
	
	/**
	 * this should be called with the arguments that the receiving programming once to send
	 * 
	 * As a general convention, parameters in order should be ...
	 * uuid
	 * full URL to retrieve the document
	 * json object that represents the document
	 * sourceURL what url the document originated at ...
	 * additionalData JSONObject with any other fields that may be necessary
	 * 
	 * @returns a dom-based object with call backs
	 */
	function produceObjectAnalyticsMenu(varArgs) {
		var menuObject = $('<div class="btn-group btn-group-sm objectMenu" role="group" aria-label=""></div>');
		var args = Array.from(arguments);
		
		for (var i=0; i<objectAnalytics.length;i++) {
			//if (i>0) { result = result +"\t"; }
			var item = objectAnalytics[i];
			if (item.itemType == "analytic") {
				var buttonObject = $('<button type="button" class="btn btn-link" style="padding-left:0;">'+item.name+'</button>');
				
				buttonObject.click(createFunction(item.callBack,args));
				menuObject.append(buttonObject);
			}
			else {
				//result += item.name;
				var dropMenu = $('<div class="dropdown"><button class="btn btn-link dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">'+item.name +'</button><div>')
				
				/*
				var dropMenu = $('<div class="btn-group btn-group-sm btn-link" role="group">' +
						         '<button class="btn btn-link dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">' +
						         item.name + '<span class="caret"></span></button></div>');
				
				var dropMenu = $('<div class="btn-group btn-group-sm btn-link" role="group">' +
				         '<button class="btn btn-link dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">' +
				         item.name + '<span class="caret"></span></button></div>');				
				*/
				
				//var unOrderedList = $('<ul class="dropdown-menu" aria-labelledby="dropdownMenu1"></ul>');
				var unOrderedList = $('<div class="dropdown-menu" aria-labelledby="dropdownMenuButton">');
				for (var j=0; j<item.items.length;j++) {
					var subItem = item.items[j];
					
					if (subItem.itemType == "analytic") {
						var listItemAction = $('<a class="dropdown-item" href="">'+subItem.name+'</a>');
						listItemAction.click(createFunction(subItem.callBack,args));
						unOrderedList.append(listItemAction);
					}
					else if (subItem.itemType == "label") {
						var listItem = $('<h6 class="dropdown-header">'+subItem.name+'</h6>');
						unOrderedList.append(listItem);
					}
				}
				dropMenu.append(unOrderedList);
				menuObject.append(dropMenu);
			}
		}
		return menuObject;
	}

	function defineStandardDocumentMenu(instrumentationPage, showExpandMenu = false, fullPageAnalytics=false) {
		if (standardAnalyticsDefined) {
			LASLogger.log(LASLogger.LEVEL_INFO,"Registered object level analytics / menu - already called, skipping");	 

			return false;
		}
		else {
			standardAnalyticsDefined = true;
		}
		
		JobFactory.setDefaultInstrumentationData( {});
		JobFactory.setDefaultInstrumentationPage(instrumentationPage);
		
		// Initialize the record level analytics for this page
		RecordLevelAnalytics.setDefaultInstrumentationData({});
		RecordLevelAnalytics.setDefaultInstrumentationPage(instrumentationPage);
		

		
		// setup the record level analytics menus
		if (showExpandMenu) {
			OKAnalyticsManager.registerObjectAnalytic("a00","Record", "Expand in New Window",RecordLevelAnalytics.showDocumentInNewWindow);
		}
		OKAnalyticsManager.registerObjectAnalytic("a1","Record", "Complete",RecordLevelAnalytics.showFullJSONRecord);
		OKAnalyticsManager.registerObjectAnalytic("a1","Record", "Complete (editor)",RecordLevelAnalytics.showFullJSONRecordEditor);
		OKAnalyticsManager.registerObjectAnalytic("a19","Record", "Translation",RecordLevelAnalytics.showTranslatedSideBySide);
		OKAnalyticsManager.registerObjectAnalytic("a2","Record", "Concepts",RecordLevelAnalytics.showConcepts);
		OKAnalyticsManager.registerObjectAnalytic("a51","Record", "Download Original",RecordLevelAnalytics.downloadOriginalContent);
		OKAnalyticsManager.registerObjectAnalytic("a17","Record", "Geo Tags",RecordLevelAnalytics.showGeoTags);
		OKAnalyticsManager.registerObjectAnalytic("a3","Record", "HTTP Headers",RecordLevelAnalytics.showHeader);
		OKAnalyticsManager.registerObjectAnalytic("a4","Record", "Meta Data",RecordLevelAnalytics.showMetaData);
		OKAnalyticsManager.registerObjectAnalytic("a3","Record", "Outlinks",RecordLevelAnalytics.showOutlinks);
		OKAnalyticsManager.registerObjectAnalytic("a3","Record", "Provenance",RecordLevelAnalytics.showProvenance);
		OKAnalyticsManager.registerObjectAnalytic("a5","Record", "Structured Data (schema.org)",RecordLevelAnalytics.showStructuredData);
		OKAnalyticsManager.registerObjectAnalytic("a52","Record", "Structural Extraction (via CSS)",RecordLevelAnalytics.showStructuralExtraction);
		OKAnalyticsManager.registerObjectAnalytic("a0","Record", "Text",RecordLevelAnalytics.showText);

		if (fullPageAnalytics) {
			OKAnalyticsManager.registerObjectAnalytic("a80","Analytics", "Annotate Resources",RecordLevelAnalytics.annotateResources);
		}
		OKAnalyticsManager.registerObjectAnalytic("a81","Analytics", "Keyphrases",RecordLevelAnalytics.displayKeyphrases);
		OKAnalyticsManager.registerObjectAnalytic("a9","Analytics", "Keywords",RecordLevelAnalytics.displayKeywords);
		OKAnalyticsManager.registerObjectAnalytic("a21","Analytics", "Named Entities",RecordLevelAnalytics.displayNamedEntities);
		OKAnalyticsManager.registerObjectAnalytic("a10","Analytics", "Overview Statistics",RecordLevelAnalytics.viewOverallStatistics);	
		OKAnalyticsManager.registerObjectAnalytic("a20","Analytics", "Relations",RecordLevelAnalytics.displayRelations);
		OKAnalyticsManager.registerObjectAnalytic("a22","Analytics", "Show Domain WhoIs",RecordLevelAnalytics.showDomainWhoIs);
		OKAnalyticsManager.registerObjectAnalytic("a18","Analytics", "Show Word Cloud",RecordLevelAnalytics.showWordCloud);
		OKAnalyticsManager.registerObjectAnalytic("a11","Analytics", "Summarize",RecordLevelAnalytics.summarizeRecord);
		OKAnalyticsManager.registerObjectAnalytic("a12","Analytics", "View Site's robot.txt",RecordLevelAnalytics.viewRobotsFile);
		OKAnalyticsManager.registerObjectLabel("Analytics", "Google Based Analytics");
		OKAnalyticsManager.registerObjectAnalytic("a13","Analytics", "Info",RecordLevelAnalytics.googleInfo);
		OKAnalyticsManager.registerObjectAnalytic("a14","Analytics", "Link to",RecordLevelAnalytics.googleLinkTo);
		OKAnalyticsManager.registerObjectAnalytic("a15","Analytics", "Related",RecordLevelAnalytics.googleRelated);

		OKAnalyticsManager.registerObjectAnalytic("a6",null, "Create Job",JobFactory.startNewJobForRecord);
		OKAnalyticsManager.registerObjectAnalytic("a16",null, "Create Outlinks Job",JobFactory.startNewOutlinksJobForRecord);
		OKAnalyticsManager.registerObjectAnalytic("a7",null, "Create RSS Job",JobFactory.startNewRSSJobForRecord);

		
		LASLogger.log(LASLogger.LEVEL_INFO,"Registered object level analytics / menu");	 
	    		
	}
	
	function useFullPageAnalytics() {
		return fullPageAnalytics;
	}
	
	return {
		registerObjectAnalytic : registerObjectAnalytic,
		registerObjectLabel : registerObjectLabel, 
		produceObjectAnalyticsMenu : produceObjectAnalyticsMenu,
		defineStandardDocumentMenu : defineStandardDocumentMenu,
		useFullPageAnalytics : useFullPageAnalytics
	};
}());


