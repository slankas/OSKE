/**
 * Event Handling code for the document tab.
 */

"use strict"
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


$(document).ready(function() {
	openke.view.ProjectDocument.initialize();
});

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.ProjectDocument = (function () {

// page-level scoped variables
var detailDocumentUUID   = null;  // what is the current document being edited / viewed
var showInactiveDocuments = false; // Tracks whether or not to display inactive documents
var detailDocumentStatus = 'unknown';

var contentChanged = false; // track whether or not the user has made any edits.  if they edit their way back to the start, it's still an edit..


	
function initialize() {
	openke.model.ProjectDocument.setDefaultInstrumentationPage('application.document.')
	LASLogger.instrEvent('application.document');
	
	window.page = "documet"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	// Hide save and edit buttons - read panel is read only when user first accesses the page
	$("#btSave").hide()
	$("#btExport").hide();
	$("#btExportSrc").hide();
	$("#btExportZip").hide();
	$("#btMakeCurrentDocument").hide() 
	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").hide()
	
	$("#documentEdit").hide()
	
	$("#documentLoading").hide()

	// event handlers
	$("#btSave").click(saveDetail);
	$("#btExport").click(exportDetail);
	$("#btExportSrc").click(exportDetailWithSources);
	$("#btExportZip").click(exportDetailWithSourcesToServer);
	$("#btDocumentNew").click(createNewDocument);
	$("#btMakeCurrentDocument").click(makeCurrentDocument) ;
	$("#btSetActive").click(function() {changeStatus('active');})
	$("#btSetInactive").click(function() {changeStatus('inactive');})
	$("#btDelete").click(deleteDocument)
	
	$('#contents').summernote({ 
		height: 400,
		callbacks: {
			onKeydown: function(e) {
		    	contentChanged=true;
		    },
	        onPaste: function(e) {
		    	contentChanged=true;
	        },
	        onChange: function(contents, $editable){
	        	toggleSaveButton();
	        }
		}
	});
	$('#documentName').keydown(function() {contentChanged=true;})
	
	openke.model.ProjectDocument.loadAvailableDocuments(populateDocumentList);
	
	var documentUUID = $('#documentUUID').val()
    if (documentUUID != null && documentUUID != "") {
    	loadDocument(documentUUID);
    }
    else {
    	loadCurrentDocument();
    }	
}

function toggleSaveButton() {
	if (contentChanged){
		$("#btSave").removeAttr('disabled');
	}else{
		$("#btSave").attr('disabled', 'disabled');
	}
	
}
	
function populateDocumentList(data) {
	if (data.length == 0) {
		$("#documentLinks").html("<i>No active documents</i>")
		return;
	}
	$("#documentLinks").html("<p></p>")
	
	var foundInactiveDocument = false
	for (var i = 0; i < data.length; i++) {
		var docRecord = data[i]

		var html = '<a href="#" onClick="openke.view.ProjectDocument.loadDocument(\''+ docRecord.id+'\'); return false" onMouseOver="">'+escapeHtml(docRecord.name)+'</a><br>'
		
		if (docRecord.status == 'inactive') {
			foundInactiveDocument = true;
			if (showInactiveDocuments) {
				html = "<i>"+html+"</i>"
			} else {
				continue;
			}
		}
			
		$('#documentLinks').append(html)
	}
	
	if (foundInactiveDocument) {
		$('#documentLinks').append("<p>&nbsp;<p>");
		if (showInactiveDocuments) {
			$('#documentLinks').append('<div class="text-right"><button class="btn btn-primary btn-sm" id="btOnlyActive" onClick="openke.view.ProjectDocument.setInactiveDocumentDisplay(false); return false">Active Scratchpads Only</button></div>');
		}
		else {
			$('#documentLinks').append('<div class="text-right"><button class="btn btn-primary btn-sm" id="btOnlyActive" onClick="openke.view.ProjectDocument.setInactiveDocumentDisplay(true); return false">All Scratchpads</button></div>');
		}
	}
}

function setInactiveDocumentDisplay(newValue) {
	showInactiveDocuments = newValue;
	openke.model.ProjectDocument.loadAvailableDocuments(populateDocumentList);
}

function loadCurrentDocument() {
	if (LASHeader.getCurrrentScratchpadUUID() != null) {
		loadDocument(LASHeader.getCurrrentScratchpadUUID());
	}
}

function loadDocument(documentUUID) {
	if (contentChanged) {
		LASLogger.instrEvent('application.document.confirmLoadOverUnsavedData');
		bootbox.confirm({
		    title: "Switch Document?",
		    message: "Do you want to switch documents?  You will lose any edits that you have made on the current page.",
		    buttons: {
		        cancel: {
		            label: '<i class="fa fa-times"></i> Cancel'
		        },
		        confirm: {
		            label: '<i class="fa fa-check"></i> Confirm'
		        }
		    },
		    callback: function (result) {
		        if (result) { 
		        	$("#documentLoading").show();
		        	$("#documentEmpty").hide();
		        	openke.model.ProjectDocument.loadDocument(documentUUID, populateDocument);      
		        }
		    }
		});
	}
	else {
		$("#documentLoading").show();
		$("#documentEmpty").hide();
		openke.model.ProjectDocument.loadDocument(documentUUID, populateDocument);
	}	

}

function deleteDocument() {
	if (detailDocumentUUID == null) {
		bootbox.alert("Minor application error: delete document called w/out document loaded on page");
		return;
	}
	LASLogger.instrEvent('application.document.confirmDeleteDocument');
	bootbox.confirm({
	    title: "Delete Project?",
	    message: "Are you sure you want to delete the current document?  You will not be able to recover any stored information.",
	    buttons: {
	        cancel: {
	            label: '<i class="fa fa-times"></i> Cancel'
	        },
	        confirm: {
	            label: '<i class="fa fa-check"></i> Confirm'
	        }
	    },
	    callback: function (result) {
	        if (result) {
	        	openke.model.ProjectDocument.deleteDocument(detailDocumentUUID, function() {
	        		if (LASHeader.getCurrrentScratchpadUUID() == detailDocumentUUID) {   //if the project in the edit/display panel is set as the current project, then unassign it.
			    		LASHeader.setCurrrentScratchpad(null,null);	    		
			    	}
	        		
	        		detailDocumentUUID = null;
        			switchToEmptyDisplay();
        			openke.model.ProjectDocument.loadAvailableDocuments(populateDocumentList);

	        	}, function() {
	        		bootbox.alert("Unable to delete document")
	        	});
		    	
	        }
	    }
	});	
	return false;
}


function populateDocument(data) {
	if (data == null || data.id == null) {
		switchToEmptyDisplay();
		bootbox.alert("Invalid document selected - select another document from the scratchpad list.");
		return;
	}
	switchToEditDisplay()
	
	detailDocumentUUID   = data.id;
	detailDocumentStatus = data.status;
	
	
	$("#documentName").val(data.name);
	$('#contents').summernote('code', data.contents);
	
	if (data.status == 'active') {
		$("#btSetInactive").show()
		$("#btSetActive").hide()
	}
	else {
		$("#btSetActive").show()
		$("#btSetInactive").hide()
	}
	$('#documentName').focus()
	contentChanged = false;
	
	$("#btMakeCurrentDocument").show() 
}

function saveSuccessCallback(data) {
	Snackbar.show({text: $('#documentName').val() + " saved", duration: 3500})
	detailDocumentUUID = data.document.id;
	if (LASHeader.getCurrrentScratchpadUUID() == detailDocumentUUID) { 
		makeCurrentDocument(); // forces the name to be updated 	    	
	}
	populateDocument(data.document);
	openke.model.ProjectDocument.loadAvailableDocuments(populateDocumentList);  // this is also good on update as the name may have changed. (for create, the name wasn't there)
	contentChanged = false;	
}

function saveErrorCallback() {
	bootbox.alert("Unable to save document")
}

function saveDetail() {
	var docObj = createJSONObjectFormDetails();
	var passed = validateSubmission(docObj);
	if (!passed) {return;}
	
    if (detailDocumentUUID != null) {
    	openke.model.ProjectDocument.updateDocument(detailDocumentUUID, docObj,saveSuccessCallback,saveErrorCallback)
    }
    else {
    	openke.model.ProjectDocument.createDocument(docObj,saveSuccessCallback,saveErrorCallback)
    }
    contentChanged = false;
    toggleSaveButton();
}

function exportDetail() {  // TODO: this works for smaller documents, but falls for large documents.  May just want to force a save and get from there no matter what...
	/*
	var name = $('#documentName').val().trim();
	var content = $('#contents').summernote('code');	
	
	LASLogger.instrEvent('application.document.exportDocument');
	LASExport.exportHTMLBlockToWordDoc(content,name,"export");
	*/
	$("#downloadLink").attr({target: '_blank', download:$('#documentName').val().trim()+'.doc', href  : openke.model.ProjectDocument.getExportURL(detailDocumentUUID)});
	Snackbar.show({text: "Export initiated", duration: 3500})
	$("#downloadLink")[0].click();
	
	return false;	
}


function exportDetailWithSources() {

	var docObj = {};    // to hold exported json docs
	var uuid = "";      // to hold export doc uuid
	var tmpArray = [];  // to hold the array of uuids
	var zip = new JSZip();
	
	// find the divs inside summernote with the ref uuids
	tmpArray = $("div.note-editable").find("div").toArray();
	
	tmpArray.forEach(function(element){
		uuid = element.getAttribute("datasource");
		var url = openke.global.Common.getRestURLPrefix()+"/document/"+uuid+"/scratchpadDoc"
		
		$.ajax({
			dataType: "JSON",
			url: url,
			async: false,
			success: function(data){
				var doc = JSON.stringify(data[0]);
					docObj[uuid] = doc;
			}
		});
	});

	// add the source files to the zip. Syntax is file, content
	for(var key in docObj){
		var element = docObj[key];
		zip.file(key+'.txt', element);
	}
	// add the summary to the zip
	zip.file($('#documentName').val().trim()+'.doc', $('#contents').summernote('code'));

	// generate the zip and prompt the user
    zip.generateAsync({type:"base64"}).then(function (base64) {
    	var pom = document.createElement('a');
    	pom.setAttribute('href', 'data:application/zip;base64,' + base64);
    	pom.setAttribute('download', $('#documentName').val().replace(/\s/g,'')+'.zip');
	    	if (document.createEvent) {
	            var event = document.createEvent('MouseEvents');
	            event.initEvent('click', true, true);
	            pom.dispatchEvent(event);
	        }
	        else {
	            pom.click();
	        }
	    }, function (err) {
	        jQuery("#btExportSrc").text(err);
	    });

	return false;
}


function exportDetailWithSourcesToServer() {

	var docObj = {};    // to hold exported json docs
	var uuid = "";      // to hold export doc uuid
	var tmpArray = [];  // to hold the array of uuids
	var zip = new JSZip();
	
	// find the divs inside summernote with the ref uuids
	tmpArray = $("div.note-editable").find("div").toArray();
	
	tmpArray.forEach(function(element){
		uuid = element.getAttribute("datasource");
		var url = openke.global.Common.getRestURLPrefix()+"/document/"+uuid+"/scratchpadDoc"
		
		$.ajax({
			dataType: "JSON",
			url: url,
			async: false,
			success: function(data){
				var doc = JSON.stringify(data[0]);
					docObj[uuid] = doc;
			}
		});
	});

	// add the source files to the zip. Syntax is file, content
	for(var key in docObj){
		var element = docObj[key];
		zip.file(key+'.txt', element);
	}
	// add the summary to the zip
	zip.file($('#documentName').val().trim()+'.doc', $('#contents').summernote('code'));

	var filename = "doc_"+$('#documentName').val().replace(/\s/g,'');
	//goes to FileUploadController
	var url = openke.global.Common.getRestURLPrefix()+"/upload/zip/"+filename;
	
	// generate the zip and send to server for saving
    zip.generateAsync({type:"base64"}).then(function (base64) {
    	$.ajax({
			type: "POST",
			url: url,
			data: base64,
			contentType: false,
			processData: false,
			success: function(data){
				//alert("posted "+filename+" to server");
				alert(data);
			}
		});
    	
	    }, function (err) {
	        jQuery("#btExportZip").text(err);
	    });

	return false;
}



function switchToEditDisplay(isNew) {
	//alert("editing detail")
	$("#documentEdit").show();
	$("#documentEmpty").hide();
	$("#documentLoading").hide();

	$("#btSave").show();	
	$("#btExport").show();
	$("#btExportSrc").show();
	$("#btExportZip").show();
	$("#btMakeCurrentDocument").hide() 
	$("#btCancel").show()
	
	if (!isNew) {
		$("#btDelete").show();
	}

	if (detailDocumentStatus == 'active')   { $("#btSetInactive").show() }
	if (detailDocumentStatus == 'inactive') { $("#btSetActive").show() }
}


function switchToEmptyDisplay() {
	$("#btSave").hide();
	$("#btExport").hide();
	$("#btExportSrc").hide();
	$("#btExportZip").hide();
	$("#btMakeCurrentDocument").hide() 
	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").hide()
	
	$("#documentEdit").hide();
	$("#documentEmpty").show();
	$("#documentLoading").hide();
}


function createNewDocument() {
	// is there an current form that needs to be saved?? TODO
	if (contentChanged) {
		LASLogger.instrEvent('application.document.confirmCreateNewDocument');
		bootbox.confirm({
		    title: "Create New Document?",
		    message: "Do you want to create a new document?  You will lose any edits that you have made on the current form.",
		    buttons: {
		        cancel: {
		            label: '<i class="fa fa-times"></i> Cancel'
		        },
		        confirm: {
		            label: '<i class="fa fa-check"></i> Confirm'
		        }
		    },
		    callback: function (result) {
		        if (result) {
		        	startNewDocument();
		        }
		    }
		});
	}
	else {
		startNewDocument();
	}
}

function  startNewDocument() {
	LASLogger.instrEvent('application.document.startNewDocument');

	$('#documentName').val("")
	$('#contents').summernote('code', "");

	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").hide()
	
	detailDocumentUUID = null; 
	detailDocumentStatus = 'unknown';
	
	switchToEditDisplay(true);
	$('#documentName').focus()
	contentChanged = false;
	
	$("#btExport").hide();
	$("#btExportSrc").hide();
	$("#btExportZip").hide();
}

function makeCurrentDocument() {
	LASHeader.setCurrrentScratchpad(detailDocumentUUID,$('#documentName').val());
}

function changeStatus(newStatus) {
	openke.model.ProjectDocument.updateDocumentStatus(detailDocumentUUID, newStatus, function() {
		openke.model.ProjectDocument.loadAvailableDocuments(populateDocumentList);
		if (newStatus == 'inactive') {
			detailDocumentUUID   = null;
			detailDocumentStatus = 'unknown';
			switchToEmptyDisplay();
		}
		else {
			detailDocumentStatus = 'active';
			$("#btSetInactive").show();
			$("#btSetActive").hide();
		}
	},
	function() {
		bootbox.alert("Unable to change document status")
	});
}


function navigateTo(location) {
	var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
	LASLogger.instrEvent('domain.home.link', {
		link : completeLocation
	});
	
    window.location = completeLocation;
    
}




function createJSONObjectFormDetails() {
		
	var result = {
		name : $('#documentName').val().trim(),
		status : 'active',
		contents : $('#contents').summernote('code')
	}
	
	return result;
}

function validateSubmission(docObj) {
	//TODO: add client-side checking.  just name needs to be completed.
	return true;
}

return {
	initialize: initialize,
	changeStatus : changeStatus,
	createJSONObjectFormDetails : createJSONObjectFormDetails,
	createNewDocument: createNewDocument,
	deleteDocument: deleteDocument,
	exportDetail: exportDetail,
	exportDetailWithSources: exportDetailWithSources,
	exportDetailWithSourcesToServer: exportDetailWithSourcesToServer,
	loadCurrentDocument: loadCurrentDocument,
	loadDocument: loadDocument,
	makeCurrentDocument: makeCurrentDocument,
	navigateTo: navigateTo,
	populateDocument: populateDocument,
	populateDocumentList: populateDocumentList,
	saveDetail: saveDetail,
	saveErrorCallback: saveErrorCallback,
	saveSuccessCallback: saveSuccessCallback,
	setInactiveDocumentDisplay: setInactiveDocumentDisplay,
	startNewDocument: startNewDocument,
	switchToEditDisplay: switchToEditDisplay,
	switchToEmptyDisplay: switchToEmptyDisplay,
	validateSubmission: validateSubmission
};
}());
