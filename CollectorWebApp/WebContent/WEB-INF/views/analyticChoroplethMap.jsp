<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.analyze.choroplethMap"); %>

<title>Collector: Analytic Choropleth Map</title>
<LINK rel="SHORTCUT ICON" href="${applicationRoot}resources/images/LAS_Logo.ico">
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/nouislider.9.2.0/nouislider.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/analyticVisualize.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>
</head>
<body>
	<%@include file="header.jsp"%>
	<%@include file="includes/analyzeHeader.jsp"%>
	<div class="container-fluid analyticDisplay">
		<div class="overlay">
			<div id="loading-img"></div>
		</div>
		<div class="row" style="height: 68%">
			<div style="width: 100%;">
				<div id="choro" style="width: 95%; height: 600px; margin: 0 auto;"></div>
			</div>
		</div>
		<div class="row">
			<div class="col-md-12">
				<div id="timeSlider"></div>
			</div>
		</div>

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
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/elasticSupport.js?build=${build}"></script>	
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/chosen/chosen.jquery.js"></script>
		
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/nouislider.9.2.0/nouislider.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/component/DateSlider.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/Analytics.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analyticChoroplethMap.js?build=${build}"></script>
		
</body>
</html>