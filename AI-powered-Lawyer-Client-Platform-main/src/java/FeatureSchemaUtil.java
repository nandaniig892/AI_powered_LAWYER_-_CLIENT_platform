import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class FeatureSchemaUtil {

    private static volatile boolean initialized = false;

    private FeatureSchemaUtil() {
    }

    public static void ensureInitialized(Connection conn) throws SQLException {
        if (initialized) {
            return;
        }
        synchronized (FeatureSchemaUtil.class) {
            if (initialized) {
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS case_timeline (" +
                    "timeline_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "case_id INT NOT NULL, " +
                    "actor_user_id INT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "note TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS notifications (" +
                    "notification_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "type VARCHAR(50) DEFAULT 'info', " +
                    "related_case_id INT NULL, " +
                    "is_read BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS case_messages (" +
                    "message_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "case_id INT NOT NULL, " +
                    "sender_user_id INT NOT NULL, " +
                    "message_text TEXT NULL, " +
                    "file_path VARCHAR(255) NULL, " +
                    "is_read BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS lawyer_reviews (" +
                    "review_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "case_id INT NOT NULL UNIQUE, " +
                    "client_user_id INT NOT NULL, " +
                    "lawyer_user_id INT NOT NULL, " +
                    "rating INT NOT NULL, " +
                    "review_text TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS complaints (" +
                    "complaint_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "case_id INT NOT NULL, " +
                    "complainant_user_id INT NOT NULL, " +
                    "against_user_id INT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "status VARCHAR(30) DEFAULT 'open', " +
                    "resolution_note TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "resolved_at TIMESTAMP NULL)"
                );
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS admin_logs (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "admin_user_id INT NOT NULL, " +
                    "target_user_id INT NULL, " +
                    "action_type VARCHAR(80) NOT NULL, " +
                    "details TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
            }
            initialized = true;
        }
    }
}
