
$(document).ready(function() {
	$('#btHome').click(function(){
		LASLogger.instrEvent('application.domainDiscovery.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
		
	});
	
	$('#btNewSession').click(function() {
		LASLogger.instrEvent('application.domainDiscovery.createNewSession', {}, function() {window.location=openke.global.Common.getPageURLPrefix()+'/domainDiscoverySession';});		
	})
	
	$("#sessions").DataTable({
		"pageLength" : 25,
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
				"data" : "sessionName",
				"width" : "51%"
			}, {
				"data" : "userID",
				"width" : "22%"
			}, {
				"data" : "creationDateTime",
				"className" : "nowrap",
				"width" : 0
			},{
				"data" : "lastActivityDateTime",
				"className" : "nowrap"
			}, {
				"data" : "action",
				"width" : "5%",
				"orderable": false
			} ],
			"order" : [ [ 3, "desc" ] ]			
	});
	LASLogger.instrEvent('application.domainDiscovery.onPage', {});
	
	refreshTable() 
	
	$("#cbShowAllDiscoverySessions").change(refreshTable);
	
	
	
});
	
function refreshTable() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/searchSession", populateSessionTable);
}

function populateSessionTable(data) {
	var table = $('#sessions').DataTable();
	table.clear();
	
	var showAllUsers = $("#cbShowAllDiscoverySessions").is(':checked');
	
	for (var i = 0; i < data.length; i++) {
		var newRow = data[i];
		if (showAllUsers == false && newRow.userID != $('#author').val()) {
			continue;
		} 
				
		var name = "<a onclick='LASLogger.instrEvent(\"application.domainDiscovery.sessionSelected\", {sessionID : \""+newRow.sessionID+"\",sessionName : \""+newRow.sessionName+"\" }); return true;' href='"+openke.global.Common.getPageURLPrefix()+"/domainDiscoverySession?sessionUUID="+ newRow.sessionID+"'>"+newRow.sessionName+"</a>";
		var deleteAction = "<span onclick='deleteSession(\""+newRow.sessionID+"\",\""+newRow.userID+"\",this)' class='fas fa-trash-alt'></span>"
		newRow.sessionName = name;
		newRow.action = deleteAction
		table.row.add(newRow);
	}
	table.draw();	
}


function deleteSession(sessionID,userID,obj) {
	if ($('#author').val() != userID) {
		bootbox.alert("You may only delete your own sessions");
		return;
	}
	
	bootbox.confirm({
	    title: "Delete Discovery Session?",
	    message: "Are you sure you want to delete this session? This action cannot be reversed.",
	    buttons: {
	        cancel: {
	            label: '<i class="fa fa-times"></i> No'
	        },
	        confirm: {
	            label: '<i class="fa fa-check"></i> Yes'
	        }
	    },
	    callback: function (result) {
	        if (result) {
	        	LASLogger.instrEvent('application.domainDiscovery.deleteSession', { "sessiondID" : sessionID });
	        	var dialog = bootbox.alert ("Delete discovery session in progress ...");
	        	$.ajax({
	        		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionID,
	        		type : "DELETE",
	        		contentType: "application/json; charset=utf-8",
	        		dataType : "JSON",
	        		success: function(data) {
	        			if (data.status === "successful") {
	        				dialog.modal('hide');
	        				$('#sessions').DataTable().row( $(obj).parents('tr') ).remove().draw();
	        			}
	        			else {
	        				bootbox.alert(data.message);
	        			}
	        		}
	        	});
	        }
	    }
	});
}
	
