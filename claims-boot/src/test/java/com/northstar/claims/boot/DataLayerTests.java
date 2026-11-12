package com.northstar.claims.boot;

import com.northstar.claims.boot.dao.*;
import com.northstar.claims.boot.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class DataLayerTests {
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired AdjusterDAO adjusters;
    @Autowired ClaimDAO claims;
    @Autowired PolicyDAO policies;
    @Autowired NoteDAO notes;
    @Autowired SettlementDAO settlements;
    @Autowired PaymentDAO payments;
    @Autowired ReportDAO reports;

    @Test
    void allEntityRepositoriesReadSeedAndMissingRows() throws Exception {
        for (Class<?> type : new Class<?>[]{AdjusterDAO.class, ClaimDAO.class, PolicyDAO.class,
                NoteDAO.class, SettlementDAO.class, PaymentDAO.class}) {
            Object dao = context.getBean(type);
            var rows = (java.util.List<?>) type.getMethod("findAll").invoke(dao);
            assertThat(rows).isNotEmpty();
            assertThat(type.getMethod("count").invoke(dao)).isEqualTo(rows.size());
            assertThat(type.getMethod("findById", int.class).invoke(dao, -1)).isNull();
            type.getMethod("delete", int.class).invoke(dao, -1);
        }
        assertThat(adjusters.findByUsername("adjuster1").getUsername()).isEqualTo("adjuster1");
        assertThat(adjusters.authenticate("missing", "wrong")).isNull();
        assertThat(adjusters.findActive()).isNotEmpty();
        assertThat(claims.findByStatus("missing")).isEmpty();
        assertThat(claims.search("", "")).hasSize(claims.count());
        assertThat(policies.search("no-such-policy")).isEmpty();
        assertThat(notes.findByClaim(-1)).isEmpty();
        assertThat(notes.findUnused(-1)).isNull();
        assertThat(settlements.findByClaim(-1)).isNull();
        assertThat(payments.findByClaim(-1)).isEmpty();
        assertThat(payments.totalIssued(-1)).isZero();
    }

    @Test
    void updatesAndReportsRetainLegacySqlBehavior() throws Exception {
        int id = ((com.northstar.claims.boot.model.Claim) claims.findAll().get(0)).getClaimId();
        claims.assign(id, "adjuster1");
        claims.updateStatus(id, "INVESTIGATING");
        claims.updateReserve(id, 123.45);
        assertThat(claims.findById(id).getAssignedAdjuster()).isEqualTo("adjuster1");
        assertThat(claims.findById(id).getStatus()).isEqualTo("INVESTIGATING");
        assertThat(claims.findById(id).getReserveAmount()).isEqualTo(123.45);
        assertThat(reports.openClaimsByAdjuster()).isNotEmpty();
        assertThat(reports.lossRatioByLine()).isNotEmpty();
        assertThat(reports.agedClaims()).isNotEmpty();
        assertThat(reports.claimCounts("status")).isNotEmpty();
        assertThat(reports.claimsForAdjuster("missing")).isEmpty();
        assertThat(reports.closedClaimsThrough("1900-01-01")).isEmpty();
        assertThat(reports.closuresPostedThrough("1900-01-01")).isEmpty();
        assertThatThrownBy(() -> reports.claimCounts("no_such_column"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> claims.search("'", ""))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void claimNumbersAndReferenceValuesKeepTheirSemantics() {
        ClaimNumberService numbers = new ClaimNumberService();
        assertThat(numbers.first()).isEmpty();
        assertThat(numbers.last()).isEmpty();
        assertThat(numbers.numeric(null)).isZero();
        assertThat(numbers.numeric("invalid")).isZero();
        assertThat(numbers.create(42)).isEqualTo("CLM-00042");
        assertThat(numbers.numeric("CLM-00042")).isEqualTo(42);
        numbers.record("CLM-00042");
        numbers.record("invalid");
        assertThat(numbers.count()).isEqualTo(1);
        numbers.issued().clear();
        assertThat(numbers.last()).isEqualTo("CLM-00042");
        ReferenceData reference = new ReferenceData();
        assertThat(reference.lineCount()).isEqualTo(4);
        assertThat(reference.statusCount()).isEqualTo(5);
        assertThat(reference.description("AUTO")).isEqualTo("Automobile");
        assertThat(reference.description("missing")).isEmpty();
        assertThat(ClaimsConfig.get("report.asof.date")).isEqualTo("2019-04-01");
        assertThat(ClaimsConfig.get("missing")).isEmpty();
        assertThat(jdbc.queryForObject("values current_user", String.class)).isEqualTo("CLAIMS_APP");
    }
}
