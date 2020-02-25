<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.citationsHeatMap"); %>

<title>Collector: Visualize Citations</title>
<LINK rel="SHORTCUT ICON" href="${applicationRoot}resources/images/LAS_Logo.ico">
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/nouislider.9.2.0/nouislider.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/citationsVisualize.css" />
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>
</head>
<body>
<%@include file="header.jsp"%>
<div class="text-nowrap container-fluid">
<div class="row">
	<div class="form-group col-md-8">
<div class="form-inline">
  <div class="form-group">
    <label for="exampleInputName2">X-axis</label>
    <select id='xAxisField'>
    	<option value="" selected></option>
    	<!-- <option value='action'>Actions</option>  -->
		<option value='author'>Authors</option>
		<option value='chemical'>Chemicals</option>
		<!--<option value='concept'>Concepts</option>-->
		<option value='country'>Countries</option>
		<option value='journal'>Journals</option>
		<option value='authorKeyword'>Keywords - Author</option>
		<option value='meshKeyword'>Keywords - MESH</option>
		<!--<option value='kit'>Kits</option>-->
		<!--<option value='thing'>Things</option>-->
		<!--<option value='university'>Universities</option>-->
		<!--<option value='vendor'>Vendors</option>-->
    </select>
    <input type="text" class="form-control" id="xAxisSize" placeholder="x-axis size" size=5>
  </div>
  <div class="form-group">
    <label for="exampleInputName2">Y-axis</label>
    <select id='yAxisField'>
        <option value="" selected></option>
    	<option value='action'>Actions</option>
		<option value='author'>Authors</option>
		<option value='chemical'>Chemicals</option>
		<option value='concept'>Concepts</option>
		<option value='country'>Countries</option>
		<option value='journal'>Journals</option>
		<option value='authorKeyword'>Keywords - Author</option>
		<option value='meshKeyword'>Keywords - MESH</option>
		<option value='kit'>Kits</option>
		<option value='thing'>Things</option>
		<option value='university'>Universities</option>
		<option value='vendor'>Vendors</option>
    </select>
    <input type="text" class="form-control" id="yAxisSize" placeholder="y-axis size" size=5>
  </div>
  <button class="btn btn-primary" id='btView'>View</button>
</div>
	</div>
	<div class="form-group col-md-4">
		<div class='btn-toolbar pull-right'>
			<div class='btn-group'>
				<a><button class="btn btn-primary" id="btnViewAsSearch">View as Search</button></a>&nbsp;				  
				<a><button id='btFilters' class="btn btn-primary">Filters</button></a>
			</div>
		</div>
	</div>
</div>
<div style="width:100%;">
<div id="tester" style="width:95%;height:600px; margin: 0 auto;"></div>
</div>
<div id="timeSlider"></div>
<div class="row">
	<div class="form-group col-md-12">
		<div>Filters: <span id="filterText">none</span></div>
	</div>
</div>
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
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/elasticSupport.js?build=${build}"></script>	
	
<script src="${applicationRoot}resources/nouislider.9.2.0/nouislider.js"></script>
<script src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>
<script src="${applicationRoot}resources/js/application/component/DateSlider.js"></script>

<script src="${applicationRoot}resources/js/application/citationsHeatMap.js"></script>
		
<form target="_blank" id='searchForm' name='searchForm' method='POST' action="${applicationRoot}${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>
</body>
</html>