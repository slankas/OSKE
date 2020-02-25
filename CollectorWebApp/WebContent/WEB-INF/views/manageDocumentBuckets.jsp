<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.documentBuckets"); %>

<title>OpenKE: Manage Document Buckets</title>

<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/css/elasticSearch/light_style.css" />

<!-- DataTable imports -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">

<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/js/chosen/chosen.css">

</head>
<body>
<%@include file="header.jsp"%>
<div class="container-fluid">
	<input type="hidden" name="documentBucketID"  id="documentBucketID"  value="<%=request.getAttribute("documentBucketID")%>" />
	<input type="hidden" name="documentBucketTag" id="documentBucketTag" value="<%=request.getAttribute("documentBucketTag")%>" />
	<div class="row">
			<div class="form-group col-md-12" style="display: block;">
            <table class="display table table-striped table-bordered table-tight" >
                <thead>
                 <tr>
                     <th id="thdMyCollections" colspan="5">
                     <h5 style="display: inline-block" class="card-title">Document Buckets</h5>
                     </th>
                 </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>
		         <table class="display table table-striped table-bordered table-tight" id="tblBuckets">
					<thead>
                        <tr>
                            <th id=thdBucketTag>Tag</th>
                            <th id=thdBucketQuestion>Question</th>
                            <th id=thdStatus>Owner</th>
                            <th id=thdStartTime>Description</th>
                            <th id=thdEndTime>Date Created</th>
                            <th id=thdProcessingTime>Actions</th>
                        </tr>
					</thead>
				</table>
				</td>
				</tr>
				</tbody>
				</table>

				<button type="button" class="btn btn-primary" style="margin-bottom: 20px;" id=btCreateDocumentBucket onClick="createDocumentBucket()">Create</button>
                			
				<div class="content-card" id="collectDocumentcard"><a id="searchResultsAnchor"></a>
				<h4 id="searchResultsTitle">Bucket Documents</h4>
                <div id="#restResponseAccordion" class="card-group">
                    <div class="card card-default">
                         <div class="card-header">
                              <h4 style="display: inline-block" class="card-title" id="searchResultsHeading"></h4>
                              &emsp;
                              <a style="display: inline-block" href='javascript:LASExportDialog.openExportDialog();' id="searchResultsExport">Export</a>
                              &emsp;
                              <a style="display: inline-block" href='' id="btCreateIndex">Create Index</a>  
                              &emsp;
                              <a style="display: inline-block" href='' id="btShowIndex">View Index</a> 
                              
                          </div>
                        <div >
                            <div id="tblSearchResults" >
                            </div>
                        </div>
                    </div>
                    
                </div>
			   <div id="pager"></div>
            </div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-12">
			<div class="btn-group floatleft">
				<button type="submit" class="btn btn-primary" id="btnDomainHome"><span class="fas fa-home"></span>&nbsp;Domain Home</button>			
			</div>
		</div>
	</div>	
		
</div>
    
<%@include file="/WEB-INF/views/includes/MessageDialog.jsp"%>
<%@include file="/WEB-INF/views/includes/ExportDialog.jsp"%>    
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentBucket.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentBucketSupport.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/manageDocumentBuckets.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentBucket.js?build=${build}"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootstrap-paginator.js"></script>
	
	<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/group/DocumentIndex.js?build=${build}"></script>	
	
<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>	
</body>
</html>