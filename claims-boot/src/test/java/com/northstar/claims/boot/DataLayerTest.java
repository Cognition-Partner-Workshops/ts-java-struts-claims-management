package com.northstar.claims.boot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "claims.db.url=jdbc:hsqldb:file:target/test-db/data")
class DataLayerTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired NamedParameterJdbcTemplate named;
    @Autowired DataSourceTransactionManager transactions;
    @Autowired ClaimsConfig config;

    @Test
    void sharedConfigAndApplicationConnections() throws Exception {
        Properties legacy = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("../src/main/resources/northstar.properties"))) {
            legacy.load(reader);
        }
        for (String key : legacy.stringPropertyNames()) {
            assertThat(config.get(key)).isEqualTo(legacy.getProperty(key));
        }
        assertThat(config.get("missing")).isEmpty();
        assertThat(config.reportAsOfDate()).isEqualTo(LocalDate.of(2019, 4, 1));
        assertThat(jdbc.queryForObject("VALUES CURRENT_USER", String.class)).isEqualTo("CLAIMS_APP");
        assertThat(named.queryForObject("VALUES :value", java.util.Map.of("value", 7), Integer.class)).isEqualTo(7);
        try (var connection = transactions.getDataSource().getConnection()) {
            assertThat(connection.getAutoCommit()).isTrue();
        }
    }

    @Test
    void everyTableMatchesLegacySeed() throws Exception {
        try (var reference = DriverManager.getConnection("jdbc:hsqldb:mem:reference", "SA", "");
             var statement = reference.createStatement()) {
            for (String file : new String[] {"schema.sql", "seed.sql"}) {
                StringBuilder sql = new StringBuilder();
                for (String line : Files.readAllLines(Path.of("../src/main/resources/db", file))) {
                    if (line.trim().isEmpty() || line.trim().startsWith("--")) continue;
                    sql.append(line);
                    if (line.trim().endsWith(";")) {
                        statement.executeUpdate(sql.substring(0, sql.length() - 1));
                        sql.setLength(0);
                    }
                }
            }
            var tables = jdbc.queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE'", String.class);
            assertThat(tables).isNotEmpty();
            for (String table : tables) {
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rows.next();
                    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class))
                            .as(table).isEqualTo(rows.getLong(1));
                }
            }
            statement.execute("SHUTDOWN");
        }
    }

    @Test
    void keepExistingDataAndReset() throws Exception {
        String url = "jdbc:hsqldb:file:target/test-db/keep";
        DatabaseBootstrap.bootstrap(url, true, config);
        try (var c = DriverManager.getConnection(url, "SA", ""); var s = c.createStatement()) {
            s.executeUpdate("UPDATE ADJUSTER SET FULL_NAME='Kept' WHERE ADJUSTER_ID=1");
            s.execute("SHUTDOWN");
        }
        DatabaseBootstrap.bootstrap(url, false, config);
        try (var c = DriverManager.getConnection(url, "SA", ""); var s = c.createStatement()) {
            try (var rows = s.executeQuery("SELECT FULL_NAME FROM ADJUSTER WHERE ADJUSTER_ID=1")) {
                rows.next();
                assertThat(rows.getString(1)).isEqualTo("Kept");
            }
            s.execute("SHUTDOWN");
        }
        DatabaseBootstrap.bootstrap(url, true, config);
        assertThatThrownBy(() -> DatabaseBootstrap.executeScript(null, "missing.sql"))
                .isInstanceOf(NullPointerException.class);
    }
}
