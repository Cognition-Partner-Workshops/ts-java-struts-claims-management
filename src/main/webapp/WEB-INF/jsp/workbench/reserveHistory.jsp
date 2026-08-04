<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/workbench/reserveHistory.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.reserve.history"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.effective.date"/></th>
<th><bean:message key="jsp.previous.reserve"/></th>
<th><bean:message key="jsp.new.reserve"/></th>
<th><bean:message key="jsp.reason"/></th></tr>
<logic:iterate name="reserveHistory" id="history">
<tr>
<td><bean:write name="history" property="changeDate"/></td>
<td><ns:field name="oldReserve_${history.historyId}" value="${history.previousAmount}" type="money"/></td>
<td><ns:field name="newReserve_${history.historyId}" value="${history.newAmount}" type="money"/></td>
<td><bean:write name="history" property="reason"/></td>
</tr>
</logic:iterate>
<tr><td colspan="4">Reserve changes are recorded separately from claim notes.</td></tr>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.claim"/></th><td><ns:field name="reserveHistoryClaim" value="${claimId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.current.reserve"/></th><td><ns:field name="currentReserve" value="${reserveAmount}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.review.status"/></th><td><ns:field name="reserveReviewStatus" value="${claimStatus}" type="text"/></td></tr>
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
