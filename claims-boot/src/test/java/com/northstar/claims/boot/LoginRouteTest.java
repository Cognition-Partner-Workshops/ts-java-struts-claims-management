package com.northstar.claims.boot;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Shared-foundation routes against legacy observed behavior (ROUTES.md, transcripts/). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginRouteTest {
    private static final Pattern FIELD = Pattern.compile("<span id=\"f_([^\"]+)\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern VIEW = Pattern.compile("<!--\\s*ns:view\\s+([^ ]+)\\s*-->");

    @Autowired
    private ServletWebServerApplicationContext context;

    @LocalServerPort
    private int port;

    private HttpClient client;

    @BeforeEach
    void newBrowser() {
        client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String username, String password)
            throws IOException, InterruptedException {
        String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static List<String> views(String body) {
        List<String> views = new ArrayList<>();
        Matcher matcher = VIEW.matcher(body);
        while (matcher.find()) {
            views.add(matcher.group(1));
        }
        return views;
    }

    private static Map<String, String> fields(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = FIELD.matcher(body);
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }
        return fields;
    }

    @Test
    void loginScreenUsesLegacyMessagesAndView() throws Exception {
        assertThat(context.getServletContext().getContextPath()).isEqualTo("/claims");
        HttpResponse<String> response = get("/claims/login.do?username=supervisor");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(views(response.body())).containsExactly("/WEB-INF/jsp/login.jsp");
        assertThat(response.body()).contains("<title>Claims Login</title>", "value=\"supervisor\"",
                "action=\"/claims/login.do\"", "href=\"/claims/northstar.css\"",
                "Use your assigned claims workstation credentials.", "value=\"Sign In\"");
        assertThat(fields(response.body())).isEmpty();
        assertThat(get("/claims/").body()).contains("<!-- ns:view /WEB-INF/jsp/login.jsp -->");
    }

    @Test
    void rejectedLoginsRedisplayLoginWithoutSigningIn() throws Exception {
        String[][] attempts = {{"supervisor", "wrong"}, {"", ""}, {"nobody", "legacy1"}, {"adjuster1", "x"}};
        for (String[] attempt : attempts) {
            HttpResponse<String> response = post("/claims/login.do", attempt[0], attempt[1]);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(views(response.body())).containsExactly("/WEB-INF/jsp/login.jsp");
            assertThat(response.body()).contains("value=\"" + attempt[0] + "\"");
        }
        assertThat(get("/claims/home.do").statusCode()).isEqualTo(302);
    }

    @Test
    void validLoginRendersHomeAndProtectsActions() throws Exception {
        HttpResponse<String> anonymous = get("/claims/home.do");
        assertThat(anonymous.statusCode()).isEqualTo(302);
        assertThat(anonymous.headers().firstValue("Location"))
                .hasValue("http://localhost:" + port + "/claims/login.do");

        HttpResponse<String> login = post("/claims/login.do", "supervisor", "supervisor");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(views(login.body())).containsExactly("/WEB-INF/jsp/home.jsp");
        assertThat(fields(login.body())).containsExactly(Map.entry("signedInUser", "supervisor"));

        HttpResponse<String> home = get("/claims/home.do");
        assertThat(home.statusCode()).isEqualTo(200);
        assertThat(home.body()).contains("<h1>NorthStar Claims</h1>", "Operator: supervisor",
                "Application version: 3.4.1 (build 20081117)", "<td>2019-04-01</td>",
                "<td class=\"footer\">NorthStar Claims Operations</td>",
                "<a href=\"/claims/logout.do\">Logout</a>");

        HttpResponse<String> logout = get("/claims/logout.do");
        assertThat(logout.statusCode()).isEqualTo(200);
        assertThat(views(logout.body())).containsExactly("/WEB-INF/jsp/login.jsp");
        assertThat(get("/claims/home.do").statusCode()).isEqualTo(302);
    }

    @Test
    void prefixCredentialsSignInAdjusters() throws Exception {
        HttpResponse<String> login = post("/claims/login.do", "adjusterUnknown", "legacyAnything");
        assertThat(fields(login.body())).containsExactly(Map.entry("signedInUser", "adjusterUnknown"));
        assertThat(get("/claims/home.do").body()).contains("<span id=\"f_signedInUser\">adjusterUnknown</span>");
    }

    @Test
    void adminModuleAndUnknownRoutesBehaveAsLegacy() throws Exception {
        assertThat(get("/claims/admin/adjusters.do").statusCode()).isEqualTo(302);
        post("/claims/login.do", "supervisor", "supervisor");
        Object[][] expectations = {
            {"/claims/admin/adjusters.do", 404, "404"},
            {"/claims/admin/reference.do", 404, "404"},
            {"/claims/admin/editAdjuster.do", 500, "500"},
            {"/claims/admin/missing.do", 500, "500"},
            {"/claims/unknown.do", 200, "null"},
            {"/claims/login", 404, "404"},
        };
        for (Object[] expected : expectations) {
            HttpResponse<String> response = get((String) expected[0]);
            assertThat(response.statusCode()).as((String) expected[0]).isEqualTo(expected[1]);
            assertThat(views(response.body())).containsExactly("/WEB-INF/jsp/error.jsp");
            assertThat(response.body()).contains("Reference status: " + expected[2],
                    "<a href=\"/claims/login.do\">Return to login</a>");
            assertThat(fields(response.body())).isEmpty();
        }
        assertThat(get("/claims/northstar.css").body()).contains(".layout{width:900px");
    }
}
