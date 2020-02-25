/**
 * 
 */

// start of initialization code
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

$(document).ready(function() {
	
	LASLogger.instrEvent('application.jobStatus');
	
	window.page = "jobStatus"; // Page name to be stored in window object so that LasHeader.js could access it for logging
	
		
	var table = $('#tblRecentJobs').DataTable({
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "jobName",
			"width" : "50%",
		}, {
			"data" : "status",
			"width" : "0",
		}, {
			"className" : "nowrap",
			"data" : "startTime",
			"width" : "0",
		}, {
			"data" : "processingTime",
			"width" : "0",
		}, {
			"data" : "numPageVisited",
			"width" : "0",
		}, {
			"data" : "action",
			"width" : "0",
		}, ],
		"order" : [ [ 2, "desc" ] ]
	});

	var table = $('#tblVisitedPages').DataTable({
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "url",
			"width" : "60%",
		}, {
			"data" : "status",
			"width" : "0",
		}, {
			"className" : "nowrap",
			"data" : "visitedTimeStamp",
			"width" : "0",
		}, {
			"data" : "action",
			"width" : "0",
		}, ],
		"order" : [ [ 2, "desc" ] ]
	});


	getRecentJobsTable();
	getVisitedPageTable();
	

	$("#btUploadFiles").on('click',  function() {	advancedCollectorNavigateTo('fileUpload');	    return false;	});
	$("#btViewHandlers").on('click', function() {	advancedCollectorNavigateTo('handlers');	    return false;	});
	$("#btViewAllJobs").on('click',  function() {	advancedCollectorNavigateTo('jobHistory');	    return false;	});
	$("#btViewAllPages").on('click', function() {	advancedCollectorNavigateTo('visitedPages');	return false;	});
	$("#btCollectorView").on('click', function() {	advancedCollectorNavigateTo('collector');	return false;	});

});

function advancedCollectorNavigateTo(location) {
	var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
	LASLogger.instrEvent('application.advancedCollectorView.link', {
		link : completeLocation
	}, function() {window.location = completeLocation;	});     
} 


function getVisitedPageTable() {
	 $.ajax({
	      type: "GET",
	      url: openke.global.Common.getRestURLPrefix()+"/visitedPages/recent",
	      success: refreshVisitedPageTable,
	  	  error: function (xhr, ajaxOptions, thrownError) {
	        if(xhr.status==403) {
	            $("#visitedPagesArea").hide()
	        }
	      }});
}

function getRecentJobsTable() {
	 $.ajax({
	      type: "GET",
	      url:  openke.global.Common.getRestURLPrefix()+"/jobsHistory/recent",
	      success: refreshRecentJobsTable,
	  	  error: function (xhr, ajaxOptions, thrownError) {
	        if(xhr.status==403) {
	            $("#recentJobsArea").hide()
	        }
	      }});
}

function stopJob(jobId) {
	LASLogger.instrEvent('application.home.stop_job', {
		jobID : jobId
	});

	if (jobId == null) {
		return false;
	}
	var url = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobId + "/stop"
    $.ajax({
		type : "POST",
		url : url,
		success : function(data) {
			var status = data;
			if (status != null && status != "") {
				
				bootbox.alert({
					title: "Status Changed",
					message: "Requested to stop job"
				})
				
				getRecentJobsTable();
				return false;
			}
		},
		error : function(data) {
			document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = data.responseJSON.reason;
		},
		dataType : "text",
	});
	
    return false;
}	
	

function refreshRecentJobsTable(data) {
	var table = $('#tblRecentJobs').DataTable();
	table.clear();
	for (var i = 0; i < data.jobHistory.length; i++) {
		newRow = data.jobHistory[i];

		newRow.action = "<a href= '"+openke.global.Common.getPageURLPrefix()+"/visitedPages?jobHistoryID="	+ newRow.jobHistoryID + "'>Visited Pages</a>";

		if (newRow.status === "processing" && newRow.jobName !== "DirectoryWatcher") {
			newRow.action += " <a href='javascript:stopJob(\"" + newRow.jobID + "\");'>Stop</a>";
		}

		table.row.add(newRow);

	}
	table.draw();
	
	$(window).trigger('resize');
}

function refreshVisitedPageTable(data) {
	var table = $('#tblVisitedPages').DataTable();
	table.clear();
	for (var i = 0; i < data.visitedPages.length; i++) {
		newRow = data.visitedPages[i];
		

		if (newRow.status != 'irrelevant') {
			var id = '' + newRow.id + '';
			var a = "<a target='_blank' href='"+openke.global.Common.getRestURLPrefix()+"/visitedPages/" + id + "/content' >Stored</a>";
			newRow.action = a;
		} else {
			newRow.action = '';
		}
		
 		if (newRow.url.substring(0,5) != "file:" && newRow.url.substring(0,5) != "mail:") {
 			newRow.action += " " + "<a target='_blank' href= '" + newRow.url + "'>" + "Original" + "</a>";
  		}		  		
 		
		newRow.url = newRow.url.match(new RegExp('.{1,60}', 'g')).join("\n"); //split up long URLs for display.  must come after defining the action
 			
		
		table.row.add(newRow);
	}
	table.draw();
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages/count", function(data) {
		$('#numberOfPagesVisited').html(data);
	});
	
	$(window).trigger('resize');
}


