<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/home.jsp"/>
<%@ include file="header.jsp" %>
<%@ include file="nav.jsp" %>
<h2><bean:message key="home.heading"/></h2>
<p><bean:message key="home.introduction"/></p>
<table class="dashboard" cellpadding="3" cellspacing="0" border="1">
<tr>
    <th><bean:message key="home.column.area"/></th>
    <th><bean:message key="home.column.action"/></th>
</tr>
<tr>
    <td><bean:message key="home.policyArea"/></td>
    <td><a href="policy/list.do"><bean:message key="home.policyLink"/></a></td>
</tr>
<tr>
    <td><bean:message key="home.claimArea"/></td>
    <td><a href="workbench/list.do"><bean:message key="home.claimLink"/></a></td>
</tr>
<tr>
    <td><bean:message key="home.reportArea"/></td>
    <td><a href="report/index.do"><bean:message key="home.reportLink"/></a></td>
</tr>
</table>
<p><bean:message key="home.sessionUser"/>:
<ns:field name="signedInUser" value="${sessionScope.user}" type="text"/></p>
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
<%@ include file="footer.jsp" %>
