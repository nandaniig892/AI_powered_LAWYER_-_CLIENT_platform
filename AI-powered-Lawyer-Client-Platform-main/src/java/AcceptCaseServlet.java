import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AcceptCaseServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null || !"lawyer".equals(session.getAttribute("userType"))) {
                out.print("{\"success\":false,\"message\":\"Lawyer login required\"}");
                return;
            }

            String caseIdParam = request.getParameter("caseId");
            if (caseIdParam == null || caseIdParam.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"caseId is required\"}");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            int caseId = Integer.parseInt(caseIdParam);

            Connection conn = null;
            try {
                conn = DBConnectionUtil.getConnection();
                FeatureSchemaUtil.ensureInitialized(conn);
                conn.setAutoCommit(false);

                Integer lawyerId = getLawyerId(conn, userId);
                if (lawyerId == null) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Lawyer profile not found\"}");
                    return;
                }

                String updateSql = "UPDATE cases SET case_status = 'active', lawyer_id = ? WHERE case_id = ? AND case_status = 'pending'";
                int rowsUpdated;
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, lawyerId);
                    pstmt.setInt(2, caseId);
                    rowsUpdated = pstmt.executeUpdate();
                }

                if (rowsUpdated == 0) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Case is no longer available\"}");
                    return;
                }

                CaseAccessUtil.CaseParticipantInfo info = CaseAccessUtil.loadCaseParticipantInfo(conn, caseId);
                FeatureSupportUtil.addCaseTimeline(conn, caseId, userId, "active", "Case accepted by lawyer");

                if (info != null) {
                    FeatureSupportUtil.createNotification(
                        conn,
                        info.getClientUserId(),
                        "Case accepted",
                        "Your case #" + caseId + " has been accepted by a lawyer.",
                        "case_status",
                        caseId
                    );
                }

                conn.commit();
                out.print("{\"success\":true,\"message\":\"Case accepted successfully\"}");
            } catch (Exception e) {
                e.printStackTrace();
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        rollbackEx.printStackTrace();
                    }
                }
                out.print("{\"success\":false,\"message\":\"Failed to accept case\"}");
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    private Integer getLawyerId(Connection conn, int userId) throws SQLException {
        String sql = "SELECT lawyer_id FROM lawyers WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("lawyer_id");
                }
            }
        }
        return null;
    }
}
