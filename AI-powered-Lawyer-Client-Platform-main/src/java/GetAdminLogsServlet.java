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

public class GetAdminLogsServlet extends HttpServlet {

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

                String sql = "SELECT al.log_id, al.action_type, al.details, al.created_at, " +
                             "au.first_name AS admin_first, au.last_name AS admin_last, " +
                             "tu.first_name AS target_first, tu.last_name AS target_last " +
                             "FROM admin_logs al " +
                             "INNER JOIN users au ON al.admin_user_id = au.user_id " +
                             "LEFT JOIN users tu ON al.target_user_id = tu.user_id " +
                             "ORDER BY al.created_at DESC LIMIT 100";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    out.print("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) {
                            out.print(",");
                        }
                        String targetName = rs.getString("target_first");
                        String targetLabel = targetName == null ? "" : rs.getString("target_first") + " " + rs.getString("target_last");
                        out.print("{");
                        out.print("\"id\":" + rs.getInt("log_id") + ",");
                        out.print("\"action\":\"" + FeatureSupportUtil.escapeJson(rs.getString("action_type")) + "\",");
                        out.print("\"details\":\"" + FeatureSupportUtil.escapeJson(rs.getString("details")) + "\",");
                        out.print("\"admin\":\"" + FeatureSupportUtil.escapeJson(rs.getString("admin_first") + " " + rs.getString("admin_last")) + "\",");
                        out.print("\"target\":\"" + FeatureSupportUtil.escapeJson(targetLabel) + "\",");
                        out.print("\"createdAt\":\"" + rs.getTimestamp("created_at").toString() + "\"");
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
