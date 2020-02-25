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
var LASLogger = (function () {
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

	/**
	 * Call once at beginning to ensure your app can safely call console.log() and
	 * console.dir(), even on browsers that don't support it.  You may not get useful
	 * logging on those browers, but at least you won't generate errors.
	 * 
	 * source: http://stackoverflow.com/questions/690251/what-happened-to-console-log-in-ie8
	 * 
	 * @param  alertFallback - if 'true', all logs become alerts, if necessary. 
	 *   (not suitable for production)
	 */
	function fixConsole(alertFallback) {
		if (typeof console === "undefined") {
			console = {}; // define it if it doesn't exist already
		}
		if (typeof console.log === "undefined") {
			if (alertFallback) { console.log = function (msg) { alert(msg); }; }
			else { console.log = function () {}; }
		}
		if (typeof console.dir === "undefined") {
			if (alertFallback) {
				// THIS COULD BE IMPROVED� maybe list all the object properties?
				console.dir = function (obj) { alert("DIR: " + obj); };
			}
			else { console.dir = function () {}; }
		}
	}

	fixConsole(false);  //change this to true for development in browsers without a console

	var privateMembers = {
		fixConsole : fixConsole,
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
}());