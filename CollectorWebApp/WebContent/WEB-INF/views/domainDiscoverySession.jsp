<%@page import="edu.ncsu.las.source.SourceHandlerInterface"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<%
	pageContext.setAttribute("currentPage", "domain.discover.session");
%>
<title>Domain Discovery: Session</title>

<!--  cron editing -->
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/css/external/jquery-cron.css" />
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/css/external/jquery-gentleSelect.css" />

<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/css/domainDiscovery.css?build=${build}" />
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/css/viewIndex.css?build=${build}" />
<link rel="stylesheet" type="text/css" href="${applicationRoot}resources/css/external/split.css" />
<link rel="stylesheet" href="${applicationRoot}resources/jqTree.1.3.8/jqtree.css">

<!-- DataTable imports used - by popup windows -->
<link rel="stylesheet" href="${applicationRoot}resources/css/mytable.css">
<link rel="stylesheet" href="${applicationRoot}resources/css/pagination.css">
</head>
<body>
	<div class="overlay"> <div id="loading-img"></div></div>
	<%@include file="header.jsp"%><br>
	<input type="hidden" name="sessionUUID" id="sessionUUID" value="<%=request.getAttribute("sessionUUID")%>" />
	<input type="hidden" name="passedSearchTerms" id="passedSearchTerms" value="<%=request.getAttribute("searchTerms")%>" />
	<input type="hidden" name="passedSearchSource" id="passedSearchSource" value="<%=request.getAttribute("searchSource")%>" />
	<input type="hidden" name="currentPage" id="currentPage" value="<%=pageContext.getAttribute("currentPage")%>" />
		
	<input type="hidden" name="wordnetHolder" id="wordnetHolder" value="" />
	<input type="hidden" name="wordnetHolderKey" id="wordnetHolderKey" value="" />
	
	<input type="hidden" name="nlexHolder" id="nlexHolder" value="" />
	<input type="hidden" name="nlexHolderKey" id="nlexHolderKey" value="" />

	<!-- fields are used to determine whether or not to expose this functionality -->
	<input type="hidden" name="translateSupport" id="translateSupport" value="<%=Configuration.getConfigurationObject(request.getAttribute("domain").toString(), ConfigurationType.AWS)!=null %>" />
	<input type="hidden" name="nlExpansionSupport" id="nlExpansionSupport" value="<%=Configuration.getConfigurationObject(request.getAttribute("domain").toString(), ConfigurationType.NLEXPANSION)!=null %>" />


	<div class="container-fluid">

		<h4>
			<span id="sessionList" class="fas fa-list sessionList"></span> Domain Discovery Session
			<span id="titleSessionName"></span>
		</h4>
		<div class="row">
			<div class="col-md-12">
				<div class="card card-default sessionExists">
					<div class="card-header col-md-12">
						<strong>Search History:</strong>&nbsp;<a class="pointer" id="btPriorExecutionsStatisticsCSV" href='#'>Report</a>
						<div class="btn-group btn-group-sm" role="group" id="executionList" aria-label=""></div>
						<button type="button" class="btn btn-outline-primary pull-right" id="analyzeButton"><span class="far fa-chart-bar"></span>&nbsp;Analyze</button>
						<button type="button" class="btn btn-outline-primary pull-right" id="exportAllButton" title="Export all session searches for transfer">Export All</button>
						
						<div id="discoveryIndex">
							<strong>Search Index:</strong>&nbsp;
								<a class="pointer" id="btCreateIndex" href='#'>Create</a> &nbsp; 
								<a class="pointer" id="btShowIndex" href='#'>View</a>
						</div>
					</div>
				</div>
			</div>
		</div>
		<div class="row">&nbsp;</div>
		<div class="row">
			<div class="col-md-12">
				<div class="card card-default">
					<div class="card-header">
						<h5 style="display: inline-block" class="card-title">Query</h5>
					</div>
					<div class="card-body">

						<form class="form-horizontal" role="form">
							<div class="form-group row">
								<label class="control-label col-md-1" for="sessionName">Session Name:</label>
								<div class="col-md-4">
									<input type="text" class="form-control" id="sessionName" placeholder="Enter Session Name">
								</div>
							</div>
							<div class="form-group row noFileUse">
								<label class="control-label col-md-1" for="searchTerms" id="searchTermsLabel">Search Terms:</label>
								<div class="col-md-4">
									<input type="text" class="form-control" id="searchTerms" placeholder="Enter Search Terms">
								</div>
								<div class="btn-group">
									<button type="button" class="btn btn-primary" id="searchButton">
										<span class="fas fa-search"></span>&nbsp;Search
									</button>
									<button type="button" class="btn btn-danger" id="stopButton">
										<span class="fas fa-stop-circle"></span>&nbsp;Stop
									</button>
									
									<button type="button" class="btn btn-outline-secondary" id="translateTermsButton">
										<span class="fas fa-language"></span>&nbsp;Translate Search Terms
									</button>
									
									<div class="dropdown">
										<button type="button" class="btn dropdown-toggle" id="relatedButton" data-toggle="dropdown"
												aria-haspopup="true" aria-expanded="false" style="height: 100%">
											<span class="fas fa-question-circle"></span>&nbsp;Related Searches
										</button>
										<div class="dropdown-menu" aria-labelledby="relatedButton">
											<a class="dropdown-item" id="relatedGoogle" href="#">Related Google Searches</a>
											<a class="dropdown-item" id="relatedDictionaryExpansion" href="#">Expand Search Words</a>
											<a class="dropdown-item" id="relatedWordsGeneral" href="#">Generalized Related Words</a>
											<a class="dropdown-item" id="relatedWordsSpecific" href="#">Specialized Related Words</a>
											<a class="dropdown-item nlExpansionMenuItem" id="nlexSemantic"  href="#">NL Expansion Semantic</a>
											<a class="dropdown-item nlExpansionMenuItem" id="nlexSyntactic" href="#">NL Expansion Syntactic</a>
										</div>
									</div>
									
									<button type="button" data-toggle="collapse" data-target="#settings" class ="btn"><span class="fas fa-cog"></span>&nbsp;Advanced</button>
									<div class="dropdown">
										<button class="btn dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown"
											aria-haspopup="true" aria-expanded="false" style="height: 100%">Create
										</button>
										<div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
											<a class="dropdown-item" id="createJobButton" href="#">Job</a>
											<a class="dropdown-item" id="createSearchAlert" href="#">Search Alert</a>
										</div>
									</div>
								</div>
							</div>
								
							<div id="transRow" class="form-group row d-none" >	
								<label class="control-label col-md-1" for="searchTermsTranslate" id="searchTermsTranslateLabel">Translated Terms:</label>
								<div class="col-md-4">
									<input type="text" class="form-control" id="searchTermsTranslate" placeholder="Translated Search Terms">
								</div>
							</div>	
							
							<div class="form-group row noFileUse noURLUse">
								<label class="control-label col-md-1" >Search in:</label>
									<div class="col-md-1">
										<select id="targetLanguage" class="form-control form-control-sm" disabled>
											<option value="none" selected="selected">(Optional)</option>
											<option value="af">Afrikaans</option>
											<option value="sq">Albanian</option>
											<option value="am">Amharic</option>
											<option value="ar">Arabic</option>
											<option value="az">Azerbaijani</option>
											<option value="bn">Bengali</option>
											<option value="bs">Bosnian</option>
											<option value="bg">Bulgarian</option>
											<option value="zh">Chinese (Simplified)</option>
											<option value="zh-TW">Chinese (Traditional)</option>
											<option value="hr">Croatian</option>
											<option value="cs">Czech</option>
											<option value="da">Danish</option>
											<option value="fa-AF">Dari</option>
											<option value="nl">Dutch</option>
											<option value="en">English</option>
											<option value="et">Estonian</option>
											<option value="fi">Finnish</option>
											<option value="fr">French</option>
											<option value="fr-CA">French (Canadian)</option>
											<option value="ka">Georgian</option>
											<option value="de">German</option>
											<option value="el">Greek</option>
											<option value="ha">Hausa</option>
											<option value="he">Hebrew</option>
											<option value="hi">Hindi</option>
											<option value="hu">Hungarian</option>
											<option value="id">Indonesian</option>
											<option value="it">Italian</option>
											<option value="ja">Japanese</option>
											<option value="ko">Korean</option>
											<option value="lv">Latvian</option>
											<option value="ms">Malay</option>
											<option value="no">Norwegian</option>
											<option value="fa">Persian (Farsi)</option>
											<option value="ps">Pashto</option>
											<option value="pl">Polish</option>
											<option value="pt">Portuguese</option>
											<option value="ro">Romanian</option>
											<option value="ru">Russian</option>
											<option value="sr">Serbian</option>
											<option value="sk">Slovak</option>
											<option value="sl">Slovenian</option>
											<option value="so">Somali</option>
											<option value="es">Spanish</option>
											<option value="sw">Swahili</option>
											<option value="sv">Swedish</option>
											<option value="tl">Tagalog</option>
											<option value="ta">Tamil</option>
											<option value="th">Thai</option>
											<option value="tr">Turkish</option>
											<option value="uk">Ukrainian</option>
											<option value="ur">Urdu</option>
											<option value="vi">Vietnamese</option>

										</select>
									</div>
									<div class="input-group col-md-2" style="padding-left: 1px;" id="enableForeignLanguageSearch">
										<input class="form-check-input" type="checkbox" value="translate" 
										id="translateCheck" 
										title="Optional. Search in selected language." 
										style="margin-top:6px; display:inline-block;" >
										&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
										<label for="translateCheck" style="display:inline-block;">Enable foreign language search</label>
									</div>
							</div>

							<div class="form-group row noFileUse noURLUse">
								<label class="control-label col-md-1">Number of Search Results:</label>
								<div class="col-md-1">
									<input type="text" class="form-control" id="numberOfsearchResults" value="20">
								</div>
							</div>
							<div class="form-group row">
								<label class="control-label col-md-1">Method:</label>
								<div class="col-md-1">
									<select id="searchAPI" class="form-control">
										<option value="" selected="selected">Select Handler...</option>
										<%
											for (SourceHandlerInterface shi : Collector.getTheCollecter().getSourceHandlersForDomainDiscovery(domain)) {
										%>
										<option value="<%=shi.getSourceHandlerName()%>"
											data-primarylabel="<%=shi.getPrimaryLabel().toString()%>"
											data-primaryHidden="<%=shi.getPrimaryLabel().isHidden()%>"
											data-maxresults="<%=shi.getMaximumNumberOfSearchResults()%>"
											data-default="<%=shi.isDefaultDomainDiscoverySearchMethod()%>"
											data-supportsjob="<%=shi.supportsJob()%>"><%=shi.getSourceHandlerDisplayName()%>
										</option>
										<%
											}
										%>
										<option value="file" 
											data-primarylabel="file" 
											data-primaryHidden="false" 
											data-maxresults="1" 
											data-default="false">Upload File
										</option>
										<option value="weblist" 
											data-primarylabel="weblist" 
											data-primaryHidden="false" 
											data-maxresults="1" 
											data-default="false">URL List
										</option>
									</select>
								</div>
							</div>
							<div class="form-group row fileUse">
								<label class="control-label col-md-1">File to Upload:</label>
								<div class="col-md-1">
									<input id="fileupload" type="file" name="fileUpload">
								</div>
							</div>
							<div class="form-group row urlListUse">
								<label class="control-label col-md-1" for="urlList">URLs:</label>
								<div class="col-md-4">
									<textarea class="form-control" id="urlList" placeholder="Enter one URL per line ..."></textarea>
								</div>
								<div>
									<div class="btn-group">
										<button type="button" class="btn btn-primary" id="urlSearchButton">
											<span class="fas fa-search"></span>&nbsp;Search
										</button>
										<button type="button" class="btn btn-danger" id="urlStopButton">
											<span class="fas fa-stop-circle"></span>&nbsp;Stop
										</button>
										<button type="button" class="btn btn-primary" id="urlCreateJobButton">
											<span class="fas fa-plus-circle"></span>&nbsp;Create Job
										</button>
									</div>
								</div>
							</div>
							<div id="settings" class="collapse">
								<div class="form-group row noFileUse noURLUse">
									<label class="control-label col-md-1" for="advConfig">Advanced Configuration:</label>
									<div class="col-md-4">
										<textarea class="form-control" id="advConfig">{}</textarea>
									</div>
								</div>
							</div>
						</form>

					</div>
				</div>
			</div>
		</div>
		<div class="row">&nbsp;</div>
		<div class="row">
			<div class="col-md-12">
				<div class="card card-default" id="searchResultscard">
					<div class="card-header">
						<h5 style="display: inline-block" class="card-title" id="searchResultsHeading">Search Results</h5>
						
						&emsp; 
						<a class="btn btn-outline-primary btn-sm crawlComplete pointer" style="text-decoration: none" id="searchResultsExport" href='#'>Export Results</a> 
						&emsp; 
						<a class="btn btn-outline-primary btn-sm crawlComplete pointer" style="text-decoration: none" id="searchResultStatistics" href='#'>Result Statistics</a>
						&emsp; 
						<a class="btn btn-outline-primary btn-sm crawlComplete pointer" style="text-decoration: none" id="switchToListView" href='#'>Switch to List View</a> 
						<a class="btn btn-outline-primary btn-sm crawlComplete pointer" style="text-decoration: none" id="switchToIndexView" href='#'>Switch to Index View</a>
						
						
					</div>
					<div class="card-body">
						<!--  	<div class="row">
						<input type="checkbox" checked data-toggle="toggle" data-on="R" data-off="NR" data-onstyle="success" data-offstyle="danger" data-size="mini">
						
					</div>	-->

						<div id="searchResults">
							<div class="row" id="overallStatus"></div>
							<div class="row" id="relevenceLegend">
								<small> 
									<span class="relevant far fa-thumbs-up"></span>Relevant&nbsp;&nbsp; 
									<span class="unknownrelevant far fa-hand-point-right"></span>Unassigned relevance&nbsp;&nbsp; 
									<span class="notrelevant far fa-thumbs-down"></span> Not relevant
								</small>
							</div>
							<div class="row">&nbsp;</div>
							<div class="row">
								<div id="resultTableDiv" class="col-md-8 col-md-offset-0">
									<table id="results" style="width: 100%">
										<thead></thead>
										<tbody></tbody>
									</table>
								</div>
								<div id="topicModelling"
									class="pull-right col-md-4 card crawlComplete"
									style="padding: 0px">
									<div class="card-header">
										<h4>Topic Modeling</h4>
										<form role="form" id="topicControls">
											<table>
												<tr valign="top">
													<td>Number of Topics:</td>
													<td>
														<input type="text" class="form-control" id="numTopics"
															style="width: 30px; margin-top: 0px; line-height: 10px; display: inline-block" value="7">
													</td>
													<td>Stem Words:</td>
													<td>
														<input type="checkbox" class="form-control" id="stemWordFlag" checked style="margin-top: 0px; width: 20px">
													</td>
													<td>Include Documents:</td>
													<td>
														<div style="display: list-item; list-style-type: none;">
															<input type="checkbox" class="form-check-input"
																id="relevantFlag" checked
																style="margin-top: 0px; width: 20px"><span
																style="margin-left: 20px;"
																class="relevant far fa-thumbs-up"></span>
														</div>
														<div style="display: list-item; list-style-type: none;">
															<input type="checkbox" class="form-check-input"
																id="unkrelevantFlag" checked
																style="margin-top: 0px; width: 20px"><span
																style="margin-left: 20px;"
																class="unknownrelevant far fa-hand-point-right"></span>
														</div>
														<div style="display: list-item; list-style-type: none;">
															<input type="checkbox" class="form-check-input"
																id="notrelevantFlag"
																style="margin-top: 0px; width: 20px"><span
																style="margin-left: 20px;"
																class="notrelevant far fa-thumbs-down"></span>
														</div>
													</td>
													<td>
														<button class="btn btn-primary btn-sm" id="btTopics">Generate</button>
													</td>
												</tr>
											</table>
										</form>

										<div id="topics">
											<br>
											<p></p>
										</div>
									</div>
								</div>
							</div>
						</div>
						<%@include file="includes/DocumentIndexView.jsp"%><br>
					</div>
				</div>
			</div>
		</div>

		<div class="row">
			<div class="col-md-12">
				<br>
				<button type="button" class="btn btn-outline-primary  btn-md sessionList" id="btDomainDiscovery">
					<span class="fas fa-list"></span> Discovery Sessions
				</button>
				<button type="button" class="btn btn-outline-primary  btn-md" id="btHome">
					<span class="fas fa-home"></span> Domain Home
				</button>
			</div>
		</div>
	</div>

	<div id="myTest"></div>


	<form name='summarize' id='summarizeForm'
		action='${applicationRoot}<%=domain%>/summary' method=post
		target=_blank>
		<input type=hidden name='summaryText' value='' id='summarizeTextField' />
	</form>


	<%@include file="/WEB-INF/views/includes/MessageDialog.jsp"%>
	<%@include file="/WEB-INF/views/includes/ExportDialog.jsp"%>




</body>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-ui-1.12.1.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/bootstrap-4.1.2/js/bootstrap.bundle.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/bootbox.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.js"></script>
<script src="${applicationRoot}resources/js/fileUpload/js/vendor/jquery.ui.widget.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/jqTree.1.3.8/tree.jquery.js"></script>

<!-- DataTable imports -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/DataTables-1.10.18/js/dataTables.bootstrap4.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/dataTables/Buttons-1.5.2/js/dataTables.buttons.min.js"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/fileUpload/js/jquery.fileupload.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASExport.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASTextAnalysis.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentBucket.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentBucketSupport.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/DocumentIndex.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/ProjectDocument.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/documentIndexView.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/domainDiscoverySession_summaryReport.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/model/Analytics.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/elasticSupport.js?build=${build}"></script>

<%@include file="/WEB-INF/views/includes/DocumentAnalytics.jsp"%>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/analytics/group/DocumentIndex.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/domainDiscoverySession.js?build=${build}"></script>

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/porter.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/split.js"></script>

<!--  Used to make cron job entries appear human readable: prettyCron.toString("37 10 * * * *");  -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/cronstrue.js"></script>

<!-- Edit cron:  see http://shawnchin.github.io/jquery-cron/ -->
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-gentleSelect.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-cron.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/elasticSearch/jsontree.min.js"></script>

</html>