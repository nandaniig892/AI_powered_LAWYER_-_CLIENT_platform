import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AdminLawyerActionServlet extends HttpServlet {

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

            String action = request.getParameter("action");
            String targetUserIdParam = request.getParameter("targetUserId");
            if (action == null || targetUserIdParam == null) {
                out.print("{\"success\":false,\"message\":\"action and targetUserId are required\"}");
                return;
            }

            int adminUserId = (Integer) session.getAttribute("userId");
            int targetUserId = Integer.parseInt(targetUserIdParam);
            action = action.trim().toLowerCase();

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);
                conn.setAutoCommit(false);

                if ("verify".equals(action)) {
                    try (PreparedStatement verifyLawyer = conn.prepareStatement("UPDATE lawyers SET is_verified = TRUE WHERE user_id = ?")) {
                        verifyLawyer.setInt(1, targetUserId);
                        verifyLawyer.executeUpdate();
                    }
                    try (PreparedStatement activateUser = conn.prepareStatement("UPDATE users SET is_active = TRUE WHERE user_id = ?")) {
                        activateUser.setInt(1, targetUserId);
                        activateUser.executeUpdate();
                    }
                    FeatureSupportUtil.createNotification(conn, targetUserId, "Lawyer account verified", "Your lawyer profile has been approved by admin.", "admin", null);
                    FeatureSupportUtil.createAdminLog(conn, adminUserId, targetUserId, "VERIFY_LAWYER", "Verified lawyer profile");
                } else if ("suspend".equals(action)) {
                    try (PreparedStatement suspendUser = conn.prepareStatement("UPDATE users SET is_active = FALSE WHERE user_id = ?")) {
                        suspendUser.setInt(1, targetUserId);
                        suspendUser.executeUpdate();
                    }
                    FeatureSupportUtil.createNotification(conn, targetUserId, "Account suspended", "Your account was suspended by admin. Contact support for details.", "admin", null);
                    FeatureSupportUtil.createAdminLog(conn, adminUserId, targetUserId, "SUSPEND_USER", "Suspended lawyer account");
                } else if ("activate".equals(action)) {
                    try (PreparedStatement activateUser = conn.prepareStatement("UPDATE users SET is_active = TRUE WHERE user_id = ?")) {
                        activateUser.setInt(1, targetUserId);
                        activateUser.executeUpdate();
                    }
                    FeatureSupportUtil.createNotification(conn, targetUserId, "Account activated", "Your account was reactivated by admin.", "admin", null);
                    FeatureSupportUtil.createAdminLog(conn, adminUserId, targetUserId, "ACTIVATE_USER", "Reactivated lawyer account");
                } else if ("unverify".equals(action)) {
                    try (PreparedStatement unverifyLawyer = conn.prepareStatement("UPDATE lawyers SET is_verified = FALSE WHERE user_id = ?")) {
                        unverifyLawyer.setInt(1, targetUserId);
                        unverifyLawyer.executeUpdate();
                    }
                    FeatureSupportUtil.createNotification(conn, targetUserId, "Verification removed", "Your lawyer verification has been removed by admin.", "admin", null);
                    FeatureSupportUtil.createAdminLog(conn, adminUserId, targetUserId, "UNVERIFY_LAWYER", "Removed lawyer verification");
                } else {
                    conn.rollback();
                    out.print("{\"success\":false,\"message\":\"Invalid action\"}");
                    return;
                }

                conn.commit();
                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Action failed\"}");
            }
        }
    }
}
