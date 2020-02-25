<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.literatureDiscovery"); %>

<title>OpenKE: Literature Discovery Home</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/newstape.css?build=${build}" />

<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/plotly-latest_v1_38_3.min.js"></script>
</head>
<body>
<%@include file="header.jsp"%><br>
	<div class="lnkadjudicator">
		<a id="lnkadjudicator" href="${applicationRoot}<%=domain%>/adjudication">text to replace</a>
	</div><br>
	<div class="row">
		<div class="form-group col-md-12">
			<table style="width: 15%; display: inline-block; vertical-align: top">
				<tbody>
                    <tr>
                        <td>
                            <nav class="navbar navbar-default sidebar" role="navigation">
                              <div class="sidebar-sticky" >
                                        <ul class="nav flex-column">
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('domainDiscovery')">Domain Discovery<span style="font-size: 16px;"></span></a></li>      <% } %>                                            
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('manageJobs')">Jobs<span style="font-size: 16px;"></span></a></li>                 <% } %>
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('jobHistory')">Job History<span style="font-size: 16px;"></span></a></li>                 <% } %>
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('visitedPages')">Visited Pages<span style="font-size: 16px;"></span></a></li>            <% } %>
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('handlers')">Source and Document Handlers<span style="font-size: 16px;"></span></a></li>  <% } %>
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('structuralExtraction')">Structural Extraction<span style="font-size: 16px;"></span></a></li>   <% } %>
                                            <% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('fileUpload')">Upload Files<span style="font-size: 16px;"></span></a></li>                <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('search')">Search<span  style="font-size: 16px;"></span></a></li>                         <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('documentIndex')">Document Indexes<span  style="font-size: 16px;"></span></a></li>        <% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('manageDocumentBuckets')">Document Buckets<span style="font-size: 16px;"></span></a></li>          <% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('concept')">Concepts<span style="font-size: 16px;"></span></a></li>                       <% } %>
											<% if (!domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('manageSearchAlerts')">Search Alerts<span style="font-size: 16px;"></span></a></li>       <% } %>
                                            <% if (domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('citationsVisualize')">Geospatial<span style="font-size: 16px;"></span></a></li>       <% } %>
                                            <% if (domain.equals("whpubmed") && u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('citationsHeatMap')">HeatMap<span style="font-size: 16px;"></span></a></li>       <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><li class="nav-item"><a href="javascript:navigateTo('feedback')">Feedback<span style="font-size: 16px;"></span></a></li>                       <% } %> 
                                            <% if (u.hasAccess(domain,RoleType.ADMINISTRATOR)) { %><li class="nav-item"><a href="javascript:navigateTo('maintainUsers')">Maintain Users<span style="font-size: 16px;"></span></a></li>     <% } %>
										</ul>
                              </div>
                            </nav>
                        </td>
                    </tr>
					<% if (!domain.equals("whpubmed")) { %>
                    <tr>
                        <td>
                            <nav class="navbar navbar-default sidebar" role="navigation">
                                <div class="container-fluid">
                                    <div class="collapse navbar-collapse"
                                        id="bs-sidebar-navbar-collapse-1">
                                        <ul id="navApplicationHyperlinks" class="nav navbar-nav">
                                        </ul>
                                    </div>
                                </div>
                            </nav>
                        </td>
                    </tr>
					
					<tr>
						<td>
							<table style="width: 100%;" class="table-striped table-tight"
								id="tblSettings">
								<thead>
									<tr>
										<th colspan=2 id=thdActions>General Statistics</th>
									</tr>
								</thead>
								<tbody>
									<tr>
									    <td>Jobs executed: </td>
										<td id="numberOfJobs"></td>
									</tr>
									<tr>
										<td>Running jobs: </td>
										<td id="numberOfRunningJobs"></td>
									</tr>
									<tr>
										<td>Pages visited: </td>
										<td id="numberOfPagesVisited"></td>
									</tr>
									<tr>
										<td>Pages stored: </td>
										<td id="numberOfPagesStored"></td>
									</tr>
									<tr>
										<td>Total size: </td>
										<td id="sizeOfPagesStored"></td>
									</tr>
									<tr><td colspan=2>&nbsp;</td></tr>
									<tr><td colspan=2>Build: <i><%= edu.ncsu.las.servlet.SystemInitServlet.getWebApplicationBuildTimestamp(getServletContext()) %></i></td></tr>
								</tbody>
							</table>

						</td>
					</tr>
					<% } else { %>
					<tr>
						<td>
							<table style="width: 100%;" class="table-striped table-tight"
								id="tblSettings">
								<thead>
									<tr>
										<th colspan=2 id=thdActions>Discovery Statistics</th>
									</tr>
								</thead>
								<tbody>
									<tr>
									    <td>Total # citations:</td>
										<td>26,759,399</td>
									</tr>
									<tr>
										<td>Identified citations:</td>
										<td>6,609</td>
									</tr>
									<tr>
										<td>Full-text records:</td>
										<td>6,286</td>
									</tr>
									<tr>
										<td>Full-text size:</td>
										<td>1,587,032,702</td>
									</tr>
									<tr>
										<td>PDF Size: </td>
										<td>14,087,963,618</td>
									</tr>
									<tr>
										<td valign=top>Selection Criteria: </td>
										<td style='font-family: "Courier New", Courier, monospace'>CRISPR<br>Cas9</td>
									</tr>
									<tr><td colspan=2>&nbsp;</td></tr>
									<tr><td colspan=2>Build: <i><%= edu.ncsu.las.servlet.SystemInitServlet.getWebApplicationBuildTimestamp(getServletContext()) %></i></td></tr>
								</tbody>
							</table>

						</td>
					</tr>
					<% } %>
				</tbody>
			</table>

			<div style="width: 80%; display: inline-block; vertical-align: top">
				<div class="row">
					<div class="form-group col-md-6">
						<table style="width: 100%; vertical-align: top"	class="table-striped table-tight">
							<tbody>	<tr> <td>
								<table style="width: 100%;"	class="display table table-striped table-bordered table-tight" id="topAuthorsTable">
									<thead>
										<tr>
											<th id=thdAuthorFullName>Top Authors</th>
											<th id=thdAuthorNumPaper># Publications</th>
										</tr>
									</thead>
								</table>
							</td></tr></tbody>
						</table>
						<table style="width: 100%; vertical-align: top"	class="table-striped table-tight">
							<tbody>	<tr> <td>
								<table style="width: 100%;"	class="display table table-striped table-bordered table-tight" id="topUniversitiesTable">
									<thead>
										<tr>
											<th id=thdUniversityName>Top Universities</th>
											<th id=thdUniversityMentions># Mentions</th>
										</tr>
									</thead>
								</table>
							</td></tr></tbody>
						</table>		
						<table style="width: 100%; vertical-align: top"	class="table-striped table-tight">
							<tbody>	<tr> <td>
								<table style="width: 100%;"	class="display table table-striped table-bordered table-tight" id="topVendorsTable">
									<thead>
										<tr>
											<th id=thdVendor>Top Vendors</th>
											<th id=thdVendorMentions># Mentions</th>
										</tr>
									</thead>
								</table>
							</td></tr></tbody>
						</table>											
					</div>
					<div class="form-group col-md-6">
						<div id='paperHistogram' style="width:95%;height:300px; margin: 0 auto;"></div>
						<div id='paperPDFPageCount' style="width:95%;height:300px; margin: 0 auto;"></div>
					</div>
				</div>
				<div class="row">
					<div class="form-group col-md-12">
						<div class="card card-default">
							<div class="card-header">
								Recent Science News
								<div style="float: right; vertical-align: middle">
									<span id='pauseButton' class='fas fa-pause'></span><span
										id='playButton' class='fas fa-play'></span>
								</div>
							</div>
							<div class="card-body">
								<div class="newstape">
									<div class="newstape-content"></div>
								</div>
							</div>
						</div>

					</div>
				</div>				
			</div>

		</div>
	</div>
	<div class="row">
	Time: <i><%= java.time.Instant.now().toString() %></i>
	</div>	
	 <% if (!domain.equals("whpubmed")) { %>
<% if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.NEWS_FEED_UTILIZE)) {  %>	
	<div class="row">
	  <div class="col-md-4">
		<div class="card card-default">
		  <div class="card-header">
		    News
		    <div style="float:right;vertical-align: middle"><span id='pauseButton' class='fas fa-pause'></span><span id='playButton' class='fas fa-play'></span></div> 
		  </div>
		  <div class="card-body">
			<div class="newstape">
			  <div class="newstape-content">
			  </div>
			</div>
		  </div>
		</div>

<% } %>

	  </div>
	  	  <div class="col-md-8">
		<div class="card card-default">
		  <div class="card-header">
		    Search Alerts
		    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btAcknowledgeAll">Acknowledge All</button></div>
		  </div>
		  <div class="card-body" id="searchAlertBody">
			<div id="searchAlertList">
			</div>
		  </div>
		</div>

	  </div>
	</div>	
	<% } %>
	
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/literatureDiscoveryHome.js?build=${build}"></script>

	<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>

	<!--  for scrollable news ticker -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/jquery.newstape/jquery.newstape.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>


<form id='searchForm' name='searchForm' method='POST' action="${applicationRoot}${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>
</body>
</html>