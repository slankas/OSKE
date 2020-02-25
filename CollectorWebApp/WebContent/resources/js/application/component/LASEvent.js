/**
 * Description: Stores information about the event.  For all event types, the sourceObject must be defined.
 *
 * Create date - 20170306
 * 
 * 		var eventObject = new LASEvent(objectSource);

		eventObject.fieldChanged = "normalization";
		eventObject.fieldValue   = $("input[name=optionNormalizationDisplayType]:checked").val();
		eventObject.data = getDataForVisualizationState();

		LASEventManager.sendEvent(LASEventType.VISUALIZATION_OPTION_CHANGE, eventObject);
 * 
 * 
 * 
 */


/**
 * Constructor
 * sourceObj The source object that generates the event
 */

function LASEvent(sourceObj) {
	"use strict";
	this.source = sourceObj;
}


var LASEventType = (function () {
	"use strict";

	var WARNING_EVENT = "WARNING_EVENT",
	    TAB_SELECTED  = "TAB_SELECTED",
	    FILTER_CHANGE = "FILTER_CHANGE",
	    ALERT_DATA_LOAD_START = "ALERT_DATA_LOAD_START",
	    ALERT_DATA_LOAD_END   = "ALERT_DATA_LOAD_END",
	    VISUALIZATION_OPTION_CHANGE = "VISUALIZATION_OPTION_CHANGE",
	    MAP_OPTION_CHANGE = "MAP_OPTION_CHANGE",
	    LISTINGS_OPTION_CHANGE = "LISTINGS_OPTION_CHANGE",
	    ALERT_SELECTED = "ALERT_SELECTED",
		GRAPH_TYPE_CHANGE = "GRAPH_TYPE_CHANGE",
		REPORT_TYPE_CHANGE = "REPORT_TYPE_CHANGE",
	    ALERT_LISTINGS_DIALOG_OPEN = "ALERT_LISTINGS_DIALOG_OPEN",
	    ALERT_LISTINGS_DIALOG_CLOSE = "ALERT_LISTINGS_DIALOG_CLOSE",
	    MAP_BOUNDARY_CHANGE = "MAP_BOUNDARY_CHANGE",
	    WINDOW_RESIZE = "WINDOW_RESIZE",
		RESTORE_APPLICATION = "RESTORE_APPLICATION",
		RESTORE_APPLICATION_POST_ALERT_LOAD = "RESTORE_APPLICATION_POST_ALERT_LOAD",
		ANALYTICS_TRACK_EVENT = "ANALYTICS_TRACK_EVENT";

	return {
		WARNING_EVENT : WARNING_EVENT,
		TAB_SELECTED  : TAB_SELECTED,
		FILTER_CHANGE : FILTER_CHANGE,
		ALERT_DATA_LOAD_START : ALERT_DATA_LOAD_START,
		ALERT_DATA_LOAD_END   : ALERT_DATA_LOAD_END,
		VISUALIZATION_OPTION_CHANGE : VISUALIZATION_OPTION_CHANGE,
		MAP_OPTION_CHANGE : MAP_OPTION_CHANGE,
		LISTINGS_OPTION_CHANGE : LISTINGS_OPTION_CHANGE,
		ALERT_SELECTED : ALERT_SELECTED,
		GRAPH_TYPE_CHANGE : GRAPH_TYPE_CHANGE,
		REPORT_TYPE_CHANGE : REPORT_TYPE_CHANGE,
		ALERT_LISTINGS_DIALOG_OPEN : ALERT_LISTINGS_DIALOG_OPEN,
		ALERT_LISTINGS_DIALOG_CLOSE : ALERT_LISTINGS_DIALOG_CLOSE,
		MAP_BOUNDARY_CHANGE : MAP_BOUNDARY_CHANGE,
		WINDOW_RESIZE : WINDOW_RESIZE,
		RESTORE_APPLICATION: RESTORE_APPLICATION,
		RESTORE_APPLICATION_POST_ALERT_LOAD: RESTORE_APPLICATION_POST_ALERT_LOAD,
		ANALYTICS_TRACK_EVENT: ANALYTICS_TRACK_EVENT
	};
}());



//Following declarations can be removed once implemented in the classes
//Parameters for all the events.

/*
LASEventType.TAB_SELECTED
   selectedTab - name of the tab that was selected
   
LASEventType.MAP_BOUNDARY_CHANGE
   northLatitude - number (real) representing the northernmost/top latitude of the map display
   eastLongitude - number (real) representing the easternmost longitude of the map display
   westLongitude - number (real) representing the western most longitude of the map display
   southLatitude - number (real) representing the southern most longitude of the map display
   
LASEventType.FILTER_CHANGE //on checkbox-select/unselect
   alertData - array of the data that has been filtered
   
   
LASEventType.ALERT_DATA_LOAD_START //on checkbox-select/unselect
   startDate
   EndDate
   
LASEventType.ALERT_DATA_LOAD_END //on checkbox-select/unselect
	alertData - array of data that has been filtered (from the current filter applied)

LASEventType.VISUALIZATION_OPTION_CHANGE
    fieldChanged:  "normalization","cluster","convex"
    fieldValue: "absolute","population"     true,false - for cluster and true
    data: { normalization: value, cluster: value, convex: value }

LASEventType.MAP_OPTIONS_CHANGE
	fieldChanged:  "cluster", "position"
	fieldValue: cluster: count, clinical_issues, none 
	            position: true/false
 	data: {cluster: value, position: value }
 

LASEventType.LISTINGS_OPTION_CHANGE
	fieldChanged:  "dialog","boundary"
	fieldValue: true / false
    data: { dialog: value, boundary: value }
	
LASEventType.ALERT_SELECTED
	id : which ID was selected
	zipCode : what was the zip code of the selected ID,  this is needed for zip 3 regions.
	ansiStateCode : what was the ansi state code associated with the alert.  used to create the alert region ID

LASEventType.GRAPH_TYPE_CHANGE
	fieldChanged
	fieldValue: barSeries, lineSeries, percentages
	
	data: { graphType: barSeries, lineSeries, percentages,
	        categorization: date, eventType, source, method, level
	      }
	
LASEventType.REPORT_TYPE_CHANGE
    selectedReport: daily,detail	
	
LASEventType.ALERT_LISTINGS_DIALOG_OPEN
	No properties

LASEventType.WINDOW_RESIZE - called when the window is resized.
	No properties
	
LASEventType.RESTORE_APPLICATION - this is called on click of restore from the snaphshot dialog.
Also if the url has a field - state=<stateHashValue>

	restoreFromObject - JSON object that is used to restore the application. Indexed by components

LASEventType.RESTORE_APPLICATION_POST_ALERT_LOAD
	After the alert data has been loaded and all event handlers from ALERT_DATA_LOAD_END have completed, this
	event fires for any components to update themselves given the alert data now present.
	restoreFromObject - JSON object that is used to restore the application.
	
LASEventType.ANALYTICS_TRACK_EVENT
	Tracks all the analytics events and executes a _gaq.push to report a particular event to analytics engine.
	eventObject has the following :
	category - Category of the event.
	action - Action performed.
	label - Label of the event.
	
*/