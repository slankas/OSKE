<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.analyze.visualize"); %>


<title>Collector: Analytic Geospatial</title>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/demonstrator.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/analyticVisualize.css" />

<!-- Leaflet resources for the maps -->
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet_1.3.4/leaflet.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/leaflet_1.3.4/leaflet.js"></script>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet.markercluster-1.3.0/MarkerCluster.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet.markercluster-1.3.0/MarkerCluster.Default.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/leaflet.markercluster-1.3.0/leaflet.markercluster-src.js"></script>

<!-- DataTable imports -->
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>

</head>
<body>
<%@include file="header.jsp"%>
<%@include file="includes/analyzeHeader.jsp"%>
<div class="container-fluid analyticDisplay">
<div class="overlay">
   <div id="loading-img"></div>
</div>
<!--  Filters is now produced in LASHeader.js,  need to find a home for cluster results in the UI 
<div class="row">
	<div class="form-group col-md-12">
		<div class='btn-toolbar pull-right'>
			<div>Cluster Results: <input type=checkbox id='clusterResults' checked>
			     <a><button class="btn btn-primary" id="btViewAsSearch">View as Search</button></a>
			     <a><button id='btnFilters' class="btn btn-primary">Filters</button></a>
			</div>
		</div>
	</div>
</div>
 -->
<div class="row" style="height:68%"><div class="col-md-12"  style="height:auto">
<div id="map"></div>
</div></div>
<div class="row"><div class="col-md-12">
<div id="timeSlider"></div>
</div></div>

</div>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
  
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootstrap-paginator.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentBucket.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentBucketSupport.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentIndexView.js?build=${build}"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>

<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>
	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/nouislider.9.2.0/nouislider.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/component/DateSlider.js"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/Analytics.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analyticVisualize.js"></script>

</body>
</html>