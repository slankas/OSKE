<%@page import="edu.ncsu.las.source.SourceHandlerInterface"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.searchAlerts"); %>

    <title>OpenKE: Manage Search Alerts</title>
    <link rel="SHORTCUT ICON" href="${applicationRoot}resources/images/LAS_Logo.ico">
    
    	<!--  cron editing -->
	<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/jquery-cron.css" />
	<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/jquery-gentleSelect.css" />
        <!-- DataTable imports -->
	<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
	<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
	<!--  <link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/mytable.css?build=${build}" /> -->
	<style>
	.fas {display:inline;}
	</style>
</head>
<body>
<%@include file="header.jsp"%>
<div class="container-fluid">
<div class="row">
<div class="form-group col-md-12">
<h5 style="display: inline-block" class="card-title">Manage Search Alerts</h5>
</div>
</div>

<div class="row">
<div class="form-group col-md-12">
    <table class="display table table-striped table-bordered table-tight" id="tblAlerts">
        <thead>
        <tr>
            <th class="thHeadings" id="thActions"></th>        
            <th class="thHeadings" id="thName">Name</th>
            <th class="thHeadings" id="thConfiguration">Configuration</th>
            <th class="thHeadings" id="thOwner">Owner</th>
            <th class="thHeadings" id="thSchedule">Schedule</th>
            <th class="thHeadings" id="thLastRun">Last Run</th>
            <th class="thHeadings" id="thNextRun">Next Run</th>
            <th class="thHeadings" id="thStatus">State</th>
        </tr>
        </thead>
    </table>
</div>
</div>
<% if (u.hasAccess(domain,RoleType.ADMINISTRATOR)) { %>    
<div class="row">
<div class="form-group col-md-12">
View alerts by all owners: <input id='cbViewAllAlerts' checked type='checkbox' value='true'>
</div>
</div>
<% } %>



<div class="row">
  <div class="form-group col-md-12">
    <button class="btn btn-primary" id="btnOpenSearchAlertDialog"><span class="fas fa-pencil-alt"></span> Create Search Alert</button>
    <button class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span> Domain Home</button>
  </div>
</div>


<div class="row" id="searchAlertRow">
  <div class="col-md-8">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Search Alerts</strong>
	    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btViewAllNotifications">View All</button>&nbsp;&nbsp;<button class="btn btn-primary btn-sm" id="btAcknowledgeAll">Acknowledge All</button></div>
	  </div>
	  <div class="card-body" id="searchAlertBody">
		<div id="searchAlertList">
		</div>
	  </div>
	</div>
  </div>
</div>	
	
	
<div class="modal fade" id="searchAlertModal" tabindex="-1" role="dialog" aria-labelledby="searchAlertModalLabel">
  <div class="modal-dialog modal-lg" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title" id="searchAlertModalLabel">Create Search Alert</h4>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
      </div>
      <div class="modal-body">
				<form class="form-horizontal" role="form">
					  <div class="form-group row">
					    <label class="control-label col-md-3" for="searchAlertName">Search Alert Name:</label>
					    <div class="col-md-4">
					      <input type="text" class="form-control" id="searchAlertName" placeholder="Enter Search Alert Name">
					    </div>
					  </div>
					  <div class="form-group row">
					    <label class="control-label col-md-3" for="searchTerms" id="searchTermsLabel">Search Terms:</label>
					    <div class="col-md-4"> 
					      <input type="text" class="form-control" id="searchTerms" placeholder="Enter Search Terms">
					    </div>
					  </div>
					  <div class="form-group row">
					    <label class="control-label col-md-3" >Number of Search Results:</label>
					    <div class="col-md-1">
					      <input type="text" class="form-control" id="numberOfsearchResults" value="20">
					    </div>
					  </div>
					  <div class="form-group row">
					    <label class="control-label col-md-3">Schedule:</label>
					    <div class="col-md-9" style='margin-top: 6px;'>
					      <div id='searchAlertSchedule'></div>
					    </div>
					  </div>					  
					  
					  <div class="form-group row">
					    <label class="control-label col-md-3" >Method:</label>
					    <div class="col-md-3">
					      <select id="searchAPI" class="form-control">
                          <%  for (SourceHandlerInterface shi: Collector.getTheCollecter().getSourceHandlersForDomainDiscovery(domain)) {  %>
                                  <option value="<%= shi.getSourceHandlerName() %>" data-primarylabel="<%=shi.getPrimaryLabel().toString()%>" data-primaryHidden="<%=shi.getPrimaryLabel().isHidden()%>" data-maxresults="<%=shi.getMaximumNumberOfSearchResults()%>" data-default="<%=shi.isDefaultDomainDiscoverySearchMethod()%>"><%= shi.getSourceHandlerDisplayName() %></option>
                           <% } %>				
					      </select>
					    </div>
					  </div>		
					  
					  <div class="form-group row">
					    <label class="control-label col-md-3" >Acknowledge initial results: </label>
					    <div class="col-md-9" style='margin-top:2px'> <input  id='preacknowledge' checked type='checkbox' value='true'>
					    </div>
					  </div>				  
				</form>
      </div>
      <div class="modal-footer">
           <button id="btCreateSearchAlert" type="button" class="btn btn-primary">Create</button>
           <button id="btCloseDialog" type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>	
	
</div>
	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/manageSearchAlerts.js?build=${build}"></script>

<!--  Used to make cron job entries appear human readable: prettyCron.toString("37 10 * * * *");  -->
<!-- Edit cron:  see http://shawnchin.github.io/jquery-cron/ -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-gentleSelect.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-cron.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/cronstrue.js"></script>

<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>

<!-- DataTable imports -->
<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>



</body>
</html>