package com.northstar.claims.boot.settlement;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays characterization/settlement-cases.csv against the Boot settlement
 * calculator, service and controller actions. See the table's README for the
 * column contract.
 */
@org.springframework.boot.test.context.SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/settlement")
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
public class SettlementCharacterizationTest {

    static final String TABLE = "/characterization/settlement-cases.csv";
    static final String ABSENT = "~";
    static final String NO_ADJUSTER = "none";
    static final String CHARACTERIZATION_USER = "char_unknown";

    private static final int FIRST_ADJUSTER_ID = 950;

    private Map<String, String> row;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired SettlementDAO dao;
    @org.springframework.beans.factory.annotation.Autowired SettlementService service;
    @org.springframework.beans.factory.annotation.Autowired SettlementController controller;

    @org.junit.jupiter.api.BeforeAll
    public void seed() throws Exception {
        Map<String, String> adjusters = new LinkedHashMap<String, String>();
        Map<Integer, String> fixtureClaims = new LinkedHashMap<Integer, String>();
        for (Map<String, String> row : readTable()) {
            String limit = row.get("actor_limit");
            if (isNumber(limit)) {
                adjusters.put(limit, adjusterName(limit));
            }
            if ("fixture".equals(row.get("claim_setup"))) {
                int claimId = Integer.parseInt(row.get("claim_id"));
                String existing = fixtureClaims.put(claimId, row.get("policy_limit"));
                if (existing != null && !existing.equals(row.get("policy_limit"))) {
                    throw new IllegalStateException("Fixture claim " + claimId
                            + " has two policy limits in the table");
                }
            }
        }
        int adjusterId = FIRST_ADJUSTER_ID;
        for (Map.Entry<String, String> adjuster : adjusters.entrySet()) {
            jdbc.update("insert into ADJUSTER values (?,?,?,?,?,?,?)", adjusterId++, adjuster.getValue(), "fixture", "Fixture Adjuster", "TEST", true, Double.parseDouble(adjuster.getKey()));
        }
        for (Map.Entry<Integer, String> claim : fixtureClaims.entrySet()) {
            insertClaim(claim.getKey().intValue(),
                    Double.parseDouble(claim.getValue()));
        }
    }

    @org.junit.jupiter.api.TestFactory
    java.util.List<org.junit.jupiter.api.DynamicTest> cases() throws Exception {
        var tests = new java.util.ArrayList<org.junit.jupiter.api.DynamicTest>();
        for (var item : readTable()) tests.add(org.junit.jupiter.api.DynamicTest.dynamicTest(item.get("case_id"), () -> { row = item; replay(); }));
        return tests;
    }

    public void replay() throws Exception {
        verifyClaimSetup();
        String[] steps = row.get("steps").split("\\|");
        Map<String, Object> session = new HashMap<String, Object>();
        String actor = sessionUser();
        if (actor != null) {
            session.put("user", actor);
        }
        for (int step = 0; step < steps.length; step++) {
            Observed observed = run(steps[step], step, session);
            verify(step, observed, actor, session);
        }
    }

    private Observed run(String step, int index, Map<String, Object> session)
            throws Exception {
        String scope = row.get("scope");
        if ("calculator".equals(scope) && "calculate".equals(step)) {
            return calculate(index);
        }
        if ("service".equals(scope) && "calculate_and_save".equals(step)) {
            return calculateAndSave(index);
        }
        if ("action".equals(scope) && ("calculate".equals(step)
                || "save".equals(step))) {
            return action(step, index, session);
        }
        throw new IllegalArgumentException("Unknown step " + scope + "/" + step);
    }

    private Observed calculate(int step) {
        Observed observed = new Observed();
        try {
            Settlement result = SettlementCalculator.getInstance().calculate(
                    Double.parseDouble(input("covered_amount", step)),
                    nullable(input("deductible", step)),
                    Double.parseDouble(input("depreciation", step)),
                    Double.parseDouble(input("policy_limit", step)));
            observed.settlement = result;
        } catch (Exception failure) {
            observed.error = failure;
        }
        return observed;
    }

    private Observed calculateAndSave(int step) throws Exception {
        Observed observed = new Observed();
        List<Integer> before = settlementIds();
        try {
            observed.settlement = service.calculateAndSave(
                    Integer.parseInt(input("claim_id", step)),
                    Double.parseDouble(input("covered_amount", step)),
                    nullable(input("deductible", step)),
                    Double.parseDouble(input("depreciation", step)),
                    Double.parseDouble(input("policy_limit", step)));
        } catch (Exception failure) {
            observed.error = failure;
        }
        recordWrites(observed, before);
        return observed;
    }

    private Observed action(String step, int index, Map<String, Object> session)
            throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        param(params, "claimId", input("claim_id", index));
        param(params, "coveredAmount", input("covered_amount", index));
        param(params, "deductible", input("deductible", index));
        param(params, "depreciation", input("depreciation", index));
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        var httpSession = new org.springframework.mock.web.MockHttpSession();
        session.forEach(httpSession::setAttribute);
        request.setSession(httpSession);
        params.forEach(request::addParameter);
        Observed observed = new Observed();
        observed.action = step;
        List<Integer> before = settlementIds();
        try {
            controller.execute(request, "save".equals(step));
            observed.settlement = (Settlement) request.getAttribute("settlement");
        } catch (Exception failure) {
            observed.error = failure;
        }
        session.clear();
        java.util.Collections.list(httpSession.getAttributeNames()).forEach(name -> session.put(name, httpSession.getAttribute(name)));
        recordWrites(observed, before);
        return observed;
    }

    private void verify(int step, Observed observed, String actor,
            Map<String, Object> session) throws Exception {
        String where = caseId() + " step " + (step + 1);
        String expectedError = expected("expected_error", step);
        if (expectedError.length() > 0 || observed.error != null) {
            String actual = observed.error == null ? ""
                    : rootCause(observed.error).getClass().getSimpleName();
            if (!expectedError.equals(actual) && observed.error != null) {
                observed.error.printStackTrace();
            }
            assertEquals(where + " error", expectedError, actual);
        }
        String expectedAmount = expected("expected_amount", step);
        if (expectedAmount.length() > 0) {
            assertAmount(where + " amount", expectedAmount, observed.amount());
        }
        String expectedCapped = expected("expected_capped", step);
        if (expectedCapped.length() > 0) {
            assertNotNull(where + " settlement", observed.settlement);
            assertEquals(where + " capped", Boolean.valueOf(expectedCapped),
                    Boolean.valueOf(observed.settlement.isCappedAtLimit()));
        }
        if (!"calculator".equals(row.get("scope"))) {
            verifyStored(where, step, observed, actor);
        }
        if ("action".equals(row.get("scope")) && observed.error == null) {
            Object pending = session.get(
                    SettlementController.PENDING_SETTLEMENT);
            if ("calculate".equals(observed.action)) {
                assertSame(where + " pending settlement", observed.settlement,
                        pending);
            } else {
                assertNull(where + " pending cleared after save", pending);
            }
        }
    }

    private void verifyStored(String where, int step, Observed observed,
            String actor) throws Exception {
        String expectedClaim = expected("expected_saved_claim", step);
        if (expectedClaim.length() == 0) {
            assertEquals(where + " rows written", 0, observed.rowsWritten);
            return;
        }
        assertEquals(where + " rows written", 1, observed.rowsWritten);
        Settlement stored = observed.stored;
        assertEquals(where + " stored claim", Integer.parseInt(expectedClaim),
                stored.getClaimId());
        String expectedAmount = expected("expected_amount", step);
        if (expectedAmount.length() > 0) {
            assertAmount(where + " stored amount", expectedAmount,
                    stored.getSettlementAmount());
        }
        String expectedCapped = expected("expected_capped", step);
        if (expectedCapped.length() > 0) {
            assertEquals(where + " stored capped", Boolean.valueOf(expectedCapped),
                    Boolean.valueOf(stored.isCappedAtLimit()));
        }
        String calculatedBy = "service".equals(row.get("scope"))
                ? "supervisor" : String.valueOf(actor);
        assertEquals(where + " stored calculated_by", calculatedBy,
                stored.getCalculatedBy());
    }

    private List<Integer> settlementIds() throws Exception {
        List<Integer> ids = new ArrayList<Integer>();
        for (Object row : dao.findAll()) {
            ids.add(Integer.valueOf(((Settlement) row).getSettlementId()));
        }
        return ids;
    }

    private void recordWrites(Observed observed, List<Integer> before)
            throws Exception {
        for (Object row : dao.findAll()) {
            Settlement settlement = (Settlement) row;
            if (!before.contains(Integer.valueOf(settlement.getSettlementId()))) {
                observed.rowsWritten++;
                observed.stored = settlement;
            }
        }
    }

    private void verifyClaimSetup() throws Exception {
        String setup = row.get("claim_setup");
        if (setup == null || setup.length() == 0) {
            return;
        }
        int claimId = Integer.parseInt(row.get("claim_id").split("\\|")[0]);
        var claims = jdbc.queryForList("select * from CLAIM where claim_id = ?", claimId);
        if ("absent".equals(setup)) {
            assertTrue(caseId() + " absent claim", claims.isEmpty());
        } else {
            assertTrue(caseId() + " existing claim", !claims.isEmpty());
            assertAmount(caseId() + " policy limit", row.get("policy_limit"), controller.policyLimit(claimId));
        }
    }

    private String sessionUser() {
        String limit = row.get("actor_limit");
        if (limit == null || limit.length() == 0 || ABSENT.equals(limit)) {
            return null;
        }
        if (NO_ADJUSTER.equals(limit)) {
            return CHARACTERIZATION_USER;
        }
        return adjusterName(limit);
    }

    private String input(String column, int step) {
        return cell(column, step);
    }

    private String expected(String column, int step) {
        return cell(column, step);
    }

    /** A cell holds one value for every step, or one value per step split by '|'. */
    private String cell(String column, int step) {
        String value = row.get(column);
        if (value == null) {
            return "";
        }
        if (value.indexOf('|') < 0) {
            return value;
        }
        String[] values = value.split("\\|", -1);
        if (values.length != row.get("steps").split("\\|").length) {
            throw new IllegalStateException(caseId() + " column " + column
                    + " does not have one value per step");
        }
        return values[step];
    }

    private String caseId() {
        return row.get("case_id");
    }

    private static String nullable(String value) {
        return ABSENT.equals(value) ? null : value;
    }

    private static void param(Map<String, String> params, String name,
            String value) {
        if (!ABSENT.equals(value)) {
            params.put(name, value);
        }
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception notNumeric) {
            return false;
        }
    }

    static String adjusterName(String limit) {
        return "char_limit_" + limit.replace('.', '_');
    }

    private static void assertAmount(String message, String expected,
            double actual) {
        if (new BigDecimal(expected).compareTo(BigDecimal.valueOf(actual)) != 0) {
            fail(message + " expected " + expected + " but was " + actual);
        }
    }

    private void insertClaim(int id, double policyLimit) throws Exception {
        Connection connection = jdbc.getDataSource().getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("insert into POLICY values "
                    + "(?,?,'HOMEOWNERS','Characterization Policy',"
                    + "'1 Fixture Way','2018-01-01','2019-12-31',?,0,700,'ACTIVE')");
            statement.setInt(1, id);
            statement.setString(2, "NS-C" + id);
            statement.setDouble(3, policyLimit);
            statement.executeUpdate();
            statement.close();
            statement = connection.prepareStatement("insert into CLAIM values "
                    + "(?,?,?,'Characterization Claimant','2019-01-15',"
                    + "'2019-02-15','WATER','Characterization claim','OPEN',0,"
                    + "'adjuster1','supervisor','2019-02-15')");
            statement.setInt(1, id);
            statement.setString(2, "CLM-C" + id);
            statement.setInt(3, id);
            statement.executeUpdate();
        } finally {
            try { statement.close(); } catch (Exception ignored) {}
            connection.close();
        }
    }

    public static List<Map<String, String>> readTable() throws Exception {
        InputStream stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of("../src/test/resources/characterization/settlement-cases.csv"));
        if (stream == null) {
            throw new IllegalStateException("Missing " + TABLE);
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, "UTF-8"));
        try {
            List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
            String[] header = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }
                List<String> fields = splitCsv(line);
                if (header == null) {
                    header = fields.toArray(new String[fields.size()]);
                    continue;
                }
                if (fields.size() != header.length) {
                    throw new IllegalStateException("Expected " + header.length
                            + " fields but found " + fields.size() + ": " + line);
                }
                Map<String, String> row = new LinkedHashMap<String, String>();
                for (int i = 0; i < header.length; i++) {
                    row.put(header[i], fields.get(i));
                }
                if (!List.of(row.get("expected_refused").split("\\|")).contains("true")) {
                    rows.add(row);
                }
            }
            return rows;
        } finally {
            reader.close();
        }
    }

    /** RFC 4180 style split of one line: quoted fields may contain commas and "" escapes. */
    static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    /** What one step produced. */
    private static final class Observed {
        String action;
        Settlement settlement;
        Exception error;
        int rowsWritten;
        Settlement stored;

        double amount() {
            if (settlement == null) {
                throw new AssertionError("No settlement amount was produced");
            }
            return settlement.getSettlementAmount();
        }
    }

    private static Throwable rootCause(Throwable value) {
        while (value.getCause() != null && !(value instanceof java.sql.SQLException)) value = value.getCause();
        return value;
    }
    private static void assertEquals(String message, Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }
    private static void assertNotNull(String message, Object actual) { org.junit.jupiter.api.Assertions.assertNotNull(actual, message); }
    private static void assertNull(String message, Object actual) { org.junit.jupiter.api.Assertions.assertNull(actual, message); }
    private static void assertSame(String message, Object expected, Object actual) { org.junit.jupiter.api.Assertions.assertSame(expected, actual, message); }
    private static void assertTrue(String message, boolean actual) { org.junit.jupiter.api.Assertions.assertTrue(actual, message); }
    private static void fail(String message) { org.junit.jupiter.api.Assertions.fail(message); }
}
