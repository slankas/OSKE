<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","system.addEditDomain"); %>

<title>Domains</title>
</head>

<body>
<input type="hidden" name="domainID" id="domainID" value="<%=request.getAttribute("domain")%>" />
<%@include file="header.jsp"%>
<div class="container-fluid">
	<div class="row">
			<div class="form-group col-md-12">
				<div>
					<table>
						<thead>
							<tr>
								<td colspan="5"><h4 id='lblHeading'>New Domain</h4></td>
                                <td style="white-space: nowrap;" colspan="2" align="right">Status: <label id='lblStatus' >---</label></td>
							</tr>
							<tr>
								<td colspan="5"><label id="err_label" style="color: red">Please add a name.</label><label id="success_label" style="color: green">Success</label></td>
								<td style="white-space: nowrap;" colspan="2" align="right" id="tdStatusDateTime">Status Datetime: <label id='lblStatusDatetime'></label></td>
							</tr>
							<tr>
								<td>Identifier:</td>
								<td colspan="6"><input type="text"	id="txtInstanceName" name="txtInstanceName" autocomplete="on" maxlength="15" /></td>
							</tr>
							<tr>
								<td>Full Name:</td>
								<td colspan="6"><input style="width: 100%;" type="text"	id="txtFullName" name="txtFullName" autocomplete="on" maxlength="100" /></td>
							</tr>
							<tr>
								<td>Description:</td>
							</tr>
							<tr>
								<td colspan="7"><textarea rows="2" id="txtDescription" name="txtDescription" style="width:100%"></textarea></td>
							</tr>
							<tr>
								<td style="white-space: nowrap;">Primary Contact:</td>
								<td><input type="text"	id="txtPrimaryContact" name="txtPrimaryContact" autocomplete="on" maxlength="100" /></td>
								<td align="right">Display Order:</td>
								<td colspan="1"><input type="number" id="txtDisplayOrder" value="10" name="txtDisplayOrder" autocomplete="on" /></td>
							</tr>
							<tr>
								<td style="white-space: nowrap;">Offline:</td>
								<td><input id="rbOfflineTrue" name="rbOffline" value="true" type="radio">Yes&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input id="rbOfflineFalse" name="rbOffline" value="false" checked type="radio">No</td>
							</tr>							
							<tr>
								<td>Configuration:</td><td colspan=3>
								<button class="btn btn-primary btn-sm" id="btnLoadDefault" onclick="return loadDefaultConfig()" >Load Template</button>&nbsp;&nbsp;
								<button class="btn btn-primary btn-sm" id="btnFormatCfg" onclick="return formatJSON()" >Format JSON</button>&nbsp;&nbsp;
								</td>
							</tr>
							<tr>
								<td colspan="7">	
									<div style="height: 200px;" id="editor">{&#10;}</div>
								</td>
							</tr>
							<tr>
								<td colspan=7>
                                    <button style="margin-right:20px;" class="btn btn-primary" id="btnManage"       onclick="return goManageDomains()" >Manage Domains</button>
                                    <button style="margin-right:20px;" class="btn btn-primary" id="btnCreateSubmit" onclick="return submitForm()">Submit</button>
                                    <button style="margin-right:20px;" class="btn btn-primary" id="btnCopyToNew"    onclick="return copyToNew()">Copy for New Domain</button>
									<button style="margin-right:20px;" class="btn btn-primary" id="btnInactivate"   onclick="return editStatus('inactive')">Inactivate</button>
									<button style="margin-right:20px;" class="btn btn-primary" id="btnArchive"      onclick="return editStatus('archive') ">Archive</button>
									<button style="margin-right:20px;" class="btn btn-primary" id="btnActivate"     onclick="return editStatus('activate') ">Activate</button>
								</td>
							</tr>
						</thead>
					</table>
				</div>
			</div>
	</div>
</div>
<div class="modal fade" tabindex="-1" role="dialog" id="myModal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
         <h4 class="modal-title" id ="myModalTitle">Status Changed</h4>
         <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
      </div>
      <div class="modal-body" id ="myModalDialogText">
        <p>Message&hellip;</p>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/domainAddEditPage.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/ace/ace.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	
<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>

</body>
</html>