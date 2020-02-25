<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.notImplemented"); %>
<title>Not Implemented</title>
</head>
<body>
	<%@include file="header.jsp"%><br>
    <div class="col-md-12">
	<div class="lblError">
		<label>Functionality not implemented</label>
    </div>
    </div>
	<br>
	<br>

	<form >
		<div class="col-md-12">
                <div class="btn-group btn-group-sm floatleft">
                    <button type="submit" class="btn btn-primary" id="btnHome"
                        formaction="${applicationRoot}">Home</button>
                </div>
		</div>	
	</form>
</body>
</html>