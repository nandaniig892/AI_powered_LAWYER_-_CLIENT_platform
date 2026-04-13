public final class AIConfig {

    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.0-flash";
    private static final String DEFAULT_GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models";

    private AIConfig() {
    }

    public static String getGeminiApiKey() {
        return readConfig("GEMINI_API_KEY", "");
    }

    public static String getGeminiModel() {
        return readConfig("GEMINI_MODEL", DEFAULT_GEMINI_MODEL);
    }

    public static String getGeminiEndpoint() {
        return readConfig("GEMINI_ENDPOINT", DEFAULT_GEMINI_ENDPOINT);
    }

    private static String readConfig(String key, String defaultValue) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty(key);
        if (fromProperty != null && !fromProperty.trim().isEmpty()) {
            return fromProperty.trim();
        }
        return defaultValue;
    }
}
