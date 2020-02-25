<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.summary"); %>


<title>OpenKE: Text Summarization</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/summary.css?build=${build}" />

</head>
<body>
<%@include file="header.jsp"%><br>

<div style="padding:10px">
<h3>Text Summary</h3>
<div>
	<div style="float:left;"> Retention level:<span id="sliderVal">100</span></div>
	<div style="float:left;width:400px;clear: left" id="slider"></div>
	<div style="float:right;vertical-align: top; position:relative; top: -8px;"><label for="annotate"> <input type="checkbox" id="annotate" value="annotate" name="annotate" checked> Annotate Resources </label> </div>
</div>
<div style="width:100%;display: table; border-collapse: collapse;margin: 5px;">
    <div style="width:50%; display: table-cell; border:1px solid black;">
       <h4>Original Text</h4>
       <div id="originalText" style="width:100%;padding:10px;"><%=request.getAttribute("summaryText").toString().replaceAll("\n","<br>") %></div>
    </div>

    <div style="width:50%; display: table-cell; border:1px solid black;">
    	<h4>Summary</h4>
        <div id="summaryText" style="width:100%;padding:10px;"></div>
    </div>

</div>


<p>&nbsp;<p>
Summarization Method: TextRank<br>
Mihalcea, Rada, and Paul Tarau. "<i>Textrank: Bringing order into text</i>". Proceedings of the 2004 conference on empirical methods in natural language processing. 2004. 
[<a href='http://www.aclweb.org/anthology/W04-3252'>paper</a>]<br>
Python Library: <a href='https://radimrehurek.com/gensim/'>gensim</a>
</div>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/d3.v3.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/summary.js?build=${build}"></script>
</body>
</html>