<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.analyze.heatMapTimeline"); %>

<title>Collector: Analytic Heatmap Timeline</title>
<LINK rel="SHORTCUT ICON" href="${applicationRoot}resources/images/LAS_Logo.ico">
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/nouislider.9.2.0/nouislider.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/analyticVisualize.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>
</head>
<body>
<%@include file="header.jsp"%>
<%@include file="includes/analyzeHeader.jsp"%>
	<div class="text-nowrap container-fluid">
		<div class="row">
			<div class="form-group col-md-8">
				<div class="form-inline">
					<div class="form-group">
						<label for="xAxisField">Timeframe:</label> 
						<select id='xAxisField' class="form-control input-sm select-dropdown" style="width: 180px">
							<option value="1M" selected>by Month</option>
							<option value="1w">by Week</option>
						</select> 
					</div>
					<div class="form-group">
						<label for="yAxisField">Item:</label> 
						<select id='yAxisField' class="form-control input-sm select-dropdown" style="width: 180px"></select> 
						<input type="text" class="form-control" id="yAxisSize" placeholder="set size" title="optional-set y axis size" size=5>
					</div>
					<button class="btn btn-primary" id='btView'>View</button>
				</div>
			</div>
			<div class="form-group col-md-4">
				<div class='btn-toolbar pull-right'>
					<!--
			<div class='btn-group'>
				<a><button class="btn btn-primary" id="btnViewAsSearch">View as Search</button></a>&nbsp;				  
				<a><button id='btnFilters' class="btn btn-primary">Filters</button></a>
			</div>
			-->
				</div>
			</div>
		</div>
		<div style="width: 100%;">
			<div id="tester" style="width: 95%; height: 600px; margin: 0 auto;"></div>
		</div>
		<div id="timeSlider"></div>
	</div>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
  
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
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analyticHeatMapTimeline.js?build=${build}"></script>
		
</body>
</html>