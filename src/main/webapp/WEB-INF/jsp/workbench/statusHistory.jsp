<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/workbench/statusHistory.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.status.history"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.date"/></th>
<th><bean:message key="jsp.old.status"/></th>
<th><bean:message key="jsp.new.status"/></th>
<th><bean:message key="jsp.changed.by"/></th></tr>
<logic:iterate name="statusHistory" id="history">
<tr>
<td><ns:field name="statusDate_${history.historyId}" value="${history.changeDate}" type="date"/></td>
<td><bean:write name="history" property="oldStatus"/></td>
<td><bean:write name="history" property="newStatus"/></td>
<td><bean:write name="history" property="changedBy"/></td>
</tr>
</logic:iterate>
<tr><td colspan="4">The status history is part of the claim audit trail.</td></tr>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.claim"/></th><td><ns:field name="statusHistoryClaim" value="${claimId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.current.status"/></th><td><ns:field name="statusHistoryCurrent" value="${claimStatus}" type="text"/></td></tr>
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
