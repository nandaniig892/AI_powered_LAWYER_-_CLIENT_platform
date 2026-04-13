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

public class GetCaseTimelineServlet extends HttpServlet {

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

            String caseIdParam = request.getParameter("caseId");
            if (caseIdParam == null || caseIdParam.trim().isEmpty()) {
                out.print("[]");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            String userType = (String) session.getAttribute("userType");
            int caseId = Integer.parseInt(caseIdParam);

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                CaseAccessUtil.CaseParticipantInfo info = CaseAccessUtil.loadCaseParticipantInfo(conn, caseId);
                if (!CaseAccessUtil.canAccessCase(userType, userId, info)) {
                    out.print("[]");
                    return;
                }

                String sql = "SELECT ct.timeline_id, ct.status, ct.note, ct.created_at, u.first_name, u.last_name " +
                             "FROM case_timeline ct " +
                             "LEFT JOIN users u ON ct.actor_user_id = u.user_id " +
                             "WHERE ct.case_id = ? ORDER BY ct.created_at ASC";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, caseId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        out.print("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                out.print(",");
                            }
                            String actor = rs.getString("first_name");
                            String actorName = (actor == null) ? "System" : (rs.getString("first_name") + " " + rs.getString("last_name"));
                            out.print("{");
                            out.print("\"id\":" + rs.getInt("timeline_id") + ",");
                            out.print("\"status\":\"" + FeatureSupportUtil.escapeJson(rs.getString("status")) + "\",");
                            out.print("\"note\":\"" + FeatureSupportUtil.escapeJson(rs.getString("note")) + "\",");
                            out.print("\"actor\":\"" + FeatureSupportUtil.escapeJson(actorName) + "\",");
                            out.print("\"createdAt\":\"" + rs.getTimestamp("created_at").toString() + "\"");
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
