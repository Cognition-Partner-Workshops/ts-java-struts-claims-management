package com.northstar.claims.boot.settlement;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SettlementController {
    public static final String PENDING_SETTLEMENT = "pendingSettlement";
    private final JdbcTemplate jdbc;
    private final SettlementDAO dao;

    public SettlementController(JdbcTemplate jdbc, SettlementDAO dao) {
        this.jdbc = jdbc;
        this.dao = dao;
    }

    @RequestMapping("/settlement/calculate.do")
    public String calculate(HttpServletRequest request, org.springframework.ui.Model model) {
        String view = execute(request, false);
        copyAttributes(request, model);
        return view;
    }

    @RequestMapping("/settlement/save.do")
    public String save(HttpServletRequest request, org.springframework.ui.Model model) {
        String view = execute(request, true);
        copyAttributes(request, model);
        return view;
    }

    public String execute(HttpServletRequest request, boolean save) {
        SettlementForm form = SettlementForm.bind(request);
        Settlement proposed = resolve(form, request, save);
        String user = String.valueOf(request.getSession().getAttribute("user"));
        var adjusters = jdbc.query("select * from ADJUSTER where username = ?",
                (rs, row) -> rs.getDouble("approval_limit"), user);
        double limit = adjusters.isEmpty() ? 0 : adjusters.get(0);
        if (proposed.getSettlementAmount() > limit) {
            request.setAttribute("nsValidationErrors", List.of("errors.settlement.authority"));
            request.setAttribute("refusedAmount", proposed.getSettlementAmount());
            request.setAttribute("approvalLimit", limit);
            request.setAttribute("refusedAdjuster", user);
            return "settlement/refused";
        }
        Settlement value = resolve(form, request, save);
        if (save) {
            value.setSettlementId(jdbc.queryForObject("select coalesce(max(settlement_id),0)+1 from SETTLEMENT", Integer.class));
            value.setCalculatedBy(user);
            value.setCalculatedDate("2019-04-01");
            dao.save(value);
            request.getSession().removeAttribute(PENDING_SETTLEMENT);
            request.setAttribute("settlementAmount", value.getSettlementAmount());
        } else {
            request.getSession().setAttribute(PENDING_SETTLEMENT, value);
            request.setAttribute("policyLimit", policyLimit(value.getClaimId()));
        }
        request.setAttribute("settlement", value);
        request.setAttribute("screenName", "detail");
        return save ? "settlement/save" : "settlement/calculate";
    }

    @RequestMapping("/settlement/detail.do")
    public String detail(HttpServletRequest request, org.springframework.ui.Model model) {
        request.setAttribute("settlement", dao.findByClaim(integer(request.getParameter("claimId"), 119)));
        request.setAttribute("screenName", "detail");
        request.setAttribute("screenMode", "read");
        request.setAttribute("operatorScope", "claims");
        copyAttributes(request, model);
        return "settlement/detail";
    }

    private void copyAttributes(HttpServletRequest request, org.springframework.ui.Model model) {
        for (String name : List.of("settlement", "settlementAmount", "policyLimit", "screenName",
                "screenMode", "operatorScope", "nsValidationErrors", "refusedAmount", "approvalLimit", "refusedAdjuster")) {
            model.addAttribute(name, request.getAttribute(name));
        }
    }

    private Settlement resolve(SettlementForm form, HttpServletRequest request, boolean save) {
        if (save) {
            Settlement pending = (Settlement) request.getSession().getAttribute(PENDING_SETTLEMENT);
            if (pending != null && request.getParameter("coveredAmount") == null
                    && pending.getClaimId() == integer(request.getParameter("claimId"), pending.getClaimId())) {
                return pending;
            }
        }
        int claimId = integer(request.getParameter("claimId"), 119);
        Settlement value = SettlementCalculator.getInstance().calculate(
                decimal(request.getParameter("coveredAmount"), 5000), form.getDeductible(),
                decimal(request.getParameter("depreciation"), 0), policyLimit(claimId));
        value.setClaimId(claimId);
        return value;
    }

    double policyLimit(int claimId) {
        var claims = jdbc.query("select * from CLAIM where claim_id = ?", (rs, row) -> rs.getInt("policy_id"), claimId);
        if (!claims.isEmpty()) {
            var policies = jdbc.query("select * from POLICY where policy_id = ?", (rs, row) -> rs.getDouble("policy_limit"), claims.get(0));
            if (!policies.isEmpty()) return policies.get(0);
        }
        return 10000;
    }

    private int integer(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (Exception ignored) { return fallback; }
    }

    private double decimal(String text, double fallback) {
        try { return Double.parseDouble(text); } catch (Exception ignored) { return fallback; }
    }
}
