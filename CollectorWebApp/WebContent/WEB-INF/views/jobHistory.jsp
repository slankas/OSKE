<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.jobHistory"); %>

<title>Job History</title>

<!-- For datepicker -->
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/jquery.datetimepicker.css" />
<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

</head>
<body>
<%@include file="header.jsp"%>
<input type="hidden" name="jobId" id="jobId" value="<%=request.getAttribute("jobId")%>" />
<div class="container-fluid">
	<div class="row">
		<div class="col-md-12">
			<div class="btn-group btn-group-sm" style="display: block;">
				<table id="tblJobHistory">
					<thead>
						<tr>
							<th id="thdJobHistory" colspan="5">Job History</th>
						</tr>
						<tr>
							<td>Start Date/Time</td>
							<td><input type="text" id="startTime" name="startTime" /></td>

							<td>Job Name</td>
							<td><select id="drpdn_jobName" name="drpdn_jobName">
									<option value="" selected="selected">All</option>
							</select></td>
						</tr>
						<tr>
							<td>End Date/Time</td>
							<td><input type="text" id="endTime" name="endTime" /></td>


							<td><input type="submit" class="btn btn-primary" id="btFilterJobHistory" value="Filter" /></td>
							<td><img id="imgLoading" src='${applicationRoot}resources/images/ajax-loader.gif'></td>
						</tr>
					</thead>
				</table>
				<br> <label id="err_label_date" style="color: red">
					Enter date in 'yyyymmdd' format.</label>
				<P>
					<label id="label_srch_message"></label>
				<P>
				<table class="display table table-striped table-bordered table-tight" id="tblJobHistoryData">
					<thead>
						<tr>
							<th id=thdJobName>Job Name</th>
							<th id=thdStatus>Status</th>
							<th id=thdStartTime>Start Time</th>
							<th id=thdEndTime>End Time</th>
							<th id=thdProcessingTime>Processing<br>Time</th>
							<th id=thdJobCollector>Job Collector</th>
							<th id=thdComments>Comments</th>
							<th id=thdNumPagesVisited># Pages Visited</th>
							<th id=thdTotalSizeRetrieved>Total Size Retrieved</th>
						</tr>
					</thead>
				</table>
			</div>
		</div>
	</div>
	<div class="row">
			<div class="col-md-12">
				<div class="floatleft">
					<button type="submit" class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span>  Domain Home</button>
				</div>
			</div>
		
	</div>
</div>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/jobHistory.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
	
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