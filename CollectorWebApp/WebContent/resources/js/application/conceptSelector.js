/**
 * Meant to provide a generic window to drill-down into a concept hierarch through
 * a pie-chart.  
 * 
 * If the user "shift-clicks" on a wedge, then the function "conceptShiftSearchCalled" in 
 * the calling window is called.
 * 
 */

var pageConceptChart

$(document).ready(function() {
	LASLogger.instrEvent('application.conceptSelector.open', {});
	
	$('#btResetConcepts').click(function(event) { 
		LASLogger.instrEvent('application.conceptSelector.reset', {});
		pageConceptChart.resetToTop();
	});
	
	$('#btClose').click(function(event) { 
		LASLogger.instrEvent('application.conceptSelector.close', {});
		window.close();
	});
	
	pageConceptChart  = new ConceptChart("conceptPie", "normal", "", null, null, csCallback)
	
});

function csCallback(searchObject) {
	if (typeof window.opener.conceptShiftSearchCalled === "function") { 
		window.opener.conceptShiftSearchCalled(searchObject);
		LASLogger.instrEvent('application.conceptSelector.shiftSearch', searchObject);
	}
	window.close();
}