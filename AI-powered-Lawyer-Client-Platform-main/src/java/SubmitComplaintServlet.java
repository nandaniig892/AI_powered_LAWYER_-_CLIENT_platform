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

public class SubmitComplaintServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                out.print("{\"success\":false,\"message\":\"Login required\"}");
                return;
            }

            String caseIdParam = request.getParameter("caseId");
            String description = request.getParameter("description");
            if (caseIdParam == null || description == null || description.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"caseId and description are required\"}");
                return;
            }

            int caseId = Integer.parseInt(caseIdParam);
            int complainantUserId = (Integer) session.getAttribute("userId");
            String userType = (String) session.getAttribute("userType");

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);
                conn.setAutoCommit(false);

                CaseAccessUtil.CaseParticipantInfo info = CaseAccessUtil.loadCaseParticipantInfo(conn, caseId);
                if (!CaseAccessUtil.canAccessCase(userType, complainantUserId, info)) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Case access denied\"}");
                    return;
                }

                Integer againstUserId = CaseAccessUtil.getCounterpartyUserId(userType, complainantUserId, info);
                if (againstUserId == null) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Counterparty is not available for complaint\"}");
                    return;
                }

                String insertSql = "INSERT INTO complaints (case_id, complainant_user_id, against_user_id, description, status) " +
                                   "VALUES (?, ?, ?, ?, 'open')";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, caseId);
                    insertStmt.setInt(2, complainantUserId);
                    insertStmt.setInt(3, againstUserId);
                    insertStmt.setString(4, description.trim());
                    insertStmt.executeUpdate();
                }

                String adminSql = "SELECT user_id FROM users WHERE user_type = 'admin' AND is_active = TRUE";
                try (PreparedStatement adminStmt = conn.prepareStatement(adminSql);
                     ResultSet adminRs = adminStmt.executeQuery()) {
                    while (adminRs.next()) {
                        FeatureSupportUtil.createNotification(
                            conn,
                            adminRs.getInt("user_id"),
                            "New complaint submitted",
                            "A new complaint was submitted for case #" + caseId,
                            "complaint",
                            caseId
                        );
                    }
                }

                conn.commit();
                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to submit complaint\"}");
            }
        }
    }
}
