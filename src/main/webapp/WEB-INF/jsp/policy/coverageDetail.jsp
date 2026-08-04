<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/policy/coverageDetail.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.policy.coverage.detail"/></h2>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.policy"/></th><td><ns:field name="coveragePolicyId" value="${policy.policyId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.policy.number"/></th><td><bean:write name="policy" property="policyNumber"/></td></tr>
<tr>
<th><bean:message key="jsp.line.of.business"/></th><td><bean:write name="policy" property="lineOfBusiness"/></td></tr>
<tr>
<th><bean:message key="jsp.policy.limit"/></th><td><ns:field name="coveragePolicyLimit" value="${policy.policyLimit}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.deductible"/></th><td><ns:field name="coverageDeductible" value="${policy.deductible}" type="money"/></td></tr>
</table>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.coverage.code"/></th>
<th><bean:message key="jsp.description"/></th>
<th><bean:message key="jsp.limit"/></th>
<th><bean:message key="jsp.status"/></th></tr>
<logic:iterate name="coverages" id="coverage">
<tr>
<td><bean:write name="coverage" property="coverageCode"/></td>
<td><bean:write name="coverage" property="description"/></td>
<td><ns:field name="coverageLimit_${coverage.coverageId}" value="${coverage.limit}" type="money"/></td>
<td><bean:write name="coverage" property="status"/></td>
</tr>
</logic:iterate>
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
