<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.userAgreement.review"); %>


    <title>User Agreement</title>

</head>
<body>
<%@include file="header.jsp"%>
<div style="margin: 0px 10px 0px 10px;">
	<h2>User Agreement - Under Review</h2>
    Your submitted user agreement is still under review. You cannot access the system until that review is completed.
</div>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>

</body>
</html>
