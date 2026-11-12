package com.northstar.claims.boot.policy;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/policy")
class PolicyRouteTest {
    private static final Pattern FIELD = Pattern.compile("<span id=\"f_([^\"]+)\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern VIEW = Pattern.compile("<!--\\s*ns:view\\s+([^ ]+)\\s*-->");
    private static final Pattern ERROR = Pattern.compile("<!--\\s*ns:error\\s+([^ ]+)\\s*-->");

    @LocalServerPort
    private int port;

    @Autowired
    private PolicyDAO policies;

    @Autowired
    private ObjectMapper json;

    private HttpResponse<String> request(HttpClient client, String path, String form) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (form != null) {
            builder.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient browser(String user) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        String password = user.equals("supervisor") ? "supervisor" : "legacy1";
        request(client, "/claims/login.do", "username=" + user + "&password=" + password);
        return client;
    }

    private static Map<String, String> fields(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = FIELD.matcher(body);
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }
        return fields;
    }

    @TestFactory
    List<DynamicTest> frozenPolicyResponses() throws Exception {
        Path transcripts = Path.of("../transcripts");
        if (!Files.isDirectory(transcripts)) {
            transcripts = Path.of("transcripts");
        }
        List<DynamicTest> tests = new ArrayList<>();
        try (var paths = Files.list(transcripts)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                JsonNode transcript = json.readTree(path.toFile());
                String route = transcript.path("request").path("path").asText();
                if (!route.startsWith("/claims/policy/")) {
                    continue;
                }
                tests.add(DynamicTest.dynamicTest(transcript.path("scenario").asText(), () -> {
                    HttpClient client = browser(transcript.path("actor").asText());
                    HttpResponse<String> response = request(client, route, null);
                    JsonNode expected = transcript.path("expected");
                    assertThat(response.statusCode()).isEqualTo(expected.path("status").asInt());
                    Matcher view = VIEW.matcher(response.body());
                    assertThat(view.find()).isTrue();
                    assertThat("forward:" + view.group(1)).isEqualTo(expected.path("result").asText());
                    JsonNode actualFields = json.valueToTree(fields(response.body()));
                    assertThat(actualFields).isEqualTo(expected.path("business_fields"));
                    assertThat(ERROR.matcher(response.body()).find()).isFalse();
                }));
            }
        }
        assertThat(tests).hasSize(23);
        return tests;
    }

    @Test
    void authenticationAndPostBindingsKeepLegacyContract() throws Exception {
        HttpClient anonymous = HttpClient.newHttpClient();
        assertThat(request(anonymous, "/claims/policy/list.do", null).statusCode()).isEqualTo(302);
        HttpClient client = browser("supervisor");
        String search = request(client, "/claims/policy/search.do", "lineOfBusiness=AUTO").body();
        assertThat(fields(search)).hasSize(11).containsEntry("policyLimit_9001", "1000.00");
        assertThat(search).contains("name=\"lineOfBusiness\" value=\"AUTO\"",
                "action=\"/claims/policy/search.do\"", "method=\"post\"");
        assertThat(fields(request(client, "/claims/policy/search.do", "lineOfBusiness=").body()))
                .isEqualTo(fields(search));
        assertThat(fields(request(client, "/claims/policy/search.do", "lineOfBusiness=HOME").body())).isEmpty();
        assertThat(fields(request(client, "/claims/policy/search.do", "lineOfBusiness=%27").body())).isEmpty();
        String view = request(client, "/claims/policy/view.do", "policyId=9001").body();
        assertThat(fields(view)).containsExactly(
                Map.entry("policyLimit", "1000.00"), Map.entry("deductibleApplied", "100.00"),
                Map.entry("policyStatus", "ACTIVE"));
        assertThat(view).contains("href=\"coverages.do?policyId=9001\"",
                "href=\"coverageDetail.do?policyId=9001\"", "href=\"insuredParty.do?policyId=9001\"",
                "href=\"renewal.do?policyId=9001\"");
    }

    @Test
    void stylesheetInclusionsMatchTheLegacyJspHeaders() throws Exception {
        HttpClient client = browser("supervisor");
        for (String route : List.of("search", "list", "view", "coverages")) {
            assertThat(request(client, "/claims/policy/" + route + ".do?policyId=9001", null).body())
                    .doesNotContain("rel=\"stylesheet\"");
        }
        for (String route : List.of("coverageDetail", "insuredParty", "renewal")) {
            assertThat(request(client, "/claims/policy/" + route + ".do?policyId=9001", null).body())
                    .contains("href=\"/claims/northstar.css\"");
        }
    }

    @Test
    void placeholderInsuredDataDoesNotMutatePolicy() throws Exception {
        Policy before = policies.findById(9001);
        String body = request(browser("supervisor"), "/claims/policy/insuredParty.do?policyId=9001", null).body();
        assertThat(body).contains("POL-9001", "Policy insured party", "Address on policy record");
        assertThat(policies.findById(9001)).usingRecursiveComparison().isEqualTo(before);
        assertThat(request(browser("supervisor"), "/claims/policy/renewal.do?policyId=99999", null).body())
                .contains("<span id=\"f_renewalPolicyId\">9001</span>");
    }

    @Test
    @Transactional
    void jdbcDaoKeepsLegacyQueriesAndMapping() {
        assertThat(policies.count()).isEqualTo(42);
        assertThat(policies.findAll()).hasSize(42)
                .extracting(Policy::getPolicyId).isSorted();
        Policy policy = policies.findById(9001);
        assertThat(policy.getPolicyNumber()).isEqualTo("NS-09001");
        assertThat(policy.getPolicyLimit()).isEqualTo(1000.0);
        assertThat(policy.getDeductible()).isEqualTo(100.0);
        assertThat(policies.findById(99999)).isNull();
        assertThat(policies.findByLine("AUTO")).hasSize(11)
                .extracting(Policy::getPolicyNumber).isSorted();
        assertThat(policies.findByLine("HOME")).isEmpty();
        assertThat(policies.search("NS-09001")).hasSize(1);
        assertThat(policies.search(policy.getInsuredName())).extracting(Policy::getPolicyId).contains(9001);
        policies.updateStatus(9001, "TEST");
        assertThat(policies.findById(9001).getStatus()).isEqualTo("TEST");
        assertThatThrownBy(() -> policies.delete(9001)).isInstanceOf(DataIntegrityViolationException.class);
        policies.delete(99999);
        assertThat(policies.count()).isEqualTo(42);
    }
}
