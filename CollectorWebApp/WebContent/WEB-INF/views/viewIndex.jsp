<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.viewIndex"); %>
<title>View Index</title>


<link rel="stylesheet" href="${applicationRoot}resources/jqTree.1.3.8/jqtree.css">
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/viewIndex.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/split.css" />
<!-- DataTable imports -->
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>
</head>

<body>
	<div class="overlay">
	    <div id="loading-img"></div>
	</div>
<input type="hidden" name="documentArea" id="documentArea" value="<%=request.getAttribute("documentArea")%>" />
<input type="hidden" name="documentIndexID" id="documentIndexID" value="<%=request.getAttribute("documentIndexID")%>" />
<%@include file="header.jsp"%><br>
<h3 id="indexTitle">Loading Index ...</h3>
<%@include file="includes/DocumentIndexView.jsp"%><br>
</body>



<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
<script type="text/javascript" charset="utf8"src="${applicationRoot}resources/jqTree.1.3.8/tree.jquery.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>


<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASExport.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>

<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentBucket.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentBucketSupport.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentIndexView.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/viewIndex.js?build=${build}"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/elasticSearch/jsontree.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/split.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/porter.js"></script>




</html>