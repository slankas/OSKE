/* 
    DateSlider.js
    Date Created: 4/24/2017
    
    DateSlider provides additional capabilities on top of nouislider.js (https://refreshless.com/nouislider/)
    to provide a date-based ranger slider with "play capabilities"
    
    The callback function is called with a JSON object containing "startValue" and "endValue".  Both contain
    the slider value in UTC milliseconds.
    
    Usage:
       HTML:  need to add a div element where the DateSlide will be created
              <div id="timeSlider"></div>
       Javascript:
       	    var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
			var playOptions = {
					windowSize : 90,
					windowSizeUnit: "days",
		            stepForwardSize: 30,
		            stepForwardUnit: "days",
		            timeDelay:    5000, 
		            allowPlay: true
			}
			
		    function sliderUpdated(data) {
				data.startValue
				data.endValue
			}
			var myDateSlider = new DateSlider('timeSlider', sliderUpdated, "20100101T000000Z", "20161231T235959Z", step, "YYYYMMDD", true, true, playOptions )
	
   
    Requires: nouislider 9.2.0 (with modification to update pip on range change. see https://github.com/leongersen/noUiSlider/issues/780
              momentjs 2.18.1  (tested version, may work with others)
              bootbox 4.3
              Fontawesome 5 (css needs to be included)
              
    Constructor:
    sliderDivID - This is the div element ID that holds the slider and its ancillary components.
    callbackFunction - whenever the start or end markers are updated, this function will be called with a single JSON object that has two properties: startValue, endValue
    minDateTime - what is the start (bottom) value of the date slider?  String using ISO-8601 format
    maxDatetime - what is the top value of the date slider?  String in ISO-8601 format (e.g., "20101225T200520Z")
    stepMS - for each move of a slide, what is the change in value.  Expressed in milliseconds.  One day: 24*60*60*1000 
    dateFormatString - when displaying the date on the component.  See https://momentjs.com/docs/#/displaying/format/ for details.
    showTooltip - boolean
    playOptions - javascript object with the following fields
        windowSize - how wide should the "window" be for the time.  Expressed as an integer
        windowSizeUnit: years, months, days, hours, minutes, seconds
        stepForwardSize - at each "tick" of the play time, how much forward should be the window be moved forawrd.  integer
        stepForwardUnit: years, months, days, hours, minutes, seconds
        timeDelay   - what is the delay between each tick of playing.
        allowPlay   - boolean.  Whether or not the feature is allowed.  If false, buttons do not appear and the other 3 values are not used            
*/
function DateSlider(sliderDivID, callbackFunction, minDateTime, maxDateTime, stepMS, dateFormatString, showRangeValue, showTooltip,  playOptions) {	
    this.dateFormat = dateFormatString;
   	this.playOptions = playOptions;
   	
   	this.toFormat =  function (value) {
		return moment(Math.floor(value)).format(dateFormatString);  // prototype function wasn't getting the dateFormat
	} 
   	
   	var minValue = moment.utc(minDateTime).valueOf();
   	var maxValue = moment.utc(maxDateTime).valueOf();
   	var options = {
		    range: {
		        min: minValue, 		
		        max: maxValue
		    },
		    connect: true,
		    step: stepMS, 
		    tooltips: [showTooltip, showTooltip ],
		    format: { to: this.toFormat, from: Number },
		    start: [minValue, maxValue ]
		};
   	if (showRangeValue) {
   		options.pips = {
				mode: 'count',
				density: 4,
				values: 5,
				format: { to: this.toFormat, from: Number }
			}
   	}
   	

   	document.getElementById(sliderDivID).style.margin = "0px 25px 60px 25px";
   	this.dateSlider = noUiSlider.create(document.getElementById(sliderDivID),options );
   	
   	var currentValues = {	startValue: Number.MIN_VALUE, endValue: Number.MAX_VALUE }; // use this to track the range last sent out. if this is unchanged, don't omit the event.
   	
   	this.dateSlider.on('update', function(values, handle, unencoded, tap, positions ){
   		var result = {
   				startValue: unencoded[0],
   		        endValue: unencoded[1]
   		}
   		
   		if (result.startValue != currentValues.startValue || result.endValue != currentValues.endValue) {
   			currentValues = result;
   			callbackFunction(result);
   		}
   	});
   	
	this.getDateRange = function () {
		return currentValues;
	}
   	
   	if (playOptions.allowPlay == true) {
		var myPlayTimer = null;
		var mySlider = this;
		
		var step = function () {
			var lowerValue = moment.utc(currentValues.startValue).add(mySlider.playOptions.stepForwardSize, mySlider.playOptions.stepForwardUnit).valueOf();
			var upperValue = moment.utc(lowerValue).add(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
			if (upperValue > mySlider.dateSlider.options.range.max) {
				upperValue = mySlider.dateSlider.options.range.max;
				
				lowerValue = moment.utc(upperValue).subtract(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
				lowerValue = Math.max(lowerValue,mySlider.dateSlider.options.range.min);
				
				clearInterval(myPlayTimer);
			}
			var settings =  [ lowerValue, upperValue ];
			mySlider.dateSlider.set(settings);
		} 
		
		
		this.startPlay =  function () {
			clearInterval(myPlayTimer);
			
			var play = true;
			var lowerValue = currentValues.startValue;
			var upperValue = moment.utc(lowerValue).add(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
			if (upperValue > mySlider.dateSlider.options.range.max) {
				upperValue = mySlider.dateSlider.options.range.max;
				
				lowerValue = moment.utc(upperValue).subtract(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
				lowerValue = Math.max(lowerValue,mySlider.dateSlider.options.range.min);

				play = false;
				
			}
			var settings =  [ lowerValue, upperValue ];
			mySlider.dateSlider.set(settings);
			if (play) {
				myPlayTimer = setInterval(step, mySlider.playOptions.timeDelay);
			}
		}
		
		this.stopPlay = function () {
			clearInterval(myPlayTimer);
		}    
		
		this.moveToStart = function () {   			
			var lowerValue = mySlider.dateSlider.options.range.min;
			var upperValue = moment.utc(lowerValue).add(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
			if (upperValue > mySlider.dateSlider.options.range.max) {
				upperValue = mySlider.dateSlider.options.range.max;
				clearInterval(myPlayTimer);
			}
			var settings =  [ lowerValue, upperValue ];
			mySlider.dateSlider.set(settings);
		}      			

		this.stepBack = function () {
			var lowerValue = moment.utc(currentValues.startValue).subtract(mySlider.playOptions.stepForwardSize, mySlider.playOptions.stepForwardUnit).valueOf();
			var upperValue = moment.utc(lowerValue).add(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
			
			if (lowerValue < mySlider.dateSlider.options.range.min) {
				lowerValue = mySlider.dateSlider.options.range.min
				upperValue = moment.utc(lowerValue).add(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
			}
			
			if (upperValue > mySlider.dateSlider.options.range.max) {
				upperValue = mySlider.dateSlider.options.range.max; 				
				clearInterval(myPlayTimer);
			}
			var settings =  [ lowerValue, upperValue ];
			mySlider.dateSlider.set(settings);	
		}  
		
		this.stepForward = function () {
			step();
		}
		
		this.moveToEnd = function () {
			var upperValue = mySlider.dateSlider.options.range.max;
			var lowerValue = moment.utc(upperValue).subtract(mySlider.playOptions.windowSize, mySlider.playOptions.windowSizeUnit).valueOf();
			if (lowerValue < mySlider.dateSlider.options.range.min) {
				lowerValue = mySlider.dateSlider.options.range.min
				clearInterval(myPlayTimer);
			}
			var settings =  [ lowerValue, upperValue ];
			mySlider.dateSlider.set(settings);    			

		}   
   		
		this.playSettings = function () {    			
			var dialog = bootbox.dialog({
				title: 'Change Play Settings',
			    message: '<form class="form-inline"><div class="form-group"><label for="timeWindowSize">Period width </label><input type="text" class="form-control" id="timeWindowSize" maxlength="3" size="3"value="'+mySlider.playOptions.windowSize+'" style="width:30px"></div><div class="form-group"><select class="form-control" id="timeWindowSizeUnit"><option value="seconds">seconds</option><option value="minutes">minutes</option><option value="hours">hours</option><option value="days">days</option><option value="months">months</option><option value="years">years</option></select></div></form><form class="form-inline"><div class="form-group"><label for="timeWindowStep">Step by </label><input type="text" class="form-control" id="timeWindowStep" maxlength="3" size="3"value="'+mySlider.playOptions.stepForwardSize+'" style="width:30px"></div><div class="form-group"><select class="form-control" id="timeWindowStepUnit"><option value="seconds">seconds</option><option value="minutes">minutes</option><option value="hours">hours</option><option value="days">days</option><option value="months">months</option><option value="years">years</option></select></div></form><form class="form-inline"><div class="form-group"><label for="timeDelay">Time Delay</label><input type="text" class="form-control" id="timeDelay" maxlength="3" size="3" value="'+(mySlider.playOptions.timeDelay/1000)+'" style="width:30px"><label for="timeDelay">seconds</label></div></form>',           
			    closeButton: true,
			    onEscape: true,
			    buttons: {
			        confirm: {
			            label: 'Set',
			            className: 'btn-primary',
			            callback: function() { //TODO: we should have more error checking on these ...
			            	mySlider.playOptions.windowSize      = $("#timeWindowSize").val();
			            	mySlider.playOptions.windowSizeUnit  = $("#timeWindowSizeUnit").val();
			            	mySlider.playOptions.stepForwardSize = $("#timeWindowStep").val();
			            	mySlider.playOptions.stepForwardUnit = $("#timeWindowStepUnit").val();
			            	mySlider.playOptions.timeDelay       = $("#timeDelay").val() * 1000;
			            }
			        },
			        cancel: {
			            label: 'Cancel',
			            className: 'btn-primary',
			            callback: function() {
			            	console.log("cancel");
			            }
			        }
			    }
			});
			$("#timeWindowSizeUnit").val(mySlider.playOptions.windowSizeUnit);
			$("#timeWindowStepUnit").val(mySlider.playOptions.stepForwardUnit);
		}   
   		       		
   		var buttonBar   = $('<div class="btn-group" role="group" style="display: block; margin:0 auto; width: 200px; top: 250%; position: relative"></div>');
   		var settingsBar = $('<div class="btn-group" role="group" style="display: block;float: right; top: 250%;"></div>');
   		
   		var btnGotoStart   = $('<button type="button" class="btn btn-primary  btn-sm"><span style="padding-left: 7px;" class="fas fa-fast-backward"></span></button>');
   		var btnStepBack    = $('<button type="button" class="btn btn-primary  btn-sm"><span class="fas fa-step-backward"></span></button>');
   		var btnPlay        = $('<button type="button" class="btn btn-primary  btn-sm"><span class="fas fa-play"></span></button>');
   		var btnPause       = $('<button type="button" class="btn btn-primary  btn-sm"><span class="fas fa-pause"></span></button>');
   		var btnStepForward = $('<button type="button" class="btn btn-primary  btn-sm"><span class="fas fa-step-forward"></span></button>');
   		var btnGotoEnd     = $('<button type="button" class="btn btn-primary  btn-sm"><span class="fas fa-fast-forward"></span></button>');
   		var btnSettings    = $('<button type="button" class="btn btn-primary  btn-sm"><span style="padding-left: 2px;" class="fas fa-cog"></span></button>');
   		
   		buttonBar.append(btnGotoStart);
   		buttonBar.append(btnStepBack);
   		buttonBar.append(btnPlay);
   		buttonBar.append(btnPause);
   		buttonBar.append(btnStepForward);
   		buttonBar.append(btnGotoEnd);
   		
   		settingsBar.append(btnSettings);
   		
   		btnGotoStart.click(this.moveToStart)
   		btnStepBack.click(this.stepBack)
   		btnPlay.click(this.startPlay);
   		btnPause.click(this.stopPlay);
   		btnStepForward.click(this.stepForward);
   		btnGotoEnd.click(this.moveToEnd);
   		
   		btnSettings.click(this.playSettings);
		
   		$('#'+sliderDivID).append(buttonBar);
   		$('#'+sliderDivID).append(settingsBar);
   	}

}
 
/**
 * Change the boundaries for the dateSlider
 * 
 *  minDateTime - what is the start (bottom) value of the date slider?  String using ISO-8601 format
 *  maxDatetime - what is the top value of the date slider?  String in ISO-8601 format (e.g., "20101225T200520Z")
 *  newStepMS - for each move of a slide, what is the change in value.  Expressed in milliseconds.  One day: 24*60*60*1000 
 */
DateSlider.prototype.changeRangeAndStep = function(minDateTime, maxDateTime, newStepMS= 604800000) {  // milliseconds in one week7 * 24 * 60 * 60 * 1000
	this.dateSlider.updateOptions({
		range: {
			'min' : moment.utc(minDateTime).valueOf(),
			'max' : moment.utc(maxDateTime).valueOf()
		},
		step: newStepMS
	});
	var settings =  [  moment.utc(minDateTime).valueOf(), moment.utc(maxDateTime).valueOf() ];
	this.dateSlider.set(settings);
}
