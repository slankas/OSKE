LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


function goBack() {
	LASLogger.instrEvent('application.' + PAGE + '.cancel', {}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/manageJobs";});
}

// Job-Id will be null if creating new job.
var jobId = null;

var PAGE = "add_job"; // Page type. Whether it is a new job(add job) or edit existing job.

var cronField;

var loadedJobRecord;  // stores the job record from the server.

var adjudicationObject = { "utilize" : false }  
var adjudicationAnswers = []

$(document).ready(function() {
    editor = ace.edit("editor");
    editor.setOptions({
        maxLines: Infinity
    });
    editor.setTheme("ace/theme/xcode");
    editor.session.setMode("ace/mode/json");
    editor.getSession().setTabSize(4);
    editor.getSession().setUseSoftTabs(true);

	createCronWidget(JobFactory.getRandomizedStartTime());
	
	window.page = "add_job"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	jobId = $('#jobID').val()
	status = $('')
    if (jobId != null && jobId != "") {
    	PAGE = "edit_job";   
    	window.page = "edit_job";
    	LASLogger.instrEvent('application.edit_job', {
    		jobID : jobId
    	});
    	getJob();
    } else {
    	LASLogger.instrEvent('application.add_job');
		document.getElementById("tdStatusDateTime").style.display = "none";
		
		setButtonVisibility("");
        
        document.getElementById("trJobLinks").style.display = "none"
        	
        $('#btnCreateSubmit').html('Create');
        $('#btnCreateSubmitBack').html('Create & View Jobs');
    }
	document.getElementById("err_label").style.display = "none";
	document.getElementById("success_label").style.display = "none";

	$('#visitLink').on('click', function(e) {
		var destination = $('#txtPrimaryValue').val();
		window.open(destination)
		return false;
	});
	
	// event handler for randomize checkbox (enable input if checked, disable if not)
	$("#isRandom").change(toggleRandomize);
	
	$('#cronSwitch').on('click',cronEditSwitch);
	$('#btnTestFormAuthentication').on('click', testFormAuthentication);
	$('#btnTestConfiguration').on('click', testFormConfiguration);
	
	$('#drpdnSrcHandler').on('change', sourceHandlerChanged);
	$('#btnLoadTemplate').on('click', loadFullConfig);
	$('#btnFormatCfg').on('click', formatJSON);
	
	$('#btnShowParameters').on('click',	
	    function(e) {
			LASLogger.instrEvent('application.' + PAGE + '.show_parameters');

			e.preventDefault();
			var screen_width = screen.width * .8;
		    var screen_height = screen.height* .7;
		    var top_loc = screen.height *.15;
		    var left_loc = screen.width *.1;

		    window.open(openke.global.Common.getPageURLPrefix()+"/jobParameters?name="+$('#drpdnSrcHandler').val(),'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
		    window.focus();
			return false;
	    });
	
	
	var message = $('#message').val()
	if (message != null && message != "") {
		$('#myModal').modal('show')                // initializes and invokes show immediately
		$('#myModalDialogText').html(message);
	}
	
	configureAdjudicationQuestions();
	toggleRandomize();
	
});


function toggleRandomize(){
	if ( $("#isRandom").is(":checked")) {
		$("#randomPercent").prop('disabled', false);
	}else{
		$('#randomPercent').val('');
		$("#randomPercent").prop('disabled', true);
	}
}


function configureAdjudicationQuestions() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/questions", function (data) {
		adjudicationObject = data;
		if (adjudicationObject.utilize) {
			$("#adjudicatorQuestionsLink").text(adjudicationObject.title);
			$("#adjudicatorQuestionsLink").click(function() {
				displayAdjudicatorQuestions();
			});
			if (adjudicationAnswers.length == 0) {
				adjudicationAnswers = JSON.parse(JSON.stringify(adjudicationObject.questions));
			}
			updateAdjudicationStatus();
		}
		else {
			$("#adjudicatorQuestionArea").hide()
		}
	});
}

function updateAdjudicationStatus() {
	var numQuestions = adjudicationAnswers.length;
	var numComplete  = 0;
	
	for (var i=0 ; i< numQuestions; i++) {
		if (adjudicationAnswers[i].hasOwnProperty("answer") && adjudicationAnswers[i].answer.trim().length > 0) {
			numComplete++;
		}
	}
	$("#numAdjQuestComplete").text(numComplete)
	$("#numAdjQuestTotal").text(numQuestions)	
}

window.updateAdjudicationAnswer =  function(questionNum, answer) {
	adjudicationAnswers[questionNum].answer= answer
	updateAdjudicationStatus()
}

function getJob() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId, populateJob);
}

function populateJob(data) {
	if (data == null || data.id == null) {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Invalid Job Id";
		return;
	}
	loadedJobRecord = data;
	
	document.getElementById("lblStatus").innerHTML = data.status;
	document.getElementById("lblStatusDatetime").innerHTML = data.statusTimestamp;
	document.getElementById("txtJobName").value = data.name;
	document.getElementById("txtPrimaryValue").value = data.primaryFieldValue;
	document.getElementById("txtPriority").value = data.priority;
	editor.setValue(JSON.stringify(data.configuration,null,4), 1);
	$("#isExport").prop('checked', data.exportData);
	
	document.getElementById("txtJustification").value = data.justification;

	document.getElementById("drpdnSrcHandler").value = data.sourceHandler;
	
	setCronValue(data.cronSchedule);
	
	//set randomization fields
	$("#isRandom").prop('checked', (data.randomPercent != 0));
	toggleRandomize();
	if (data.randomPercent != 0){
		document.getElementById("randomPercent").value = data.randomPercent;
	}
	
	if (data.adjudicationAnswers.length > 0) {
		adjudicationAnswers = data.adjudicationAnswers;
		updateAdjudicationStatus();
	}
	
	if (data.status == "processing") {
		bootbox.alert("This job is currently processing and can not be edited.");
		LASLogger.instrEvent('application.' + PAGE + '.cancelJobProcessing', { uuid:jobId }, function() {window.location=openke.global.Common.getPageURLPrefix()+"/manageJobs";});
	}
	
	
	var a = document.getElementById('recentVisitedPages');
	a.href = "visitedPages?jobHistoryID="	+ data.latestJobID 
	a.innerHTML = "Most Recent Visited Pages"
		
	a = document.getElementById('jobHistory');
	a.href = "jobHistory?jobId=" + data.id
	a.innerHTML = "Job History"
		
	document.getElementById("lblHeading").innerHTML = "Edit Job: " + "<b>" + data.name + "</b>";
	setButtonVisibility(data.status);
	
	$('#btnCreateSubmit').html('Submit')
	$('#btnCreateSubmitBack').html('Submit & View Jobs')
	sourceHandlerChanged();

}

function setButtonVisibility(status) {
	switch (status) {
	case "new": $("#btnStop").hide();
		        $("#btnRunNow").hide();
		        $("#btnHold").hide();
		        $("#btnPurgeData").hide();
		        $("#btnDeleteJob").hide();
		        break;
	case "processing": $("#btnStop").show();
                       $("#btnRunNow").hide();
                       $("#btnHold").hide();
                       $("#btnPurgeData").hide();
                       $("#btnDeleteJob").hide();
                       break;
	case "complete":   $("#btnStop").hide();
                       $("#btnRunNow").show();
                       $("#btnHold").show();
                       $("#btnPurgeData").show();
                       $("#btnDeleteJob").show();
                       break;
	case "ready":      $("#btnStop").hide();
                       $("#btnRunNow").hide();
                       $("#btnHold").show();
                       $("#btnPurgeData").show();
                       $("#btnDeleteJob").show();
                       break;
	case "errored":    $("#btnStop").hide();
                       $("#btnRunNow").show();
                       $("#btnSchedule").show();
                       $("#btnHold").show();
                       $("#btnPurgeData").show();
                       $("#btnDeleteJob").show();
                       break;     
	case "stopping":   $("#btnStop").hide();
                       $("#btnRunNow").hide();
                       $("#btnHold").hide();
                       $("#btnPurgeData").hide();
                       $("#btnDeleteJob").hide();
                       break;
	case "hold":       $("#btnStop").hide();
                       $("#btnRunNow").show();
                       $("#btnSchedule").show();
                       $("#btnHold").hide();
                       $("#btnPurgeData").show();
                       $("#btnDeleteJob").show();
                       break;  
	case "scheduled":  $("#btnStop").hide();
	                   $("#btnRunNow").show();
	                   $("#btnHold").show();
	                   $("#btnPurgeData").show();
	                   $("#btnDeleteJob").show();
	                   break;                     
	case "adjudication":$("#btnStop").hide();
					    $("#btnRunNow").hide();
					    $("#btnHold").hide(); 
					    $("#btnSaveAsNew").hide();
					    $("#btnSchedule").hide();
					    $("#btnPurgeData").show();
					    $("#btnDeleteJob").show();
					    break;
	case "inactive":$("#btnStop").hide();
				    $("#btnRunNow").hide();
				    $("#btnHold").hide(); 
				    $("#btnSaveAsNew").hide();
				    $("#btnSchedule").hide();
				    $("#btnPurgeData").show();
				    $("#btnDeleteJob").show();
                    break;					    
	case "draft":   $("#btnStop").hide();
				    $("#btnRunNow").hide();
				    $("#btnHold").hide(); 
				    $("#btnSaveAsNew").hide();
				    $("#btnSchedule").hide();
				    $("#btnPurgeData").hide();
				    $("#btnDeleteJob").show();
    break;					    
                    
    default: $("#btnStop").hide();
             $("#btnRunNow").hide();
             $("#btnHold").hide(); 
             $("#btnSaveAsNew").hide();
             $("#btnSchedule").hide();
             $("#btnPurgeData").hide();
             $("#btnDeleteJob").hide();
             break;
	}
}


function sourceHandlerChanged() {
	if ($('#drpdnSrcHandler option:selected').attr("data-testable") === "true") {
		$('#btnTestConfiguration').show();
	}
	else {
		$('#btnTestConfiguration').hide();
	}
	
	if ($('#drpdnSrcHandler option:selected').attr("data-primaryhidden") === "true") {
		$('#primaryLabelRow').hide();
	}
	else {
		$('#primaryLabelRow').show();
	}	
	
	var label = $('#drpdnSrcHandler option:selected').attr("data-primarylabel");
	$('#primaryLabel').text(label+":");
	
	if (label.startsWith("URL")) {
		$('#visitLink').show();
	}
	else {
		$('#visitLink').hide();
	}
}


function createCronWidget(value) {
	try {
		cronField = $('#divSchedule').cron({
		    initial: value,
		    customValues: {
		        "2 hours" : "0 0 */2 * * ?",
		        "4 hours" : "0 0 */4 * * ?",
		        "8 hours" : "0 0 */8 * * ?",
		        "12 hours" : "0 0 */12 * * ?",
		        "2 days at 0900" : "0 0 9 */2 * ?"
		    },
		    useGentleSelect: true,
		    effectOpts: {
		        openSpeed: 200,
		        closeSpeed: 200
		    }
		});		
	}
	catch (e) {
		LASLogger.log(LASLogger.LEVEL_INFO, e);
		createAdvancedScheduleBox(value) ;
		bootbox.alert("Unable to use CRON widget.  Showing advanced view");
	}
}

function createAdvancedScheduleBox(value) {
	$("#divSchedule").empty();
	$("#divSchedule").append('<input style="width: 100%;" type="text"	id="txtSchedule" name="txtSchedule" autocomplete="on" maxlength="1000" />')
	$("#txtSchedule").val(value);
	$('#txtSchedule').tooltip({'animation': false, 'trigger':'focus', 'title': cronDisplayText});
	$('#txtSchedule').on("keyup", function() { $('#txtSchedule').tooltip('show');});
	$('#cronSwitch').text("Guided");
}

function getCronValue() {
	var switchTo = $('#cronSwitch').text();
	
	if (switchTo === "Advanced") {
		return cronField.cron("value");
	}
	else {
		return $("#txtSchedule").val();
	}	
}

function setCronValue(newValue) {
	var switchTo = $('#cronSwitch').text();
	
	if (switchTo === "Advanced") {  // currently displaying the widget
		if (cronHasComplicatedExpression(newValue)) {
			cronEditSwitch();
			$("#txtSchedule").val(newValue);
			return;
		}
		else {
			try {
				cronField.cron("value",newValue);
			}
			catch (e) {
				LASLogger.log(LASLogger.LEVEL_INFO, e);
				createAdvancedScheduleBox(newValue);
			}
		}
	}
	else {
		$("#txtSchedule").val(newValue);
	}
}

function cronHasComplicatedExpression(value) {
	return value.indexOf(",") != -1 || value.indexOf("-") != -1 || value.indexOf("/") != -1
}

function cronDisplayText() { 
	try {
		return cronstrue.toString(getCronValue());
	}
	catch (e) {
		LASLogger.log(LASLogger.LEVEL_INFO, "Can't get display text: "+e);
		return "undefined";
	}
}

function cronEditSwitch() {
	var switchTo = $('#cronSwitch').text();
	var currentValue = getCronValue();
	
	if (switchTo === "Advanced") {
		createAdvancedScheduleBox(currentValue);
	}
	else {
		if (cronHasComplicatedExpression(currentValue)) {
			bootbox.alert("Unable to use guided view with lists, ranges, or step values");
			return;
		}
		$("#divSchedule").empty();
		$('#cronSwitch').text("Advanced");
		createCronWidget(currentValue);
	}
	
	return false;
}


function loadFullConfig() {
	LASLogger.instrEvent('application.' + PAGE + '.load_template');

	editor.setValue("{ \"limitToDomain\": true, \"webCrawler\" : { \"maxDownloadSize\" : 1000000000} }", 1);

	var sourceHandlerName = $('#drpdnSrcHandler').val();
	if (sourceHandlerName == null || sourceHandlerName =="") {
		return false;
	}
	$("#tblSrcParameters tr").remove();

	$.getJSON(openke.global.Common.getRestURLPrefix()+"/handler/source/" + sourceHandlerName + "/config",
		function(result) {
		editor.setValue(JSON.stringify(result,null,4), 1);
		});

	return false;
}


function editStatus(status) {
	if (jobId == null) {
		return false;
	}

	var url = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId + "/"+status;
    
	LASLogger.instrEvent('application.' + PAGE + '.' + status , { 
		name : $('#txtJobName').val(), 
		url : url, 
		uuid : jobId
	});			

    $.ajax({
		type : "POST",
		url : url,
		success : function(data) {
			var status = data;
			if (status != null && status != "") {
				document.getElementById("lblStatus").innerHTML = status;
				setButtonVisibility(status);
				
				var message="Unknown status result occurred";
				if (status== "ready") { message= "Requested job to run"; }
				else if (status == "stopping") { message= "Requested to stop job"; }
				else if (status == "hold") {message= "Job put on hold status"; }
				else if (status == "scheduled") {message= "Job has been scheduled"; }
				
				$('#myModal').modal('show')                // initializes and invokes show immediately
				$('#myModalTitle').html("Status Changed");
				$('#myModalDialogText').html(message);
				return false;
			}
		},
		error : function(data) {
			document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = jQuery.parseJSON(data.responseText).reason;
		},
		dataType : "text",
	});
    return false;
}

function submitForm(viewJobsFlag = false) {
	performValidationAndSubmit(true,false, viewJobsFlag);
	return false;

}

function saveAsNew() {
	performValidationAndSubmit(true,true, false);
	return false;
}


function performValidationAndSubmit(shouldSubmit,submitAsNew, viewJobsFlag) {
	document.getElementById("success_label").style.display = "none";

	if ($('#txtJobName').val() == null || $('#txtJobName').val().trim() == "") {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Please add a name.";

		$("#txtJobName").focus();
		return false;
	}
	
	var checkPrimaryValue = true;
	if ($('#drpdnSrcHandler option:selected').attr("data-primaryhidden") === "true") {
		$('#txtPrimaryValue').val("");
		checkPrimaryValue = false;
	}
	
	if (checkPrimaryValue == true && (
		$('#txtPrimaryValue').val() == null || $('#txtPrimaryValue').val().trim() == "" )    )  {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Please add a URL.";

//TODO: FIX ME!  needs to take into accoun the current search handler...
		
		$("#txtPrimaryValue").focus();
		return false;
	} else if ($('#drpdnSrcHandler').val() == null || $('#drpdnSrcHandler').val().trim == "") {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Please add source handler.";

		$("#drpdnSrcHandler").focus();
		return false;
	} else if (editor.getValue() == null || editor.getValue().trim() == "") {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Please add configuration.";

		$('textarea.ace_text-input').focus();
		return false;
	} 
	
	//check cron configure;
	var cronText = cronDisplayText(getCronValue());
	if (cronText.indexOf("undefined") != -1) {
		bootbox.alert("Invalid schedule setting: "+crontText);
		return false;
	}
	
	if (adjudicationObject.utilize && adjudicationObject.require) {
		if ($("#numAdjQuestComplete").text() != $("#numAdjQuestTotal").text()) {
			bootbox.alert("You must complete all of the adjudicator questions before submitting this job.");
			return false;
		}
		
	}
	
	
	
    $.ajax({
		type :    "GET",
		dataType: "json",
		url : 	   openke.global.Common.getRestURLPrefix()+"/jobs/jobname/" + $('#txtJobName').val(),
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (data.id == jobId && !submitAsNew) {
				if (shouldSubmit) {submitActualForm(submitAsNew, viewJobsFlag);}
			}
			else {
				document.getElementById("err_label").style.display = "inline";
				document.getElementById("err_label").innerHTML = "Job name already exists - enter a different name.";
				$("#txtJobName").focus();
				return false;
			}
		},
		error : function(data) {  // job was not found
			if (shouldSubmit) {submitActualForm(submitAsNew, viewJobsFlag);}
		}
	});
	
	return false;
}

function submitActualForm(submitAsNew,viewJobsFlag) {
    var jobJSON = getJobJSON();
    var job = JSON.stringify(jobJSON);
    var url = "";
    var methodType = "";
    var message = "";
    var title = "";
    var holdJobID = jobId;
    var requestType = "submit";
        
    if (submitAsNew) {
    	requestType = "saveAsNew";
    	jobId = "";
    }
       
    if (jobId != null && jobId != "") {
    	url = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId 	
    	message = "Job updated.";
    	title = "Job Update";
    	methodType = "PUT";
	} else {
    	url = openke.global.Common.getRestURLPrefix()+"/jobs";
    	title = "Create Job";
    	message = "Job created.";
    	methodType = "POST";
	}
    
	LASLogger.instrEvent('application.' + PAGE + '.' + requestType , { 
		name : jobJSON.name, 
		url : jobJSON.url, 
		uuid : jobId
	});			
	

    $.ajax({
		type : methodType,
		url : url,
		data: job,
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (viewJobsFlag) {
				window.location =openke.global.Common.getPageURLPrefix()+"/manageJobs";
				return;
			}
			data = JSON.parse(data);
			if (title == "Create Job") {
				jobId = data.id;
				$('#btnCreateSubmit').html('Submit')
				$('#btnCreateSubmitBack').html('Submit & View Jobs')
			}
			populateJob(data);
			document.getElementById("lblHeading").innerHTML = "Edit Job: " + "<b>" + data.name + "</b>";
			document.getElementById("lblStatus").innerHTML = data.status;
			document.getElementById("err_label").style.display = "none";
			$('#myModalDialogText').html(message);
			$('#myModalTitle').html(title);
			$('#myModal').modal('show')                // initializes and invokes show immediately
			return false;
		},
		error : function(data) {
			jobId = holdJobID;
			document.getElementById("err_label").style.display = "inline";
			var reason = jQuery.parseJSON(data.responseText).reason;
			reason = "<li>"+ reason.replace(/\n/g,"<li>")
			document.getElementById("err_label").innerHTML = "Error editing job:<ul>"+reason +"</ul>";//data.responseJSON.reason;
		},
	});
	return false;
}


function getJobJSON() {
	var isExport = false;
	if ( $("#isExport").is(':checked') ){
		isExport = true;
	}
	var job = {
		name : document.getElementById("txtJobName").value,
		status : document.getElementById("lblStatus").innerHTML,
		statusTimestamp : document.getElementById("lblStatusDatetime").innerHTML,
		primaryFieldValue : document.getElementById("txtPrimaryValue").value,
		priority : document.getElementById("txtPriority").value,
		configuration : editor.getValue(),
		justification : document.getElementById("txtJustification").value,
		sourceHandler: document.getElementById("drpdnSrcHandler").value,
		schedule : getCronValue(),
		randomPercent: $("#randomPercent").val(),
		adjudicationAnswers : adjudicationAnswers,
		exportData: isExport
	}

	return job;
}


function testFormAuthentication() {
	LASLogger.instrEvent('application.' + PAGE + '.testFormAuthentication');
	var authObject = {
		configuration : editor.getValue(),
		sourceHandler: document.getElementById("drpdnSrcHandler").value
	}
	
    $.ajax({
		type :    "POST",
		dataType: "json",
		data: JSON.stringify(authObject),
		url : 	   openke.global.Common.getRestURLPrefix()+"/jobs/authentication/test",
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			var windowData = JSONTree.create(data);
			openNewWindow(windowData);
		},
		error : function(data) {  // job was not found
			$('#myModalDialogText').html( JSON.stringify( data, null, 2 ));
			$('#myModalTitle').html("Test Authentication Failed");
			$('#myModal').modal('show')                // initializes and invokes show immediately
		}
	});	
}

 

function testFormConfiguration() {
	LASLogger.instrEvent('application.' + PAGE + '.testFormConfiguration');
	var dataObject = {
		name : document.getElementById("txtJobName").value,
		primaryField : document.getElementById("txtPrimaryValue").value,
		configuration : editor.getValue(),
		sourceHandler : document.getElementById("drpdnSrcHandler").value
	}
	
    $.ajax({
		type :    "POST",
		dataType: "json",
		data: JSON.stringify(dataObject),
		url : 	   openke.global.Common.getRestURLPrefix()+"/jobs/configuration/test",
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			var windowData = JSONTree.create(data);
			openNewWindow(windowData);
		},
		error : function(data) {  // job was not found
			$('#myModalDialogText').html( JSON.stringify( data, null, 2 ));
			$('#myModalTitle').html("Test Configuration Failed");
			$('#myModal').modal('show')                // initializes and invokes show immediately
		}
	});	
}


function purgeDataForJob() {
	if ($("#author").val() !== loadedJobRecord.ownerEmail && $("#roleAdjudicator").val() !== "true") {
		bootbox.alert({"title":"Purge Collected Data - Permission Denied", "message": "Only "+ loadedJobRecord.ownerEmail +" or an adjudicator can purge data for this job"});
		return false;
	}
	bootbox.prompt({
	    title: "Purge Collected Data - " +loadedJobRecord.name+"?<br>Enter the rationale to purge this job.<br>All collected data will be permamently removed.",
	    message: "This action cannot be undone.",
	    inputType: 'textarea',
	    callback: function (message) {
	    	if (message) {
	    		message = message.trim();
	    		if (message.length == 0) {return;}
	    		
	    		LASLogger.instrEvent('application.' + PAGE + '.purge'  , { 
	    			rationale: message,
	    			uuid : jobId
	    		});			
	    		
	    		
	    		var dataObject = {
	    				rationale: message
	    		}
	    		var purgeURL = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId +"/purge"	
	    	    $.ajax({
	    			type :    "POST",
	    			dataType: "json",
	    			data:     JSON.stringify(dataObject),
	    			url : 	   purgeURL,
	    		    contentType: "application/json; charset=utf-8",
	    			success : function(data) {
	    				bootbox.alert({
	    					title: "Purge Job Initiated",
	    					message: "You will receive an email when the purge is complete.",
	    					callback: function () {
	    						window.location =openke.global.Common.getPageURLPrefix()+"/manageJobs";
	    					}
	    				});
	    			},
	    			error : function(data) {
	    				bootbox.alert({
	    					title: "Unable to Purge Job",
	    					message: data.responseJSON.reason
	    				})
	    			}
	    		});		    		
	    	}
	    	else {
	    		return; // user cancelled
	    	}
	    }
	    
	});
}

function deleteJob() {
	if ($("#author").val() !== loadedJobRecord.ownerEmail && $("#roleAdjudicator").val() !== "true") {
		bootbox.alert({"title":"Delete job - Permission Denied", "message": "Only "+ loadedJobRecord.ownerEmail +" or an adjudicator can delete this job"});
		return false;
	}
	
	bootbox.prompt({
	    title: "Delete Job - " +loadedJobRecord.name+"?<br>Enter the rationale to delete this job.<br>All collected data will be permamently removed.  Job record will no longer be visible.",
	    message: "This action cannot be undone.",
	    inputType: 'textarea',
	    callback: function (message) {
	    	if (message) {
	    		message = message.trim();
	    		if (message.length == 0) {return;}
	    		
	    		LASLogger.instrEvent('application.' + PAGE + '.purge'  , { 
	    			rationale: message,
	    			uuid : jobId
	    		});			
	    			    		
	    		var dataObject = {
	    				rationale: message
	    		}
	    		var purgeURL = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId +"/delete"	
	    	    $.ajax({
	    			type :    "POST",
	    			dataType: "json",
	    			data:     JSON.stringify(dataObject),
	    			url : 	   purgeURL,
	    		    contentType: "application/json; charset=utf-8",
	    			success : function(data) {
	    				bootbox.alert({
	    					title: "Delete Job Initiated",
	    					message: "You will receive an email when the deletion is complete.",
	    					callback: function () {
	    						window.location =openke.global.Common.getPageURLPrefix()+"/manageJobs";
	    					}
	    				});
	    			},
	    			error : function(data) {
	    				bootbox.alert({
	    					title: "Unable to Delete Job",
	    					message: data.responseJSON.reason
	    				})
	    			}
	    		});		    		
	    	}
	    	else {
	    		return; // user cancelled
	    	}
	    }
	    
	});
}



function formatJSON() {
	var data = editor.getValue();
	if(isValidJSON(data)) {
		editor.setValue(JSON.stringify(JSON.parse(data), null, 4),1);
	} else {
		displayErrorMessage("Cannot format invalid JSON", $('textarea.ace_text-input'));
	}

	return false;
}

function displayAdjudicatorQuestions() {
    var css = 	'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/bootstrap/css/bootstrap.css" />' +
				'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/demonstrator.css" />';

	var js =  	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/jquery-3.1.1.js"></script>' +
				'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/bootstrap/js/bootstrap.js"></script>' +
				'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/bootbox.js"></script>' +
				'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASLogger.js"></script>' +
				'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/common.js"></script>';
	
	var screen_width = Math.min(screen.width, 900);
	var screen_height = screen.height* .8;
	var top_loc = screen.height *.05;
	var left_loc = screen.width *.1;
	
	var aWin = window.open("",'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
	
	aWin.document.write("<html><head>");
	aWin.document.write('<meta charset="utf-8" /><meta name="viewport" content="width=device-width, initial-scale=1.0" /><title>' + adjudicationObject.title +'</title>');
	aWin.document.write('<link rel="SHORTCUT ICON" href="'+openke.global.Common.getContextRoot()+'resources/images/LAS_Logo.ico">');
	aWin.document.write(css);
	aWin.document.write("</head><body>");
	
	aWin.document.write('<div style="margin: 0px 10px 0px 10px;">');
	aWin.document.write('<div class="row"><h2>'+adjudicationObject.title+'</h2>'+adjudicationObject.overviewText+'<p>&nbsp;<p></div>')
	
	var questionElementString = "";
	for (var i=0; i < adjudicationAnswers.length; i++) {
		var rec = adjudicationAnswers[i];
		var answer = rec.hasOwnProperty("answer") ? rec.answer : ""
		var questionHTML = "<div><h3>"+rec.category+": "+rec.subcategory+"</h3>" + rec.question; 
		questionHTML += "<textarea onKeyUp='opener.updateAdjudicationAnswer("+i+", $(this).val() )'  style='display:block;' class='form-control' id='adjQST_"+i+"' rows=3 cols=80>"+answer+"</textarea>"

		questionHTML += "</div>";
	    	
		questionElementString += questionHTML;
	}
	aWin.document.write('<div class="row">'+ questionElementString+'</div>')
		
	aWin.document.write("<div class='row'>&nbsp; </div>")
	aWin.document.write("<div class='row'><button class='btn btn-default' onclick='window.close()'>Close</button>")
	aWin.document.write("<div class='row'>&nbsp; </div>")
	aWin.document.write("</body>");
	aWin.document.write(js);
	aWin.document.write("</html>");
	
	aWin.focus();
}

