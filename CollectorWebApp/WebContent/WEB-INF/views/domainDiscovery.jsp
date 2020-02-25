<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.discover"); %>
<title>Domain Discovery</title>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/domainDiscovery.css?build=${build}" />
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
</head>

<body>
<%@include file="header.jsp"%><br>
<div id="main">

<div class="container-fluid">
  <div class="row">
    <div class="col">
      <h3>Domain Discovery</h3>
    </div>
    <div class="col">
       <div class="float-right">
        <input type="checkbox" class="form-check-input" id="cbShowAllDiscoverySessions">
        <label class="form-check-label" for="cbShowAllDiscoverySessions" style="margin-left: 16px;">Show all users</label>
       </div>
    </div>
  </div>
</div>
<div class="text-nowrap container-fluid">
<!-- <style>
.form-control{
    display: inline;
}
</style> -->
<table id="sessions" class="table-bordered table-striped "  >
 <thead>
 	<tr>
 	<th>Session</th>
 	<th>User</th>
 	<th>Created</th>
 	<th>Latest Activity</th>
 	<th>Action</th>
 	</tr>
 </thead>
</table>
</div>
<br>
    <button type="button" class="btn btn-primary" id="btNewSession">Start New Session</button>
	<button type="button" class="btn btn-primary" id="btHome"><span class="fas fa-home"></span> Domain Home</button>
</div>


</body>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/domainDiscovery.js?build=${build}"></script>
 

</html>