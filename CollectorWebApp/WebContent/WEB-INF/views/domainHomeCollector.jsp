<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<%pageContext.setAttribute("currentPage","noheader.collectorHome");  %>

<title>OpenKE: Collector</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/newstape.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/collectorHome.css?build=${build}" />

<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/css/external/jquery.datetimepicker.css" />

<!-- DataTable imports used ???-->
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
	
	<div class="analystContainer">
		<div class="analystMenu">
		                    
                            <div class="btn-group-vertical" role="navigation">

                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('domainDiscovery')">Domain Discovery</button>      <% } %>  
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('search')">Search</button>                         <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('documentIndex')">Document Indexes</button>        <% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('manageDocumentBuckets')">Document Buckets</button><% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('concept')">Concepts</button>                       <% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('manageSearchAlerts')">Search Alerts</button>       <% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('structuralExtraction')">Structural Extraction</button>   <% } %>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('fileUpload')">Upload Files</button>                <% } %>
											<button class="btn btn-light disabled " style="text-align: left;">&nbsp;</button>
											<% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('jobStatus')">Job Status</button>                 <% } %>                                          
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('manageJobs')">Jobs</button>                 <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('jobHistory')">Job History</button>                 <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('visitedPages')">Visited Pages</button>            <% } %>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('handlers')">Handlers</button>  <% } %>
                                            
                                            <button class="btn btn-light disabled " style="text-align: left;">&nbsp;</button>
                                            <% if (u.hasAccess(domain,RoleType.ANALYST)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('feedback')">Feedback</button>                       <% } %> 
                                            <% if (u.hasAccess(domain,RoleType.ADMINISTRATOR)) { %><button class="btn btn-light" style="text-align: left;" onclick="javascript:navigateTo('maintainUsers')">Maintain Users</button>     <% } %>
                                            
                                            <button class="btn btn-light disabled " style="text-align: left;">&nbsp;</button>
                                            <% if (showAnalystHome) { %> <button class="btn btn-light" style="text-align: left;" onclick="javascript:LASHeader.setDefaultHome('collector'); return false">Switch to Collector Home</button> <%} 
                                             else { %> <button class="btn btn-light" style="text-align: left;" onclick="javascript:LASHeader.setDefaultHome('analyst'); return false">Switch to Analyst Home</button> <%} %>



                            </div>
                            <div> &nbsp;<br>
                            <div class="btn-group-vertical" id="navApplicationHyperlinks">
                            <!-- 
                            <nav class="navbar navbar-default sidebar navbar-expand-lg navbar-light bg-light" role="navigation">
                                        <ul id="navApplicationHyperlinks" class="nav flex-column">
                                        </ul>
                            </nav>
                             -->
                             
                            </div>
                            </div>
                            <div>
                            <p>
                            <table style="width: 100%;" class="table-nopad"	id="tblSettings">
								<thead>
									<tr>
										<th colspan=2 id=thdActions>General Statistics</th>
									</tr>
								</thead>
								<tbody>
									<tr>
									    <td>Domain Established: </td><td id="domainEstablishedDate"></td>
									</tr>
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>
									<tr>
									    <td>Jobs executed: </td><td id="numberOfJobs"></td>
									</tr>
									<tr>
										<td>Running jobs: </td><td id="numberOfRunningJobs"></td>
									</tr>
									<tr>
										<td>Pages visited: </td><td id="numberOfPagesVisited"></td>
									</tr>
									<tr>
										<td>Pages stored: </td><td id="numberOfPagesStored"></td>
									</tr>
									<tr>
										<td>Total size: </td><td id="sizeOfPagesStored"></td>
									</tr>
<% } %>
									<tr><td colspan=2>&nbsp;</td></tr>
									<tr><td colspan=2>Build: <i><%= edu.ncsu.las.servlet.SystemInitServlet.getWebApplicationBuildTimestamp(getServletContext()) %></i></td></tr>
									<tr><td colspan=2>Time: <i><%= java.time.Instant.now().toString().replaceAll("-|:","").substring(0,15) %>Z</i></td></tr>
								</tbody>
							</table>
							</div>	
		</div>
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>
		<div class="analystDomainDiscovery">
			<div class="card card-default">
				  <div class="card-header">
				    Domain Discovery Sessions
				    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btDomainDiscoveryNew">Create Session</button></div>
				  </div>
				  <div class="card-body">

				  
					<div id="domainDiscoverycard" class="text-nowrap">
						<table id="sessions" class="table-bordered table-striped"  >
							<thead><tr><th>Session</th><th>User</th><th>Created</th></tr></thead>
						</table>
					</div>
				  </div>
			</div>
		</div>
		<div class="analystSearch">
		    <div class="card card-default">
			  <div class="card-header">
			    Search Retrieved Pages
			  </div>
			  <div class="card-body">
				<div id="searchcard">
				
                       <table style="width: 100%;">
                           <tr>
                               <td><label for="txtSearchQuery">Text:</label></td>
                               <td><input class="form-control" style="width: 100%;" type="text" 
                                   id="txtSearchQuery" name="txtSearchQuery" autocomplete="on" maxlength="100" 
                                   value=''/></td>
                                     <td style="width: 100px;">
                                      <select class="form-control" id="drpdnSearchType" name="drpdnSearchType">
                                        <option value="keyword" selected="selected">Keyword</option>
                                           <option value="regex">Regex</option>
                                           <option value="exactPhrase">Exact Phrase</option>		                                        
                                   </select>
                                     </td>
                                     <td>
                                         <button class="btn btn-primary" id="btnSearch">Search</button>
                                     </td>
                           </tr>
						<tr>
							<td>Scope:</td>
							<td><div class="form-check-inline"><label><input type="radio" name="radScope" value="text" checked="checked"/>Text</label></div>
							    <div class="form-check-inline"><label><input type="radio" name="radScope" value="_all" />Everything</label></div>
							</td>
						</tr>
                           <tr>
                               <td><label for="sortField">Sort by:</label></td>
                               <td colspan=3>
                               <div style="display: inline-block;     padding-right: 20px;" class="form-check-inline">
                                      <select class="form-control" id="sortField" name="sortField" style="width: 200px;">
                                           <option value="crawled_dt">Crawled Timestamp</option>
                                           <option value="published_date.date">Published Date</option>
                                           <option value="relevance" selected="selected">Relevance</option>
                                           <option value="text_length">Text Length</option>
                                           <option value="html_title.keyword">Title (HTML)</option>			                                        
                                   </select>
                                   </div>


                                           <div class="form-check-inline"><label><input type="radio" name="sortOrder" value="asc">Ascending</label></div>
                                           <div class="form-check-inline"><label><input type="radio" name="sortOrder" checked value="desc">Descending</label></div>
                                     </td>
                           </tr>
                         </table>				
				
				</div>
			  </div>
			</div>
		</div>
		<div class="analystSearchAlerts">
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
		<div class="analystRSS">
			<% if (Configuration.getConfigurationPropertyAsBoolean(domain, ConfigurationType.NEWS_FEED_UTILIZE)) {  %>	
	
			<div class="card card-default">
			  <div class="card-header">
			    <%= Configuration.getConfigurationProperty(domain, ConfigurationType.NEWS_FEED_TITLE) %>
			    <div style="float:right;vertical-align: middle"><span id='pauseButton' class='fas fa-pause'></span><span id='playButton' class='fas fa-play'></span></div> 
			  </div>
			  <div class="card-body">
				<div class="newstape" style="height: 510px;">
				  <div class="newstape-content">
				  </div>
				</div>
			  </div>
			</div>
            <% } %>
		</div>		
		<div class="analystConcepts">
	
			<div class="card card-default">
			  <div class="card-header">
			    Concepts
			    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btTopConcepts">Top</button>&nbsp;<button class="btn btn-primary btn-sm" id="btResetConcepts">Reset</button></div>
			  </div>
			  <div class="card-body">
			    <div id="conceptPie"></div>
			    <div id="conceptDateRangePane">Crawl date from <input type="text" id="conceptStartTime" name="startTime"/> to <input type="text" id="conceptEndTime" name="endTime" /> (Zulu/GMT time)</div>
			    <div style="float:right;"><button type="button" class="btn btn-primary btn-sm" id="btConceptSettings"><span class="fas fa-cog"></span></button>
			                              <button type="button" class="btn btn-primary btn-sm" id="btConceptHelp"><span class="fas fa-question-circle"></span></button></div>
			    <!-- <div>Click on wedge to drill down, Shift-click on wedge to search</div> -->
			    <!-- <div><table style="width:100%;"><tr><td>Crawl date from</td><td><input type="text" id="startTime" name="startTime" /></td><td>to</td><td> <input type="text" id="endTime" name="endTime" /></td> <td>(use Zulu / GMT time)</td></tr></table></div>
			     -->
			  </div>
			</div>
			
		</div>	
		<div class="analystTradecraft">
			<div class="card card-default">
			  <div class="card-header">
			    Open Source Tradecraft &amp; OpenKE
			  </div>
			  <div class="card-body">
			    Question Decomposition with 5 w's
			    <br>
			    Mind Mapping
			    <br>
			    Concepts &amp; PESTLE-S
			    <br>&nbsp;
			  </div>
			</div>
		</div>
		<%} %>
	</div>
	

	
		
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/conceptChart.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/domainHomeCollector.js?build=${build}"></script>

	<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>

	<!--  for scrollable news ticker -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/jquery.newstape/jquery.newstape.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>


<form id='searchForm' name='searchForm' method='POST' action="${applicationRoot}${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>
</body>
</html>