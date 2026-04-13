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

public class GetCaseChatListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                out.print("[]");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            String userType = (String) session.getAttribute("userType");
            if (!"client".equals(userType) && !"lawyer".equals(userType)) {
                out.print("[]");
                return;
            }

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                String sql;
                if ("client".equals(userType)) {
                    sql = "SELECT c.case_id, c.case_title, c.case_status, " +
                          "u.first_name AS other_first, u.last_name AS other_last, " +
                          "SUM(CASE WHEN cm.is_read = FALSE AND cm.sender_user_id <> ? THEN 1 ELSE 0 END) AS unread_count " +
                          "FROM cases c " +
                          "INNER JOIN clients cl ON c.client_id = cl.client_id " +
                          "LEFT JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                          "LEFT JOIN users u ON l.user_id = u.user_id " +
                          "LEFT JOIN case_messages cm ON cm.case_id = c.case_id " +
                          "WHERE cl.user_id = ? " +
                          "GROUP BY c.case_id, c.case_title, c.case_status, u.first_name, u.last_name " +
                          "ORDER BY c.created_at DESC";
                } else {
                    sql = "SELECT c.case_id, c.case_title, c.case_status, " +
                          "u.first_name AS other_first, u.last_name AS other_last, " +
                          "SUM(CASE WHEN cm.is_read = FALSE AND cm.sender_user_id <> ? THEN 1 ELSE 0 END) AS unread_count " +
                          "FROM cases c " +
                          "INNER JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                          "INNER JOIN clients cl ON c.client_id = cl.client_id " +
                          "INNER JOIN users u ON cl.user_id = u.user_id " +
                          "LEFT JOIN case_messages cm ON cm.case_id = c.case_id " +
                          "WHERE l.user_id = ? " +
                          "GROUP BY c.case_id, c.case_title, c.case_status, u.first_name, u.last_name " +
                          "ORDER BY c.created_at DESC";
                }

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, userId);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        out.print("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                out.print(",");
                            }
                            String otherFirst = rs.getString("other_first");
                            String otherLast = rs.getString("other_last");
                            String otherName = (otherFirst == null || otherLast == null)
                                ? "Not assigned yet"
                                : otherFirst + " " + otherLast;

                            out.print("{");
                            out.print("\"caseId\":" + rs.getInt("case_id") + ",");
                            out.print("\"caseTitle\":\"" + FeatureSupportUtil.escapeJson(rs.getString("case_title")) + "\",");
                            out.print("\"caseStatus\":\"" + FeatureSupportUtil.escapeJson(rs.getString("case_status")) + "\",");
                            out.print("\"otherParty\":\"" + FeatureSupportUtil.escapeJson(otherName) + "\",");
                            out.print("\"unreadCount\":" + rs.getInt("unread_count"));
                            out.print("}");
                            first = false;
                        }
                        out.print("]");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                out.print("[]");
            }
        }
    }
}
