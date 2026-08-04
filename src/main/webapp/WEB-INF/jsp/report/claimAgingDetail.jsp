<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/report/claimAgingDetail.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.aged.claim.detail"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.claim"/></th>
<th><bean:message key="jsp.reported.date"/></th>
<th><bean:message key="jsp.status"/></th>
<th><bean:message key="jsp.reserve"/></th>
<th><bean:message key="jsp.adjuster"/></th></tr>
<logic:iterate name="claims" id="claim">
<tr>
<td><ns:field name="agingClaim_${claim.claimId}" value="${claim.claimId}" type="integer"/></td>
<td><ns:field name="agingDate_${claim.claimId}" value="${claim.reportedDate}" type="date"/></td>
<td><bean:write name="claim" property="status"/></td>
<td><ns:field name="agingReserve_${claim.claimId}" value="${claim.reserveAmount}" type="money"/></td>
<td><bean:write name="claim" property="assignedAdjuster"/></td>
</tr>
</logic:iterate>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.as.of.date"/></th><td><ns:field name="agingDetailAsOf" value="${reportAsOf}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.claim.count"/></th><td><ns:field name="agingDetailCount" value="${claimCount}" type="integer"/></td></tr>
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
