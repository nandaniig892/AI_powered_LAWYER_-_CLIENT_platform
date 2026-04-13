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

public class GetClientCaseTrackerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null || !"client".equals(session.getAttribute("userType"))) {
                out.print("[]");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                String sql = "SELECT c.case_id, c.case_title, c.case_type, c.case_status, c.city, c.created_at, " +
                             "lu.user_id AS lawyer_user_id, lu.first_name AS lawyer_first, lu.last_name AS lawyer_last, " +
                             "lr.review_id " +
                             "FROM cases c " +
                             "INNER JOIN clients cl ON c.client_id = cl.client_id " +
                             "LEFT JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                             "LEFT JOIN users lu ON l.user_id = lu.user_id " +
                             "LEFT JOIN lawyer_reviews lr ON lr.case_id = c.case_id " +
                             "WHERE cl.user_id = ? " +
                             "ORDER BY c.created_at DESC";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, userId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        out.print("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                out.print(",");
                            }
                            int lawyerUserIdRaw = rs.getInt("lawyer_user_id");
                            boolean hasLawyer = !rs.wasNull();
                            int reviewIdRaw = rs.getInt("review_id");
                            boolean hasReview = !rs.wasNull();
                            String status = rs.getString("case_status");
                            boolean canReview = hasLawyer && !hasReview && !"pending".equalsIgnoreCase(status);

                            out.print("{");
                            out.print("\"caseId\":" + rs.getInt("case_id") + ",");
                            out.print("\"title\":\"" + FeatureSupportUtil.escapeJson(rs.getString("case_title")) + "\",");
                            out.print("\"type\":\"" + FeatureSupportUtil.escapeJson(rs.getString("case_type")) + "\",");
                            out.print("\"status\":\"" + FeatureSupportUtil.escapeJson(status) + "\",");
                            out.print("\"city\":\"" + FeatureSupportUtil.escapeJson(rs.getString("city")) + "\",");
                            out.print("\"createdAt\":\"" + rs.getTimestamp("created_at").toString() + "\",");
                            out.print("\"hasLawyer\":" + hasLawyer + ",");
                            if (hasLawyer) {
                                out.print("\"lawyerUserId\":" + lawyerUserIdRaw + ",");
                                out.print("\"lawyerName\":\"" + FeatureSupportUtil.escapeJson(rs.getString("lawyer_first") + " " + rs.getString("lawyer_last")) + "\",");
                            } else {
                                out.print("\"lawyerUserId\":null,");
                                out.print("\"lawyerName\":\"Not assigned\",");
                            }
                            out.print("\"hasReview\":" + hasReview + ",");
                            out.print("\"canReview\":" + canReview);
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
