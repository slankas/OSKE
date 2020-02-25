$(document).ready(function() {
		LASLogger.instrEvent('application.home');
		LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);		
		window.page = "home"; // Page name to be stored in window object so that LasHeader.js could access it for logging

		var tblDomains = $('#tblDomain').DataTable({
			"pageLength" : 50,
			"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
			"dom": 'p',
	        "buttons": [
	            'excel','print'
	        ],
			"columns" : [  {
				"data" : "fullName",
				"width" : "25%"
			}, {
				"data" : "description",
				"width" : "50%"
			}, {
				"data" : "effectiveTimestamp",
				"width" : "150"
			}, {
				"data" : "primaryContact",
				"width" : "0" 
			},			{
				"data" : "domainInstanceName",
				"width" : "0"
			}
			],
			"order" : [ [ 0, "asc" ] ]
		});


		$('#btnManageDomains').on('click', navigateToManageDomains);
    	$('#btnManageUserAgreements').on('click', navigateToUserAgreementAdjudicator);

		refreshTables();
	});


function navigateToManageDomains() {
	LASLogger.instrEvent('application.home.manageDomains');
	window.location = openke.global.Common.getContextRoot()+"system/domains";
}

function navigateToUserAgreementAdjudicator() {
	LASLogger.instrEvent('application.home.visitUserAgreement');
    window.location = openke.global.Common.getContextRoot()+"system/userAgreementAdjudicator";
}

function refreshTables() {
	$.getJSON(openke.global.Common.getContextRoot()+"rest/domain/", refreshDomainTable);
}


function refreshDomainTable(data) {
	var table = $('#tblDomain').DataTable();
	table.clear();

	for (var i = 0; i < data.domains.length; i++) {
		var newRow = data.domains[i];
		var name = newRow.fullName;

		newRow.fullName = "<a href='"+openke.global.Common.getContextRoot()+newRow.domainInstanceName+"'>"+name+"</a>";

        table.row.add(newRow);
	}
    table.draw();
}


