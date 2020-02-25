/**
 * 
 */
"use strict";

var userAgreementRecords;  // JSON array of the user agreements

/*
var timestampOfUser = [];
*/


$(document).ready(function() {
    LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);
    LASLogger.instrEvent('application.userAgreementAdjudicator.startPage');

    var tblUserAgreements = $('#tblUserAgreements').DataTable({
        "pageLength" : 50,
        "lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
        "dom": 'p',
        "columns" : [ {
            "data" : "emailId",
            "width" : "20%"
        }, {
            "data" : "status",
            "width" : "10%"
        }, {
            "data" : "agreementTimestamp",
            "width" : 100
        }, {
            "data" : "expirationTimestamp",
            "width" : 100
        }, {
            "data" : "decision",
            "width" : "40%"
        }
        ],
        "order" : [ [ 0, "asc" ] ]
    });

    $('#btnViewAllRecords').on('click', function() {
    	LASLogger.instrEvent('application.userAgreementAdjudicator.viewAllRecords');
    	refreshTable("?complete=true")
    })
    
    refreshTable();
});

function refreshTable(argument = "") {
    $.getJSON(openke.global.Common.getContextRoot()+"rest/system/userAgreement/user"+argument, refreshUserAgreementTable);
}



function refreshUserAgreementTable(data) {
    var table = $('#tblUserAgreements').DataTable();
    table.clear();
    
    userAgreementRecords = data.agreements;


    for (var i = 0; i < data.agreements.length; i++) {
        var record = data.agreements[i];

        record.decision = '<a  href="#" onclick="viewDetails(\''+i+'\')">View</a>&nbsp;&nbsp;'
        
        if (record.status === "review") {
        	record.decision += '<a href="#" onclick="approveUserAgreement(\''+i+'\')">Approve</a>&nbsp;&nbsp';
        	record.decision += '<a href="#" onclick="denyUserAgreement(\''+i+'\')">Deny</a>&nbsp;&nbsp';
        	record.decision += '<a href="#" onclick="reworkUserAgreement(\''+i+'\')">Rework</a>&nbsp;&nbsp';
        } 
        else if (record.status === "approved") {
        	record.decision += '<a href="#" onclick="changeExpirationDate(\''+i+'\')">Change Expiration Date</a>&nbsp;&nbsp';
        	record.decision += '<a href="#" onclick="revokeUserAgreement(\''+i+'\')">Revoke</a>&nbsp;&nbsp';
        } 
       
        table.row.add(record);
    }
    table.draw();

}


function viewDetails(i){
	var record  = userAgreementRecords[i]
    var version = record.agreementVersion;

    $.ajax({
        contentType: "application/json; charset=utf-8",
        type : "GET",
        url : openke.global.Common.getContextRoot()+"rest/system/userAgreement/text/"+version,
        dataType: 'json',
        success: function(data){
        	if (data.status === "error") {
        		bootbox.alert(data.message);
        		return false;
        	}
        	
        	var message = "";
        	
        	for (var i = 0; i < data.form.questions.length; i++) {
        		var question = data.form.questions[i].questionText;
        		var answer   = record.answers[data.form.questions[i].questionName];
            	message += "<b>"+question+"</b><br>"+answer+"<p>&nbsp;<br>"
        	}
        	
            bootbox.alert({
                title: "User Agreement Questions/Answers",
                message: message
            });

        },
        error: function(e){
            alert(e.status);
        }
    });

}

function approveUserAgreement(i){
	var record  = userAgreementRecords[i];
	
	var dataRecord =  {
		action  : "approve",
		emailID : record.emailId,
		agreementTimestamp: record.agreementTimestamp
	}

	LASLogger.instrEvent('application.userAgreementAdjudicator.action_approve',dataRecord);
    $.ajax({
        contentType: "application/json; charset=utf-8",
        type : "PUT",
        url : openke.global.Common.getContextRoot()+"rest/system/userAgreement/user/"+record.emailId,
        data : JSON.stringify(dataRecord),
        dataType: 'json',
        success: function(s){
        	if (s.status == "error") {
        		bootbox.alert(s.message)
        		return false;
        	}
        	else {
        		bootbox.alert("User agreement has been approved.");
        	    refreshTable();
            }
        },
        error: function(e){
            alert(e.status);
        }
    });
}




function changeExpirationDate(i){

    bootbox.prompt({
        title: "Enter new expiration date for user:",
        inputType: 'date',
        callback: function (result) {
            if (result===null) {
                return;
            }

            result = result.split("-");
            var newDate = result[0]+"/"+result[1]+"/"+result[2];
            var expirationTimestamp = new Date(newDate).getTime();
        	
        	var record  = userAgreementRecords[i];        	
        	var dataRecord =  {
        		action  : "changeExpiration",
        		emailID : record.emailId,
        		agreementTimestamp: record.agreementTimestamp,
        		expirationTimestamp: expirationTimestamp
        	}

        	LASLogger.instrEvent('application.userAgreementAdjudicator.action_changeExpiration',dataRecord);
            $.ajax({
                contentType: "application/json; charset=utf-8",
                type : "PUT",
                url : openke.global.Common.getContextRoot()+"rest/system/userAgreement/user/"+record.emailId,
                data : JSON.stringify(dataRecord),
                dataType: 'json',
                success: function(s){
                	if (s.status == "error") {
                		bootbox.alert(s.message)
                		return false;
                	}
                	else {
                		bootbox.alert("User Agreement Expiration date changed");
                	    refreshTable();
                    }
                },
                error: function(e){
                    alert(e.status);
                }
            });
        }
    });
}

function denyUserAgreement(i){
    return performAction(i,"Enter your reason for denying this agreement:", "deny", "User agreement has been denied and the user notified.")
}

function reworkUserAgreement(i){
	return performAction(i, "Enter your reason why this agreement needs to be reworked:", "rework", "The user agreement has been sent back and the user notified.")
}

function revokeUserAgreement(i){
	return performAction(i, "Enter the reason for revoking this user agreement: ", "revoke", "The user agreement has been revoked")
}

function performAction(i, prompt, action, successMessage){
    bootbox.prompt(prompt, function(reason){
    	var record  = userAgreementRecords[i];
    	
    	var dataRecord =  {
    		action  : action,
    		emailID : record.emailId,
    		agreementTimestamp: record.agreementTimestamp,
    		adjudicatorComments: reason
    	}

    	LASLogger.instrEvent('application.userAgreementAdjudicator.action_'+action,dataRecord);
        $.ajax({
            contentType: "application/json; charset=utf-8",
            type : "PUT",
            url : openke.global.Common.getContextRoot()+"rest/system/userAgreement/user/"+record.emailId,
            data : JSON.stringify(dataRecord),
            dataType: 'json',
            success: function(s){
            	if (s.status == "error") {
            		bootbox.alert(s.message)
            		return false;
            	}
            	else {
            		bootbox.alert(successMessage);
            	    refreshTable();
                }
            },
            error: function(e){
                alert(e.status);
            }
        });
    });
}
