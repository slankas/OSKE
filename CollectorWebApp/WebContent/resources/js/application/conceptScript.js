
var treeLoadData=[];
var tblConcepts;             // reference to the dataTable
var currentCategoryID = "";  // What categoryID is currently selected in the tree?

$(document).ready(function() {	
	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.concept.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});
	$('#importConcepts').click(function(){
		LASLogger.instrEvent('application.concept.importConcepts', {}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/conceptImport";});
	});
	
	
	$('#conceptArea').hide();
	$('#exportAllData').click(function(event) {
		
	    $.ajax({
			url : openke.global.Common.getRestURLPrefix()+"/concepts",
			contentType: "application/json; charset=utf-8",
			type : "GET",
			dataType : "JSON",
			success: function(data) {
				exportJsonToCSV("concepts.csv",data.concepts,["fullCategoryName","name","type","regex"]);
			}
		});
		return false;
	});
	
	tblConcepts = $("#conceptTable").DataTable({
		"pagingType": "full_numbers",
		"pageLength" : 25,
		"lengthMenu": [[10, 25, 50,100, -1], [10, 25, 50, 100, "All"]],
		"language": {
            "zeroRecords": "No records found",
            "infoEmpty": ""
		},
		"columns" : [
		     {
				"data" : "name",
				"width" : "30%"
			},
			{
				"data" : "type",
				"width" : "30%"
			},
			{
				"data" : "regex",
				"width" : "30%",
			},
			{
				"data"  : "id",
				"width" : "10%",
				"fnCreatedCell" : function(	nTd, sData, oData, iRow, iCol) {
					$(nTd).html("<span onclick='deleteSingleConcept(\""+oData.id+"\",this)' class='fas fa-times icon-remove'></span>");
				}
			} ]
    });	
});

$(function() {   
	// file upload callbacks
	$('#fileupload').bind('fileuploaddone', function (e, data) {
		//alert(currentCategoryID)
		
		for (var i=0; i < data.files.length; i++) {
			conceptData = JSONTree.create(data.result.files[i].concepts);
 			openNewWindow(conceptData,data.result.files[i].name);
		}

	});
	
	// set up drag and drop for uploads
	var dropZone = document.getElementById('drop-zone');

	dropZone.ondrop = function(e) {
	    var file_names = [];
		for (var i = 0; i < e.dataTransfer.files.length; i++) {
	    	file_names.push(e.dataTransfer.files[i].name);
		}

    	LASLogger.instrEvent('application.upload_files.upload_file-drag', {
    		file_names :  file_names
    	});

		e.preventDefault();
		this.className = 'upload-drop-zone';

		//startUpload(e.dataTransfer.files)
	}

	dropZone.ondragover = function() {
		this.className = 'upload-drop-zone drop';
		return false;
	}

	dropZone.ondragleave = function() {
		this.className = 'upload-drop-zone';
		return false;
	}

	
    /*Initialize the tree*/
    $('#tree1').tree({
        data: treeLoadData,
        dragAndDrop: true
    });
    
	var treeDropZone = document.getElementById('tree1');

	dropZone.ondrop = function(e) {
	    var file_names = [];
		for (var i = 0; i < e.dataTransfer.files.length; i++) {
	    	file_names.push(e.dataTransfer.files[i].name);
		}

    	LASLogger.instrEvent('application.upload_files.upload_file-drag', {
    		file_names :  file_names
    	});

		e.preventDefault();
		this.className = 'upload-drop-zone';

		//startUpload(e.dataTransfer.files)
	}
    
    /*Ajax call to get the All the categories from server*/   
    $.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/concepts/category",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
			treeLoadData=data['conceptCategories'];
			
			 /*Inserting data into tree rootid="00000000-0000-0000-0000-000000000000"*/  
		    $.each( treeLoadData, function( i, value ){
		    	
		    	var pid = treeLoadData[i]['parentid'];
		    
		    	if (pid == "00000000-0000-0000-0000-000000000000")	{
		    		$('#tree1').tree('appendNode',
		            	{   
		            		id:treeLoadData[i]['id'],
		            	    name:treeLoadData[i]['name'] ,
		                    parentid:treeLoadData[i]['parentid']
		            	}
		            );
		    	} else { 
		    		$('#tree1').tree('appendNode',
		            	{   
		            		id:treeLoadData[i]['id'],
		            	    name:treeLoadData[i]['name'] ,
		                    parentid:treeLoadData[i]['parentid']
		            	},
		            	$('#tree1').tree('getNodeById',treeLoadData[i]['parentid'])
		           );
		    	}
		    });
		
		}
	});
   

    
    $("#catName").keyup(function(event){
    	 if(event.keyCode == 13) {
            $("#btAdd").click();
        }
    });
   $('#btAdd').click(addCategory);

	$('#btDeleteCategory').prop('disabled',true);
	   

	// add a row to put new data in ...
	$("#conceptTable").append($('<tbody><tr> <td><input type="text" id="nameinput" placeholder="Add Name" class="form-control"></td><td><input type="text" id="typeinput" placeholder="Add Type" class="form-control"></td><td><input type="text" id="regexinput" placeholder="Add Regex" class="form-control"><span id="errormsg"></span></td><td><button type="submit" id="btAddConcept" class="btn btn-default btn-success btn-sm confirm " style="margin-bottom: 5px;">Add</button></td></tr></tbody>'));
		
	$('#btAddConcept').click(addConceptRecord);	
	$('#btDeleteCategory').click(deleteCategory);
	
	$('#tree1').bind( 'tree.select', function(event) {
		var node = event.node;
		
		if (event.node)  {
			$('#btDeleteCategory').prop('disabled',false);
			
		    currentCategoryID = node['id'];
		      
		    $('#conceptArea').show();
		       
		    loadConceptTable(node['id']);
		}
		else { // no category nodes are currently selected
			currentCategoryID = "";
			$('#btDeleteCategory').prop('disabled',true);

			$('#conceptArea').hide();
			
			tblConcepts.clear();
		}
	});  
	$('#fileupload').attr('action', openke.global.Common.getRestURLPrefix()+"/concepts/testFile");
});


// Event handler when the add button is clicked (or user hits return)
function addCategory()  {
	if (  $('#catName').val()==='') {
		return;
	}

	var pnode = $('#tree1').tree('getSelectedNode');
	var parentName;
	var catid;
	var rootid = "00000000-0000-0000-0000-000000000000"
		
	/* Assigning parent based on selected node, if any, otherwise parent is root*/
	if (pnode == false)	{
		parentName=rootid;
	} else	{
		parentName=pnode['id'];
	}
	
	
    /*Tree node description*/	
	var newNode= {
				name:$('#catName').val(),
				parentid:parentName //$('#tree1').tree('getNodeByName',$('#catName').val()).parent,	
	}
		
	/*sending node data to server*/
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/concepts/category",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(newNode),
		dataType : "JSON",
		success: function(data) {
				// get the UID from the result
			
				newNode.id = data.concept.id;
				
				// Add node in UI(Front End)
				$('#tree1').tree( 'appendNode', newNode, pnode );
				$('#catName').val(''); //Reset text
			},
		error: function (jqXHR, textStatus, errorThrown) {
			// TODO
		}
	
	});
}



function addConceptRecord() {   
	if( $('#nameinput').val() === '' || $('#typeinput').val() === '' || $('#regexinput').val() === '') {  
		bootbox.alert("You must enter the name, type, and regular expression.");
		return;
	}
	if ($('#tree1').tree('getSelectedNode') == false) {
		bootbox.alert("A category must be selected first from the left.");
		return;
	}
	
	var tableRowData={
			categoryid:$('#tree1').tree('getSelectedNode')['id'],
			name:$('#nameinput').val(),
			type:$('#typeinput').val(),
			regex:$('#regexinput').val()
	}
	

	$.ajax ({
		url : openke.global.Common.getRestURLPrefix()+"/concepts/concept",
		type : "POST",
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(tableRowData),
		dataType : "JSON",
		success: function(data) {
		
			if ( data.hasOwnProperty('error')) {
				$("#errormsg").html(data['error']).css('color', 'red');
			}
			else {
				$('#errormsg').empty();
				tableRowData.id = data.concept.id;
				tblConcepts.row.add(tableRowData).draw();
				$('#nameinput').val('');
				$('#typeinput').val('');
				$('#regexinput').val('');
			}
		}
	});
	
}

function deleteNode(node) {
	var idToDelete=node['id'];
	   $.ajax({
    		url : openke.global.Common.getRestURLPrefix()+"/concepts/category/"+idToDelete,
    		type : "DELETE",
    		contentType: "application/json; charset=utf-8",
    		dataType : "JSON",
    		success: function(data) {
    			$('#tree1').tree('removeNode', node);
    		}
    	});	
	   
	   $('#conceptArea').hide();
		$('#btDeleteCategory').prop('disabled',true);
}

function deleteCategory() {
	
	var delnode= $('#tree1').tree('getSelectedNode');

	//Confirm to delete if sub-categories or associated concepts present    
	if(delnode.children.length > 0 ||tblConcepts.rows().count() > 0 ) {

		bootbox.confirm({ 
			size: 'medium',
			title:'Are you sure?',
			message: "You have subcategories or concepts associated", 
			buttons:{
				'confirm': {
		            label: 'Delete'
				}
			},
			callback: function(result){ 

				if(result)
					deleteNode(delnode);
			}

		});
	}
	else {
		deleteNode(delnode);
	}
}


function loadConceptTable(categoryID) {
    $.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/concepts/category/"+categoryID+"/concept",
		contentType: "application/json; charset=utf-8",
		
		dataType : "JSON",
		success: function(data) {
			tblConcepts.clear();
			for (var i = 0; i < data.concepts.length; i++) {
				var newRow = data.concepts[i];			
				tblConcepts.row.add(newRow);
			}
			tblConcepts.draw();
		}
});    
}


function deleteSingleConcept(conceptUUID,obj) {
	$.ajax({
		url : openke.global.Common.getRestURLPrefix()+"/concepts/concept/"+conceptUUID,
		type : "DELETE",
		contentType: "application/json; charset=utf-8",
		dataType : "JSON",
		success: function(data) {
		    tblConcepts.row( $(obj).parents('tr') ).remove().draw();
		}
	});
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

