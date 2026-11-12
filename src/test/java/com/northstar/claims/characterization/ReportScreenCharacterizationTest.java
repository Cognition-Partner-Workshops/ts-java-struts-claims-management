package com.northstar.claims.characterization;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Renders every report screen through the real Struts actions and JSPs on an
 * embedded Jetty and pins what the operator sees: the forwarded view, the
 * ns:field values and the cells of the results/detail tables.
 */
public class ReportScreenCharacterizationTest {

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

    private static Server server;
    private static String base;
    private static String cookie;

    @BeforeClass
    public static void start() throws Exception {
        ReportFixtures.pinPlatform();
        server = new Server(0);
        WebAppContext context = new WebAppContext();
        context.setContextPath("/claims");
        context.setResourceBase(new File("src/main/webapp").getAbsolutePath());
        context.setDescriptor(new File("src/main/webapp/WEB-INF/web.xml").getAbsolutePath());
        context.setParentLoaderPriority(true);
        context.setTempDirectory(new File("target/characterization-jetty"));
        context.setConfigurations(new Configuration[] {
            new WebInfConfiguration(), new WebXmlConfiguration(),
            new MetaInfConfiguration(), new FragmentConfiguration()});
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*(struts-taglib|standard|jstl|apache-jsp)[^/]*\\.jar$");
        context.setAttribute("org.eclipse.jetty.containerInitializers",
                Arrays.asList(new ContainerInitializer[] {
                    new ContainerInitializer(new JettyJasperInitializer(), null)}));
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        context.addBean(new ServletContainerInitializersStarter(context), true);
        context.setThrowUnavailableOnStartupException(true);
        server.setHandler(context);
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        base = "http://127.0.0.1:" + port + "/claims";
    }

    @AfterClass
    public static void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

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
        login();
        for (int i = 0; i < SCREENS.length; i++) {
            Golden.verify(state + "/screens/" + SCREENS[i][0] + ".json", render(SCREENS[i][1]));
        }
    }

    private void login() throws Exception {
        HttpURLConnection c = open("/login.do");
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream out = c.getOutputStream();
        out.write("username=supervisor&password=supervisor".getBytes("UTF-8"));
        out.close();
        assertEquals(200, c.getResponseCode());
        String header = c.getHeaderField("Set-Cookie");
        if (header != null) {
            cookie = header.split(";", 2)[0];
        }
        body(c);
    }

    private HttpURLConnection open(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        c.setInstanceFollowRedirects(false);
        if (cookie != null) {
            c.setRequestProperty("Cookie", cookie);
        }
        return c;
    }

    private static String body(HttpURLConnection c) throws Exception {
        InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        try {
            return in == null ? "" : Golden.readFully(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private Map render(String path) throws Exception {
        HttpURLConnection c = open(path);
        int status = c.getResponseCode();
        String html = body(c).replace("\r\n", "\n");
        Map out = new TreeMap();
        out.put("request", "GET " + path);
        out.put("status", Integer.valueOf(status));
        Matcher view = VIEW.matcher(html);
        out.put("view", view.find() ? view.group(1) : null);
        Matcher heading = HEADING.matcher(html);
        out.put("heading", heading.find() ? text(heading.group(1)) : null);
        List fields = new ArrayList();
        Matcher f = FIELD.matcher(html);
        while (f.find()) {
            fields.add(Arrays.asList(new String[] {f.group(1), unescape(f.group(2))}));
        }
        out.put("fields", fields);
        List tables = new ArrayList();
        Matcher t = TABLE.matcher(html);
        while (t.find()) {
            Map table = new TreeMap();
            table.put("class", t.group(1));
            List rows = new ArrayList();
            Matcher r = ROW.matcher(t.group(2));
            while (r.find()) {
                List cells = new ArrayList();
                Matcher cell = CELL.matcher(r.group(1));
                while (cell.find()) {
                    cells.add(text(cell.group(2)));
                }
                rows.add(cells);
            }
            table.put("rows", rows);
            tables.add(table);
        }
        out.put("tables", tables);
        return out;
    }

    private static String text(String html) {
        return unescape(html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&amp;", "&");
    }
}
