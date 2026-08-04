<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/error.jsp"/>
<html><head><title><bean:message key="errors.title"/></title></head><body>
<table class="error" cellpadding="4" cellspacing="0" border="1">
<tr><td><h1><bean:message key="errors.title"/></h1></td></tr>
<tr><td><bean:message key="errors.system"/></td></tr>
<tr><td><bean:message key="errors.reference"/>: <%= request.getAttribute("javax.servlet.error.status_code") %></td></tr>
<tr><td><a href="<%= request.getContextPath() %>/login.do"><bean:message key="errors.loginLink"/></a></td></tr>
<tr><td><bean:message key="errors.contact"/></td></tr>
<tr><td><bean:message key="errors.retry"/></td></tr>
<tr><td><bean:message key="errors.reference"/></td></tr>
<tr><td><bean:message key="errors.status"/></td></tr>
<tr><td><bean:message key="errors.session"/></td></tr>
</table>
</body></html>
