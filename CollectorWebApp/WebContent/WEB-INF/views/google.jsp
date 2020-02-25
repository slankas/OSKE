<%@page import="edu.ncsu.las.model.collector.type.ConfigurationType"%>
<%@page import="edu.ncsu.las.model.collector.Domain"%>
<%@page import="edu.ncsu.las.model.collector.Configuration"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.login"); %>
<meta name="google-signin-client_id" content="<%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.WEBAPP_AUTH_OAUTH_CLIENTID)%>">
<title>OpenKE</title>
</head>
<body>

<% String proj2Name = pageContext.getAttribute("applicationRoot").toString();
   if (proj2Name.contains(";")) { proj2Name = proj2Name.substring(0,proj2Name.indexOf(";")); pageContext.setAttribute("projName", proj2Name); }  %>
<script>
	var openkeContextRoot = ${applicationRoot}
</script>
<header class=headerStyle>
	<div class="row">
		<div class="col-md-3">  
			<a class=headerLeftAbsoluteStyle href="${applicationRoot}"><img height="75" width="250" src='${applicationRoot}<%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.APPLICATION_LOGO)%>'> </a>
		</div>
	</div>
	</header>




	<br>

<div class="messageAllMargin center-div"> Sign into to OpenKE using your Google credentials<p>
<div class="g-signin2 inner-div" data-onsuccess="onSignIn"></div>
</div>
	

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/EscapeHtml.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASHeader.js?build=${build}"></script>
	
	<script src="https://apis.google.com/js/platform.js" ></script>
	
	<script>

	function onSignIn(googleUser) {
		var id_token = googleUser.getAuthResponse().id_token;

		$.ajax({	
			url : openkeContextRoot+"rest/system/user/authenticate",
			type : "POST",
			data : JSON.stringify({"token" : id_token}),
			contentType: "application/json; charset=utf-8",
			success : function(data) {
				var auth2 = gapi.auth2.getAuthInstance();
			    auth2.signOut()
				location.reload()
			},
			error : function(jqXHR, textStatus, errorThrown) {
				bootbox.alert(jqXHR.responseJSON.reason);
			}
		});
		

		}
	</script>
</body>
</html>