import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GetCasesServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        // Debug: Check session
        System.out.println("=== GetCasesServlet Debug ===");
        System.out.println("Session exists: " + (session != null));
        
        if (session == null) {
            System.out.println("ERROR: No session found");
            out.print("[]");
            return;
        }
        
        Integer userId = (Integer) session.getAttribute("userId");
        String firstName = (String) session.getAttribute("firstName");
        
        System.out.println("User ID from session: " + userId);
        System.out.println("First name from session: " + firstName);
        
        if (userId == null) {
            System.out.println("ERROR: userId is null in session");
            out.print("[]");
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnectionUtil.getConnection();
            
            System.out.println("Database connected successfully");
            
            // First get client_id
            String getClientIdSql = "SELECT client_id FROM clients WHERE user_id = ?";
            PreparedStatement pstmtClientId = conn.prepareStatement(getClientIdSql);
            pstmtClientId.setInt(1, userId);
            ResultSet rsClientId = pstmtClientId.executeQuery();
            
            if (!rsClientId.next()) {
                System.out.println("ERROR: No client found for user_id: " + userId);
                out.print("[]");
                rsClientId.close();
                pstmtClientId.close();
                return;
            }
            
            int clientId = rsClientId.getInt("client_id");
            System.out.println("Found client_id: " + clientId);
            rsClientId.close();
            pstmtClientId.close();
            
            // Now get cases
            String sql = "SELECT * FROM cases WHERE client_id = ? ORDER BY created_at DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, clientId);
            
            System.out.println("Executing query: " + sql);
            System.out.println("With client_id: " + clientId);
            
            rs = pstmt.executeQuery();
            
            out.print("[");
            boolean first = true;
            int count = 0;
            
            while (rs.next()) {
                if (!first) out.print(",");
                
                count++;
                
                String title = rs.getString("case_title");
                String type = rs.getString("case_type");
                String cityVal = rs.getString("city");
                String urgency = rs.getString("urgency");
                String status = rs.getString("case_status");
                Timestamp createdAt = rs.getTimestamp("created_at");
                
                System.out.println("Case " + count + ": " + title);
                
                out.print("{");
                out.print("\"id\":" + rs.getInt("case_id") + ",");
                out.print("\"title\":\"" + escapeJson(title) + "\",");
                out.print("\"type\":\"" + escapeJson(type) + "\",");
                out.print("\"city\":\"" + escapeJson(cityVal) + "\",");
                out.print("\"urgency\":\"" + escapeJson(urgency) + "\",");
                out.print("\"status\":\"" + escapeJson(status) + "\",");
                out.print("\"createdAt\":\"" + createdAt.toString().substring(0, 10) + "\"");
                out.print("}");
                
                first = false;
            }
            
            out.print("]");
            System.out.println("Total cases returned: " + count);
            
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            e.printStackTrace();
            out.print("[]");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
