var treeLoadData=[];


$(document).ready(function() {
	$('#btnDomainHome').click(function(){
		LASLogger.instrEvent('application.conceptImport.home', {}, function() {window.location=openke.global.Common.getPageURLPrefix();});
	});
	$('#btnConcepts').click(function(){
		LASLogger.instrEvent('application.conceptImport.concepts', {}, function() {window.location=openke.global.Common.getPageURLPrefix()+"/concept";});
	});
		
	
});

$(function() {   
	// file upload callbacks
	$('#fileupload').bind('fileuploaddone', function (e, data) {

        var message = "<table class='table'><tr><th>File name</th><th>Number Added</th></tr>"
		for (var i=0; i < data.result.files.length; i++) {
			var name = data.result.files[i].name;
			   
            message += "<tr><td>" +name +"</td><td>"+data.result.files[i].numRecordsAdded+"</td></tr>"
            			
		}
		message += "</table>"
		bootbox.alert({
				message: message,
				title:"Import Details"
			});			

        
        
		for (var i=0; i < data.result.files.length; i++) {
		
			if (data.result.files[i].badRegexes.length > 0) {
				var badArray = data.result.files[i].badRegexes;
   
	            var message = "<table class='table'><tr><th>Name</th><th>Type</th><th>Regular Expression</th></tr>"
	            
	            for (var j=0; j<badArray.length;j++) {
	            	var record = badArray[j];
	            	
	            	message += "<tr><td>" +record.name +"</td><td>"+record.type+"</td><td>"+record.regex+"</td></tr>"
	            }
	            	
				message += "</table>"
					
				bootbox.alert({
					message: message,
					title:"Invalid Regular Expressions"
				});
			}
			
			if (data.result.files[i].badNames.length > 0) {
				var badArray = data.result.files[i].badNames;
			
	            var message = "<table class='table'><tr><th>Name</th></tr>"
	            
	            for (var j=0; j<badArray.length;j++) {
	            	var record = badArray[j];
	            	
	            	message += "<tr><td>" +record +"</td></tr>"
	            }
	            	
				message += "</table>"
					
				bootbox.alert({
					message: message,
					title:"Invalid Concept Names"
				});
			}			
			
		}
		
	})
	
	// set up drag and drop for uploads
	var dropZone = document.getElementById('drop-zone');

	dropZone.ondragover = function() {
		this.className = 'upload-drop-zone drop';
		return false;
	}

	dropZone.ondragleave = function() {
		this.className = 'upload-drop-zone';
		return false;
	}
	
	dropZone.ondrop = function(e) {
	    var file_names = [];
		for (var i = 0; i < e.dataTransfer.files.length; i++) {
	    	file_names.push(e.dataTransfer.files[i].name);
		}

    	LASLogger.instrEvent('application.concepts.upload', {
    		file_names :  file_names
    	});

		e.preventDefault();
		this.className = 'upload-drop-zone';
	}
	
	$('#fileupload').attr('action', openke.global.Common.getRestURLPrefix()+"/concepts/upload");
});




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

