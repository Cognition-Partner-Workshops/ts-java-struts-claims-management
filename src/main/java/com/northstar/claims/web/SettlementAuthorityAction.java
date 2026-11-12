package com.northstar.claims.web;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.northstar.claims.dao.AdjusterDAO;
import com.northstar.claims.dao.PolicyDAO;
import com.northstar.claims.model.Adjuster;
import com.northstar.claims.model.Claim;
import com.northstar.claims.model.Policy;
import com.northstar.claims.model.Settlement;
import com.northstar.claims.service.SettlementCalculator;
import com.northstar.claims.web.form.SettlementForm;

/**
 * Shared entry point for the settlement screens. The operator's approval
 * limit is checked here before the screen logic runs.
 */
public abstract class SettlementAuthorityAction extends ClaimsActionSupport {

    public static final String PENDING_SETTLEMENT = "pendingSettlement";

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String user = String.valueOf(request.getSession().getAttribute("user"));
        Settlement proposed = resolveSettlement(form, request);
        double limit = approvalLimit(user);
        if (proposed.getSettlementAmount() > limit) {
            List errors = new ArrayList();
            errors.add("errors.settlement.authority");
            request.setAttribute("nsValidationErrors", errors);
            request.setAttribute("refusedAmount",
                    new Double(proposed.getSettlementAmount()));
            request.setAttribute("approvalLimit", new Double(limit));
            request.setAttribute("refusedAdjuster", user);
            log.warn("Settlement " + proposed.getSettlementAmount()
                    + " refused for " + user + " with limit " + limit);
            return mapping.findForward("refused");
        }
        return executeSettlement(mapping, form, request, response);
    }

    /** Screen logic for the settlement action. */
    protected abstract ActionForward executeSettlement(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception;

    /** Returns the settlement the request is working with. */
    protected Settlement resolveSettlement(ActionForm form,
            HttpServletRequest request) throws Exception {
        int claimId = integer(request.getParameter("claimId"), 119);
        double covered = decimal(request.getParameter("coveredAmount"), 5000);
        double depreciation = decimal(request.getParameter("depreciation"), 0);
        double deductible = form instanceof SettlementForm
                ? ((SettlementForm) form).getDeductible() : 0;
        Settlement settlement = SettlementCalculator.getInstance().calculate(
                covered, deductible, depreciation, policyLimit(claimId));
        settlement.setClaimId(claimId);
        return settlement;
    }

    protected double policyLimit(int claimId) throws Exception {
        Claim claim = findClaim(claimId);
        if (claim != null) {
            Policy policy = new PolicyDAO().findById(claim.getPolicyId());
            if (policy != null) {
                return policy.getPolicyLimit();
            }
        }
        return 10000;
    }

    private double approvalLimit(String user) throws Exception {
        Adjuster adjuster = new AdjusterDAO().findByUsername(user);
        return adjuster == null ? 0 : adjuster.getApprovalLimit();
    }
}
