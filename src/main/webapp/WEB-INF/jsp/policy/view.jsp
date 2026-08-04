<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/policy/view.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="policy.view.heading"/></h2>
<table class="details" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="policy.number"/></th><td><bean:write name="policy" property="policyNumber"/></td></tr>
<tr>
<th><bean:message key="policy.insured"/></th><td><bean:write name="policy" property="insuredName"/></td></tr>
<tr>
<th><bean:message key="policy.address"/></th><td><bean:write name="policy" property="insuredAddress"/></td></tr>
<tr>
<th><bean:message key="policy.limit"/></th><td><ns:field name="policyLimit" value="${policy.policyLimit}" type="money"/></td></tr>
<tr>
<th><bean:message key="policy.deductible"/></th><td><ns:field name="deductibleApplied" value="${policy.deductible}" type="money"/></td></tr>
<tr>
<th><bean:message key="policy.status"/></th><td><ns:field name="policyStatus" value="${policy.status}" type="text"/></td></tr>
</table>
<p><a href="coverages.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.coverageLink"/></a></p>
<p><a href="coverageDetail.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.coverageDetailLink"/></a> |
<a href="insuredParty.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.insuredDetailLink"/></a> |
<a href="renewal.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.renewalLink"/></a></p>
<p><a href="coverageDetail.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.coverageDetailLink"/></a> |
<a href="insuredParty.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.insuredDetailLink"/></a> |
<a href="renewal.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:message key="policy.renewalLink"/></a></p>
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
