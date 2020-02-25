<%@page import="edu.ncsu.las.model.collector.type.SourceParameter"%>
<%@page import="edu.ncsu.las.source.SourceHandlerInterface"%>
<%@include file="/WEB-INF/views/includes/standardPageStart.jsp"%>
<% pageContext.setAttribute("currentPage","noheader.jobParameters"); %>

<title>Jobs</title>
</head>
<body>
<%
    String name = request.getParameter("name");
	if (name == null) { name = ""; }
    SourceHandlerInterface shi = edu.ncsu.las.collector.Collector.getTheCollecter().getSourceHandler(name);
    if (shi != null) {
%>
<p>&nbsp;
<h4>&nbsp;&nbsp;<%= shi.getSourceHandlerDisplayName() %> Parameters</h4>
<p>
<table class="display table table-striped table-bordered table-tight" style="width:90%; margin:auto"	id="tblSrcParameters">
	<thead>
		<tr>
			<th id="thdName">Name</th>
			<th id="thdDescription">Description</th>
			<th id="thdRequired">Required</th>
			<th id="thdExample">Example</th>
		</tr>
	</thead>
	<tbody>
<%
    	java.util.TreeMap<String, SourceParameter> params = shi.getConfigurationParameters();
    	java.util.ArrayList<String> keys = new java.util.ArrayList<String>(params.keySet());
    	java.util.Collections.sort(keys);
    	for (String key: keys) {
    		SourceParameter sp = params.get(key);
 %>
            <tr><td><%=sp.getName() %></td><td><%=sp.getDescription() %></td><td><%=sp.isRequired() %></td><td><%=sp.getExample() %></td></tr>
<%
    	}
%>
	</tbody>
</table>
<%    	
    }
    else {
%>
    	No Such Handler!
<%
    }

%>	

<p>&nbsp;<p>
<button class="btn btn-primary btn-sm" style="position: absolute;  right: 20px;" id="btnLoadDefault" onclick="self.close()" >Close</button>

</body>
</html>