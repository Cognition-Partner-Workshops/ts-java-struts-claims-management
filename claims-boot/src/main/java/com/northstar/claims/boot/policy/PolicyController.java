package com.northstar.claims.boot.policy;

import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/policy")
public class PolicyController {
    private static final Log log = LogFactory.getLog(PolicyController.class);
    private final PolicyDAO policies;

    public PolicyController(PolicyDAO policies) {
        this.policies = policies;
    }

    @RequestMapping("/search.do")
    public String search(@ModelAttribute("policySearchForm") PolicySearchForm form, Model model) {
        String line = form.getLineOfBusiness();
        if (line == null || line.isEmpty()) {
            line = "AUTO";
        }
        List<Policy> rows;
        try {
            rows = policies.findByLine(line);
        } catch (Exception failure) {
            log.warn("Policy search failed", failure);
            rows = List.of();
        }
        model.addAttribute("policies", rows);
        model.addAttribute("searchLine", line);
        model.addAttribute("searchCount", rows.size());
        model.addAttribute("screenName", "detail");
        return "policy/search";
    }

    @RequestMapping("/list.do")
    public String list(Model model) {
        List<Policy> rows;
        try {
            rows = policies.findAll();
        } catch (Exception failure) {
            log.warn("Policy list failed", failure);
            rows = List.of();
        }
        model.addAttribute("policies", rows);
        model.addAttribute("policyCount", rows.size());
        model.addAttribute("screenName", "detail");
        return "policy/list";
    }

    @RequestMapping("/view.do")
    public String view(@ModelAttribute("policyViewForm") PolicyViewForm form, Model model) {
        int id = integer(form.getPolicyId(), 1);
        model.addAttribute("policyId", id);
        return detail(model, policies.findById(id), "view", false);
    }

    @RequestMapping("/coverages.do")
    public String coverages(@RequestParam(required = false) String policyId, Model model) {
        model.addAttribute("coverages", List.of());
        model.addAttribute("coverageCount", 0);
        return detail(model, policies.findById(integer(policyId, 1)), "coverages", false);
    }

    @RequestMapping("/coverageDetail.do")
    public String coverageDetail(@RequestParam(required = false) String policyId, Model model) {
        model.addAttribute("coverages", List.of());
        return detail(model, policies.findById(integer(policyId, 9001)), "coverageDetail", true);
    }

    @RequestMapping("/insuredParty.do")
    public String insuredParty(@RequestParam(required = false) String policyId, Model model) {
        int id = integer(policyId, 9001);
        Policy policy;
        try {
            policy = policies.findById(id);
        } catch (Exception failure) {
            policy = new Policy();
        }
        if (policy == null) {
            policy = new Policy();
        }
        policy.setPolicyId(id);
        policy.setPolicyNumber("POL-" + id);
        policy.setInsuredName("Policy insured party");
        policy.setInsuredAddress("Address on policy record");
        policy.setEffectiveDate("2018-01-01");
        policy.setExpiryDate("2019-12-31");
        policy.setStatus("ACTIVE");
        return detail(model, policy, "insuredParty", true);
    }

    @RequestMapping("/renewal.do")
    public String renewal(@RequestParam(required = false) String policyId, Model model) {
        Policy policy = policies.findById(integer(policyId, 9001));
        if (policy == null) {
            policy = policies.findById(9001);
        }
        return detail(model, policy, "renewal", true);
    }

    private String detail(Model model, Policy policy, String view, boolean readMode) {
        // Legacy bean:write fails on a missing policy and forwards to error.jsp.
        model.addAttribute("policy", Objects.requireNonNull(policy));
        model.addAttribute("screenName", "detail");
        if (readMode) {
            model.addAttribute("screenMode", "read");
            model.addAttribute("operatorScope", "claims");
        }
        return "policy/" + view;
    }

    private int integer(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception failure) {
            return fallback;
        }
    }
}
