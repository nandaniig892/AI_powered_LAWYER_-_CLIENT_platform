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

public class SubmitReviewServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null || !"client".equals(session.getAttribute("userType"))) {
                out.print("{\"success\":false,\"message\":\"Client login required\"}");
                return;
            }

            String caseIdParam = request.getParameter("caseId");
            String ratingParam = request.getParameter("rating");
            String reviewText = request.getParameter("reviewText");
            if (caseIdParam == null || ratingParam == null) {
                out.print("{\"success\":false,\"message\":\"caseId and rating are required\"}");
                return;
            }

            int caseId = Integer.parseInt(caseIdParam);
            int rating = Integer.parseInt(ratingParam);
            if (rating < 1 || rating > 5) {
                out.print("{\"success\":false,\"message\":\"Rating should be between 1 and 5\"}");
                return;
            }

            int clientUserId = (Integer) session.getAttribute("userId");
            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);
                conn.setAutoCommit(false);

                String validateSql = "SELECT c.case_status, lu.user_id AS lawyer_user_id " +
                                     "FROM cases c " +
                                     "INNER JOIN clients cl ON c.client_id = cl.client_id " +
                                     "LEFT JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                                     "LEFT JOIN users lu ON l.user_id = lu.user_id " +
                                     "WHERE c.case_id = ? AND cl.user_id = ?";
                Integer lawyerUserId = null;
                String caseStatus = null;
                try (PreparedStatement validateStmt = conn.prepareStatement(validateSql)) {
                    validateStmt.setInt(1, caseId);
                    validateStmt.setInt(2, clientUserId);
                    try (ResultSet rs = validateStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            out.print("{\"success\":false,\"message\":\"Case not found\"}");
                            return;
                        }
                        caseStatus = rs.getString("case_status");
                        int raw = rs.getInt("lawyer_user_id");
                        lawyerUserId = rs.wasNull() ? null : raw;
                    }
                }

                if (lawyerUserId == null) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Lawyer not assigned for this case\"}");
                    return;
                }
                if ("pending".equalsIgnoreCase(caseStatus)) {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Review can be added after case starts\"}");
                    return;
                }

                String checkSql = "SELECT review_id FROM lawyer_reviews WHERE case_id = ?";
                boolean exists;
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, caseId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        exists = rs.next();
                    }
                }

                if (exists) {
                    String updateSql = "UPDATE lawyer_reviews SET rating = ?, review_text = ?, created_at = CURRENT_TIMESTAMP WHERE case_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, rating);
                        updateStmt.setString(2, reviewText);
                        updateStmt.setInt(3, caseId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO lawyer_reviews (case_id, client_user_id, lawyer_user_id, rating, review_text) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, caseId);
                        insertStmt.setInt(2, clientUserId);
                        insertStmt.setInt(3, lawyerUserId);
                        insertStmt.setInt(4, rating);
                        insertStmt.setString(5, reviewText);
                        insertStmt.executeUpdate();
                    }
                }

                FeatureSupportUtil.createNotification(
                    conn,
                    lawyerUserId,
                    "New client review",
                    "You received a " + rating + "/5 review for case #" + caseId,
                    "review",
                    caseId
                );

                conn.commit();
                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to submit review\"}");
            }
        }
    }
}
