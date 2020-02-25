function showSessionSummaryReport() {
	var dialog = bootbox.dialog({
	    title: "Report Started",
		message: "The report has been started. Please wait for it to download.",
	});
	sendInstrumentationEvent("showSessionSummaryReport");
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/execution",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(executionData) {
			$.ajax({
				url : openke.global.Common.getRestURLPrefix()+"/searchSession/"+sessionId+"/documentsSummary",
				contentType: "application/json; charset=utf-8",
				dataType : "JSON",
				success: function(documentData) {
					produceSessionSummaryReport(executionData,documentData);
					dialog.modal('hide');
				},
				error: function (data) {
					bootbox.alert(data.responseJSON.reason);
				}
			});	
		    return false;  	

		},
		error: function (data) {
			bootbox.alert(data.responseJSON.reason);
		}
	});	
    return false;  	
}

function createStringFromRecord(jsonRecord, fieldArray) {
	var result = "";
	for (var index in fieldArray) {
		result += "\""+ escapeDoubleQuote(jsonRecord[fieldArray[index]]) + "\","
	}
	
	return result
}

function produceSessionSummaryReport(executionData,documentData) {
	var columns = [ "title","url","description","publishedDate","textLength","textMinimizedLength","totalOutgoingLinks","totalOutgoingLinksDifferentDomain","numConcepts"];
	
    var CSV;    
    
    var numRow = ",,,,,,,,,Execution Number";
    for (var index in executionData) {
    	numRow += ","+executionData[index].executionNumber 
    }

    var termRow = ",,,,,,,,,Search Terms";
    for (var index in executionData) {
    	termRow += ",\""+ escapeDoubleQuote(executionData[index].searchTerms) +"\""; 
    }
    
    var searchCountRow = ",,,,,,,,,Num Search Results";
    for (var index in executionData) {
    	searchCountRow += ",\""+ executionData[index].numSearchResults +"\""; 
    }
    	
    
    var mainHeaderRow = '"title","url","description","publishDate","textLength","textMinimizedLength","totalOutgoingLinks","totalOutgoingLinksDifferentDomain","numConcepts","topPlace"';
    for (var index in executionData) {
    	mainHeaderRow += ","+ escapeDoubleQuote(executionData[index].searchAPI); 
    }
    
    CSV = numRow + '\r\n' + termRow + '\r\n' + searchCountRow + '\r\n' + mainHeaderRow + '\r\n';
    

    for (var i = 0; i < documentData.length; i++) {
    	var record = documentData[i];
        var row = createStringFromRecord(record,columns);
        
        var placeString = "";
        var topPlace = Number.MAX_SAFE_INTEGER;
        for (var j in executionData) {
        	var execIndex = "exec_" + executionData[j].executionNumber;
        	
        	var pos = record[execIndex];
        	if (pos > 0) {
        		placeString += ","+pos;
        		if (pos < topPlace) { topPlace = pos;}
        	}
        	else {
        		placeString += ",";
        	}
        }
        if (topPlace == Number.MAX_SAFE_INTEGER ) { topPlace = ""; }
        row += 	topPlace + placeString;
        
        CSV += row + '\r\n';
    }

   
    var fileName = (openke.global.Common.getDomain()+" "+$('#sessionName').val()+"_summary").replace(/ /g,"_") + ".csv"; // replaces spaces with underscores in name 
    

    var uri = 'data:text/csv;charset=utf-8,' + escape(CSV);
    
    var link = document.createElement("a");    
    link.href = uri;
    link.class += "hideVisibility";
    link.download = fileName;
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);	
}


function escapeDoubleQuote(value) {
	if (typeof value === "string") {
		return value.replace(/"/g,"\"\"");
	}
	else {
		return value;
	}
}

