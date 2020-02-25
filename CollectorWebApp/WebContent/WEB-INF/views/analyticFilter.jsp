<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<%
	pageContext.setAttribute("currentPage", "noheader.citationsFilter");
%>
<%
	pageContext.setAttribute("displayHeader", Boolean.FALSE);
%>


<title>Collector: Analysis - Filter</title>
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/css/analyticVisualize.css" />
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/js/chosen/chosen.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/jquery.datetimepicker.css" />
<style>
	.card-body {
		padding: 0rem;
	}
	
	.btn {
		margin-right: 4px;
	}
	
	.fa_custom {
		color: #357ebd;
	}
</style>
</head>
<body>
	<%@include file="header.jsp"%><br>
	<%@include file="includes/analyzeHeader.jsp"%>
	<div class="text-nowrap container-fluid analyticDisplay">
		<div class="row">&nbsp;</div>

		<div class="row padBottom">
			<div class="col-md-12 pull-right">
				
				<div class="input-group" style="width: 800px;">
					<label for="filterSelectEntity">Entity</label>
					<select id="filterSelectEntity" class="form-control input-sm select-dropdown" style="width: 150px" title="Add a filter card"></select>
					
					<label for="filterSelectConcept">Concept</label>
					<select id="filterSelectConcept" class="form-control input-sm select-dropdown" style="width: 150px" title="Add a filter card"></select>
					<!-- remove categories post symposium: 
					<label for="filterSelectCategory">Category</label>
					<select id="filterSelectCategory" class="form-control input-sm" style="width: 150px"></select>
					-->
				</div>
			</div>
		</div>
		<hr/>

		<div class="row" id='cardContainer'>
			<div class="col-md-2">
				<div class="card card-default padBottom">
					<div class="card-header">
						<strong class="float-left" title="People">People</strong>
						<a><button class="btn btn-danger close float-right" id="cardBtnEntityPERSON" name="PERSON" aria-label="Close">&times;</button></a>
					</div>
					<div class="card-body">
						<select class="selectcard" name="PERSON" id="PERSONEntityKeywordList" data-type="Entity" data-title="People" data-origname="PERSON" form="filterForm"
							multiple size=10 style="width: 100%; max-width: 100%;">
						</select>
					</div>
				</div>
			</div>
			<div class="col-md-2">
				<div class="card card-default padBottom">
					<div class="card-header">
						<strong class="float-left" title="Places">Places</strong>
						<a><button class="btn btn-danger close float-right" id="cardBtnEntityGPE" name="GPE"  aria-label="Close">&times;</button></a>
					</div>
					<div class="card-body">
						<select class="selectcard" name="GPE" id="GPEEntityKeywordList" data-type="Entity" data-origname="GPE"  data-title="Places" form="filterForm" multiple
							size=10 style="width: 100%; max-width: 100%;">
						</select>
					</div>
				</div>
			</div>
			<div class="col-md-2">
				<div class="card card-default padBottom">
					<div class="card-header">
						<strong class="float-left" title="Groups">Groups</strong>
						<a><button class="btn btn-danger close float-right" id="cardBtnEntityNORP" name="NORP" aria-label="Close">&times;</button></a>
					</div>
					<div class="card-body">
						<select class="selectcard" name="NORP" id="NORPEntityKeywordList" data-type="Entity" data-origname="NORP" data-title="Groups" form="filterForm"
							multiple size=10 style="width: 100%; max-width: 100%;">
						</select>
					</div>
				</div>
			</div>
			<div class="col-md-2">
				<div class="card card-default padBottom">
					<div class="card-header">
						<strong class="float-left" title="Organizations">Organizations</strong>
						<a><button class="btn btn-danger close float-right" id="cardBtnEntityORG" name="ORG" aria-label="Close">&times;</button></a>
					</div>
					<div class="card-body">
						<select class="selectcard" name="ORG" id="ORGEntityKeywordList" data-type="Entity" data-origname="ORG" data-title="Organizations" form="filterForm" multiple
							size=10 style="width: 100%; max-width: 100%;">
						</select>
					</div>
				</div>
			</div>
			<p id="namedEntitiesPlaceholder"></p>

		</div>

		<div class="row">&nbsp;</div>
		
		<fieldset class="form-group" style="border: 1px solid #ddd; margin-top: 1em; margin-bottom: 0em; padding: 0.5em; border-radius: 4px;">
		<legend style="width:auto; font-size:1.25rem; padding: 0px 5px;">Additional Filters</legend>

			<div class="row" id='filtersKeywords'>
				<div class="input-group col-sm-12">
					<div class="input-group-prepend">
						<span class="input-group-text" id="keyword1" style="background-color: #d7e6f4;">Text Keywords:</span>
					</div>
					<input type="text" id="filterInputKeywords" class="form-control" 
						placeholder="Enter keywords that must appear in the text, separate keywords with commas" 
						aria-label="Text Keywords:" aria-describedby="keyword1"
						style="width: 150px" 
						pattern="[a-zA-Z0-9\s]+(,[0-9a-zA-Z\s]+)*"
						title="Enter only letters and numbers separated by commas"
					/>
				</div>
			</div>
			
			<div class="row" id="filterSessions1">
				<div class="input-group col-sm-12">
					<input class="form-check-input" type="checkbox" value="session" 
					id="sessionCheck" 
					title="If unchecked, no discovery session data will be analyzed" 
					style="margin-top:10px; display:inline-block;" checked>
					&nbsp;&nbsp;&nbsp;&nbsp;
					<label for="sessionCheck" style="display:inline-block;">Include Discovery Sessions</label>
				</div>
			</div>
			
			<div class="row" id="filterSessions2">
				<div class="input-group col-sm-12">
					<label for="filterSelectSession" style="margin-left:0px;">Limit to Specific Discovery Sessions:</label>
					<select id="filterSelectSession" class="form-control session-dropdown chosen-select" data-placeholder="Select session(s)" multiple>
						<option selected>Choose...</option>
					</select>
				</div>
			</div>
			<div class="row" id="filterCrawlDates" style="margin-top: 10px;">
				<div class="col-sm-6">
					<div class="input-group datepicker">
						<label style="margin-left:0px;">Crawl Date from:</label>
						<input type='text' class='form-control' id='startTimePick' aria-label="zulu" aria-describedby="date1"> 
						<div class="input-group-append">
							<span class="input-group-text" id="date1" style="background-color: #d7e6f4;">Zulu/GMT</span>
						</div>
						&nbsp;&nbsp;<i class='far fa-calendar-alt fa_custom fa-2x'></i>
					</div>
				</div>
				<div class="col-sm-6">
					<div class="input-group datepicker">
						<label style="margin-left:0px;">Crawl Date to:&nbsp;&nbsp;</label>
	                    <input type="text" class='form-control' id='endTimePick' aria-label="zulu" aria-describedby="date2">
	                    <div class="input-group-append">
							<span class="input-group-text" id="date2" style="background-color: #d7e6f4;">Zulu/GMT</span>
						</div>
	                    &nbsp;&nbsp;<i class='far fa-calendar-alt fa_custom fa-2x'></i>
					</div>
				</div>
			</div>
			<div class="row">&nbsp;</div>
		
		</fieldset>
			
		<div class="row">&nbsp;</div>

		<div class="row">
			<div class="form-group col-md-12">
				<div class='btn-toolbar pull-left'>
					<div class='btn-group'>
						<a><button class="btn btn-primary" id="btnSortAlpha">Sort Alphabetically</button></a>
						<a><button class="btn btn-primary" id="btnSortFreq">Sort by Frequency</button></a>
						<a><button class="btn btn-primary" id="btnClearfilters">Clear Filters</button></a> 
						<a><button class="btn btn-primary" id="btnUpdatefilters">Update Filters</button></a> 
						<a><button class="btn btn-primary" id="btnSearchByFilters">Search by Filters</button></a> 
						<a><button class="btn btn-primary" id="btnApplyfilter">Apply Filter</button></a>
						<!-- <a><button class="btn btn-primary" id="btnSelectAllSessions">All Sessions</button></a>
						<a><button class="btn btn-primary" id="btnDeselectAllSessions">No Sessions</button></a> -->
					</div>
				</div>
			</div>
		</div>
	</div>
</body>

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.js"></script>

	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery.datetimepicker.full.min.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootstrap-paginator.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASHeader.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/countryTranslation.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/elasticSupport.js?build=${build}"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/Analytics.js?build=${build}"></script>
	<script	type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analyticFilter.js"></script>
	
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/chosen/chosen.jquery.js"></script>
	<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/momentjs.2.22.2/moment.js"></script>
</html>