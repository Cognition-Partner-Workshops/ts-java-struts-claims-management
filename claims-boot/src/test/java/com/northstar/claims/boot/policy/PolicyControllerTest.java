package com.northstar.claims.boot.policy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyControllerTest {
    @Test
    void listAndSearchSwallowDatabaseFailuresButDetailDoesNot() {
        PolicyDAO dao = mock(PolicyDAO.class);
        PolicyController controller = new PolicyController(dao);
        var failure = new DataAccessResourceFailureException("unavailable");
        when(dao.findAll()).thenThrow(failure);
        when(dao.findByLine("AUTO")).thenThrow(failure);
        when(dao.findById(9001)).thenThrow(failure);
        var model = new ExtendedModelMap();
        assertThat(controller.list(model)).isEqualTo("policy/list");
        assertThat(model.get("policies")).asList().isEmpty();
        assertThat(model.get("policyCount")).isEqualTo(0);
        assertThat(controller.search(new PolicySearchForm(), model)).isEqualTo("policy/search");
        assertThat(model.get("searchLine")).isEqualTo("AUTO");
        assertThat(model.get("searchCount")).isEqualTo(0);
        assertThat(controller.insuredParty(null, model)).isEqualTo("policy/insuredParty");
        Policy placeholder = (Policy) model.get("policy");
        assertThat(placeholder.getPolicyId()).isEqualTo(9001);
        assertThat(placeholder.getInsuredName()).isEqualTo("Policy insured party");
        assertThatThrownBy(() -> controller.coverageDetail(null, model)).isSameAs(failure);
    }
}
