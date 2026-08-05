<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/settlement/calculate.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="settlement.heading"/></h2>
<p><a href="detail.do?claimId=${claimId}"><bean:message key="settlement.detailLink"/></a></p>
<table class="details" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="settlement.covered"/></th><td><ns:field name="coveredAmount" value="${settlement.coveredAmount}" type="money"/></td></tr>
<tr>
<th><bean:message key="settlement.deductible"/></th><td><ns:field name="deductibleApplied" value="${settlement.deductibleApplied}" type="money"/></td></tr>
<tr>
<th><bean:message key="settlement.depreciation"/></th><td><ns:field name="depreciation" value="${settlement.depreciation}" type="money"/></td></tr>
<tr>
<th><bean:message key="settlement.capped"/></th><td><ns:field name="cappedAtLimit" value="${settlement.cappedAtLimit}" type="money"/></td></tr>
<tr>
<th><bean:message key="settlement.amount"/></th><td><ns:field name="settlementAmount" value="${settlement.settlementAmount}" type="money"/></td></tr>
</table>
<p><a href="save.do"><bean:message key="settlement.save"/></a></p>
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
