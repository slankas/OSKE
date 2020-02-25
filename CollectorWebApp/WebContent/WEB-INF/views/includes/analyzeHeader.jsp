<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="edu.ncsu.las.model.collector.type.RoleType"%>
<%@page import="edu.ncsu.las.model.collector.Configuration"%>
<%@page import="edu.ncsu.las.model.collector.Domain"%>
<%@page import="edu.ncsu.las.model.collector.type.ConfigurationType"%>
<%@page import="edu.ncsu.las.collector.Collector"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>


<% 
   boolean hasValidDomainAH = (request.getAttribute("givenUserId") != null && request.getAttribute("domain") != null &&  Collector.getTheCollecter().getDomain(request.getAttribute("domain").toString()) != null);
   String currDomainAH="";
   if (hasValidDomainAH) {  currDomainAH  = request.getAttribute("domain").toString(); }
   edu.ncsu.las.model.collector.User userAnalyzeHeader = (edu.ncsu.las.model.collector.User) request.getAttribute("userRole");
   if (userAnalyzeHeader.hasAccess(currDomainAH,RoleType.ANALYST) && pageContext.getAttribute("currentPage").toString().startsWith("domain.analyze")) { %>
    <div class="container-fluid">
	    <div class="row">
			<div class="col-md-12">
				<ul class="nav navbar navbar-expand-lg navbar-light bg-light mr-auto" style="border-left: #dee2e6 1px solid; border-right: #dee2e6 1px solid;">  
					<li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_analyticfrequencies" style ="padding-left: 0rem;">Frequencies</a></li> 
					<li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_analyticvisualize">Geospatial</a></li>
					<li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_analyticheatMap">Heatmap</a></li>
					<li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_analyticheatMapTimeline">Heatmap Timeline</a></li>
					<li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_analyticChoroplethMap">Choropleth</a></li>
				</ul>
			</div>
		</div>
	    <div class="row">
	      <div class="col-md-12">
			<div class="nav navbar navbar-expand-lg"  style="border: #dee2e6 1px solid;">  
		        <div class="nav navbar-nav mr-auto" id="hdrFilterContents"></div>
				<div class="nav navbar-nav navbar-right justify-content-end" id="hdrFilterActions">
				     <button id='btnClearFilters' class="btn btn-primary btn-sm">Clear</button>&nbsp;&nbsp;
				     <button id='btnFilters' class="btn btn-primary btn-sm">Filters</button>
	        	</div>		
			</div> 
		  </div>
		</div>	
	</div>
<% }
%>		
<form target=_blank id='analyzeSearchForm' name='analyzeSearchForm' method='POST' action="${applicationRoot}${domain}/search"><input type='hidden' name='searchQuery' id='searchQuery' value='' /></form>	
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/view/analyze.js?build=${build}"></script>
