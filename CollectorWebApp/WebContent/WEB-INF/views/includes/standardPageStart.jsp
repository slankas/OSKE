<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:url var="applicationRoot"  value="/" />
<% String projName = pageContext.getAttribute("applicationRoot").toString();
   if (projName.contains(";")) { projName = projName.substring(0,projName.indexOf(";")); pageContext.setAttribute("projName", projName); }
   pageContext.setAttribute("build",edu.ncsu.las.servlet.SystemInitServlet.getWebApplicationBuildTimestamp(getServletContext()));   
   edu.ncsu.las.model.collector.User u = (edu.ncsu.las.model.collector.User) request.getAttribute("userRole"); 
   String domain = request.getAttribute("domain") == null ? null : request.getAttribute("domain").toString(); %>

<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<LINK rel="SHORTCUT ICON" href="${applicationRoot}resources/images/LAS_Logo.ico">
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/bootstrap-4.1.2/css/bootstrap.min.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/fontawesome-free-5.1.1-web/css/all.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/nouislider.9.2.0/nouislider.css" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/snackbar-polonel-0.1.11/snackbar.css?build=${build}" />
<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/external/jquery-ui-1.12.1.css">


<link rel="stylesheet" type="text/css"	href="${applicationRoot}resources/css/demonstrator.css?build=${build}" />
<c:url var="applicationRoot"  value="/" />

