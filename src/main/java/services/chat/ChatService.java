package services.chat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.InputStream;
import java.util.Properties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import utils.JsonUtils;

public final class ChatService {
    private static final URI OPENROUTER_CHAT_URI = URI.create("https://openrouter.ai/api/v1/chat/completions");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http;
    private final OpenRouterConfigStore configStore;
    private final String model;
    private final String appUrl;
    private final String appTitle;

    public ChatService() {
        this(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(),
                new OpenRouterConfigStore(),
                envOrDefault("OPENROUTER_MODEL", "openai/gpt-4o-mini"),
                envOrEmpty("OPENROUTER_APP_URL"),
                envOrDefault("OPENROUTER_APP_TITLE", "Studly JavaFX")
        );
    }

    public ChatService(HttpClient http, OpenRouterConfigStore configStore, String model, String appUrl, String appTitle) {
        this.http = http;
        this.configStore = configStore == null ? new OpenRouterConfigStore() : configStore;
        this.model = model == null || model.isBlank() ? "openai/gpt-4o-mini" : model.trim();
        this.appUrl = appUrl == null ? "" : appUrl.trim();
        this.appTitle = appTitle == null || appTitle.isBlank() ? "Studly JavaFX" : appTitle.trim();

        // Persist non-secret defaults for convenience.
        this.configStore.saveDefaultsIfMissing(this.model, this.appUrl, this.appTitle);
    }

    public String chat(String systemPrompt, String userMessage) throws IOException, InterruptedException {
        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Missing OpenRouter API key. Set OPENROUTER_API_KEY or configure it in-app.");
        }
        String user = userMessage == null ? "" : userMessage.trim();
        if (user.isEmpty()) {
            throw new IllegalArgumentException("Empty user message.");
        }

        String sys = (systemPrompt == null || systemPrompt.isBlank())
                ? "You are a helpful assistant. Answer briefly and clearly."
                : systemPrompt.trim();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", sys),
                Map.of("role", "user", "content", user)
        ));

        String body = JsonUtils.stringify(payload);

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(OPENROUTER_CHAT_URI)
                .timeout(TIMEOUT)
                .setHeader("Authorization", "Bearer " + apiKey)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (!appUrl.isBlank()) {
            req.setHeader("HTTP-Referer", appUrl);
        }
        if (!appTitle.isBlank()) {
            req.setHeader("X-Title", appTitle);
        }

        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            if (resp.statusCode() == 401) {
                throw new IOException(
                        "OpenRouter HTTP 401 (Unauthorized). Your API key is missing/invalid/revoked. " +
                        "Open Studly AI ⚙ and re-save your key (paste only the key, without 'Bearer'). " +
                        "Response: " + safeSnippet(resp.body())
                );
            }
            throw new IOException("OpenRouter HTTP " + resp.statusCode() + ": " + safeSnippet(resp.body()));
        }

        Object root = JsonUtils.parse(resp.body());
        Map<String, Object> rootMap = JsonUtils.asObject(root);
        List<Object> choices = JsonUtils.asArray(rootMap.get("choices"));
        if (choices.isEmpty()) {
            throw new IOException("OpenRouter response missing choices[0].message.content");
        }
        Map<String, Object> choice = JsonUtils.asObject(choices.get(0));
        Map<String, Object> message = JsonUtils.asObject(choice.get("message"));
        String content = JsonUtils.asString(message.get("content"));
        if (content.isBlank()) {
            throw new IOException("OpenRouter response missing choices[0].message.content");
        }
        return content;
    }

    public boolean hasApiKeyConfigured() {
        return !resolveApiKey().isBlank();
    }

    public void saveApiKey(String apiKey) throws IOException {
        configStore.saveApiKey(apiKey);
    }

    public String getConfigFilePath() {
        return configStore.getConfigPath().toString();
    }

    private static String safeSnippet(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 400 ? t.substring(0, 400) + "…" : t;
    }

    private String resolveApiKey() {
        String sysProp = System.getProperty("openrouter.apiKey", "").trim();
        if (!sysProp.isBlank()) {
            return normalizeKey(sysProp);
        }
        String env = envOrEmpty("OPENROUTER_API_KEY").trim();
        if (!env.isBlank()) {
            return normalizeKey(env);
        }
        String file = configStore.getApiKey();
        if (!file.isBlank()) {
            return normalizeKey(file);
        }
        String classpath = classpathProperty("openrouter.apiKey");
        return normalizeKey(classpath);
    }

    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        String k = raw.trim();
        if (k.toLowerCase().startsWith("bearer ")) {
            k = k.substring("bearer ".length()).trim();
        }
        return k;
    }

    private static String classpathProperty(String key) {
        try (InputStream in = ChatService.class.getResourceAsStream("/openrouter.properties")) {
            if (in == null) return "";
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty(key);
            return v == null ? "" : v.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String envOrEmpty(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v;
    }

    private static String envOrDefault(String key, String def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        return v;
    }
}

