<%@ page isELIgnored="false" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<table class="menu" cellpadding="2" cellspacing="0" border="0">
<tr><td><bean:message key="nav.quickLinks"/></td></tr>
<tr><td><a href="<%= request.getContextPath() %>/intake/new.do"><bean:message key="nav.newClaim"/></a></td></tr>
<tr><td><a href="<%= request.getContextPath() %>/payment/history.do"><bean:message key="nav.payments"/></a></td></tr>
<tr><td><a href="<%= request.getContextPath() %>/report/openByAdjuster.do"><bean:message key="nav.reports"/></a></td></tr>
<tr><td><a href="<%= request.getContextPath() %>/policy/search.do"><bean:message key="nav.policySearch"/></a></td></tr>
<tr><td><a href="<%= request.getContextPath() %>/logout.do"><bean:message key="nav.logout"/></a></td></tr>
<tr><td><bean:message key="nav.operatorMenu"/></td></tr>
<tr><td><bean:message key="nav.currentSession"/></td></tr>
<tr><td><bean:message key="nav.helpDesk"/></td></tr>
<tr><td><bean:message key="nav.version"/></td></tr>
<tr><td><bean:message key="nav.environment"/></td></tr>
</table>
