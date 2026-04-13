import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LawyerRegisterServlet extends HttpServlet {
    
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
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        // Personal Information
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String city = request.getParameter("city");
        String password = request.getParameter("password");
        
        // Professional Information
        String barNumber = request.getParameter("barNumber");
        String stateLicensed = request.getParameter("stateLicensed");
        String yearsExperience = request.getParameter("yearsExperience");
        String specialization = request.getParameter("specialization");
        String hourlyRate = request.getParameter("hourlyRate");
        
        // Validation
        if (firstName == null || firstName.trim().isEmpty() ||
            lastName == null || lastName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() ||
            city == null || city.trim().isEmpty() ||
            password == null || password.length() < 8 ||
            barNumber == null || barNumber.trim().isEmpty() ||
            stateLicensed == null || stateLicensed.trim().isEmpty() ||
            yearsExperience == null || yearsExperience.trim().isEmpty() ||
            specialization == null || specialization.trim().isEmpty() ||
            hourlyRate == null || hourlyRate.trim().isEmpty()) {
            
            response.sendRedirect("lawyer-register.html?error=invalid");
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmtCheck = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtLawyer = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnectionUtil.getConnection();
            conn.setAutoCommit(false);
            
            // Check if email exists
            String checkSql = "SELECT user_id FROM users WHERE email = ?";
            pstmtCheck = conn.prepareStatement(checkSql);
            pstmtCheck.setString(1, email);
            rs = pstmtCheck.executeQuery();
            
            if (rs.next()) {
                conn.rollback();
                response.sendRedirect("lawyer-register.html?error=exists");
                return;
            }
            
            // Check if bar number exists
            String checkBarSql = "SELECT lawyer_id FROM lawyers WHERE bar_number = ?";
            PreparedStatement pstmtBar = conn.prepareStatement(checkBarSql);
            pstmtBar.setString(1, barNumber);
            ResultSet rsBar = pstmtBar.executeQuery();
            
            if (rsBar.next()) {
                rsBar.close();
                pstmtBar.close();
                conn.rollback();
                response.sendRedirect("lawyer-register.html?error=bar_exists");
                return;
            }
            rsBar.close();
            pstmtBar.close();
            
            String passwordHash = hashPassword(password);
            
            // Insert into users table
            String userSql = "INSERT INTO users (email, password_hash, user_type, first_name, last_name, phone_number, city) VALUES (?, ?, 'lawyer', ?, ?, ?, ?)";
            pstmtUser = conn.prepareStatement(userSql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmtUser.setString(1, email);
            pstmtUser.setString(2, passwordHash);
            pstmtUser.setString(3, firstName);
            pstmtUser.setString(4, lastName);
            pstmtUser.setString(5, phone);
            pstmtUser.setString(6, city);
            
            int rowsAffected = pstmtUser.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmtUser.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    
                    // Insert into lawyers table
                    String lawyerSql = "INSERT INTO lawyers (user_id, bar_number, state_licensed, years_experience, primary_specialization, city_practice, hourly_rate) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    pstmtLawyer = conn.prepareStatement(lawyerSql);
                    pstmtLawyer.setInt(1, userId);
                    pstmtLawyer.setString(2, barNumber);
                    pstmtLawyer.setString(3, stateLicensed);
                    pstmtLawyer.setString(4, yearsExperience);
                    pstmtLawyer.setString(5, specialization);
                    pstmtLawyer.setString(6, city);
                    pstmtLawyer.setString(7, hourlyRate);
                    pstmtLawyer.executeUpdate();
                    
                    conn.commit();
                    
                    // Success page
                    try (PrintWriter out = response.getWriter()) {
                        out.println("<!DOCTYPE html><html><head><title>Success</title>");
                        out.println("<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap' rel='stylesheet'>");
                        out.println("<style>");
                        out.println("body{font-family:'Inter',sans-serif;background:#F8FAFC;display:flex;justify-content:center;align-items:center;height:100vh;margin:0}");
                        out.println(".box{background:white;padding:3rem;border-radius:20px;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,0.3);max-width:500px}");
                        out.println(".icon{font-size:4rem;color:#10b981;margin-bottom:1rem}");
                        out.println("h2{color:#111827;margin-bottom:1rem}");
                        out.println("p{color:#6b7280;margin-bottom:2rem}");
                        out.println(".btn{display:inline-block;background:#C9A227;color:white;padding:1rem 2rem;text-decoration:none;border-radius:8px;font-weight:600}");
                        out.println(".info{background:#fef3c7;color:#92400e;padding:1rem;border-radius:8px;margin-bottom:1.5rem;font-size:0.9rem}");
                        out.println("</style></head><body>");
                        out.println("<div class='box'><div class='icon'>✓</div>");
                        out.println("<h2>Registration Successful!</h2>");
                        out.println("<div class='info'>⏳ Your account is under review. You'll receive an email once verified.</div>");
                        out.println("<p>Welcome to LegalConnect, Adv. " + firstName + " " + lastName + "!</p>");
                        out.println("<a href='login.html' class='btn'>Login Now</a></div></body></html>");
                    }
                } else {
                    conn.rollback();
                    response.sendRedirect("lawyer-register.html?error=failed");
                }
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { }
            }
            System.err.println("SQL error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("lawyer-register.html?error=database");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmtCheck != null) pstmtCheck.close();
                if (pstmtUser != null) pstmtUser.close();
                if (pstmtLawyer != null) pstmtLawyer.close();
                if (conn != null) conn.close();
            } catch (SQLException e) { }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("lawyer-register.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Lawyer Registration Servlet";
    }
}
