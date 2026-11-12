package com.northstar.claims.boot.intake;

import java.util.regex.Pattern;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Screen contracts and DB effects that would otherwise be lost in a framework port. */
@SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:mem:intake-workbench")
@AutoConfigureMockMvc
class IntakeWorkbenchTest {
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

    private String read(String path) throws Exception {
        return mvc.perform(get(path).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void errorsKeepOrderAndDoNotInsertOrRepopulateTheForm() throws Exception {
        int before = jdbc.queryForObject("select count(*) from CLAIM", Integer.class);
        String body = mvc.perform(post("/intake/submit.do").session(session)
                .param("claimantName", "   ").param("description", " ")
                .param("lossDate", "2019-04-01")).andReturn().getResponse().getContentAsString();
        assertThat(Pattern.compile("<!-- ns:error (.*?) -->").matcher(body).results()
                .map(match -> match.group(1)).toList()).containsExactly("errors.claimant.required",
                        "errors.description.required", "errors.lossdate.format");
        assertThat(body).contains("<!-- ns:view /WEB-INF/jsp/intake/new.jsp -->",
                "action=\"intake/submit.do\"", "name=\"claimantName\" size=\"40\"");
        assertThat(jdbc.queryForObject("select count(*) from CLAIM", Integer.class)).isEqualTo(before);
    }

    @Test
    void lenientDateAndReserveConversionKeepTheLegacyInsertDefaults() throws Exception {
        String body = mvc.perform(post("/intake/submit.do").session(session)
                .param("claimantName", "  Claimant  ").param("description", "Loss")
                .param("lossDate", "02/30/2019").param("lossType", "FIRE")
                .param("initialReserve", "abc")).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("<span id=\"f_claimId\">121</span>",
                "<span id=\"f_claimStatus\">OPEN</span>");
        assertThat(jdbc.queryForMap("select claimant_name, loss_date, loss_type, reserve_amount, "
                + "policy_id, created_by from CLAIM where claim_id = 121"))
                .containsEntry("CLAIMANT_NAME", "  Claimant  ").containsEntry("LOSS_TYPE", "WATER")
                .containsEntry("RESERVE_AMOUNT", 0d).containsEntry("POLICY_ID", 9001)
                .containsEntry("CREATED_BY", "supervisor");
        assertThat(session.getAttribute("lastLossDate")).isEqualTo("2019-03-02");
        assertThat(read("/workbench/view.do?claimId=121"))
                .contains("<span id=\"f_lossDate\">2019-03-02</span>");
        assertThat(read("/intake/confirm.do?claimId=121"))
                .contains("<span id=\"f_claimStatus\"></span>");
    }

    @Test
    void mutationsOnlyShowChangedFieldsAndNotesAndHistoryStayUnwritten() throws Exception {
        int noteCount = jdbc.queryForObject("select count(*) from CLAIM_NOTE", Integer.class);
        int historyCount = jdbc.queryForObject("select count(*) from RESERVE_HISTORY", Integer.class);
        String assign = read("/workbench/assign.do?claimId=119&assignedAdjuster=adjuster4");
        assertThat(assign).contains("<span id=\"f_assignedAdjuster\">adjuster2</span>",
                "<span id=\"f_claimStatus\"></span>", "<span id=\"f_reserveAmount\"></span>");
        assertThat(jdbc.queryForObject("select assigned_adjuster from CLAIM where claim_id=119", String.class))
                .isEqualTo("adjuster2");
        assertThat(read("/workbench/reserve.do?claimId=119&reserveAmount=abc"))
                .contains("<span id=\"f_reserveAmount\">4500.00</span>");
        assertThat(jdbc.queryForObject("select reserve_amount from CLAIM where claim_id=119", Double.class))
                .isEqualTo(4500d);
        assertThat(read("/workbench/note.do?claimId=119&noteText=Reviewed"))
                .contains("<span id=\"f_noteText\">Reviewed</span>");
        assertThat(jdbc.queryForObject("select count(*) from CLAIM_NOTE", Integer.class)).isEqualTo(noteCount);
        assertThat(jdbc.queryForObject("select count(*) from RESERVE_HISTORY", Integer.class)).isEqualTo(historyCount);
        assertThat(read("/workbench/reserveHistory.do?claimId=119"))
                .contains("<span id=\"f_currentReserve\">4500.00</span>").doesNotContain("id=\"f_oldReserve_");
        assertThat(read("/workbench/notes.do?claimId=999999"))
                .contains("<span id=\"f_noteClaimId\">119</span>");
    }

    @Test
    void closePreservesFirstClosureAndReopenDeletesIt() throws Exception {
        read("/workbench/status.do?claimId=116&status=CLOSED");
        assertThat(jdbc.queryForObject("select count(*) from CLAIM_CLOSURE where claim_id=116", Integer.class))
                .isEqualTo(1);
        jdbc.update("update CLAIM_CLOSURE set closed_date = date '2019-03-30' where claim_id=116");
        read("/workbench/status.do?claimId=116&status=CLOSED");
        assertThat(jdbc.queryForObject("select cast(closed_date as varchar(10)) from CLAIM_CLOSURE where claim_id=116", String.class))
                .isEqualTo("2019-03-30");
        assertThat(read("/workbench/statusHistory.do?claimId=116"))
                .contains("<span id=\"f_statusHistoryCurrent\">CLOSED</span>").doesNotContain("id=\"f_statusDate_");
        read("/workbench/status.do?claimId=116&status=OPEN");
        assertThat(jdbc.queryForObject("select count(*) from CLAIM_CLOSURE where claim_id=116", Integer.class)).isZero();
    }

    @Test
    void unknownClaimsAndOrphanRoutesUseTheLegacyErrorForward() throws Exception {
        for (String route : new String[]{"/workbench/view.do?claimId=999999", "/workbench/assignment.do",
                "/intake/intake/submit.do"}) {
            assertThat(read(route)).contains("<!-- ns:view /WEB-INF/jsp/error.jsp -->");
        }
        assertThat(read("/workbench/reserveHistory.do?claimId=999999"))
                .contains("<span id=\"f_reserveReviewStatus\">UNKNOWN</span>");
    }
}
