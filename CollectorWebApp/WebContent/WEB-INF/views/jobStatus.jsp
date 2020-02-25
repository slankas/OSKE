<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.collectorView"); %>

<title>OpenKE: Job Status</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/jobStatus.css?build=${build}" />

<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
	
</head>
<body>
<%@include file="header.jsp"%><br>

<button class="btn btn-primary btn-sm" id="btUploadFiles">Upload Files</button>
<button class="btn btn-primary btn-sm" id="btViewHandlers">Handlers</button>
<button class="btn btn-primary btn-sm" id="btCollectorView">Collector View</button>

	<div class="statusContainer">
		<div class="statusRecentJobs">
			<table class="table-striped table-tight" id="recentJobsArea">
				<thead>
					<tr>
						<th id=thdRunningJobs><h5>Most Recent Jobs</h5><div class='pull-right'><button class="btn btn-primary btn-sm" id="btViewAllJobs">View All Jobs</button></div></th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>
							<table style="width: 100%;"	class="table-striped table-bordered"id="tblRecentJobs">
								<thead>
									<tr>
										<th id=thdJobName>Job Name</th>
										<th id=thdStatus>Status</th>
										<th id=thdStatusDate>Status Date</th>
										<th id=thdStatusDate>Processing<br>Time</th>
										<th id=thdStatusDate># Pages</th>
										<th id=thdActions>Actions</th>
									</tr>
								</thead>
							</table>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
		<div class="statusVisitedPages">
			<table class="table-striped table-tight" id="visitedPagesArea">
				<thead>
					<tr>
						<th id=thdVisitedPages><h5>Most Recent Visited Pages</h5><div class='pull-right'><button class="btn btn-primary btn-sm" id="btViewAllPages">View All Pages</button></div></th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>
							<table	style="width: 100%;" class="table-striped table-bordered" id="tblVisitedPages">
								<thead>
									<tr>
										<th id=thdID>URL</th>
										<th id=thdTopic>Status</th>
										<th id=thdAnalytics>Status Date</th>
										<th id=thdDateModified>Actions</th>
									</tr>
								</thead>
							</table>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
	
	<div>
	Time: <i><%= java.time.Instant.now().toString() %></i>
	</div>	
	

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/jobStatus.js?build=${build}"></script>

	<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>


<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>

</body>
</html>
