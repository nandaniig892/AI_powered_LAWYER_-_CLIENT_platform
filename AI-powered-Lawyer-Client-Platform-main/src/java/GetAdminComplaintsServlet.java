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

public class GetAdminComplaintsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null || !"admin".equals(session.getAttribute("userType"))) {
                out.print("[]");
                return;
            }

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                String sql = "SELECT c.complaint_id, c.case_id, c.description, c.status, c.resolution_note, c.created_at, c.resolved_at, " +
                             "comp.first_name AS complainant_first, comp.last_name AS complainant_last, " +
                             "ag.first_name AS against_first, ag.last_name AS against_last " +
                             "FROM complaints c " +
                             "INNER JOIN users comp ON c.complainant_user_id = comp.user_id " +
                             "INNER JOIN users ag ON c.against_user_id = ag.user_id " +
                             "ORDER BY c.created_at DESC";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    out.print("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) {
                            out.print(",");
                        }
                        out.print("{");
                        out.print("\"complaintId\":" + rs.getInt("complaint_id") + ",");
                        out.print("\"caseId\":" + rs.getInt("case_id") + ",");
                        out.print("\"description\":\"" + FeatureSupportUtil.escapeJson(rs.getString("description")) + "\",");
                        out.print("\"status\":\"" + FeatureSupportUtil.escapeJson(rs.getString("status")) + "\",");
                        out.print("\"resolutionNote\":\"" + FeatureSupportUtil.escapeJson(rs.getString("resolution_note")) + "\",");
                        out.print("\"complainant\":\"" + FeatureSupportUtil.escapeJson(rs.getString("complainant_first") + " " + rs.getString("complainant_last")) + "\",");
                        out.print("\"against\":\"" + FeatureSupportUtil.escapeJson(rs.getString("against_first") + " " + rs.getString("against_last")) + "\",");
                        out.print("\"createdAt\":\"" + rs.getTimestamp("created_at").toString() + "\",");
                        if (rs.getTimestamp("resolved_at") == null) {
                            out.print("\"resolvedAt\":null");
                        } else {
                            out.print("\"resolvedAt\":\"" + rs.getTimestamp("resolved_at").toString() + "\"");
                        }
                        out.print("}");
                        first = false;
                    }
                    out.print("]");
                }
            } catch (Exception e) {
                e.printStackTrace();
                out.print("[]");
            }
        }
    }
}
