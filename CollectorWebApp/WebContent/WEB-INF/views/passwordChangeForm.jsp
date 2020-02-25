<%@page import="java.time.Instant"%>
<%@page import="edu.ncsu.las.model.collector.Configuration"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","system.passwordChange"); %>

<title>OpenKE: Password Change</title>
</head>
<body>
	<%@include file="header.jsp"%>
	<br>


<div class="center-div">	
 <form  style="margin: 10px;" id="passwordChangeForm" action="<%=pageContext.getAttribute("applicationRoot")%>system/localAuth/changePassword" method="post">
      <h1 class="h3 mb-3 font-weight-normal">Password Change</h1>
      <label for="inputEmail" class="sr-only">Email address</label>
      <input type="password" id="newPassword" name="newPassword" class="form-control" placeholder="New password" required autofocus>
      <label for="inputPassword" class="sr-only">Password</label>
      <input type="password" id="verifyPassword" name="verifyPassword" class="form-control" placeholder="Verify password" required>
       <p>&nbsp;<p>
       <% if (request.getAttribute("error") !=null) { %>
       		<div class="alert alert-danger" role="alert"><%=request.getAttribute("error").toString() %></div><p>&nbsp;<p>
	   <% } %>
      
    </form>
    <button class="btn btn-lg btn-primary btn-block" onclick="validateAndSubmitPassword()">Change Password</button>
 </div>
 
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/EscapeHtml.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASHeader.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
	
	<script>
		function validateAndSubmitPassword() {
			if ($('#newPassword').val().trim() == "") {
				Snackbar.show({text: "You must enter a new password."})
				return false;
			}
			if ($('#newPassword').val().trim().length < <%=Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM,ConfigurationType.WEBAPP_AUTH_LOCAL_MIN_PASSWORD_LENGTH)%> ) {
				Snackbar.show({text: "Your new password is too short, must be at least " + <%=Configuration.getConfigurationPropertyAsInt(Domain.DOMAIN_SYSTEM,ConfigurationType.WEBAPP_AUTH_LOCAL_MIN_PASSWORD_LENGTH)%> + " characters"})
				return false;
				
			}
			if ($('#newPassword').val().trim() != $('#verifyPassword').val().trim()) {
				Snackbar.show({text: "Your passwords do not match."})
				return false;;
			} 
			
			
			$('#passwordChangeForm').submit();
			return false;
		}
	</script>
	
</body>
</html>