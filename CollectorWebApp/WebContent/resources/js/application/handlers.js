function refreshTables() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/handler/document", refreshDocHandlerTable);
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/handler/source", refreshSrcHandlerTable);
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/handler/annotator", refreshAnnotatorTable);
}

function refreshDocHandlerTable(data) {
	var tblDocumentHandlers = $('#tblDocumentHandlers').DataTable();
	tblDocumentHandlers.clear();
	for (var i = 0; i < data.length; i++) {
		newRow = data[i];
		// LASLogger.logObject(LASLogger.LEVEL_TRACE,newRow);
		var mimetype  = data[i].mimeType + "";
		var mimes = mimetype.split(",");
		var newmime = "";
		for (var j=0; j<mimes.length; j++){
			if (j>0) {newmime = newmime +",<br/>";} 
			 newmime = newmime+ mimes[j] ;
		}
		newRow.mimeType = newmime;
		tblDocumentHandlers.row.add(newRow);
	}
	tblDocumentHandlers.draw();
}

function refreshAnnotatorTable(data) {
	var tblAnnotators = $('#tblAnnotators').DataTable();
	tblAnnotators.clear();
	for (var i = 0; i < data.length; i++) {
		newRow = data[i];
		tblAnnotators.row.add(newRow);
	}
	tblAnnotators.draw();
}

function refreshSrcHandlerTable(data) {
	var tblSourceHandlers = $('#tblSourceHandlers').DataTable();
	tblSourceHandlers.clear();
	for (var i = 0; i < data.length; i++) {
		newRow = data[i];
		
		var prettyConfig = escapeHtmlNewLine2(JSON.stringify(newRow.sourceHandlerConfiguration, null, '\t'));
		var c = "<div class=scrollable>" + prettyConfig + "</div>";
		newRow.sourceHandlerConfiguration = c;		

		tblSourceHandlers.row.add(newRow);
	}
	tblSourceHandlers.draw();
}

function showSrcParameters(sourceHandlerName) {
	event.preventDefault();
	$("#tblSrcParameters tbody tr").remove();
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/handler/source/" + sourceHandlerName + "/param",
		function(result) {
			var parameters = result.parameters;
		 	for (i=0; i<parameters.length;i++) {
		 		var name        = parameters[i].name;
		 		var description = parameters[i].description
		 		var required    = parameters[i].required
		 		var example     = parameters[i].example
				$("#tblSrcParameters tbody").append("<tr><td>" + name + "</td><td>" + description + "</td><td>" + required + "</td><td>" + example + "</td></tr>");
			}
		 	$('.modal-dialog').css('width', '80%');
		 	$('.modal-dialog').css('margin-left', '10%');
			$('#srcParameters').modal('show');
		});
}


LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);
$(document).ready(function() {
	LASLogger.instrEvent('application.handlers');

	window.page = "handlers"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	var tblDocumentHandlers = $('#tblDocumentHandlers').DataTable({
		"pageLength" : 50,
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "name",
			"width" : "0"
		},{
			"data" : "processingOrder",
			"width" : "1%"
		}, {
			"data" : "mimeType",
			"width" : "30%"
		}, {
			"data" : "documentDomain",
			"width" : "0"
		}, {
			"data" : "description",
			"width" : "60%"
		} ],
		"order" : [ [ 0, "asc" ] ]
	});

	var tblAnnotatorHandlers = $('#tblAnnotators').DataTable({
		"pageLength" : 50,
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "name",
			"width" : "0"
		},{
			"data" : "code",
			"width" : "5%"
		}, {
			"data" : "description",
			"width" : "50%"
		}, {
			"data" : "mimeType",
			"width" : "5%"
		}, {
			"data" : "executionPoint",
			"width" : "5%"
		} , {
			"data" : "priority",
			"width" : "5%"
		} ],
		"order" : [ [ 0, "asc" ] ]
	});	
	
	var tblSourceHandlers = $('#tblSourceHandlers').DataTable(
		{
			"pageLength" : 50,
			"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
			"columns" : [
					{
						"data" : "sourceHandlerName",
						"width" : "5%"
					},
					{
						"data" : "description",
						"width" : "30%"
					},
					{
						"data" : "sourceHandlerConfiguration",
						"width" : "25%"
					},
					{
						"data" : "configurationParameters",
						"width" : "30%",
						"fnCreatedCell" : function(
								nTd, sData, oData,
								iRow, iCol) {
							$(nTd)
									.html(
											"<a href= '#' onclick='showSrcParameters(\""
													+ oData.sourceHandlerName
													+ "\")'>Parameters</a>");
						}

					} ],
			"order" : [ [ 0, "asc" ] ]
		});

		bootbox.setDefaults({
			show : true,
			animate : false,
			cancelButton : false
		});
		$('#srcParameters').modal('hide');
		refreshTables();

		
		$("#btnDomainHome").on('click', function() {
			var completeLocation =openke.global.Common.getPageURLPrefix() +"/"
			LASLogger.instrEvent('application.handlers.link', {link : completeLocation},function() {window.location = completeLocation;} );
		});

	});
