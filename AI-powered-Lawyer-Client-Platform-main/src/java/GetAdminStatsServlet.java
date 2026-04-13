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

public class GetAdminStatsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null || !"admin".equals(session.getAttribute("userType"))) {
                out.print("{\"pendingVerifications\":0,\"openComplaints\":0,\"activeUsers\":0,\"activeCases\":0}");
                return;
            }

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                int pendingVerifications = scalar(conn, "SELECT COUNT(*) FROM lawyers WHERE is_verified = FALSE");
                int openComplaints = scalar(conn, "SELECT COUNT(*) FROM complaints WHERE status IN ('open','in_review')");
                int activeUsers = scalar(conn, "SELECT COUNT(*) FROM users WHERE is_active = TRUE");
                int activeCases = scalar(conn, "SELECT COUNT(*) FROM cases WHERE case_status IN ('active','in_progress')");

                out.print("{");
                out.print("\"pendingVerifications\":" + pendingVerifications + ",");
                out.print("\"openComplaints\":" + openComplaints + ",");
                out.print("\"activeUsers\":" + activeUsers + ",");
                out.print("\"activeCases\":" + activeCases);
                out.print("}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"pendingVerifications\":0,\"openComplaints\":0,\"activeUsers\":0,\"activeCases\":0}");
            }
        }
    }

    private int scalar(Connection conn, String sql) throws Exception {
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
