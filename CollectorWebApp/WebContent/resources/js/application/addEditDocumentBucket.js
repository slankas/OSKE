/**
 * 
 */
var collaboratorCount = 0;
var firstParticipant = true;

//documentBucketID will be null if creating new collection.
var documentBucketID = null;

var PAGE = "add_document_bucket"; // Page type. Whether it is a new collection(add collection) or edit existing collection.


$(document).ready(function() {
	window.page = "add_document_bucket"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.addEditDocumentBucket.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});

	$('#btDocumentBuckets').click(function(){
		LASLogger.instrEvent('application.addEditDocumentBucket.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/manageDocumentBuckets";});
	});
		
	$("#err_label_name").hide();

	documentBucketID = $('#documentBucketID').val()
	
    if (documentBucketID != null && documentBucketID != "null" && documentBucketID != "") {
    	PAGE = "edit_document_bucket";   
    	window.page = "edit_document_bucket";
    	LASLogger.instrEvent('application.edit_document_bucket', {
    		documentBucketID : documentBucketID
    	});
    	$("#lblCollectionType").text("Edit Document Bucket")
    	getDocumentBucket();
    } else {
    	LASLogger.instrEvent('application.add_document_bucket');
	}

	var emailid = "";

	$("#tbl_label").hide();
	$("#tblData").hide();
	
	$("#txtTag").focus();
	/* Adding author as default participant - END */

	$('#btAddParticipant').on('click', addParticipant);
	$('#btSubmit').on('click',storeDocumentBucket);

	if ($("#allowUserSearch").val() !== "false") {
		$('#addParty').bind( "keydown",
				function(event) {
					if (event.keyCode === $.ui.keyCode.TAB	&& $(this).autocomplete("instance").menu.active) {
						event.preventDefault();
					}
					if (event.keyCode === $.ui.keyCode.ENTER) {
						return false;
					}
				}).bind("keyup", function(event) {
			if (event.keyCode === $.ui.keyCode.ENTER) {
				var emailid = $('#addParty').val().toLowerCase();
				if (!(emailid == null || emailid == "")) {
					var url = openke.global.Common.getRestURLPrefix()+"/documentbucket/participant?emailid=" + emailid;
					$.getJSON(url, populateTable);
					$(this).autocomplete('close').val('');
				}
				return false;
			}
		}).autocomplete(
				{
					minLength : 4,
					source : function(request, response) {
						LASLogger.instrEvent(
								'application.new_document_bucket.search_participant', {
									text : $('#addParty').val()
								});
						$.ajax({
							url : openke.global.Common.getContextRoot()+"rest/person/query?searchString="+ $('#addParty').val(),
							dataType : "json",
							success : function(data) {
								var people = [];
	
								for (var i = 0; i < data.People.length && i < 12; i++) {
									var row = data.People[i].displayName + " ("	+ data.People[i].emailAddress + ")";
									people.push(row);
								}
								response(people);
							}
						});
					},
					focus : function() {
						// prevent value inserted on focus when false.
						return true;
					},
					select : function(event, ui) {
						if (event.keyCode !== $.ui.keyCode.ENTER) {
							var emailid = ui.item.value;
							if (!(emailid == null || emailid == "")) {
								var url = openke.global.Common.getRestURLPrefix()+"/documentbucket/participant?emailid=" + emailid;
								$.getJSON(url, populateTable);
							}
						}
						return false;
					}
				});
	}
	else {
		$('#addParty').attr("placeholder", "Manually enter a name and email");
	}

});


function getDocumentBucket() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/documentbucket/" + documentBucketID, populateDocumentBucket);
}


function populateDocumentBucket(data) {
	if (data == null || data.id == null) {
		return;
	}
	
	document.getElementById("txtTag").value = data.tag;
	document.getElementById("txtQuestion").value = data.question;
	document.getElementById("txtDescription").value = data.description;
	document.getElementById("txtPersonalNotes").value = data.personalNotes;

	var collaborators = data.collaborators
	if (data.collaborators.length == 0) {
		$("#tblData").hide();
		//$("#tblData thead").append(	"<tr id=\"tblhead\"><th>Name</th><th>Email</th><th></th></tr>");
		$("#tbl_label").hide();
	}
	else {
		$("#tblData").show();
		$("#tblData thead").append(	"<tr id=\"tblhead\"><th>Name</th><th>Email</th><th></th></tr>");
		$("#tbl_label").show();
		for (var i = 0; i < collaborators.length; i++) {
			$("#tblData tbody").append("<tr><td>"+ collaborators[i].name	+ "</td><td id='tdemail'>"+ collaborators[i].email + "</td>"
									+ "<td><img src='"+openke.global.Common.getContextRoot()+"resources/images/delete.png' onclick='return Delete(this)'/></td></tr>");
			collaboratorCount++;
		}
	}
}

function addParticipant() {
	$("#err_label_participant").hide();
	if ($('#addParty').val() == null || $('#addParty').val() == "") {
		$("#err_label_participant").html(" Please add an email.");
		$("#err_label_participant").show();
		return false;
	}

	var emailid = $('#addParty').val().toLowerCase();
	var url = openke.global.Common.getRestURLPrefix()+"/documentbucket/participant?emailid=" + emailid;
	$.getJSON(url, populateTable);
}

function populateTable(data) {
	$("#err_label_participant").hide();
	if (data.email == null) // Checking if it's a valid unity id.
	{
		$("#err_label_participant").html(" Unity ID doesn't exist.");
		$("#err_label_participant").show();
		return;
	}

	// Checking if the unity id is already added to the list of participants.
	var curr_email = data.email;
	var flag = false; // flag = true implies unity id is already added.
	$("#tblData tr").each(function() {
		var $row = $(this);
		if ($row.find(':nth-child(2)').text() == curr_email) {
			$("#err_label_participant").html(" Participant already added.");
			$("#err_label_participant").show();
			flag = true;
			return false;
		}
	});
	if (flag) {
		return false;
	}

	else {

		if (collaboratorCount == 0) {
			$("#tblData").show();
			$("#tblData thead").append("<tr id=\"tblhead\"><th>Name</th><th>Email</th><th></th></tr>");
			$("#tbl_label").show();
		}
		$("#tblData tbody")
				.append("<tr><td>"+ data.name+ "</td><td id='tdemail'>"	+ data.email+ "</td><td><img src='"+openke.global.Common.getContextRoot()+"resources/images/delete.png' onclick='return Delete(this)'/></td></tr>");

		collaboratorCount++;

		$("#drpdn_rounds").append("<option id='drpdn" + collaboratorCount + "' value='" + collaboratorCount + "'>"+ collaboratorCount + "</option>");
		$('#drpdn_rounds option[value="' + collaboratorCount + '"]').attr("selected", true);

		if (!firstParticipant) {
			LASLogger.instrEvent('brainstorm.new_session.add_participant', {
				participant : data.email
			});
		} else {
			firstParticipant = false;
		}
	}

	document.getElementById("addParty").value = "";
	if (collaboratorCount > 1)
		document.getElementById("addParty").focus();
}

function Delete(btn) {
	var par = $(btn).parent().parent(); // tr
	par.remove();
	var drpopt = "#drpdn" + collaboratorCount;
	$(drpopt).remove();
	collaboratorCount--;
	$('#drpdn_rounds option[value="' + collaboratorCount + '"]').prop("selected", true);
	if (collaboratorCount <= 0) {
		$("#tblhead").remove();
		$("#tbl_label").hide();
		$("#tblData").hide();
	}

	return false;
};

function hide() {
	document.getElementById("err_label_topic").style.display = "none";
	document.getElementById("err_label_rounds").style.display = "none";
	document.getElementById("err_label_participant").style.display = "none";
}

function validateCollection() {
	if ($('#txtTag').val() == null || $('#txtTag').val() == "" || $('#txtTag').val() == " ") {
		document.getElementById("err_label_name").style.display = "inline";
		$("#txt_topic").focus();
		return false;
	} else {
		return true;
	}
}

function getDocumentBucketJSON() {
	var collabArray = [];
	$("#tblData tbody tr").each(
		function() {
			var $row = $(this);
			var record = { "name" : $row.find(':nth-child(1)').text(), "email" :$row.find(':nth-child(2)').text()	  }
			collabArray.push(record)
	});
	
	var result = {
		"tag"            : $("#txtTag").val(),
		"question"       : $("#txtQuestion").val(),
		"description"    : $("#txtDescription").val(),
		"personalNotes"  : $("#txtPersonalNotes").val(),
		"collaborators"  : collabArray
	};
	return result;
}

function storeDocumentBucket() {
	if (validateCollection() == false) {	return;	}

    var documentBucketJSON = getDocumentBucketJSON();
    var strDocumentBucket = JSON.stringify(documentBucketJSON);
	var documentBucketID = $("#documentBucketID").val();
	var method = "POST"
	
    var url = openke.global.Common.getRestURLPrefix()+"/documentbucket"     
    if (documentBucketID != null && documentBucketID != "") {
    	url = url + "/" + documentBucketID;
    	method = "PUT"
    }

	
    $.ajax({
		type : method,
		url : url,
		data: strDocumentBucket,
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			if (data.status = "success") {
				window.location =openke.global.Common.getPageURLPrefix()+"/manageDocumentBuckets";
				return
			}
			else {
				bootbox.alert("Unable to save document bucket")
			}
			return;
		},
		error : function(data) {
			bootbox.alert("Unable to save bucket")
		},
	});
	return false;
	
}
