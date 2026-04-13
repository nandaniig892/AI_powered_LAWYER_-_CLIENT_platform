import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnectionUtil {

    private static final String DEFAULT_DB_URL =
        "jdbc:mysql://localhost:3306/legalconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "root";
    private static volatile boolean defaultWarningLogged = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private DBConnectionUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String dbUrl = firstNonEmpty(
            getConfigValue("LEGALCONNECT_DB_URL"),
            getConfigValue("DB_URL"),
            DEFAULT_DB_URL
        );
        String dbUser = firstNonEmpty(
            getConfigValue("LEGALCONNECT_DB_USER"),
            getConfigValue("DB_USER"),
            DEFAULT_DB_USER
        );
        String dbPassword = firstNonEmpty(
            getConfigValue("LEGALCONNECT_DB_PASSWORD"),
            getConfigValue("DB_PASSWORD"),
            DEFAULT_DB_PASSWORD
        );

        if (dbUrl.isEmpty() || dbUser.isEmpty() || dbPassword.isEmpty()) {
            throw new SQLException("Database configuration missing. Set LEGALCONNECT_DB_URL, LEGALCONNECT_DB_USER, and LEGALCONNECT_DB_PASSWORD.");
        }

        if (isUsingDefaults(dbUrl, dbUser, dbPassword) && !defaultWarningLogged) {
            defaultWarningLogged = true;
            System.err.println("Warning: Using fallback local DB configuration. Set LEGALCONNECT_DB_URL, LEGALCONNECT_DB_USER, and LEGALCONNECT_DB_PASSWORD for secure deployment.");
        }

        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private static String getConfigValue(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value == null ? "" : value.trim();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isUsingDefaults(String dbUrl, String dbUser, String dbPassword) {
        return DEFAULT_DB_URL.equals(dbUrl)
            && DEFAULT_DB_USER.equals(dbUser)
            && DEFAULT_DB_PASSWORD.equals(dbPassword);
    }
}
