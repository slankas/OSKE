$(document).ready(function() {
		LASLogger.instrEvent('application.domains.manage');
		LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);		
		window.page = "home"; // Page name to be stored in window object so that LasHeader.js could access it for logging

		var tblSourceHandlers = $('#tblDomain').DataTable({
			"pageLength" : 50,
			"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
			"dom": 'lftip',
	        "buttons": [
	            'excel','print'
	        ],
			"columns" : [ {
				"data" : "domainInstanceName",
				"width" : "5%"
			}, {
				"data" : "fullName",
				"width" : "10%"
			}, {
				"data" : "description",
				"width" : "20%"
			},{
				"data" : "configuration",
				"width" : "25%"
			}, {
				"data" : "offline",
				"width" : "3%"
			}, {
				"data" : "effectiveTimestamp",
				"width" : "10%"
			}, {
				"data" : "primaryContact",
				"width" : "10%" 
			}
			],
			"order" : [ [ 0, "asc" ] ]
		});

		$('#btnAddDomain').on('click', navigateToAddDomain);
		$('#btnSystemConfiguration').on('click', viewSystemConfiguration);

		refreshTables();
	
	});


function navigateToAddDomain() {
	window.location = openke.global.Common.getContextRoot()+"system/addEditDomain";
}

function refreshTables() {
	$.getJSON(openke.global.Common.getContextRoot()+"rest/domain/", refreshDomainTable);
}

function viewSystemConfiguration() {
	LASLogger.instrEvent('application.domains.viewSystemConfiguration');
	
	$.getJSON(openke.global.Common.getContextRoot()+"rest/domain/system/systemConfig", function(data) {
		var windowData = JSONTree.create(data);
		openNewWindow(windowData);
	});	
	
}

function purgeDomain(name) {
	
	bootbox.prompt("This operation will remove all data except job configurations within the domain.  To continue, enter 'purge'", function(result) { 
		if (result == 'purge') {
			LASLogger.instrEvent('application.domains.purge', {
	    		domainID : name
	    	});
			
			var dialog = bootbox.alert ("Purge domain in progress ...");
			$.ajax({
				url : openke.global.Common.getContextRoot()+"rest/domain/purgeDomain/"+name,
				type : "POST",
				contentType: "application/json; charset=utf-8",
				dataType : "JSON",
				success: function(data) {
					dialog.modal('hide');
					if (data['status'] == 'Success') {
						
						bootbox.alert("Domain has been purged.");
					}
					else {
						bootbox.alert(data['message'])	
					}
				},
				error: function(data) {
					dialog.modal('hide');
					bootbox.alert("Only System Admin can purge a domain")
				}
			});
		}
		else {
			bootbox.alert("Purge cancelled")
		}
	});
}

function refreshDomainTable(data) {
	//alert(JSON.stringify(data.domains[1]))
	var table = $('#tblDomain').DataTable();
	table.clear();
	for (var i = 0; i < data.domains.length; i++) {
		var newRow = data.domains[i];
		var name = newRow.domainInstanceName;
		//alert( (newRow['configuration']['allowOnlineDomainPurge'] == 'true'))
		
		newRow.domainInstanceName = name + "<br><a href='"+openke.global.Common.getContextRoot()+name+"'>view</a>&nbsp;&nbsp;<a href='"+openke.global.Common.getContextRoot()+"system/addEditDomain?domain="+name+"'>edit</a>"
		if (newRow.allowOnlineDomainPurge ) {
			newRow.domainInstanceName += "&nbsp;&nbsp;<a href=\"javascript:purgeDomain('"+name+"');\" id='purgebtn' >purge</button>"
		}

		var prettyConfig = escapeHtmlNewLine2(JSON.stringify(newRow.configuration, null, '\t'));
		var c = "<div class=scrollable>" + prettyConfig + "</div>";
		newRow.configuration = c;	
		
		table.row.add(newRow);
	}
	table.draw();

}


