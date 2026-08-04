<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/report/adjusterWorkload.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.adjuster.workload"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.adjuster"/></th>
<th><bean:message key="jsp.open.claims"/></th>
<th><bean:message key="jsp.reserve.total"/></th>
<th><bean:message key="jsp.oldest.report"/></th></tr>
<logic:iterate name="reportRows" id="row">
<tr>
<td><bean:write name="row" property="adjuster"/></td>
<td><ns:field name="workloadOpen_${row.adjuster}" value="${row.openCount}" type="integer"/></td>
<td><ns:field name="workloadReserve_${row.adjuster}" value="${row.reserveTotal}" type="money"/></td>
<td><ns:field name="workloadOldest_${row.adjuster}" value="${row.oldestReportedDate}" type="date"/></td>
</tr>
</logic:iterate>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.as.of.date"/></th><td><ns:field name="workloadAsOf" value="${reportAsOf}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.rows.returned"/></th><td><ns:field name="workloadRows" value="${reportRowCount}" type="integer"/></td></tr>
</table>
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
