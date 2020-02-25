<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.documentIndexes"); %>


<title>Document Indexes</title>

<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

</head>
<body>
<%@include file="header.jsp"%>
<br>
<div class="text-nowrap container-fluid">
	<div class="row">
		<div class="form-group col-md-12">
			<h3>Document Indexes</h3>
		         <table class="display table table-striped table-bordered table-tight" id="tblDocumentIndexes">
					<thead>
                        <tr>
                            <th id='thdAction' data-orderable="false"></th>
                            <th id='thdTitle'>Name</th>
                            <th id='thdDateCreated'>Date Created</th>
                            <th id='thdOwner'>Owner</th>
                            <th id='thdDocumentCount'># of Documents</th>
                        </tr>
					</thead>
				</table>
			</div>
		</div>

		<div class="row">
			<div class="col-md-12">
				<div class="btn-group floatleft">
					<button type="button" class="btn btn-primary  btn-md" id="btHome"><span class="fas fa-home"></span> Domain Home</button>			
				</div>
			</div>
		</div>	
</div>		
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/documentIndex.js?build=${build}"></script>
	
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/group/DocumentIndex.js?build=${build}"></script>	
	
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