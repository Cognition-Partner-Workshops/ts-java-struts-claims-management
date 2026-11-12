package com.northstar.claims.boot.settlement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/settlement-routes")
@AutoConfigureMockMvc
class SettlementRoutesTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void saveEnforcesAdjusterAuthorityAtTheCentBoundary() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "adjuster5");
        jdbc.update("delete from SETTLEMENT where claim_id = ?", 118);
        mvc.perform(post("/settlement/save.do").with(user("adjuster5")).session(session)
                        .param("claimId", "118").param("coveredAmount", "5000.01").param("deductible", "0"))
                .andExpect(status().isOk()).andExpect(view().name("settlement/refused"))
                .andExpect(model().attribute("nsValidationErrors", java.util.List.of("errors.settlement.authority")))
                .andExpect(model().attribute("refusedAmount", 5000.01))
                .andExpect(model().attribute("approvalLimit", 5000.0))
                .andExpect(model().attribute("refusedAdjuster", "adjuster5"));
        assertEquals(0, jdbc.queryForObject("select count(*) from SETTLEMENT where claim_id = ?", Integer.class, 118));

        for (String amount : java.util.List.of("4999.99", "5000")) {
            mvc.perform(post("/settlement/save.do").with(user("adjuster5")).session(session)
                            .param("claimId", "118").param("coveredAmount", amount).param("deductible", "0"))
                    .andExpect(status().isOk()).andExpect(view().name("settlement/save"))
                    .andExpect(model().attribute("settlementAmount", Double.valueOf(amount)));
        }
        assertEquals(2, jdbc.queryForObject("select count(*) from SETTLEMENT where claim_id = ?", Integer.class, 118));
    }

    @Test
    void calculateThenSaveAndReadWithBothMethods() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "supervisor");
        mvc.perform(get("/settlement/calculate.do").with(user("supervisor")).session(session))
                .andExpect(status().isOk()).andExpect(view().name("settlement/calculate"))
                .andExpect(content().string(containsString("id=\"f_settlementAmount\"")));
        mvc.perform(post("/settlement/save.do").with(user("supervisor")).session(session).param("claimId", "119"))
                .andExpect(view().name("settlement/save"))
                .andExpect(content().string(containsString("supervisor")));
        mvc.perform(post("/settlement/detail.do").with(user("supervisor")).session(session).param("claimId", "119"))
                .andExpect(view().name("settlement/detail"))
                .andExpect(content().string(containsString("id=\"f_detailAmount\"")));
    }

    @Test
    void calculateAndSaveForOperator() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "adjuster4");
        mvc.perform(post("/settlement/calculate.do").with(user("adjuster4")).session(session)
                        .param("claimId", "118").param("coveredAmount", "3000").param("deductible", "1000"))
                .andExpect(view().name("settlement/calculate"))
                .andExpect(model().attribute("settlement",
                        org.hamcrest.Matchers.hasProperty("settlementAmount", org.hamcrest.Matchers.is(2000.0))));
        mvc.perform(post("/settlement/save.do").with(user("adjuster4")).session(session)
                        .param("claimId", "118"))
                .andExpect(view().name("settlement/save"))
                .andExpect(model().attribute("settlementAmount", 2000.0));
        mvc.perform(get("/settlement/detail.do").with(user("adjuster4")).session(session)
                        .param("claimId", "118"))
                .andExpect(view().name("settlement/detail"))
                .andExpect(model().attribute("settlement",
                        org.hamcrest.Matchers.hasProperty("settlementAmount", org.hamcrest.Matchers.is(2000.0))));
    }

    @Test
    void emptyDetailAndCalculationError() throws Exception {
        mvc.perform(get("/settlement/detail.do").with(user("supervisor")).param("claimId", "99999"))
                .andExpect(view().name("error")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Reference status: null")));
        mvc.perform(get("/settlement/calculate.do").with(user("supervisor")).param("coveredAmount", "NaN"))
                .andExpect(view().name("error")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Reference status: null")));
    }

    @Test
    void saveAboveApprovalLimitIsRefusedWithoutWritingSettlement() throws Exception {
        int before = settlementCount();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "adjuster5");
        mvc.perform(post("/settlement/save.do").with(user("adjuster5")).session(session)
                        .param("claimId", "118").param("coveredAmount", "250000"))
                .andExpect(view().name("settlement/refused"))
                .andExpect(model().attribute("approvalLimit", 5000.0))
                .andExpect(content().string(containsString("<!-- ns:error errors.settlement.authority -->")));
        assertEquals(before, settlementCount());
    }

    @Test
    void reusedPendingSettlementIsCheckedAgainstSavingUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "supervisor");
        mvc.perform(post("/settlement/calculate.do").with(user("supervisor")).session(session)
                        .param("claimId", "118").param("coveredAmount", "20000"))
                .andExpect(view().name("settlement/calculate"));
        int before = settlementCount();
        mvc.perform(post("/settlement/save.do").with(user("adjuster4")).session(session)
                        .param("claimId", "118"))
                .andExpect(view().name("settlement/refused"));
        assertEquals(before, settlementCount());
    }

    @Test
    void sessionUserAttributeDoesNotRaiseApprovalLimit() throws Exception {
        int before = settlementCount();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "supervisor");
        mvc.perform(post("/settlement/save.do").with(user("adjuster4")).session(session)
                        .param("claimId", "118").param("coveredAmount", "20000"))
                .andExpect(view().name("settlement/refused"))
                .andExpect(model().attribute("approvalLimit", 2500.0));
        mvc.perform(post("/settlement/save.do").with(user("adjusterX")).session(session)
                        .param("claimId", "118").param("coveredAmount", "20000"))
                .andExpect(view().name("settlement/refused"))
                .andExpect(model().attribute("approvalLimit", 0.0));
        assertEquals(before, settlementCount());
    }

    private int settlementCount() {
        return jdbc.queryForObject("select count(*) from SETTLEMENT", Integer.class);
    }
}
