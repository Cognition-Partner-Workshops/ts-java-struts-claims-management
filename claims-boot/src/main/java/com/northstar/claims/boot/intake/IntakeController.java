package com.northstar.claims.boot.intake;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IntakeController {
    private final ClaimDAO claims;

    public IntakeController(ClaimDAO claims) {
        this.claims = claims;
    }

    @RequestMapping("/intake/new.do")
    public String intake() {
        return "intake/new";
    }

    @RequestMapping("/intake/confirm.do")
    public String confirm(HttpServletRequest request, Model model) {
        model.addAttribute("claimId", request.getParameter("claimId"));
        return "intake/confirm";
    }

    @RequestMapping("/intake/submit.do")
    public String submit(HttpServletRequest request, Model model) {
        IntakeForm form = IntakeForm.from(request);
        List<String> errors = form.validate();
        if (!errors.isEmpty()) {
            model.addAttribute("nsValidationErrors", errors);
            return "intake/new";
        }
        int id = claims.submit(form);
        request.getSession().setAttribute("lastLossDate", form.normalizedDate());
        model.addAttribute("claimId", String.valueOf(id));
        model.addAttribute("claimStatus", "OPEN");
        return "intake/confirm";
    }
}
