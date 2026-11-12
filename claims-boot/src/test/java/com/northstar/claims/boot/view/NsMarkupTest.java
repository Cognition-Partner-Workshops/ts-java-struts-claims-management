package com.northstar.claims.boot.view;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.assertj.core.api.Assertions.assertThat;

class NsMarkupTest {
    private final NsMarkup markup = new NsMarkup();

    @Test
    void preservesExactFieldSpansAndLegacyEscaping() {
        assertThat(markup.field("claim\"<&", "O'Brien & <claim>\"", "text"))
                .isEqualTo("<span id=\"f_claim&quot;&lt;&amp;\">O'Brien &amp; &lt;claim&gt;&quot;</span>");
        assertThat(markup.field("empty", null, "money"))
                .isEqualTo("<span id=\"f_empty\"></span>");
        assertThat(markup.field("boolean", true, null))
                .isEqualTo("<span id=\"f_boolean\">true</span>");
    }

    @Test
    @ResourceLock("defaultLocale")
    void retainsDoubleRoundingTruncationLenientDatesAndInvalidInput() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertThat(markup.format("1.005", "money")).isEqualTo("1.01");
            assertThat(markup.format("0", "money")).isEqualTo("0.00");
            assertThat(markup.format("-19.9", "integer")).isEqualTo("-19");
            assertThat(markup.format("2019-02-30", "date")).isEqualTo("2019-03-02");
            assertThat(markup.format("2019-04-01 12:30:00", "date")).isEqualTo("2019-04-01");
            for (String type : new String[] {"money", "integer", "date", "text", ""}) {
                assertThat(markup.format("not a value", type)).isEqualTo("not a value");
                assertThat(markup.format(null, type)).isEmpty();
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void preservesHarnessCommentMarkers() {
        assertThat(markup.view("/WEB-INF/jsp/home.jsp"))
                .isEqualTo("<!-- ns:view /WEB-INF/jsp/home.jsp -->");
        assertThat(markup.error("errors.claimant.required"))
                .isEqualTo("<!-- ns:error errors.claimant.required -->");
    }
}
