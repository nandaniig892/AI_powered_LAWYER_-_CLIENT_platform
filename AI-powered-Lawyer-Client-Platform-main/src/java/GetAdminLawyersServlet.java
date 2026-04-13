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

public class GetAdminLawyersServlet extends HttpServlet {

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

                String sql = "SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone_number, u.city, u.is_active, " +
                             "l.lawyer_id, l.bar_number, l.state_licensed, l.years_experience, l.primary_specialization, l.hourly_rate, l.is_verified " +
                             "FROM users u " +
                             "INNER JOIN lawyers l ON u.user_id = l.user_id " +
                             "WHERE u.user_type = 'lawyer' " +
                             "ORDER BY l.is_verified ASC, u.user_id DESC";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    out.print("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) {
                            out.print(",");
                        }
                        out.print("{");
                        out.print("\"userId\":" + rs.getInt("user_id") + ",");
                        out.print("\"lawyerId\":" + rs.getInt("lawyer_id") + ",");
                        out.print("\"firstName\":\"" + FeatureSupportUtil.escapeJson(rs.getString("first_name")) + "\",");
                        out.print("\"lastName\":\"" + FeatureSupportUtil.escapeJson(rs.getString("last_name")) + "\",");
                        out.print("\"email\":\"" + FeatureSupportUtil.escapeJson(rs.getString("email")) + "\",");
                        out.print("\"phone\":\"" + FeatureSupportUtil.escapeJson(rs.getString("phone_number")) + "\",");
                        out.print("\"city\":\"" + FeatureSupportUtil.escapeJson(rs.getString("city")) + "\",");
                        out.print("\"barNumber\":\"" + FeatureSupportUtil.escapeJson(rs.getString("bar_number")) + "\",");
                        out.print("\"stateLicensed\":\"" + FeatureSupportUtil.escapeJson(rs.getString("state_licensed")) + "\",");
                        out.print("\"yearsExperience\":\"" + FeatureSupportUtil.escapeJson(rs.getString("years_experience")) + "\",");
                        out.print("\"specialization\":\"" + FeatureSupportUtil.escapeJson(rs.getString("primary_specialization")) + "\",");
                        out.print("\"hourlyRate\":\"" + FeatureSupportUtil.escapeJson(rs.getString("hourly_rate")) + "\",");
                        out.print("\"isVerified\":" + rs.getBoolean("is_verified") + ",");
                        out.print("\"isActive\":" + rs.getBoolean("is_active"));
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
