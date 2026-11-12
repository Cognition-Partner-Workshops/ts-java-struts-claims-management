import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/** Tab-delimited, base64-encoded JDBC values; a dash represents SQL NULL. */
public class DatabaseSnapshot {
    private static String encode(String value) {
        return value == null ? "-" : Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:hsqldb:hsql://127.0.0.1:9137/northstar", "SA", "");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT * FROM " + args[0] + " ORDER BY " + args[1])) {
            ResultSetMetaData metadata = rows.getMetaData();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                if (column > 1) System.out.print("\t");
                System.out.print(encode(metadata.getColumnName(column)));
            }
            System.out.println();
            while (rows.next()) {
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    if (column > 1) System.out.print("\t");
                    System.out.print(encode(rows.getString(column)));
                }
                System.out.println();
            }
        }
    }
}
