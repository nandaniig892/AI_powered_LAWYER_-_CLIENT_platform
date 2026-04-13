import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GetLawyerStatsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            out.print("{\"newCases\":0,\"activeCases\":0,\"totalClients\":0,\"avgRating\":0.0}");
            return;
        }
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            out.print("{\"newCases\":0,\"activeCases\":0,\"totalClients\":0,\"avgRating\":0.0}");
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        int newCases = 0;
        int activeCases = 0;
        int totalClients = 0;
        double avgRating = 0.0;
        
        try {
            conn = DBConnectionUtil.getConnection();
            FeatureSchemaUtil.ensureInitialized(conn);
            
            // Get lawyer_id
            String getLawyerIdSql = "SELECT lawyer_id FROM lawyers WHERE user_id = ?";
            pstmt = conn.prepareStatement(getLawyerIdSql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                out.print("{\"newCases\":0,\"activeCases\":0,\"totalClients\":0,\"avgRating\":0.0}");
                return;
            }
            
            int lawyerId = rs.getInt("lawyer_id");
            rs.close();
            pstmt.close();
            
            // Count new cases (pending status, all cases)
            String newCasesSql = "SELECT COUNT(*) as count FROM cases WHERE case_status = 'pending'";
            pstmt = conn.prepareStatement(newCasesSql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                newCases = rs.getInt("count");
            }
            rs.close();
            pstmt.close();
            
            // Count active cases (assigned to this lawyer)
            String activeCasesSql = "SELECT COUNT(*) as count FROM cases WHERE lawyer_id = ? AND case_status IN ('active','in_progress')";
            pstmt = conn.prepareStatement(activeCasesSql);
            pstmt.setInt(1, lawyerId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                activeCases = rs.getInt("count");
            }
            rs.close();
            pstmt.close();
            
            // Count unique clients from active cases
            String clientsSql = "SELECT COUNT(DISTINCT client_id) as count FROM cases WHERE lawyer_id = ?";
            pstmt = conn.prepareStatement(clientsSql);
            pstmt.setInt(1, lawyerId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                totalClients = rs.getInt("count");
            }
            rs.close();
            pstmt.close();
            
            // Calculate average rating from submitted client reviews
            String ratingSql = "SELECT AVG(rating) AS avg_rating FROM lawyer_reviews WHERE lawyer_user_id = ?";
            pstmt = conn.prepareStatement(ratingSql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                avgRating = rs.getDouble("avg_rating");
                if (rs.wasNull()) {
                    avgRating = 0.0;
                }
            }
            rs.close();
            pstmt.close();
            
            out.print("{");
            out.print("\"newCases\":" + newCases + ",");
            out.print("\"activeCases\":" + activeCases + ",");
            out.print("\"totalClients\":" + totalClients + ",");
            out.print("\"avgRating\":" + avgRating);
            out.print("}");
            
            System.out.println("Stats for lawyer " + lawyerId + ": New=" + newCases + ", Active=" + activeCases + ", Clients=" + totalClients);
            
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"newCases\":0,\"activeCases\":0,\"totalClients\":0,\"avgRating\":0.0}");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {}
        }
    }
}
