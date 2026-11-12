package com.northstar.claims.boot.payment;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/** Ports the four payment actions without adding validation or transactions. */
@Controller
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentDAO payments;
    private final PaymentSettlementDAO settlements;

    public PaymentController(PaymentDAO payments, PaymentSettlementDAO settlements) {
        this.payments = payments;
        this.settlements = settlements;
    }

    @RequestMapping("/issue.do")
    public String issue(HttpServletRequest request, Model model) {
        // Actions read the first raw request parameter; do not coerce blank strings.
        PaymentForm form = new PaymentForm();
        form.setClaimId(request.getParameter("claimId"));
        form.setPayeeName(request.getParameter("payeeName"));
        form.setAmount(request.getParameter("amount"));
        form.setPaymentMethod(request.getParameter("paymentMethod"));
        form.setCheckNumber(request.getParameter("checkNumber"));
        int claimId = integer(form.getClaimId(), 119);
        var settlement = settlements.findByClaim(claimId);
        Payment payment = new Payment();
        int paymentId = payments.nextId();
        payment.setPaymentId(paymentId);
        payment.setClaimId(claimId);
        // A missing settlement must fail as in the legacy action.
        payment.setSettlementId(settlement.settlementId());
        payment.setPayeeName(form.getPayeeName());
        payment.setAmount(decimal(form.getAmount(), settlement.settlementAmount()));
        payment.setPaymentMethod(form.getPaymentMethod());
        payment.setCheckNumber("CHK-" + paymentId);
        payment.setIssuedDate("2019-04-03");
        payment.setStatus("ISSUED");
        payments.insert(payment);
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("claimId", claimId);
        model.addAttribute("paymentAmount", payment.getAmount());
        model.addAttribute("checkNumber", payment.getCheckNumber());
        model.addAttribute("paymentStatus", payment.getStatus());
        model.addAttribute("screenName", "detail");
        return "payment/issue";
    }

    @RequestMapping("/history.do")
    public String history(HttpServletRequest request, Model model) {
        int claimId = integer(request.getParameter("claimId"), 119);
        var rows = payments.findByClaim(claimId);
        model.addAttribute("payments", rows);
        model.addAttribute("claimId", claimId);
        model.addAttribute("paymentCount", rows.size());
        model.addAttribute("screenName", "detail");
        return "payment/history";
    }

    @RequestMapping("/detail.do")
    public String detail(HttpServletRequest request, Model model) {
        int id = integer(request.getParameter("paymentId"), 61);
        Payment payment = payments.findById(id);
        // JSP bean:write fails when no payment exists; retain the HTTP 200 error page.
        if (payment == null) {
            throw new IllegalStateException("No payment bean for " + id);
        }
        model.addAttribute("payment", payment);
        model.addAttribute("claimId", payment.getClaimId());
        model.addAttribute("screenName", "detail");
        model.addAttribute("screenMode", "read");
        model.addAttribute("operatorScope", "claims");
        return "payment/detail";
    }

    @RequestMapping("/remittance.do")
    public String remittance(HttpServletRequest request, Model model) {
        int id = integer(request.getParameter("claimId"), 119);
        var rows = payments.findByClaim(id);
        model.addAttribute("payments", rows);
        model.addAttribute("claimId", id);
        model.addAttribute("paymentCount", rows.size());
        model.addAttribute("paymentTotal", payments.totalIssued(id));
        model.addAttribute("screenName", "detail");
        return "payment/remittance";
    }

    private static int integer(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception failure) {
            return fallback;
        }
    }

    private static double decimal(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception failure) {
            return fallback;
        }
    }
}
