package com.northstar.claims.web;

import java.util.List;
import com.northstar.claims.dao.ReportDAO;
import com.northstar.claims.util.ClaimsConfig;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Counts claims closed on or before the reporting date. */
public class ClosedClaimsReportAction extends ClaimsActionSupport {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        List rows = new ReportDAO().closedClaimsThrough(
                ClaimsConfig.get("report.asof.date"));
        request.setAttribute("reportRows", rows);
        request.setAttribute("reportRowCount", new Integer(rows.size()));
        request.setAttribute("reportAsOf", ClaimsConfig.get("report.asof.date"));
        request.setAttribute("screenName", "detail");
        return mapping.findForward("report");
    }
}
