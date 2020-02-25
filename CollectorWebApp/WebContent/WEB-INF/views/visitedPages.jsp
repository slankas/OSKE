<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.visitedPages"); %>
<title>Visited Pages</title>

<!-- For datepicker -->
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/jquery.datetimepicker.css" />

<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">


</head>
<body>
<input type="hidden" name="jobHistoryID" id="jobHistoryID"	value="<%=request.getAttribute("jobHistoryID")%>" />
<input type="hidden" name="id" id="id"	value="<%=request.getAttribute("id")%>" />
<%@include file="header.jsp"%>
<p>&nbsp;<p>
<div class="container-fluid">
	<div class="row">
		<div class="form-group col-md-12">
			<div class="btn-group btn-group-sm" style="display: block;">
				<table id="tblJobHistory">
					<thead>
						<tr>
							<th id="thdVisitedPages" colspan="5">Visited Pages</th>
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
							<td><input type="submit" class="btn btn-primary"id="btFilterJobHistory" value="Filter" /></td>
							<td><img id="imgLoading" src='${applicationRoot}resources/images/ajax-loader.gif'></td>
						</tr>
					</thead>
				</table>
				<br> <label id="err_label_user" style="color: red">
					Enter date in 'yyyymmdd' format.</label>
				<P>
					<label id="label_srch"></label>
				<P>
				<table	class="table-striped table-bordered" id="tblVisitedPages">
					<thead>
						<tr>
							<th id=thdURL>URL</th>
							<th id=thdAction>Action</th>
							<th id=thdContentType>Content-Type</th>
							<th id=thdTimestamp>Timestamp</th>
							<th id=thdStatus>Status</th>
						</tr>
					</thead>
				</table>
			</div>
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
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/visitedPages.js?build=${build}"></script>
	<!-- For datepicker -->
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