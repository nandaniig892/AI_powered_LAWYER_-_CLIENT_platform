import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AiSupportServlet extends HttpServlet {

    private static final int MAX_DOC_TEXT_CHARS = 4500;
    private static final int MAX_PROMPT_CHARS = 1500;
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final String DISCLAIMER =
        "This output is informational and grounded only in provided records. It is not final legal advice.";

    private static final Pattern YEARS_PATTERN = Pattern.compile("(\\d+)");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        String userType = safe((String) session.getAttribute("userType"));
        String role = safe(request.getParameter("role"));
        String userPrompt = safe(request.getParameter("prompt"));
        Integer caseId = parseInteger(request.getParameter("caseId"));
        int userId = (Integer) session.getAttribute("userId");

        if (role.isEmpty()) {
            role = userType;
        }
        if (!userType.equals(role)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized role");
            return;
        }
        if (caseId == null || caseId <= 0) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Valid caseId is required");
            return;
        }
        if (userPrompt.isEmpty()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Prompt is required");
            return;
        }
        if (userPrompt.length() > MAX_PROMPT_CHARS) {
            userPrompt = userPrompt.substring(0, MAX_PROMPT_CHARS);
        }

        try (Connection conn = DBConnectionUtil.getConnection()) {
            CaseContext caseContext = loadCaseContext(conn, caseId);
            if (caseContext == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Case not found");
                return;
            }

            CaseAccessUtil.CaseParticipantInfo participantInfo =
                new CaseAccessUtil.CaseParticipantInfo(
                    caseContext.caseId,
                    caseContext.clientUserId,
                    caseContext.lawyerUserId,
                    caseContext.caseTitle
                );
            if (!CaseAccessUtil.canAccessCase(userType, userId, participantInfo)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "You cannot access this case");
                return;
            }

            String legalKnowledge = buildLegalKnowledge(caseContext.caseType, userPrompt);
            String documentSnippet = loadDocumentSnippet(request, caseContext.documentPath);
            boolean enoughEvidence = hasEnoughEvidence(caseContext, documentSnippet);
            boolean strictEvidenceGate = requiresStrictEvidenceGate(userPrompt);

            List<Map<String, Object>> recommendations = "client".equals(role)
                ? buildLawyerRecommendations(conn, caseContext, userPrompt)
                : new ArrayList<Map<String, Object>>();

            Map<String, Object> analysis;
            String mode;

            if (!enoughEvidence && strictEvidenceGate) {
                analysis = buildInsufficientEvidenceAnalysis(caseContext, legalKnowledge, recommendations);
                mode = "grounded-insufficient";
            } else {
                String geminiKey = AIConfig.getGeminiApiKey();
                if (geminiKey.isEmpty()) {
                    analysis = buildLocalGroundedAnalysis(
                        caseContext,
                        userPrompt,
                        documentSnippet,
                        legalKnowledge,
                        recommendations,
                        strictEvidenceGate
                    );
                    mode = "fallback";
                } else {
                    try {
                        String modelPrompt = buildGeminiPrompt(
                            role,
                            userPrompt,
                            caseContext,
                            documentSnippet,
                            legalKnowledge,
                            strictEvidenceGate,
                            enoughEvidence
                        );
                        String rawModelOutput = callGemini(modelPrompt, geminiKey);
                        Map<String, Object> parsed = parseJsonObject(extractFirstJsonObject(rawModelOutput));
                        analysis = normalizeModelAnalysis(
                            parsed,
                            caseContext,
                            userPrompt,
                            legalKnowledge,
                            recommendations,
                            strictEvidenceGate
                        );
                        mode = "live";
                    } catch (Exception ex) {
                        analysis = buildLocalGroundedAnalysis(
                            caseContext,
                            userPrompt,
                            documentSnippet,
                            legalKnowledge,
                            recommendations,
                            strictEvidenceGate
                        );
                        mode = "fallback";
                    }
                }
            }

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("ok", true);
            payload.put("mode", mode);
            payload.put("caseId", caseContext.caseId);
            payload.put("role", role);
            payload.put("analysis", analysis);
            response.getWriter().print(toJson(payload));
        } catch (SQLException ex) {
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + ex.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    private CaseContext loadCaseContext(Connection conn, int caseId) throws SQLException {
        String sql = "SELECT c.case_id, c.case_title, c.case_type, c.case_description, c.case_status, c.city, c.urgency, c.budget, c.document_path, " +
                     "cu.user_id AS client_user_id, lu.user_id AS lawyer_user_id " +
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
                CaseContext ctx = new CaseContext();
                ctx.caseId = rs.getInt("case_id");
                ctx.caseTitle = safe(rs.getString("case_title"));
                ctx.caseType = safe(rs.getString("case_type"));
                ctx.caseDescription = safe(rs.getString("case_description"));
                ctx.caseStatus = safe(rs.getString("case_status"));
                ctx.city = safe(rs.getString("city"));
                ctx.urgency = safe(rs.getString("urgency"));
                ctx.budget = safe(rs.getString("budget"));
                ctx.documentPath = safe(rs.getString("document_path"));
                ctx.clientUserId = rs.getInt("client_user_id");
                int lawyerUserRaw = rs.getInt("lawyer_user_id");
                ctx.lawyerUserId = rs.wasNull() ? null : lawyerUserRaw;
                return ctx;
            }
        }
    }

    private String buildLegalKnowledge(String caseType, String prompt) {
        String lower = (caseType + " " + prompt).toLowerCase(Locale.ENGLISH);
        List<String> rules = new ArrayList<String>();
        rules.add("Maintain chronology of events with exact dates and supporting records.");
        rules.add("Verify jurisdiction and limitation periods before filing.");
        rules.add("Preserve original evidence and maintain chain of custody for digital files.");

        if (containsAny(lower, "criminal", "bail", "arrest", "fir")) {
            rules.add("Criminal matters typically require FIR/complaint copy, witness details, and procedural timeline.");
            rules.add("Assess immediate liberty-related risks and urgency before strategy.");
        } else if (containsAny(lower, "property", "land", "title", "tenant", "rent")) {
            rules.add("Property disputes typically rely on title documents, mutation/registry, and possession evidence.");
            rules.add("Map prior notices, encumbrances, and ownership history in sequence.");
        } else if (containsAny(lower, "family", "divorce", "custody", "maintenance")) {
            rules.add("Family matters generally require relationship records, communication history, and dependency evidence.");
            rules.add("Child welfare and interim relief factors should be documented early.");
        } else if (containsAny(lower, "contract", "agreement", "breach", "commercial")) {
            rules.add("Contract disputes need executed agreements, performance proof, notice trail, and damages basis.");
            rules.add("Identify specific clauses tied to obligations, breach, cure period, and remedies.");
        } else if (containsAny(lower, "labour", "employment", "salary", "termination")) {
            rules.add("Employment disputes need appointment terms, payroll records, notices, and policy references.");
            rules.add("Track chronology of HR communication and adverse actions.");
        } else {
            rules.add("Ground all analysis in available records and avoid conclusions not supported by evidence.");
        }
        return joinWithNewline(rules);
    }

    private String loadDocumentSnippet(HttpServletRequest request, String documentPath) {
        if (documentPath.isEmpty()) {
            return "";
        }
        try {
            String normalized = documentPath.replace("\\", "/");
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            String realPath = request.getServletContext().getRealPath("/" + normalized);
            if (realPath == null || realPath.trim().isEmpty()) {
                return "";
            }
            File file = new File(realPath);
            if (!file.exists() || !file.isFile()) {
                return "";
            }

            String name = file.getName().toLowerCase(Locale.ENGLISH);
            if (!isPlainTextFile(name)) {
                return "[Document attached: " + file.getName() + " (binary or unsupported for text extraction)]";
            }

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                int c;
                while ((c = reader.read()) != -1 && out.length() < MAX_DOC_TEXT_CHARS) {
                    out.append((char) c);
                }
            }
            return out.toString().trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private boolean isPlainTextFile(String fileName) {
        return fileName.endsWith(".txt")
            || fileName.endsWith(".md")
            || fileName.endsWith(".csv")
            || fileName.endsWith(".json")
            || fileName.endsWith(".xml")
            || fileName.endsWith(".log");
    }

    private boolean hasEnoughEvidence(CaseContext ctx, String documentSnippet) {
        int score = 0;
        if (!ctx.caseDescription.isEmpty() && ctx.caseDescription.length() >= 60) {
            score += 2;
        }
        if (!ctx.caseType.isEmpty()) {
            score += 1;
        }
        if (!ctx.city.isEmpty()) {
            score += 1;
        }
        if (!documentSnippet.isEmpty() && !documentSnippet.startsWith("[Document attached:")) {
            score += 2;
        }
        return score >= 3;
    }

    private boolean requiresStrictEvidenceGate(String prompt) {
        String p = safe(prompt).toLowerCase(Locale.ENGLISH);
        if (p.isEmpty()) {
            return false;
        }
        if (p.length() <= 20 && (containsAny(p, "hi", "hello", "hey", "thanks", "thank you"))) {
            return false;
        }
        return containsAny(
            p,
            "case strength",
            "strength analysis",
            "winning chance",
            "who will win",
            "outcome prediction",
            "liable",
            "applicable rules",
            "which law applies",
            "legal sections",
            "recommend lawyer",
            "best lawyer"
        );
    }

    private List<Map<String, Object>> buildLawyerRecommendations(Connection conn, CaseContext ctx, String prompt)
            throws SQLException {
        String sql = "SELECT u.user_id, u.first_name, u.last_name, u.city, " +
                     "l.primary_specialization, l.years_experience, l.city_practice, l.hourly_rate, l.bio, " +
                     "COALESCE(AVG(lr.rating), 0) AS avg_rating, COUNT(lr.review_id) AS review_count " +
                     "FROM users u " +
                     "INNER JOIN lawyers l ON u.user_id = l.user_id " +
                     "LEFT JOIN lawyer_reviews lr ON lr.lawyer_user_id = u.user_id " +
                     "WHERE u.user_type = 'lawyer' AND u.is_active = TRUE AND l.is_verified = TRUE " +
                     "GROUP BY u.user_id, u.first_name, u.last_name, u.city, l.primary_specialization, l.years_experience, l.city_practice, l.hourly_rate, l.bio " +
                     "ORDER BY avg_rating DESC, review_count DESC";

        String caseSignal = safe(ctx.caseType) + " " + safe(ctx.caseDescription) + " " + safe(prompt);
        List<LawyerScore> scores = new ArrayList<LawyerScore>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String lawyerCity = safe(rs.getString("city"));
                String cityPractice = safe(rs.getString("city_practice"));
                String specialization = safe(rs.getString("primary_specialization"));
                String bio = safe(rs.getString("bio"));
                double avgRating = rs.getDouble("avg_rating");
                int reviewCount = rs.getInt("review_count");
                int years = parseYears(rs.getString("years_experience"));

                String profileSignal = specialization + " " + bio + " " + cityPractice + " " + lawyerCity;
                double embeddingSimilarity = cosineSimilarity(caseSignal, profileSignal);
                double specializationScore = tokenOverlap(caseSignal, specialization + " " + bio);
                double cityScore = cityMatchScore(ctx.city, lawyerCity, cityPractice);
                double experienceScore = Math.min(years / 15.0, 1.0);
                double ratingScore = Math.min(avgRating / 5.0, 1.0);

                double finalScore =
                    (0.35 * embeddingSimilarity) +
                    (0.25 * specializationScore) +
                    (0.15 * cityScore) +
                    (0.15 * ratingScore) +
                    (0.10 * experienceScore);

                LawyerScore score = new LawyerScore();
                score.userId = rs.getInt("user_id");
                score.name = safe(rs.getString("first_name")) + " " + safe(rs.getString("last_name"));
                score.city = lawyerCity;
                score.specialization = specialization;
                score.experience = safe(rs.getString("years_experience"));
                score.hourlyRate = safe(rs.getString("hourly_rate"));
                score.avgRating = avgRating;
                score.reviewCount = reviewCount;
                score.embeddingSimilarity = round3(embeddingSimilarity);
                score.finalScore = round3(finalScore);
                score.reason = buildRecommendationReason(cityScore, specializationScore, ratingScore, experienceScore);
                scores.add(score);
            }
        }

        Collections.sort(scores, new Comparator<LawyerScore>() {
            @Override
            public int compare(LawyerScore a, LawyerScore b) {
                return Double.compare(b.finalScore, a.finalScore);
            }
        });

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < scores.size() && i < MAX_RECOMMENDATIONS; i++) {
            LawyerScore s = scores.get(i);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("userId", s.userId);
            row.put("name", s.name.trim());
            row.put("city", s.city);
            row.put("specialization", s.specialization);
            row.put("experience", s.experience);
            row.put("hourlyRate", s.hourlyRate);
            row.put("avgRating", round3(s.avgRating));
            row.put("reviewCount", s.reviewCount);
            row.put("embeddingSimilarity", s.embeddingSimilarity);
            row.put("recommendationScore", s.finalScore);
            row.put("reason", s.reason);
            result.add(row);
        }
        return result;
    }

    private String buildRecommendationReason(double cityScore, double specializationScore, double ratingScore, double experienceScore) {
        List<String> parts = new ArrayList<String>();
        if (cityScore >= 0.9) {
            parts.add("strong city match");
        }
        if (specializationScore >= 0.35) {
            parts.add("specialization matches case context");
        }
        if (ratingScore >= 0.75) {
            parts.add("high client ratings");
        }
        if (experienceScore >= 0.6) {
            parts.add("solid experience level");
        }
        if (parts.isEmpty()) {
            parts.add("best available overall profile fit");
        }
        return joinWithComma(parts);
    }

    private String buildGeminiPrompt(
            String role,
            String userPrompt,
            CaseContext ctx,
            String documentSnippet,
            String legalKnowledge,
            boolean strictEvidenceGate,
            boolean enoughEvidence) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a legal assistant for a case platform.\n");
        prompt.append("Critical constraints:\n");
        prompt.append("1) Use only the provided case context, document snippet, and legal knowledge notes.\n");
        if (strictEvidenceGate) {
            prompt.append("2) If evidence is missing, set insufficient_evidence=true and summary exactly to \"I don't know from provided documents.\".\n");
        } else {
            prompt.append("2) If evidence is limited, you may still give procedural next steps and checklists without inventing facts.\n");
        }
        prompt.append("3) Do not provide final legal advice; keep informational tone.\n");
        prompt.append("4) Return JSON only. No markdown, no prose.\n\n");

        prompt.append("Required JSON schema:\n");
        prompt.append("{\n");
        prompt.append("  \"assistant_reply\": \"string\",\n");
        prompt.append("  \"summary\": \"string\",\n");
        prompt.append("  \"applicable_rules\": [\"string\"],\n");
        prompt.append("  \"case_strength\": {\"level\":\"low|medium|high\",\"reasoning\":[\"string\"]},\n");
        prompt.append("  \"proof_required\": [\"string\"],\n");
        prompt.append("  \"confidence\": 0,\n");
        prompt.append("  \"insufficient_evidence\": false,\n");
        prompt.append("  \"insufficient_evidence_reason\": \"string\"\n");
        prompt.append("}\n\n");

        prompt.append("Role: ").append(role).append("\n");
        prompt.append("User question: ").append(userPrompt).append("\n\n");
        prompt.append("Strict evidence gate required: ").append(strictEvidenceGate).append("\n");
        prompt.append("Evidence appears sufficient: ").append(enoughEvidence).append("\n\n");
        prompt.append("assistant_reply should be natural conversational text directly answering the user question.\n");
        prompt.append("Even for short/unclear/garbage prompts, respond briefly and helpfully without inventing case facts.\n\n");

        prompt.append("Case context:\n");
        prompt.append("- case_id: ").append(ctx.caseId).append("\n");
        prompt.append("- title: ").append(ctx.caseTitle).append("\n");
        prompt.append("- type: ").append(ctx.caseType).append("\n");
        prompt.append("- status: ").append(ctx.caseStatus).append("\n");
        prompt.append("- city: ").append(ctx.city).append("\n");
        prompt.append("- urgency: ").append(ctx.urgency).append("\n");
        prompt.append("- budget: ").append(ctx.budget).append("\n");
        prompt.append("- description: ").append(ctx.caseDescription).append("\n");
        prompt.append("- document_path: ").append(ctx.documentPath).append("\n\n");

        prompt.append("Document snippet (may be empty):\n");
        prompt.append(documentSnippet.isEmpty() ? "[No extractable text found]" : documentSnippet).append("\n\n");

        prompt.append("Legal knowledge notes:\n");
        prompt.append(legalKnowledge).append("\n");
        return prompt.toString();
    }

    private String callGemini(String prompt, String apiKey) throws IOException {
        String endpoint = safe(AIConfig.getGeminiEndpoint());
        String model = safe(AIConfig.getGeminiModel());
        if (endpoint.isEmpty() || model.isEmpty()) {
            throw new IOException("Gemini endpoint/model not configured.");
        }

        String url = endpoint + "/" + URLEncoder.encode(model, "UTF-8") + ":generateContent?key=" + URLEncoder.encode(apiKey, "UTF-8");
        String payload = "{"
            + "\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}],"
            + "\"generationConfig\":{\"temperature\":0.1,\"responseMimeType\":\"application/json\"}"
            + "}";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(45000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        String body;
        try (InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream()) {
            body = readStream(stream);
        }

        if (status < 200 || status >= 300) {
            throw new IOException("Gemini request failed (" + status + "): " + truncate(body, 220));
        }
        return extractGeminiText(body);
    }

    private String extractGeminiText(String body) throws IOException {
        try {
            Map<String, Object> json = parseJsonObject(body);
            Object candidatesObj = json.get("candidates");
            if (!(candidatesObj instanceof List)) {
                throw new IOException("Gemini response missing candidates");
            }
            List<?> candidates = (List<?>) candidatesObj;
            if (candidates.isEmpty() || !(candidates.get(0) instanceof Map)) {
                throw new IOException("Gemini response has empty candidates");
            }
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Object contentObj = candidate.get("content");
            if (!(contentObj instanceof Map)) {
                throw new IOException("Gemini response missing content");
            }
            Map<?, ?> content = (Map<?, ?>) contentObj;
            Object partsObj = content.get("parts");
            if (!(partsObj instanceof List)) {
                throw new IOException("Gemini response missing parts");
            }
            List<?> parts = (List<?>) partsObj;
            if (parts.isEmpty() || !(parts.get(0) instanceof Map)) {
                throw new IOException("Gemini response parts empty");
            }
            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            String text = safe(asString(part.get("text")));
            if (text.isEmpty()) {
                throw new IOException("Gemini returned empty text");
            }
            return text;
        } catch (ScriptException ex) {
            throw new IOException("Failed to parse Gemini response", ex);
        }
    }

    private Map<String, Object> normalizeModelAnalysis(
            Map<String, Object> modelJson,
            CaseContext ctx,
            String userPrompt,
            String legalKnowledge,
            List<Map<String, Object>> recommendations,
            boolean strictEvidenceGate) {
        Map<String, Object> analysis = new LinkedHashMap<String, Object>();

        String summary = safe(asString(modelJson.get("summary")));
        String assistantReply = safe(asString(modelJson.get("assistant_reply")));
        List<String> rules = asStringList(modelJson.get("applicable_rules"));
        List<String> proof = asStringList(modelJson.get("proof_required"));
        int confidence = clampInt(asInteger(modelJson.get("confidence"), 40), 0, 100);
        boolean insufficient = asBoolean(modelJson.get("insufficient_evidence"));
        String insufficientReason = safe(asString(modelJson.get("insufficient_evidence_reason")));

        Map<String, Object> caseStrength = asMap(modelJson.get("case_strength"));
        String strengthLevel = safe(asString(caseStrength.get("level")));
        if (strengthLevel.isEmpty()) {
            strengthLevel = "medium";
        }
        List<String> strengthReasons = asStringList(caseStrength.get("reasoning"));

        if ((strictEvidenceGate && insufficient) || summary.isEmpty()) {
            summary = "I don't know from provided documents.";
            assistantReply = "I don't know from provided documents.";
            if (insufficientReason.isEmpty()) {
                insufficientReason = "Evidence in provided case details/documents is not enough for reliable analysis.";
            }
            confidence = Math.min(confidence, 35);
            strengthLevel = "low";
            if (strengthReasons.isEmpty()) {
                strengthReasons.add("Insufficient documentary support and factual detail for stronger assessment.");
            }
        } else if (insufficient) {
            if (insufficientReason.isEmpty()) {
                insufficientReason = "Evidence is limited, so this response is guidance-oriented and not a final case judgment.";
            }
            confidence = Math.min(confidence, 50);
        }

        if (assistantReply.isEmpty()) {
            assistantReply = summary.isEmpty()
                ? "I understood your message, but I need a bit more detail to help clearly."
                : summary;
        }

        if (rules.isEmpty()) {
            rules = splitKnowledgeLines(legalKnowledge);
        }
        if (proof.isEmpty()) {
            proof = defaultProofList(ctx.caseType, userPrompt);
        }

        Map<String, Object> strength = new LinkedHashMap<String, Object>();
        strength.put("level", strengthLevel);
        strength.put("reasoning", strengthReasons);

        analysis.put("summary", summary);
        analysis.put("assistant_reply", assistantReply);
        analysis.put("applicable_rules", rules);
        analysis.put("case_strength", strength);
        analysis.put("proof_required", proof);
        analysis.put("confidence", confidence);
        analysis.put("insufficient_evidence", insufficient || "I don't know from provided documents.".equals(summary));
        analysis.put("insufficient_evidence_reason", insufficientReason);
        analysis.put("lawyer_recommendations", recommendations);
        analysis.put("disclaimer", DISCLAIMER);
        return analysis;
    }

    private Map<String, Object> buildLocalGroundedAnalysis(
            CaseContext ctx,
            String userPrompt,
            String documentSnippet,
            String legalKnowledge,
            List<Map<String, Object>> recommendations,
            boolean strictEvidenceGate) {
        Map<String, Object> analysis = new LinkedHashMap<String, Object>();
        Map<String, Object> strength = new LinkedHashMap<String, Object>();

        int evidencePoints = 0;
        if (ctx.caseDescription.length() >= 120) evidencePoints += 2;
        else if (ctx.caseDescription.length() >= 50) evidencePoints += 1;
        if (!documentSnippet.isEmpty() && !documentSnippet.startsWith("[Document attached:")) evidencePoints += 2;
        if (!ctx.caseType.isEmpty()) evidencePoints += 1;
        if (!ctx.urgency.isEmpty()) evidencePoints += 1;

        String level;
        if (evidencePoints <= 2) {
            level = "low";
        } else if (evidencePoints <= 4) {
            level = "medium";
        } else {
            level = "high";
        }

        List<String> strengthReasoning = new ArrayList<String>();
        strengthReasoning.add("Assessment uses submitted case fields and available document text only.");
        if (!ctx.caseDescription.isEmpty()) {
            strengthReasoning.add("Case description provides factual baseline for issue framing.");
        }
        if (!documentSnippet.isEmpty() && !documentSnippet.startsWith("[Document attached:")) {
            strengthReasoning.add("Extracted document snippet supports evidence-grounded analysis.");
        } else {
            strengthReasoning.add("Limited document text extraction reduces certainty.");
        }

        String summary = "Grounded response from selected case context and available records.";
        String assistantReply = "I understood: \"" + truncate(userPrompt, 180) + "\". " +
            "Based on current records, I can give guidance and next steps. " +
            "For higher confidence, add key facts or upload readable supporting documents.";
        int confidence = clampInt(30 + (evidencePoints * 12), 20, 85);
        boolean insufficient = evidencePoints < 3;
        String insufficientReason = insufficient
            ? "Available case data is not detailed enough for high-confidence conclusions."
            : "";
        if (insufficient && strictEvidenceGate) {
            summary = "I don't know from provided documents.";
            assistantReply = "I don't know from provided documents.";
            confidence = Math.min(confidence, 35);
            level = "low";
        } else if (insufficient) {
            confidence = Math.min(confidence, 45);
            insufficientReason = "Share clearer facts/timeline in the prompt or attach readable case documents to improve case-specific output.";
        }

        strength.put("level", level);
        strength.put("reasoning", strengthReasoning);

        analysis.put("summary", summary);
        analysis.put("assistant_reply", assistantReply);
        analysis.put("applicable_rules", splitKnowledgeLines(legalKnowledge));
        analysis.put("case_strength", strength);
        analysis.put("proof_required", defaultProofList(ctx.caseType, userPrompt));
        analysis.put("confidence", confidence);
        analysis.put("insufficient_evidence", insufficient);
        analysis.put("insufficient_evidence_reason", insufficientReason);
        analysis.put("lawyer_recommendations", recommendations);
        analysis.put("disclaimer", DISCLAIMER);
        return analysis;
    }

    private Map<String, Object> buildInsufficientEvidenceAnalysis(
            CaseContext ctx,
            String legalKnowledge,
            List<Map<String, Object>> recommendations) {
        Map<String, Object> analysis = new LinkedHashMap<String, Object>();
        Map<String, Object> strength = new LinkedHashMap<String, Object>();
        List<String> reasons = new ArrayList<String>();
        reasons.add("Insufficient factual detail in case description or extractable document text.");
        reasons.add("Cannot generate reliable legal interpretation without stronger evidence.");
        strength.put("level", "low");
        strength.put("reasoning", reasons);

        analysis.put("summary", "I don't know from provided documents.");
        analysis.put("assistant_reply", "I don't know from provided documents.");
        analysis.put("applicable_rules", splitKnowledgeLines(legalKnowledge));
        analysis.put("case_strength", strength);
        analysis.put("proof_required", defaultProofList(ctx.caseType, ""));
        analysis.put("confidence", 20);
        analysis.put("insufficient_evidence", true);
        analysis.put("insufficient_evidence_reason", "For case-specific judgment, share clearer facts/timeline in your prompt. Documents are optional but improve confidence.");
        analysis.put("lawyer_recommendations", recommendations);
        analysis.put("disclaimer", DISCLAIMER);
        return analysis;
    }

    private List<String> defaultProofList(String caseType, String userPrompt) {
        String lower = (safe(caseType) + " " + safe(userPrompt)).toLowerCase(Locale.ENGLISH);
        List<String> proof = new ArrayList<String>();
        proof.add("Chronological event timeline with specific dates.");
        proof.add("Identity and address proofs of concerned parties.");
        proof.add("All notices, emails, chats, and call records relevant to the dispute.");
        proof.add("Any signed agreements, acknowledgments, or receipts.");

        if (containsAny(lower, "rental", "rent", "lease", "tenant", "landlord")) {
            proof.add("Signed rent/lease agreement and all addendums.");
            proof.add("Rent receipts, bank transfer records, and deposit payment proof.");
            proof.add("Property handover/inventory condition record and photos.");
            proof.add("Notice emails/messages about rent default, eviction, or termination.");
        } else if (containsAny(lower, "property", "land", "title")) {
            proof.add("Ownership/title records, registry extracts, mutation/encumbrance records.");
            proof.add("Possession evidence such as tax bills, utility records, or possession memo.");
        } else if (containsAny(lower, "criminal", "bail", "fir", "arrest")) {
            proof.add("FIR/complaint copy, arrest memo, and charge-related paperwork.");
            proof.add("Independent witness statements and location/time corroboration records.");
        } else if (containsAny(lower, "family", "divorce", "custody", "maintenance")) {
            proof.add("Relationship/marriage records and dependency-related financial documents.");
            proof.add("Any communication proving conduct, support, or custody-relevant circumstances.");
        } else if (containsAny(lower, "contract", "agreement", "commercial", "breach")) {
            proof.add("Executed contract versions, annexures, and amendment trail.");
            proof.add("Performance, delivery, payment, and breach notice evidence.");
        }
        return proof;
    }

    private String extractFirstJsonObject(String text) {
        if (text == null) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int depth = 0;
        for (int i = start; i >= 0 && i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return trimmed.substring(start, i + 1);
                }
            }
        }
        return "{}";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) throws ScriptException, IOException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        if (engine == null) {
            throw new IOException("JavaScript engine unavailable for JSON parsing.");
        }
        engine.put("jsonString", json);
        Object parsed = engine.eval("Java.asJSONCompatible(JSON.parse(jsonString))");
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        return new LinkedHashMap<String, Object>();
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<String, Object>();
    }

    private List<String> asStringList(Object value) {
        List<String> list = new ArrayList<String>();
        if (value instanceof List) {
            List<?> raw = (List<?>) value;
            for (Object o : raw) {
                String item = safe(asString(o));
                if (!item.isEmpty()) {
                    list.add(item);
                }
            }
        } else {
            String asText = safe(asString(value));
            if (!asText.isEmpty()) {
                list.add(asText);
            }
        }
        return list;
    }

    private Integer asInteger(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String txt = safe(asString(value));
        if (txt.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(txt);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        String txt = safe(asString(value)).toLowerCase(Locale.ENGLISH);
        return "true".equals(txt) || "yes".equals(txt) || "1".equals(txt);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double cityMatchScore(String caseCity, String lawyerCity, String cityPractice) {
        String c = normalizeText(caseCity);
        String l = normalizeText(lawyerCity);
        String p = normalizeText(cityPractice);
        if (c.isEmpty()) return 0;
        if (!l.isEmpty() && (l.contains(c) || c.contains(l))) return 1.0;
        if (!p.isEmpty() && (p.contains(c) || c.contains(p))) return 0.9;
        return 0.1;
    }

    private double tokenOverlap(String a, String b) {
        Map<String, Integer> fa = tokenFreq(a);
        Map<String, Integer> fb = tokenFreq(b);
        if (fa.isEmpty() || fb.isEmpty()) return 0;
        int common = 0;
        for (String k : fa.keySet()) {
            if (fb.containsKey(k)) common++;
        }
        int denom = Math.max(fa.size(), fb.size());
        return denom == 0 ? 0 : (double) common / denom;
    }

    private double cosineSimilarity(String a, String b) {
        Map<String, Integer> fa = tokenFreq(a);
        Map<String, Integer> fb = tokenFreq(b);
        if (fa.isEmpty() || fb.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<String, Integer> e : fa.entrySet()) {
            int v = e.getValue().intValue();
            normA += v * v;
            Integer vb = fb.get(e.getKey());
            if (vb != null) {
                dot += v * vb.intValue();
            }
        }
        for (Integer v : fb.values()) {
            int iv = v.intValue();
            normB += iv * iv;
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Map<String, Integer> tokenFreq(String text) {
        String normalized = normalizeText(text);
        Map<String, Integer> freq = new HashMap<String, Integer>();
        if (normalized.isEmpty()) {
            return freq;
        }
        String[] tokens = normalized.split("\\s+");
        for (String t : tokens) {
            if (t.length() < 3 || isStopWord(t)) {
                continue;
            }
            Integer count = freq.get(t);
            freq.put(t, count == null ? 1 : count + 1);
        }
        return freq;
    }

    private boolean isStopWord(String token) {
        return "the".equals(token) || "and".equals(token) || "for".equals(token) || "with".equals(token)
            || "that".equals(token) || "this".equals(token) || "from".equals(token) || "into".equals(token)
            || "case".equals(token) || "client".equals(token) || "lawyer".equals(token) || "have".equals(token)
            || "your".equals(token) || "about".equals(token) || "were".equals(token) || "been".equals(token);
    }

    private String normalizeText(String text) {
        return safe(text).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9\\s]", " ").trim();
    }

    private int parseYears(String yearsText) {
        String txt = safe(yearsText);
        Matcher m = YEARS_PATTERN.matcher(txt);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        return 0;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitKnowledgeLines(String legalKnowledge) {
        List<String> out = new ArrayList<String>();
        String[] lines = safe(legalKnowledge).split("\\r?\\n");
        for (String line : lines) {
            String clean = safe(line).replaceFirst("^-\\s*", "");
            if (!clean.isEmpty()) {
                out.add(clean);
            }
        }
        return out;
    }

    private String joinWithNewline(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append("- ").append(lines.get(i));
        }
        return sb.toString();
    }

    private String joinWithComma(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private Integer parseInteger(String value) {
        String txt = safe(value);
        if (txt.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(txt);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ok", false);
        payload.put("message", message);
        response.getWriter().print(toJson(payload));
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static final class CaseContext {
        int caseId;
        int clientUserId;
        Integer lawyerUserId;
        String caseTitle;
        String caseType;
        String caseDescription;
        String caseStatus;
        String city;
        String urgency;
        String budget;
        String documentPath;
    }

    private static final class LawyerScore {
        int userId;
        String name;
        String city;
        String specialization;
        String experience;
        String hourlyRate;
        double avgRating;
        int reviewCount;
        double embeddingSimilarity;
        double finalScore;
        String reason;
    }
}
