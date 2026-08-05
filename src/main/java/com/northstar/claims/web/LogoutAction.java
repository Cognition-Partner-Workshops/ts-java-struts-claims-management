package com.northstar.claims.web;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the LogoutAction request in the claims web module.
 * The action loads screen data and completes the configured request workflow.
 */
public class LogoutAction extends ClaimsActionSupport {

    /** Clears the session and leaves no operator state behind. */

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        request.setAttribute("logoutStatus", "COMPLETE");
        request.setAttribute("screenName", "detail");
        return mapping.findForward("login");
    }

}
