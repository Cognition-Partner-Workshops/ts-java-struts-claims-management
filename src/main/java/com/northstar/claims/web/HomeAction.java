package com.northstar.claims.web;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Opens the operations home screen for an authenticated operator. */
public class HomeAction extends ClaimsActionSupport {
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        request.setAttribute("screenName", "home");
        request.setAttribute("screenMode", "read");
        request.setAttribute("operatorScope", "claims");
        request.setAttribute("homeArea", "claims operations");
        request.setAttribute("homeNavigation", "standard");
        request.setAttribute("homeStatus", "available");
        request.setAttribute("homeFormat", "html");
        request.setAttribute("homeRole", "operator");
        request.setAttribute("homeVersion", "claims");
        return mapping.findForward("home");
    }
}
