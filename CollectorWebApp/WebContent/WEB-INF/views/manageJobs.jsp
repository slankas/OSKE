<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.jobs"); %>

<title>OpenKE: Manage Jobs</title>

<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<!-- <link rel="stylesheet" type="text/css" href="DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>  -->
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

</head>
<body>
<%@include file="header.jsp"%>
<div class="container-fluid">
	<div class="row">
		<div class="col-md-12">
			<div>
				<div class='btn-toolbar pull-right'>
					<div class='btn-group'>
						<a>
							<button type="button" class="btn btn-primary" id="btnAddJob">Add Job</button>
							
						</a>
					</div>
				</div>
				<h5>Jobs</h5>
				<br> <label id="err_label_user" style="color: red"></label>
			</div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-12">
			<table class="table table-striped table-bordered" id="tblJobs">
				<thead>
					<tr>
						<th class="thHeadings" id="thdName">Name</th>
						<th class="thHeadings" id="thdURL">URL /<br>Search Terms</th>
						<th class="thHeadings" id="thdSourceHandler">Source Handler</th>
						<th class="thHeadings" id="thdSchedule">Schedule</th>
						<th class="thHeadings" id="thdConfiguration">Configuration</th>
						<th class="thHeadings" id="thdStatus">Status</th>
						<th class="thHeadings" id="thdStatusDate">Status Date</th>
						<th class="thHeadings" id="thdNextRunDate">Next Run</th>
						<th class="thHeadings" id="thdPriority">Priority</th>
						<th class="thHeadings" id="thdOwnerEmail">Owner Email</th>
					</tr>
				</thead>
			</table>
			<br>

		</div>
	</div>
	<div class="row">
		<div class="col-md-12">
			<div class="floatleft">
				<button type="submit" class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span> Domain Home</button>&nbsp;&nbsp;			
				<button class="btn btn-outline-primary" id="btnScheduleErrored">Schedule All Errored Jobs</button>
				<button class="btn btn-outline-primary" id="exportButton" title="Export all job configs for transfer">Export Job Configurations</button>
			</div>
		</div>
	</div>

</div>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jszip.min_3.1.5.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/manageJobs.js?build=${build}"></script>
	
	<!--  Used to make cron job entries appear human readable: prettyCron.toString("37 10 * * * *");  -->
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/cronstrue.js"></script>
    
	<!-- DataTable imports -->
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