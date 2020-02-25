<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.userAgreement.history"); %>

    <title>User Agreement History</title>

    <link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
    <link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>

</head>
<body>
<%@include file="header.jsp"%>
<h2>User agreement history</h2>

<div class="row">
<div class="col-md-12">
    <table class="display table table-striped table-bordered table-tight"
           id="tblUsrAgrHis">
        <thead>
        <tr>
            <th class="thHeadings" id="idAgreeTS">Signature Date</th>
            <th class="thHeadings" id="idExpireTS">Expiration Date</th>
            <th class="thHeadings" id="idStatusTS">Status Date</th>
            <th class="thHeadings" id="idStatus">Status</th>
            <th class="thHeadings" id="idSAdjudicator">Adjudicator</th>
            <th class="thHeadings" id="idSAdjComments">Adjudicator Comments</th>
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
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/userAgreementHistory.js"></script>


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