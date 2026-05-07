package utils;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class GitHubOAuthService {

    private static final String AUTH_URL  = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL  = "https://api.github.com/user";
    private static final String EMAIL_URL = "https://api.github.com/user/emails";
    private static final int    CB_PORT   = 8888;

    // ── Profile DTO ───────────────────────────────────────────────────────────

    public static final class UserProfile {
        public final String githubId;
        public final String email;
        public final String firstName;
        public final String lastName;
        public final String avatarUrl;
        public final String accessToken;

        UserProfile(String githubId, String email, String firstName, String lastName,
                    String avatarUrl, String accessToken) {
            this.githubId    = githubId;
            this.email       = email;
            this.firstName   = firstName;
            this.lastName    = lastName;
            this.avatarUrl   = avatarUrl;
            this.accessToken = accessToken;
        }
    }

    // ── Construction ──────────────────────────────────────────────────────────

    private final String clientId;
    private final String clientSecret;

    public GitHubOAuthService() {
        Properties config = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/github-oauth.properties")) {
            if (in != null) config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientId     = config.getProperty("github.client.id",     "").trim();
        clientSecret = config.getProperty("github.client.secret", "").trim();
    }

    public boolean isConfigured() {
        return !clientId.isEmpty()     && !clientId.startsWith("YOUR_")
            && !clientSecret.isEmpty() && !clientSecret.startsWith("YOUR_");
    }

    // ── Full OAuth flow ───────────────────────────────────────────────────────

    /**
     * Opens the system browser, waits for the GitHub callback on port 8888,
     * exchanges the code for a token and returns the user's profile.
     * Must be called off the JavaFX Application Thread.
     */
    public UserProfile authenticate() throws Exception {
        String redirectUri = "http://localhost:" + CB_PORT + "/oauth2/callback";

        String authUrl = AUTH_URL
            + "?client_id="    + enc(clientId)
            + "&redirect_uri=" + enc(redirectUri)
            + "&scope="        + enc("user:email read:user");

        openBrowser(authUrl);

        String code = waitForAuthCode();
        if (code == null) throw new Exception("GitHub login cancelled or timed out.");

        String accessToken = exchangeCode(code, redirectUri);

        // Fetch profile — /user may return null email when the user set it to private
        Map<String, String> userInfo = getApiJson(USER_URL, accessToken);
        String email = userInfo.get("email");
        if (email == null || email.isBlank() || "null".equals(email)) {
            email = fetchPrimaryEmail(accessToken);
        }
        if (email == null || email.isBlank()) {
            throw new Exception(
                "Could not retrieve email from GitHub.\n"
                + "Make sure your GitHub email is set to public, or that you granted 'user:email' scope.");
        }

        String githubId = userInfo.getOrDefault("id", "");
        // Use display name, fall back to login handle
        String login    = userInfo.getOrDefault("login", "");
        String fullName = userInfo.getOrDefault("name", "");
        if (fullName.isBlank()) fullName = login;
        String[] nameParts = splitName(fullName);

        return new UserProfile(
            githubId,
            email,
            nameParts[0],
            nameParts[1],
            userInfo.getOrDefault("avatar_url", null),
            accessToken
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void openBrowser(String url) throws Exception {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            // Windows fallback
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        }
    }

    private String waitForAuthCode() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(CB_PORT)) {
            serverSocket.setSoTimeout(180_000); // 3-minute timeout
            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String requestLine = reader.readLine(); // "GET /oauth2/callback?code=xxx HTTP/1.1"

                String code  = null;
                String error = null;
                if (requestLine != null) {
                    String[] parts = requestLine.split(" ");
                    if (parts.length >= 2) {
                        String path = parts[1];
                        int q = path.indexOf('?');
                        if (q >= 0) {
                            String query = path.substring(q + 1);
                            code  = parseParam(query, "code");
                            error = parseParam(query, "error");
                        }
                    }
                }

                // Respond to the browser tab
                boolean success = code != null && error == null;
                String html = success
                    ? "<!DOCTYPE html><html><head>"
                        + "<style>body{font-family:sans-serif;text-align:center;padding-top:60px;"
                        + "background:#f0fff4;}</style></head><body>"
                        + "<h2 style='color:#24292e'>&#10003; GitHub Authentication Successful</h2>"
                        + "<p style='color:#555'>You can close this tab and return to <strong>Studly</strong>.</p>"
                        + "</body></html>"
                    : "<!DOCTYPE html><html><head>"
                        + "<style>body{font-family:sans-serif;text-align:center;padding-top:60px;"
                        + "background:#fff0f0;}</style></head><body>"
                        + "<h2 style='color:#d73a49'>&#10007; Authentication Cancelled</h2>"
                        + "<p style='color:#555'>You denied access. You can close this tab and return to <strong>Studly</strong>.</p>"
                        + "</body></html>";

                byte[] body = html.getBytes(StandardCharsets.UTF_8);
                String header = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html; charset=utf-8\r\n"
                    + "Content-Length: " + body.length + "\r\n"
                    + "Connection: close\r\n\r\n";
                OutputStream out = socket.getOutputStream();
                out.write(header.getBytes(StandardCharsets.UTF_8));
                out.write(body);
                out.flush();

                return success ? code : null;
            }
        } catch (BindException e) {
            throw new Exception(
                "Port " + CB_PORT + " is already in use. Close the application using it and try again.");
        }
    }

    private String exchangeCode(String code, String redirectUri) throws Exception {
        String params = "client_id="     + enc(clientId)
            + "&client_secret="          + enc(clientSecret)
            + "&code="                   + enc(code)
            + "&redirect_uri="           + enc(redirectUri);

        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json"); // request JSON, not form-encoded
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readStream(is);
        if (status >= 400) {
            throw new Exception("Token exchange failed (HTTP " + status + "): " + body);
        }
        Map<String, String> result = parseJson(body);
        if (result.containsKey("error")) {
            throw new Exception("GitHub token error: " + result.get("error_description"));
        }
        String token = result.get("access_token");
        if (token == null || token.isBlank()) {
            throw new Exception("No access_token in GitHub response: " + body);
        }
        return token;
    }

    private Map<String, String> getApiJson(String endpoint, String accessToken) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readStream(is);
        if (status >= 400) {
            throw new Exception("GitHub API error (HTTP " + status + "): " + body);
        }
        return parseJson(body);
    }

    /** Calls /user/emails and returns the primary verified email. */
    private String fetchPrimaryEmail(String accessToken) throws Exception {
        URL url = new URL(EMAIL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        return parsePrimaryEmail(readStream(conn.getInputStream()));
    }

    /**
     * Finds the email with "primary":true in the GitHub emails JSON array.
     * Falls back to the first email found if none is marked primary.
     */
    private String parsePrimaryEmail(String json) {
        Pattern objPat   = Pattern.compile("\\{([^}]+)\\}");
        Pattern emailPat = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"");
        Matcher objMatcher = objPat.matcher(json);
        String firstEmail = null;
        while (objMatcher.find()) {
            String obj = objMatcher.group(1);
            Matcher em = emailPat.matcher(obj);
            if (!em.find()) continue;
            String email = em.group(1);
            if (firstEmail == null) firstEmail = email;
            if (obj.contains("\"primary\":true") || obj.contains("\"primary\": true")) {
                return email;
            }
        }
        return firstEmail;
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        int space = fullName.indexOf(' ');
        if (space < 0) return new String[]{fullName, ""};
        return new String[]{fullName.substring(0, space), fullName.substring(space + 1)};
    }

    // ── Shared parsing / HTTP utilities ──────────────────────────────────────

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** Flat JSON parser — first occurrence wins so nested arrays don't clobber top-level keys. */
    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\]]+))");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String key   = m.group(1);
            String value = m.group(2) != null ? m.group(2) : m.group(3).trim();
            map.putIfAbsent(key, value);
        }
        return map;
    }

    private String parseParam(String query, String key) {
        for (String kv : query.split("&")) {
            String[] pair = kv.split("=", 2);
            if (pair.length == 2 && pair[0].equals(key)) {
                try { return URLDecoder.decode(pair[1], "UTF-8"); }
                catch (Exception e) { return pair[1]; }
            }
        }
        return null;
    }

    private String enc(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }
}