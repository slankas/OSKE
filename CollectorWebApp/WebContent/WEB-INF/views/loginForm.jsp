<%@page import="java.time.Instant"%>
<%@page import="edu.ncsu.las.model.collector.Configuration"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","system.login"); %>

<title>OpenKE: Login</title>
</head>
<body>
	<%@include file="header.jsp"%>
	<br>


<div class="center-div">	
 <form  style="margin: 10px;" id="loginForm" name="loginForm" action="<%=pageContext.getAttribute("applicationRoot")%>system/localAuth/authenticate" method="post">
      <h1 class="h3 mb-3 font-weight-normal">Please sign in</h1>
      <label for="inputEmail" class="sr-only">Email address</label>
      <input type="email" id="email" name="email" class="form-control" placeholder="Email address" required autofocus>
      <label for="inputPassword" class="sr-only">Password</label>
      <input type="password" id="password" name="password" class="form-control" placeholder="Password" required>
      <!-- 
      <div class="checkbox mb-3">
        <label>
          <input type="checkbox" value="remember-me"> Remember me
        </label>
      </div>
       -->
       <p>&nbsp;<p>
       <% if (request.getAttribute("error") !=null) { %>
       		<div class="alert alert-danger" role="alert"><%=request.getAttribute("error").toString() %></div><p>&nbsp;<p>
	   <% } %>
      <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
      <p>
      <%  if (request.getSession(true).getAttribute("allowPasswordReset") != null && request.getSession().getAttribute("allowPasswordReset").equals(Boolean.TRUE)) { %>
            <a class="btn btn-link" href="javascript:forgotPassword();">Forgot Your Password?</a>
            	<script>
		function forgotPassword() {
			if ($('#email').val().trim() == "") {
				Snackbar.show({text: "You must enter your email address to reset your password."})
				return;
			}
			$('#loginForm').attr('action', "<%=pageContext.getAttribute("applicationRoot")%>system/localAuth/resetPassword").submit();
			return false;
		}
	</script>
      <% } else { %>
      		<script>window.onload = function() {document.loginForm.action = "<%=pageContext.getAttribute("applicationRoot")%>system/localAuth/authenticateLDAP";}</script>
      <% } %>
    </form>
 </div>
 
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/EscapeHtml.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASHeader.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
	

	
</body>
</html>