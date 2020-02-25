+function($) {
	//'use strict';
	
	LASLogger.instrEvent('application.upload_files');

	window.page = "upload_file"; // Page name to be stored in window object so that LasHeader.js could access it for logging

	// UPLOAD CLASS DEFINITION
	// ======================

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

}(jQuery);

$(document).ready(function() {
	$("#btnDomainHome").on('click', function() {
		var completeLocation =openke.global.Common.getPageURLPrefix() +"/"
		LASLogger.instrEvent('application.fileUpload.link', {link : completeLocation},function() {window.location = completeLocation;} );
	});
});

function clearFiles() {
    LASLogger.instrEvent('application.upload.clear');

	$("#tblPresentation > tbody").html("");
}

$("#addFiles").change(function() {
    var file_names = [];
    for (var i = 0; i < $(this).get(0).files.length; ++i) {
    	file_names.push($(this).get(0).files[i].name);
    }
	
    LASLogger.instrEvent('application.upload_files.upload_file-drag', {
		file_names : file_names
	});
});
