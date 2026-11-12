package com.northstar.claims.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimsBootApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void explicitDoPathResolvesUnderClaimsContext() throws Exception {
        mockMvc.perform(get("/claims/login.do").contextPath("/claims"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ns:view /WEB-INF/jsp/login.jsp")));
    }

    @Test
    void extensionlessPathDoesNotResolve() throws Exception {
        mockMvc.perform(get("/claims/login").contextPath("/claims"))
                .andExpect(status().isNotFound());
    }
}
