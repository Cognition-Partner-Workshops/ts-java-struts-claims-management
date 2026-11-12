package com.northstar.claims.boot.intake;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/** Request-scoped FNOL values, including Struts' forgiving double conversion. */
public record IntakeForm(String claimantName, String description, String lossDate,
                         double initialReserve) {
    public static IntakeForm from(HttpServletRequest request) {
        double reserve = 0;
        try {
            reserve = Double.parseDouble(request.getParameter("initialReserve"));
        } catch (Exception ignored) {
            // BeanUtils binds blank and nonnumeric doubles as zero.
        }
        return new IntakeForm(request.getParameter("claimantName"),
                request.getParameter("description"), request.getParameter("lossDate"), reserve);
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (claimantName == null || claimantName.trim().isEmpty()) {
            errors.add("errors.claimant.required");
        }
        if (description == null || description.trim().isEmpty()) {
            errors.add("errors.description.required");
        }
        if (lossDate != null && !lossDate.isEmpty() && !lossDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
            errors.add("errors.lossdate.format");
        }
        return errors;
    }

    public String normalizedDate() {
        if (lossDate == null || lossDate.isEmpty()) {
            return "2019-04-01";
        }
        try {
            SimpleDateFormat input = new SimpleDateFormat("MM/dd/yyyy");
            input.setLenient(true);
            return new SimpleDateFormat("yyyy-MM-dd").format(input.parse(lossDate));
        } catch (Exception ignored) {
            return "2019-04-01";
        }
    }
}
