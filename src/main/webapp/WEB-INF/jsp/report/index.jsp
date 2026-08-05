<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/report/index.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="report.index.heading"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="report.index.name"/></th>
<th><bean:message key="report.index.link"/></th></tr>
<tr><td><bean:message key="report.open.heading"/></td>
<td><a href="openByAdjuster.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.loss.heading"/></td>
<td><a href="lossRatio.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.aged.heading"/></td>
<td><a href="agedClaims.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.adjusterWorkload.heading"/></td>
<td><a href="adjusterWorkload.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.claimAgingDetail.heading"/></td>
<td><a href="claimAgingDetail.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.reconciliation.heading"/></td>
<td><a href="reconciliation.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.premiumDetail.heading"/></td>
<td><a href="premiumDetail.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.adjusterWorkload.heading"/></td>
<td><a href="adjusterWorkload.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.claimAgingDetail.heading"/></td>
<td><a href="claimAgingDetail.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.reconciliation.heading"/></td>
<td><a href="reconciliation.do"><bean:message key="button.open"/></a></td></tr>
<tr><td><bean:message key="report.premiumDetail.heading"/></td>
<td><a href="premiumDetail.do"><bean:message key="button.open"/></a></td></tr>
</table>
<p><bean:message key="report.index.note"/></p>
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
