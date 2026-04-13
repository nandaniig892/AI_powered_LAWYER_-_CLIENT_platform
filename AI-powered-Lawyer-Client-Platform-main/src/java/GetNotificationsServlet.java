import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class GetNotificationsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                out.print("{\"success\":false,\"message\":\"Not logged in\"}");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                int unreadCount = 0;
                String countSql = "SELECT COUNT(*) AS cnt FROM notifications WHERE user_id = ? AND is_read = FALSE";
                try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                    countStmt.setInt(1, userId);
                    try (ResultSet countRs = countStmt.executeQuery()) {
                        if (countRs.next()) {
                            unreadCount = countRs.getInt("cnt");
                        }
                    }
                }

                String listSql = "SELECT notification_id, title, message, type, is_read, related_case_id, created_at " +
                                 "FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 25";
                try (PreparedStatement listStmt = conn.prepareStatement(listSql)) {
                    listStmt.setInt(1, userId);
                    try (ResultSet rs = listStmt.executeQuery()) {
                        out.print("{\"success\":true,\"unreadCount\":" + unreadCount + ",\"items\":[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                out.print(",");
                            }
                            out.print("{");
                            out.print("\"id\":" + rs.getInt("notification_id") + ",");
                            out.print("\"title\":\"" + FeatureSupportUtil.escapeJson(rs.getString("title")) + "\",");
                            out.print("\"message\":\"" + FeatureSupportUtil.escapeJson(rs.getString("message")) + "\",");
                            out.print("\"type\":\"" + FeatureSupportUtil.escapeJson(rs.getString("type")) + "\",");
                            out.print("\"isRead\":" + rs.getBoolean("is_read") + ",");
                            int relatedCaseId = rs.getInt("related_case_id");
                            if (rs.wasNull()) {
                                out.print("\"relatedCaseId\":null,");
                            } else {
                                out.print("\"relatedCaseId\":" + relatedCaseId + ",");
                            }
                            out.print("\"createdAt\":\"" + rs.getTimestamp("created_at").toString() + "\"");
                            out.print("}");
                            first = false;
                        }
                        out.print("]}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to load notifications\"}");
            }
        }
    }
}
