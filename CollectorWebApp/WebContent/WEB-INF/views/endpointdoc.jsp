<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Endpoint Documentation</title>
</head>
<body>
  <div class="container">
    <h1>Spring MVC 3.1 Demo Endpoints</h1>
    <c:forEach items="${handlerMethods}" var="entry">
      <div>
        <hr>
        <p><strong>${entry.value}</strong></p>      
      </div>
      <div class="span-3 colborder">
        <p>
          <span class="alt">Patterns:</span><br> 
          <c:if test="${not empty entry.key.patternsCondition.patterns}">
            ${entry.key.patternsCondition.patterns}
          </c:if>
        </p>
      </div>
      <div class="span-3 colborder">
        <p>
          <span class="alt">Request Methods:</span><br>
          <c:if test="${not empty entry.key.methodsCondition.methods}">
            ${entry.key.methodsCondition.methods}
          </c:if>
        </p>
      </div>
      <div class="span-3 colborder">
        <p>
          <span class="alt">Headers:</span><br>
          <c:if test="${not empty entry.key.headersCondition.expressions}">
            ${entry.key.headersCondition.expressions}
          </c:if>
        </p>
      </div>
      <div class="span-3 colborder">
        <p>
          <span class="alt">Parameters:</span><br>
          <c:if test="${not empty entry.key.paramsCondition.expressions}">
            ${entry.key.paramsCondition.expressions}
          </c:if>
        </p>
      </div>
      <div class="span-3 colborder">
        <p>
          <span class="alt">Consumes:</span><br>
          <c:if test="${not empty entry.key.consumesCondition.expressions}">
            ${entry.key.consumesCondition.expressions}
          </c:if>
        </p>
      </div>
      <div class="span-3 last">
        <p>
          <span class="alt">Produces:</span><br>
          <c:if test="${not empty entry.key.producesCondition.expressions}">
            ${entry.key.producesCondition.expressions}
          </c:if>
        </p>
      </div>
    </c:forEach>
    </div>
</body>
</html>