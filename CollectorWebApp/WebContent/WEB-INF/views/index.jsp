<%@page import="java.time.Instant"%>
<%@page import="edu.ncsu.las.model.collector.Configuration"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","system.home"); %>

<title>OpenKE</title>
<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css"> 


</head>
<body>
	<%@include file="header.jsp"%>
	<br>
<div class="container-fluid">
	<div class="row">
		<div class="form-group col-md">
				<div class='btn-toolbar pull-right'>
					<div class='btn-group'>
					  <% if (u.hasAccess(Domain.DOMAIN_SYSTEM,RoleType.ADMINISTRATOR)) { %>
						<a>
							<button class="btn btn-primary" id="btnManageDomains">Manage Domains</button> 
						</a>
						<% } %>

						<% if (u.hasAccess(Domain.DOMAIN_SYSTEM,RoleType.ADJUDICATOR)) { %>
						<a>
							<button class="btn btn-primary" id="btnManageUserAgreements">Manage User Agreements</button>
						</a>
						<% } %>
					</div>
				</div>
		</div>
	</div>
	<div class="row">
	<div class="col-md"> 
			<table class="table-bordered table-striped" id="tblDomain">
				<thead>
					<tr>
						<th class="thHeadings" id="thdFullName">Full Name</th>
						<th class="thHeadings" id="thdDescription">Description</th>
						<th class="thHeadings" id="thdStatusDate">Last Configuration Change</th>
						<th class="thHeadings" id="thdPrimaryContact">Primary Contact</th>
						<th class="thHeadings" id="thdName">ID</th>
					</tr>
				</thead>
			</table>
			</div>
	</div>
<% if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM, ConfigurationType.KIBANA_UTILIZE_DASHBOARD)) { %>
	<div class="row">
	<h4><%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.KIBANA_HOME_TITLE)%></h4>
	<iframe class="seamless" src="<%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.KIBANA_HOME_DASHBOARD)%>" height="300" width="100%"></iframe>
	</div>
<% } %>
	
	<div class="row">
	Build: <i><%= edu.ncsu.las.servlet.SystemInitServlet.getWebApplicationBuildTimestamp(getServletContext()) %></i>
	</div>
	<div class="row">
	Time: <i><%= Instant.now().toString().replaceAll("-|:","") %></i>
	</div>	
</div>

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/index.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/EscapeHtml.js?build=${build}"></script>
	
	<!--  Used to make cron job entries appear human readable: prettyCron.toString("37 10 * * * *");  -->
    <script type="text/javascript" charset="utf8" src="resources/js/external/cronstrue.js"></script>
    
	<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>	

<script>
if (typeof openke == 'undefined') {
	location.reload(true)
}
</script>
	
</body>
</html>