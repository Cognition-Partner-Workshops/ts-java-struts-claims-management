<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/policy/insuredParty.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.insured.party.detail"/></h2>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.policy"/></th><td><ns:field name="insuredPolicyId" value="${policy.policyId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.policy.number"/></th><td>${policy.policyNumber}</td></tr>
<tr>
<th><bean:message key="jsp.insured.name"/></th><td>${policy.insuredName}</td></tr>
<tr>
<th><bean:message key="jsp.address"/></th><td>${policy.insuredAddress}</td></tr>
<tr>
<th><bean:message key="jsp.city"/></th><td><bean:message key="jsp.address.on.file"/></td></tr>
<tr>
<th><bean:message key="jsp.state"/></th><td><bean:message key="jsp.address.on.file"/></td></tr>
<tr>
<th><bean:message key="jsp.postal.code"/></th><td><bean:message key="jsp.address.on.file"/></td></tr>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.effective.date"/></th><td><ns:field name="insuredEffective" value="${policy.effectiveDate}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.expiration.date"/></th><td><ns:field name="insuredExpiration" value="${policy.expiryDate}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.policy.status"/></th><td><ns:field name="insuredPolicyStatus" value="${policy.status}" type="text"/></td></tr>
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
