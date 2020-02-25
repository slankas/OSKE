<%@page import="java.util.Base64"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","domain.manage.search"); %>

<title>OpenKE: Search</title>

<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/css/elasticSearch/light_style.css" />
<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/css/external/jquery.datetimepicker.css" />
<!-- DataTable imports -->
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/DataTables-1.10.18/css/dataTables.bootstrap4.min.css"/>
	<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/dataTables/Buttons-1.5.2/css/buttons.bootstrap4.min.css"/>
<link rel="stylesheet" href="https://gitcdn.github.io/bootstrap-toggle/2.2.2/css/bootstrap-toggle.min.css">

<link rel="stylesheet" type="text/css"  href="${applicationRoot}resources/js/chosen/chosen.css">

<style>

.table-borderless > tbody > tr > td,
.table-borderless > tbody > tr > th,
.table-borderless > tfoot > tr > td,
.table-borderless > tfoot > tr > th,
.table-borderless > thead > tr > td,
.table-borderless > thead > tr > th {
    border: none;
}

table {
	margin-bottom: 0 !important;
}

</style>

</head>
<body style="height: 100%;">
<input type="hidden" name="searchURL" id="searchURL" value="<%=request.getAttribute("searchURL")%>" />
<input type="hidden" name="author" id="author"  value="<%=request.getAttribute("givenUserId")%>" />
<input type="hidden" name="queryObject" id="queryObject"  value='<%=request.getAttribute("queryObject") == null ? "": Base64.getEncoder().encodeToString(request.getAttribute("queryObject").toString().getBytes("UTF-8")) %>' />
<input type="hidden" name="currentPage" id="currentPage" value="<%=pageContext.getAttribute("currentPage")%>" />
    
<div class="overlay">
   <div id="loading-img"></div>
</div>

<%@include file="header.jsp"%><br>
<div class="container-fluid">
  <div >
     <div class="col-md-12 card" style="padding:0px">
       <div class="card-header"><h5>Query</h5></div>
       	
           <table class="" style="border-spacing:0px">
               <tr>
                   <td><label for="txtSearchQuery">Text:</label></td>
                   <td><input class="form-control" style="width: 100%;" type="text"  id="txtSearchQuery" name="txtSearchQuery" autocomplete="on" maxlength="100" value='<%=request.getAttribute("query")%>'/></td>
                   <td><select class="form-control" id="drpdnSearchType" name="drpdnSearchType">
                            <option value="keyword" selected="selected">Keyword</option>
                               <option value="regex">Regex</option>
                               <option value="exactPhrase">Exact Phrase</option>		                                        
                       </select>
                   </td>
                   <td><button class="btn btn-primary" id="btnSearch" onclick="elasticSearch()">Search</button></td>
               </tr>
			   <tr>
				   <td>Scope:</td>
				   <td><input type="radio" name="radScope" value="text" checked="checked"/>&nbsp;Text&nbsp;&nbsp;&nbsp;<input type="radio" name="radScope" value="_all" />&nbsp;Everything
				   </td>
			   </tr>
               <tr>
                   <td>Sort by:</td>
                   <td colspan=3><select class="form-control" id="sortField" name="sortField" style="width: auto; display: inline-block;">
                            <option value="crawled_dt">Crawled Timestamp</option>
                            <option value="published_date.date">Published Date</option>
                            <option value="relevance" selected="selected">Relevance</option>
                            <option value="text_length">Text Length</option>
                            <option value="html_title.keyword">Title (HTML)</option>			                                        
                       </select>&nbsp;&nbsp;
                       <input type="radio" name="sortOrder" value="asc">&nbsp;Ascending&nbsp;&nbsp;
                       <input type="radio" name="sortOrder" checked value="desc">&nbsp;Descending
                   </td>
              </tr>			                                                     
              <tr>
                  <td colspan=4>
                      <button class="btn btn-outline-primary btn-sm" id="ehFilter"><i class="fas fa-angle-right"></i>&nbsp;Filter</button>
                      	&nbsp;&nbsp;&nbsp;&nbsp;<small>(Note: Filters require an exact match)</small>
                      
                      <div class="expander-content" id="ecFilter">
                           <table>
                             <tr>
                               <td>Domain:</td>
                               <td><input class="form-control" type="text" placeholder="Ex: amazon.com" id="txtDomain" name="txtDomain" autocomplete="on" maxlength="100" /></td>
                               <td colspan=2><a href="javascript:clearSearchFilters();"> Clear filters</a></td>
                             </tr>
                             <tr>
                               <td>Crawl date from</td>
                               <td colspan="2">
                                 <input type="text" id="startTime" name="startTime" /> to
                                 <input type="text" id="endTime" name="endTime" />
                               </td>
                               <td>(use Zulu / GMT time)</td>
                             </tr>
                             <tr>
                               <td colspan="4">
                                            <div><b>Additional Filter Fields</b></div>
                                            <div>
				<div id="divSearchFields" style="width: 90%">
<table class="table table-condensed table-borderless" id="tblSearchFields">
	<thead><tr>
		<th style="width:250px;">Field</th>
	<th style="text-align: center;">Keyword?</th>
	<th>Search Type</th>
	<th>Search Term</th>
	<th>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
	<th></th>
</tr></thead>
<tr>
	<td><select id="drpdnSearchField" name="drpdnSearchField"
	data-placeholder="Choose a Country..." class="chosen-select" tabindex="2">
	  <option value="" selected="selected">Select Fields...</option>
                                      </select></td>
                                     <td style="text-align: center; vertical-align: middle;"><input type="checkbox" value="on" id="useKeyword" name="useKeyword"></td>
                                     <td><select class="form-control" id="drpdnSearchType" name="drpdnSearchType">
                                            <option value="term">Exact Match</option>
                                            <option value="fuzzy">Fuzzy Match</option>
	       <option value="prefix" selected>Starts with</option>
	       <option value="regexp">Regular Expression</option>
	       <option value="wildcard">Wildcard</option>
	       <option value="range_lt">&#60;</option>
	       <option value="range_lte">&#60;=</option>
	       <option value="range_gt">&#62;</option>
	       <option value="range_gte">&#62;=</option>
	       <option value="exists">exists</option>
	       <option value="not exists">not exists</option>
                                         </select>
                                     </td>
                                      
	<td><input class='form-control' data-min-width='200' type="text" id="txtSearchField" /></td>
	<td><select class="form-control" id="drpdnBooleanOprn" name="drpdnBooleanOprn">
                                                      <option value="and" selected="selected">And</option>
                                                      <option value="andnot">And Not</option>
                                                      <option value="or">Or</option>                                             
                                                    </select></td>
	<td><input class="btn btn-primary btn-sm" type="button"
		id="btnAddSearchField" value="Add"
		onclick="insertRow(this)" /></td>
							</tr>
						</table>
					</div>
				</div>
				<div><button class="btn btn-primary btn-sm" id="btAddConcept">Add Concept Filter</button></div>
                                         </td>
                                       </tr>
                              </table>
    </div>
                       </td>
                     </tr>
                      <tr>
                       <td colspan=3><label id="err_label" style="color: red"></label>
                </td>
              </tr>
      </table>
            </div>
  </div>
   <div class="row">&nbsp;</div>
  <div class="row">                      
			<div class="col-md-12 content-card">
				<div id="#restResponseAccordion" class="card-group">
					<div class="card card-default">
                         <div class="card-header">
<!--                          <strong><span id="searchResultsHeading">Search Results</span></strong> -->
                              <h5 style="display: inline-block" class="card-title" id="searchResultsHeading">Search Results</h5>
                              &emsp;
                              <a class="btn btn-outline-primary btn-sm" style="text-decoration: none" href='#' id="searchResultsExport" role="button">Export</a>
                              &emsp;
                              <a class="btn btn-outline-primary btn-sm" style="text-decoration: none" href="#" id="btCreateIndex" role="button">Create Index</a>
                              &emsp;
                              <a class="btn btn-outline-primary btn-sm" style="text-decoration: none" href='javascript:summarizeAllDocuments();' id="summarizeDocuments" role="button">Summarize All Documents</a>
                          </div>
						<div>
							<table id="tblSearchResults" class="searchcardResults .table" style="width: 99%;"></table>
						</div>
					</div>
                    
				</div>

			</div>
  </div>
  <div class="row" style="margin-top:10px;"><div class="col-md-12"><div id="pager"></div></div></div>
</div>
<%@include file="/WEB-INF/views/includes/MessageDialog.jsp"%>
<%@include file="/WEB-INF/views/includes/ExportDialog.jsp"%>

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
    
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootstrap-paginator.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	
    <%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>
    
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentBucket.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentBucketSupport.js?build=${build}"></script>	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASElasticSearch.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/ProjectDocument.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/group/DocumentIndex.js?build=${build}"></script>	

	
    <script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/chosen/chosen.jquery.js"></script>

	<!-- DataTable imports -->
<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
</body>
</html>