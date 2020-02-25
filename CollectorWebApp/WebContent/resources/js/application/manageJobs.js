LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

$(document).ready(function() {
		LASLogger.instrEvent('application.manageJobs');
		
		window.page = "manage_jobs"; // Page name to be stored in window object so that LasHeader.js could access it for logging

		var tblSourceHandlers = $('#tblJobs').DataTable({
			"pageLength" : 50,
			"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
			"dom": 'pltif', // 'iftpl',
	        "buttons": [
	            'excel','print'
	        ],
			"columns" : [ {
				"data" : "name",
				"width" : "200"
			}, {
				"data" : "primaryFieldValue",
				"width" : "200"
			}, {
				"data" : "sourceHandler",
				"width" : "150"
			}, {
				"data" : "schedule",
				"width" : "150"
			}, {
				"data" : "configuration",
				"width" : "200" 
			}, {
				"data" : "status",
				"width" : "100"
			}, {
				"className" : "nowrap",
				"data" : "statusTimestamp"
			}, {
			    "className" : "nowrap",
				"data" : "nextRun"
			}, {
				"data" : "priority"
			},
			{
				"data" : "ownerEmail",
				"width" : "0"
			}],
			"order" : [ [ 0, "asc" ] ]
		});

		$('#btnAddJob').click(navigateToAddJob);
		$('#exportButton').click(exportJobs);
		$('#btnDomainHome').click(function(){
			LASLogger.instrEvent('application.concept.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
		});
		$('#btnScheduleErrored').on('click', scheduleAllErroredJobs );
		
		
		refreshTables();
	});


function editJob(jobId,status) {
	status = status.toUpperCase();
	if( status == "READY" || status == "COMPLETE" || status == "SCHEDULED" ||status == "INACTIVE" || status=="HOLD" || status == "ERRORED" || status == "ADJUDICATION"  || status == "DRAFT")
		window.location =openke.global.Common.getPageURLPrefix()+"/addEditJob?jobId=" + jobId;
	else
		$('#err_label_user').html("Job cannot be edited while in \""+status+"\" status.")
}

function navigateToAddJob() {
	window.location =openke.global.Common.getPageURLPrefix()+"/addEditJob";
}

function gotoJobHistory(jobId) {
	window.location =openke.global.Common.getPageURLPrefix()+"/jobHistory?jobId=" + jobId;
}
	
function gotoVisitedPages(jobId) {
	window.location =openke.global.Common.getPageURLPrefix()+"/visitedPages?jobId=" + jobId;
}

function formatNumber(num) {
	if (num < 10) {
		return "0"+num;
	}
	else {return ""+num;}
}


function refreshTables() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs", refreshJobsTable);
}

function refreshJobsTable(data) {
	var table = $('#tblJobs').DataTable();
	table.clear();
	for (var i = 0; i < data.jobs.length; i++) {
		var newRow = data.jobs[i];

		var prettyConfig = escapeHtmlNewLine2(JSON.stringify(newRow.configuration, null, '\t'));
		var c = "<div class=scrollable>" + prettyConfig + "</div>";
		newRow.configuration = c;		
		
		var id = '"' + newRow.id + '"';
		var status = '"' + newRow.status + '"';
		var name = newRow.name;
		name = name+"<br><a class='no-print' href='javascript:editJob(" + id + ","+status+")' >Edit</a>"+
		            "<br><a class='no-print' href='javascript:gotoJobHistory(" + id + ")' >Job&nbsp;History</a>"+
		            "<br><a class='no-print' href='javascript:gotoVisitedPages(" + id + ")' >Visited&nbsp;Pages</a>";
		newRow.name = name;
		newRow.schedule = cronDisplayText(newRow.cronSchedule) + "<br>" +newRow.cronSchedule;
		if (newRow.status !== "ready" && newRow.status !== "complete" && newRow.status !== "scheduled") {
			newRow.nextRun = "";
		}
		
		table.row.add(newRow);
	}
	table.draw();
}



function scheduleAllErroredJobs() {
	LASLogger.instrEvent('application.manageJobs.scheduleAllErrorredJobs');
	
    $.ajax({
		type : "PUT",
		dataType: "json",
		url : openke.global.Common.getRestURLPrefix()+"/jobs/scheduleErrored",
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			refreshTables();
			bootbox.alert("Errored jobs moved to a scheduled state");
		},
		error : function(data) {  // job was not found
			if (shouldSubmit) {submitActualForm(submitAsNew);}
		}
	});
	
	return false;
}

function cronDisplayText(cronString) { 
	try {
		return cronstrue.toString(cronString);
	}
	catch (e) {
		LASLogger.log(LASLogger.LEVEL_INFO, "Can't get display text: "+e);
		return "undefined";
	}
}


function exportJobs() {
	var docObj = {};    // to hold exported json docs
	var zip = new JSZip();
	var url = openke.global.Common.getRestURLPrefix()+"/jobs"
	
	$.ajax({
		dataType: "JSON",
		url: url,
		async: false,
		success: function(data){
			for (var i = 0; i < data.jobs.length; i++) {
				var job = data.jobs[i];
				var id = job.name.replace(/\W|_/g,'');
				docObj[id] = job;
			}
		}
	});

	// add the source files to the zip. Syntax is file, content
	for(var key in docObj){
		var element = docObj[key];
		zip.file(key+'.json', JSON.stringify(element));
	}

	var filename = "job_Export";
	//goes to FileUploadController
	var url = openke.global.Common.getRestURLPrefix()+"/upload/zip/"+filename;
	
	// generate the zip and send to server for saving
    zip.generateAsync({type:"base64"}).then(function (base64) {
    	$.ajax({
			type: "POST",
			url: url,
			data: base64,
			contentType: false,
			processData: false,
			success: function(data){
				alert(data);
			}
		});
    	
	    }, function (err) {
	        jQuery("#exportButton").text(err);
	    });

	return false;
}
