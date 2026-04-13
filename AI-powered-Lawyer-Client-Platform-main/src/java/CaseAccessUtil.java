import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CaseAccessUtil {

    public static final class CaseParticipantInfo {
        private final int caseId;
        private final int clientUserId;
        private final Integer lawyerUserId;
        private final String caseTitle;

        public CaseParticipantInfo(int caseId, int clientUserId, Integer lawyerUserId, String caseTitle) {
            this.caseId = caseId;
            this.clientUserId = clientUserId;
            this.lawyerUserId = lawyerUserId;
            this.caseTitle = caseTitle;
        }

        public int getCaseId() {
            return caseId;
        }

        public int getClientUserId() {
            return clientUserId;
        }

        public Integer getLawyerUserId() {
            return lawyerUserId;
        }

        public String getCaseTitle() {
            return caseTitle;
        }
    }

    private CaseAccessUtil() {
    }

    public static CaseParticipantInfo loadCaseParticipantInfo(Connection conn, int caseId) throws SQLException {
        String sql = "SELECT c.case_id, c.case_title, cu.user_id AS client_user_id, lu.user_id AS lawyer_user_id " +
                     "FROM cases c " +
                     "INNER JOIN clients cl ON c.client_id = cl.client_id " +
                     "INNER JOIN users cu ON cl.user_id = cu.user_id " +
                     "LEFT JOIN lawyers l ON c.lawyer_id = l.lawyer_id " +
                     "LEFT JOIN users lu ON l.user_id = lu.user_id " +
                     "WHERE c.case_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, caseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int clientUserId = rs.getInt("client_user_id");
                int lawyerUserRaw = rs.getInt("lawyer_user_id");
                Integer lawyerUserId = rs.wasNull() ? null : lawyerUserRaw;
                return new CaseParticipantInfo(
                    rs.getInt("case_id"),
                    clientUserId,
                    lawyerUserId,
                    rs.getString("case_title")
                );
            }
        }
    }

    public static boolean canAccessCase(String userType, int userId, CaseParticipantInfo info) {
        if (info == null || userType == null) {
            return false;
        }
        if ("client".equals(userType)) {
            return userId == info.getClientUserId();
        }
        if ("lawyer".equals(userType)) {
            return info.getLawyerUserId() != null && userId == info.getLawyerUserId();
        }
        return "admin".equals(userType);
    }

    public static Integer getCounterpartyUserId(String userType, int userId, CaseParticipantInfo info) {
        if (!canAccessCase(userType, userId, info)) {
            return null;
        }
        if ("client".equals(userType)) {
            return info.getLawyerUserId();
        }
        if ("lawyer".equals(userType)) {
            return info.getClientUserId();
        }
        return null;
    }
}
