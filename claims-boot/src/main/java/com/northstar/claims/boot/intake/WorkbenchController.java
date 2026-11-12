package com.northstar.claims.boot.intake;

import com.northstar.claims.boot.ClaimsConfig;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WorkbenchController {
    private final ClaimDAO claims;
    private final NoteDAO notes;
    private final ClaimsConfig config;

    public WorkbenchController(ClaimDAO claims, NoteDAO notes, ClaimsConfig config) {
        this.claims = claims;
        this.notes = notes;
        this.config = config;
    }

    @RequestMapping("/workbench/list.do")
    public String list(Model model) {
        List<Claim> rows;
        try {
            rows = claims.findByStatus("OPEN");
        } catch (Exception ignored) {
            rows = List.of();
        }
        model.addAttribute("claims", rows);
        return "workbench/list";
    }

    @RequestMapping("/workbench/view.do")
    public String view(HttpServletRequest request, Model model) {
        Claim claim = findClaim(id(request));
        // bean:write throws when the claim is missing, so the global error view wins.
        if (claim == null) throw new IllegalStateException("Missing claim");
        model.addAttribute("claim", claim);
        summary(model, claim);
        return "workbench/view";
    }

    @RequestMapping("/workbench/assign.do")
    public String assign(HttpServletRequest request, Model model) {
        int id = id(request);
        String adjuster = text(request.getParameter("adjuster"), "adjuster2");
        claims.assign(id, adjuster);
        mutation(model, id);
        model.addAttribute("assignedAdjuster", adjuster);
        return "workbench/view";
    }

    @RequestMapping("/workbench/status.do")
    public String status(HttpServletRequest request, Model model) {
        int id = id(request);
        String status = text(request.getParameter("status"), "INVESTIGATING");
        claims.updateStatus(id, status);
        claims.updateClosure(id, "CLOSED".equalsIgnoreCase(status),
                String.valueOf(request.getSession().getAttribute("user")), config.get("report.asof.date"));
        mutation(model, id);
        model.addAttribute("claimStatus", status);
        return "workbench/view";
    }

    @RequestMapping("/workbench/reserve.do")
    public String reserve(HttpServletRequest request, Model model) {
        int id = id(request);
        double reserve;
        try {
            reserve = Double.parseDouble(request.getParameter("reserveAmount"));
        } catch (Exception ignored) {
            reserve = 4500;
        }
        claims.updateReserve(id, reserve);
        mutation(model, id);
        model.addAttribute("reserveAmount", reserve);
        return "workbench/view";
    }

    @RequestMapping("/workbench/note.do")
    public String note(HttpServletRequest request, Model model) {
        mutation(model, id(request));
        model.addAttribute("noteText", text(request.getParameter("noteText"), "Review completed"));
        // The legacy action only echoes the note. It never calls NoteDAO.insert.
        return "workbench/view";
    }

    @RequestMapping("/workbench/notes.do")
    public String notes(HttpServletRequest request, Model model) {
        int id = id(request);
        model.addAttribute("notes", notes.findByClaim(id));
        Claim claim = claims.findById(id);
        if (claim == null) claim = claims.findById(119);
        summary(model, claim);
        return "workbench/notes";
    }

    @RequestMapping("/workbench/reserveHistory.do")
    public String reserveHistory(HttpServletRequest request, Model model) {
        summary(model, claims.findById(id(request)));
        model.addAttribute("reserveHistory", List.of());
        return "workbench/reserveHistory";
    }

    @RequestMapping("/workbench/statusHistory.do")
    public String statusHistory(HttpServletRequest request, Model model) {
        summary(model, claims.findById(id(request)));
        model.addAttribute("statusHistory", List.of());
        return "workbench/statusHistory";
    }

    private void mutation(Model model, int id) {
        Claim claim = findClaim(id);
        if (claim == null) throw new IllegalStateException("Missing claim");
        model.addAttribute("claim", claim);
        model.addAttribute("claimId", id);
        // Mutation actions set only the changed display field, not a full summary.
    }

    private Claim findClaim(int id) {
        try {
            return claims.findById(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void summary(Model model, Claim claim) {
        if (claim == null) {
            model.addAttribute("claimStatus", "UNKNOWN");
            model.addAttribute("reserveAmount", 0d);
            return;
        }
        model.addAttribute("claimId", claim.claimId());
        model.addAttribute("claimStatus", claim.status());
        model.addAttribute("reserveAmount", claim.reserveAmount());
        model.addAttribute("assignedAdjuster", claim.assignedAdjuster());
        model.addAttribute("lossDate", claim.lossDate());
        model.addAttribute("reportedDate", claim.reportedDate());
    }

    private int id(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("claimId"));
        } catch (Exception ignored) {
            return 119;
        }
    }

    private String text(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
