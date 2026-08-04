<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/policy/list.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="policy.list.heading"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="policy.number"/></th>
<th><bean:message key="policy.line"/></th>
<th><bean:message
key="policy.insured"/></th>
<th><bean:message key="policy.limit"/></th>
<th><bean:message key="policy.status"/></th></tr>
<logic:iterate id="policy" name="policies">
<tr><td><a href="view.do?policyId=<bean:write name="policy" property="policyId"/>"><bean:write name="policy"
property="policyNumber"/></a></td>
<td><bean:write name="policy" property="lineOfBusiness"/></td>
<td><bean:write name="policy"
property="insuredName"/></td>
<td><ns:field name="policyLimit_${policy.policyId}" value="${policy.policyLimit}" type="money"/></td>
<td><ns:field
name="policyStatus_${policy.policyId}" value="${policy.status}" type="text"/></td></tr>
</logic:iterate>
</table>
<p><bean:message key="policy.list.instruction"/></p>
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
