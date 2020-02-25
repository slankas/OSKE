<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.analyze"); %>

<title>OpenKE: Analytics</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/analyticsHome.css?build=${build}" />
<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>
</head>
<body>
<%@include file="header.jsp"%>
<%@include file="includes/analyzeHeader.jsp"%>
<div class="container-fluid">
  <div class="row">
    <div class="form-group col-md-12">

	  <div class="analyzeContainer">
		<div class="analyzeTables">
					<table style="width: 100%;"	class="display table table-striped table-bordered table-tight" id="topDomainsTable">
						<thead>
							<tr>
								<th id=thdDomain>Top Domains</th>
								<th id=thdPages># Pages</th>
							</tr>
						</thead>
					</table>
					<div>Select Entity Type: <select id="entityOptions"><option value=''>Select Type...</option></select>
					<table style="width: 100%;"	class="display table table-striped table-bordered table-tight" id="topEntitiesTable">
						<thead>
							<tr>
								<th id=thdEntityName>Top Entities:</th>
								<th id=thdEntityInstances># Instances</th>
							</tr>
						</thead>
					</table>
				    </div>
					<table style="width: 100%;"	class="display table table-striped table-bordered table-tight" id="topLocationsTable">
						<thead>
							<tr>
								<th id=thdLocation>Top Locations</th>
								<th id=thdLocationInstances># Instances</th>
							</tr>
						</thead>
					</table>	
		</div>
		<div class="analyzeCharts">
			<div id="retrievedPagesChart" style="width: 600px; height: 300px"></div>
			<div id="textLengthChart" style="width: 600px; height: 300px"></div>
		</div>
		<div class="analyzeDates">
		  <div class="card card-default">
		  <!-- <div class="card-header">Dates </div> -->
		  <div class="card-body" id="dateCardBody">
		    <table>
		    	<tr><td>Earliest Crawl Date:</td><td><span id='earliestCrawlDate'></span></td><td>&nbsp;&nbsp;&nbsp;</td>
		    	    <td>Earliest Published Date:</td><td><span id='earliestPublishedDate'></span></td>
		        </tr>
		    	<tr><td>Latest Crawl Date:</td><td><span id='latestCrawlDate'></span></td><td>&nbsp;&nbsp;&nbsp;</td>
		    	    <td>Latest Published Date:</td><td><span id='latestPublishedDate'></span></td>
		        </tr>		        
		    </table>
		  </div>
		  </div>		
		</div>
	  </div>
	   <div id="timeSlider"></div>
	</div>
  </div>
</div>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/elasticSupport.js?build=${build}"></script>	

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/chosen/chosen.jquery.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>

	
<script type="text/javascript" charset="utf8"  src="${applicationRoot}resources/nouislider.9.2.0/nouislider.js"></script>
<script type="text/javascript" charset="utf8"  src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>
<script type="text/javascript" charset="utf8"  src="${applicationRoot}resources/js/application/component/DateSlider.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/Analytics.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/domainHomeAnalytics.js?build=${build}"></script>

</body>
</html>
