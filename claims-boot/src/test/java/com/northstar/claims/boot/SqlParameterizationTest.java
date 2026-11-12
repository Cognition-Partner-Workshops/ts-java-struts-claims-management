package com.northstar.claims.boot;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/sql-parameters")
@AutoConfigureMockMvc
class SqlParameterizationTest {
    @Autowired MockMvc mvc;
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    private MockHttpSession session;

    @BeforeEach
    void seedAndLogin() throws Exception {
        new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql"),
                new ClassPathResource("db/seed.sql")).execute(source);
        session = new MockHttpSession();
        mvc.perform(post("/login.do").session(session).param("username", "supervisor")
                .param("password", "supervisor")).andExpect(status().isOk());
    }

    @Test
    void assignmentTreatsSqlAsLiteralAndOnlyChangesTargetClaim() throws Exception {
        assertLiteralUpdate("assign", "adjuster", "assigned_adjuster");
    }

    @Test
    void statusTreatsSqlAsLiteralAndOnlyChangesTargetClaim() throws Exception {
        assertLiteralUpdate("status", "status", "status");
    }

    private void assertLiteralUpdate(String route, String parameter, String column) throws Exception {
        String payload = "X' where 1=1 --";
        var before = jdbc.queryForList("select * from CLAIM where claim_id <> 119 order by claim_id");
        mvc.perform(get("/workbench/" + route + ".do").session(session)
                        .param("claimId", "119").param(parameter, payload))
                .andExpect(status().isOk()).andExpect(view().name("workbench/view"));
        assertThat(jdbc.queryForList("select * from CLAIM where claim_id <> 119 order by claim_id"))
                .isEqualTo(before);
        assertThat(jdbc.queryForObject("select " + column + " from CLAIM where claim_id = 119", String.class))
                .isEqualTo(payload);
    }

    @Test
    void policyLineTreatsSqlAsLiteralAndReturnsNoOtherLines() throws Exception {
        mvc.perform(get("/policy/search.do").session(session)
                        .param("lineOfBusiness", "AUTO' OR '1'='1"))
                .andExpect(status().isOk()).andExpect(view().name("policy/search"))
                .andExpect(model().attribute("policies", java.util.List.of()))
                .andExpect(model().attribute("searchCount", 0));
    }
}
