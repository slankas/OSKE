<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.userAgreementAdjudicator"); %>

    <title>OpenKE</title>

    <!-- DataTable imports -->
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>


</head>
<body>
<%@include file="header.jsp"%>
<br>
<div class="row">

    <div class='btn-toolbar pull-right'>
        <div class='btn-group'>
            <a>
                <button class="btn btn-primary" id="btnViewAllRecords">View All Records</button>
            </a>
        </div>

    </div>
</div>

    <div class="form-group col-md-12">
        <table class="display table table-striped table-bordered table-tight"
               id="tblUserAgreements">
            <thead>
            <tr>
                <th class="thHeadings" id="emailId">Email Id</th>
                <th class="thHeadings" id="status">Status</th>
                <th class="thHeadings" id="subdate">Submission Date</th>
                <th class="thHeadings" id="expdate">Expiration Date</th>
                <th class="thHeadings" id="dec">Actions</th>
            </tr>
            </thead>
        </table>
    </div>



</body>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/userAgreementAdjudicator.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/EscapeHtml.js?build=${build}"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>



</html>