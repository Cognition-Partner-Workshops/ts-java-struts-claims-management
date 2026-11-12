package com.northstar.claims.boot.view;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.messages.basename=ApplicationResources")
class LayoutRenderingTest {
    @Autowired private SpringTemplateEngine templates;
    @Autowired private ApplicationContext application;

    @Test
    void rendersLegacyTableAndContextRelativeLinksWithoutExtraWrappers() {
        String html = render("layout-preview/home", Map.of("asOfDate", "2019-04-01"));
        assertThat(html).contains("<!-- ns:view /WEB-INF/jsp/home.jsp -->",
                "<span id=\"f_signedInUser\">supervisor</span>",
                "href=\"/claims/northstar.css\"", "href=\"/claims/intake/new.do\"",
                "3.4.1 (build 20081117)", "Production claims desk", "Claims support",
                "Session: Authenticated", "Operator: supervisor", "2019-04-01");
        assertThat(html).doesNotContain("<main", "th:", "<th:block", "??");
        assertThat(html.split("class=\"page\"", -1)).hasSize(2);
        assertThat(html.split("class=\"menu\"", -1)).hasSize(2);
        assertThat(html.indexOf("class=\"menu\"")).isLessThan(html.indexOf("Claims Operations Home"));
        assertThat(html.indexOf("class=\"footer\"")).isGreaterThan(html.indexOf("class=\"screen-audit\""));
    }

    @Test
    void rendersMarkerAndFormHelpersWithoutInventingLoginErrorMarkers() {
        String html = render("layout-preview/helpers", Map.of("errors", List.of(
                Map.of("key", "login.failed", "message", "The username or password was not recognized."))));
        assertThat(html).contains("<!-- ns:error errors.claimant.required -->",
                "<span id=\"f_reserve\">10.50</span>", "<span id=\"f_null\"></span>",
                "<b>Operator message</b>", "The username or password was not recognized.",
                "<tr><td>Username</td><td><input type=\"text\" name=\"username\" size=\"25\"></td></tr>");
        assertThat(html).doesNotContain("ns:error login.failed", "<th:block", "??");
    }

    private String render(String template, Map<String, Object> variables) {
        MockServletContext servlet = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servlet);
        request.setContextPath("/claims");
        request.setRequestURI("/claims/home.do");
        request.getSession().setAttribute("user", "supervisor");
        var exchange = JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(request, new MockHttpServletResponse());
        WebContext context = new WebContext(exchange, Locale.US, variables);
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(application, null));
        return templates.process(template, context);
    }
}
