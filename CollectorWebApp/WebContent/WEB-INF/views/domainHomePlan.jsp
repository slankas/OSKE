<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.plan"); %>

<title>OpenKE: Project Plan</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/planHome.css?build=${build}" />

<style>
	.custom {
    	width: 75px !important;
	}
</style>
</head>
<body>
<%@include file="header.jsp"%><br>
<input type="hidden" name="projectUUID" id="projectUUID" value="<%=request.getAttribute("projectUUID")%>" />	
	<div class="projectContainer">

		<div class="projectList">
			<div class="card card-default">
			  <div class="card-header">
			    <h5 style="display: inline-block" class="card-title">Projects</h5>
			    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btProjectNew">Create</button></div>
			  </div>
			  <div class="card-body" id="projectList">

			  </div>
			</div>
		</div>



		<div class="projectDetail">
			<div class="card card-default">
				  <div class="card-header">
				  <h5 style="display: inline-block" class="card-title">Project Detail</h5>
				    
				    <div style="float:right;vertical-align: top; position:relative; top: -4px;"><button class="btn btn-primary btn-sm" id="btMakeCurrentProject">Make Current Project</button>
				                                                                                <button class="btn btn-outline-primary btn-sm" id="btEdit">Edit</button><button class="btn btn-primary btn-sm" id="btSave">Save</button>
				                                                                                <button class="btn btn-outline-primary btn-sm" id="btSetActive">Mark Active</button>
				                                                                                <button class="btn btn-outline-primary btn-sm" id="btSetInactive">Mark Inactive</button>
				                                                                                <button class="btn btn-outline-danger btn-sm" id="btDelete">Delete</button>
				                                                                                <button class="btn btn-outline-danger btn-sm" id="btCancel">Cancel</button></div>
				  </div>
				  <div class="card-body">
					<div id="projectDetailDisplay">
						<strong>Name:</strong>&nbsp;<span id="pdName"></span>
						<p>&nbsp;<p>
						<strong>Purpose:</strong><br>
						<span id="pdPurpose"></span>
						<p>&nbsp;<p>
						<strong>Key Questions:</strong><br>
						<div id="pdkeyQuestions">
					    <ul id="keyQuestionsList">
					    </ul>
					    </div>
					    <p>
					    <strong>Assumptions:</strong><br>
				        <div id="pdAssumptions">
				          <ul id="assumptionsList">
				          </ul>     
				        </div>
				             
						<strong>External Links:</strong><br>
						<div id=pdExternalLinks>
					    <ul id="externalLinksList">
					    </ul>
					    </div>					    
					</div>
					
					<div id="projectDetailEdit">
						<strong>Name:</strong> &nbsp; <input name="projectName" id="projectName" minlength="3" maxlength="255" type='text' size=60 class='form-control'>
						<p>&nbsp;<p>
						<strong>Purpose:</strong><br>
						<textarea name="projectPurpose" id="projectPurpose" rows="3" cols="80" maxlength="4000" class='form-control'></textarea>						
						<p>&nbsp;<p>
						<strong>Key Questions:</strong><br>
						<table class="table table-condensed table-borderless" id="tblKeyQuestions">
							<thead><tr><td style="width:250px;">Tag</td><td style="">Question</td><td></td></tr></thead>
							<tbody>
							  <tr>
							     <td><input name="kqTag" id="kqTag" minlength="2" maxlength="30" type='text' size=25 class='form-control'/></td>
							     <td><input name="kqQuestion" id="kqQuestion" minlength="5" maxlength="255" type='text' size=65 class='form-control'/></td>
							     <td><input class="btn btn-outline-success custom" type="button" id="btnAddKeyQuestion" value="Add"	onclick="insertKQRow()" /></td>
							  </tr>
							</tbody>
						</table>

					    <p>
					    <strong>Assumptions:</strong>
						<table class="table table-condensed table-borderless" id="tblAssumptions">
							<thead><tr><th style=""></th><th></th></tr></thead>
							<tbody>
							  <tr>
							     <td><input name="assumption" id="kqQuestion" minlength="5" maxlength="255" type='text' size=80 class='form-control'/></td>
							     <td><input class="btn btn-outline-success custom" type="button" id="btnAddAssumption" value="Add"	onclick="insertAssumptionRow()" /></td>
							  </tr>
							</tbody>
						</table>	
						
						<p>				    
						<strong>External Links:</strong><br>
						<table class="table table-condensed table-borderless" id="tblExternalLinks">
							<thead><tr><td style="">Link</td><td style="">Display Text</td><td></td></tr></thead>
							<tbody>
							  <tr>
							     <td><input name="elLink" id="elLink" minlength="10" maxlength="255" type='text' size=40 class='form-control'/></td>
							     <td><input name="elDisplayText" id="elDisplayText" minlength="2" maxlength="100" type='text' size=38 class='form-control'/></td>
							     <td><input class="btn btn-outline-success custom" type="button" id="btnAddExternalLink" value="Add"	onclick="insertELRow()" /></td>
							  </tr>
							</tbody>
						</table>				    
					</div>		
					
					<div id="projectDetailEmpty">
					  <i>No project is currently selected or set as active.</i>
					</div>
								
				 </div>
			</div>
		</div>		
	</div>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.mousewheel-3.1.13.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/domainHomePlan.js?build=${build}"></script>

</body>
</html>
