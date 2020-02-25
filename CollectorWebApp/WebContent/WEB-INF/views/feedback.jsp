<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.feedback"); %>

<title>OpenKE: Submit Feedback</title>
</head>

<body>
<%@include file="header.jsp"%><br>
<div class="container-fluid">
<div class="row">
	  <div class='col-sm-12'>
	    <h4>Feedback</h4>      
      </div>
</div>
<div class="row">
  <div class="form-group col-md-12">
    <div>
      <label id="err_label" style="color: red">Please add a name.</label>
      <form class="form-horizontal">
        <div class="form-group">
    	  <label for="txtSubject" class="col-sm-1 control-label">Subject</label>
    	  <div class="col-sm-7"><input style="width: 100%;" type="text"	id="txtSubject" name="txtSubject" autocomplete="on" maxlength="100" /> </div>
        </div>
        <div class="form-group">
    		<label for="txtComments" class="col-sm-1 control-label">Comments</label>
    		<div class="col-sm-7"><textarea rows="5" id="txtComments" name="txtComments" style="width:100%"></textarea></div>
        </div>
      </form>
    </div>
  </div>
</div>
<div class="row">
	  <div class='col-sm-12' style='float:right;'>      
          <button style="margin-right:10px;" class="btn btn-primary" id="btnSubmit">Submit</button>
          <button style="margin-right:10px;" class="btn btn-primary" id="btnCancel"><span class="fas fa-home"></span>&nbsp;&nbsp;Domain Home</button>
      </div>
</div>
</div>
</body>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/JobFactory.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/feedback.js?build=${build}"></script>

</html>