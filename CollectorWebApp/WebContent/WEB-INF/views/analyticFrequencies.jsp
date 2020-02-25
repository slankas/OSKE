<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.analyze.frequencies"); %>

<title>Collector: Frequencies</title>
<LINK rel="SHORTCUT ICON" href="${applicationRoot}resources/images/LAS_Logo.ico">
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/nouislider.9.2.0/nouislider.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/analyticVisualize.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>
</head>
<body>
<%@include file="header.jsp"%>
<%@include file="includes/analyzeHeader.jsp"%>
	<div class="text-nowrap container-fluid analyticDisplay">
		<div class="row">
			<div class="form-group col-md-8">
				<div class="form-inline">
					<div class="form-group align-middle">
						<label for="yAxisField" class="align-middle" style="margin-top: 0px;">Show Frequences for Item:</label> 
						<select id='yAxisField' class="form-control input-sm select-dropdown" style="width: 180px"></select> 
						<input type="text" class="form-control" id="yAxisSize" placeholder="# values" title="optional-set y axis size" size=5>
					</div>
					<div class="form-group">
						<label for="displayFormat" class="align-middle" style="margin-top: 0px;">Display format:</label>
						<input type="radio" class="align-bottom form-control" name="displayFormat" value="barchart" checked="checked">Bar Chart
						<input type="radio" class="align-bottom form-control" name="displayFormat" value="histogram" style="margin-left: .5rem;">Date Histogram
 					</div>
					<button style="margin-left: 1rem;" class="btn btn-primary btn-sm" id='btView'>Chart</button>
				</div>
			</div>
			<div class="form-group col-md-4">
				<div class='btn-toolbar pull-right'>
					<button style="margin-left: 1rem;" class="btn btn-primary btn-sm" id='btClearAllCharts'>Clear All Charts</button>
				</div>
			</div>
		</div>
		<div style="width: 100%;">
			<div id="plotContainer" style="width: 95%; min-height: 600px; margin: 0 auto; white-space: normal;"></div>
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
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analyticFrequencies.js?build=${build}"></script>
		
</body>
</html>