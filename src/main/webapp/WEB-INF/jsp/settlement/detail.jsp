<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/settlement/detail.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.settlement.detail"/></h2>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.settlement.number"/></th><td><ns:field name="detailSettlementId" value="${settlement.settlementId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.claim.number"/></th><td><ns:field name="detailClaimId" value="${settlement.claimId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.covered.amount"/></th><td><ns:field name="detailCoveredAmount" value="${settlement.coveredAmount}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.deductible.applied"/></th><td><ns:field name="detailDeductible" value="${settlement.deductibleApplied}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.depreciation"/></th><td><ns:field name="detailDepreciation" value="${settlement.depreciation}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.policy.cap.applied"/></th><td><ns:field name="detailCapped" value="${settlement.cappedAtLimit}" type="text"/></td></tr>
<tr>
<th><bean:message key="jsp.settlement.amount"/></th><td><ns:field name="detailAmount" value="${settlement.settlementAmount}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.calculated.by"/></th><td><bean:write name="settlement" property="calculatedBy"/></td></tr>
<tr>
<th><bean:message key="jsp.calculated.date"/></th><td><ns:field name="detailDate" value="${settlement.calculatedDate}" type="date"/></td></tr>
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
