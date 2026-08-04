<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/report/premiumDetail.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.premium.detail"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.policy"/></th>
<th><bean:message key="jsp.number"/></th>
<th><bean:message key="jsp.line"/></th>
<th><bean:message key="jsp.premium"/></th>
<th><bean:message key="jsp.status"/></th></tr>
<logic:iterate name="policies" id="policy">
<tr>
<td><ns:field name="premiumPolicy_${policy.policyId}" value="${policy.policyId}" type="integer"/></td>
<td><bean:write name="policy" property="policyNumber"/></td>
<td><bean:write name="policy" property="lineOfBusiness"/></td>
<td><ns:field name="premiumValue_${policy.policyId}" value="${policy.annualPremium}" type="money"/></td>
<td><ns:field name="premiumStatus_${policy.policyId}" value="${policy.status}" type="text"/></td>
</tr>
</logic:iterate>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.policy.rows"/></th><td><ns:field name="premiumRows" value="${policyCount}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.as.of.date"/></th><td><ns:field name="premiumAsOf" value="${reportAsOf}" type="date"/></td></tr>
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
