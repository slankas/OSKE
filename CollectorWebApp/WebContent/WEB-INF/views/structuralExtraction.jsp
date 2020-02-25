<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.structuralExtraction"); %>

<title>OpenKE: Structural Extraction</title>
<link rel="stylesheet" href="${applicationRoot}resources/css/conceptcss.css?build=${build}" />
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css?build=${build}" />

<!-- jQuery File Upload Plugin -->
<link rel="stylesheet"	href="${applicationRoot}resources/jQuery-File-Upload-9.19.1/css/jquery.fileupload.css">
</head>

<body>
<%
String importMessages = "";
if (request.getAttribute("importMessages") != null) {importMessages = ((org.json.JSONObject)request.getAttribute("importMessages")).toString();}
%>
<script>
	var importMessages = ${importMessages}
</script>
<%@include file="header.jsp"%><br>
<div class="container-fluid">
<div class="row">
  <div class="col-md-12"><header><h5>Structural Annotation / Extraction</h5></header></div>		
</div>

<div class="row">
<div class="title_box" id="newExtractRecordArea">
    <div class="title">Create New Structural Annotation Record</div>
    <div class="content">
		<form class="form-inline" onsubmit="return false;">
		  <div class="form-group">
		    <label for="newHostname">Hostname</label>
		    <input type="text" class="form-control" id="newHostname" placeholder="www.somesite.com">
		  </div>
		  <div class="form-group">
		    <label for="newPathRegex">Path Regex</label>
		    <input type="text" class="form-control" id="newPathRegex" placeholder="">
		  </div>
		  <div class="form-group">
		    <label for="newParentRecord">Parent Record</label>
		    <select class="form-control" id="newParentRecord"></select>
		  </div>
		  <div class="form-group">
		    <label for="newRecordName">Record / Field Name</label>
		    <input type="text" class="form-control" id="newRecordName" placeholder="">
		  </div>
		  <div class="form-group">
		    <label for="newCSSSelector">CSS Selector</label>
		    <input type="text" class="form-control" id="newCSSSelector" placeholder="">
		  </div>
		  <div class="form-group">
		    <label for="newExtractBy">Extract By</label>
		    <select class="form-control" id="newExtractBy">
		    	<option selected value='text'>Text</option>
		    	<option value='html'>HTML</option>
		    	<option value='text:regex'>Text with Regex</option>
		    	<option value='html:regex'>HTML with Regex</option>
		    </select>
		  </div>		  <div class="form-group">
		    <label for="newExtractRegex">Extract Regex</label>
		    <input type="text" class="form-control" id="newExtractRegex" placeholder="">
		  </div>		  
		  <button id="btCreateRecord" type="submit" class="btn btn-primary"><i class="fas fa-plus"></i>&nbsp;Create</button>&nbsp;
		  <button id="btClearFields" class="btn btn-primary">Clear</button>
		</form>
    </div>
</div>
</div>
<div class="row">
<p>
</div>
<div id="contentArea" class="table-responsive col-md-12">
	<table id="contentTable" class="table-bordered table-striped">
		<thead><tr><th><!-- Action --></th> <th>Host Name</th><th>Path Regex</th><th>Parent Record</th><th>Record/Field Name</th><th>CSS Selector</th><th>Extract By</th><th>Extract Regex</th></tr></thead>

	</table>
</div>

<div class="row" >
  <div  class="col-md-4" style="margin-left:15px">
	<button id="btExportRecords" class="btn btn-primary"><i class="fas fa-file-export"></i>&nbsp;Export all Records</button>
  </div>
  <div  class="col-md-4">
	<form id="fileImportForm" action='${applicationRoot}rest/<%=domain%>/structuralExtraction/importCSV' method="POST"	enctype="multipart/form-data" style="display:inline-block">
		Import CSV records:
		<input type="file" name="importFile" id="importFile"style="display:inline-block">
		<button id="btImportFile" class="btn btn-primary" style="display:inline-block"><i class="fas fa-file-import"></i>&nbsp;Import</button>
	</form>
  </div>  
</div>

<div class="row" >
  <div  class="col-md-12">
	<hr width=100%>
  </div>
</div>
	
<div class="row">
	<div class="col-md-12"><h5>Test Structural Annotation / Extraction</h5></div>
</div>

<div class="row">
	<div class="col-md-12">
	    <label for="testURL" style="display:inline;"><span style="font-weight: bold;">URL:</span></label>
	    <input style="display:inline; width: 400px;" type="text" class="form-control" id="testURL" placeholder="http://somedomain.com">
	    <label for="testURL" style="display:inline;">Show log:</label>
	    <input style="display:inline;" type="checkbox" id="cbShowLog"> 
	    <button id="btTestURL" style="display:inline" type="submit" class="btn  btn-primary"><i class="fas fa-check"></i>&nbsp;Test URL</button>
	</div>
</div>	

<div class="row">
	<p>&nbsp;
</div>	

<div class="row" id="fileUploadSection">
  <div class="col-md-12">
    <span class="btn btn-primary fileinput-button">
        <i class="fas fa-plus"></i>
        <span>Select files to test ...</span>
        <!-- The file input field used as target for the file upload widget -->
        <input id="fileupload" type="file" name="files[]" multiple>
    </span> (or drag a file onto this screen)
    <br>
    <br>
    <!-- The global progress bar -->
    <div id="progress" class="progress">
        <div class="progress-bar progress-bar-success"></div>
    </div>
    <!-- The container for the uploaded files -->
    <div id="files" class="files"></div>
  </div>
</div>
	
<div class="row" >
  <div  class="col-md-12">
	<hr width=100%>
  </div>
</div>

<div class="row">
  <div class="col-md-12">
	<div class="floatleft">
	  <button type="submit" class="btn btn-primary" id="btnDomainHome"><i class="fas fa-home"></i>&nbsp;&nbsp;Domain Home</button>			
	</div>
  </div>
</div>	


<div class="row" >
  <div  class="col-md-12">
    <p>&nbsp;<p>Note: <a href='https://jsoup.org/cookbook/extracting-data/selector-syntax' target=_blank>jSoup selector syntax</a> used for the CSS selector.
  </div>
</div>
</div>
</body>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/jqTree.1.3.8/tree.jquery.js"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/structuralExtraction.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/elasticSearch/jsontree.min.js"></script>

<script src="${applicationRoot}resources/jQuery-File-Upload-9.19.1/js/jquery.fileupload.js"></script>      <!-- The Templates plugin is included to render the upload/download listings -->


	<!-- Bootstrap JS is not required, but included for the responsive demo navigation -->
	<!-- <script src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script> -->
	
	<!-- The XDomainRequest Transport is included for cross-domain file deletion for IE 8 and IE 9 -->
	<!--[if (gte IE 8)&(lt IE 10)]>
<script src="resources/js/fileUpload/js/cors/jquery.xdr-transport.js"></script>
<![endif]-->

<!-- To correct the space in pagination of datatable -->
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
</html>