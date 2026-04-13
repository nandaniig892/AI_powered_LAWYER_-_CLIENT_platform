import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class UpdateLawyerProfileServlet extends HttpServlet {

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
        
        // Get form parameters
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String phone = request.getParameter("phone");
        String city = request.getParameter("city");
        String barNumber = request.getParameter("barNumber");
        String stateLicensed = request.getParameter("stateLicensed");
        String yearsExperience = request.getParameter("yearsExperience");
        String primarySpecialization = request.getParameter("primarySpecialization");
        String cityPractice = request.getParameter("cityPractice");
        String hourlyRate = request.getParameter("hourlyRate");
        String bio = request.getParameter("bio");
        
        Connection conn = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtLawyer = null;
        
        try {
            conn = DBConnectionUtil.getConnection();
            
            conn.setAutoCommit(false);
            
            // Update users table
            String sqlUser = "UPDATE users SET first_name = ?, last_name = ?, phone_number = ?, city = ? WHERE user_id = ?";
            pstmtUser = conn.prepareStatement(sqlUser);
            pstmtUser.setString(1, firstName);
            pstmtUser.setString(2, lastName);
            pstmtUser.setString(3, phone);
            pstmtUser.setString(4, city);
            pstmtUser.setInt(5, userId);
            pstmtUser.executeUpdate();
            
            // Update lawyers table
            String sqlLawyer = "UPDATE lawyers SET bar_number = ?, state_licensed = ?, years_experience = ?, " +
                              "primary_specialization = ?, city_practice = ?, hourly_rate = ?, bio = ? " +
                              "WHERE user_id = ?";
            pstmtLawyer = conn.prepareStatement(sqlLawyer);
            pstmtLawyer.setString(1, barNumber);
            pstmtLawyer.setString(2, stateLicensed);
            pstmtLawyer.setString(3, yearsExperience);
            pstmtLawyer.setString(4, primarySpecialization);
            pstmtLawyer.setString(5, cityPractice);
            pstmtLawyer.setString(6, hourlyRate);
            pstmtLawyer.setString(7, bio);
            pstmtLawyer.setInt(8, userId);
            pstmtLawyer.executeUpdate();
            
            conn.commit();
            
            // Update session attributes
            session.setAttribute("firstName", firstName);
            session.setAttribute("lastName", lastName);
            
            System.out.println("Lawyer profile updated successfully for user_id: " + userId);
            
            response.sendRedirect("lawyer-profile.jsp?success=true");
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {}
            }
            e.printStackTrace();
            System.err.println("Error updating lawyer profile: " + e.getMessage());
            response.sendRedirect("lawyer-profile.jsp?error=failed");
        } finally {
            try {
                if (pstmtUser != null) pstmtUser.close();
                if (pstmtLawyer != null) pstmtLawyer.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {}
        }
    }
}
