LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

function goBack() {
	LASLogger.instrEvent('application.feedback.cancel');

	window.location =openke.global.Common.getPageURLPrefix();
}

// Job-Id will be null if creating new job.
var jobId = null;

var PAGE = "add_job"; // Page type. Whether it is a new job(add job) or edit existing job.

var cronField;

var loadedJobRecord;  // stores the job record from the server.

var adjudicationObject = { "utilize" : false }  
var adjudicationAnswers = []

$(document).ready(function() {
    LASLogger.instrEvent('application.feedback.onPage');

    document.getElementById("err_label").style.display = "none";
	
    $('#btnSubmit').on('click', submitForm);
	$('#btnCancel').on('click', goBack);
	
});



function submitForm() {
	document.getElementById("err_label").style.display = "none";
	
	if ($('#txtSubject').val() == null || $('#txtSubject').val().trim() == "") {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Please enter a subject.";

		$("#txtSubject").focus();
		return false;
	}
	
	if ($('#txtComments').val() == null || $('#txtComments').val().trim() == "") {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Please enter your comments.";

		$("#txtComments").focus();
		return false;
	}	
	
	var commentJSON = {
			"subject"  :  $('#txtSubject').val().trim(),
			"comments" : $('#txtComments').val().trim()
	}
	    
	LASLogger.instrEvent('application.feedback.submit', commentJSON);			
	

    $.ajax({
		type : "POST",
		url : openke.global.Common.getRestURLPrefix()+"/feedback",
		data: JSON.stringify(commentJSON),
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			bootbox.alert("Feedback submitted");
			$('#txtSubject').val("")
			$('#txtComments').val("")
			return false;
		},
		error : function(data) {
			document.getElementById("err_label").innerHTML = "Unable to submit feedback";
			document.getElementById("err_label").style.display = "inline";
		},
	});
	return false;
}


