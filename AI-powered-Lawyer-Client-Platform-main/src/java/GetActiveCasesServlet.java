import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GetActiveCasesServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            out.print("[]");
            return;
        }
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            out.print("[]");
            return;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnectionUtil.getConnection();
            FeatureSchemaUtil.ensureInitialized(conn);
            
            // Get active cases assigned to this lawyer
            String sql = 
                "SELECT c.case_id, c.case_title, c.case_type, c.case_status, c.case_description, " +
                "c.city, c.urgency, c.budget, c.document_path, c.created_at, " +
                "u.first_name, u.last_name, u.email, u.phone_number " +
                "FROM cases c " +
                "INNER JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                "INNER JOIN clients cl ON c.client_id = cl.client_id " +
                "INNER JOIN users u ON cl.user_id = u.user_id " +
                "WHERE l.user_id = ? AND c.case_status <> 'pending' " +
                "ORDER BY c.created_at DESC";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            
            out.print("[");
            boolean first = true;
            
            while (rs.next()) {
                if (!first) out.print(",");
                
                out.print("{");
                out.print("\"caseId\":" + rs.getInt("case_id") + ",");
                out.print("\"title\":\"" + clean(rs.getString("case_title")) + "\",");
                out.print("\"type\":\"" + clean(rs.getString("case_type")) + "\",");
                out.print("\"status\":\"" + clean(rs.getString("case_status")) + "\",");
                out.print("\"description\":\"" + clean(rs.getString("case_description")) + "\",");
                out.print("\"city\":\"" + clean(rs.getString("city")) + "\",");
                out.print("\"urgency\":\"" + clean(rs.getString("urgency")) + "\",");
                out.print("\"budget\":\"" + clean(rs.getString("budget")) + "\",");
                
                String docPath = rs.getString("document_path");
                out.print("\"documentPath\":\"" + (docPath != null ? clean(docPath) : "") + "\",");
                
                out.print("\"clientFirstName\":\"" + clean(rs.getString("first_name")) + "\",");
                out.print("\"clientLastName\":\"" + clean(rs.getString("last_name")) + "\",");
                out.print("\"clientEmail\":\"" + clean(rs.getString("email")) + "\",");
                out.print("\"clientPhone\":\"" + clean(rs.getString("phone_number")) + "\",");
                
                String dateStr = rs.getTimestamp("created_at").toString();
                out.print("\"createdAt\":\"" + dateStr.substring(0, 10) + "\"");
                
                out.print("}");
                
                first = false;
            }
            
            out.print("]");
            
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {}
        }
    }
    
    private String clean(String str) {
        if (str == null) return "";
        return str.replace("\"", "").replace("\\", "").replace("\n", " ").replace("\r", " ");
    }
}
