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

public class UpdateComplaintStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null || !"admin".equals(session.getAttribute("userType"))) {
                out.print("{\"success\":false,\"message\":\"Admin access required\"}");
                return;
            }

            String complaintIdParam = request.getParameter("complaintId");
            String status = request.getParameter("status");
            String resolutionNote = request.getParameter("resolutionNote");
            if (complaintIdParam == null || status == null) {
                out.print("{\"success\":false,\"message\":\"complaintId and status are required\"}");
                return;
            }

            status = status.trim().toLowerCase();
            if (!("open".equals(status) || "in_review".equals(status) || "resolved".equals(status) || "rejected".equals(status))) {
                out.print("{\"success\":false,\"message\":\"Invalid complaint status\"}");
                return;
            }

            int adminUserId = (Integer) session.getAttribute("userId");
            int complaintId = Integer.parseInt(complaintIdParam);

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);
                conn.setAutoCommit(false);

                Integer complainantUserId = null;
                Integer caseId = null;
                String selectSql = "SELECT complainant_user_id, case_id FROM complaints WHERE complaint_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, complaintId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            out.print("{\"success\":false,\"message\":\"Complaint not found\"}");
                            return;
                        }
                        complainantUserId = rs.getInt("complainant_user_id");
                        caseId = rs.getInt("case_id");
                    }
                }

                String updateSql = "UPDATE complaints SET status = ?, resolution_note = ?, resolved_at = ? WHERE complaint_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, status);
                    pstmt.setString(2, resolutionNote);
                    if ("resolved".equals(status) || "rejected".equals(status)) {
                        pstmt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
                    } else {
                        pstmt.setNull(3, java.sql.Types.TIMESTAMP);
                    }
                    pstmt.setInt(4, complaintId);
                    pstmt.executeUpdate();
                }

                FeatureSupportUtil.createAdminLog(
                    conn,
                    adminUserId,
                    complainantUserId,
                    "UPDATE_COMPLAINT",
                    "Complaint #" + complaintId + " set to " + status
                );

                FeatureSupportUtil.createNotification(
                    conn,
                    complainantUserId,
                    "Complaint update",
                    "Your complaint for case #" + caseId + " is now " + status.replace("_", " "),
                    "complaint",
                    caseId
                );

                conn.commit();
                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to update complaint\"}");
            }
        }
    }
}
