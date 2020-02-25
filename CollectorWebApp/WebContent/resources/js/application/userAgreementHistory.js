/**
 * 
 */
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

var userAgreements = [];  // array of the the user agreements submitted by the current user

$(document).ready(function() {
	LASLogger.instrEvent('application.userAgreementHistory.startPage');
	
    var tblSourceHandlers = $('#tblUsrAgrHis').DataTable({
        "pageLength": 10,
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        //"dom": 'pftilB',
        "columns": [{
            "data": "agreementTimestamp",
            "width": 10
        }, {
            "data": "expirationTimestamp",
            "width": 10
        }, {
            "data": "statusTimestamp",
            "width": 10
        }, {
            "data": "status",
            "width": 10
        }, {
            "data": "adjudicatorEmailID",
            "width": 10
        }, {
            "data": "adjudicatorComments",
            "width": 10
        }
        ],
        "order" : [ [ 0, "asc" ] ]
    });

    $('#btnHome').on('click', function() {
    	LASLogger.instrEvent('application.userAgreementHistory.gotoApplicationHome');
    	window.location = openke.global.Common.getContextRoot();
    });

    refreshTables();

});

function refreshTables() {
    $.getJSON(openke.global.Common.getContextRoot()+"rest/system/userAgreement/user/"+$("#author").val()+"?complete=true", refreshUserAgreementHistoryTable);
}

var dataFromTable;

function refreshUserAgreementHistoryTable(data) {
	if (data.status != "success") {
		bootbox.alert("Unable to load your agreements.");
		return;
	}
	
	userAgreements = data.agreements;
	
    var table = $('#tblUsrAgrHis').DataTable();
    table.clear();

    for (var i = 0; i < userAgreements.length; i++) {
        var newRow = userAgreements[i];
        newRow.agreementTimestamp = '<a  href="#" onclick="viewForm(\''+i+'\')">'+newRow.agreementTimestamp+'</a>';
        table.row.add(newRow);
    }
    table.draw();
}

function viewForm(i) { 
	$.getJSON(openke.global.Common.getContextRoot()+"rest/system/userAgreement/text/"+userAgreements[i].agreementVersion, function(data) {
		displayForm(data.form,userAgreements[i])
	});
}

function displayForm(formObject, agreement){
	
    var css = '<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/bootstrap/css/bootstrap.css" />' +
	  		'<link rel="stylesheet" type="text/css" href="'+openke.global.Common.getContextRoot()+'resources/css/demonstrator.css" />';

    var js =  '<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/jquery-3.1.1.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/bootstrap/js/bootstrap.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/external/bootbox.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/LASLogger.js"></script>' +
    	'<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/js/application/common.js"></script>';
    
	var screen_width = screen.width * .5;
    var screen_height = screen.height* .8;
    var top_loc = screen.height *.05;
    var left_loc = screen.width *.1;

    var aWin = window.open("",'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
    
    aWin.document.write("<html><head>");
    aWin.document.write('<meta charset="utf-8" /><meta name="viewport" content="width=device-width, initial-scale=1.0" /><title>User Agreement</title>');
    aWin.document.write('<link rel="SHORTCUT ICON" href="'+openke.global.Common.getContextRoot()+'resources/images/LAS_Logo.ico">');
    aWin.document.write(css);
    aWin.document.write("</head><body>");
    
    aWin.document.write('<div style="margin: 0px 10px 0px 10px;">');
    aWin.document.write('<div class="row">'+formObject.agreementText+'</div>')
    
    
    if (formObject.readingText.length > 0) {
    	var requiredReading = "<h2>Required Readings</h2><ul>"
    	
    	for (var i=0; i < data.form.readingText.length; i++) {
    		var rrObj = data.form.readingText[i];
    		var text = "<li><a target=_blank href='"+ rrObj.hyperlink +"'>"+ rrObj.name +"</a>"
    		requiredReading += text
    	}
    		
    	requiredReading += "</ul>"
    	aWin.document.write('<div class="row">'+ requiredReading+'</div>')
    }

    // populate the agreement form questions
    var questionElementString ="";
    for (var i = 0; i< formObject.questions.length;i++){
    	var questionObj = formObject.questions[i];
    	
    	var questionHTML =  "<div class='form-inline'><label style='font-weight:bold !important;' for='qst_"+questionObj.questionName+"'>"+questionObj.questionText+"&nbsp;</label>"
    	
    	var answer = escapeHtml(agreement.answers[questionObj.questionName])
    	
    	if (questionObj.questionType === "textfield") {
    		questionHTML += "<input readonly type='textfield' class='form-control' id='qst_"+questionObj.questionName+"' size="+questionObj.length+" value='"+ answer +"'>"
    	}
    	else if (questionObj.questionType === "textarea") {
    		questionHTML += "<textarea readonly style='display:block;' class='form-control' id='qst_"+questionObj.questionName+"' rows=3 cols=80>"+answer+"</textarea>"
    	}
    	else {
    		
    	}
    	questionHTML += "</div>";
    	    	
    	questionElementString += questionHTML;
    }
    aWin.document.write('<div class="row">'+ questionElementString+'</div>')

    aWin.document.write("<div class='row'>&nbsp; <div class='form-inline'><label style='font-weight:bold !important;' for='tfUserOrganization'>Organization</label>");
    aWin.document.write("<input readonly type='text' class='form-control' id='tfUserOrganization' value='"+ escapeHtml(agreement.userOrganization) +"' size=50></div></div");
    aWin.document.write("<div class='row'>&nbsp;<div class='form-inline'> <label style='font-weight:bold !important;' for='tfUserName'>Name</label>");
    aWin.document.write("<input readonly type='text' class='form-control' id='tfUserName' size=50 value='"+escapeHtml(agreement.userSignature)+"'></div></div>");
    
    aWin.document.write("<div class='row'>&nbsp; </div></body>");
    aWin.document.write(js);
    aWin.document.write("</html>");

    aWin.focus();

}	
	
	
	