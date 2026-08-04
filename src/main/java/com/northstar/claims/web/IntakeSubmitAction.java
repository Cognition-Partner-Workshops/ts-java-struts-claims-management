package com.northstar.claims.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the IntakeSubmitAction request in the claims web module.
 * The action loads screen data and completes the configured request workflow.
 */
public class IntakeSubmitAction extends ClaimsActionSupport {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String claimant = request.getParameter("claimantName");
        String description = request.getParameter("description");
        String suppliedDate = request.getParameter("lossDate");
        List errors = new ArrayList();
        if (claimant == null || claimant.trim().length() == 0) {
            errors.add("errors.claimant.required");
        }
        if (description == null || description.trim().length() == 0) {
            errors.add("errors.description.required");
        }
        if (suppliedDate != null && suppliedDate.length() > 0
                && !suppliedDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
            errors.add("errors.lossdate.format");
        }
        if (!errors.isEmpty()) {
            request.setAttribute("nsValidationErrors", errors);
            return new ActionForward("/WEB-INF/jsp/intake/new.jsp", false);
        }
        String lossDate = normalizedDate(suppliedDate);
        int claimId = nextId("CLAIM");
        Connection connection = openConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "insert into CLAIM values "
                    + "(?,? ,9001,?,?,?,?,?,'OPEN',0,"
                    + "'adjuster1','supervisor',?)");
            statement.setInt(1, claimId);
            statement.setString(2, "CLM-" + claimId);
            statement.setString(3, claimant);
            statement.setString(4, lossDate);
            statement.setString(5, lossDate);
            statement.setString(6, "WATER");
            statement.setString(7, description);
            statement.setString(8, lossDate);
            statement.executeUpdate();
        } finally {
            try { statement.close(); } catch (Exception ignored) {}
            try { connection.close(); } catch (Exception ignored) {}
        }
        request.getSession().setAttribute("lastLossDate", lossDate);
        request.setAttribute("claimId", String.valueOf(claimId));
        request.setAttribute("claimStatus", "OPEN");
        request.setAttribute("screenName", "detail");
        return mapping.findForward("confirm");
    }
}
