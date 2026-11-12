package com.northstar.claims.boot;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public final class ClaimsConfig {
    private final Properties properties = new Properties();

    public ClaimsConfig() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/northstar.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        }
    }

    public String get(String key) {
        return properties.getProperty(key, "");
    }

    public LocalDate reportAsOfDate() {
        return LocalDate.parse(get("report.asof.date"));
    }
}
