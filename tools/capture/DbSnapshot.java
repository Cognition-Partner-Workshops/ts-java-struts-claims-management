import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * Capture-only helper that reads every application table over JDBC.
 *
 * Run as a single-file program: java -cp hsqldb.jar DbSnapshot.java URL USER PASSWORD.
 * Each "snapshot" line on stdin is answered with one line of JSON on stdout:
 * {"tables":[{"name","primary_key","columns":[{"name","type"}],"rows":[[...]]}]}.
 */
public class DbSnapshot {

    public static void main(String[] args) throws Exception {
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        try (Connection connection = DriverManager.getConnection(args[0], args[1],
                args.length > 2 ? args[2] : "")) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            PrintStream output = new PrintStream(System.out, true, "UTF-8");
            String line;
            while ((line = input.readLine()) != null) {
                if (line.trim().equals("snapshot")) {
                    output.println(snapshot(connection));
                }
            }
        }
    }

    private static String snapshot(Connection connection) throws Exception {
        DatabaseMetaData meta = connection.getMetaData();
        List<String> tables = new ArrayList<>();
        try (ResultSet results = meta.getTables(null, "PUBLIC", "%", new String[] { "TABLE" })) {
            while (results.next()) {
                tables.add(results.getString("TABLE_NAME"));
            }
        }
        Collections.sort(tables);
        StringBuilder json = new StringBuilder("{\"tables\":[");
        for (int t = 0; t < tables.size(); t++) {
            if (t > 0) {
                json.append(',');
            }
            table(connection, meta, tables.get(t), json);
        }
        connection.commit();
        return json.append("]}").toString();
    }

    private static void table(Connection connection, DatabaseMetaData meta, String table,
            StringBuilder json) throws Exception {
        TreeMap<Short, String> keyBySequence = new TreeMap<>();
        try (ResultSet keys = meta.getPrimaryKeys(null, "PUBLIC", table)) {
            while (keys.next()) {
                keyBySequence.put(keys.getShort("KEY_SEQ"), keys.getString("COLUMN_NAME"));
            }
        }
        List<String> primaryKey = new ArrayList<>(keyBySequence.values());
        json.append("{\"name\":").append(quote(table)).append(",\"primary_key\":[");
        for (int i = 0; i < primaryKey.size(); i++) {
            json.append(i > 0 ? "," : "").append(quote(primaryKey.get(i)));
        }
        json.append("],\"columns\":[");
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT * FROM PUBLIC." + identifier(table)
                        + " ORDER BY " + order(primaryKey, connection, table))) {
            ResultSetMetaData columns = rows.getMetaData();
            int count = columns.getColumnCount();
            for (int c = 1; c <= count; c++) {
                json.append(c > 1 ? "," : "").append("{\"name\":").append(quote(columns.getColumnName(c)))
                        .append(",\"type\":").append(quote(columns.getColumnTypeName(c))).append('}');
            }
            json.append("],\"rows\":[");
            boolean first = true;
            while (rows.next()) {
                json.append(first ? "[" : ",[");
                first = false;
                for (int c = 1; c <= count; c++) {
                    if (c > 1) {
                        json.append(',');
                    }
                    value(rows, c, columns.getColumnType(c), json);
                }
                json.append(']');
            }
            json.append("]}");
        }
    }

    private static String order(List<String> primaryKey, Connection connection, String table)
            throws Exception {
        if (!primaryKey.isEmpty()) {
            StringBuilder order = new StringBuilder();
            for (String column : primaryKey) {
                order.append(order.length() > 0 ? "," : "").append(identifier(column));
            }
            return order.toString();
        }
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT * FROM PUBLIC." + identifier(table)
                        + " WHERE 1 = 0")) {
            StringBuilder order = new StringBuilder();
            for (int c = 1; c <= rows.getMetaData().getColumnCount(); c++) {
                order.append(c > 1 ? "," : "").append(c);
            }
            return order.toString();
        }
    }

    private static void value(ResultSet rows, int column, int type, StringBuilder json)
            throws Exception {
        Object raw = rows.getObject(column);
        if (raw == null) {
            json.append("null");
            return;
        }
        switch (type) {
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
            json.append(rows.getLong(column));
            return;
        case Types.BOOLEAN:
        case Types.BIT:
            json.append(rows.getBoolean(column));
            return;
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.REAL:
            json.append(quote(BigDecimal.valueOf(rows.getDouble(column)).toPlainString()));
            return;
        case Types.DECIMAL:
        case Types.NUMERIC:
            json.append(quote(rows.getBigDecimal(column).toPlainString()));
            return;
        default:
            json.append(quote(rows.getString(column)));
        }
    }

    private static String identifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String quote(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
            case '"': quoted.append("\\\""); break;
            case '\\': quoted.append("\\\\"); break;
            case '\n': quoted.append("\\n"); break;
            case '\r': quoted.append("\\r"); break;
            case '\t': quoted.append("\\t"); break;
            default:
                if (ch < 0x20) {
                    quoted.append(String.format("\\u%04x", (int) ch));
                } else {
                    quoted.append(ch);
                }
            }
        }
        return quoted.append('"').toString();
    }
}
