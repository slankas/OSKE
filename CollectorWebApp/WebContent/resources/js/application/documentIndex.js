LASLogger.setCurrentLevel(LASLogger.LEVEL_INFO);


$(document).ready(function() {
    LASLogger.instrEvent('application.document_indexes');

	$('#btHome').click(function(){
		LASLogger.instrEvent('application.documentIndex.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});

   
	var table = $('#tblDocumentIndexes').DataTable({
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"columns" : [ {
			"data" : "action",
			"width" : "50",
		}, {
			"data" : "name",
			"width" : "0",
		}, {
			"data" : "dateCreated",
			"width" : "130",
		}, {
			"data" : "ownerEmail",
			"width" : "0",
		}, {
			"data" : "numDocuments",
			"width" : "150",
		}, ],
		"order" : [ [ 1, "asc" ] ]
	});

	refreshIndexTable();

});

function refreshIndexTable() {
	$.getJSON(openke.global.Common.getRestURLPrefix()+"/documentIndex/",populateIndexTable );	
}


function populateIndexTable(data) {

	var table = $('#tblDocumentIndexes').DataTable();
	table.clear();
	for (var i = 0; i < data.length; i++) {
		newRow = {}

		
		newRow.action =  "<i onclick='showIndex(\"" + data[i].id + "\");' class='fas fa-search'></i>";
		newRow.action += "&nbsp;&nbsp;&nbsp;<i onclick='javascript:deleteIndex(\"" + data[i].id + "\");' class='fas fa-trash-alt'></i>";

		newRow.name = "<span onclick='showIndex(\"" + data[i].id + "\")'>"+data[i].name+"</span>";
		newRow.dateCreated = data[i].dateCreated.substring(0,10);
		newRow.ownerEmail = data[i].ownerEmail;
		newRow.numDocuments = data[i].numDocuments
		table.row.add(newRow);

	}
	table.draw();

}


function showIndex(documentIndex) {
	LASLogger.instrEvent('application.document_indexes.view', { "area" : "normal", "documentIndex": documentIndex});
	DocumentIndex.showIndexView("normal",documentIndex)
}
	
function deleteIndex(documentIndex) {
	//TODO: should validate that we are the owner or an administrator for the domain

	LASLogger.instrEvent('application.document_indexes.deleteConfirm', { "area" : "normal", "documentIndex": documentIndex});

	bootbox.confirm("Are you sure you wish to delete this index?  The action cannot be reversed.", 
			       function(result) {
		if (result) {
			var url = openke.global.Common.getRestURLPrefix()+"/documentIndex/normal/"+documentIndex;
		    $.ajax({
				type : "DELETE",
				url : url,
				success : function(data) {
					//alert("Index has been removed")
					LASLogger.instrEvent('application.document_indexes.delete', { "area" : "normal", "documentIndex": documentIndex});
					setTimeout(refreshIndexTable, 1500);
				},
				error : function(data) {
					//TODO
					// alert("error");
				},
				dataType : "text",
			});			
		}
		else {
			LASLogger.instrEvent('application.document_indexes.deleteCancel', { "area" : "normal", "documentIndex": documentIndex});
		}
		
	});
	
	
}	

