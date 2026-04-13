import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class UpdateProfileServlet extends HttpServlet {

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
        
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String phone = request.getParameter("phone");
        String city = request.getParameter("city");
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBConnectionUtil.getConnection();
            
            String sql = "UPDATE users SET first_name = ?, last_name = ?, phone_number = ?, city = ? WHERE user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, phone);
            pstmt.setString(4, city);
            pstmt.setInt(5, userId);
            
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                // Update session attributes
                session.setAttribute("firstName", firstName);
                session.setAttribute("lastName", lastName);
                
                response.sendRedirect("client-profile.jsp?success=profile");
            } else {
                response.sendRedirect("client-profile.jsp?error=failed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("client-profile.jsp?error=failed");
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {}
        }
    }
}
