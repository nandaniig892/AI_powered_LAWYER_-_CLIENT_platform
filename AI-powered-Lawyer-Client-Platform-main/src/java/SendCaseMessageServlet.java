import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
    maxRequestSize = 1024 * 1024 * 8
)
public class SendCaseMessageServlet extends HttpServlet {

    private static final String CHAT_UPLOAD_DIR = "chat_files";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                out.print("{\"success\":false,\"message\":\"Not logged in\"}");
                return;
            }

            String caseIdParam = request.getParameter("caseId");
            if (caseIdParam == null || caseIdParam.trim().isEmpty()) {
                out.print("{\"success\":false,\"message\":\"caseId required\"}");
                return;
            }

            int userId = (Integer) session.getAttribute("userId");
            String userType = (String) session.getAttribute("userType");
            int caseId = Integer.parseInt(caseIdParam);
            String messageText = request.getParameter("message");
            if (messageText != null) {
                messageText = messageText.trim();
            }

            String filePath = null;
            Part filePart = request.getPart("messageFile");
            if (filePart != null && filePart.getSize() > 0) {
                String fileName = extractFileName(filePart);
                String cleanName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                String newFileName = System.currentTimeMillis() + "_" + cleanName;
                String basePath = getServletContext().getRealPath("");
                String folderPath = basePath + File.separator + CHAT_UPLOAD_DIR;

                File folder = new File(folderPath);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String absolutePath = folderPath + File.separator + newFileName;
                try (InputStream input = filePart.getInputStream();
                     FileOutputStream output = new FileOutputStream(absolutePath)) {
                    byte[] buffer = new byte[2048];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                filePath = CHAT_UPLOAD_DIR + "/" + newFileName;
            }

            if ((messageText == null || messageText.isEmpty()) && filePath == null) {
                out.print("{\"success\":false,\"message\":\"Message or file required\"}");
                return;
            }

            try (Connection conn = DBConnectionUtil.getConnection()) {
                FeatureSchemaUtil.ensureInitialized(conn);

                CaseAccessUtil.CaseParticipantInfo info = CaseAccessUtil.loadCaseParticipantInfo(conn, caseId);
                if (!CaseAccessUtil.canAccessCase(userType, userId, info)) {
                    out.print("{\"success\":false,\"message\":\"Access denied\"}");
                    return;
                }

                Integer recipientUserId = CaseAccessUtil.getCounterpartyUserId(userType, userId, info);
                if (recipientUserId == null) {
                    out.print("{\"success\":false,\"message\":\"Cannot message until counterpart is assigned\"}");
                    return;
                }

                String insertSql = "INSERT INTO case_messages (case_id, sender_user_id, message_text, file_path) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, caseId);
                    pstmt.setInt(2, userId);
                    pstmt.setString(3, messageText);
                    pstmt.setString(4, filePath);
                    pstmt.executeUpdate();
                }

                FeatureSupportUtil.createNotification(
                    conn,
                    recipientUserId,
                    "New case message",
                    "You received a new message on case #" + caseId + " (" + info.getCaseTitle() + ")",
                    "chat",
                    caseId
                );

                out.print("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"message\":\"Failed to send message\"}");
            }
        }
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp == null) {
            return "file";
        }
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.startsWith("filename")) {
                return trimmed.substring(trimmed.indexOf('=') + 1).replace("\"", "");
            }
        }
        return "file";
    }
}
