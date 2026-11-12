package com.northstar.claims.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessionAdminTests {
    @Autowired MockMvc mvc;

    @Test
    void protectedScreensRedirectWithoutOperator() throws Exception {
        for (String path : new String[]{"/home.do", "/logout.do", "/admin/adjusters.do",
                "/admin/editAdjuster.do", "/admin/reference.do"}) {
            mvc.perform(get("/claims" + path).contextPath("/claims"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/claims/login.do"));
        }
    }

    @Test
    void acceptedLoginsRenderHomeAndSetBothSessionNames() throws Exception {
        for (String[] credentials : new String[][]{{"supervisor", "supervisor"},
                {"adjuster-anything", "legacy-anything"}}) {
            var result = mvc.perform(post("/login.do").param("username", credentials[0])
                            .param("password", credentials[1]))
                    .andExpect(status().isOk()).andExpect(view().name("home"))
                    .andExpect(model().attribute("loginStatus", "AUTHENTICATED"))
                    .andExpect(content().string(containsString("ns:view /WEB-INF/jsp/home.jsp")))
                    .andExpect(content().string(containsString(
                            "<span id=\"f_signedInUser\">" + credentials[0] + "</span>")))
                    .andReturn();
            assertThat(result.getRequest().getSession().getAttribute("user")).isEqualTo(credentials[0]);
            assertThat(result.getRequest().getSession().getAttribute("displayName")).isEqualTo(credentials[0]);
        }
    }

    @Test
    void rejectedAndEmptyLoginsKeepLegacyInvisibleMessage() throws Exception {
        for (String username : new String[]{"", "supervisor"}) {
            var result = mvc.perform(post("/login.do").param("username", username).param("password", "wrong"))
                    .andExpect(status().isOk()).andExpect(view().name("login"))
                    .andExpect(model().attribute("message", "login.failed"))
                    .andExpect(content().string(containsString("ns:view /WEB-INF/jsp/login.jsp")))
                    .andExpect(content().string(not(containsString("ns:error")))).andReturn();
            assertThat(result.getRequest().getSession().getAttribute("user")).isNull();
        }
        mvc.perform(get("/login.do")).andExpect(status().isOk())
                .andExpect(model().attribute("loginStatus", "REJECTED"));
    }

    @Test
    void homeAdminAndLogoutMatchLegacy() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "supervisor");
        session.setAttribute("displayName", "supervisor");
        mvc.perform(get("/home.do").session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("asOfDate", "2019-04-01"))
                .andExpect(content().string(containsString("<span id=\"f_signedInUser\">supervisor</span>")));
        mvc.perform(get("/admin/adjusters.do").session(session)).andExpect(status().isOk())
                .andExpect(model().attributeExists("adjusters", "adjusterCount"))
                .andExpect(forwardedUrl("/admin/WEB-INF/jsp/admin/adjusters.jsp"));
        mvc.perform(get("/admin/reference.do").session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("referenceVersion", "2019.04"))
                .andExpect(model().attribute("lossTypes", java.util.List.of("WATER", "FIRE", "THEFT", "COLLISION")))
                .andExpect(forwardedUrl("/admin/WEB-INF/jsp/admin/reference.jsp"));
        for (String page : new String[]{"adjusters", "reference"}) {
            mvc.perform(get("/admin/WEB-INF/jsp/admin/" + page + ".jsp").session(session))
                    .andExpect(status().isNotFound()).andExpect(view().name("error"))
                    .andExpect(content().string(containsString("ns:view /WEB-INF/jsp/error.jsp")))
                    .andExpect(content().string(containsString("Reference status: 404")))
                    .andExpect(content().string(not(containsString("id=\"f_"))));
        }
        mvc.perform(get("/admin/editAdjuster.do").session(session))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("ns:view /WEB-INF/jsp/error.jsp")));
        mvc.perform(get("/logout.do").session(session)).andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("ns:view /WEB-INF/jsp/login.jsp")));
        assertThat(session.isInvalid()).isTrue();
    }
}
