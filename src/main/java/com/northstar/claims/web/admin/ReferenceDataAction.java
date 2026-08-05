package com.northstar.claims.web.admin;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the ReferenceDataAction request in the claims web module.
 * The action loads screen data and completes the configured request workflow.
 */
public class ReferenceDataAction
        extends com.northstar.claims.web.ClaimsActionSupport {

    /** Executes the common workflow and returns the configured forward. */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        request.setAttribute("referenceVersion", "2019.04");
        request.setAttribute("lossTypes",
                java.util.Arrays.asList(new String[] {
                    "WATER", "FIRE", "THEFT", "COLLISION" }));
        return mapping.findForward("reference");
    }
}
