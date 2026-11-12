package com.northstar.claims.boot.report;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Fixed seed only, against the "baseline" goldens. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/report-baseline")
class ReportBaselineGoldenTest extends ReportGoldenSupport {
    @Test
    void baseline() throws Exception {
        characterize("baseline");
    }
}
