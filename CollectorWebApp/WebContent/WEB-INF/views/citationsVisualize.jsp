<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.citationsVisualize"); %>

<title>Collector: Visualize Citations</title>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/demonstrator.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/citationsVisualize.css" />

<!-- Leaflet resources for the maps -->
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet_1.3.4/leaflet.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/leaflet_1.3.4/leaflet.js"></script>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet.markercluster-1.3.0/MarkerCluster.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/leaflet.markercluster-1.3.0/MarkerCluster.Default.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/leaflet.markercluster-1.3.0/leaflet.markercluster-src.js"></script>

</head>
<body>
<%@include file="header.jsp"%>
<div class="container-fluid">
<div class="row">
	<div class="form-group col-md-12">
		<div class='btn-toolbar pull-right'>
			<div>Cluster Results: <input type=checkbox id='clusterResults' checked>
			     <a><button class="btn btn-primary" id="btViewAsSearch">View as Search</button></a>
			     <a><button id='btFilters' class="btn btn-primary">Filters</button></a>
			</div>
		</div>
	</div>
</div>
<div class="row" style="height:75%"><div class="col-md-12"  style="height:auto">
<div id="map"></div>
</div></div>
<div class="row"><div class="col-md-12">
<div id="timeSlider"></div>
</div></div>
<div class="row"><div class="col-md-12">
	<div class="form-group col-md-12">
		<div>Filters: <span id="filterText">none</span></div>
	</div>
</div></div>
</div>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="https://gitcdn.github.io/bootstrap-toggle/2.2.2/js/bootstrap-toggle.min.js"></script>
  
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootstrap-paginator.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	
<script src="${applicationRoot}resources/nouislider.9.2.0/nouislider.js"></script>
<script src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>
<script src="${applicationRoot}resources/js/application/component/DateSlider.js"></script>

<script src="${applicationRoot}resources/js/application/citationsVisualize.js"></script>
<form target="_blank" id='searchForm' name='searchForm' method='POST' action="${applicationRoot}${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>
</body>
</html>