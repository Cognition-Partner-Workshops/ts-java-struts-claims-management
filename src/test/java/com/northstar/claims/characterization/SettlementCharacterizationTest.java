package com.northstar.claims.characterization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.northstar.claims.dao.ClaimDAO;
import com.northstar.claims.dao.ConnectionPool;
import com.northstar.claims.dao.PolicyDAO;
import com.northstar.claims.dao.SettlementDAO;
import com.northstar.claims.fixture.AdjusterFixtureBuilder;
import com.northstar.claims.model.Claim;
import com.northstar.claims.model.Settlement;
import com.northstar.claims.service.SettlementCalculator;
import com.northstar.claims.service.SettlementService;
import com.northstar.claims.util.DatabaseBootstrap;
import com.northstar.claims.web.SettlementAuthorityAction;
import com.northstar.claims.web.SettlementCalculateAction;
import com.northstar.claims.web.SettlementSaveAction;
import com.northstar.claims.web.form.SettlementForm;

/**
 * Replays characterization/settlement-cases.csv against the legacy settlement
 * calculator, service and Struts actions. See the table's README for the
 * column contract.
 */
@RunWith(Parameterized.class)
public class SettlementCharacterizationTest {

    static final String TABLE = "/characterization/settlement-cases.csv";
    static final String ABSENT = "~";
    static final String NO_ADJUSTER = "none";
    static final String CHARACTERIZATION_USER = "char_unknown";

    private static final int FIRST_ADJUSTER_ID = 950;

    private final Map<String, String> row;

    public SettlementCharacterizationTest(String caseId, Map<String, String> row) {
        this.row = row;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> cases() throws Exception {
        List<Object[]> cases = new ArrayList<Object[]>();
        for (Map<String, String> row : readTable()) {
            cases.add(new Object[] {row.get("case_id"), row});
        }
        return cases;
    }

    @BeforeClass
    public static void seed() throws Exception {
        System.setProperty("claims.db.path", "target/db/test-northstar");
        DatabaseBootstrap.bootstrap(true);
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
            new AdjusterFixtureBuilder().username(adjuster.getValue())
                    .approvalLimit(Double.parseDouble(adjuster.getKey()))
                    .insert(adjusterId++);
        }
        for (Map.Entry<Integer, String> claim : fixtureClaims.entrySet()) {
            insertClaim(claim.getKey().intValue(),
                    Double.parseDouble(claim.getValue()));
        }
    }

    @Test
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
            observed.settlement = new SettlementService().calculateAndSave(
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
        Map<String, Object> attributes = new HashMap<String, Object>();
        HttpServletRequest request = request(params, attributes, session);
        ActionMapping mapping = mapping(step);
        SettlementForm form = new SettlementForm();
        form.reset(mapping, request);
        Map<String, String[]> submitted = new HashMap<String, String[]>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            submitted.put(param.getKey(), new String[] {param.getValue()});
        }
        BeanUtils.populate(form, submitted);
        SettlementAuthorityAction action = "calculate".equals(step)
                ? (SettlementAuthorityAction) new SettlementCalculateAction()
                : new SettlementSaveAction();
        Observed observed = new Observed();
        observed.action = step;
        List<Integer> before = settlementIds();
        try {
            ActionForward forward = action.execute(mapping, form, request, null);
            observed.refused = Boolean.valueOf("refused".equals(forward.getName()));
            if (observed.refused.booleanValue()) {
                observed.refusedAmount = (Double) attributes.get("refusedAmount");
                assertTrue(caseId() + " refusal error key",
                        ((List) attributes.get("nsValidationErrors"))
                                .contains("errors.settlement.authority"));
            } else {
                observed.settlement = (Settlement) attributes.get("settlement");
            }
        } catch (Exception failure) {
            observed.error = failure;
        }
        recordWrites(observed, before);
        return observed;
    }

    private void verify(int step, Observed observed, String actor,
            Map<String, Object> session) throws Exception {
        String where = caseId() + " step " + (step + 1);
        String expectedError = expected("expected_error", step);
        if (expectedError.length() > 0 || observed.error != null) {
            String actual = observed.error == null ? ""
                    : observed.error.getClass().getSimpleName();
            if (!expectedError.equals(actual) && observed.error != null) {
                observed.error.printStackTrace();
            }
            assertEquals(where + " error", expectedError, actual);
        }
        String expectedRefused = expected("expected_refused", step);
        if (expectedRefused.length() > 0) {
            assertEquals(where + " refused", Boolean.valueOf(expectedRefused),
                    observed.refused);
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
        if ("action".equals(row.get("scope")) && observed.error == null
                && !observed.refused.booleanValue()) {
            Object pending = session.get(
                    SettlementAuthorityAction.PENDING_SETTLEMENT);
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

    private static List<Integer> settlementIds() throws Exception {
        List<Integer> ids = new ArrayList<Integer>();
        for (Object row : new SettlementDAO().findAll()) {
            ids.add(Integer.valueOf(((Settlement) row).getSettlementId()));
        }
        return ids;
    }

    private static void recordWrites(Observed observed, List<Integer> before)
            throws Exception {
        for (Object row : new SettlementDAO().findAll()) {
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
        Claim claim = new ClaimDAO().findById(claimId);
        if ("absent".equals(setup)) {
            assertNull(caseId() + " claim " + claimId + " must not exist", claim);
            return;
        }
        assertNotNull(caseId() + " claim " + claimId + " must exist", claim);
        assertAmount(caseId() + " policy limit of claim " + claimId,
                row.get("policy_limit"),
                new PolicyDAO().findById(claim.getPolicyId()).getPolicyLimit());
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

    private static void insertClaim(int id, double policyLimit) throws Exception {
        Connection connection = ConnectionPool.getInstance().getConnection();
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
            ConnectionPool.getInstance().release(connection);
        }
    }

    private static ActionMapping mapping(String step) {
        ActionMapping mapping = new ActionMapping();
        mapping.addForwardConfig(new ActionForward("settlement",
                "/WEB-INF/jsp/settlement/" + step + ".jsp", false));
        mapping.addForwardConfig(new ActionForward("refused",
                "/WEB-INF/jsp/settlement/refused.jsp", false));
        return mapping;
    }

    private static HttpServletRequest request(final Map<String, String> params,
            Map<String, Object> attributes, Map<String, Object> session) {
        HttpSession httpSession = (HttpSession) proxy(HttpSession.class,
                params, session, null);
        return (HttpServletRequest) proxy(HttpServletRequest.class, params,
                attributes, httpSession);
    }

    private static Object proxy(Class type, final Map<String, String> params,
            final Map<String, Object> attributes, final HttpSession session) {
        return Proxy.newProxyInstance(
                SettlementCharacterizationTest.class.getClassLoader(),
                new Class[] {type}, new InvocationHandler() {
            public Object invoke(Object target, Method method, Object[] args) {
                String name = method.getName();
                if ("getParameter".equals(name)) {
                    return params.get(args[0]);
                }
                if ("getSession".equals(name)) {
                    return session;
                }
                if ("getAttribute".equals(name)) {
                    return attributes.get(args[0]);
                }
                if ("setAttribute".equals(name)) {
                    attributes.put((String) args[0], args[1]);
                    return null;
                }
                if ("removeAttribute".equals(name)) {
                    attributes.remove(args[0]);
                    return null;
                }
                return null;
            }
        });
    }

    public static List<Map<String, String>> readTable() throws Exception {
        InputStream stream = SettlementCharacterizationTest.class
                .getResourceAsStream(TABLE);
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
                rows.add(row);
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
        Boolean refused;
        Double refusedAmount;
        Exception error;
        int rowsWritten;
        Settlement stored;

        double amount() {
            if (refusedAmount != null) {
                return refusedAmount.doubleValue();
            }
            if (settlement == null) {
                throw new AssertionError("No settlement amount was produced");
            }
            return settlement.getSettlementAmount();
        }
    }
}
