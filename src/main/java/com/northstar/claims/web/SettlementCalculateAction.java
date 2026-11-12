package com.northstar.claims.web;

import com.northstar.claims.model.Settlement;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the SettlementCalculateAction request in the claims web module.
 * The calculated settlement is kept in the session for the save screen.
 */
public class SettlementCalculateAction extends SettlementAuthorityAction {

    protected ActionForward executeSettlement(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Settlement settlement = resolveSettlement(form, request);
        request.getSession().setAttribute(PENDING_SETTLEMENT, settlement);
        request.setAttribute("settlement", settlement);
        request.setAttribute("policyLimit",
                new Double(policyLimit(settlement.getClaimId())));
        request.setAttribute("screenName", "detail");
        return mapping.findForward("settlement");
    }
}
