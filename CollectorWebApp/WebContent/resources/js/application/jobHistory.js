LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var startTime = $('#startTime').val();
var endTime = $('#endTime').val();
var jobID = $('#drpdn_jobName').val();

$(document).ready(function() {
    LASLogger.instrEvent('application.job_history');

	window.page = "job_history"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	var tblJobHistoryData = $('#tblJobHistoryData').DataTable( {
		"pageLength" : 50,
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"language": {
            "zeroRecords": "No records found",
            "infoEmpty": ""
		},
		"dom": 'lftip',
        "buttons": [
            'excel','print'
        ],
		"columns" : [{
				"data" : "jobName",
				"width" : "30%",
				"fnCreatedCell" : function(nTd, sData, oData,iRow, iCol) {
									  if (oData.jobName != "DirectoryWatcher") {
								          $(nTd).html("<a href='#' onclick='editJob(\""+ oData.jobID+ "\")'>"+ escapeHtml(oData.jobName)+ "</a>");
									  }
				                  }
			},
			{
				"data" : "status",
				"width" : "20%"
			},
			{
				"data" : "startTime",
				"className" : "nowrap",
				"width" : "0",
				"type" : "date-range"
			},
			{
				"data" : "endTime",
				"className" : "nowrap",
				"width" : "0",
				"type" : "date-range"
			},
			{
				"data" : "processingTime",
				"width" : "0"
			},			
			{
				"data" : "jobCollector",
				"width" : "0"
			},
			{
				"data" : "comments",
				"width" : "0"
			},
			{
				"data" : "numPageVisited",
				"width" : "0",
				"fnCreatedCell" : function(	nTd, sData, oData, iRow, iCol) {
									if (oData.numPageVisited != 0) {
										$(nTd).html("<a href= 'visitedPages?jobHistoryID="	+ oData.jobHistoryID+ "'>"	+ oData.numPageVisited+ "</a>");
									}
								  }
			},
			{
				"data" : "totalPageSizeVisited",
				"width" : "0"
			} ],
	"order" : [ [ 2, "desc" ] ]
   });

	bootbox.setDefaults({
		show : true,
		animate : false,
		cancelButton : false
	});

	$(document).on({
	    ajaxStart: function() { 		
	    	document.getElementById("imgLoading").style.display = "inline";     
	    	},
	     ajaxStop: function() { 					    	
	    	 document.getElementById("imgLoading").style.display = "none";     
	     }    
	});

	refresh();

	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs", function(data) {
		for (var i = 0; i < data.jobs.length; i++) {
			$("#drpdn_jobName").append("<option value=\"" + data.jobs[i].id + "\">"+ data.jobs[i].name + "</option>");
		}
		var jobID = getUrlParameter("jobId");
		if (jobID ) {
			$("#drpdn_jobName").val(jobID);
		}
	});
	
	$('#err_label_date').hide();

	$("#startTime").datetimepicker({
		format : 'Y-m-d H:i:s'
	});
	$("#endTime").datetimepicker({
		format : 'Y-m-d H:i:s'
	});
	
	
	$("#btnDomainHome").on('click', function() {
		var completeLocation =openke.global.Common.getPageURLPrefix() +"/"
		LASLogger.instrEvent('application.jobHistory.link', {link : completeLocation},function() {window.location = completeLocation;} );
	});
					
});


function advancedCollectorNavigateTo(location) {
	var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
	LASLogger.instrEvent('application.advancedCollectorView.link', {
		link : completeLocation
	}, function() {window.location = completeLocation;	});     
} 

function refresh() {
	// special logic for the first time the page loads when job ID has been passed as a parameter
	var jobId = document.getElementById("jobId").value;
	if (jobId != null && jobId != 'null') {
		jobID = jobId
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobsHistory?jobId=" + jobId,	refreshJobHistoryTable);
		return;
	} 
	
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobsHistory?startTime=" + startTime + "&endTime=" + endTime+"&jobId="+jobID, refreshJobHistoryTable);	
}

function refreshJobHistoryTable(data) {
	var message = "Jobs executed";
	
	if (startTime != "") {
		if (endTime != "") {
			message += " between "+startTime + " and " + endTime;
		} else {
			message += " since " + startTime;
		}
	} else if (endTime != "") {
		message += " before "+ endTime;
	} else if (jobID == ""){
		message += " in the past 24 hours";
	}
	$('#label_srch_message').html(message);
	
	if (jobID != "") {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/"+jobID, addJobNameToMessage);
	}
	
	
	var tblJobHistoryData = $('#tblJobHistoryData').DataTable();
	tblJobHistoryData.clear();

	for (var i = 0; i < data.jobHistory.length; i++) {
		newRow = data.jobHistory[i];
		// LASLogger.logObject(LASLogger.LEVEL_TRACE,newRow);
		tblJobHistoryData.row.add(newRow);
	}
	tblJobHistoryData.draw();
	
	// special logic for the first time the page loads when job ID has been passed as a parameter
	// by setting this to "null", the value won't be used when "filter" is pressed.
	document.getElementById("jobId").value = "null";
}

function addJobNameToMessage(data) {
	var message = $('#label_srch_message').html();
	
	message += " for " + data.name
	$('#label_srch_message').html(message);

}

$('#btFilterJobHistory').on('click',
	function() {
		$('#err_label_date').hide();
		
		jobID     = $('#drpdn_jobName').val();
		startTime = $('#startTime').val();
		endTime   = $('#endTime').val();

		LASLogger.instrEvent('application.job_history.filter', {
			jobID : jobID,
			startTime : startTime,
			endTime : endTime
		});	

		var dtRegex = new RegExp(/(^\d{4}-\d{1,2}-\d{1,2})( )(0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9]){2}$/);
		if ((startTime != "" && !dtRegex.test(startTime))|| 
			(endTime != "" && !dtRegex.test(endTime))) {
			$('#err_label_date').show();
			return false;
		}
		
		refresh();
	});

function editJob(jobId) {
	window.location =openke.global.Common.getPageURLPrefix()+"/addEditJob?jobId=" + jobId;
}
function isSafari() {
	if (navigator.userAgent.indexOf('Safari') != -1
			&& navigator.userAgent.indexOf('Chrome') == -1) // If Safari, return
		// true
		return true;
	else
		// If another browser,
		return false;
}