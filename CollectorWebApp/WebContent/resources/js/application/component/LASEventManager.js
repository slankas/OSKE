/**
 * Create date - 20170306
 * Description: Implement the publish subscribe model
 * This class provides the capability for an object to register for an event
 * When the event occurs the sendEvent is called which results in action on the object.
 * 
 */

var LASEventManager = (function () {
	"use strict";

	var eventCallBacks = {};

	/**
	 * Register a specific function to be called whenever a certain event occurs.
	 * 
	 * @param eventType - value of LASEventType, specific event/channel to listen for.
	 * @param callbackFunction - what function to call when event occurs
	 */
	function registerForEvent(eventType, callbackFunction) {
		if ('undefined' === typeof(eventType)) {
			LASLogger.log(LASLogger.LEVEL_FATAL,"LASEventManager.registerForEvent: registered undefined event for function" + callbackFunction.name);
			throw "Undefined event registration.";
		}
		
		var currentEventList = eventCallBacks[eventType];
		if (callbackFunction && callbackFunction !== '') {
			if ('undefined' === typeof currentEventList) {
				currentEventList = [];
				eventCallBacks[eventType] = currentEventList;
			}
			currentEventList.push(callbackFunction);
		}
	}

	/**
	 * Send an event to all listeners(functions)
	 * @param LASEventType this is the event type
	 * @param eventObject extra information passed about the event
	 */
	function sendEvent(eventType, eventObject) {
		
		LASLogger.log(LASLogger.LEVEL_TRACE,"sendEvent start: "+eventType);
		
		var currentEventList = eventCallBacks[eventType],
		    i = 0,
		    func = null;

		if ('undefined' !== typeof currentEventList) {
			for (i = 0; i < currentEventList.length; i++) {
				func = currentEventList[i];
				LASLogger.log(LASLogger.LEVEL_TRACE,"sendEvent calling method "+func.name);		
				func(eventType, eventObject);
				LASLogger.log(LASLogger.LEVEL_TRACE,"sendEvent return from method "+func.name);
			}
		}

		LASLogger.log(LASLogger.LEVEL_TRACE,"sendEvent complete: "+eventType);
	}

	/**
	 * Stop the callback function from responding to the specified event.
	 * @param eventType this is the event type
	 * @param callbackFunction this is the callback function name that will be called when the event occurs
	 */
	function deregisterFromEvent(eventType, callbackFunction) {
		var currentEventList = eventCallBacks[eventType],
		    i = 0;

		if (callbackFunction && callbackFunction !== '') {
			if ('undefined' !== typeof currentEventList) {
				for (i = 0; i < currentEventList.length; i++) {
					if (currentEventList[i] === callbackFunction) {
						currentEventList.splice(i, 1);
						i--; // need to recheck this index.
					}
				}
			}
		}
	}

	return {
		registerForEvent : registerForEvent,
		sendEvent : sendEvent,
		deregisterFromEvent : deregisterFromEvent
	};
}());