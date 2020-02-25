<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.citationsFilter"); %>
<% pageContext.setAttribute("displayHeader",Boolean.FALSE); %>


<title>Collector: Visualize Citations - Filter</title>
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/citationsVisualize.css" />
<style>
.card-body { padding: 0rem;}
.btn {margin-right: 4px;}
</style>
</head>
<body>
<%@include file="header.jsp"%><br>
<div class="text-nowrap container-fluid">
<div class="row">
&nbsp;
</div>

<div class="row">
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header"><strong>Author Keywords</strong></div>
	  <div class="card-body">
	    <select name="authorKeywordList" id="authorKeywordList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>MESH Keywords</strong>
	  </div>
	  <div class="card-body">
	    <select name="meshKeywordList" id="meshKeywordList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Journals</strong>
	  </div>
	  <div class="card-body">
	    <select name="journalList" id="journalList" form="filterForm" multiple size=10  style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div> 
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Countries</strong>
	  </div>
	  <div class="card-body">
	    <select name="countryList" id="countryList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>   
   <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Universities</strong>
	  </div>
	  <div class="card-body">
	    <select name="universityList" id="universityList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>    
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Authors</strong>
	  </div>
	  <div class="card-body">
	    <select name="authorList" id="authorList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>    
</div>

<div class="row">
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Vendors</strong>
	  </div>
	  <div class="card-body">
	    <select name="vendorList" id="vendorList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>Chemicals</strong>
	  </div>
	  <div class="card-body">
	    <select name="chemicalList" id="chemicalList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>CRISPR Actions</strong>
	  </div>
	  <div class="card-body">
	    <select name="actionList" id="actionList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>  
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>CRISPR Concepts</strong>
	  </div>
	  <div class="card-body">
	    <select name="conceptList" id="conceptList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>  
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>CRISPR Kits</strong>
	  </div>
	  <div class="card-body">
	    <select name="kitList" id="kitList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>  
  <div class="col-md-2">
	<div class="card card-default">
	  <div class="card-header">
	    <strong>CRISPR Things</strong>
	  </div>
	  <div class="card-body">
	    <select name="thingList" id="thingList" form="filterForm" multiple size=10 style="width:100%;max-width:100%;">
	    </select>
	  </div>
	</div>
  </div>      
</div>

<div class="row">
&nbsp;
</div>

<div class="row">
	<div class="form-group col-md-12">
		<div class='btn-toolbar pull-right'>
			<div class='btn-group'>
				<a><button class="btn btn-primary" id="btnSortAlpha">Sort Alphabetically</button></a>	
				<a><button class="btn btn-primary" id="btnSortFreq">Sort by Frequency</button></a>	
				<a><button class="btn btn-primary" id="btnClearfilters">Clear Filters</button></a>
				<a><button class="btn btn-primary" id="btnUpdatefilters">Update Filters</button></a>
				<a><button class="btn btn-primary" id="btnSearchByFilters">Search by Filters</button></a>					  
				<a><button class="btn btn-primary" id="btnApplyfilter">Apply Filter</button></a>	
			</div>
		</div>
	</div>
</div>
</div>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>
<script type="text/javascript" charset="utf8" src="https://gitcdn.github.io/bootstrap-toggle/2.2.2/js/bootstrap-toggle.min.js"></script>
  
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootstrap-paginator.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/countryTranslation.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/elasticSupport.js?build=${build}"></script>		
<script src="${applicationRoot}resources/js/application/citationsFilter.js"></script>
		

<form target="_blank" id='searchForm' name='searchForm' method='POST' action="${applicationRoot}${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>
</body>
</html>