<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.conceptSelector"); %>


<title>Collector</title>
	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>

</head>
<body>
<input type="hidden" name="domain" id="domain"	value="<%=request.getAttribute("domain")%>" />
<input type="hidden" name="applicationContextRoot" id="applicationContextRoot"	value="<%=pageContext.getAttribute("applicationRoot").toString()%>" />	

<div class="card card-default">
  <div class="card-header">
    Concepts
    <div style="float:right;vertical-align: top; position:relative; top: -4px;">
      <button class="btn btn-primary btn-sm" id="btResetConcepts">Reset</button>
      <button class="btn btn-primary btn-sm" id="btClose">Close</button>
    </div>
  </div>
  <div class="card-body">
    <div id="conceptPie"></div>
    <div>Click on wedge to drill down, Shift-click to add</div>
  </div>
</div>
				
		
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/conceptChart.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/conceptSelector.js?build=${build}"></script>

</body>
</html>
