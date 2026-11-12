package com.northstar.claims.boot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public final class DatabaseBootstrap {
    private DatabaseBootstrap() {}

    public static void bootstrap(String url, boolean reset, ClaimsConfig config) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "SA", "")) {
            try {
                boolean exists;
                try (var tables = connection.getMetaData().getTables(null, "PUBLIC", "ADJUSTER", new String[] {"TABLE"})) {
                    exists = tables.next();
                }
                if (reset || !exists) {
                    executeScript(connection, "db/schema.sql");
                    executeScript(connection, "db/seed.sql");
                }
                try (Statement statement = connection.createStatement()) {
                    try {
                        statement.execute("CREATE USER " + config.get("db.user")
                                + " PASSWORD '" + config.get("db.password") + "' ADMIN");
                    } catch (SQLException alreadyPresent) {
                        statement.execute("ALTER USER " + config.get("db.user")
                                + " SET PASSWORD '" + config.get("db.password") + "'");
                    }
                }
                connection.commit();
            } finally {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("shutdown");
                }
            }
        }
    }

    static void executeScript(Connection connection, String resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                DatabaseBootstrap.class.getClassLoader().getResourceAsStream(resource), Charset.defaultCharset()));
             Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0 || line.trim().startsWith("--")) {
                    continue;
                }
                sql.append(line);
                if (line.trim().endsWith(";")) {
                    statement.executeUpdate(sql.substring(0, sql.length() - 1));
                    sql.setLength(0);
                }
            }
        }
    }
}
