package com.northstar.claims.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginRouteTest {
    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ServletWebServerApplicationContext context;

    @LocalServerPort
    private int port;

    private ResponseEntity<String> get(String path) {
        return rest.getForEntity("http://localhost:" + port + path, String.class);
    }

    @Test
    void explicitDoRouteResolvesUnderClaimsContextWithLegacyParameter() {
        assertThat(context.getServletContext().getContextPath()).isEqualTo("/claims");
        ResponseEntity<String> response = get("/claims/login.do?username=supervisor");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("NorthStar Claims", "value=\"supervisor\"");
    }

    @Test
    void missingOrEmptyUsernameRendersPlaceholder() {
        for (String path : new String[] {"/claims/login.do", "/claims/login.do?username="}) {
            ResponseEntity<String> response = get(path);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("value=\"\"");
        }
    }

    @Test
    void suffixlessAndUnknownRoutesAreNotMapped() {
        assertThat(get("/claims/login").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/claims/unknown.do").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
