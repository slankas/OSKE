/**
 * Generic component to allow exporting of domain discovery sessions and  search holdings results
 *                      
 */
var LASExportDialog = (function () {
	"use strict";
	
	var exportDialogCurrentPage     = "";    // specifies whether or not the user is on the "search" or "domainDiscoverySession" page
	var exportDialogQuery           = {};    // If on the search page, this was the query used for the search
	var exportDialogDomainDiscovery = {};    // If on the domain discovery page, this will include the sessionID and, optionally, the executionNumber.
	
	function openExportDialog(currentPageParam, queryParam, domainDiscoveryParam ) {
		exportDialogCurrentPage     = currentPageParam;
		exportDialogQuery           = queryParam;
		exportDialogDomainDiscovery = domainDiscoveryParam;

		$('#exportModal').modal('show')

		LASLogger.instrEvent('application.'+ exportDialogCurrentPage +'.exportDialog.open')		
	}

	function cancelExportDialog() {
		$('#exportModal').modal('hide');
		
		LASLogger.instrEvent('application.'+ exportDialogCurrentPage +'.exportDialog.cancel');
	}

	function submitExport() {
		
		var expName = $('#inExportName').val();
		
		/*if (document.getElementById('drpdnDestination').value === "directory" || document.getElementById('drpdnDestination').value === "HDFS" ) {
			//export name is only used for these two items ...
			var reg = /[^A-Za-z0-9_\-\.]/;
			if (reg.test(expName)) {
				bootbox.alert("The export name can only contain numbers, letters, underscores, dashes, and periods.");
				return false;
			}
			if (expName.trim() === "") {
				bootbox.alert("You must provide an export name.");
				return false;
			}
		}*/ 
		

		var restURL = openke.global.Common.getRestURLPrefix();
		
		if  (exportDialogCurrentPage == "search") {
			restURL = restURL +"/search/export";
		}
		else {
			restURL = restURL + "/searchSession/export"
		}
				
		var options = {
				"destination": document.getElementById('drpdnDestination').value,
				"format"     : document.getElementById('drpdnFormat').value,
				"naming"	 : "uuid",
				"currPage"   : exportDialogCurrentPage
				/*"grouping"   : document.getElementById('drpdnGrouping').value,
				"naming"     : document.getElementById('drpdnFileName').value,
				"stem"       : document.getElementById('drpdnStemWords').value,
				"exportName" : expName*/
		}
		
		var exportData = {
				"query" : exportDialogQuery,
				"domainDiscovery" : exportDialogDomainDiscovery,
				"options": options
		}
		
		LASLogger.instrEvent('application.'+ exportDialogCurrentPage +'.export', exportData);
	    $.ajax({
			type : "POST",
			url : restURL,
			data : JSON.stringify(exportData),
			success : function() {
				$('#exportModal').modal('hide');
				LASMessageDialog.showMessage('Export','You will receive an email with further instructions to access your export.');
			},
			error : function(data) {
				$('#exportModal').modal('hide');
				LASMessageDialog.showMessage('Export','An error occured with your export.');
			},
			dataType : "json",
			contentType : "application/json"
		});
	}

	function checkExportValues(event) {
		if (document.getElementById('drpdnDestination').value === "voyant") {
			document.getElementById('drpdnGrouping').value = "noGroup";
			document.getElementById('drpdnFormat').value = "indTextOnly";
			document.getElementById('drpdnFileName').value = "url";	
			document.getElementById('inExportName').value = "";
			
			if (event.srcElement.id !== "drpdnDestination") {
				bootbox.alert("No options are available when exporting to Voyant.");
			}
			return;
		}
		
		if (event.srcElement.id === "drpdnDestination" && event.srcElement.value === "directory") {
			// these are the "suggested" values
			document.getElementById('drpdnGrouping').value = "noGroup";
			document.getElementById('drpdnFormat').value = "indTextExp";
			return;
		}
		
		
		if (event.srcElement.id === "drpdnFormat") {
			if (event.srcElement.value === "jsonArray" || event.srcElement.value === "csvFile") {
				document.getElementById('drpdnGrouping').value = "noGroup";
				document.getElementById('drpdnFileName').value = "na";
				//$("select[name='drpdnGrouping']  option:[value='byDate']").attr("disabled","disabled");
				//$("select option:contains('url')").attr("disabled","disabled");
				//$("select option:contains('uuid')").attr("disabled","disabled");
			}
		}
		else if (event.srcElement.id === "drpdnGrouping") {
			if (event.srcElement.value === "byDate") {
				if (document.getElementById('drpdnFormat').value == "jsonArray" || document.getElementById('drpdnFormat').value == "csvFile") {
					document.getElementById('drpdnGrouping').value = "noGroup";
					bootbox.alert("Can not group the selected format.");
				}
			}
		}
		else if (event.srcElement.id === "drpdnFileName") {
			if (event.srcElement.value === "url" || event.srcElement.value == "uuid") {
				if (document.getElementById('drpdnFormat').value == "jsonArray" || document.getElementById('drpdnFormat').value == "csvFile") {
					document.getElementById('drpdnFileName').value = "na";
					bootbox.alert("Can not name files in the selected format.");
				}			
			}
		}
	}



	var privateMembers = {
		
	};

	return {
		openExportDialog : openExportDialog,
		cancelExportDialog : cancelExportDialog , 
		submitExport : submitExport, 
		checkExportValues : checkExportValues,
		privateMembers : privateMembers
	};
}());