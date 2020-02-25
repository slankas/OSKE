<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.adjudication"); %>


<title>Adjudicate</title>

<!-- DataTable imports -->
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>

</head>
<body>
<%@include file="header.jsp"%><br>

	<div class="text-nowrap container-fluid">
	<div class="row">
		<div class="form-group col-md-12">
			<div>
				<h4>Adjudicate Jobs</h4>
				<br> <label id="err_label_user" style="color: red"></label>
			</div>
		</div>
		<div class="form-group col-md-12">
			<table class="display table table-striped table-bordered table-tight"
				id="tblJobs">
				<thead>
					<tr>
						<th class="thHeadings" id="thdName">Name</th>
						<th class="thHeadings" id="thdURL">URL /<br>Search Terms</th>
						<th class="thHeadings" id="thdSourceHandler">Source Handler</th>
						<th class="thHeadings" id="thdSchedule">Schedule</th>
						<th class="thHeadings" id="thdConfiguration">Configuration</th>
						<th class="thHeadings" id="thdRandomPercent">Randomize Wait<br>Percentage</th>
						<th class="thHeadings" id="thdStatusDate">Status Date</th>
						<th class="thHeadings" id="thdOwnerEmail">Owner Email</th>
						<th class="thHeadings" id="thdAction">Take Action</th>
					</tr>
				</thead>
			</table>
			<br><br>

		</div>


			<div class="col-md-12">
				<div class="btn-group btn-group-sm">
					<form>
					<button type="submit" class="btn btn-primary" id="btnGoBack" formaction="${applicationRoot}<%=domain%>">Domain Home</button>
					</form>
				</div>
			</div>


	</div>
	</div>
	
	
<div class="modal fade" tabindex="-1" role="dialog" id="commentModal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title" id ="commentModalTitle">Disapprove Job</h4>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
      </div>
      <div class="modal-body" id = "commentModalComments">
      	Enter the reason(s) for the disapproval:<p><textarea rows="5" cols="75" id="txtComments" name="txtComments"></textarea><br>
      </div>
      
      <div class="modal-footer">
        <input type="submit" class="btn btn-primary" id="btUpdateComment" value="Disapprove" />
        <button type="button" class="btn btn-primary" data-dismiss="modal">Cancel</button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
	
<div class="modal fade" tabindex="-1" role="dialog" id="messageModal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title" id ="messageModalTitle">Status Changed</h4>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
      </div>
      <div class="modal-body" id ="messageModalDialogText"></div>  
      <div class="modal-footer">
        <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->	
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/adjudication.js?build=${build}"></script>
	
	<!--  Used to make cron job entries appear human readable: prettyCron.toString("37 10 * * * *");  -->
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/cronstrue.js"></script>	
	
<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.bootstrap4.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.html5.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.print.min.js"></script>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/buttons.flash.min.js"></script>

</body>
</html>