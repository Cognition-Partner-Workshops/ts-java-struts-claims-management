package com.northstar.claims.characterization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import com.northstar.claims.dao.ClaimDAO;
import com.northstar.claims.dao.PolicyDAO;
import com.northstar.claims.dao.ReportDAO;
import com.northstar.claims.model.Claim;
import com.northstar.claims.model.Policy;

/**
 * Pins the rows and totals each report query returns today, on the fixed
 * seed ("baseline") and on the seed plus boundary rows ("edge").
 */
public class ReportDaoCharacterizationTest {

    static final String[] ORDER_BY = {
        "status", "status desc", "count", "count desc", "count desc, status"
    };

    static final String[] ADJUSTERS = {"adjuster1", "adjuster5", "adjuster9", "nobody"};

    @Test
    public void baseline() throws Exception {
        characterize("baseline");
    }

    @Test
    public void edge() throws Exception {
        characterize("edge");
    }

    private void characterize(String state) throws Exception {
        ReportFixtures.seed("edge".equals(state));
        String asOf = ReportFixtures.asOfDate();
        ReportDAO dao = new ReportDAO();
        String dir = state + "/dao/";
        Golden.verify(dir + "open-by-adjuster.json",
                snapshot("ReportDAO.openClaimsByAdjuster()", asOf, dao.openClaimsByAdjuster()));
        Golden.verify(dir + "loss-ratio-by-line.json",
                snapshot("ReportDAO.lossRatioByLine()", asOf, dao.lossRatioByLine()));
        Golden.verify(dir + "aged-claims.json",
                snapshot("ReportDAO.agedClaims()", asOf, dao.agedClaims()));
        for (int i = 0; i < ORDER_BY.length; i++) {
            Golden.verify(dir + "claim-inventory-" + slug(ORDER_BY[i]) + ".json",
                    snapshot("ReportDAO.claimCounts(\"" + ORDER_BY[i] + "\")", asOf,
                            dao.claimCounts(ORDER_BY[i])));
        }
        Golden.verify(dir + "closed-claims.json",
                snapshot("ReportDAO.closedClaimsThrough(\"" + asOf + "\")", asOf,
                        dao.closedClaimsThrough(asOf)));
        Golden.verify(dir + "closures-posted.json",
                snapshot("ReportDAO.closuresPostedThrough(\"" + asOf + "\")", asOf,
                        dao.closuresPostedThrough(asOf)));
        for (int i = 0; i < ADJUSTERS.length; i++) {
            Golden.verify(dir + "adjuster-claims-" + ADJUSTERS[i] + ".json",
                    snapshot("ReportDAO.claimsForAdjuster(\"" + ADJUSTERS[i] + "\")", asOf,
                            dao.claimsForAdjuster(ADJUSTERS[i])));
        }
        Golden.verify(dir + "adjuster-workload.json",
                snapshot("ReportDAO.openClaimsByAdjuster() via AdjusterWorkloadDetailAction",
                        asOf, dao.openClaimsByAdjuster()));
        Golden.verify(dir + "claim-aging-detail.json",
                snapshot("ClaimDAO.findByStatus(\"OPEN\") via ClaimAgingDetailAction", asOf,
                        claimRows(new ClaimDAO().findByStatus("OPEN"))));
        Golden.verify(dir + "premium-detail.json",
                snapshot("PolicyDAO.findAll() via PremiumDetailAction", asOf,
                        policyRows(new PolicyDAO().findAll())));
        Golden.verify(dir + "reconciliation.json",
                snapshot("ReportDAO.lossRatioByLine() via ReconciliationAction", asOf,
                        dao.lossRatioByLine()));
    }

    static Map snapshot(String source, String asOf, List rows) {
        Map out = new TreeMap();
        out.put("source", source);
        out.put("asOf", asOf);
        out.put("rows", rows);
        out.put("totals", Golden.totals(rows));
        return out;
    }

    private static List claimRows(List claims) {
        List rows = new ArrayList();
        for (Iterator i = claims.iterator(); i.hasNext();) {
            Claim c = (Claim) i.next();
            Map row = new TreeMap();
            row.put("claimId", Integer.valueOf(c.getClaimId()));
            row.put("claimNumber", c.getClaimNumber());
            row.put("reportedDate", date(c.getReportedDate()));
            row.put("status", c.getStatus());
            row.put("reserveAmount", new Double(c.getReserveAmount()));
            row.put("assignedAdjuster", c.getAssignedAdjuster());
            rows.add(row);
        }
        return rows;
    }

    private static List policyRows(List policies) {
        List rows = new ArrayList();
        for (Iterator i = policies.iterator(); i.hasNext();) {
            Policy p = (Policy) i.next();
            Map row = new TreeMap();
            row.put("policyId", Integer.valueOf(p.getPolicyId()));
            row.put("policyNumber", p.getPolicyNumber());
            row.put("lineOfBusiness", p.getLineOfBusiness());
            row.put("annualPremium", new Double(p.getAnnualPremium()));
            row.put("status", p.getStatus());
            rows.add(row);
        }
        return rows;
    }

    private static String date(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Date) {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
            return f.format((java.util.Date) value);
        }
        return value.toString();
    }

    static String slug(String orderBy) {
        return orderBy.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+$", "");
    }
}
