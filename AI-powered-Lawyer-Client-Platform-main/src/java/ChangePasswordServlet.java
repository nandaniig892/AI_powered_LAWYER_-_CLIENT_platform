import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ChangePasswordServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("login.html");
            return;
        }
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect("login.html");
            return;
        }
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnectionUtil.getConnection();
            
            // Verify current password
            String checkSql = "SELECT password_hash FROM users WHERE user_id = ?";
            pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                response.sendRedirect("client-profile.jsp?error=failed");
                return;
            }
            
            String storedHash = rs.getString("password_hash");
            String currentPasswordHash = hashPassword(currentPassword);
            
            if (!storedHash.equals(currentPasswordHash)) {
                response.sendRedirect("client-profile.jsp?error=wrongpassword");
                return;
            }
            
            // Update password
            String newPasswordHash = hashPassword(newPassword);
            String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
            pstmt = conn.prepareStatement(updateSql);
            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);
            
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                response.sendRedirect("client-profile.jsp?success=password");
            } else {
                response.sendRedirect("client-profile.jsp?error=failed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("client-profile.jsp?error=failed");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {}
        }
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
