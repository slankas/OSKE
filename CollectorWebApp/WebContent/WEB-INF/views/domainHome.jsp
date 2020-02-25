<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<%pageContext.setAttribute("currentPage","domain.dashboard");  %>

<title>OpenKE: Analyst Dashboard</title>

<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/newstape.css?build=${build}" />
<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/css/external/jquery.datetimepicker.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/domainHome.css?build=${build}" />

<!-- DataTable imports -->
	<!-- <link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/> -->
	<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
	<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>

</head>
<body>
	<%@include file="header.jsp"%><br>
	<div class="lnkadjudicator">
		<a id="lnkadjudicator" href="${applicationRoot}<%=domain%>/adjudication">text to replace</a>
	</div><br>

<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>
	<div class="analystContainer">
		<div class="analystProject">
			<div class="card card-default h-100">
				  <div class="card-header">
				  	<h5 style="display: inline-block" class="card-title">Projects</h5>
				  </div>
				  <div class="card-body" id="projectList">
				  </div>
			</div>
		</div>
		<div class="analystDomainDiscovery">
			<div class="card card-default">
				  <div class="card-header" style='padding: .45rem 1.25rem .1rem 1.25rem;'>
				    <h5 style="display: inline-block" class="card-title">Discovery Sessions</h5>
				    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><input id='discSearchTerms' type='text' size=40 placeholder='Enter search terms'> <select id='discSearchSource'>
				       <option value='federatedAcademicSearch'>Academic
				       <option value='federatedSearch' selected>Federated
                       <option value='holdings'>Holdings</select> <button class="btn btn-primary btn-sm" id="btDomainDiscoveryNew">New Session</button></div>
				  </div>
				  <div class="card-body">

				  
					<div id="domainDiscoverycard" class="text-nowrap">
						<table id="sessions" class="table-bordered table-striped"  >
							<thead><tr><th>Session</th><th>User</th><th>Created</th><th>Latest Activity</th></tr></thead>
						</table>
						<div class=""> <!-- class:form-check -->
						    <input type="checkbox" class="form-check-input" id="cbShowAllDiscoverySessions">
						    <label class="form-check-label" for="cbShowAllDiscoverySessions" style="margin-left: 16px;">Show all users</label>
                        </div>
					</div>
				  </div>
			</div>
		</div>
		<div class="analystSearchAlerts">
			<div class="card card-default">
				<div class="card-header">
				    <h5 style="display: inline-block" class="card-title">Search Alerts</h5>
				    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btAcknowledgeAll">Acknowledge All</button></div>
				  </div>
				  <div class="card-body" id="searchAlertBody">
					<div id="searchAlertList">
					</div>
				  </div>
			</div>
		</div>
		<div class="analystRSS">
			<% if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.NEWS_FEED_UTILIZE)) {  %>	
	
			<div class="card card-default">
			  <div class="card-header">
			  	<h5 style="display: inline-block" class="card-title">
			    <%= Configuration.getConfigurationProperty(domain, ConfigurationType.NEWS_FEED_TITLE) %>
			    </h5>
			    <div style="float:right;vertical-align: middle"><span id='newsSettingButton' class='fas fa-cog' title="News Settings"></span><span id='pauseButton' class='fas fa-pause' title="Pause News Scrolling"></span><span id='playButton' class='fas fa-play' title="Scroll News"></span></div> 
			  </div>
			  <div class="card-body">
				<div class="newstape" style="height:650px;">
				  <div class="newstape-content" >
				  </div>
				</div>
			  </div>
			</div>
            <% } %>
		</div>		
		<div class="analystConcepts">
	
			<div class="card card-default">
			  <div class="card-header">
			    <h5 style="display: inline-block" class="card-title">Concepts</h5>
			    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btTopConcepts">Top</button>&nbsp;<button class="btn btn-primary btn-sm" id="btResetConcepts">Reset</button></div>
			  </div>
			  <div class="card-body">
			    <div id="conceptPie"></div>
			    <div id="conceptDateRangePane">Crawl date from <input type="text" id="conceptStartTime" name="startTime"/> to <input type="text" id="conceptEndTime" name="endTime" /> (Zulu/GMT time)</div>
			    <div style="float:right;"><button type="button" class="btn btn-primary btn-sm" id="btConceptSettings" title="Settings"><span class="fas fa-cog"></span></button>
			                              <button type="button" class="btn btn-primary btn-sm" id="btConceptDownload" title="Copy to current scratchpad"><span class="fas fa-share-square"></span></button>
			                              <button type="button" class="btn btn-primary btn-sm" id="btConceptHelp" title="Help"><span class="fas fa-question-circle"></span></button></div>
			    <!-- <div>Click on wedge to drill down, Shift-click on wedge to search</div> -->
			    <!-- <div><table style="width:100%;"><tr><td>Crawl date from</td><td><input type="text" id="startTime" name="startTime" /></td><td>to</td><td> <input type="text" id="endTime" name="endTime" /></td> <td>(use Zulu / GMT time)</td></tr></table></div>
			     -->
			  </div>
			</div>
			
		</div>	
		
		<% if (Configuration.getConfigurationPropertyAsBoolean(currDomain, ConfigurationType.RESOURCE_UTILIZE) ) { %>
		<div class="analystTradecraft">
			<div class="card card-default">
			  <div class="card-header"><%=Configuration.getConfigurationProperty(currDomain, ConfigurationType.RESOURCE_TITLE)%></div>
			  <div class="card-body">
			    <% JSONArray links = Configuration.getConfigurationPropertyAsArray(currDomain, ConfigurationType.RESOURCE_OBJECTS);
			       for (int i=0;i<links.length();i++) {
			    	   JSONObject jo = links.getJSONObject(i);
			    %>
			       <a href="<%=jo.getString("link")%>"><%=jo.getString("displayText") %></a><br>
			           
			    <%
			       }
			    %>
			    <br>&nbsp;
			  </div>
			</div>
		</div>
		 <%} %>
	</div>
<% } %>
	
Build: <i><%= edu.ncsu.las.servlet.SystemInitServlet.getWebApplicationBuildTimestamp(getServletContext()) %></i><br>
Time: <i><%= java.time.Instant.now().toString().replaceAll("-|:","").substring(0,15) %>Z</i>

	
		
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/ProjectDocument.js?build=${build}"></script>	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/conceptChart.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/domainHome.js?build=${build}"></script>

	<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>

	<!--  for scrollable news ticker -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/jquery.newstape/jquery.newstape.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>




<form id='searchForm' name='searchForm' method='POST' action="${applicationRoot}/${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>
</body>
</html>
