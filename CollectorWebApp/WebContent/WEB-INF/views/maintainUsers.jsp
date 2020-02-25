<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manager.users"); %>

<title>OpenKE: Maintain Users</title>
<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

<% 
%>
	
</head>
<body>
<%@include file="header.jsp"%><br>
<input type=hidden id='allowUserSearch' name='allowUserSearch' value='<%=Configuration.getConfigurationPropertyAsBoolean(currDomain, ConfigurationType.LDAP_UTILIZE)%>' />
<div class="container-fluid">
	<div class="row">
		<div class="form-group col-md-12">
			<div class="btn-group btn-group-sm" style="display: block;">
				<table id="tblAddUser">
					<thead>
						<tr>
							<th id="thdAddNewUsers" colspan="2"><h5>Add New Users</h5></th>
						</tr>
					</thead>
					<tr>
						<td>User</td>
						<td><input type="text" id="txtaddUser" name="txtaddUser" autocomplete="on" placeholder="Enter name or email to search.." size="50" maxlength="256" /><br>
						    <span class='grayout'><small>Manually enter a user with this format: FirstName LastName (email address)</small></span></td>
					</tr>
					
					<% if (request.getAttribute("domain").equals(Domain.DOMAIN_SYSTEM)) { %>
					                    <tr>
					                        <td>Domain</td>
											<td><select id="drpdn_domain" name="drpdn_domain">
					<%     for (Domain d: Collector.getTheCollecter().getDomainsSorted()) { %>
					                                <option value="<%=d.getDomainInstanceName()%>"><%=d.getFullName()%></option>
					<%     } %>
											</select></td>
										</tr>
					<% } %>
					
					<tr><td>&nbsp;</td></tr>
					
					<tr>
						<td>Role</td>
						<td><select id="drpdn_roles" name="drpdn_roles">
								<option value="administrator">Administrator</option>
								<option value="analyst">Analyst</option>
								<option value="adjudicator">Adjudicator</option>
						</select></td>
					</tr>
					<tr>
						<td></td>
						<td><input type="submit" class="btn btn-primary" id="btAddUser"	value="Add User" /></td>
					</tr>
				</table>
				<br> <label id="err_label_user" style="color: red">You must select a user.</label>
				<br> <br> 
				<h5>Current Users</h5>
				<input id="showAllUsers" type="checkbox"> Show All Users
				<table	class="display table table-striped table-bordered table-tight" id="tblData">
					<thead>
						<tr>
							<th id=thdEmail>Email</th>
							<th id=thdName>Name</th>
							<th id=thdRole>Role</th>
							<th id=thdStatus>Status</th>
							<th id=thdStatusDate>Status Date</th>
						</tr>
					</thead>
				</table>

			</div>
		</div>
	</div>
	
		<div class="row">
			<div class="col-md-12">
				<div class="btn-group floatleft">
					<button type="submit" class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span>&nbsp;&nbsp;Domain Home</button>			
				</div>
			</div>
		</div>	
	
</div>
	<div class="modal" id="updateRole">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<h4 class="modal-title">
						<span id="updateRoleTitle">Update Role</span>
					</h4>
					<button type="button" class="close" data-dismiss="modal">
						<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
					</button>
				</div>
				<div class="modal-body">
					<table>
						<tr>
							<td>Role</td>
							<td><select id="drpdn_update_roles"	name="drpdn_update_roles">
									<option value="administrator">Administrator</option>
									<option value="analyst">Analyst</option>
									<option value="adjudicator">Adjudicator</option>
							</select></td>
						</tr>
						<tr>
							<td></td>
							<td>
								<div class="btn-group btn-group-sm floatleft">
									<input type="submit" class="btn btn-primary" id="btUpdateRole"	value="Update Role" />
								</div>
							</td>
						</tr>
					</table>
				</div>
			</div>
			<!-- /.modal-content -->
		</div>
		<!-- /.modal-dialog -->
	</div>
	<!-- /.modal -->

	<div class="modal" id="updateStatus">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<h4 class="modal-title">
						<span id="updateStatusTitle">Update Status</span>
					</h4>
					<button type="button" class="close" data-dismiss="modal">
						<span aria-hidden="true">&times;</span><span class="sr-only">Close</span>
					</button>
				</div>
				<div class="modal-body">
					<table>
						<tr>
							<td>Status</td>
							<td><select id="drpdn_update_status" name="drpdn_update_status">
									<option value="active">active</option>
									<option value="inactive">inactive</option>
									<option value="removed">removed</option>
							</select></td>
						</tr>
						<tr>
							<td></td>
							<td>
								<div class="btn-group btn-group-sm floatleft">
									<input type="submit" class="btn btn-primary" id="btUpdateStatus" value="Update Status" />
								</div>
							</td>
						</tr>
					</table>
				</div>
			</div>
			<!-- /.modal-content -->
		</div>
		<!-- /.modal-dialog -->
	</div>
	<!-- /.modal -->

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/maintainUsers.js?build=${build}"></script>


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