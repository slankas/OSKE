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
 */
module.exports = function () {
	"use strict";
	
	var LEVEL_TRACE = 1,
		LEVEL_DEBUG = 3,
		LEVEL_INFO  = 5,
		LEVEL_INSTR = 6,
		LEVEL_WARN  = 7,
		LEVEL_ERROR = 9,
		LEVEL_FATAL = 11;

	var currentLevel = LEVEL_ERROR;

	/** sets a new logging level.  Any messages passed at this level or higher will be printed to the console */
	function setCurrentLevel(newLoggingLevel) {
		currentLevel = newLoggingLevel;
	}

	/** returns the current logging level */
	function getCurrentLevel() {
		return currentLevel;
	}

	/** prints the message to the log if the current logging level is greater than or equal to the passed in level */
	function log(level, message) {
		if (level >= currentLevel) {
			console.log(new Date().toISOString() + "(" + level + "): " + message);
		}
	}
	
	/** prints the object to the log (with no timestamp) if the current logging level is greater than or equal to the passed in level */
	function logObject(level, objectValue) {
		if (level >= currentLevel) {
			console.log(objectValue);
		}
	}
	
	var noop = function(data) {return true;};
	
	/** passing a callbackAction is necessary when the user will immediately leave a page - action is otherwise cancelled with instrumentation event not being sent.*/
	function instrEvent(evtDesc, evtInfo, callbackAction) {
		if (typeof evtDesc === "undefined") {	return; }
		if (typeof callbackAction === "undefined") { callbackAction = noop; }

		
		var data = {};
		data.evtDesc = evtDesc;
		
		if (typeof evtInfo !== "undefined") {data.evtInfo = evtInfo; }
		
		var date = new Date();
		data.evtTime = date.getTime();
		


		$.post(openke.global.Common.getRestURLPrefix()+"/instrumentation/event", {"data":JSON.stringify(data)}, callbackAction);
	}

	var privateMembers = {
		currentLevel : currentLevel
	};

	return {
		LEVEL_TRACE : LEVEL_TRACE,
		LEVEL_DEBUG : LEVEL_DEBUG,
		LEVEL_INFO  : LEVEL_INFO,
		LEVEL_INSTR : LEVEL_INSTR,
		LEVEL_WARN  : LEVEL_WARN,
		LEVEL_ERROR : LEVEL_ERROR,
		LEVEL_FATAL : LEVEL_FATAL,
		getCurrentLevel : getCurrentLevel,
		setCurrentLevel : setCurrentLevel,
		log : log,
		logObject : logObject,
		instrEvent : instrEvent,
		privateMembers : privateMembers
	};
}();
