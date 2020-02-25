<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.userProfile"); %>

    <title>User Profile</title>

    <link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
    <link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>

</head>
<body>
<%@include file="header.jsp"%>

<h2>User Profile</h2>

<div class="row">
    <div class="form-group col-md-12">
    <%
        if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_REQUIRE_USERAGREEMENT)) {
    %>
    <button type="button" class="btn btn-blue" id="usrAgrHis">User Agreement History</button>
    <%
        }
    %>
</div>
</div>

<h3>Application Access</h3>
<div class="row">
    <div class="form-group col-md-12">
        <table class="display table table-striped table-bordered table-tight"
               id="tblUserAccess">
            <thead>
            <tr>
                <th class="thHeadings" id="domain">Domain</th>
                <th class="thHeadings" id="role">Role</th>
            </tr>
            </thead>
        </table>
    </div>
</div>

<div class="row">
    <div class="col-md-12">
        <div class="btn-group floatleft">
            <button type="submit" class="btn btn-primary" id="btnHome">OpenKE Home</button>
        </div>
    </div>
</div>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/userProfile.js?build=${build}"></script>


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