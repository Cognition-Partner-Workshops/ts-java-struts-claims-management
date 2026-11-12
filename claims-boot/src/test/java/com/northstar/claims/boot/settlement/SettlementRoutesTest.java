package com.northstar.claims.boot.settlement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/settlement-routes")
@AutoConfigureMockMvc
class SettlementRoutesTest {
    @Autowired MockMvc mvc;

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
    void refusalEmptyDetailAndCalculationError() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "adjuster4");
        mvc.perform(post("/settlement/calculate.do").with(user("adjuster4")).session(session)
                        .param("claimId", "118").param("coveredAmount", "5000").param("deductible", "1000"))
                .andExpect(view().name("settlement/refused"))
                .andExpect(content().string(containsString("<!-- ns:error errors.settlement.authority -->")));
        mvc.perform(get("/settlement/detail.do").with(user("supervisor")).param("claimId", "99999"))
                .andExpect(view().name("settlement/detail")).andExpect(status().isOk());
        mvc.perform(get("/settlement/calculate.do").with(user("supervisor")).param("coveredAmount", "NaN"))
                .andExpect(view().name("error")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Reference status: null")));
    }
}
