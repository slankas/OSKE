
var tblContent;             // reference to the dataTable
var structuralExtractionRecords = {};  // CER's indexed by the UUID
var editRecord;  // global variable so we can set fields in the edit dialog

var csvKeys =  ["domainInstanceName","id","hostname","pathRegex","recordParentID","recordName","recordSelector","recordExtractBy","recordExtractRegex","userEmailID","lastDatabaseChange"];

$(document).ready(function() {
	LASLogger.instrEvent('application.structuralExtraction.startPage');
	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.concept.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});
	
	$('#btCreateRecord').on('click', addstructuralExtractionRecord);
	$('#btClearFields').on('click', clearInputFields);
	$('#btTestURL').on('click', testURLExtraction);
	$('#btExportRecords').on('click', exportRecords);
	
	tblContent = $("#contentTable").DataTable({
		"pagingType": "full_numbers",
		"pageLength" : 25,
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"language": {
            "zeroRecords": "No records found",
            "infoEmpty": ""
		},
		"columns" : [
			{
				"data"  : "action",
				"width" : "5%",
				"orderable": false
			},
		     {
				"data" : "hostname",
				"width" : "15%"
			},
			{
				"data" : "pathRegex",
				"width" : "15%"
			},
			{
				"data" : "parentRecordName",
				"width" : "15%"
			},
			{
				"data" : "recordName",
				"width" : "15%",
			},
			{
				"data" : "recordSelector",
				"width" : "15%",
			},	
			{
				"data" : "recordExtractBy",
				"width" : "5%",
			},
			{
				"data" : "recordExtractRegex",
				"width" : "15%",
			},
			 ],
			order: [ [ 1, 'asc' ], [ 3, 'asc' ], [ 4, 'asc' ] ]
    });
	
	initializeFileUploadCapability();
	loadContentTable();
	
	if (importMessages !== "") {
		bootbox.alert(JSON.stringify(importMessages)); //TODO need to cleanup the response message on an upload from the server.
	}
});

function initializeFileUploadCapability() {   
	$("div#progress").hide();  // hide the progress bar
	var url = openke.global.Common.getRestURLPrefix()+"/structuralExtraction/testFile";
    $('#fileupload').fileupload({
        url: url,
        dataType: 'json',
        done: function (e, data) {
        	$("div#progress").hide()
        	/* displays the uploaded files
            $.each(data.result.files, function (index, file) {
                $('<p/>').text(file.name).appendTo('#files');
            });
            */
    		for (var i=0; i < data.result.files.length; i++) {
    			var fileData = data.result.files[i]
    			var logData = fileData._logMessages;
    			var logWin = getNewWindowReference();
				displayJSONObjectInNewWindow(logWin,logData,"Processing log: "+fileData.name);
				logWin.moveBy(0,-100); // need to make this visible to the user
				displayJSONObjectInNewWindow(getNewWindowReference(),fileData.content,"Extracted content: "+fileData.name);
    		}
        },
        progressall: function (e, data) {
        	$("div#progress").show()

            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .progress-bar').css(
                'width',
                progress + '%'
            );
        }
    }).prop('disabled', !$.support.fileInput)
        .parent().addClass($.support.fileInput ? undefined : 'disabled');
    
}

function loadContentTable() {
    $.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/structuralExtraction",
		contentType: "application/json; charset=utf-8",
		type : "GET",
		dataType : "JSON",
		success: function(data) {
			$('#newParentRecord').empty()
			$('#newParentRecord').append('<option selected value=""></option>')
			structuralExtractionRecords = {}
			for (var i = 0; i < data.structuralExtractionRecords.length; i++) {
				addRowToInternalStorage(data.structuralExtractionRecords[i]);

			}

			tblContent.clear();
			for (var i = 0; i < data.structuralExtractionRecords.length; i++) {
				var newRow = data.structuralExtractionRecords[i];			
				addRowToContentTable(newRow);
			}
			tblContent.draw();
		}
});    
}

function clearInputFields() {
	 $('#newHostname').val(""); 
	 $('#newPathRegex').val(""); 
	 $('#newRecordName').val(""); 
	 $('#newCSSSelector').val(""); 
	 $('#newParentRecord').val(""); 
	 $('#newParentRecord').val(""); 
	 $('#newExtractBy').val("text"); 
	 $('#newExtractRegex').val(""); 
}

function addRowToInternalStorage(row) {
	structuralExtractionRecords[row.id] = row;
	
	$('#newParentRecord option[value="'+row.id+'"').remove()
	
	if (row.hasOwnProperty("recordParentID") == false) {  // only add as a parent option if the record isn't a child
		$('#newParentRecord').append('<option value="'+row.id+'">'+ row.hostname + ": "+ row.recordName+'</option>')
	}
} 


function augmentRowForContentTable(row) {
	row.parentRecordName = "";
	if (row.hasOwnProperty("recordParentID")) {
		var parentID = row.recordParentID;
		if (structuralExtractionRecords.hasOwnProperty(parentID)) {
			row.parentRecordName = structuralExtractionRecords[parentID].hostname +": "+ structuralExtractionRecords[parentID].recordName
		}
	}
	
	row.action = "<span onclick='editstructuralExtractionRecord(\""+row.id+"\",this)' class='fas fa-edit'></span>&nbsp;&nbsp;" +
	                  "<span onclick='duplicatestructuralExtractionRecord(\""+row.id+"\",this)' class='fas fa-share'></span>&nbsp;&nbsp;" +
					  "<span onclick='deletestructuralExtractionRecord(\""+row.id+"\",this)' class='fas fa-times'></span>";
	
	return row;
}

/**
 * Note: after calling this method, the table needs to be re-redrawn.
 * @param rowToAdd
 * @returns
 */
function addRowToContentTable(rowToAdd) {
	var augmentedRow = augmentRowForContentTable(rowToAdd)

	tblContent.row.add(augmentedRow);	
}



function addstructuralExtractionRecord() {   
	if( $('#newHostname').val().trim() === '' || $('#newRecordName').val().trim() === '' || $('#newCSSSelector').val().trim() === '') {  
		bootbox.alert("You must enter the host name, record name, and CSS selector.");
		return false; 
	}

	
	var newRecordData = {
		hostname           : $('#newHostname').val().trim(),
		pathRegex          : $('#newPathRegex').val().trim(),
		recordName         : $('#newRecordName').val().trim(),
		recordSelector     : $('#newCSSSelector').val().trim(),
		recordExtractBy    : $('#newExtractBy').find(":selected").val(),
		recordExtractRegex : $('#newExtractRegex').val().trim(),
		recordParentID     : $('#newParentRecord').find(":selected").val()
	}
	
	LASLogger.instrEvent('application.structuralExtraction.addRecord',newRecordData);
	
	$.ajax ({
		url : openke.global.Common.getRestURLPrefix()+"/structuralExtraction/",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(newRecordData),
		dataType : "JSON",
		success: function(data) {
		
			if ( data.hasOwnProperty('error')) {
				$("#errormsg").html(data['error']).css('color', 'red');
			}
			else {
				$('#errormsg').empty();
				addRowToInternalStorage(data.record)
				addRowToContentTable(data.record);
				tblContent.draw();
				// clear out input fields?  right now, I'm not as we may want to use those fields for others...
			}
			return false;
		}
	});
	
	return false;	
}


/**
 * checks whether or not the given ID is present as a parent record ID in any other fields...
 * @param cerID
 * @returns
 */
function actsAsParentRecord(cerID) {
	var result = false;
	for (var key in structuralExtractionRecords) {
	    if (structuralExtractionRecords.hasOwnProperty(key)) {
			var record = structuralExtractionRecords[key];
			
			if (record.hasOwnProperty("recordParentID") && record.recordParentID === cerID) {
				result = true;
				break;
			}
	    }
	}
	
	return result;
}

function editstructuralExtractionRecord(cerID,obj) {
	editRecord = structuralExtractionRecords[cerID];
	
	var options = '<option selected value=""></option>';
	for (var key in structuralExtractionRecords) {
	    if (structuralExtractionRecords.hasOwnProperty(key)) {
	    	var row = structuralExtractionRecords[key];
	    	if (row.hasOwnProperty("recordParentID") == false) {  // only add as a parent option if the record isn't a child
	    		options = options + '<option value="'+row.id+'">'+ row.hostname + ": "+ row.recordName+'</option>';
	    	}
	    }
	}
	
	bootbox.dialog({
		title: 'Edit Content Extraction Record',
	    message: '<form class="form-inline">'+
	             '<div class="form-group"><label for="editHostname">Hostname</label><input type="text" class="form-control" id="editHostname" placeholder="www.somesite.com"></div>' +
	             '<div class="form-group"><label for="editPathRegex">Path Regex</label><input type="text" class="form-control" id="editPathRegex" placeholder=""></div>' +
	             '<div class="form-group"><label for="editParentRecord">Parent Record</label><select class="form-control" id="editParentRecord">'+options+'</select></div>' +
	             '<div class="form-group"><label for="editRecordName">Record / Field Name</label><input type="text" class="form-control" id="editRecordName" placeholder=""></div>' +
	             '<div class="form-group"><label for="editCSSSelector">CSS Selector</label>  <input type="text" class="form-control" id="editCSSSelector" placeholder=""></div>'+
	             '<div class="form-group"><label for="editExtractBy">Extract By</label><select class="form-control" id="editExtractBy"><option selected value="text">Text</option><option value="html">HTML</option><option value="text:regex">Text with Regex</option><option value="html:regex">HTML with Regex</option></select></div>' +
	             '<div class="form-group"><label for="editExtractRegex">Extract Regex/label><input type="text" class="form-control" id="editExtractRegex" placeholder=""></div>' +
	             '</form>' + 
	             '<script>' +
	         	 "$('#editHostname').val(editRecord.hostname); " +
	        	 "$('#editPathRegex').val(editRecord.pathRegex); " +
	        	 "$('#editRecordName').val(editRecord.recordName); " +
	        	 "$('#editCSSSelector').val(editRecord.recordSelector); " +
	        	 "$('#editParentRecord').val(editRecord.recordParentID); " +
	        	 "$('#editExtractBy').val(editRecord.recordExtractBy); " + 
         		 "$('#editExtractRegex').val(editRecord.recordExtractRegex);" +
	             '</script>',
	    closeButton: true,
	    onEscape: true,
	    buttons: {
	        confirm: {
	            label: 'Update',
	            className: 'btn-default',
	            callback: function() { 
	            	if( $('#editHostname').val().trim() === '' || $('#editRecordName').val().trim() === '' || $('#editCSSSelector').val().trim() === '') {  
	            		bootbox.alert("You must enter the host name, record name, and CSS selector.");
	            		return false; 
	            	}

	            	var editRecordData = {
	            		id             : editRecord.id,
	            		hostname       : $('#editHostname').val().trim(),
	            		pathRegex      : $('#editPathRegex').val().trim(),
	            		recordName     : $('#editRecordName').val().trim(),
	            		recordSelector : $('#editCSSSelector').val().trim(),
	            		recordExtractBy    : $('#editExtractBy').find(":selected").val(),
	            		recordExtractRegex : $('#editExtractRegex').val().trim(),
	            		recordParentID     : $('#editParentRecord').find(":selected").val()
	            	}
	            	
	            	LASLogger.instrEvent('application.structuralExtraction.editRecord',editRecordData);
	            	
	            	$.ajax ({
	            		url : openke.global.Common.getRestURLPrefix()+"/structuralExtraction/"+editRecord.id,
	            		type : "PUT",
	            		contentType: "application/json; charset=utf-8",
	            		data : JSON.stringify(editRecordData),
	            		dataType : "JSON",
	            		success: function(data) {
	            		
	            			if ( data.hasOwnProperty('error')) {
	            				$("#errormsg").html(data['error']).css('color', 'red');
	            			}
	            			else {
	            				$('#errormsg').empty();
	            				var row = augmentRowForContentTable(data.record);
	            				tblContent.row( $(obj).parents('tr') ).data(row).draw();	            				
	            				addRowToInternalStorage(data.record)
	            			}
	            		}
	            	});	            	
	            }
	        },
	        cancel: {
	            label: 'Cancel',
	            className: 'btn-info',
	            callback: function() {
	            	LASLogger.instrEvent('application.structuralExtraction.editRecord.cancel',editRecord);
	            }
	        }
	    }
	});
}


function duplicatestructuralExtractionRecord(cerID,obj) {
	var record = structuralExtractionRecords[cerID];
	
	LASLogger.instrEvent('application.structuralExtraction.duplicateRecord',record);
	
	$('#newHostname').val(record.hostname)
	$('#newPathRegex').val(record.pathRegex)
	$('#newRecordName').val(record.recordName)
	$('#newCSSSelector').val(record.recordSelector)
	$('#newParentRecord').val(record.recordParentID)
	$('#newExtractBy').val(record.recordExtractBy)
	 $('#newExtractRegex').val(record.recordExtractRegex)
	
}

function deletestructuralExtractionRecord(cerID,obj) {
	// need a check that this record doesn't exist as a 
	if (actsAsParentRecord(cerID)) {
		bootbox.alert("This record is a parent record.  You must delete all of the children records first.");
		return false;
	}
	
	
	LASLogger.instrEvent('application.structuralExtraction.addRecord',{structuralExtractionRecordID: cerID});
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/structuralExtraction/"+cerID,
		type : "DELETE",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			$('#newParentRecord option[value="'+cerID+'"').remove()
			tblContent.row( $(obj).parents('tr') ).remove().draw();
		}
	});
}

function testURLExtraction() {
	var urlToTest = $('#testURL').val().trim();
	if (urlToTest === "") {
		bootbox.alert("You must enter a URL to test.");
		return false;
	}

	var showLogParam = "";
	var logWin = null;
	if (document.getElementById('cbShowLog').checked) {
		logWin= getNewWindowReference();
		showLogParam = "?log=true";
	}
	
	var myWin = getNewWindowReference(); 
	// using base64 encoding (btoa) necessary as the server wouldn't properly recognize the url otherwise.  The "/" also causes problem, so convert to a $
	var urlEncoded = btoa(urlToTest).replace(/\//g, "$")
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/structuralExtraction/extract/"+ urlEncoded +showLogParam,
		type : "GET",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			if (data.status == "success") {
				if (logWin != null) {
					var logData = data._logMessages;
					displayJSONObjectInNewWindow(logWin,logData,"Processing log: "+urlToTest);
					logWin.moveBy(0,-100); // need to make this visible to the user
				}
				displayJSONObjectInNewWindow(myWin,data.content,"Extracted content: "+urlToTest);
			}
			else {
				bootbox.alert(data.message)
			}
		},
		error: function () {
			myWin.close();
			bootbox.alert("An unknown error has occured.")
		}
		
	})
	
	
	return false;
}



function clearFiles() {
    LASLogger.instrEvent('application.concepts.clearUploadedFiles');

	$("#tblPresentation > tbody").html("");
}

$("#addFiles").change(function() {
    var file_names = [];
    for (var i = 0; i < $(this).get(0).files.length; ++i) {
    	file_names.push($(this).get(0).files[i].name);
    }
	
    LASLogger.instrEvent('application.categoryConcept.upload_file-drag', {
		file_names : file_names
	});
});

function readyDataForExport() {
	  var records = [];
	  for (var property in structuralExtractionRecords) {
		  if (structuralExtractionRecords.hasOwnProperty(property)) {
			  var temp = JSON.parse(JSON.stringify(structuralExtractionRecords[property]))
			  delete temp.action;
			  if (temp.hasOwnProperty('recordParentID') == false) {
				  temp.recordParentID = "";
			  }
		      records.push(temp);
		  }
	  }
	  return records;
}

function exportRecords() {
	bootbox.dialog({
		  message: "Select your format option",
		  title: "Export Content Extraction Records",
		  buttons: {
		    csv: {
		      label: "CSV",
		      className: "btn-default",
		      callback: function() {
		    	  exportJsonToCSV("exportedstructuralExtractionRecords.csv", readyDataForExport(), csvKeys) 
		      }
		    },
		    json: {
			      label: "JSON",
			      className: "btn-default",
			      callback: function() {
			    	  exportToJsonFile(readyDataForExport())
			      }
			    },
	        cancel: {
	            label: 'Cancel',
	            className: "btn-info",
	        },
		  }
		});
}

function exportToJsonFile(jsonData) {
    let dataStr = JSON.stringify(jsonData);
    let dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
    
    let exportFileDefaultName = 'data.json';
    
    let linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
}


/***
 * This function will export the data in rows to a csv file.
 * Assumptions: rows is an array.  all objects are the same in the array
 * header is optional, if not present, taken from first record
 */
function exportJsonToCSV(filename, rows, keys) {
    var processRow = function (row,fieldNames) {
        var finalVal = '';
        for (var j = 0; j < fieldNames.length; j++) {
        	var fieldValue = row[fieldNames[j]];
        	
            var innerValue = fieldValue === null ? '' : fieldValue.toString();
            if (fieldValue instanceof Date) {
                innerValue = fieldValue.toLocaleString();
            };
            var result = innerValue.replace(/"/g, '""');
            if (result.search(/("|,|\n)/g) >= 0)
                result = '"' + result + '"';
            if (j > 0)
                finalVal += ',';
            finalVal += result;
        }
        return finalVal + '\n';
    };
    
    var findKeys = function(dataArray) {
    	var result = [];
    	for (var j = 0; j < dataArray.length && j < 1; j++) {
            var record = dataArray[0];
            
            for (var key in record) {
                if (record.hasOwnProperty(key)) {
                    result.push(key)
                }
            }            
        }
        return result;
    }

    var createHeaderRow = function(headers) {
    	var result = '';
    	for (var j = 0; j < headers.length ; j++) {
    		if (j>0) { result += ","}
    		var field  = headers[j].replace(/"/g, '""');
    		if (field.search(/("|,|\n)/g) >= 0)
    			field = '"' + field + '"';
    		result += field;
    	}
    	return result + '\n';
    }
    
    if (typeof keys === 'undefined') { keys = findKeys(rows);}
    var csvFile = createHeaderRow(keys);
    for (var i = 0; i < rows.length; i++) {
        csvFile += processRow(rows[i],keys);
    }

    var blob = new Blob([csvFile], { type: 'text/csv;charset=utf-8;' });
    if (navigator.msSaveBlob) { // IE 10+
        navigator.msSaveBlob(blob, filename);
    } else {
        var link = document.createElement("a");
        if (link.download !== undefined) { // feature detection
            // Browsers that support HTML5 download attribute
            var url = URL.createObjectURL(blob);
            link.setAttribute("href", url);
            link.setAttribute("download", filename);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
    }
}

function getNewWindowReference() {
	var screen_width = screen.width * .5;
    var screen_height = screen.height* .5;
    var top_loc = screen.height *.15;
    var left_loc = screen.width *.1;

    var myWindow = window.open("",'_blank','toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width='+screen_width+',height='+screen_height+',top='+top_loc+', left='+left_loc);
    
    return myWindow;
}

function displayJSONObjectInNewWindow(popupWindow, data,title) {	    
    var js = '<script type="text/javascript" charset="utf8" src="'+openke.global.Common.getContextRoot()+'resources/ace/ace.js"></script>';
    
    popupWindow.document.writeln('<div style="height: 200px;" id="editor">'+escapeHtml(JSON.stringify(data,null,4))+'</div>');
    popupWindow.document.writeln(js);
    popupWindow.document.writeln('<script>editor = ace.edit("editor");editor.setOptions({ maxLines: Infinity});editor.setTheme("ace/theme/xcode");editor.session.setMode("ace/mode/json");editor.getSession().setTabSize(4); editor.getSession().setUseSoftTabs(true);')
    popupWindow.document.writeln('editor.getSession().setUseWrapMode(true);'); 
    popupWindow.document.writeln('</script>');
    popupWindow.focus();
    popupWindow.document.title = title;
    
    return popupWindow;
}

