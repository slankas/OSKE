/*
 applications scripts for managingSearchAlerts.
 
 Changed to support both a list of SearchAlerts, plus viewing alert ntoifications for a particular item
 */

var cronField;
$(document).ready(function() {
    LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);
    LASLogger.instrEvent('application.searchAlert.onPage', {});

    $("#searchAlertRow").hide();
    
    var tblSourceHandlers = $('#tblAlerts').DataTable({
        "pageLength" : 25,
        "lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
        "dom": 'p',
        "buttons": [
            'excel','print'
        ],
        "columns" : [ {
            "data" : "actions",
            "width" : "0"
        }, {
            "data" : "alertName",
            "width" : "20%"
        }, {
            "data" : "configuration",
            "width" : "20%"
        }, {
            "data" : "ownerEmailId",
            "width" : "0"
        }, {
            "data" : "schedule",
            "width" : "0"
        }, {
            "data" : "dateLastRun",
            "width" : "0"
        },{
            "data" : "cronNextRun",
            "width" : "0"
        }, {
            "data" : "state",
            "width" : "0"
        }
        ],
        "order" : [ [ 1, "asc" ] ]
    });
    
    //Establish the cron field component
	cronField = $('#searchAlertSchedule').cron({
	    initial: "0 0 */8 * * ?",
	    customValues: {
	        "2 hours" : "0 0 */2 * * ?",
	        "4 hours" : "0 0 */4 * * ?",
	        "8 hours" : "0 0 */8 * * ?",
	        "12 hours" : "0 0 */12 * * ?",
	        "2 days at 0700" : "0 0 7 */2 * ?"
	    },
	    useGentleSelect: true,
	    effectOpts: {
	        openSpeed: 200,
	        closeSpeed: 200
	    }
	});			


    //setup event handling
    $("#btViewAllNotifications").click(viewAllNotifications);
    $("#btAcknowledgeAll").click(acknowledgeAllAlerts);
    $("#cbViewAllAlerts").change(refreshTables);
    $("#btCreateSearchAlert").click(createSearchAlert);
    $("#btnOpenSearchAlertDialog").click(function() {$('#searchAlertModal').modal('show');});
	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.searchAlert.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});


	$('#searchAPI').change(function() { 
		var label = $('#searchAPI option:selected').attr("data-primarylabel");
		$('#searchTermsLabel').text(label+":");		
	});
	
    refreshTables();

});

function refreshTables() {
	
	var showAll=false
	if ($("#cbViewAllAlerts").is(":checked")) {
		showAll=true
	}
	LASLogger.instrEvent('application.searchAlert.showSearchAlerts', { "showAll": showAll});
	
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchAlert?showAll="+showAll, refreshAlertTable);
}

function refreshAlertTable(data) {

    var table = $('#tblAlerts').DataTable();
    table.clear();

    for (var i = 0; i < data.searchAlerts.length; i++) {
        var newRow = data.searchAlerts[i];

        var viewAction   = "<span onclick='viewAlertNotifications(\""+ newRow.alertId +"\");' class='fas fa-list' data-toggle='tooltip' data-original-title='View notifications'></span>";
        var playAction   = "<span onclick='changeAlertState(\""+ newRow.alertId +"\",\"waiting\");' class='fas fa-play-circle' data-toggle='tooltip' data-original-title='Start generating notifications'></span>";
        var pauseAction  = "<span onclick='changeAlertState(\""+ newRow.alertId +"\",\"paused\");' class='fas fa-pause' data-toggle='tooltip' data-original-title='Pause notifications'></span>";
        var deleteAction = "<span onclick='deleteAlert(\""+ newRow.alertId +"\", this);' class='fas fa-trash-alt' data-toggle='tooltip' data-original-title='Delete alert'></span>";
        
        newRow.actions =  viewAction + "&nbsp;&nbsp;&nbsp;";
        if (newRow.state === "paused") {
        	newRow.actions += playAction;
        }
        else {
        	newRow.actions += pauseAction;
        }
        newRow.actions += "&nbsp;&nbsp;&nbsp;" + deleteAction;
        	
        newRow.configuration = newRow.sourceHandler+"<br>"+newRow.searchTerm
        newRow.schedule = cronstrue.toString(newRow.cronSchedule)
        
        table.row.add(newRow);
    }
    table.draw();
    $('[data-toggle="tooltip"]').tooltip({
        placement : 'top'
    });

}

function viewAlertNotifications(alertID, showAll="false") {
	LASLogger.instrEvent('application.searchAlert.showSearchAlertNotifications', { "alertID":alertID, "showAll": showAll});
	
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchAlert/"+alertID+"/notifications?showAll="+showAll, refreshAlertNotificationTable);
}

function refreshAlertNotificationTable(data) {
	$("#searchAlertList").empty();
    $("#searchAlertRow").show();
    
    $("#searchAlertList").attr("alertID", data.alertID)
    
    for (var i = 0; i < data.notifications.length; i++) {
    	var record = data.notifications[i];
		var acknowledgeAction = "<span onclick='acknowledgeAlert(this,\""+record.alertID+"\",\""+record.resultURL+"\")' class='fas fa-times'></span>";
        
		var rec = new openke.component.ResultObject("", record.resultTitle, record.resultURL, record.resultDescription, {}, true, true);	
		if (record.acknowledgement == false) {
			rec.getRecordDOM().find(".relevancyCell").append(acknowledgeAction);
		}
		rec.getRecordDOM().find(".message").append(record.resultTimestamp);
		rec.getRecordDOM().find("div.title").attr("alertID",record.alertID);
		
		$('#searchAlertList').append(rec.getRecordDOM());
		rec.displayRecord();
    }
}

function viewAllNotifications() {
	viewAlertNotifications( $("#searchAlertList").attr("alertID"),"true");
}


function acknowledgeAlert(obj, alertID, resultURL){
	LASLogger.instrEvent('application.searchAlert.acknowledgeSearchAlertNotification', { "alertID":alertID, "url": resultURL});
	$(obj).parents('tr').remove();
    
	var url = openke.global.Common.getRestURLPrefix()+"/searchAlert/" + alertID ;
	var data = { "action" : "acknowledge", 
			     "url": resultURL};
    $.ajax({
		type :    "POST",
		dataType: "json",
		data: JSON.stringify(data),
		contentType: "application/json; charset=utf-8",
		url : url,
		success : function(data) {
			if (data.status === "failed") {
				bootbox.alert({
					title: "Acknowledgement Error",
					message: data.message
				})
			}
		},
		error : function(data) {
			bootbox.alert({
				title: "Acknowledgement Error",
				message: "Unable to acknowledge error"
			})

		}
	});  
}

function acknowledgeAllAlerts(){
	LASLogger.instrEvent('application.searchAlert.acknowledgeAllSearchAlertNotification', { "alertID":alertID});
	$("div#searchAlertList div.title").each(function() {
		
		var url = $(this).find("a").attr('href');
		var alertID = $(this).attr("alertID");
		acknowledgeAlert(this, alertID,url);
	});
	
}




function changeAlertState(alertID, newState) {
	LASLogger.instrEvent('application.searchAlert.changeSearchAlertState', { "alertID":alertID,"state": newState});
	
	var url = openke.global.Common.getRestURLPrefix()+"/searchAlert/" + alertID ;
	var data = { "action" : "changeState", 
			     "state": newState};
    $.ajax({
		type :    "POST",
		dataType: "json",
		data: JSON.stringify(data),
		contentType: "application/json; charset=utf-8",
		url : url,
		success : function(data) {
			if (data.status === "failed") {
				bootbox.alert({
					title: "Unable to change state",
					message: data.message
				})
			}
			else {
				refreshTables();
			}
		},
		error : function(data) {
			bootbox.alert({
				title: "Unable to change state",
				message: "Unknown error: "+data
			})

		}
	});  	
}

function deleteAlert(alertID, element) {
	bootbox.confirm({ 
		size: 'medium',
		title:'Delete Alert',
		message: "Are you sure want to delete this alert?  This action cannot be reversed and any notification history will be lost.", 
		buttons:{
			'confirm': {
	            label: 'Delete'
			}
		},
		callback: function(result){ 
			if (result) {
				LASLogger.instrEvent('application.searchAlert.deleteAlert', { "alertID":alertID});
			    $.ajax({
			        contentType: "application/json; charset=utf-8",
			        type : "DELETE",
			        url : openke.global.Common.getRestURLPrefix()+"/searchAlert/"+alertID,
			        dataType: 'json',
			        success: function(s){
			        	$('#tblAlerts').DataTable().row( $(element).parents('tr') ).remove().draw();
			        	if ( $("#searchAlertList").attr("alertID") === alertID) {
			        		$("#searchAlertRow").hide(); 
			        	}
			            if(s.status !=="success") {
			                bootbox.alert("Unable to delete alert: "+s.message);
			            }
			        },
			        error: function(e){
			            alert(e.status);
			        }
			    })
			    
			}
			else {
				LASLogger.instrEvent('application.alertNotification.deleteAlertCancelled', { alertID: alertID});
			}
		}


    });

}


function createSearchAlert() {
	
	if ($("#searchAlertName").val().trim() === "") {
		bootbox.alert("You must enter a search alert name.")
		return 
	}

	if ($("#searchAlertName").val().trim().length > 256) {
		bootbox.alert("The search alert name must be less than 256 characters.")
		return 
	}
	
	
	if ($("#searchTerms").val().trim() === "") {
		bootbox.alert("You must enter search terms / URL.")
		return 
	}		
	
	var numResults = $('#numberOfsearchResults').val();
	if (isPositiveInteger(numResults) == false) {
		bootbox.alert("Number of search results must be an integer greater than zero.")
		return;
	}
	if (numResults > 100) {
			bootbox.alert("Maximum of 100 search results.  Value changed to 100.");
			$('#numberOfsearchResults').val("100");
			return;
	}	
	
	
    var alertPost = {
            alertName : $("#searchAlertName").val().trim(),
            searchTerm : $("#searchTerms").val().trim(),
            cronSchedule : cronField.cron("value"),
            numberOfSearchResults : numResults,
    		sourceHandler : $("#searchAPI").val(),
    		preacknowledge: $("#preacknowledge").is(":checked")
        };

    $('#searchAlertModal').modal('hide');
    
    $.ajax({
            contentType: "application/json; charset=utf-8",
            type : "POST",
            url : openke.global.Common.getRestURLPrefix()+"/searchAlert",
            data : JSON.stringify(alertPost),
            dataType: 'json',
            success: function(s){
            	
                if (s.status==="success") {
                	alertPost.alertID = s.alertID;
                	LASLogger.instrEvent('application.alertNotification.searchAlertCcreated', alertPost);
                	refreshTables()
                }
                else {
                	bootbox.alert("Unable to create search alert")
                }
                return;
            },
            error: function(e){
            	console.log(e.status);
                bootbox.alert("Unable to create search alert")
    			return 
            }
        });	    		
	
	
}

