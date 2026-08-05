package com.northstar.claims.web;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the IntakeNewAction request in the claims web module.
 * The action loads screen data and completes the configured request workflow.
 */
public class IntakeNewAction extends ClaimsActionSupport {

    /** Establishes the defaults used by the FNOL entry screen. */

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        request.setAttribute("intakeMode", "NEW");
        request.setAttribute("defaultLossDate", "04/01/2019");
        request.setAttribute("formTitle", "New first notice of loss");
        request.setAttribute("screenName", "detail");
        return mapping.findForward("intake");
    }

}
