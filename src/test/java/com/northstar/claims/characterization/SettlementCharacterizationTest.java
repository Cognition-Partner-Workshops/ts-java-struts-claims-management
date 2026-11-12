package com.northstar.claims.characterization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
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
import com.northstar.claims.model.Settlement;
import com.northstar.claims.service.SettlementCalculator;
import com.northstar.claims.service.SettlementService;
import com.northstar.claims.util.DatabaseBootstrap;
import com.northstar.claims.web.SettlementCalculateAction;
import com.northstar.claims.web.form.SettlementForm;

/**
 * Replays characterization/settlement.csv against the legacy settlement code.
 * See characterization/settlement.md for the column and token conventions.
 */
@RunWith(Parameterized.class)
public class SettlementCharacterizationTest {

    private static final String TABLE = "/characterization/settlement.csv";

    private final Map<String, String> row;

    public SettlementCharacterizationTest(String id, Map<String, String> row) {
        this.row = row;
    }

    @BeforeClass
    public static void seed() throws Exception {
        System.setProperty("claims.db.path", "target/db/test-northstar");
        DatabaseBootstrap.bootstrap(true);
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> rows() throws Exception {
        List<Object[]> rows = new ArrayList<Object[]>();
        InputStream in = SettlementCharacterizationTest.class
                .getResourceAsStream(TABLE);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, "UTF-8"));
        try {
            List<String> header = split(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                List<String> cells = split(line);
                assertEquals("column count for " + line, header.size(),
                        cells.size());
                Map<String, String> row = new LinkedHashMap<String, String>();
                for (int i = 0; i < header.size(); i++) {
                    row.put(header.get(i), cells.get(i));
                }
                rows.add(new Object[] {row.get("id"), row});
            }
        } finally {
            reader.close();
        }
        return rows;
    }

    @Test
    public void matchesLegacyBehaviour() throws Exception {
        String layer = row.get("layer");
        if ("calculator".equals(layer)) {
            checkCalculator();
        } else if ("screen".equals(layer)) {
            checkScreen();
        } else if ("service".equals(layer)) {
            checkService();
        } else {
            fail("unknown layer " + layer);
        }
    }

    private void checkCalculator() {
        Settlement result;
        try {
            result = SettlementCalculator.getInstance().calculate(
                    number("covered_amount"), input("deductible"),
                    number("depreciation"), number("policy_limit"));
        } catch (RuntimeException failure) {
            assertError(failure);
            return;
        }
        assertOutcome("ok");
        assertSettlement(result);
    }

    private void checkScreen() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        Map<String, String[]> formValues = new HashMap<String, String[]>();
        put(params, formValues, "claimId", input("claim_id"));
        put(params, formValues, "coveredAmount", input("covered_amount"));
        put(params, formValues, "deductible", input("deductible"));
        put(params, formValues, "depreciation", input("depreciation"));
        SettlementForm form = new SettlementForm();
        BeanUtils.populate(form, formValues);

        Map<String, Object> requestAttributes = new HashMap<String, Object>();
        Map<String, Object> sessionAttributes = new HashMap<String, Object>();
        if (input("user") != null) {
            sessionAttributes.put("user", input("user"));
        }
        HttpSession session = (HttpSession) proxy(HttpSession.class,
                params, sessionAttributes, null);
        HttpServletRequest request = (HttpServletRequest) proxy(
                HttpServletRequest.class, params, requestAttributes, session);

        ActionForward forward;
        try {
            forward = new SettlementCalculateAction().execute(mapping(), form,
                    request, null);
        } catch (RuntimeException failure) {
            assertError(failure);
            return;
        }
        assertOutcome(forward.getName());
        if ("refused".equals(forward.getName())) {
            assertMoney("expected_amount",
                    (Double) requestAttributes.get("refusedAmount"));
            assertMoney("expected_approval_limit",
                    (Double) requestAttributes.get("approvalLimit"));
            assertTrue(requestAttributes.containsKey("nsValidationErrors"));
            assertTrue(!sessionAttributes.containsKey("pendingSettlement"));
            return;
        }
        assertSettlement((Settlement) requestAttributes.get("settlement"));
        assertMoney("expected_policy_limit",
                (Double) requestAttributes.get("policyLimit"));
    }

    private void checkService() throws Exception {
        Settlement result;
        try {
            result = new SettlementService().calculateAndSave(
                    Integer.parseInt(input("claim_id")),
                    number("covered_amount"), input("deductible"),
                    number("depreciation"), number("policy_limit"));
        } catch (RuntimeException failure) {
            assertError(failure);
            return;
        }
        assertOutcome("ok");
        assertSettlement(result);
        Map<String, String> extra = extra();
        if (extra.containsKey("settlement_id")) {
            assertEquals(id("settlement_id"), extra.get("settlement_id"),
                    String.valueOf(result.getSettlementId()));
        }
        if (extra.containsKey("calculated_by")) {
            assertEquals(id("calculated_by"), extra.get("calculated_by"),
                    result.getCalculatedBy());
        }
        if (extra.containsKey("calculated_date")) {
            assertEquals(id("calculated_date"), extra.get("calculated_date"),
                    result.getCalculatedDate());
        }
    }

    private void assertSettlement(Settlement result) {
        assertMoney("expected_amount",
                Double.valueOf(result.getSettlementAmount()));
        assertMoney("expected_deductible_applied",
                Double.valueOf(result.getDeductibleApplied()));
        String capped = row.get("expected_capped");
        if (capped.length() > 0) {
            assertEquals(id("expected_capped"), Boolean.valueOf(capped),
                    Boolean.valueOf(result.isCappedAtLimit()));
        }
    }

    private void assertMoney(String column, Double actual) {
        String expected = row.get(column);
        if (expected.length() == 0) {
            return;
        }
        if (actual == null) {
            fail(id(column) + " expected " + expected + " but was absent");
        }
        BigDecimal actualValue = BigDecimal.valueOf(actual.doubleValue());
        if (new BigDecimal(expected).compareTo(actualValue) != 0) {
            fail(id(column) + " expected " + expected + " but was "
                    + actualValue.toPlainString());
        }
    }

    private void assertError(RuntimeException failure) {
        String expected = row.get("expected_outcome");
        String actual = "error:" + failure.getClass().getSimpleName();
        if (!expected.equals(actual)) {
            throw failure;
        }
    }

    private void assertOutcome(String actual) {
        assertEquals(id("expected_outcome"), row.get("expected_outcome"),
                actual);
    }

    private String id(String column) {
        return row.get("id") + " " + column;
    }

    private Map<String, String> extra() {
        Map<String, String> values = new HashMap<String, String>();
        String text = row.get("expected_extra");
        if (text.length() == 0) {
            return values;
        }
        for (String pair : text.split(";")) {
            int split = pair.indexOf('=');
            values.put(pair.substring(0, split), pair.substring(split + 1));
        }
        return values;
    }

    /** Cell text with tokens decoded; null for an empty cell. */
    private String input(String column) {
        String value = row.get(column);
        if (value.length() == 0) {
            return null;
        }
        if ("(blank)".equals(value)) {
            return "";
        }
        if ("(spaces)".equals(value)) {
            return "   ";
        }
        return value;
    }

    private double number(String column) {
        return Double.parseDouble(input(column));
    }

    private static void put(Map<String, String> params,
            Map<String, String[]> formValues, String name, String value) {
        if (value != null) {
            params.put(name, value);
            formValues.put(name, new String[] {value});
        }
    }

    private static ActionMapping mapping() {
        ActionMapping mapping = new ActionMapping();
        mapping.addForwardConfig(new ActionForward("settlement",
                "/WEB-INF/jsp/settlement/calculate.jsp", false));
        mapping.addForwardConfig(new ActionForward("refused",
                "/WEB-INF/jsp/settlement/refused.jsp", false));
        return mapping;
    }

    private static Object proxy(Class<?> type, final Map<String, String> params,
            final Map<String, Object> attributes, final HttpSession session) {
        return Proxy.newProxyInstance(
                SettlementCharacterizationTest.class.getClassLoader(),
                new Class<?>[] {type}, new InvocationHandler() {
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

    /** Splits one CSV line; double-quoted cells may contain commas. */
    static List<String> split(String line) {
        List<String> cells = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }
}
