<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/payment/history.jsp"/>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="page" cellpadding="0" cellspacing="0" border="0">
<tr><td class="banner"><h1><bean:message key="app.title"/></h1></td></tr>
<tr><td class="navigation"><a href="<%= request.getContextPath() %>/policy/list.do"><bean:message key="nav.policies"/></a> |
<a href="<%= request.getContextPath() %>/workbench/list.do"><bean:message key="nav.workbench"/></a> |
<a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a> |
<a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td class="content">
<%@ include file="../nav.jsp" %>
<p><a href="detail.do?paymentId=61"><bean:message key="payment.detailLink"/></a> |
<a href="remittance.do?claimId=${claimId}"><bean:message key="payment.remittanceLink"/></a></p>
<p><a href="detail.do?paymentId=61"><bean:message key="payment.detailLink"/></a> |
<a href="remittance.do?claimId=${claimId}"><bean:message key="payment.remittanceLink"/></a></p>
<h2><bean:message key="payment.history.heading"/></h2>
<table class="results" cellpadding="3" cellspacing="0" border="1">
<tr>
<th><bean:message key="payment.check"/></th>
<th><bean:message key="payment.amount"/></th>
<th><bean:message key="payment.status"/></th></tr>
<logic:iterate id="payment" name="payments">
<tr><td><ns:field name="checkNumber_${payment.paymentId}" value="${payment.checkNumber}" type="text"/></td>
<td><ns:field
name="paymentAmount_${payment.paymentId}" value="${payment.amount}" type="money"/></td>
<td><ns:field name="paymentStatus_${payment.paymentId}"
value="${payment.status}" type="text"/></td></tr>
</logic:iterate>
</table>
<p><bean:message key="payment.history.instruction"/></p>
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
