package com.northstar.claims.web;

import com.northstar.claims.dao.SettlementDAO;
import com.northstar.claims.model.Settlement;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the SettlementSaveAction request in the claims web module.
 * Without submitted amounts it saves the settlement left by the calculate screen.
 */
public class SettlementSaveAction extends SettlementAuthorityAction {

    protected ActionForward executeSettlement(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Settlement value = resolveSettlement(form, request);
        value.setSettlementId(nextId("SETTLEMENT"));
        value.setCalculatedBy(String.valueOf(
                request.getSession().getAttribute("user")));
        value.setCalculatedDate("2019-04-01");
        new SettlementDAO().save(value);
        request.getSession().removeAttribute(PENDING_SETTLEMENT);
        request.setAttribute("settlement", value);
        request.setAttribute("settlementAmount",
                new Double(value.getSettlementAmount()));
        request.setAttribute("screenName", "detail");
        return mapping.findForward("settlement");
    }

    protected Settlement resolveSettlement(ActionForm form,
            HttpServletRequest request) throws Exception {
        Settlement pending = (Settlement) request.getSession()
                .getAttribute(PENDING_SETTLEMENT);
        if (pending != null && request.getParameter("coveredAmount") == null
                && pending.getClaimId() == integer(
                        request.getParameter("claimId"), pending.getClaimId())) {
            return pending;
        }
        return super.resolveSettlement(form, request);
    }
}
