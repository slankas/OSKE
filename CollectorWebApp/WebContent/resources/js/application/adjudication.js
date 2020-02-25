LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var adjudicationObject = { "utilize" : false }  
var loadedJobsData = [];

$(document).ready(function() {
		LASLogger.instrEvent('application.adjudicateJob');
		
		document.getElementById("err_label_user").style.display = "none";
		var tblSourceHandlers = $('#tblJobs').DataTable({
			"pageLength" : 50,
			"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
			"dom": 'pftilB',
	        "buttons": [
	            'excel','print'
	        ],
			"columns" : [ {
				"data" : "name",
				"width" : "0"
			}, {
				"data" : "primaryFieldValue",
				"width" : "0"
			}, {
				"data" : "sourceHandler",
				"width" : "1%"
			}, {
				"data" : "schedule",
				"width" : "10%"
			}, {
				"data" : "configuration",
				"width" : "30%" 
			}, {
				"data" : "randomPercent",
				"width" : "0"
			}, {
				"data" : "statusTimestamp",
				"className" : "nowrap"
			},{
				"data" : "ownerEmail",
				"width" : "0"
			},{
				"data" : "action",
				"width" : "0"
			}],
			"order" : [ [ 0, "asc" ] ]
		});
		configureAdjudicationQuestions();
		refreshTables();

	});


function configureAdjudicationQuestions() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/questions", function (data) {
		adjudicationObject = data;
	});
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

function refreshTables() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/jobs/adjudication", refreshJobsTable);
}

function refreshJobsTable(data) {
	loadedJobsData = data.jobs;
	
	var table = $('#tblJobs').DataTable();
	table.clear();
	for (var i = 0; i < data.jobs.length; i++) {
		var newRow = data.jobs[i];

		var prettyConfig = escapeHtmlNewLine2(JSON.stringify(newRow.configuration, null, '\t'));
		var c = "<div class=scrollable>" + prettyConfig + "</div>";
		var jobId = '"' + newRow.id + '"';
		newRow.configuration = c;
		newRow.schedule = cronDisplayText(newRow.cronSchedule) + "<br>" +newRow.cronSchedule;
		newRow.action = "<button style='margin-right:20px;' class='btn-sm btn-primary' id='btnApproveJob' onclick='return editStatus(" + jobId + ",\"approve\")'>Approve</button>" +
		                "<br><br><button style='margin-right:20px;' class='btn-sm btn-primary' id='btnApproveAndRunJob' onclick='return editStatus(" + jobId + ",\"approve\",true)'><nobr>Approve &amp; Run Now</nobr></button>" +
						"<br><br><button style='margin-right:20px;' class='btn-sm btn-warning' id='btnDisapproveJob' onclick='return editStatus(" + jobId + ",\"disapprove\")'>Disapprove</button>" +
 		                "<br><br><button style='margin-right:20px;' class='btn-sm btn-danger' id='btnDeleteJob' onclick='return deleteJob(\""+btoa(newRow.name)+"\"," + jobId + ")'>Delete</button>";
		
		//primaryFieldValue
		if (adjudicationObject.utilize) {
			newRow.primaryFieldValue = newRow.primaryFieldValue + "<br><a href='javascript: displayAnswers("+i+"); '>View Adjudication Answers</a>"
		}
		
		table.row.add(newRow);
	}
	table.draw();
}

// called when the user clicks the button in the table to approve or disapprove.
function editStatus(jobID,action,runNow=false) {
	if (jobID == null) {
		return false;
	}
	else { 
		var comment = "";
		
		if(action=="disapprove"){
			$('#commentModal').modal('show')                // initializes and invokes show immediately
			$('#btUpdateComment').on('click',function() {
				$('#commentModal').modal('hide')
				comment = $('#txtComments').val();
				if(comment=="") {
					comment="Disapproved for unknown reasons";
				}
				comment += " by "+ $('#author').val() + ". ";
				var data = {
						action: "disapprove",
						comment: comment
				}
				callAdjudication(jobID, data, false);
			});
		}
		else {
			var data = {
					action: "approve",
					comment: " Approved by "+ $('#author').val()
			}
			callAdjudication(jobID, data, runNow);
		}
	}
    return false;
}
function callAdjudication(jobID, postData, runNowFlag){
	
    $.ajax({
		type : "PUT",
		url : openke.global.Common.getRestURLPrefix()+"/jobs/adjudication/"+jobID,
		data: JSON.stringify(postData),
		contentType: "application/json; charset=utf-8",
		success : function(data) {
				if (runNowFlag) {
					var url = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobID + "/run";
				    
					LASLogger.instrEvent('application.adjudicateJob.status', { 
						url : url, 
						uuid : jobID
					});	
				    $.ajax({
						type : "POST",
						url : url,
						success : function(data) {
							bootbox.alert("Job has been approved and set to run");
						},
						error : function(data) {
							bootbox.alert("Job has approved, but unable to set the state to run");
						},
						dataType : "text",
					});
				}
				else {
					bootbox.alert("Job status changed to "+data.result+"<P>&nbsp;<p>Comments:<P>"+postData.comment)
				} 
				$('#txtComments').val("");
				refreshTables();			
					
				return false;
		},
		error : function(data) {
			document.getElementById("err_label_user").style.display = "inline";
			document.getElementById("err_label_user").innerHTML = jQuery.parseJSON(data.responseText).reason;
		}
	});
}

function deleteJob(jobName, jobID) {
	jobName = atob(jobName)
	bootbox.prompt({
	    title: "Delete Job - " + jobName+"?<br>Enter the rationale to delete this job.<br>All collected data will be permamently removed.  Job record will no longer be visible.",
	    message: "not visible",
	    inputType: 'textarea',
	    callback: function (message) {
	    	if (message) {
	    		message = message.trim();
	    		if (message.length == 0) {return;}
	    		
	    		LASLogger.instrEvent('application.adjudicateJob.deleteJob'  , { 
	    			rationale: message,
	    			uuid : jobID
	    		});			
	    		
	    		
	    		var dataObject = {
	    				rationale: message
	    		}
	    		var purgeURL = openke.global.Common.getRestURLPrefix()+"/jobs/" + jobID +"/delete"	
	    	    $.ajax({
	    			type :    "POST",
	    			dataType: "json",
	    			data:     JSON.stringify(dataObject),
	    			url : 	   purgeURL,
	    		    contentType: "application/json; charset=utf-8",
	    			success : function(data) {
	    				refreshTables();
	    				bootbox.alert({
	    					title: "Delete Job Initiated",
	    					message: "You will receive an email when the deletion is complete."
	    				});
	    			},
	    			error : function(data) {
	    				bootbox.alert({
	    					title: "Unable to delete Job",
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


function displayAnswers(index) {
	var adjudicationAnswers = loadedJobsData[index].adjudicationAnswers;
	
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
		questionHTML += "<textarea readonly style='display:block;' class='form-control' id='adjQST_"+i+"' rows=3 cols=80>"+answer+"</textarea>"
		
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