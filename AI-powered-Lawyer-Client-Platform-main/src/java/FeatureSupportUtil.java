import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class FeatureSupportUtil {

    private FeatureSupportUtil() {
    }

    public static void addCaseTimeline(Connection conn, int caseId, Integer actorUserId, String status, String note)
            throws SQLException {
        String sql = "INSERT INTO case_timeline (case_id, actor_user_id, status, note) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, caseId);
            if (actorUserId == null) {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(2, actorUserId);
            }
            pstmt.setString(3, status);
            pstmt.setString(4, note);
            pstmt.executeUpdate();
        }
    }

    public static void createNotification(Connection conn, int userId, String title, String message, String type, Integer relatedCaseId)
            throws SQLException {
        String sql = "INSERT INTO notifications (user_id, title, message, type, related_case_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, title);
            pstmt.setString(3, message);
            pstmt.setString(4, type == null ? "info" : type);
            if (relatedCaseId == null) {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(5, relatedCaseId);
            }
            pstmt.executeUpdate();
        }
    }

    public static void createAdminLog(Connection conn, int adminUserId, Integer targetUserId, String actionType, String details)
            throws SQLException {
        String sql = "INSERT INTO admin_logs (admin_user_id, target_user_id, action_type, details) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminUserId);
            if (targetUserId == null) {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(2, targetUserId);
            }
            pstmt.setString(3, actionType);
            pstmt.setString(4, details);
            pstmt.executeUpdate();
        }
    }

    public static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
