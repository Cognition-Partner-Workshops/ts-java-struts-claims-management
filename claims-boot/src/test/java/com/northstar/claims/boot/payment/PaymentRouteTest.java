package com.northstar.claims.boot.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP rendering and committed PAYMENT rows, including the legacy failure paths. */
@SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/payments")
@AutoConfigureMockMvc
class PaymentRouteTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PaymentDAO payments;
    private MockHttpSession session;

    @BeforeEach
    void freshPaymentsAndLogin() throws Exception {
        jdbc.update("delete from PAYMENT where payment_id > 60");
        jdbc.update("delete from SETTLEMENT where settlement_id > 120");
        session = (MockHttpSession) mvc.perform(post("/claims/login.do").contextPath("/claims")
                        .param("username", "supervisor").param("password", "supervisor"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String page(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.contextPath("/claims").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder issue(String amount, String method) {
        return post("/claims/payment/issue.do").param("claimId", "119")
                .param("payeeName", "Reserved Claimant").param("amount", amount)
                .param("paymentMethod", method);
    }

    @Test
    void protectsEveryPaymentAction() throws Exception {
        for (String route : new String[] {"issue", "history", "detail", "remittance"}) {
            mvc.perform(get("/claims/payment/" + route + ".do").contextPath("/claims"))
                    .andExpect(status().isFound());
        }
        assertThat(payments.count()).isEqualTo(60);
    }

    @Test
    void issuesThenRendersHistoryDetailAndRemittanceWithCommittedRow() throws Exception {
        String receipt = page(issue("1000.00", "CHECK").param("checkNumber", "IGNORED"));
        assertThat(receipt).contains("<!-- ns:view /WEB-INF/jsp/payment/issue.jsp -->",
                "<span id=\"f_paymentAmount\">1000.00</span>",
                "<span id=\"f_checkNumber\">CHK-61</span>",
                "<span id=\"f_paymentStatus\">ISSUED</span>");
        Payment stored = payments.findById(61);
        assertThat(stored.getClaimId()).isEqualTo(119);
        assertThat(stored.getAmount()).isEqualTo(1000);
        assertThat(stored.getPayeeName()).isEqualTo("Reserved Claimant");
        assertThat(stored.getPaymentMethod()).isEqualTo("CHECK");
        assertThat(stored.getStatus()).isEqualTo("ISSUED");
        assertThat(stored.getIssuedDate()).isEqualTo("2019-04-03");
        assertThat(stored.getSettlementId()).isEqualTo(jdbc.queryForObject(
                "select max(settlement_id) from SETTLEMENT where claim_id = 119", Integer.class));

        String history = page(get("/claims/payment/history.do"));
        assertThat(history).contains("<span id=\"f_checkNumber_61\">CHK-61</span>",
                "<span id=\"f_paymentAmount_61\">1000.00</span>",
                "<span id=\"f_paymentStatus_61\">ISSUED</span>");
        assertThat(history.split("href=\"detail.do\\?paymentId=61\"", -1)).hasSize(3);
        String detail = page(get("/claims/payment/detail.do"));
        assertThat(detail).contains("<span id=\"f_detailPaymentId\">61</span>",
                "<span id=\"f_detailPaymentClaim\">119</span>", "Reserved Claimant", "CHECK",
                "<span id=\"f_detailPaymentAmount\">1000.00</span>",
                "<span id=\"f_detailPaymentDate\">2019-04-03</span>",
                "<span id=\"f_detailPaymentStatus\">ISSUED</span>",
                "href=\"/claims/northstar.css\"");
        assertThat(page(get("/claims/payment/remittance.do"))).contains(
                "<span id=\"f_remitCheck_61\">CHK-61</span>",
                "<span id=\"f_remitDate_61\">2019-04-03</span>",
                "<span id=\"f_remitCount\">1</span>",
                "<span id=\"f_remitTotal\">1000.00</span>");
    }

    @Test
    void missingFieldsAndMissingRecordsKeepErrorPageAndDoNotConsumeIds() throws Exception {
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {
                get("/claims/payment/issue.do"),
                post("/claims/payment/issue.do").param("claimId", "118").param("payeeName", "Payee"),
                post("/claims/payment/issue.do").param("claimId", "999999")
                        .param("payeeName", "Payee").param("paymentMethod", "CHECK").param("amount", "5"),
                get("/claims/payment/detail.do"),
                get("/claims/payment/detail.do").param("paymentId", "999999")}) {
            assertThat(page(request)).contains("<!-- ns:view /WEB-INF/jsp/error.jsp -->",
                    "Reference status: null");
            assertThat(payments.count()).isEqualTo(60);
        }
        assertThat(page(issue("0", "CHECK"))).contains("CHK-61");
    }

    @Test
    void acceptsGetZeroNegativeAndUnrecognizedMethodCodesVerbatim() throws Exception {
        page(get("/claims/payment/issue.do").param("claimId", "119")
                .param("payeeName", "<Payee & Co>").param("paymentMethod", " eft ").param("amount", "0"));
        page(issue("-1", ""));
        assertThat(payments.findById(61).getPaymentMethod()).isEqualTo(" eft ");
        assertThat(payments.findById(62).getPaymentMethod()).isEmpty();
        assertThat(payments.findById(62).getAmount()).isEqualTo(-1);
        assertThat(page(get("/claims/payment/remittance.do"))).contains("&lt;Payee &amp; Co&gt;",
                "<span id=\"f_remitCount\">2</span>", "<span id=\"f_remitTotal\">-1.00</span>");
    }

    @Test
    void invalidOrMissingAmountUsesLatestSettlementWithoutRoundingStoredMoney() throws Exception {
        jdbc.update("insert into SETTLEMENT values (1000,119,100,0,0,false,88.125,'supervisor','2019-04-01')");
        assertThat(page(issue("abc", "ACH"))).contains("<span id=\"f_paymentAmount\">88.13</span>");
        page(get("/claims/payment/issue.do").param("claimId", "119")
                .param("payeeName", "Payee").param("paymentMethod", "CHECK"));
        assertThat(payments.findById(61).getSettlementId()).isEqualTo(1000);
        assertThat(payments.findById(61).getAmount()).isEqualTo(88.125);
        assertThat(payments.findById(62).getAmount()).isEqualTo(88.125);
    }

    @Test
    void invalidIdsUseDefaultsAndUnknownClaimsHaveEmptyRemittance() throws Exception {
        page(issue("10", "CHECK"));
        assertThat(page(get("/claims/payment/detail.do").param("paymentId", "bad")))
                .contains("<span id=\"f_detailPaymentId\">61</span>");
        assertThat(page(get("/claims/payment/history.do").param("claimId", "bad")))
                .contains("<span id=\"f_checkNumber_61\">CHK-61</span>");
        assertThat(page(get("/claims/payment/remittance.do").param("claimId", "bad")))
                .contains("<span id=\"f_remitClaimId\">119</span>");
        assertThat(page(get("/claims/payment/remittance.do").param("claimId", "999999")))
                .contains("<span id=\"f_remitCount\">0</span>", "<span id=\"f_remitTotal\">0.00</span>");
        jdbc.update("update PAYMENT set status = 'VOID' where payment_id = 61");
        assertThat(page(get("/claims/payment/remittance.do"))).contains(
                "<span id=\"f_remitStatus_61\">VOID</span>", "<span id=\"f_remitTotal\">10.00</span>");
        assertThat(payments.findAll()).hasSize(61);
        payments.delete(61);
        assertThat(payments.findById(61)).isNull();
    }
}
