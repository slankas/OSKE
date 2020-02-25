<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.addEditDocumentBuckets"); %>

<title>Document Bucket: Add/Edit</title>


<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
</head>
<body >
<%@include file="header.jsp"%><br>
<input type="hidden" name="documentBucketID" id="documentBucketID" value="<%=request.getAttribute("documentBucketID")%>" />
<input type=hidden id='allowUserSearch' name='allowUserSearch' value='<%=Configuration.getConfigurationPropertyAsBoolean(currDomain, ConfigurationType.LDAP_UTILIZE)%>' />
<div class="container-fluid">
        <div class="row">
            <div class="form-group col-md-12">
                <h3 id="lblCollectionType">New Document Bucket</h3>
                    <table>
                        <tr>
                            <td valign="top">Tag</td>
                            <td><input type="text" id="txtTag" name="txtTag" placeholder="Enter tag" size=95>
                                <label  id="err_label_name" style="color: red"> Please add a name.</label></td>
                        </tr>
                        <tr>
                            <td valign="top">Question</td>
                            <td><textarea style="width:100%" rows="3" id="txtQuestion" name="txtQuestion" placeholder="Enter question..."></textarea></td>
                        </tr>
                        <tr>
                            <td valign="top">Description</td>
                            <td><textarea style="width:100%" rows="3" id="txtDescription" name="txtDescription" placeholder="Enter description..."></textarea></td>
                        </tr>
                        <tr>
                            <td valign="top">Personal Notes</td>
                            <td><textarea  style="width:100%"  rows="3" id="txtPersonalNotes" name="txtPersonalNotes"  placeholder="Enter notes..."></textarea></td>
                        </tr>
                        <tr>
                            <td>Add Collaborator</td>
                            <td>
                                <table id="tblEmail">
                                    <tr>
                                        <td style="padding: 0px;"><input type="text" id="addParty" name="addParty" size=95 autocomplete="on" placeholder="Enter name or email to search.." /><br>
                                        <span class='grayout'>Manually enter a user with this format: FirstName LastName (email address)</span></td>
                                        <td align="right"><input type="button" id="btAddParticipant" value="Add" style="margin-left: 10px;"/></td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <td valign="top" id="tbl_label">Collaborators:</td>
                            <td>
                                <table class="table table-striped table-tight" id="tblData">
                                    <thead>
                                    </thead>
                                    <tbody>
                                    </tbody>
                                </table>
                            </td>
                        </tr>
                    </table>
            </div>
        </div>

        <div class="row">
            <div class="col-md-12">
                <div class="floatleft">
                    <button type="submit" class="btn btn-primary" id="btSubmit">Submit</button>&nbsp;
                    <button type="button" class="btn btn-primary" id="btDocumentBuckets">Document Buckets</button>
                    <button type="button" class="btn btn-primary  btn-md" id="btnDomainHome"><span class="fas fa-home"></span> Domain Home</button>
                </div>
            </div>
        </div>
</div>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/addEditDocumentBucket.js?build=${build}"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
    
    <!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>

    
</body>
</html>
