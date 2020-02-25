/**
 * 
 */

// start of initialization code
"use strict"
LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);

$(document).ready(function() {
	openke.view.Plan.initialize();
});

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.Plan = (function () {
	
// page-level variables hidden into our scope	
var detailProjectUUID   = null;  // what is the current project being edited / viewed
var showInactiveProjects = false; // Tracks whether or not to display inactive projects
var detailProjectStatus = 'unknown';
	
function initialize() {
	LASLogger.instrEvent('application.plan');
	
	window.page = "plan"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	// Hide save and edit buttons - read panel is read only when user first accesses the page
	$("#btSave").hide()
	$("#btEdit").hide()
	$("#btMakeCurrentProject").hide() 
	$("#btCancel").hide()
	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").hide()
	
	$("#projectDetailEdit").hide()
	$("#projectDetailDisplay").hide()
	

	// event handlers
	$("#btSave").click(saveDetail);
	$("#btEdit").click(switchToEditDisplay);
	$("#btProjectNew").click(editNewDetail);
	$("#btMakeCurrentProject").click(makeCurrentProject) ;
	$("#btCancel").click(cancel);
	$("#btSetActive").click(function() {changeStatus('active');})
	$("#btSetInactive").click(function() {changeStatus('inactive');})
	$("#btDelete").click(deleteProject)
	
	$("#btnAddKeyQuestion").click(insertKQRow);
	$("#btnAddAssumption").click(insertAssumptionRow);
	$("#btnAddExternalLink").click(insertELRow);	
	
	$("#projectDetailDisplay").click(function() {
	    var sel = getSelection().toString();
	    if (!sel) {
	    	switchToEditDisplay();
	    }		
	});

	
	// custom css to fix formatting of search label
	//$("div.analystDomainDiscovery #sessions_filter > label").css("display","inline-flex");
	//$("div.analystDomainDiscovery #sessions_filter  input").css({"position": "relative", "top":"-5px"});

	loadProjectList();
	
	var projectUUID = $('#projectUUID').val()
    if (projectUUID != null && projectUUID != "") {
    	loadProject(projectUUID);
    }
    else {
    	loadCurrentProject();
    }	
}	
	
function loadProjectList() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/project/", populateProjectList);	
}

function populateProjectList(data) {
	if (data.length == 0) {
		$("#projectList").html("<i>No active projects</i>")
		return;
	}
	$("#projectList").html("<p id='projectLinks'></p>")
	
	var foundInactiveProject = false
	for (var i = 0; i < data.length; i++) {
		var projectRecord = data[i]

		var html = '<a href="#" onClick="openke.view.Plan.loadProject(\''+ projectRecord.id+'\'); return false" onMouseOver="">'+escapeHtml(projectRecord.name)+'</a><br>'
		
		if (projectRecord.status == 'inactive') {
			foundInactiveProject = true;
			if (showInactiveProjects) {
				html = "<i>"+html+"</i>"
			} else {
				continue;
			}
		}
			
		$('#projectLinks').append(html)
	}
	
	if (foundInactiveProject) {
		$('#projectLinks').append("<p>&nbsp;<p>");
		if (showInactiveProjects) {
			$('#projectLinks').append('<div class="text-right"><button class="btn btn-secondary btn-sm" id="btOnlyActive" onClick="openke.view.Plan.setInactiveProjectDisplay(false); return false">Active Projects Only</button></div>');
		}
		else {
			$('#projectLinks').append('<div class="text-right"><button class="btn btn-secondary btn-sm" id="btOnlyActive" onClick="openke.view.Plan.setInactiveProjectDisplay(true); return false">All Projects</button></div>');
		}
	}
}

function setInactiveProjectDisplay(newValue) {
	showInactiveProjects = newValue;
	loadProjectList();
}

function loadCurrentProject() {
	if (LASHeader.getCurrentProjectUUID() != null) {
		loadProject(LASHeader.getCurrentProjectUUID());
	}
}

function loadProject(projectUUID) {
	// is there an current form that needs to be saved?? TODO
	if ($("#btSave").is(":visible")) {
		bootbox.confirm({
		    title: "Switch Project?",
		    message: "Do you want to switch projects?  You will lose any edits that you have made on the current form.",
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
		        	$.getJSON(openke.global.Common.getRestURLPrefix()+"/project/" + projectUUID, populateProject);
		        }
		    }
		});
	}
	else {
		$.getJSON(openke.global.Common.getRestURLPrefix()+"/project/" + projectUUID, populateProject);
	}	

}

function deleteProject() {
	if (detailProjectUUID == null) {
		bootbox.alert("Minor application error: delete project called w/out project loaded on page");
		return;
	}
	bootbox.confirm({
	    title: "Delete Project?",
	    message: "Are you sure you want to delete the current project?  Any created document buckets will remain.  You will not be able to recover the project details.",
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
		    	//if the project in the edit/display panel is set as the current project, then unassign it.
		    	if (LASHeader.getCurrentProjectUUID() == detailProjectUUID) {
		    		LASHeader.setCurrentProject(null,null);	    		
		    	}
	            $.ajax({
	        		type : "DELETE",
	        		url : openke.global.Common.getRestURLPrefix()+"/project/"+detailProjectUUID,
	        	    contentType: "application/json; charset=utf-8",
	        		success : function(data) {
	        			if (data.status == "success") {
	        				detailProjectUUID = null;
	        				switchToEmptyDisplay();
	        				loadProjectList();
	        			}
	        			else {
	        				bootbox.alert("Unable to delete project")
	        			}
	        		},
	        		error : function(data) {
	        			bootbox.alert("Unable to delete project")
	        		},
	        	});	        	
	        }
	    }
	});	
	return false;
}


function populateProject(data) {
	if (data == null || data.id == null) {
		return;
	}
	switchToViewDisplay()
	
	resetKeyQuestionTable()
	resetAssumptionsTable() 
	resetExternalLinksTable()
	$("#keyQuestionsList").empty();
	$("#assumptionsList").empty();
	$("#externalLinksList").empty();
		
	
	detailProjectUUID = data.id;
	detailProjectStatus = data.status;
	
	$("#pdName").text(data.name);
	$("#pdPurpose").text(data.purpose);
	
	$("#projectName").val(data.name);
	$("#projectPurpose").val(data.purpose);
	
	for (var i=0; i< data.keyQuestions.length; i++) {
		var tableRow = $("#tblKeyQuestions tr:last");
		tableRow.find('input').eq(0).val(data.keyQuestions[i].tag);
		tableRow.find('input').eq(1).val(data.keyQuestions[i].question);
		insertKQRow();		
		
		$("#keyQuestionsList").append('<li>'+escapeHtml(data.keyQuestions[i].question)+'('+ escapeHtml(data.keyQuestions[i].tag) +')</li>');
	}	
	
	for (var i=0; i< data.assumptions.length; i++) {
		var tableRow = $("#tblAssumptions tr:last");
		tableRow.find('input').eq(0).val(data.assumptions[i]);
		insertAssumptionRow();
		
		$("#assumptionsList").append('<li>'+escapeHtml(data.assumptions[i])+'</li>');
	}	
	
	for (var i=0; i< data.relatedURLs.length; i++) {
		var tableRow = $("#tblExternalLinks tr:last");
		tableRow.find('input').eq(0).val(data.relatedURLs[i].link);
		tableRow.find('input').eq(1).val(data.relatedURLs[i].title);
		insertELRow();
		
		$("#externalLinksList").append('<li>'+escapeHtml(data.relatedURLs[i].title)+' - <a href="'+escapeHtml(data.relatedURLs[i].link)+'">'+ escapeHtml(data.relatedURLs[i].link) +'</a></li>');

	}	

	if (data.status == 'active') {
		$("#btSetInactive").show()
		$("#btSetActive").hide()
	}
	else {
		$("#btSetActive").show()
		$("#btSetInactive").hide()
	}
}



function saveDetail() {
	var projObj = createJSONObjectFormDetails();
	var passed = validateSubmission(projObj);
	if (!passed) {return;}
	
	var method = "POST"
	
    var url = openke.global.Common.getRestURLPrefix()+"/project"     
    if (detailProjectUUID != null) {
    	url = url + "/" + detailProjectUUID;
    	method = "PUT"
    }

	
    $.ajax({
		type : method,
		url : url,
		data: JSON.stringify(projObj),
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (data.status == "success") {
				detailProjectUUID = data.project.id;
		    	if (LASHeader.getCurrentProjectUUID() == detailProjectUUID) { 
		    		makeCurrentProject(); // forces the name to be updated   		
		    	}
		    	else if (method == "POST"){
		    		bootbox.confirm({
		    		    title: "Switch Project and Scratchpad?",
		    		    message: "Do you want to switch the current project and scratchpad to "+data.project.name+"?",
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
		    		        	makeCurrentProject();
		    		        	LASHeader.setCurrrentScratchpad(data.ancillary.projectDocuments[0].id,data.ancillary.projectDocuments[0].name);
		    		        }
		    		    }
		    		});
		    		
		    	}

				populateProject(data.project);
				switchToViewDisplay();
				loadProjectList();
			}
			else {
				bootbox.alert("Unable to save project")
			}
		},
		error : function(data) {
			bootbox.alert("Unable to save project")
		},
	});
	return false;	
}

function switchToEditDisplay(isNew) {
	//alert("editing detail")
	$("#projectDetailEdit").show();
	$("#projectDetailDisplay").hide();
	$("#projectDetailEmpty").hide();

	$("#btSave").show();
	$("#btEdit").hide();	
	$("#btMakeCurrentProject").hide() 
	$("#btCancel").show()
	
	if (!isNew) {
		$("#btDelete").show();
	}

	if (detailProjectStatus == 'active')   { $("#btSetInactive").show() }
	if (detailProjectStatus == 'inactive') { $("#btSetActive").show() }
}

function switchToViewDisplay() {
	$("#btSave").hide();
	$("#btEdit").show();
	$("#btMakeCurrentProject").show() 
	$("#btCancel").hide()
	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").show()
	
	if (detailProjectStatus == 'active')   { $("#btSetInactive").show() }
	if (detailProjectStatus == 'inactive') { $("#btSetActive").show() }

	
	$("#projectDetailEdit").hide();
	$("#projectDetailEmpty").hide();	
	$("#projectDetailDisplay").show();	
}

function switchToEmptyDisplay() {
	$("#btSave").hide();
	$("#btEdit").hide();
	$("#btMakeCurrentProject").hide() 
	$("#btCancel").hide()
	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").hide()
	
	$("#projectDetailEdit").hide();
	$("#projectDetailEmpty").show();	
	$("#projectDetailDisplay").hide();	
}


function editNewDetail() {
	// is there an current form that needs to be saved?? TODO
	if ($("#btSave").is(":visible")) {
		bootbox.confirm({
		    title: "Create New Project?",
		    message: "Do you want to create a new project?  You will lose any edits that you have made on the current form.",
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
		        	startNewDetail();
		        }
		    }
		});
	}
	else {
		startNewDetail();
	}
}

function  startNewDetail() {
	resetKeyQuestionTable()
	resetAssumptionsTable() 
	resetExternalLinksTable()
	$('#projectName').val("")
	$('#projectPurpose').val("")

	
	
	detailProjectUUID = null; // this will force a creation on the save.
	detailProjectStatus = 'unknown';
	
	switchToEditDisplay(true);
	
	$("#btSetActive").hide()
	$("#btSetInactive").hide()
	$("#btDelete").hide()
	$('#projectName').focus()
}

function makeCurrentProject() {
	LASHeader.setCurrentProject(detailProjectUUID,$("#pdName").text());
}

function changeStatus(newStatus) {
	var statusObj = {
			status : newStatus
	}
	
    $.ajax({
		type : "PUT",
		url : openke.global.Common.getRestURLPrefix()+"/project/"+detailProjectUUID+"/status",
		data: JSON.stringify(statusObj),
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (data.status == "success") {
				loadProjectList();
				if (newStatus == 'inactive') {
					detailProjectUUID   = null;
					detailProjectStatus = 'unknown';
					switchToEmptyDisplay();
				}
				else {
					detailProjectStatus = 'active';
					$("#btSetInactive").show();
					$("#btSetActive").hide();
				}
			}
			else {
				bootbox.alert("Unable to change project status")
			}
		},
		error : function(data) {
			bootbox.alert("Unable to change project status")
		},
	});
	return false;	
	
	
}


function cancel() {
	if (detailProjectUUID != null) {
		switchToViewDisplay()
	}
	else {
		switchToEmptyDisplay()
	}
}


function navigateTo(location) {
	var completeLocation =openke.global.Common.getPageURLPrefix() +"/" +location
	LASLogger.instrEvent('domain.plan.link', {
		link : completeLocation
	});
	
    window.location = completeLocation;
    
}


//Adds a row to search field

var kqRowID = 1;
function insertKQRow() {
	var tblKeyquestions = document.getElementById('tblKeyQuestions');

	var new_row = tblKeyquestions.rows[1].cloneNode(true);
	
	kqRowID = kqRowID +1;
	
	var rowID = kqRowID; // length won't work if we add/delete things, was  len = tblKeyquestions.rows.length;

	var current_row = tblKeyquestions.rows[tblKeyquestions.rows.length - 1];
	var add_button = current_row.cells[2].getElementsByTagName('input')[0];
	add_button.value = 'Remove';
	add_button.setAttribute("onclick", "javascript: openke.view.Plan.deleteRow(this,'tblKeyQuestions');");
	add_button.classList.remove("btn-outline-success");
	add_button.classList.add("btn-outline-danger");
	
	var inp = new_row.cells[0].getElementsByTagName('input')[0];
	inp.id += rowID;
	inp.value = '';
	
	var inp2 = new_row.cells[1].getElementsByTagName('input')[0];
	inp2.id += rowID;
	inp2.value = '';
		
	var inp3 = new_row.cells[2].getElementsByTagName('input')[0];
	inp3.id += rowID;
	inp3.value = 'Add';
	inp3.setAttribute("onclick", "javascript: openke.view.Plan.insertKQRow(this);");
	inp3.classList.remove("btn-outline-danger");
	inp3.classList.add("btn-outline-success");

	$('#tblKeyQuestions > tbody').append(new_row)
}

var elRowID = 1;
function insertELRow() {
	var tblKeyquestions = document.getElementById('tblExternalLinks');

	var new_row = tblKeyquestions.rows[1].cloneNode(true);
	
	elRowID = elRowID +1;
	var rowID = elRowID; // length won't work if we add/delete things, was  len = tblKeyquestions.rows.length;

	var current_row = tblKeyquestions.rows[tblKeyquestions.rows.length - 1];
	var add_button = current_row.cells[2].getElementsByTagName('input')[0];
	add_button.value = 'Remove';
	add_button.setAttribute("onclick", "javascript: openke.view.Plan.deleteRow(this,'tblExternalLinks');");
	add_button.classList.remove("btn-outline-success");
	add_button.classList.add("btn-outline-danger");
	
	var inp = new_row.cells[0].getElementsByTagName('input')[0];
	inp.id += rowID;
	inp.value = '';
	
	var inp2 = new_row.cells[1].getElementsByTagName('input')[0];
	inp2.id += rowID;
	inp2.value = '';
		
	var inp3 = new_row.cells[2].getElementsByTagName('input')[0];
	inp3.id += rowID;
	inp3.value = 'Add';
	inp3.setAttribute("onclick", "javascript: openke.view.Plan.insertELRow(this);");
	inp3.classList.remove("btn-outline-danger");
	inp3.classList.add("btn-outline-success");

	$('#tblExternalLinks > tbody').append(new_row)
}

var assumtionRowID = 1;
function insertAssumptionRow() {
	var tblKeyquestions = document.getElementById('tblAssumptions');

	var new_row = tblKeyquestions.rows[1].cloneNode(true);
	
	assumtionRowID = assumtionRowID +1;
	var rowID = assumtionRowID; // length won't work if we add/delete things, was  len = tblKeyquestions.rows.length;

	var current_row = tblKeyquestions.rows[tblKeyquestions.rows.length - 1];
	var add_button = current_row.cells[1].getElementsByTagName('input')[0];
	add_button.value = 'Remove';
	add_button.setAttribute("onclick", "javascript: openke.view.Plan.deleteRow(this,'tblAssumptions');");
	add_button.classList.remove("btn-outline-success");
	add_button.classList.add("btn-outline-danger");
	
	var inp = new_row.cells[0].getElementsByTagName('input')[0];
	inp.id += rowID;
	inp.value = '';
			
	var inp3 = new_row.cells[1].getElementsByTagName('input')[0];
	inp3.id += rowID;
	inp3.value = 'Add';
	inp3.setAttribute("onclick", "javascript: openke.view.Plan.insertAssumptionRow(this);");
	inp3.classList.remove("btn-outline-danger");
	inp3.classList.add("btn-outline-success");

	$('#tblAssumptions > tbody').append(new_row)
}


function deleteRow(row,strTableID) {
	var rowIndex = row.parentNode.parentNode.rowIndex;
	document.getElementById(strTableID).deleteRow(rowIndex);
}

// Clears / resets the question table to its starting state.
function resetKeyQuestionTable() {
	
	$("#tblKeyQuestions tr:gt(1)").remove();
	
	var tbl = document.getElementById('tblKeyQuestions');
	var current_row = tbl.rows[1];
	
	var inp0 = current_row.cells[0].getElementsByTagName('input')[0];
	inp0.value = '';

	var inp1 = current_row.cells[1].getElementsByTagName('input')[0];
	inp1.value = '';
	
	var inp2 = current_row.cells[2].getElementsByTagName('input')[0];
	inp2.value = 'Add';
	inp2.setAttribute("onclick", "javascript: openke.view.Plan.insertKQRow(this);");		
}

//Clears / resets the question table to its starting state.
function resetAssumptionsTable() {
	$("#tblAssumptions tr:gt(1)").remove();
	
	var tbl = document.getElementById('tblAssumptions');
	var current_row = tbl.rows[1];
	
	var inp0 = current_row.cells[0].getElementsByTagName('input')[0];
	inp0.value = '';
	
	var inp2 = current_row.cells[1].getElementsByTagName('input')[0];
	inp2.value = 'Add';
	inp2.setAttribute("onclick", "javascript: openke.view.Plan.insertAssumptionRow(this);");	
}

//Clears / resets the question table to its starting state.
function resetExternalLinksTable() {
	
	$("#tblExternalLinks tr:gt(1)").remove();
	
	var tbl = document.getElementById('tblExternalLinks');
	var current_row = tbl.rows[1];
	
	var inp0 = current_row.cells[0].getElementsByTagName('input')[0];
	inp0.value = '';

	var inp1 = current_row.cells[1].getElementsByTagName('input')[0];
	inp1.value = '';
	
	var inp2 = current_row.cells[2].getElementsByTagName('input')[0];
	inp2.value = 'Add';
	inp2.setAttribute("onclick", "javascript: openke.view.Plan.insertELRow(this);");		
}


function createJSONObjectFormDetails() {
	
	var kqArray=[]
	var tblKeyquestions = document.getElementById('tblKeyQuestions');
	for (var i = 1; i < tblKeyquestions.rows.length; i++) {
		var kqObj = {
			tag : tblKeyquestions.rows[i].cells[0].getElementsByTagName('input')[0].value.trim(),
			question : tblKeyquestions.rows[i].cells[1].getElementsByTagName('input')[0].value.trim()
		}
		if (kqObj.tag.length >0 && kqObj.question.length >0 ) {
			kqArray.push(kqObj)
		}
	}

	var assumptionArray=[]
	var tblAssumptions = document.getElementById('tblAssumptions');
	for (var i = 1; i < tblAssumptions.rows.length; i++) {
		var assumption = tblAssumptions.rows[i].cells[0].getElementsByTagName('input')[0].value.trim();
		if (assumption.length >0 ) {	assumptionArray.push(assumption) }
	}
	
	var elArray=[]
	var tblKeyquestions = document.getElementById('tblExternalLinks');
	for (var i = 1; i < tblKeyquestions.rows.length; i++) {
		var elObj = {
			link : tblKeyquestions.rows[i].cells[0].getElementsByTagName('input')[0].value.trim(),
			title : tblKeyquestions.rows[i].cells[1].getElementsByTagName('input')[0].value.trim()
		}
		if (elObj.link.length >0 && elObj.title.length >0 ) {
			elArray.push(elObj)
		}
	}	
	
	var result = {
		name : $('#projectName').val().trim(),
		status : 'active',
		purpose : $('#projectPurpose').val().trim(),
		keyQuestions : kqArray,
		assumptions : assumptionArray,
		relatedURLs : elArray
	}
	
	return result;
}

function validateSubmission(projObj) {
	
	//TODO: add client-side checking
	return true;
}

var privateMembers = {
		populateProject :  populateProject,
		populateProjectList : populateProjectList,
		validateSubmission : validateSubmission
}

return {
	initialize : initialize,
	cancel : cancel,
	changeStatus : changeStatus, 
	createJSONObjectFormDetails : createJSONObjectFormDetails,
	deleteProject : deleteProject,
	deleteRow : deleteRow, 
	editNewDetail : editNewDetail,
	insertAssumptionRow : insertAssumptionRow,
	insertELRow : insertELRow,
	insertKQRow : insertKQRow,
	loadCurrentProject : loadCurrentProject,
	loadProject : loadProject,
	loadProjectList : loadProjectList,
	makeCurrentProject : makeCurrentProject,
	navigateTo : navigateTo,
	resetAssumptionsTable : resetAssumptionsTable,
	resetExternalLinksTable : resetExternalLinksTable,
	resetKeyQuestionTable : resetKeyQuestionTable,
	saveDetail : saveDetail,
	setInactiveProjectDisplay : setInactiveProjectDisplay, 
	startNewDetail : startNewDetail,
	switchToEditDisplay : switchToEditDisplay, 
	switchToEmptyDisplay : switchToEmptyDisplay,
	switchToViewDisplay : switchToViewDisplay
};
}());
