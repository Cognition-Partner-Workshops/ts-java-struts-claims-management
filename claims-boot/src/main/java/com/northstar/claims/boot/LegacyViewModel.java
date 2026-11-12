package com.northstar.claims.boot;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values the legacy layout read from the session and ClaimsConfig. */
@ControllerAdvice
public class LegacyViewModel {

    private final ClaimsConfig config;

    public LegacyViewModel(ClaimsConfig config) {
        this.config = config;
    }

    @ModelAttribute
    public void populate(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        model.addAttribute("user", session == null ? null : session.getAttribute("user"));
        model.addAttribute("reportAsOfDate", config.get("report.asof.date"));
    }
}
