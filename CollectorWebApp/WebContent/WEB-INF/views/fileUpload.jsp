<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.fileUpload"); %>

<title>File Upload</title>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/dataTables.bootstrap.css">

<!-- <link rel="stylesheet" href="resources/css/fileUpload/css/style.css">
 -->
<!-- blueimp Gallery styles -->
<link rel="stylesheet"	href="${applicationRoot}resources/css/fileUpload/css/blueimp-gallery.min.css">
<!-- CSS to style the file input field as button and adjust the Bootstrap progress bars -->
<link rel="stylesheet"	href="${applicationRoot}resources/css/fileUpload/css/jquery.fileupload.css">
<link rel="stylesheet"	href="${applicationRoot}resources/css/fileUpload/css/jquery.fileupload-ui.css">

</head>
<body>
<%@include file="header.jsp"%><br>
<div class="container-fluid">
	<div class="row">
	    <div class="col-lg-12">
		<form id="fileupload" action='upload' method="POST" 	enctype="multipart/form-data">
			<!-- Redirect browsers with JavaScript disabled to the home page -->
			<noscript>
				<input type="hidden" name="redirect" value="${applicationRoot}">
			</noscript>
			<!-- The fileupload-buttonbar contains buttons to add/delete files and start/cancel the upload -->
			<div class="fileupload-buttonbar">
				
					<!-- The fileinput-button span is used to style the file input field as button -->
					<h4>Select files from your computer</h4>
					<span class="btn btn-primary fileinput-button"> <i class="fas fa-plus"></i> <span>Add files...</span> 
					<input type="file" name="files[]" multiple type="submit" id="addFiles">
					</span>
					<button name="btnClearAll" id="btnClearAll" type="reset"	class="btn btn-clear cancel" onclick="clearFiles()">
						<i class="fas fa-trash-alt"></i> <span>Clear All</span>
					</button>
					<button style="visibility: hidden;" name="btnCancel" id="btnCancel"	type="reset" class="btn btn-warning cancel">
						<i class="fas fa-ban-circle"></i> <span>Cancel	upload</span>
					</button>

					<!-- The global file processing state -->
					<!--<span class="fileupload-process"></span> -->
					<h4>Or drag and drop files below</h4>
					<div class="upload-drop-zone" id="drop-zone">Just drag and drop files here</div>


					<!-- The global progress state -->
					<div class="col-lg-7-padding fileupload-progress fade">
						<!-- The global progress bar -->
						<div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" style="width: 80%;">
							<div class="progress-bar progress-bar-success" style="width: 0%;"></div>
						</div>
						<!-- The extended global progress state -->
						<div class="progress-extended">&nbsp;</div>
						<HR>
					</div>


					<!-- The table listing the files available for upload/download -->
					<table id="tblPresentation" role="presentation" class="table-striped-presentation">
						<tbody class="files"></tbody>
					</table>
					<!-- table-striped-presentation  -->
				


			</div>
		</form>
		</div>
	</div>

	<div id="blueimp-gallery"
		class="blueimp-gallery blueimp-gallery-controls" data-filter=":even">
		<div class="slides"></div>
		<h3 class="title"></h3>
		<a class="prev">‹</a> <a class="next">›</a> <a class="close">×</a>
		<a class="play-pause"></a>
		<ol class="indicator"></ol>
	</div>

		<div class="row">
			<div class="col-md-12">
				<div class="btn-group floatleft">
					<button type="submit" class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span>&nbsp;Domain Home</button>
				</div>
			</div>
		</div>
</div>
	<!-- The template to display files available for upload -->
	<script id="template-upload" type="text/x-tmpl">
{% for (var i=0, file; file=o.files[i]; i++) { %}
    <tr class="template-upload fade">
        <td>
            <p class="name">{%=file.name%}</p>
            <strong class="error text-danger"></strong>
        </td>
        <td>
            <p class="size">Processing...</p>
            <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0"><div class="progress-bar progress-bar-success" style="width:0%;"></div></div>
        </td>
        <td>
            {% if (!i && !o.options.autoUpload) { %}
                <button class="btn btn-blue start">
                    <i class="fas fa-upload"></i>
                    <span>Start</span>
                </button>
            {% } %}
            {% if (!i) { %}
                <button class="btn btn-warning cancel">
                    <i class="fas fa-ban-circle"></i>
                    <span>Cancel</span>
                </button>
            {% } %}
        </td>
    </tr>
{% } %}
</script>
	<!-- The template to display files available for download (Files already uploaded) -->
	<script id="template-download" type="text/x-tmpl">
{% for (var i=0, file; file=o.files[i]; i++) { %}
    <tr class="template-download fade">
        <td>
            {% if (file.success) { %}
                <div><span class="label label-cool">Success</span> {%=file.name%}</div>
            {% } %}
            {% if (file.error) { %}
                <div><span class="label label-danger">Error</span> {%=file.name + ' : ' + file.error%}</div>
            {% } %}
        </td>
        <td>
            <span class="size">{%=o.formatFileSize(file.size)%}</span>
        </td>
        <td>
            {% if (file.success) { %}
            {% } else { %}
                <button class="btn btn-warning cancel">
                    <i class="fas fa-ban-circle"></i>
                    <span>Cancel</span>
                </button>
            {% } %}
        </td>
    </tr>
{% } %}
</script>

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.dataTables.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/dataTables.bootstrap.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASUploadFile.js?build=${build}"></script>

	<!-- The jQuery UI widget factory, can be omitted if jQuery UI is already included -->
	<script src="${applicationRoot}resources/js/fileUpload/js/vendor/jquery.ui.widget.js"></script>

	<!-- The Templates plugin is included to render the upload/download listings -->
	<script src="${applicationRoot}resources/js/fileUpload/js/tmpl.min.js"></script>

	<!-- The Load Image plugin is included for the preview images and image resizing functionality -->
	<script src="${applicationRoot}resources/js/fileUpload/js/load-image.all.min.js"></script>

	<!-- The Canvas to Blob plugin is included for image resizing functionality -->
	<script src="${applicationRoot}resources/js/fileUpload/js/canvas-to-blob.min.js"></script>

	<!-- Bootstrap JS is not required, but included for the responsive demo navigation -->
	<!-- <script src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>
 -->
	<!-- blueimp Gallery script -->
	<script src="${applicationRoot}resources/js/fileUpload/js/jquery.blueimp-gallery.min.js"></script>

	<!-- The Iframe Transport is required for browsers without support for XHR file uploads -->
	<script src="${applicationRoot}resources/js/fileUpload/js/jquery.iframe-transport.js"></script>

	<!-- The basic File Upload plugin -->
	<script src="${applicationRoot}resources/js/fileUpload/js/jquery.fileupload.js"></script>

	<!-- The File Upload processing plugin -->
	<script src="${applicationRoot}resources/js/fileUpload/js/jquery.fileupload-process.js"></script>

	<!-- The File Upload validation plugin -->
	<script src="${applicationRoot}resources/js/fileUpload/js/jquery.fileupload-validate.js"></script>

	<!-- The File Upload user interface plugin -->
	<script src="${applicationRoot}resources/js/fileUpload/js/jquery.fileupload-ui.js"></script>

	<!-- The main application script -->
	<script src="${applicationRoot}resources/js/fileUpload/js/main.js"></script>

	<!-- The XDomainRequest Transport is included for cross-domain file deletion for IE 8 and IE 9 -->
	<!--[if (gte IE 8)&(lt IE 10)]>
<script src="resources/js/fileUpload/js/cors/jquery.xdr-transport.js"></script>
<![endif]-->

</body>
</html>
