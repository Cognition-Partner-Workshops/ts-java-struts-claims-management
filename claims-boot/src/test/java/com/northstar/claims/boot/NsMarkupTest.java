package com.northstar.claims.boot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NsMarkupTest {
    private final NsMarkup ns = new NsMarkup();

    @Test
    void fieldMatchesLegacyFieldTag() {
        assertThat(ns.field("signedInUser", "a<b&\"c\">", "text"))
                .isEqualTo("<span id=\"f_signedInUser\">a&lt;b&amp;&quot;c&quot;&gt;</span>");
        assertThat(ns.field("amount", "1234.5", "money")).isEqualTo("<span id=\"f_amount\">1234.50</span>");
        assertThat(ns.field("amount", "n/a", "money")).isEqualTo("<span id=\"f_amount\">n/a</span>");
        assertThat(ns.field("count", "7.9", "integer")).isEqualTo("<span id=\"f_count\">7</span>");
        assertThat(ns.field("lossDate", "2019-02-30", "date")).isEqualTo("<span id=\"f_lossDate\">2019-03-02</span>");
        assertThat(ns.field("missing", null, "text")).isEqualTo("<span id=\"f_missing\"></span>");
        assertThat(ns.field("number", 42, "text")).isEqualTo("<span id=\"f_number\">42</span>");
    }

    @Test
    void viewErrorAndMessageMarkers() {
        assertThat(ns.view("/WEB-INF/jsp/home.jsp")).isEqualTo("<!-- ns:view /WEB-INF/jsp/home.jsp -->");
        assertThat(ns.error("intake.error.claimant")).isEqualTo("<!-- ns:error intake.error.claimant -->");
        assertThat(ns.message("  body \n")).isEqualTo("body");
        assertThat(ns.message(null)).isEmpty();
    }
}
