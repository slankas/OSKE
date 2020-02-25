<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.authenticationError"); %>

<title>Authentication Error</title>
</head>
<body>
	<%@include file="header.jsp"%><br>
	<div class="lblError">
		<label>Authentication Error</label>
	</div>
	<br>
	<br>
	You do not have access.

	<form action="" method="post">
		<div class="row">
			<div class="col-md-12">
				<div class="btn-group btn-group-sm floatleft">
					<button type="submit" class="btn btn-primary" id="btnGoBack"
						formaction="${applicationRoot}">Home</button>
				</div>
			</div>
		</div>
	</form>
</body>
</html>