<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/workbench/notes.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.claim.note.history"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.date"/></th>
<th><bean:message key="jsp.author"/></th>
<th><bean:message key="jsp.note"/></th>
<th><bean:message key="jsp.disposition"/></th></tr>
<logic:iterate name="notes" id="note">
<tr>
<td><bean:write name="note" property="noteDate"/></td>
<td><bean:write name="note" property="author"/></td>
<td><bean:write name="note" property="noteText"/></td>
<td><bean:message key="jsp.note.recorded"/></td>
</tr>
</logic:iterate>
<tr><td colspan="4"><bean:message key="jsp.notes.retained"/></td></tr>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.claim.identifier"/></th><td><ns:field name="noteClaimId" value="${claimId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.current.status"/></th><td><ns:field name="noteClaimStatus" value="${claimStatus}" type="text"/></td></tr>
<tr>
<th><bean:message key="jsp.current.reserve"/></th><td><ns:field name="noteReserve" value="${reserveAmount}" type="money"/></td></tr>
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
