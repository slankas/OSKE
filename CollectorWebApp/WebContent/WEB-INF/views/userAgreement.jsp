<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.userAgreement"); %>


<title>User Agreement</title>
</head>
<body>
<%@include file="header.jsp"%>
<%	if (request.getAttribute("message") != null ) { %>		
      <input type="hidden" name="uaMessage" id="uaMessage"	value="<%=request.getAttribute("message")%>" />
<% 	}  %>

<div style="margin: 0px 10px 0px 10px;">
<div class="row">
	<span id="agreementText"> </span>
</div>
<div class="row">
	<span id="requiredReadings"></span>
</div>
<div class="row">
	<span id="questions"></span>
</div>

<div class="row">
  &nbsp;
  <div class='form-inline'>
    <label style='font-weight:bold !important;' for='tfUserOrganization'>Organization</label>
    <input type='text' class='form-control' id='tfUserOrganization' placeholder='Enter your organization'size=50>
  </div>
</div>

<div class="row">
  &nbsp;
  <div class='form-inline'>
    <label style='font-weight:bold !important;' for='tfUserName'>Name</label>
    <input type='text' class='form-control' id='tfUserName' placeholder='Enter your full name'size=50>
    <button class="btn btn-primary" onclick="signUserAgreement()">Sign Agreement</button>
  </div>
</div>
<div class="row">
	&nbsp;
</div>

</div>

</body>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/userAgreementForm.js?build=${build}"></script>

</html>

