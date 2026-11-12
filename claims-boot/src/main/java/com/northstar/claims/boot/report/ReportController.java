package com.northstar.claims.boot.report;

import java.util.List;
import java.util.Map;

import com.northstar.claims.boot.ClaimsConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Ports of the eleven read-only report actions mapped under /report in struts-config.xml. */
@Controller
@RequestMapping("/report")
public class ReportController {
    /** ClaimsActionSupport.reportDate(): a literal, independent of report.asof.date. */
    static final String LEGACY_REPORT_DATE = "2019-04-01";

    private final ReportDao reports;
    private final ClaimsConfig config;

    public ReportController(ReportDao reports, ClaimsConfig config) {
        this.reports = reports;
        this.config = config;
    }

    @RequestMapping("/index.do")
    public String index(Model model) {
        model.addAttribute("reportAsOf", LEGACY_REPORT_DATE);
        return "report/index";
    }

    @RequestMapping("/openByAdjuster.do")
    public String openByAdjuster(Model model) {
        rows(model, reports.openClaimsByAdjuster());
        return "report/openByAdjuster";
    }

    @RequestMapping("/lossRatio.do")
    public String lossRatio(Model model) {
        rows(model, reports.lossRatioByLine());
        return "report/lossRatio";
    }

    @RequestMapping("/agedClaims.do")
    public String agedClaims(Model model) {
        rows(model, reports.agedClaims(config.get("report.asof.date")));
        return "report/agedClaims";
    }

    @RequestMapping("/claimInventory.do")
    public String claimInventory(Model model) {
        String asOf = config.get("report.asof.date");
        rows(model, reports.closuresPostedThrough(asOf));
        model.addAttribute("reportAsOf", asOf);
        return "report/claimInventory";
    }

    @RequestMapping("/closedClaims.do")
    public String closedClaims(Model model) {
        String asOf = config.get("report.asof.date");
        rows(model, reports.closedClaimsThrough(asOf));
        model.addAttribute("reportAsOf", asOf);
        return "report/closedClaims";
    }

    @RequestMapping("/adjusterWorkload.do")
    public String adjusterWorkload(Model model) {
        rows(model, reports.openClaimsByAdjuster());
        model.addAttribute("reportAsOf", LEGACY_REPORT_DATE);
        return "report/adjusterWorkload";
    }

    @RequestMapping("/adjusterClaims.do")
    public String adjusterClaims(@RequestParam(name = "adjuster", required = false) String adjuster,
            Model model) {
        String trimmed = adjuster == null ? "" : adjuster.trim();
        rows(model, reports.claimsForAdjuster(trimmed));
        model.addAttribute("adjuster", trimmed);
        model.addAttribute("reportAsOf", config.get("report.asof.date"));
        return "report/adjusterClaims";
    }

    @RequestMapping("/claimAgingDetail.do")
    public String claimAgingDetail(Model model) {
        List<Map<String, Object>> claims = reports.claimsByStatus("OPEN");
        model.addAttribute("claims", claims);
        model.addAttribute("claimCount", claims.size());
        model.addAttribute("reportAsOf", LEGACY_REPORT_DATE);
        return "report/claimAgingDetail";
    }

    @RequestMapping("/premiumDetail.do")
    public String premiumDetail(Model model) {
        List<Map<String, Object>> policies = reports.policies();
        model.addAttribute("policies", policies);
        model.addAttribute("policyCount", policies.size());
        model.addAttribute("reportAsOf", LEGACY_REPORT_DATE);
        return "report/premiumDetail";
    }

    @RequestMapping("/reconciliation.do")
    public String reconciliation(Model model) {
        rows(model, reports.lossRatioByLine());
        model.addAttribute("reportAsOf", LEGACY_REPORT_DATE);
        return "report/reconciliation";
    }

    private static void rows(Model model, List<Map<String, Object>> rows) {
        model.addAttribute("reportRows", rows);
        model.addAttribute("reportRowCount", rows.size());
    }
}
