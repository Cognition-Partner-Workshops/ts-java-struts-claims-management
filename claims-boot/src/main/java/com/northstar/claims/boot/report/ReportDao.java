package com.northstar.claims.boot.report;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Port of the legacy ReportDAO (plus the ClaimDAO.findByStatus and PolicyDAO.findAll reads the
 * detail reports use). SQL, row keys and value types (counts and totals as Double) are kept as
 * the characterization goldens pin them.
 */
@Repository
public class ReportDao {
    private final JdbcTemplate jdbc;

    public ReportDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> openClaimsByAdjuster() {
        return jdbc.query("select assigned_adjuster, count(*) as open_count, "
                + "coalesce(sum(reserve_amount),0) as reserve_total "
                + "from CLAIM where status in ('OPEN','INVESTIGATING') "
                + "group by assigned_adjuster order by assigned_adjuster", (rs, n) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("adjuster", rs.getString("assigned_adjuster"));
                    row.put("openCount", rs.getDouble("open_count"));
                    row.put("reserveTotal", rs.getDouble("reserve_total"));
                    return row;
                });
    }

    public List<Map<String, Object>> lossRatioByLine() {
        String sql = "select p.line_of_business, sum(p.annual_premium) premium_total,"
                + " coalesce(sum(c.reserve_amount),0) reserve_total,"
                + " coalesce(sum(pay.amount),0) paid_total"
                + " from POLICY p left join CLAIM c on p.policy_id = c.policy_id"
                + " left join PAYMENT pay on c.claim_id = pay.claim_id"
                + " group by p.line_of_business order by p.line_of_business";
        return jdbc.query(sql, (rs, n) -> {
            Map<String, Object> row = new HashMap<>();
            double premium = rs.getDouble("premium_total");
            double loss = rs.getDouble("reserve_total") + rs.getDouble("paid_total");
            row.put("lineOfBusiness", rs.getString("line_of_business"));
            row.put("premiumTotal", premium);
            row.put("lossTotal", loss);
            row.put("lossRatio", premium == 0 ? 0d : loss / premium);
            return row;
        });
    }

    public List<Map<String, Object>> agedClaims(String asOfDate) {
        String age = "datediff('day', reported_date, date '" + asOfDate + "')";
        String bucket = "case "
                + "when " + age + " <= 30 then '0_30' "
                + "when " + age + " <= 60 then '31_60' "
                + "when " + age + " <= 90 then '61_90' else '91_PLUS' end";
        return jdbc.query("select " + bucket + " as age_bucket, "
                + "count(*) as claim_count, "
                + "coalesce(sum(reserve_amount),0) as reserve_total "
                + "from CLAIM where status not in ('CLOSED','DENIED') "
                + "group by " + bucket + " order by age_bucket", (rs, n) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("bucket", rs.getString("age_bucket").trim());
                    row.put("claimCount", rs.getDouble("claim_count"));
                    row.put("reserveTotal", rs.getDouble("reserve_total"));
                    return row;
                });
    }

    /** Legacy ad-hoc ordering query; {@code orderBy} is appended as given, as in ReportDAO. */
    public List<Map<String, Object>> claimCounts(String orderBy) {
        return jdbc.query("select status, count(*) as count from CLAIM group by status order by "
                + orderBy, (rs, n) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("status", rs.getString("status"));
                    row.put("count", rs.getDouble("count"));
                    return row;
                });
    }

    public List<Map<String, Object>> claimsForAdjuster(String adjuster) {
        return jdbc.query("select claim_number, status, reserve_amount "
                + "from CLAIM where assigned_adjuster = ? order by claim_number", (rs, n) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("claimNumber", rs.getString("claim_number"));
                    row.put("status", rs.getString("status"));
                    row.put("reserveAmount", rs.getDouble("reserve_amount"));
                    return row;
                }, adjuster);
    }

    /** Closures recorded on or before the as-of date. */
    public List<Map<String, Object>> closedClaimsThrough(String asOfDate) {
        return closures("c.closed_date <= ?", asOfDate);
    }

    /** Closures counted from the day after they are recorded (month-end inventory rule). */
    public List<Map<String, Object>> closuresPostedThrough(String asOfDate) {
        return closures("dateadd('day', 1, c.closed_date) <= ?", asOfDate);
    }

    private List<Map<String, Object>> closures(String condition, String asOfDate) {
        return jdbc.query("select cl.claim_number, c.closed_date "
                + "from CLAIM_CLOSURE c join CLAIM cl on cl.claim_id = c.claim_id "
                + "where " + condition + " order by cl.claim_number", (rs, n) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("claimNumber", rs.getString("claim_number"));
                    row.put("closedDate", rs.getString("closed_date"));
                    return row;
                }, java.sql.Date.valueOf(asOfDate));
    }

    /** ClaimDAO.findByStatus as read by the aged claim detail report. */
    public List<Map<String, Object>> claimsByStatus(String status) {
        return jdbc.query("select * from CLAIM where status = ? order by claim_id",
                ReportDao::claimRow, status);
    }

    /** PolicyDAO.findAll as read by the premium detail report. */
    public List<Map<String, Object>> policies() {
        return jdbc.query("select * from POLICY order by policy_id", ReportDao::policyRow);
    }

    private static Map<String, Object> claimRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("claimId", rs.getInt("claim_id"));
        row.put("claimNumber", rs.getString("claim_number"));
        row.put("reportedDate", rs.getString("reported_date"));
        row.put("status", rs.getString("status"));
        row.put("reserveAmount", rs.getDouble("reserve_amount"));
        row.put("assignedAdjuster", rs.getString("assigned_adjuster"));
        return row;
    }

    private static Map<String, Object> policyRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("policyId", rs.getInt("policy_id"));
        row.put("policyNumber", rs.getString("policy_number"));
        row.put("lineOfBusiness", rs.getString("line_of_business"));
        row.put("annualPremium", rs.getDouble("annual_premium"));
        row.put("status", rs.getString("status"));
        return row;
    }
}
