package com.northstar.claims.fixture;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import com.northstar.claims.dao.ConnectionPool;

/** Inserts adjuster rows used by the action tests. */
public class AdjusterFixtureBuilder {

    public static final String FIXTURE_PASSWORD = "Fixture#Adjuster2009";

    private String username = "fixture_adjuster";
    private double approvalLimit = 750;

    public AdjusterFixtureBuilder username(String value) {
        username = value;
        return this;
    }

    public AdjusterFixtureBuilder approvalLimit(double value) {
        approvalLimit = value;
        return this;
    }

    public String insert(int adjusterId) throws Exception {
        Connection connection = ConnectionPool.getInstance().getConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "insert into ADJUSTER values (?,?,?,?,?,?,?)");
            statement.setInt(1, adjusterId);
            statement.setString(2, username);
            statement.setString(3, md5(FIXTURE_PASSWORD));
            statement.setString(4, "Fixture Adjuster");
            statement.setString(5, "TEST");
            statement.setBoolean(6, true);
            statement.setDouble(7, approvalLimit);
            statement.executeUpdate();
        } finally {
            try { statement.close(); } catch (Exception ignored) {}
            ConnectionPool.getInstance().release(connection);
        }
        return username;
    }

    static String md5(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(text.getBytes("UTF-8"));
        return String.format("%032x", new BigInteger(1, hash));
    }
}
