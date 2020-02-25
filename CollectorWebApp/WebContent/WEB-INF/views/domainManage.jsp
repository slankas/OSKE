<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","system.manageDomains"); %>

<title>OpenKE: Manage Domains</title>

<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

</head>
<body>
<%@include file="header.jsp"%>
<div class="container-fluid">
	<div class="row">
		<div class="col-md-12">
			<div>
				<div class='pull-right'>
							<button class="btn btn-primary" id="btnAddDomain">Add Domain</button>&nbsp;
							<button class="btn btn-primary" id="btnSystemConfiguration">System Configuration</button> 
				</div>
				<h4>Available Domains</h4>
				<br> <label id="err_label_user" style="color: red"></label>
			</div>
		</div>
		<div class="col-md-12">
			<table class="display table table-striped table-bordered table-tight" id="tblDomain">
				<thead>
					<tr>
						<th class="thHeadings" id="thdName">Name</th>
						<th class="thHeadings" id="thdFullName">Full Name</th>
						<th class="thHeadings" id="thdDescription">Description</th>
						<th class="thHeadings" id="thdConfiguration">Configuration</th>
						<th class="thHeadings" id="thdOffline">Offline</th>
						<th class="thHeadings" id="thdStatusDate">Status Date</th>
						<th class="thHeadings" id="thdPrimaryContact">Primary Contact</th>
					</tr>
				</thead>
			</table>
		</div>
	</div>
</div>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/manageDomains.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/elasticSearch/jsontree.min.js"></script>
    
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