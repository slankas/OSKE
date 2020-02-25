<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="edu.ncsu.las.model.collector.type.RoleType"%>
<%@page import="edu.ncsu.las.model.collector.Configuration"%>
<%@page import="edu.ncsu.las.model.collector.Domain"%>
<%@page import="edu.ncsu.las.model.collector.type.ConfigurationType"%>
<%@page import="edu.ncsu.las.collector.Collector"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%
boolean hasValidDomain = (request.getAttribute("givenUserId") != null && request.getAttribute("domain") != null &&  Collector.getTheCollecter().getDomain(request.getAttribute("domain").toString()) != null);
String currDomain="";
if (hasValidDomain) {
	 currDomain  = request.getAttribute("domain").toString();
%>
<input type="hidden" name="adjudicator" id="roleAdjudicator"	value="<%= ((edu.ncsu.las.model.collector.User) request.getAttribute("userRole")).hasAccess(request.getAttribute("domain").toString(), RoleType.ADJUDICATOR)%>" />
<input type="hidden" name="administrator" id="roleAdministrator"	value="<%= ((edu.ncsu.las.model.collector.User) request.getAttribute("userRole")).hasAccess(request.getAttribute("domain").toString(), RoleType.ADMINISTRATOR)%>" />
<%
} else { 
%>
<input type="hidden" name="adjudicator" id="roleAdjudicator" value="false" />
<input type="hidden" name="administrator" id="roleAdministrator" value="false" />
<%} %>
<input type="hidden" name="author" id="author"	value="<%=request.getAttribute("givenUserId")%>" />
<input type="hidden" name="domain" id="domain"	value="<%=request.getAttribute("domain")%>" />
<input type="hidden" name="applicationContextRoot" id="applicationContextRoot"	value="<%=pageContext.getAttribute("applicationRoot").toString()%>" />

<c:url var="applicationRoot"  value="/" />
<% 
   edu.ncsu.las.model.collector.User user = (edu.ncsu.las.model.collector.User) request.getAttribute("userRole");
   String proj2Name = pageContext.getAttribute("applicationRoot").toString();
   if (proj2Name.contains(";")) { proj2Name = proj2Name.substring(0,proj2Name.indexOf(";")); pageContext.setAttribute("domainName", proj2Name); }  

   Object homeAttribute = request.getSession(true).getAttribute("home");
   boolean showAnalystHome = ( homeAttribute == null || !homeAttribute.toString().equals("collector"));
%>
<% if (pageContext.getAttribute("displayHeader") == null || pageContext.getAttribute("displayHeader").equals(Boolean.TRUE)) { %>
<header class=headerStyle>
<div class="container-fluid">
	<div class="row">
		<div class="col-md-3">  
			<a class=headerLeftAbsoluteStyle href="${applicationRoot}" title="application home"><img  width="250" alt="Application Logo" src='${applicationRoot}<%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.APPLICATION_LOGO)%>'> </a> <!-- height="75" -->
		</div>
		<div class="col-md-6"><p>&nbsp;
		<%
		if (hasValidDomain && !request.getAttribute("domain").equals(Domain.DOMAIN_SYSTEM)) {
		%>
	    <h3 style="margin-bottom: 0rem; line-height:1;"><a href='${applicationRoot}<%=request.getAttribute("domain")%>'><%= Collector.getTheCollecter().getDomain(request.getAttribute("domain").toString()).getFullName()  %></a> </h3>
	    <%  if (showAnalystHome) { %>
	    <table><tr><td style="padding:0px">Project:</td>   <td style="padding: 0px 0px 0px 3px"><div id="headerCurrentProject"></div></td></tr>
	           <tr><td style="padding:0px">Scratchpad:</td><td style="padding: 0px 0px 0px 3px"><div id="headerCurrentScratchPad"></div></td></tr></table>
	    <p>&nbsp;
		<%	} else { %> 
	    	<p>&nbsp;<p>&nbsp;
	    <%    	
	        }
	    }
		else  {%>
			<h3>&nbsp;</h3>	    	<p>&nbsp;<p>&nbsp;
		<% } %>
		
		</div>
		<div class="headerRightAbsoluteStyle col-md-3">
			<div class="floatright">
			 <% if (user != null) { %>
				<div style="display:inline-block"><span class="welcomeField" id="welcomeField">Welcome, <%=request.getAttribute("givenName")%></span><br>
						<%
							if (hasValidDomain && !request.getAttribute("domain").equals(Domain.DOMAIN_SYSTEM)) {
							%>
							   <small>
							   <% if (showAnalystHome) { %> <a href="#" onclick="LASHeader.setDefaultHome('collector'); return false">Switch to Collector Home</a> <%} 
					           else { %> <a href="#" onclick="javascript:LASHeader.setDefaultHome('analyst'); return false">Switch to Analyst Home</a> <%} %>
							   </small><p>&nbsp;<br>
							   <a href="#" onclick="LASHeader.navigateTo('feedback'); return false">Feedback</a>
							   <% 
							   if (Configuration.getConfigurationPropertyAsBoolean(currDomain,ConfigurationType.HEADER_LINKS_UTILIZE)) {
								   JSONArray links = Configuration.getConfigurationPropertyAsArray(currDomain, ConfigurationType.HEADER_LINKS_OBJECTS);
								   for (int i=0; i < links.length(); i++) {
									   JSONObject o = links.getJSONObject(i);
									   %>
									   <br><a href='<%=o.getString("link")%>'><%=o.getString("displayText")%></a>
									   <% 
								   }
							   }
							   %>
							   
							<%} %>
							
			   </div>
				<div style="display: inline-block; position: relative;"></div><div id="topButtonBar" class="btn-group btn-group-sm" style="display: inline-block; position: absolute;  top: 5px;">
					<button type="button" class="btn btn-primary btn-sm" id="btLogOut" title="application logout"><%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.WEBAPP_AUTH_SIGNOUT_TEXT)%></button>

					
					<%
 					if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_REQUIRE_USERAGREEMENT) ) {
 						if (request.getAttribute("noHeaderButtons") == null) {
			 					%>
								<button type="button" class="btn btn-primary btn-sm" id="settings">
									<span class="fas fa-cog"></span>
								</button>
								<%
 						}
					}
					%>

				</div>
				<% } %>
			</div>

		</div>
	</div>
<% } %>	
<script>var currentPage=null </script>
<%
if (!pageContext.getAttribute("currentPage").toString().startsWith("system") && !pageContext.getAttribute("currentPage").toString().startsWith("noheader") && showAnalystHome  ) {

%>
<script>currentPage="<%=pageContext.getAttribute("currentPage")%>" </script>
	<div class="row">
		<div class="col-md-12">
			<ul class="nav nav-tabs">
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>			  <li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_dashboard">Dashboard</a> </li>                  <% } %>
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>			  <li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_plan">Plan</a> </li>                            <% } %>
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>			  <li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_discover">Discover</a> </li>                    <% } %>
			  <li class="nav-item dropdown">
			    <a class="nav-link dropdown-toggle" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" aria-expanded="false" id="topNav_manage">Manage</a>
			    <div class="dropdown-menu">
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>			      
       <a class="dropdown-item okMenu" href="#" id="topNav_concepts">Concepts</a>                                      
       <a class="dropdown-item okMenu" href="#" id="topNav_documentBuckets">Document Buckets</a>
       <a class="dropdown-item okMenu" href="#" id="topNav_docIndexes" >Document Indexes</a>
       <a class="dropdown-item okMenu" href="#" id="topNav_search"     >Holdings Search</a>
       <a class="dropdown-item okMenu" href="#" id="topNav_searchAlerts">Search Alerts</a>
       <a class="dropdown-item okMenu" href="#" id="topNav_structuralExtraction">Structural Extraction</a>
       <div class="dropdown-divider"></div>
       <a class="dropdown-item okMenu" href="#" id="topNav_jobs">Jobs</a> 
       <div class="dropdown-divider"></div>
	   <a class="dropdown-item okMenu" href="#" id="topNav_collector">Advanced Collector View</a>                      
<% } %>
			      
<% if (user.hasAccess(currDomain,RoleType.ADMINISTRATOR)) { %>   
      <% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %><div class="dropdown-divider"></div> <% } %>
                    <a class="dropdown-item okMenu" href="#" id="topNav_users">Users</a>                                            <% } %>
			      
			    </div>
			  </li>
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>			  <li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_analyze">Analyze</a></li>   <% } %>
<% if (user.hasAccess(currDomain,RoleType.ANALYST)) { %>			  <li class="nav-item"><a class="nav-link okMenu" href="#" id="topNav_document">Document</a></li> <% } %>
			</ul>
		</div>
	</div>	
	
	<% } %>
</div>	
</header>
<% if (Configuration.getConfigurationPropertyAsBoolean(Domain.DOMAIN_SYSTEM,ConfigurationType.COLLECTOR_REQUIRE_USERAGREEMENT) && request.getAttribute("userAgreementExpiration") != null) { %>
	<div class="row" style="text-align: center;">
		<div class="col-md-12" style="color:red; font-weight:bold;">
			Your OpenKE user agreement will expire at <%= request.getAttribute("userAgreementExpiration")%>. <a href='${projName}/system/userAgreementSign'>Submit a new user agreement form</a>.	
		</div>
	</div>  



<% } %>
<meta name="google-signin-client_id" content="<%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM, ConfigurationType.WEBAPP_AUTH_OAUTH_CLIENTID)%>"> 

<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/external/jquery-3.3.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/application/common.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASHeader.js?build=${build}"></script>
<script type="text/javascript" charset="utf8" src="${applicationRoot}resources/js/LASLogger.js?build=${build}"></script>

<script>
// invalidates current session (forces the authorization to be re-established) and sends the user the configured destination.  Need to set configuration item:
// TODO: can have the application return this instead.
function logout() {
	LASLogger.instrEvent('application.end');
	
    $.ajax({
		type : "DELETE",
		dataType: "json",
		url : openke.global.Common.getContextRoot()+"rest/ignoreDomainValue/user/session",
	    contentType: "application/json; charset=utf-8",
		success : function(data) {
			window.location = "<%=Configuration.getConfigurationProperty(Domain.DOMAIN_SYSTEM,ConfigurationType.WEBAPP_AUTH_SIGNOUT_URL)%>";
		},
		error : function(data) {
			//TODO
		},
	});	
};
</script>
