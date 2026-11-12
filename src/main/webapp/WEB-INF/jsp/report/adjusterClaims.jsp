<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/report/adjusterClaims.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="report.adjusterClaims.heading"/></h2>
<p><bean:message key="report.adjuster"/>: <ns:field name="adjuster" value="${adjuster}" type="text"/></p>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="report.claimNumber"/></th>
<th><bean:message key="claim.status"/></th>
<th><bean:message key="report.reserveTotal"/></th></tr>
<logic:iterate id="row" name="reportRows">
<tr><td><bean:write name="row" property="claimNumber"/></td>
<td><ns:field name="claimStatus_${row.claimNumber}" value="${row.status}" type="text"/></td>
<td><ns:field name="reserveAmount_${row.claimNumber}" value="${row.reserveAmount}" type="money"/></td></tr>
</logic:iterate>
</table>
<p><bean:message key="report.claimCount"/>: <ns:field name="claimCount" value="${reportRowCount}" type="integer"/></p>
<table class="screen-audit" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.screen.context"/></th>
<td><bean:message key="jsp.screen.readOnly"/></td>
</tr>
<tr>
<th><bean:message key="jsp.screen.operator"/></th>
<td><bean:write name="user"/></td>
</tr>
<tr>
<th><bean:message key="jsp.screen.asOf"/></th>
<td><%= com.northstar.claims.util.ClaimsConfig.get("report.asof.date") %></td>
</tr>
</table>
<%@ include file="../footer.jsp" %>
