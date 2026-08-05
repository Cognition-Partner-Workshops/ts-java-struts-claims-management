<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://northstar.com/tags" prefix="ns" %>
<ns:view path="/WEB-INF/jsp/login.jsp"/>
<html><head><title><bean:message key="login.title"/></title>
<link rel="stylesheet" type="text/css"
      href="<%= request.getContextPath() %>/northstar.css"></head><body>
<table class="login" cellpadding="4" cellspacing="0" border="0">
<tr><td colspan="2"><h1><bean:message key="login.title"/></h1></td></tr>
<tr><td colspan="2"><html:errors/></td></tr>
<html:form action="/login.do" focus="username">
<tr><td><bean:message key="login.username"/></td>
<td><html:text property="username" size="25"/></td></tr>
<tr><td><bean:message key="login.password"/></td>
<td><html:password property="password" size="25"/></td></tr>
<tr><td>&nbsp;</td>
<td><html:submit><bean:message key="login.submit"/></html:submit></td></tr>
</html:form>
<tr><td colspan="2"><bean:message key="login.help"/></td></tr>
</table>
</body></html>
