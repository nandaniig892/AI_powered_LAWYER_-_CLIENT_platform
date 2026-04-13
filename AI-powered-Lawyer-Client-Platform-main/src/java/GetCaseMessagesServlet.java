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

public class GetCaseMessagesServlet extends HttpServlet {

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

            String caseIdParam = request.getParameter("caseId");
            if (caseIdParam == null || caseIdParam.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"caseId required\"}");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            String userType = (String) session.getAttribute("userType");
            int caseId = Integer.parseInt(caseIdParam);

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                CaseAccessUtil.CaseParticipantInfo info = CaseAccessUtil.loadCaseParticipantInfo(conn, caseId);
                if (!CaseAccessUtil.canAccessCase(userType, userId, info)) {
                    out.print("{\"success\":false,\"message\":\"Access denied\"}");
                    return;
                }

                String markReadSql = "UPDATE case_messages SET is_read = TRUE WHERE case_id = ? AND sender_user_id <> ?";
                try (PreparedStatement markStmt = conn.prepareStatement(markReadSql)) {
                    markStmt.setInt(1, caseId);
                    markStmt.setInt(2, userId);
                    markStmt.executeUpdate();
                }

                String sql = "SELECT cm.message_id, cm.sender_user_id, cm.message_text, cm.file_path, cm.created_at, " +
                             "u.first_name, u.last_name " +
                             "FROM case_messages cm " +
                             "INNER JOIN users u ON cm.sender_user_id = u.user_id " +
                             "WHERE cm.case_id = ? ORDER BY cm.created_at ASC";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, caseId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        out.print("{\"success\":true,\"items\":[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                out.print(",");
                            }
                            String senderName = rs.getString("first_name") + " " + rs.getString("last_name");
                            out.print("{");
                            out.print("\"id\":" + rs.getInt("message_id") + ",");
                            out.print("\"senderUserId\":" + rs.getInt("sender_user_id") + ",");
                            out.print("\"senderName\":\"" + FeatureSupportUtil.escapeJson(senderName) + "\",");
                            out.print("\"isMine\":" + (rs.getInt("sender_user_id") == userId) + ",");
                            out.print("\"message\":\"" + FeatureSupportUtil.escapeJson(rs.getString("message_text")) + "\",");
                            String filePath = rs.getString("file_path");
                            if (filePath == null || filePath.trim().isEmpty()) {
                                out.print("\"filePath\":null,");
                            } else {
                                out.print("\"filePath\":\"" + FeatureSupportUtil.escapeJson(filePath) + "\",");
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
                out.print("{\"success\":false,\"message\":\"Failed to load messages\"}");
            }
        }
    }
}
