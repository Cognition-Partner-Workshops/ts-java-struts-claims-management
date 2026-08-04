<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/workbench/view.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<h2><bean:message key="workbench.view.heading"/></h2>
<table class="details" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="claim.number"/></th><td><bean:write name="claim" property="claimNumber"/></td></tr>
<tr>
<th><bean:message key="intake.lossDate"/></th><td><ns:field
 name="lossDate" value="${lossDate}" type="date"/></td></tr>
<tr>
<th><bean:message key="claim.status"/></th><td><ns:field name="claimStatus" value="${claimStatus}" type="text"/></td></tr>
<tr>
<th><bean:message key="claim.reserve"/></th><td><ns:field name="reserveAmount" value="${reserveAmount}" type="money"/></td></tr>
<tr>
<th><bean:message key="claim.adjuster"/></th><td><ns:field name="assignedAdjuster" value="${assignedAdjuster}" type="text"/></td></tr>
<tr>
<th><bean:message key="claim.note"/></th><td><ns:field name="noteText" value="${noteText}" type="text"/></td></tr>
</table>
<p><a href="assign.do"><bean:message key="workbench.assign"/></a> | <a href="reserve.do"><bean:message key="workbench.reserve"/></a></p>
<p><a href="status.do?status=INVESTIGATING"><bean:message key="workbench.investigate"/></a> | <a href="note.do"><bean:message
key="workbench.note"/></a></p>
<p><a href="notes.do?claimId=${claimId}"><bean:message key="workbench.notesHistory"/></a> |
<a href="reserveHistory.do?claimId=${claimId}"><bean:message key="workbench.reserveHistory"/></a> |
<a href="statusHistory.do?claimId=${claimId}"><bean:message key="workbench.statusHistory"/></a></p>
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
