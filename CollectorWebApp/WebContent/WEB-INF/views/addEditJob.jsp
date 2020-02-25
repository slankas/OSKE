<%@page import="edu.ncsu.las.source.SourceHandlerInterface"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.addEditJob"); %>

<title>Jobs</title>


<!-- DataTable imports -->
<link rel="stylesheet" type="text/css"
	href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css" />
<link rel="stylesheet" type="text/css"
	href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css" />

<!--  cron editing -->
<link rel="stylesheet" type="text/css"
	href="${applicationRoot}resources/css/external/jquery-cron.css" />
<link rel="stylesheet" type="text/css"
	href="${applicationRoot}resources/css/external/jquery-gentleSelect.css" />
</head>

<body>
	<input type="hidden" name="jobID" id="jobID"
		value="<%=request.getAttribute("jobID")%>" />
	<%@include file="header.jsp"%><br>
	
	<div class="text-nowrap container-fluid">
	<div class="row">
		<div class="form-group col-md-12">
			<div>
				<table>
						<tr>
							<td colspan="5"><span id='lblHeading' style="font-size:1.25rem;">New Job</span></td>
							<td style="white-space: nowrap;" colspan="2" align="right">Status:
								<label id='lblStatus'>---</label>
							</td>
							<td/>
						</tr>
						<tr>
							<td colspan="5"><label id="err_label" style="color: red">
									Please add a name.</label> <label id="success_label"
								style="color: green"> Success</label></td>
							<td style="white-space: nowrap;" colspan="2" align="right"
								id="tdStatusDateTime">Status Datetime: <label
								id='lblStatusDatetime'></label></td>
							<td/>
						</tr>
						<tr>
							<td>Name:</td>
							<td colspan="6"><input style="width: 100%;" type="text"
								id="txtJobName" name="txtJobName" autocomplete="on"
								maxlength="100" /></td>
							<td/>
						</tr>
						<tr id="primaryLabelRow">
							<td><span id="primaryLabel">URL:</span></td>
							<td colspan="6"><input style="width: 100%;" type="text"
								id="txtPrimaryValue" name="txtPrimaryValue" autocomplete="on"
								maxlength="100" /></td>
							<td><a id="visitLink" href="#">Visit</a></td>
						</tr>
						<tr>
							<td style="white-space: nowrap;">Source Handler</td>
							<td><select id="drpdnSrcHandler" name="drpdnSrcHandler">
									<option value="" selected="selected">Select Handler...</option>
									<%
                                        	for (SourceHandlerInterface shi: Collector.getTheCollecter().getSourceHandlersForJobs(domain)){
                                        		if (shi.supportsJob()) {
                                        %>
									<option value="<%= shi.getSourceHandlerName() %>"
										data-primarylabel="<%=shi.getPrimaryLabel().toString()%>"
										data-primaryHidden="<%=shi.getPrimaryLabel().isHidden()%>"
										data-testable="<%=shi.isConfigurationTestable()%>"><%= shi.getSourceHandlerDisplayName() %></option>
									<%
                                        	    }
                                        	}
                                        %>
							</select></td>
							<td align="right">Priority:</td>
							<td colspan="1"><input type="number" id="txtPriority"
								value="100" name="txtPriority" autocomplete="on" /> (higher =
								higher precedence)</td>
						</tr>
						<tr>
							<td>&nbsp;
								
							</td>
							<td colspan="5">
							<div class="form-check form-check-inline">
								<input class="form-check-input" type="checkbox" value="" id="isExport">
								<label class="form-check-label" for="isExport">
    								&nbsp;Export collected data for transfer when job completes
  								</label>
  							</div>
							</td>
						</tr>
					</table>
					
					<br>
					
					<fieldset class="form-group col-md-7" style="border: 1px solid #ddd; margin-top: 1em; padding: 1em; border-radius: 4px;">
					<legend style="width:auto; font-size:1.25rem;">Schedule <small>(GMT-based)</small></legend>
					<table>
						<tr>
							<td ><div id='divSchedule'></div></td>
							<td>&nbsp;</td>
							<td>&nbsp;</td>
							<td>&nbsp;</td>
							<td ><a id="cronSwitch" href="#">Advanced</a></td>
							<td>&nbsp;</td>
							<td>&nbsp;</td>
							<td>&nbsp;</td>
							<td>
								<input type="checkbox" value="" id="isRandom">
							</td>
							<td>
								<label >Randomize Job Start by:</label>
							</td>
							<td>
								<input type="number" id="randomPercent" style="width: 75px"
									aria-label="Random Percent:" aria-describedby="randomPercent"
									min="1" max="500"
									title="Percentage between 1 to 500 to add and subtract from the given start time">&nbsp;<label>%</label>
							</td>
						</tr>
					</table>
					</fieldset>
					
					<br>
					
					<table>
						<tr>
							<td style="font-size:1.25rem;">Configuration:</td>
							<td >
								<button class="btn btn-outline-primary btn-sm" id="btnLoadTemplate">Load Template</button>&nbsp;&nbsp;
								<button class="btn btn-outline-primary btn-sm" id="btnShowParameters">Show Parameters</button>&nbsp;&nbsp;
								<button class="btn btn-outline-primary btn-sm" id="btnTestFormAuthentication">Test Authentication</button>
								<button class="btn btn-outline-primary btn-sm" id="btnTestConfiguration">Test Configuration</button>
								<button class="btn btn-outline-primary btn-sm" id="btnFormatCfg">Format JSON</button>
							</td>

							<td>&nbsp;</td>
						</tr>
						<tr>
							<td colspan="2">
								<div style="border: 1px solid #ddd; margin-top: 1em; padding: 1em; border-radius: 4px;height: 200px;" id="editor">{&#10;}</div>
							</td>
							<td valign=top>
								<div style="margin-top: 1em; padding: 1em;">
									<a target="_blank" href="http://www.freeformatter.com/java-regex-tester.html">Regex&nbsp;Tester</a>
									<br>
									<a target="_blank" href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Regex&nbsp;Constructs</a>
								</div>
							</td>
						</tr>
					</table>
					
					<br>
					
					<table>
						<tr>
							<td style="font-size:1.25rem;">Justification:</td>
						</tr>
						<tr>
							<td colspan="2"><textarea rows="5" id="txtJustification"
									name="txtJustification" style="width: 100%"></textarea></td>
						</tr>
						<tr>
							<td colspan="2">
								<div id="adjudicatorQuestionArea">
									<a style="padding-right: 20px;" id="adjudicatorQuestionsLink"
										href="#">Adjudicator Questions</a><br> <span
										id="numAdjQuestComplete">0</span> / <span
										id="numAdjQuestTotal">0</span> questions complete
								</div>
							</td>
						</tr>
						<tr>
							<td colspan="2">&nbsp;</td>
						</tr>
						<tr id="trJobLinks">
							<td colspan=7 style="white-space: nowrap;"><a
								style="padding-right: 20px;" id='jobHistory'></a> <a
								style="padding-right: 20px;" id='recentVisitedPages'></a></td>
						</tr>
						<tr>
							<td colspan=2>
								<button style="margin-right: 10px;" class="btn btn-danger" id="btnCancel" onclick="return goBack()">Cancel</button>
								<button style="margin-right: 10px;" class="btn btn-primary" id="btnCreateSubmit" onclick="return submitForm(false)">Create / Submit</button>
								<button style="margin-right: 10px;" class="btn btn-primary" id="btnCreateSubmitBack" onclick="return submitForm(true)">Create / Submit Back</button>
								
								<button style="margin-right: 10px;" class="btn btn-secondary" id="btnSaveAsNew" onclick="return saveAsNew()">Save As New</button>
								<button style="margin-right: 10px;" class="btn btn-secondary" id="btnHold" onclick="return editStatus('hold')">Hold</button>
								<button style="margin-right: 10px;" class="btn btn-secondary" id="btnSchedule" onclick="return editStatus('schedule') ">Resume Schedule</button>
								
								<button style="margin-right: 10px;" class="btn btn-secondary" id="btnPurgeData" onclick="return purgeDataForJob()">Purge Data</button>
								<button style="margin-right: 10px;" class="btn btn-secondary" id="btnDeleteJob" onclick="return deleteJob()">Delete Job</button>
								
								<button style="margin-right: 10px;" class="btn btn-success" id="btnRunNow" onclick="return editStatus('run') ">Run Now</button>
								<button style="margin-right: 10px;" class="btn btn-danger" id="btnStop" onclick="return editStatus('stop')">Stop</button>
							</td>
						</tr>
				</table>
			</div>
		</div>
		<input type='hidden' id='jobId' name='jobId'
			value='<%=request.getAttribute("jobId")%>' />
	</div>
	</div>

	<div class="modal fade" tabindex="-1" role="dialog" id="myModal">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<h4 class="modal-title" id="myModalTitle">Status Changed</h4>
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
				</div>
				<div class="modal-body" id="myModalDialogText">
					<p>Message&hellip;</p>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
				</div>
			</div>
			<!-- /.modal-content -->
		</div>
		<!-- /.modal-dialog -->
	</div>
	<!-- /.modal -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/JobFactory.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/addEditJob.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/ace/ace.js"></script>

	<!--  Used to make cron job entries appear human readable: prettyCron.toString("37 10 * * * *");  -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/cronstrue.js"></script>

	<!-- Edit cron:  see http://shawnchin.github.io/jquery-cron/ -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-gentleSelect.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-cron.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/elasticSearch/jsontree.min.js"></script>

	<!-- DataTable imports -->
	<!-- DataTable imports -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>


</body>
</html>