import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,
    maxFileSize = 1024 * 1024 * 5,
    maxRequestSize = 1024 * 1024 * 10
)
public class UploadCaseServlet extends HttpServlet {

    private static final String UPLOAD_DIR = "case_documents";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !"client".equals(session.getAttribute("userType"))) {
            response.sendRedirect("login.html");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String caseTitle = request.getParameter("caseTitle");
        String caseType = request.getParameter("caseType");
        String city = request.getParameter("city");
        String urgency = request.getParameter("urgency");
        String budget = request.getParameter("budget");
        String description = request.getParameter("description");

        String documentPath = null;
        Part filePart = request.getPart("caseDocument");
        if (filePart != null && filePart.getSize() > 0) {
            documentPath = saveCaseDocument(filePart, request);
        }

        Connection conn = null;
        try {
            conn = DBConnectionUtil.getConnection();
            FeatureSchemaUtil.ensureInitialized(conn);
            conn.setAutoCommit(false);

            Integer clientId = getClientIdByUser(conn, userId);
            if (clientId == null) {
                conn.rollback();
                response.sendRedirect("client-upload-case.jsp?error=true");
                return;
            }

            int caseId = insertCase(conn, clientId, caseTitle, caseType, description, city, urgency, budget, documentPath);
            FeatureSupportUtil.addCaseTimeline(conn, caseId, userId, "pending", "Case submitted by client");
            notifyVerifiedLawyers(conn, caseId, caseTitle);

            conn.commit();
            response.sendRedirect("client-upload-case.jsp?success=true");
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            response.sendRedirect("client-upload-case.jsp?error=true");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Integer getClientIdByUser(Connection conn, int userId) throws SQLException {
        String sql = "SELECT client_id FROM clients WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("client_id");
                }
            }
        }
        return null;
    }

    private int insertCase(Connection conn, int clientId, String caseTitle, String caseType, String description,
                           String city, String urgency, String budget, String documentPath) throws SQLException {
        String sql = "INSERT INTO cases (client_id, case_title, case_type, case_description, city, urgency, budget, document_path) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, clientId);
            pstmt.setString(2, caseTitle);
            pstmt.setString(3, caseType);
            pstmt.setString(4, description);
            pstmt.setString(5, city);
            pstmt.setString(6, urgency);
            pstmt.setString(7, budget);
            pstmt.setString(8, documentPath);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create case");
    }

    private void notifyVerifiedLawyers(Connection conn, int caseId, String caseTitle) throws SQLException {
        String sql = "SELECT u.user_id " +
                     "FROM users u INNER JOIN lawyers l ON u.user_id = l.user_id " +
                     "WHERE u.user_type = 'lawyer' AND u.is_active = TRUE AND l.is_verified = TRUE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int lawyerUserId = rs.getInt("user_id");
                FeatureSupportUtil.createNotification(
                    conn,
                    lawyerUserId,
                    "New case request",
                    "A new case #" + caseId + " (" + (caseTitle == null ? "Untitled Case" : caseTitle) + ") is waiting for lawyers.",
                    "case_request",
                    caseId
                );
            }
        }
    }

    private String saveCaseDocument(Part filePart, HttpServletRequest request) {
        try {
            String fileName = getFileName(filePart);
            String cleanFileName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String newFileName = System.currentTimeMillis() + "_" + cleanFileName;

            String applicationPath = request.getServletContext().getRealPath("");
            String uploadPath = applicationPath + File.separator + UPLOAD_DIR;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String filePath = uploadPath + File.separator + newFileName;
            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            return UPLOAD_DIR + "/" + newFileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp == null) {
            return "unknown_file";
        }
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "unknown_file";
    }
}
