<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.wordCloulud"); %>

<title>Word Cloud</title>
</head>
<body onload="renderWordCloud(words);">
	<%@include file="header.jsp"%><br>

	<div class="form-group col-md-12" style="display:inline-flex;">
     <label class="radioLabel" ><input name="frequency" type="radio" value="doc_freq" checked="checked" />Document Occurrences</label> 
     <label class="radioLabel"><input name="frequency" type="radio" value="ttf" />Total Occurrences</label> 
     <label class="radioLabel"><input name="frequency" type="radio" value="term_freq" />Occurrences in this Document</label> 
	</div>

	<div id="divWordCloud" class='divWordCloud'></div>

	<script>
    	var words = ${words};
	</script>
	<body>
	
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/wordcloud/d3.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/wordcloud/LASAnalytic.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/wordcloud/d3.layout.cloud.js"></script>

</body>
</html>
