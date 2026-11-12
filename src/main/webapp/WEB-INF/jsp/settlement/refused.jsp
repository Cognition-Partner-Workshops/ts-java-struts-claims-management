<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/settlement/refused.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
java.util.List nsValidationErrors =
        (java.util.List) request.getAttribute("nsValidationErrors");
if (nsValidationErrors != null) {
    java.util.Iterator nsMessages = nsValidationErrors.iterator();
    while (nsMessages.hasNext()) {
%><!-- ns:error <%= nsMessages.next() %> --><%
    }
}
%>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="settlement.refused.heading"/></h2>
<p><bean:message key="errors.settlement.authority"/></p>
<table class="details" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="settlement.amount"/></th><td><ns:field name="refusedAmount" value="${refusedAmount}" type="money"/></td></tr>
<tr>
<th><bean:message key="settlement.approvalLimit"/></th><td><ns:field name="approvalLimit" value="${approvalLimit}" type="money"/></td></tr>
<tr>
<th><bean:message key="report.adjuster"/></th><td><ns:field name="refusedAdjuster" value="${refusedAdjuster}" type="text"/></td></tr>
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
