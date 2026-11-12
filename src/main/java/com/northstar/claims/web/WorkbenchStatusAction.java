package com.northstar.claims.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import com.northstar.claims.model.Claim;
import com.northstar.claims.util.ClaimsConfig;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles the WorkbenchStatusAction request in the claims web module.
 * The action loads screen data and completes the configured request workflow.
 */
public class WorkbenchStatusAction extends ClaimsActionSupport {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        int claimId = integer(request.getParameter("claimId"), 119);
        String status = request.getParameter("status");
        if (status == null || status.length() == 0) {
            status = "INVESTIGATING";
        }
        update("update CLAIM set status = '" + status
                + "' where claim_id = " + claimId);
        if ("CLOSED".equalsIgnoreCase(status)) {
            recordClosure(claimId, String.valueOf(
                    request.getSession().getAttribute("user")));
        }
        request.setAttribute("claim", findClaim(claimId));
        request.setAttribute("claimStatus", status);
        request.setAttribute("claimId", new Integer(claimId));
        request.setAttribute("screenName", "detail");
        return mapping.findForward("workbenchView");
    }

    private void recordClosure(int claimId, String user) throws Exception {
        Connection connection = openConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "delete from CLAIM_CLOSURE where claim_id = ?");
            statement.setInt(1, claimId);
            statement.executeUpdate();
            statement.close();
            statement = connection.prepareStatement(
                    "insert into CLAIM_CLOSURE values (?,?,?)");
            statement.setInt(1, claimId);
            statement.setString(2, ClaimsConfig.get("report.asof.date"));
            statement.setString(3, user);
            statement.executeUpdate();
        } finally {
            try { statement.close(); } catch (Exception ignored) {}
            try { connection.close(); } catch (Exception ignored) {}
        }
    }
}
