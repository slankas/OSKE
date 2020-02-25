LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);
var startTime = $('#startTime').val();
var endTime = $('#endTime').val();
var jobID = $('#drpdn_jobName').val();
var jobHistoryID = $('#jobHistoryID').val();


$(document).ready(
		function() {
			LASLogger.instrEvent('application.visit_pages', {
				jobHistoryID : jobHistoryID
			});

			window.page = "visited_pages"; // Page name to be stored in window object so that LasHeader.js could access it for logging

			$(document).on({
			    ajaxStart: function() { 		
			    	document.getElementById("imgLoading").style.display = "inline";     
			    	},
			     ajaxStop: function() { 					    	
			    	 document.getElementById("imgLoading").style.display = "none";     
			     }    
			});		
			
			var tblVisitedPageData = $('#tblVisitedPages').DataTable({
				"pageLength" : 50,
				"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
				"language": {
		            "zeroRecords": "No records found",
		            "infoEmpty": ""
				},
				"dom": "iptlf",
		        "buttons": [
		            'excel','print'
		        ],
				"columns" : [ {
					"data" : "url",
					"width": "40%"
				}, {
					"data" : "action",
					"width" : "5%"
				}, {
					"data" : "mimeType",
					"width": "10%"
				}, {
					"data" : "visitedTimeStamp",
					"className" : "nowrap",
					"type" : "date-range",
					"width" : "8%"
				}, {
					"data" : "status",
					"width" : "2%"
				}],
				"order" : [ [ 3, "desc" ] ]
			});

			$('#err_label_user').hide();

			$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs", function(data) {
				for (var i = 0; i < data.jobs.length; i++) {
					$("#drpdn_jobName").append("<option value=\"" + data.jobs[i].id + "\">"+ data.jobs[i].name + "</option>");
				}
			});
			
			$("#startTime").datetimepicker({
				format : 'Y-m-d H:i:s'
			});
			$("#endTime").datetimepicker({
				format : 'Y-m-d H:i:s'
			});
			
			$('#btFilterJobHistory').on('click',
				function() {
					$('#err_label_user').hide();
					
					jobID = $('#drpdn_jobName').val();
					startTime = $('#startTime').val();
					endTime = $('#endTime').val();
					
			    	LASLogger.instrEvent('application.visited_pages.filter', {
			    		jobID : jobID,
			    		startTime : startTime,
			    		endTime : endTime
			    	});

					var dtRegex = new RegExp(/(^\d{4}\d{1,2}\d{1,2})( )(0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9]){2}$/);
					if ((startTime != "" && !dtRegex.test(startTime))|| 
						(endTime != "" && !dtRegex.test(endTime))) {
						$('#err_label_date').show();
						return false;
					}
					if(startTime!="" || endTime!="" || jobID!=""){
						jobHistoryID = $('#jobHistoryID').val();
						$('#jobHistoryID').val("");
						$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages?startTime=" + startTime + "&&endTime=" + endTime + "&&jobID=" + jobID,	refreshVisitedPages);
					}
					else{
						$('#jobHistoryID').val(jobHistoryID);
						refreshTables();
						}
				});
			refreshTables();
			$("#btnDomainHome").on('click', function() {
				var completeLocation =openke.global.Common.getPageURLPrefix() +"/"
				LASLogger.instrEvent('application.visitedPages.link', {link : completeLocation},function() {window.location = completeLocation;} );
			});
		});

function refreshTables() {
	jobID= getUrlParameter("jobId");
	if($('#jobHistoryID').val()!="") {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages?jobHistoryID=" +$('#jobHistoryID').val(),refreshVisitedPages);
	}
	else if (jobID && jobID != "") {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages?jobID=" +jobID,refreshVisitedPages);
	}	
	else
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/visitedPages",refreshVisitedPages);
}

function refreshVisitedPages(data) {
	
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
			newRow.action += " " + "<a target='_blank' href= '" + newRow.url + "'>"	+ "Live" + "</a>";
		}
		
		newRow.url = newRow.url.match(new RegExp('.{1,100}', 'g')).join("\n"); //split up long URLs for display.  must come after defining the action

		
		table.row.add(newRow);
	}
	table.draw();
	
	//Need to update the message
	message = "Visited pages";
	if($('#jobHistoryID').val()!="") {
		message += " for job #"+ $('#jobHistoryID').val(); //TODO: Show more details for this.  Requires a json call
	}
	if (startTime != "") {
		if (endTime != "") {
			message += " between "+ startTime + " and " + endTime;
		}
		else {
			message += " since " + startTime;
		}
	}
	else if (endTime != "") {
		message += " before " + endTime;
	} 
	
	if(startTime=="" && endTime=="" && jobID=="" && $('#jobHistoryID').val()==""){
		message += " in the past 24 hours";
	}
	
	$('#label_srch').html(message);
	if (jobID && jobID != "") {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/"+jobID, addJobNameToMessage);
	}	
}
function addJobNameToMessage(data) {
	var message = $('#label_srch').html();
	
	message += " for " + data.name
	$('#label_srch').html(message);

}



function getUrlParameter(sParam) {
    var sPageURL = decodeURIComponent(window.location.search.substring(1)),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
}
