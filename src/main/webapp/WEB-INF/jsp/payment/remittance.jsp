<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/payment/remittance.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.payment.remittance"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.check"/></th>
<th><bean:message key="jsp.payee"/></th>
<th><bean:message key="jsp.issued.date"/></th>
<th><bean:message key="jsp.amount"/></th>
<th><bean:message key="jsp.status"/></th></tr>
<logic:iterate name="payments" id="payment">
<tr>
<td><ns:field name="remitCheck_${payment.paymentId}" value="${payment.checkNumber}" type="text"/></td>
<td><bean:write name="payment" property="payeeName"/></td>
<td><ns:field name="remitDate_${payment.paymentId}" value="${payment.issuedDate}" type="date"/></td>
<td><ns:field name="remitAmount_${payment.paymentId}" value="${payment.amount}" type="money"/></td>
<td><ns:field name="remitStatus_${payment.paymentId}" value="${payment.status}" type="text"/></td>
</tr>
</logic:iterate>
</table>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.claim"/></th><td><ns:field name="remitClaimId" value="${claimId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.payment.count"/></th><td><ns:field name="remitCount" value="${paymentCount}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.total.issued"/></th><td><ns:field name="remitTotal" value="${paymentTotal}" type="money"/></td></tr>
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
