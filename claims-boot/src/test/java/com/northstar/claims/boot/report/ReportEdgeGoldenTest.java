package com.northstar.claims.boot.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Seed plus the legacy ReportFixtures boundary rows, against the "edge" goldens. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/report-edge")
class ReportEdgeGoldenTest extends ReportGoldenSupport {
    /** Copied verbatim from the legacy characterization ReportFixtures.EDGE_ROWS. */
    static final String[] EDGE_ROWS = {
        // Zero-premium lines for loss ratio: one with loss, one without.
        "INSERT INTO POLICY VALUES (9101, 'NS-09101', 'ZERO_PREMIUM', 'Zero Premium Policy', '9101 Edge Road', '2018-01-01', '2019-12-31', 50000, 0, 0, 'ACTIVE')",
        "INSERT INTO POLICY VALUES (9102, 'NS-09102', 'ZERO_PREMIUM_NO_LOSS', 'Zero Premium No Loss', '9102 Edge Road', '2018-01-01', '2019-12-31', 50000, 0, 0, 'ACTIVE')",
        "INSERT INTO POLICY VALUES (9103, 'NS-09103', 'EDGE_LINE', 'Edge Line Policy', '9103 Edge Road', '2018-01-01', '2019-12-31', 50000, 250, 1000, 'ACTIVE')",
        "INSERT INTO CLAIM VALUES (9101, 'CLM-09101', 9101, 'Zero Premium Claimant', '2019-03-20', '2019-03-25', 'FIRE', 'Loss on a zero-premium line', 'OPEN', 2500, 'adjuster9', 'supervisor', '2019-03-25')",
        "INSERT INTO SETTLEMENT VALUES (9101, 9101, 400, 0, 0, FALSE, 400, 'adjuster9', '2019-03-26')",
        "INSERT INTO PAYMENT VALUES (9101, 9101, 9101, 'Zero Premium Claimant', 400, 'CHECK', 'CHK-09101', '2019-03-27', 'ISSUED')",
        // Closures around the as-of date: the day before, on it, and the day after.
        "INSERT INTO CLAIM VALUES (9110, 'CLM-09110', 9103, 'Closed Day Before', '2019-02-01', '2019-02-02', 'WATER', 'Closed the day before the as-of date', 'CLOSED', 0, 'adjuster9', 'supervisor', '2019-02-02')",
        "INSERT INTO CLAIM VALUES (9111, 'CLM-09111', 9103, 'Closed On As Of', '2019-02-01', '2019-02-02', 'WATER', 'Closed on the as-of date', 'CLOSED', 0, 'adjuster9', 'supervisor', '2019-02-02')",
        "INSERT INTO CLAIM VALUES (9112, 'CLM-09112', 9103, 'Closed Day After', '2019-02-01', '2019-02-02', 'WATER', 'Closed the day after the as-of date', 'CLOSED', 0, 'adjuster9', 'supervisor', '2019-02-02')",
        "INSERT INTO CLAIM_CLOSURE VALUES (9110, '2019-03-31', 'adjuster9')",
        "INSERT INTO CLAIM_CLOSURE VALUES (9111, '2019-04-01', 'adjuster9')",
        "INSERT INTO CLAIM_CLOSURE VALUES (9112, '2019-04-02', 'adjuster9')",
        // Aging bucket boundaries: age in days = as-of date minus reported date.
        "INSERT INTO CLAIM VALUES (9120, 'CLM-09120', 9103, 'Age Minus 1', '2019-04-01', '2019-04-02', 'FIRE', 'Reported after the as-of date', 'OPEN', 100.25, 'adjuster9', 'supervisor', '2019-04-02')",
        "INSERT INTO CLAIM VALUES (9121, 'CLM-09121', 9103, 'Age 0', '2019-04-01', '2019-04-01', 'FIRE', 'Reported on the as-of date', 'OPEN', 200.50, 'adjuster9', 'supervisor', '2019-04-01')",
        "INSERT INTO CLAIM VALUES (9122, 'CLM-09122', 9103, 'Age 30', '2019-03-01', '2019-03-02', 'FIRE', 'Age 30 days', 'OPEN', 300.75, 'adjuster9', 'supervisor', '2019-03-02')",
        "INSERT INTO CLAIM VALUES (9123, 'CLM-09123', 9103, 'Age 31', '2019-03-01', '2019-03-01', 'FIRE', 'Age 31 days', 'INVESTIGATING', 400.99, 'adjuster9', 'supervisor', '2019-03-01')",
        "INSERT INTO CLAIM VALUES (9124, 'CLM-09124', 9103, 'Age 60', '2019-01-30', '2019-01-31', 'FIRE', 'Age 60 days', 'OPEN', 500.01, 'adjuster9', 'supervisor', '2019-01-31')",
        "INSERT INTO CLAIM VALUES (9125, 'CLM-09125', 9103, 'Age 61', '2019-01-30', '2019-01-30', 'FIRE', 'Age 61 days', 'APPROVED', 600.50, 'adjuster9', 'supervisor', '2019-01-30')",
        "INSERT INTO CLAIM VALUES (9126, 'CLM-09126', 9103, 'Age 90', '2018-12-31', '2019-01-01', 'FIRE', 'Age 90 days', 'OPEN', 700.49, 'adjuster9', 'supervisor', '2019-01-01')",
        "INSERT INTO CLAIM VALUES (9127, 'CLM-09127', 9103, 'Age 91', '2018-12-31', '2018-12-31', 'FIRE', 'Age 91 days', 'OPEN', 800.51, 'adjuster9', 'supervisor', '2018-12-31')",
    };

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void edge() throws Exception {
        for (String sql : EDGE_ROWS) {
            jdbc.update(sql);
        }
        characterize("edge");
    }
}
