package com.northstar.claims.web;

import com.northstar.claims.dao.NoteDAO;
import com.northstar.claims.model.ClaimNote;
import com.northstar.claims.dao.ClaimDAO;
import com.northstar.claims.model.Claim;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Loads the notes screen data and selects its configured forward. */
public class WorkbenchNoteHistoryAction extends ClaimsActionSupport {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        int id = integer(request.getParameter("claimId"), 119);
        request.setAttribute("notes", new NoteDAO().findByClaim(id));
        Claim claim = new ClaimDAO().findById(id);
        if (claim == null) {
            claim = new ClaimDAO().findById(119);
        }
        putClaimSummary(request, claim);
        request.setAttribute("screenName", "detail");
        return mapping.findForward("notes");
    }
}
