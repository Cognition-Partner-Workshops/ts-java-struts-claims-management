package com.northstar.claims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.northstar.claims.util.ClaimsConfig;
import com.northstar.claims.util.DatabaseBootstrap;
import com.northstar.claims.util.ReportFormatter;

@RunWith(Parameterized.class)
public class ReportCharacterizationTest {
    private static Locale previousLocale;
    private static TimeZone previousZone;
    private static String previousPath;
    private final String file;
    private final String action;
    private final String collection;
    private final String columns;
    private final String adjuster;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> reports() {
        List<Object[]> cases = new ArrayList<Object[]>(Arrays.asList(new Object[][] {
            {"index", "ReportIndexAction", "", "", ""},
            {"open-by-adjuster", "OpenReportAction", "reportRows", "adjuster:text,openCount:integer,reserveTotal:money", ""},
            {"adjuster-workload", "AdjusterWorkloadDetailAction", "reportRows", "adjuster:text,openCount:integer,reserveTotal:money,oldestReportedDate:date", ""},
            {"loss-ratio", "LossRatioReportAction", "reportRows", "lineOfBusiness:text,premiumTotal:money,lossTotal:money,lossRatio:percent", ""},
            {"aged-claims", "AgedClaimsReportAction", "reportRows", "bucket:text,claimCount:integer,reserveTotal:money", ""},
            {"claim-aging-detail", "ClaimAgingDetailAction", "claims", "claimId:integer,reportedDate:date,status:text,reserveAmount:money,assignedAdjuster:text", ""},
            {"claim-inventory", "ClaimInventoryReportAction", "reportRows", "claimNumber:text,closedDate:date", ""},
            {"closed-claims", "ClosedClaimsReportAction", "reportRows", "claimNumber:text,closedDate:date", ""},
            {"reconciliation", "ReconciliationAction", "reportRows", "lineOfBusiness:text,premiumTotal:money,reserveTotal:money,paidTotal:money,variance:money", ""},
            {"premium-detail", "PremiumDetailAction", "policies", "policyId:integer,policyNumber:text,lineOfBusiness:text,annualPremium:money,status:text", ""}
        }));
        for (int i = 1; i <= 5; i++) {
            cases.add(new Object[] {"adjuster-claims-adjuster" + i, "AdjusterClaimsReportAction", "reportRows",
                    "claimNumber:text,status:text,reserveAmount:money", "adjuster" + i});
        }
        return cases;
    }

    public ReportCharacterizationTest(String file, String action, String collection,
            String columns, String adjuster) {
        this.file = file;
        this.action = action;
        this.collection = collection;
        this.columns = columns;
        this.adjuster = adjuster;
    }

    @BeforeClass
    public static void seed() throws Exception {
        previousLocale = Locale.getDefault();
        previousZone = TimeZone.getDefault();
        previousPath = System.getProperty("claims.db.path");
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // The pool retains its first URL for the lifetime of the test JVM.
        System.setProperty("claims.db.path", "target/db/test-northstar");
        DatabaseBootstrap.bootstrap(true);
        assertEquals("2019-04-01", ClaimsConfig.get("report.asof.date"));
    }

    @AfterClass
    public static void restore() {
        Locale.setDefault(previousLocale);
        TimeZone.setDefault(previousZone);
        if (previousPath == null) System.clearProperty("claims.db.path");
        else System.setProperty("claims.db.path", previousPath);
    }

    @Test
    public void matchesFrozenReport() throws Exception {
        InputStream input = getClass().getResourceAsStream("/characterization/reports/" + file + ".tsv");
        assertNotNull(file, input);
        String expected;
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
            expected = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } finally { input.close(); }
        assertEquals(file, expected, snapshot());
    }

    public String snapshot() throws Exception {
        final Map<String, Object> attributes = new TreeMap<String, Object>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[] {HttpServletRequest.class}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("setAttribute".equals(method.getName())) { attributes.put((String) args[0], args[1]); return null; }
                if ("getAttribute".equals(method.getName())) return attributes.get(args[0]);
                if ("getParameter".equals(method.getName())) return "adjuster".equals(args[0]) ? adjuster : null;
                return null;
            }
        });
        ActionMapping mapping = new ActionMapping();
        for (String name : new String[] {"report", "reportIndex", "adjusterWorkload", "claimAgingDetail", "reconciliation", "premiumDetail"}) {
            mapping.addForwardConfig(new ActionForward(name, "/" + name, false));
        }
        Action handler = (Action) Class.forName("com.northstar.claims.web." + action).newInstance();
        ActionForward forward = handler.execute(mapping, null, request, null);
        StringBuilder out = new StringBuilder("# report\t" + file + "\n# asOf\t" + ClaimsConfig.get("report.asof.date") + "\n");
        if ("closed-claims".equals(file) || "claim-inventory".equals(file)) {
            out.append("# suspect\tLegacy closed-claims count 23 versus inventory posted-closure count 21; preserved, not reconciled.\n");
        }
        out.append("forward\t").append(forward.getName()).append('\n');
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!(entry.getValue() instanceof List)) out.append("attribute\t").append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
        }
        if (collection.length() == 0) {
            String jsp = new String(Files.readAllBytes(Paths.get("src/main/webapp/WEB-INF/jsp/report/index.jsp")), StandardCharsets.UTF_8);
            Matcher links = Pattern.compile("href=\"([a-zA-Z][^\"]+\\.do[^\"]*)\"").matcher(jsp);
            int count = 0;
            while (links.find()) { out.append("link\t").append(links.group(1)).append('\n'); count++; }
            out.append("rows\t").append(count).append('\n');
            return out.toString();
        }
        List rows = (List) attributes.get(collection);
        assertNotNull(collection, rows);
        out.append("columns\t").append(columns).append('\n');
        double[] totals = new double[columns.split(",").length];
        for (Object row : rows) {
            if (row instanceof Map) out.append("raw\t").append(new TreeMap((Map) row)).append('\n');
            out.append("row");
            int i = 0;
            for (String column : columns.split(",")) {
                String[] spec = column.split(":");
                Object value = row instanceof Map ? ((Map) row).get(spec[0]) : PropertyUtils.getProperty(row, spec[0]);
                out.append('\t').append(format(value, spec[1]));
                if (value instanceof Number) totals[i] += ((Number) value).doubleValue();
                i++;
            }
            out.append('\n');
        }
        out.append("rows\t").append(rows.size()).append('\n');
        int i = 0;
        for (String column : columns.split(",")) {
            String[] spec = column.split(":");
            if ("money".equals(spec[1]) || ("integer".equals(spec[1]) && !spec[0].endsWith("Id"))) {
                out.append("total\t").append(spec[0]).append('\t').append(format(totals[i], spec[1])).append('\n');
            }
            i++;
        }
        return out.toString();
    }

    private String format(Object value, String type) {
        ReportFormatter formatter = new ReportFormatter();
        if ("money".equals(type)) return formatter.money(value);
        if ("percent".equals(type)) return formatter.percent(value);
        if (value == null) return "";
        if ("date".equals(type)) return formatter.date(Date.valueOf(value.toString()));
        if ("integer".equals(type)) return String.valueOf(((Number) value).intValue());
        return value.toString();
    }
}
