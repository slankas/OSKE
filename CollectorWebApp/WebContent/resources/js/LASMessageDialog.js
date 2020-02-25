/**
 * Allows the application to print messages to the javascript console at various defined levels.
 * 
 * Levels taken from Log4J: http://logging.apache.org/log4j/1.2/manual.html
 * 
 * Use Guidance:
 *   TRACE - for entering/exiting methods
 *   DEBUG - printing contents of objects
 *   WARN  - general errors, such as bad responsed from ajax calls
 *   ERROR - an error occurred that was unexpected in the application, but it can recover
 *   FATAL - an error occured that the application can not recover from
 *                      
 */
var LASMessageDialog = (function () {
	"use strict";
	

	/** returns the current logging level */
	function showMessage(title, content) {
		$('#myModalTitle').text('Export')
		$('#myModalDialogText').text('You will receive an email with further instructions to access your export.')
		$('#myModal').modal('show')
	}



	var privateMembers = {
		
	};

	return {
		showMessage : showMessage,
		privateMembers : privateMembers
	};
}());