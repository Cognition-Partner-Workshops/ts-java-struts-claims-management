<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/payment/detail.jsp"/>
<%@ include file="../header.jsp" %>
<%@ include file="../nav.jsp" %>
<h2><bean:message key="jsp.payment.detail"/></h2>
<table class="detail" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="jsp.payment.number"/></th><td><ns:field name="detailPaymentId" value="${payment.paymentId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.claim.number"/></th><td><ns:field name="detailPaymentClaim" value="${payment.claimId}" type="integer"/></td></tr>
<tr>
<th><bean:message key="jsp.payee"/></th><td><bean:write name="payment" property="payeeName"/></td></tr>
<tr>
<th><bean:message key="jsp.amount"/></th><td><ns:field name="detailPaymentAmount" value="${payment.amount}" type="money"/></td></tr>
<tr>
<th><bean:message key="jsp.method"/></th><td><bean:write name="payment" property="paymentMethod"/></td></tr>
<tr>
<th><bean:message key="jsp.check.number"/></th><td><ns:field name="detailPaymentCheck" value="${payment.checkNumber}" type="text"/></td></tr>
<tr>
<th><bean:message key="jsp.issued.date"/></th><td><ns:field name="detailPaymentDate" value="${payment.issuedDate}" type="date"/></td></tr>
<tr>
<th><bean:message key="jsp.status"/></th><td><ns:field name="detailPaymentStatus" value="${payment.status}" type="text"/></td></tr>
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
