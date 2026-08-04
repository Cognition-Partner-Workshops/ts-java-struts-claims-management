<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/report/reconciliation.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.claims.reconciliation"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.line"/></th>
<th><bean:message key="jsp.premium"/></th>
<th><bean:message key="jsp.reserve"/></th>
<th><bean:message key="jsp.paid"/></th>
<th><bean:message key="jsp.variance"/></th></tr>
<logic:iterate name="reportRows" id="row">
<tr>
<td><bean:write name="row" property="lineOfBusiness"/></td>
<td><ns:field name="reconcilePremium_${row.lineOfBusiness}" value="${row.premiumTotal}" type="money"/></td>
<td><ns:field name="reconcileReserve_${row.lineOfBusiness}" value="${row.reserveTotal}" type="money"/></td>
<td><ns:field name="reconcilePaid_${row.lineOfBusiness}" value="${row.paidTotal}" type="money"/></td>
<td><ns:field name="reconcileVariance_${row.lineOfBusiness}" value="${row.variance}" type="money"/></td>
</tr>
</logic:iterate>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.as.of.date"/></th><td><ns:field name="reconcileAsOf" value="${reportAsOf}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.report.rows"/></th><td><ns:field name="reconcileRows" value="${reportRowCount}" type="integer"/></td></tr>
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
<td><bean:message key="report.asof.label"/></td>
</tr>
</table>
<%@ include file="../footer.jsp" %>
