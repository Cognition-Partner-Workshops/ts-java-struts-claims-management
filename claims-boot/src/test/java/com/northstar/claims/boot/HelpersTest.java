package com.northstar.claims.boot;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HelpersTest {
    @Test
    void legacyDateAndFormattingEdges() throws Exception {
        Locale locale = Locale.getDefault();
        TimeZone zone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            var date = DateHelper.parse("2019-02-31");
            assertThat(DateHelper.format(date)).isEqualTo("2019-03-03");
            assertThat(DateHelper.parse("2019-04-01ignored")).isEqualTo(DateHelper.parse("2019-04-01"));
            assertThat(DateHelper.format(null)).isEmpty();
            assertThat(DateHelper.month(date)).isEqualTo("2019-03");
            assertThat(DateHelper.today()).isEqualTo(DateHelper.format(new java.util.Date()));
            assertThatThrownBy(() -> DateHelper.parse("")).isInstanceOf(ParseException.class);
            assertThatThrownBy(() -> DateHelper.parse(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> DateHelper.month(null)).isInstanceOf(NullPointerException.class);
            assertThat(LegacyFormatHelper.databaseDate(null)).isEmpty();
            assertThat(LegacyFormatHelper.displayDate(null)).isEmpty();
            assertThat(LegacyFormatHelper.displayDate(date)).isEqualTo("03/03/2019");
            assertThat(LegacyFormatHelper.parseDatabaseDate("2019-02-31")).isEqualTo(date);
            assertThat(LegacyFormatHelper.parseDisplayDate("02/31/2019")).isEqualTo(date);
            assertThatThrownBy(() -> LegacyFormatHelper.parseDisplayDate("bad")).isInstanceOf(ParseException.class);
            assertThat(LegacyFormatHelper.monthName(date)).isEqualTo("March");
            assertThat(LegacyFormatHelper.yearMonth(date)).isEqualTo("2019-03");
            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.FEBRUARY, 14, 13, 14, 15);
            calendar.set(Calendar.MILLISECOND, 123);
            var timed = calendar.getTime();
            var end = LegacyFormatHelper.endOfMonth(timed);
            assertThat(LegacyFormatHelper.databaseDate(end)).isEqualTo("2020-02-29");
            calendar.setTime(end);
            assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(13);
            assertThat(calendar.get(Calendar.MILLISECOND)).isEqualTo(123);
            assertThat(LegacyFormatHelper.databaseDate(LegacyFormatHelper.beginningOfMonth(timed))).isEqualTo("2020-02-01");
            assertThat(LegacyFormatHelper.databaseDate(LegacyFormatHelper.addBusinessDays(timed, 1))).isEqualTo("2020-02-17");
            assertThat(LegacyFormatHelper.addBusinessDays(timed, -1)).isEqualTo(timed);
            assertThat(LegacyFormatHelper.money(1.005)).isEqualTo("1.01");
            assertThat(LegacyFormatHelper.money(-0.0)).isEqualTo("-0.00");
            assertThat(LegacyFormatHelper.percentage(.12345)).isEqualTo("12.35%");
            assertThat(LegacyFormatHelper.parseParameters(null)).isEmpty();
            assertThat(LegacyFormatHelper.parseParameters("")).isEmpty();
            assertThat(LegacyFormatHelper.parseParameters("a=1&a=2&b=&x&c=a=b")).containsEntry("a", "2").containsEntry("b", "").containsEntry("c", "a=b").hasSize(3);
            assertThat(LegacyFormatHelper.normalizeCode(null)).isEmpty();
            assertThat(LegacyFormatHelper.normalizeCode(" ab ")).isEqualTo("AB");
            assertThat(LegacyFormatHelper.claimLabel(1, null)).isEqualTo("1 - ");
            assertThat(LegacyFormatHelper.claimLabel(2, "NS")).isEqualTo("2 - NS");
            Locale.setDefault(Locale.GERMANY);
            assertThat(LegacyFormatHelper.money(1.5)).isEqualTo("1,50");
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(LegacyFormatHelper.normalizeCode("i")).isEqualTo("İ");
        } finally {
            Locale.setDefault(locale);
            TimeZone.setDefault(zone);
        }
    }
}
