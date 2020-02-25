
<%@ include file="/WEB-INF/views/includes/taglibs.jsp"%>

<c:url var="projName"  value="/" />

<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>OpenKE</title>
<link rel="SHORTCUT ICON" href="${projName}resources/images/LAS_Logo.ico">
<link rel="stylesheet" type="text/css"	href="${projName}resources/bootstrap/css/bootstrap.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${projName}resources/css/demonstrator.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${projName}resources/nouislider.9.2.0/nouislider.css?build=${build}" />

</head>
<body>
	<div class="row">
		<div class="form-group col-md-12">
		</div>
	</div>	<div class="row">
		<div class="form-group col-md-12">
		</div>
	</div>
	<div class="row">
		<div class="form-group col-md-12">
		</div>
	</div>
	
	<div class="row">
		<div class="form-group col-md-12">
			<div id="timeSlider"></div>
		</div>
	</div>
	<div class="row">
		<div class="form-group col-md-12">
		<!-- 
<form class="form-inline">
  <div class="form-group">
    <label for="timeWindowSize">Period width </label>
    <input type="text" class="form-control" id="timeWindowSize" maxlength="3" size="3"  value="90" style="width:30px">
  </div>
  <div class="form-group">
      <select class="form-control" id="timeWindowSizeUnit">
          <option value="seconds">seconds</option>
	      <option value="minutes">minutes</option>
		  <option value="hours">hours</option>
		  <option value="days" selected>days</option>
		  <option value="months">months</option>
		  <option value="years">years</option>
	  </select>
  </div>
</form>
<form class="form-inline">
  <div class="form-group">
    <label for="timeWindowStep">Step by </label>
    <input type="text" class="form-control" id="timeWindowStep" maxlength="3" size="3"  value="30" style="width:30px">
  </div>
  <div class="form-group">
      <select class="form-control" id="timeWindowStepUnit">
          <option value="seconds">seconds</option>
	      <option value="minutes">minutes</option>
		  <option value="hours">hours</option>
		  <option value="days" selected>days</option>
		  <option value="months">months</option>
		  <option value="years">years</option>
	  </select>
  </div>
</form>
<form class="form-inline">
  <div class="form-group">
    <label for="timeDelay">Time Delay</label>
    <input type="text" class="form-control" id="timeDelay" maxlength="3" size="3" value="3" style="width:30px">
    <label for="timeDelay">seconds</label>
  </div>
</form>
 -->
		</div>
	</div>
	
		
	<script type="text/javascript" charset="utf8" src="${projName}resources/js/external/jquery-3.1.1.js"></script>
	<script type="text/javascript" charset="utf8" src="${projName}resources/bootstrap/js/bootstrap.js"></script>
	<script type="text/javascript" charset="utf8" src="${projName}resources/js/external/bootbox.js"></script>	
	<script type="text/javascript" charset="utf8" src="${projName}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${projName}resources/nouislider.9.2.0/nouislider.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${projName}resources/momentjs.2.22.2/moment.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${projName}resources/js/application/component/DateSlider.js?build=${build}"></script>
    
<script>
	
	var step = 7 * 24 * 60 * 60 * 1000;  // use a step of one week
	var playOptions = {
			windowSize : 90,
			windowSizeUnit: "days",
            stepForwardSize: 30,
            stepForwardUnit: "days",
            timeDelay:    5000, 
            allowPlay: true
	}
    
	var myDateSlider = new DateSlider('timeSlider', sliderUpdated, "20100101T000000Z", "20161231T235959Z", step, "YYYYMMDD", true, true, playOptions )
	
	//myDateSlider.changeRangeAndStep("20040101T000000Z","20100101T000000Z",step*4)
	//myDateSlider.startPlay(10000);
	
	function sliderUpdated(data) {
		LASLogger.logObject(LASLogger.LEVEL_FATAL,data);
	}
	
</script>

</body>
</html>