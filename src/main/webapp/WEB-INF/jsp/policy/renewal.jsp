<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/policy/renewal.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.policy.renewal.review"/></h2>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.policy.identifier"/></th><td><ns:field name="renewalPolicyId" value="${policy.policyId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.policy.number"/></th><td><bean:write name="policy" property="policyNumber"/></td></tr>
<tr>
<th><bean:message key="jsp.line"/></th><td><bean:write name="policy" property="lineOfBusiness"/></td></tr>
<tr>
<th><bean:message key="jsp.expiration"/></th><td><ns:field name="renewalExpiration" value="${policy.expiryDate}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.limit"/></th><td><ns:field name="renewalLimit" value="${policy.policyLimit}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.deductible"/></th><td><ns:field name="renewalDeductible" value="${policy.deductible}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.status"/></th><td><ns:field name="renewalStatus" value="${policy.status}" type="text"/></td></tr>
</table>
<table class="instructions" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.review.item"/></th>
<th><bean:message key="jsp.required.check"/></th></tr>
<tr><td><bean:message key="jsp.expiration"/></td>
<td><bean:message key="jsp.review.expiration"/></td></tr>
<tr><td><bean:message key="jsp.limit"/></td>
<td><bean:message key="jsp.review.limit"/></td></tr>
<tr><td><bean:message key="jsp.deductible"/></td>
<td><bean:message key="jsp.review.deductible"/></td></tr>
<tr><td><bean:message key="jsp.coverage"/></td>
<td><bean:message key="jsp.review.coverage"/></td></tr>
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
