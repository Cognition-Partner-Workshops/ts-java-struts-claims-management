package com.northstar.claims;

import static org.junit.Assert.assertEquals;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.Locale;
import org.junit.Test;
import com.northstar.claims.util.ReportFormatter;

public class ReportFormatterCharacterizationTest {
    @Test
    public void formatsNormalEmptyAndMalformedValues() throws Exception {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            ReportFormatter formatter = new ReportFormatter();
            for (String line : Files.readAllLines(Paths.get(
                    "src/test/resources/characterization/reports/formatter.tsv"), StandardCharsets.UTF_8)) {
                if (line.startsWith("#")) continue;
                String[] fields = line.split("\t", -1);
                String value = "<null>".equals(fields[1]) ? null : fields[1];
                String actual;
                if ("date".equals(fields[0])) actual = formatter.date(value == null ? null : Date.valueOf(value));
                else if ("percent".equals(fields[0])) actual = formatter.percent(value);
                else actual = formatter.money(value);
                assertEquals(line, fields[2], actual);
            }
        } finally { Locale.setDefault(previous); }
    }
}
