package com.northstar.claims.boot.report;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.northstar.claims.boot.ClaimsConfig;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the legacy report characterization (ReportDaoCharacterizationTest and
 * ReportScreenCharacterizationTest) against the Boot port: same queries, same screens,
 * same snapshot shape, compared with the unchanged goldens of the root project.
 */
abstract class ReportGoldenSupport {
    static final String[] ORDER_BY = {
        "status", "status desc", "count", "count desc", "count desc, status"
    };

    static final String[] ADJUSTERS = {"adjuster1", "adjuster5", "adjuster9", "nobody"};

    static final String[][] SCREENS = {
        {"index", "/report/index.do"},
        {"open-by-adjuster", "/report/openByAdjuster.do"},
        {"loss-ratio-by-line", "/report/lossRatio.do"},
        {"aged-claims", "/report/agedClaims.do"},
        {"claim-inventory", "/report/claimInventory.do"},
        {"closed-claims", "/report/closedClaims.do"},
        {"adjuster-workload", "/report/adjusterWorkload.do"},
        {"adjuster-claims-adjuster1", "/report/adjusterClaims.do?adjuster=adjuster1"},
        {"adjuster-claims-adjuster9", "/report/adjusterClaims.do?adjuster=adjuster9"},
        {"adjuster-claims-nobody", "/report/adjusterClaims.do?adjuster=nobody"},
        {"claim-aging-detail", "/report/claimAgingDetail.do"},
        {"premium-detail", "/report/premiumDetail.do"},
        {"reconciliation", "/report/reconciliation.do"},
    };

    private static final Pattern VIEW = Pattern.compile("<!--\\s*ns:view\\s+(\\S+)\\s*-->");
    private static final Pattern FIELD = Pattern.compile("<span id=\"f_([^\"]+)\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern TABLE = Pattern.compile(
            "<table class=\"(results|detail)\"[^>]*>(.*?)</table>", Pattern.DOTALL);
    private static final Pattern ROW = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
    private static final Pattern CELL = Pattern.compile("<t([hd])[^>]*>(.*?)</t[hd]>", Pattern.DOTALL);
    private static final Pattern HEADING = Pattern.compile("<h2>(.*?)</h2>", Pattern.DOTALL);

    @Autowired
    private ReportDao dao;

    @Autowired
    private ClaimsConfig config;

    @LocalServerPort
    private int port;

    private HttpClient client;

    /** Same JVM defaults the legacy characterization pins (ReportFixtures.pinPlatform). */
    @BeforeAll
    static void pinPlatform() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    void characterize(String state) throws Exception {
        String asOf = config.get("report.asof.date");
        String dir = state + "/dao/";
        Golden.verify(dir + "open-by-adjuster.json",
                snapshot("ReportDAO.openClaimsByAdjuster()", asOf, dao.openClaimsByAdjuster()));
        Golden.verify(dir + "loss-ratio-by-line.json",
                snapshot("ReportDAO.lossRatioByLine()", asOf, dao.lossRatioByLine()));
        Golden.verify(dir + "aged-claims.json",
                snapshot("ReportDAO.agedClaims()", asOf, dao.agedClaims(asOf)));
        for (String orderBy : ORDER_BY) {
            Golden.verify(dir + "claim-inventory-" + slug(orderBy) + ".json",
                    snapshot("ReportDAO.claimCounts(\"" + orderBy + "\")", asOf, dao.claimCounts(orderBy)));
        }
        Golden.verify(dir + "closed-claims.json",
                snapshot("ReportDAO.closedClaimsThrough(\"" + asOf + "\")", asOf, dao.closedClaimsThrough(asOf)));
        Golden.verify(dir + "closures-posted.json",
                snapshot("ReportDAO.closuresPostedThrough(\"" + asOf + "\")", asOf, dao.closuresPostedThrough(asOf)));
        for (String adjuster : ADJUSTERS) {
            Golden.verify(dir + "adjuster-claims-" + adjuster + ".json",
                    snapshot("ReportDAO.claimsForAdjuster(\"" + adjuster + "\")", asOf,
                            dao.claimsForAdjuster(adjuster)));
        }
        Golden.verify(dir + "adjuster-workload.json",
                snapshot("ReportDAO.openClaimsByAdjuster() via AdjusterWorkloadDetailAction", asOf,
                        dao.openClaimsByAdjuster()));
        Golden.verify(dir + "claim-aging-detail.json",
                snapshot("ClaimDAO.findByStatus(\"OPEN\") via ClaimAgingDetailAction", asOf,
                        dao.claimsByStatus("OPEN")));
        Golden.verify(dir + "premium-detail.json",
                snapshot("PolicyDAO.findAll() via PremiumDetailAction", asOf, dao.policies()));
        Golden.verify(dir + "reconciliation.json",
                snapshot("ReportDAO.lossRatioByLine() via ReconciliationAction", asOf, dao.lossRatioByLine()));

        login();
        for (String[] screen : SCREENS) {
            Golden.verify(state + "/screens/" + screen[0] + ".json", render(screen[1]));
        }
    }

    private static Map<String, Object> snapshot(String source, String asOf, List<?> rows) {
        Map<String, Object> out = new TreeMap<>();
        out.put("source", source);
        out.put("asOf", asOf);
        out.put("rows", rows);
        out.put("totals", Golden.totals(rows));
        return out;
    }

    private static String slug(String orderBy) {
        return orderBy.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+$", "");
    }

    private String base() {
        return "http://127.0.0.1:" + port + "/claims";
    }

    private void login() throws Exception {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base() + "/login.do"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=supervisor&password=supervisor"))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private Map<String, Object> render(String path) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base() + path)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String html = response.body().replace("\r\n", "\n");
        Map<String, Object> out = new TreeMap<>();
        out.put("request", "GET " + path);
        out.put("status", response.statusCode());
        Matcher view = VIEW.matcher(html);
        out.put("view", view.find() ? view.group(1) : null);
        Matcher heading = HEADING.matcher(html);
        out.put("heading", heading.find() ? text(heading.group(1)) : null);
        List<Object> fields = new ArrayList<>();
        Matcher f = FIELD.matcher(html);
        while (f.find()) {
            fields.add(Arrays.asList(f.group(1), unescape(f.group(2))));
        }
        out.put("fields", fields);
        out.put("tables", tables(html));
        return out;
    }

    private static List<Object> tables(String html) {
        List<Object> tables = new ArrayList<>();
        Matcher t = TABLE.matcher(html);
        while (t.find()) {
            Map<String, Object> table = new TreeMap<>();
            table.put("class", t.group(1));
            List<Object> rows = new ArrayList<>();
            Matcher r = ROW.matcher(t.group(2));
            while (r.find()) {
                List<Object> cells = new ArrayList<>();
                Matcher cell = CELL.matcher(r.group(1));
                while (cell.find()) {
                    cells.add(text(cell.group(2)));
                }
                rows.add(cells);
            }
            table.put("rows", rows);
            tables.add(table);
        }
        return tables;
    }

    private static String text(String html) {
        return unescape(html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&amp;", "&");
    }
}
