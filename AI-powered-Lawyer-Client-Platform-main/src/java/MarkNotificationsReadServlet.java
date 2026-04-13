import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class MarkNotificationsReadServlet extends HttpServlet {

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

            int userId = (Integer) session.getAttribute("userId");
            String notificationIdParam = request.getParameter("notificationId");

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                if (notificationIdParam != null && !notificationIdParam.trim().isEmpty()) {
                    String singleSql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ? AND user_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(singleSql)) {
                        pstmt.setInt(1, Integer.parseInt(notificationIdParam));
                        pstmt.setInt(2, userId);
                        pstmt.executeUpdate();
                    }
                } else {
                    String allSql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
                    try (PreparedStatement pstmt = conn.prepareStatement(allSql)) {
                        pstmt.setInt(1, userId);
                        pstmt.executeUpdate();
                    }
                }

                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to update notifications\"}");
            }
        }
    }
}
