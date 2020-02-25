LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


// domainId will be null if creating new domain.
var domainId = null;

var PAGE = "add_domain"; // Page type. Whether it is a new domain (add domain) or edit existing domain.

var editor;  // Reference to the ace editor

$(document).ready(function() {
    editor = ace.edit("editor");
    editor.setOptions({
        maxLines: Infinity
    });
    editor.setTheme("ace/theme/xcode");
    editor.session.setMode("ace/mode/json");
    editor.getSession().setTabSize(4);
    editor.getSession().setUseSoftTabs(true);
	
	window.page = "add_domain"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	domainId = $('#domainID').val()
	status = $('')
    if (domainId != null && domainId != "") {
    	PAGE = "edit_domain";   
    	window.page = "edit_domain";
    	LASLogger.instrEvent('application.edit_domain', {
    		domainID : domainId
    	});
    	loadDomain();
    } else {
    	LASLogger.instrEvent('application.add_domain');
		document.getElementById("tdStatusDateTime").style.display = "none";
		
		setButtonVisibility("");
        	
        $('#btnCreateSubmit').html('Create')
    }
	document.getElementById("err_label").style.display = "none";
	document.getElementById("success_label").style.display = "none";
		
	var message = $('#message').val()
	if (message != null && message != "") {
		$('#myModal').modal('show')                // initializes and invokes show immediately
		$('#myModalDialogText').html(message);
	}
	
});
	
function goManageDomains() {
	LASLogger.instrEvent('application.' + PAGE + '.cancel');

	window.location = openke.global.Common.getContextRoot()+"/system/domains";
}	
	

function loadDomain() {
	$.getJSON(openke.global.Common.getContextRoot()+"rest/domain/" + domainId, populateDomainData);
}

function populateDomainData(data) {
	if (data == null || data.domainInstanceName == null) {
		document.getElementById("err_label").style.display = "inline";
		document.getElementById("err_label").innerHTML = "Invalid Domain Instance ID";
		return;
	}
	
	document.getElementById("lblStatus").innerHTML = data.domainStatus;
	document.getElementById("lblStatusDatetime").innerHTML = data.effectiveTimestamp;
	document.getElementById("txtInstanceName").value = data.domainInstanceName;
	document.getElementById("txtFullName").value = data.fullName;
	document.getElementById("txtDisplayOrder").value = data.appearanceOrder;
	editor.setValue(JSON.stringify(data.configuration, null, 4),1);
	document.getElementById("txtDescription").value = data.description;
	document.getElementById("txtPrimaryContact").value = data.primaryContact;	
	$("input[name=rbOffline][value='"+data.offline+"']").prop("checked",true);
	
			
	document.getElementById("lblHeading").innerHTML = "Edit Domain: " + "<b>" + data.fullName + "</b>";
	setButtonVisibility(data.domainStatus);
	
	$('#txtInstanceName').prop("readonly", true);
	
	$('#btnCreateSubmit').html('Submit')
	

}

function setButtonVisibility(status) {
	switch (status) {
	case "active": $("#btnActivate").hide();
		        $("#btnInactivate").show();
		        $("#btnArchive").show();
		        $("btnCopyToNew").show();
		        break;
	case "inactive": $("#btnActivate").show();
                     $("#btnInactivate").hide();
                     $("#btnArchive").show();
                     $("btnCopyToNew").show();
                     break;
	case "archive":  $("#btnActivate").show();
                     $("#btnInactivate").show();
                     $("#btnArchive").hide();
                     $("btnCopyToNew").show();
                     break;             
    default: $("#btnActivate").hide();
             $("#btnInactivate").hide();
             $("#btnArchive").hide(); 
             $("btnCopyToNew").hide();
             break;
	}
}


function loadDefaultConfig() {
	LASLogger.instrEvent('application.' + PAGE + '.load_template');

	editor.setValue("{ }",1);
	$.getJSON(openke.global.Common.getContextRoot()+"rest/domain/system/config",
		function(result) {
			editor.setValue(JSON.stringify(result, null, 4),1);
		});	
	
	return false;	
}


function editStatus(status) {
	if (domainId == null) {
		return false;
	}
	alert("not implemented"); return false;

	
	var url = openke.global.Common.getContextRoot()+"rest/domain/" + domainId + "/" + status;
    
	LASLogger.instrEvent('application.' + PAGE + '.' + status , { 
		fullName : $('#txtFullName').val(), 
		domainInstanceName : domainId
	});			
    $.ajax({
		type : "POST",
		url : url,
		success : function(data) {
			//TODO: implement
		},
		error : function(data) {
			document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = jQuery.parseJSON(data.responseText).reason;
		},
		dataType : "text",
	});
    return false;
}



function copyToNew() {
	domainId = null;
	$("#domainID").val("");
	setButtonVisibility("");
	PAGE = "add_domain";   
	window.page = "add_domain";
	$('#txtInstanceName').prop("readonly", false);
	$('#txtInstanceName').val("");
	$('#txtFullName').val("");
	$('#txtDescription').val("");
	document.getElementById("lblHeading").innerHTML = "New Domain";
	
	return false;
}

function displayErrorMessage(message, focusField){
	document.getElementById("err_label").style.display = "inline";
	document.getElementById("err_label").innerHTML = message;

	focusField.focus();
	return false;
}

function submitForm() {
	var isNew = (domainId == null || domainId == "");
	document.getElementById("err_label").style.display     = "none";
	document.getElementById("success_label").style.display = "none";
	
	var domainInstanceName = $('#txtInstanceName').val();
	var fullName           = $('#txtFullName').val();
	var description        = $('#txtDescription').val();
	var primaryContact     = $('#txtPrimaryContact').val();
	var displayOrder       = $('#txtDisplayOrder').val();
	var configuration      = editor.getValue();
	
	// Domain Identifier
	if ( domainInstanceName == null || domainInstanceName.trim() == "" ) {
		return displayErrorMessage("Please enter a domain identifier.",$("#txtInstanceName"));
	}
	domainInstanceName = domainInstanceName.trim();
	if ( /[^a-z0-9]/.test( domainInstanceName ) ) {
		return displayErrorMessage("The domain identifier can only contain lowercase characters and numbers.",$("#txtInstanceName"));
	}	
	if (domainInstanceName.length > 15) {
		return displayErrorMessage("The domain identifier can only have 15 characters.  Current length: "+domainInstanceName.length,$("#txtInstanceName"));
	}
	
	//Full Name
	if ( fullName == null || fullName.trim() == "" ) {
		return displayErrorMessage("Please enter the full name.",$("#txtFullName"));
	}
	fullName = fullName.trim();
	if ( /[<>'"]/.test( fullName ) ) {
		return displayErrorMessage("The full name can not contain <,>,', or \".",$("#txtFullName"));
	}	
	if (fullName.length > 100) {
		return displayErrorMessage("The full name must be less than or equal to 100 characters: "+fullName.length,$("#txtFullName"));
	}	
	
	//Description
	if ( description == null || description.trim() == "" ) {
		return displayErrorMessage("Please enter a description.",$("#txtDescription"));
	}
	description = description.trim();
	if (description.length > 1024) {
		return displayErrorMessage("The description must be less than 1025 characters.  Current length: "+description.length,$("#txtDescription"));
	}	
	
	//Primary Contact
	if ( primaryContact == null || primaryContact.trim() == "" ) {
		return displayErrorMessage("Please enter a primary contact.",$("#txtPrimaryContact"));
	}
	primaryContact = primaryContact.trim();
	if ( /[^a-zA-Z0-9 ,\-\.]/.test( primaryContact ) ) {
		return displayErrorMessage("The primary contact can only alphabetical characters, numbers, spaces, dashes, periods, and commas.",$("#txtPrimaryContact"));
	}	
	if (primaryContact.length > 100) {
		return displayErrorMessage("The primary contact must be less than 101  characters.  Current length: "+primaryContact.length,$("#txtPrimaryContact"));
	}	
	
	//Display Order
	if ( displayOrder == null || displayOrder.trim() == "" ) {
		return displayErrorMessage("Please enter a display order.",$("#txtDisplayOrder"));
	}
	if (isNormalInteger(displayOrder) == false) {
		return displayErrorMessage("The display order must be a non-negative integer.",$("#txtDisplayOrder"));
	}
	
	//Configuration
	if ( configuration == null || configuration.trim() == "" ) {
		return displayErrorMessage("Please enter the configuration.",
				                   $('textarea.ace_text-input'));
	}
	if (isValidJSON(configuration) == false) {
		return displayErrorMessage("The configuration must be a valid JSON object.",
				                   $('textarea.ace_text-input'));
	}
	
	
	var domainJSON = {
			domainInstanceName : domainInstanceName,
			fullName : fullName,
			description : description,
			primaryContact:  primaryContact,
			displayOrder : Math.floor(Number(displayOrder)),
			configuration :	 JSON.parse(configuration),			
			offline : $('input[name=rbOffline]:checked').val()
	}
	
	
	//chek that the domain instance name is not already in use
	if (isNew) {
		$.ajax({	
			url:  openke.global.Common.getContextRoot() + "rest/domain/" + $('#txtInstanceName').val(),
			type:"HEAD",
			contentType: "application/json; charset=utf-8",
			dataType : "text JSON",
			success: function() {
				bootbox.alert($('#txtInstanceName').val() +" already exists as a domain identifier.  You must enter a different value.");
				return false;
			},
			error:  function (xhr, ajaxOptions, thrownError) {
		        if(xhr.status==404) {
		        	submitActualForm(isNew, domainJSON);
		        	return false;
		        }
		      }
		});	
		return false;
	}	
	
	return submitActualForm(isNew, domainJSON); //isnew should be false at this point;
}

function submitActualForm(submitAsNew, domainJSON) {
	
    var domainData = JSON.stringify(domainJSON);
    var url = "";
    var methodType = "";
    var message = "";
    var title = "";
    var holdDomainID = domainId;
    var requestType = "submit";
    
    if (submitAsNew) {
    	requestType = "saveAsNew";
    	domainId = "";
    }
     
    if (domainId != null && domainId != "") {
    	url = openke.global.Common.getContextRoot()+"rest/domain/" + domainId 	
    	message = "Domain updated.";
    	title = "Domain Update";
    	methodType = "PUT";
	} else {
    	url = openke.global.Common.getContextRoot()+"rest/domain/";
    	title = "Create Domain";
    	message = "Domain created.";
    	methodType = "POST";
	}
    
	LASLogger.instrEvent('application.' + PAGE + '.' + requestType , { 	name : domainJSON.domainInstanceName});			
	

    $.ajax({
		type : methodType,
		url : url,
		data: domainData,
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (title == "Create Domain") {
				domainId = data.domainInstanceName;
				populateDomainData(data);
				$('#btnCreateSubmit').html('Submit')
			}
			document.getElementById("lblHeading").innerHTML = "Edit Domain: " + "<b>" + data.fullName + "</b>";
			document.getElementById("lblStatus").innerHTML = data.status;
			document.getElementById("err_label").style.display = "none";
			$('#myModalDialogText').html(message);
			$('#myModalTitle').html(title);
			$('#myModal').modal('show')                // initializes and invokes show immediately
			return false;
		},
		error : function(data) {
			domainId = holdDomainID;
			document.getElementById("err_label").style.display = "inline";
			document.getElementById("err_label").innerHTML = "Error editing domain: "+JSON.stringify(JSON.parse(JSON.parse(data.responseText).reason).message);//data.responseJSON.reason;
		},
	});
	return false;
}

function formatJSON() {
	var data = editor.getValue();
	if(isValidJSON(data)) {
		editor.setValue(JSON.stringify(JSON.parse(data), null, 4),1);
	} else {
		displayErrorMessage("Cannot format invalid JSON", $('textarea.ace_text-input'));
	}
	
	return false;
}