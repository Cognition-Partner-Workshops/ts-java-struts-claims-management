package com.northstar.claims.web;

import java.util.List;
import com.northstar.claims.dao.ReportDAO;
import com.northstar.claims.util.ClaimsConfig;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Lists the claims assigned to the adjuster named on the request. */
public class AdjusterClaimsReportAction extends ClaimsActionSupport {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String adjuster = safeParameter(request, "adjuster");
        List rows = new ReportDAO().claimsForAdjuster(adjuster);
        request.setAttribute("adjuster", adjuster);
        request.setAttribute("reportRows", rows);
        request.setAttribute("reportRowCount", new Integer(rows.size()));
        request.setAttribute("reportAsOf", ClaimsConfig.get("report.asof.date"));
        request.setAttribute("screenName", "detail");
        return mapping.findForward("report");
    }
}
