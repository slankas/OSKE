<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<%pageContext.setAttribute("currentPage","domain.manage.handlers");  %>
<title>OpenKE: Source and Document handlers</title>

<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
</head>
<body>
<%@include file="header.jsp"%><br>
<div class="container-fluid">
	<div class="row">

		<div class="form-group col-md-12">
			<table class="display table table-striped table-bordered table-tight" id="tblSourceHandlers">
				<thead>
					<tr>
						<th id="thdSourceHandlers" colspan="4">Source Handlers</th>
					</tr>
					<tr>
						<th class="thHeadings" id="thdName">Name</th>
						<th class="thHeadings" id="thdDescription">Description</th>
						<th class="thHeadings" id="thdInitialConfig">Initial Configuration</th>
						<th class="thHeadings" id="thdParameters">Parameters</th>
					</tr>
				</thead>
			</table>
			<p>
			<table class="display table table-striped table-bordered table-tight"	id="tblDocumentHandlers">
				<thead>
					<tr>
						<th id="thdDocumentHandlers" colspan="5">Document Handlers</th>
					</tr>
					<tr>
						<th id="thdName">Name</th>
						<th id="thdProcessingOrder">Processing Order</th>
						<th id="thdMimeType">Mime Type</th>
						<th id="thdDomain">Document Domain</th>
						<th id="thdDescription">Description</th>
					</tr>
				</thead>
			</table>
			<p>
			<table class="display table table-striped table-bordered table-tight" id="tblAnnotators">
				<thead>
					<tr><th id="thaAnnotators" colspan="5">Annotators</th></tr>
					<tr>
						<th id="thaName">Name</th>
						<th id="thaCode">Code</th>
						<th id="thaDescription">Description</th>
						<th id="thaMimeType">Mime Type</th>
						<th id="thaExecutionPoint">ExecutionPoint</th>
						<th id="thaExecutionPoint">Priority</th>
					</tr>
				</thead>
			</table>

		</div>
	</div>
	<div class="row">
			<div class="col-md-12">
				<div class="btn-group btn-group-sm floatleft">
				    <button type="submit" class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span>  Domain Home</button>
				</div>
			</div>
		</div>
		
</div>
<!-- 
	<div class="modal" id="srcParameters">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal">
						<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
					</button>
					<h4 class="modal-title">
						<span id="srcParametersTitle">Parameters</span>
					</h4>
				</div>-->
				
	<div class="modal" tabindex="-1" role="dialog" id="srcParameters">
  		<div class="modal-dialog" role="document" style="max-width:1000px">
   		 <div class="modal-content">
      		<div class="modal-header">
        		<h5 class="modal-title"><span id="srcParametersTitle">Parameters</span></h5>
        		<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
      		</div>
				<div class="modal-body">
					<span id="srcParametersMsg"> </span>
					<table class="table-striped table-bordered"	id="tblSrcParameters">
						<thead>
							<tr>
								<th id="thdName">Name</th>
								<th id="thdDescription">Description</th>
								<th id="thdRequired">Required</th>
								<th id="thdExample">Example</th>
							</tr>
						</thead>
						<tbody></tbody>
					</table>
				</div>
			</div>
			<!-- /.modal-content -->
		</div>
		<!-- /.modal-dialog -->
	</div>
	<!-- /.modal -->

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/handlers.js?build=${build}"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>
</body>
</html>