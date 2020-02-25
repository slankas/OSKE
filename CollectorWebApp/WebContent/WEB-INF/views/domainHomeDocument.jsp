<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.document"); %>

<title>OpenKE: Scratchpad</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/documentHome.css?build=${build}" />

<!--  summernote wysyg editor -->
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/summernote_0.8.11/summernote-bs4.css" />


</head>
<body>
<%@include file="header.jsp"%><br>
<input type="hidden" name="documentUUID" id="documentUUID" value="<%=request.getAttribute("documentUUID")%>" />

<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>
	<div class="documentContainer">

		<div class="documentList">
			<div class="card card-default">
			  <div class="card-header">
			    <strong>Scratchpads</strong>
			    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btDocumentNew">Create</button></div>
			  </div>
			  <div class="card-body" id="documentLinks">

			  </div>
			</div>
		</div>



		<div class="documentDetail">
			<div class="card card-default">
				  <div class="card-header">
				  	<h5 style="display: inline-block" class="card-title">Scratchpad Details</h5>
				    
				    <div style="float:right;vertical-align: top; position:relative; top: -4px;">
				    	<button class="btn btn-primary btn-sm" id="btSave" disabled data-toggle="tooltip" data-placement="bottom" title="Save the current Scratchpad">Save</button>
				    	<button class="btn btn-outline-primary btn-sm" id="btMakeCurrentDocument" data-toggle="tooltip" data-placement="bottom" title="Make this the current Scratchpad">Make Current</button>
				        <button class="btn btn-outline-primary btn-sm" id="btExport" data-toggle="tooltip" data-placement="bottom" title="Export to Word">Export</button>
				        <button class="btn btn-outline-primary btn-sm" id="btExportSrc" data-toggle="tooltip" data-placement="bottom" title="Export to archive(.zip) with sources">Export with Sources</button>
				        <button class="btn btn-outline-primary btn-sm" id="btExportZip" data-toggle="tooltip" data-placement="bottom" title="Export archive(.zip) with sources to server for transfer">Export for Transfer</button>
				        
				        <button class="btn btn-outline-primary btn-sm" id="btSetActive" data-toggle="tooltip" data-placement="bottom" title="Enable this Scratchpad">Mark Active</button>
				        <button class="btn btn-outline-primary btn-sm" id="btSetInactive" data-toggle="tooltip" data-placement="bottom" title="Disable this Scratchpad">Mark Inactive</button>
				        <button class="btn btn-danger btn-sm" id="btDelete" data-toggle="tooltip" data-placement="bottom" title="Delete this Scratchpad">Delete</button></div>
				  </div>
				  <div class="card-body">
					<div id="documentEdit"> <!-- put editor panel in here -->
						<strong>Name:</strong> &nbsp; <input name="documentName" id="documentName" minlength="3" maxlength="255" type='text' size=60 class='form-control'>
						<p>&nbsp;<p>
						<div id="contentsBox"><div id="contents"></div></div>
						
					</div>
						
					
					<div id="documentEmpty">
					  <i>No document is selected or set as active.</i>
					</div>
					<div id="documentLoading">
					  <i>Loading ...</i>
					</div>							
				 </div>
			</div>
		</div>		
	</div>
	<% } %>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jszip.min_3.1.5.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASExport.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/ProjectDocument.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/domainHomeDocument.js?build=${build}"></script>

	<!--  summernote wysyg editor -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/summernote-bs4_0.8.11.js"></script>
	
	 <a  href="#" id="downloadLink"></a>
</body>
</html>
