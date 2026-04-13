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

public class UpdateCaseStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                out.print("{\"success\":false,\"message\":\"Not logged in\"}");
                return;
            }

            String userType = (String) session.getAttribute("userType");
            if (!"lawyer".equals(userType)) {
                out.print("{\"success\":false,\"message\":\"Only lawyers can update case status\"}");
                return;
            }

            String caseIdParam = request.getParameter("caseId");
            String status = request.getParameter("status");
            String note = request.getParameter("note");
            if (caseIdParam == null || status == null) {
                out.print("{\"success\":false,\"message\":\"caseId and status are required\"}");
                return;
            }

            if (note != null) {
                note = note.trim();
            }

            status = status.trim().toLowerCase();
            if (!("active".equals(status) || "in_progress".equals(status) || "resolved".equals(status) || "closed".equals(status))) {
                out.print("{\"success\":false,\"message\":\"Invalid status\"}");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            int caseId = Integer.parseInt(caseIdParam);

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);
                conn.setAutoCommit(false);

                CaseAccessUtil.CaseParticipantInfo info = CaseAccessUtil.loadCaseParticipantInfo(conn, caseId);
                if (!CaseAccessUtil.canAccessCase(userType, userId, info)) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Access denied\"}");
                    return;
                }

                String updateSql = "UPDATE cases c " +
                                   "INNER JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                                   "SET c.case_status = ? " +
                                   "WHERE c.case_id = ? AND l.user_id = ?";
                int updated;
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, status);
                    pstmt.setInt(2, caseId);
                    pstmt.setInt(3, userId);
                    updated = pstmt.executeUpdate();
                }

                if (updated == 0) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Case not found or unauthorized\"}");
                    return;
                }

                FeatureSupportUtil.addCaseTimeline(conn, caseId, userId, status, (note == null || note.isEmpty()) ? null : note);

                int clientUserId = info.getClientUserId();
                FeatureSupportUtil.createNotification(
                    conn,
                    clientUserId,
                    "Case status updated",
                    "Your case #" + caseId + " is now marked as " + status.replace("_", " "),
                    "case_status",
                    caseId
                );

                if ("resolved".equals(status) || "closed".equals(status)) {
                    FeatureSupportUtil.createNotification(
                        conn,
                        clientUserId,
                        "Rate your lawyer",
                        "You can now submit a review for case #" + caseId,
                        "review",
                        caseId
                    );
                }

                conn.commit();
                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to update case status\"}");
            }
        }
    }
}
