/**
 * 
 */


$(document).ready(function() {
	openke.view.MaintainUsers.initialize();
});

if (typeof openke == 'undefined') { openke = {} }
if (typeof openke.view == 'undefined') {	openke.view = {}  }
openke.view.MaintainUsers = (function () {
	var count = 0;
	var emailid = "";

function initialize() {
	LASLogger.instrEvent('application.users');

	window.page = "users"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	var columnList = [ {	"data" : "emailID", "width" : "0"	},
	      {	"data" : "name",    "width" : "0"	},
	      {	"data" : "role",	"width" : "0",
		      "fnCreatedCell" : function(nTd, sData, oData,	iRow, iCol) {
			    $(nTd).html("<a href='#' onclick='openke.view.MaintainUsers.updateRole(\""+ oData.emailID+ "\",\""+ oData.role+ "\",\""+oData.domain+"\")'>"+ escapeHtml(oData.role)+ "</a>");
		      }},
	      {	"data" : "status",	"width" : "0",
			"fnCreatedCell" : function(	nTd, sData, oData, iRow, iCol) {
				$(nTd).html("<a href='#' onclick='openke.view.MaintainUsers.updateStatus(\""	+ oData.emailID+ "\",\""+ oData.status+ "\",\""+ oData.role+ "\",\""+oData.domain+ "\")'>"+ escapeHtml(oData.status)	+ "</a>");
			} }, 
		  {	"data" : "statusDt",	"width" : "0",	},
		 ]
	
	if (openke.global.Common.getDomain() === "system") {
		var tr = document.getElementById('tblData').tHead.children[0]
	    var th = document.createElement('th');
		th.innerHTML = "Domain";
		tr.insertBefore(th, tr.firstChild);
		columnList.unshift({"data" : "domain",    "width" : "30" })
	}
	
	var table = $('#tblData').DataTable({
		"dom": 'lftip',
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : columnList,
		"order" : [ [ 0, "asc" ] ]
		});

	bootbox.setDefaults({
		show : true,
		animate : false,
		cancelButton : false
	});
	$('#updateRole').modal('hide');
	$('#updateStatus').modal('hide');
	document.getElementById("err_label_user").style.display = "none";

	$('#btAddUser').on('click', addUser);
	$('#showAllUsers').on('click', refreshTables);
	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.users.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});
	
	// Instrumentation calls for change.
	$('#drpdn_roles').change(function() {
		var selectedValue = parseInt(jQuery(this).val());
	});

	if ($("#allowUserSearch").val() !== "false") {
	    $('#txtaddUser').bind(
			"keydown", function(event) {
				if (event.keyCode === $.ui.keyCode.TAB
						&& $(this).autocomplete("instance").menu.active) {
					event.preventDefault();
				}
				if (event.keyCode === $.ui.keyCode.ENTER) {
					return false;
				}
			}).bind(
			"keyup",function(event) {
				if (event.keyCode === $.ui.keyCode.ENTER) {
					addUser();
					return false;
				}
			}).autocomplete(
			{
				minLength : 4,
				source : function(request, response) {
					$.ajax({
						url : openke.global.Common.getContextRoot()+"rest/person/query?searchString=" + $('#txtaddUser').val(),
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
					return false;
				},
				
		});
	}
	else {
		$('#txtaddUser').attr("placeholder", "Manually enter a name and email");
	}
	
	
	refreshTables();
}


function refreshTables() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/user/", refreshUserTable);
}


function refreshUserTable(data) {
	var tblData = $('#tblData').DataTable();
	tblData.clear();
	for (var i = 0; i < data.users.length; i++) {
		for (var j=0; j < data.users[i].access.length; j++) {
			if (data.users[i].access[j].status === "removed" && !document.getElementById("showAllUsers").checked) {continue;}
			var newRow = {};
		
			newRow["name"]    = data.users[i].name;
			newRow["emailID"] = data.users[i].emailID;
			newRow["userID"]  = data.users[i].userID;
			newRow["role"]    = data.users[i].access[j].role;
			newRow["status"]  = data.users[i].access[j].status;
			newRow["statusDt"]         = data.users[i].access[j].statusDt;
			newRow["changedByEmailID"] = data.users[i].access[j].changedByEmailID;
			newRow["domain"]           = data.users[i].access[j].domain;
			
			// LASLogger.logObject(LASLogger.LEVEL_TRACE,newRow);
			tblData.row.add(newRow);
		}
	}
	tblData.draw();
}



function addUser() {
	$("#err_label_user").hide();
	if ($('#txtaddUser').val() == null || $('#txtaddUser').val() == "") {
		$("#err_label_user").html("Enter a user.");
		$("#err_label_user").show();
		return false;
	}
	
	var newUser = {
		emailID: $('#txtaddUser').val(),
		role: $('#drpdn_roles').val()
	}
	
	if (openke.global.Common.getDomain() === "system") { 
		var domainForUser = $('#drpdn_domain').val()
		if (domainForUser == null || domainForUser == "") {
			$("#err_label_user").html("You must select a domain.");
			$("#err_label_user").show();
			return false;
		}
		newUser.domain = domainForUser 
	}
	
	LASLogger.instrEvent('application.users.add', {
		user : newUser
	});

	$.ajax({	
		url : openke.global.Common.getRestURLPrefix()+"/user/",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(newUser),
		success : populateTable,
		error : function(jqXHR, textStatus, errorThrown) {
			bootbox.alert(jqXHR.responseJSON.reason);
		}
	});
}

function populateTable(data, status) {
	$("#err_label_user").hide();
	if (data.status == "error") {
		$("#err_label_user").html(data.message);
		$("#err_label_user").show();
	}
	else {
		var newUser = data.user;
		
		//TODO: need to change this to just a new row, rather than hard-code...
		var tblData = $('#tblData').DataTable();
		for (var j=0; j < newUser.access.length; j++) {
			if (newUser.access[j].status === "removed" && !document.getElementById("showAllUsers").checked) {continue;}
			var newRow = {};
			
			newRow["name"]    = newUser.name;
			newRow["emailID"] = newUser.emailID;
			newRow["userID"]  = newUser.userID;
			newRow["role"]    = newUser.access[j].role;
			newRow["status"]  = newUser.access[j].status;
			newRow["statusDt"]         = newUser.access[j].statusDt;
			newRow["changedByEmailID"] = newUser.access[j].changedByEmailID;
			newRow["domain"]           = newUser.access[j].domain;
				
			// LASLogger.logObject(LASLogger.LEVEL_TRACE,newRow);
			tblData.row.add(newRow);		
		}
		
		tblData.draw();
		document.getElementById("txtaddUser").value = "";
	}
}


function updateRole(email_id, role, domain) {
	$('#updateRole').modal('show');
	$('#drpdn_update_roles option[value="' + role + '"]').prop("selected", true);

	$('#btUpdateRole').on('click',	function() {
		if ($('#drpdn_update_roles').val().toLowerCase() != role) {
			var user = {
					emailID: email_id,
					role: role,
					domain: domain,
					newRole:  $('#drpdn_update_roles').val().toLowerCase()
			}
				
	    	LASLogger.instrEvent('application.users.role_change', {
	    		user : email_id,
	    		domain: domain,
	    		new_role : $('#drpdn_update_roles').val().toLowerCase(),
	    		old_role : role
	    	});

			$.ajax({
					url : openke.global.Common.getContextRoot()+"rest/"+domain+"/user/"+ btoa(email_id),
					type : "PUT",
					contentType: "application/json; charset=utf-8",
					data : JSON.stringify(user),
					success : refreshTables,
					error : function(jqXHR, textStatus, errorThrown) {
						bootbox.alert(jqXHR.responseJSON.reason);
					}
			});			
		}
		$('#updateRole').modal('hide');
	});
}

function updateStatus(email_id, status, role, domain) {
	$('#updateStatus').modal('show');
	$('#drpdn_update_status option[value="' + status + '"]').prop("selected",true);

	$('#btUpdateStatus').on(
			'click',
			function() {
				if ($('#drpdn_update_status').val().toLowerCase() != status) {
					var user = {
							emailID: email_id,
							role: role,
							domain: domain,
							newStatus:  $('#drpdn_update_status').val().toLowerCase()
					}
						
			    	LASLogger.instrEvent('application.users.status_change', {
			    		user : email_id,
			    		domain: domain, 
			    		new_status : $('#drpdn_update_status').val().toLowerCase(),
			    		old_status : status
			    	});

					$.ajax({
							url : openke.global.Common.getContextRoot()+"rest/"+domain+"/user/"+ btoa(email_id),
							type : "PUT",
							contentType: "application/json; charset=utf-8",
							data : JSON.stringify(user),
							success : refreshTables,
							error : function(jqXHR, textStatus, errorThrown) {
								bootbox.alert(jqXHR.responseJSON.reason);
							}
					});							
				}
				$('#updateStatus').modal('hide');
			});
}

return {
	initialize: initialize,
	updateRole : updateRole,
	updateStatus: updateStatus
};
}());
