/**
 * page script for a user to submit an agreement form.
 */

LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var userAgreementForm;

$(document).ready(function() {
	LASLogger.instrEvent('application.userAgreement.startPage');
	
	if ($('#uaMessage').length) {
		bootbox.alert($('#uaMessage').val())
	}
	
    $.getJSON(openke.global.Common.getContextRoot()+"rest/system/userAgreement/text", populateUserAgreementDetails);
});




function populateUserAgreementDetails(data){
    if (data.status == "error") {
        bootbox.alert("Unable to access latest user agreement form.")
        return;
    }
    userAgreementForm = data.form;

    // display the agreement text
    document.getElementById("agreementText").innerHTML = data.form.agreementText ;
    
    // display the required readings
    if (data.form.readingText.length > 0) {
    	var requiredReading = "<h2>Required Readings</h2><ul>"
    	
    	for (var i=0; i < data.form.readingText.length; i++) {
    		var rrObj = data.form.readingText[i];
    		var text = "<li><a target=_blank href='"+ rrObj.hyperlink +"'>"+ rrObj.name +"</a>"
    		requiredReading += text
    	}
    		
    	requiredReading += "</ul>"
    	document.getElementById("requiredReadings").innerHTML = requiredReading;
    }

    // populate the agreement form questions
    var questionElement = document.getElementById("questions");
    for (var i = 0; i< data.form.questions.length;i++){
    	var questionObj = data.form.questions[i];
    	
    	var questionHTML =  "<div class='form-inline'><label style='font-weight:bold !important;' for='qst_"+questionObj.questionName+"'>"+questionObj.questionText+"&nbsp;</label>"
    	
    	if (questionObj.questionType === "textfield") {
    		questionHTML += "<input type='textfield' class='form-control' id='qst_"+questionObj.questionName+"' size="+questionObj.length+">"
    	}
    	else if (questionObj.questionType === "textarea") {
    		questionHTML += "<textarea style='display:block;' class='form-control' id='qst_"+questionObj.questionName+"' rows=3 cols=80></textarea>"
    	}
    	else {
    		
    	}
    	questionHTML += "</div>";
    	    	
        questions.innerHTML += questionHTML;
    }
    $.getJSON(openke.global.Common.getContextRoot()+"rest/system/userAgreement/user/"+$("#author").val(), populateUserAgreementData);
}

function populateUserAgreementData(data) {
	if (data.status !== "error" && data.agreement.agreementVersion == userAgreementForm.versionNumber) {
        for (var key in data.agreement.answers) {
            if (data.agreement.answers.hasOwnProperty(key)) {
            	$('#qst_'+key).val(data.agreement.answers[key])
            }
        }   
	}
	
}


function signUserAgreement() {
	var agreementForm = {
		"organization"  : document.getElementById("tfUserOrganization").value.trim(),
		"signatureName" : document.getElementById("tfUserName").value.trim(),
		"questions" : {}
	}

	if (agreementForm.organization === "") {
		bootbox.alert("You must enter an organization.");
		return false;
	}

	if (agreementForm.signatureName === "") {
		bootbox.alert("You must enter your full name as the signature");
		return false;
	}
	
    for (var i = 0; i < userAgreementForm.questions.length; i++){
    	var questionObj = userAgreementForm.questions[i];
    	var fieldName = "qst_"+questionObj.questionName;
    	
    	var value = document.getElementById(fieldName).value.trim()
    	if (value === "") {
    		bootbox.alert("You must enter a value: "+questionObj.questionText);
    		return false;
    	}
    	
    	agreementForm.questions[questionObj.questionName] = value;
    }
    
    bootbox.confirm({
        title: "Sign Agreement",
        message: "By clicking \"I Agree\", you acknowledge and agree that your electronic signature (through entering your full name) is legally equivalent to your handwritten signature.  If you do not agree to this, click \"cancel\". You may submit a hand-signed user agreement form to a member of the OpenKE team to gain access to the system.",
        buttons: {
            cancel: {
                label: '<i class="fa fa-times"></i> Cancel'
            },
            confirm: {
                label: '<i class="fa fa-check"></i> I Agree'
            }
        },
        callback: function (result) {
        	if (result === true) {
        	    var userID = document.getElementById("author").value;  
        	    
        	    LASLogger.instrEvent('application.userAgreement.submit', agreementForm);
        	    
        	    $.ajax({
        	        contentType: "application/json; charset=utf-8",
        	        type : "POST",
        	        url : openke.global.Common.getContextRoot()+"rest/system/userAgreement/user/"+userID,
        	        data : JSON.stringify(agreementForm),
        	        dataType: 'json',
        	        success: function(s){
        	            if (s.status==="success"){
        	                window.location = openke.global.Common.getContextRoot();
        	            }
        	            else {
        	            	bootbox.alert(s.message);
        	            }
        	            return false;
        	        },
        	        error: function(e) {
        	            alert(e.status);
        	        }
        	    });
       		
        	}
        	else {
        		bootbox.alert("Your form was not submitted.")
        	}
        }
    });
    
    


}