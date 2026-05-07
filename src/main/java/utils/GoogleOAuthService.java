package utils;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleOAuthService {

    private static final String AUTH_URL   = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL  = "https://oauth2.googleapis.com/token";
    private static final String UINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";

    public static final class UserProfile {
        public final String googleId;
        public final String email;
        public final String firstName;
        public final String lastName;
        public final String pictureUrl;
        public final String accessToken;
        public final String refreshToken;
        public final Timestamp tokenExpiresAt;

        UserProfile(String googleId, String email, String firstName, String lastName,
                    String pictureUrl, String accessToken, String refreshToken,
                    Timestamp tokenExpiresAt) {
            this.googleId       = googleId;
            this.email          = email;
            this.firstName      = firstName;
            this.lastName       = lastName;
            this.pictureUrl     = pictureUrl;
            this.accessToken    = accessToken;
            this.refreshToken   = refreshToken;
            this.tokenExpiresAt = tokenExpiresAt;
        }
    }

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthService() {
        Properties config = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/google-oauth.properties")) {
            if (in != null) config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientId     = config.getProperty("google.client.id", "").trim();
        clientSecret = config.getProperty("google.client.secret", "").trim();
    }

    public boolean isConfigured() {
        return !clientId.isEmpty()     && !clientId.startsWith("YOUR_")
            && !clientSecret.isEmpty() && !clientSecret.startsWith("YOUR_");
    }

    /** Full OAuth2 flow: opens browser, waits for callback, returns profile. */
    public UserProfile authenticate() throws Exception {
        // Find a free port for the local redirect server
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        String redirectUri = "http://localhost:" + port + "/oauth2/callback";

        // Build authorization URL
        String authUrl = AUTH_URL
            + "?client_id="     + enc(clientId)
            + "&redirect_uri="  + enc(redirectUri)
            + "&response_type=code"
            + "&scope="         + enc("openid email profile")
            + "&access_type=offline"
            + "&prompt=select_account";

        // Open the system browser
        openBrowser(authUrl);

        // Wait for the redirect callback (3-minute timeout)
        String code = waitForAuthCode(port);
        if (code == null) throw new Exception("Google login cancelled or timed out.");

        // Exchange code → tokens
        Map<String, String> tokens = exchangeCode(code, clientId, clientSecret, redirectUri);
        String accessToken  = tokens.get("access_token");
        String refreshToken = tokens.getOrDefault("refresh_token", null);
        int expiresIn       = parseIntSafe(tokens.getOrDefault("expires_in", "3600"));
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + expiresIn * 1000L);

        // Fetch user profile
        Map<String, String> info = getUserInfo(accessToken);
        return new UserProfile(
            info.get("sub"),
            info.get("email"),
            info.getOrDefault("given_name", ""),
            info.getOrDefault("family_name", ""),
            info.getOrDefault("picture", null),
            accessToken,
            refreshToken,
            expiresAt
        );
    }

    // ---- private helpers ----

    private void openBrowser(String url) throws Exception {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            // Windows fallback
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        }
    }

    private String waitForAuthCode(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(180_000); // 3 minutes
            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String requestLine = reader.readLine(); // e.g. "GET /oauth2/callback?code=xxx HTTP/1.1"

                String code = null;
                if (requestLine != null) {
                    String[] parts = requestLine.split(" ");
                    if (parts.length >= 2) {
                        String path = parts[1];
                        int q = path.indexOf('?');
                        if (q >= 0) code = parseParam(path.substring(q + 1), "code");
                    }
                }

                // Respond to browser so the tab shows a friendly message
                String html = "<!DOCTYPE html><html><head>"
                    + "<style>body{font-family:sans-serif;text-align:center;padding-top:60px;background:#f0f4ff;}</style>"
                    + "</head><body>"
                    + "<h2 style='color:#052659'>&#10003; Authentication Successful</h2>"
                    + "<p style='color:#555'>You can close this tab and return to <strong>Studly</strong>.</p>"
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

                return code;
            }
        }
    }

    private Map<String, String> exchangeCode(String code, String clientId,
                                              String clientSecret, String redirectUri) throws Exception {
        String params = "code="          + enc(code)
            + "&client_id="              + enc(clientId)
            + "&client_secret="          + enc(clientSecret)
            + "&redirect_uri="           + enc(redirectUri)
            + "&grant_type=authorization_code";

        return httpPost(TOKEN_URL, params);
    }

    private Map<String, String> getUserInfo(String accessToken) throws Exception {
        URL url = new URL(UINFO_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        return parseJson(readStream(conn.getInputStream()));
    }

    private Map<String, String> httpPost(String endpoint, String params) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readStream(is);
        if (conn.getResponseCode() >= 400) {
            throw new Exception("Token exchange failed (HTTP " + conn.getResponseCode() + "): " + body);
        }
        return parseJson(body);
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // Flat JSON parser — handles Google's auth/userinfo responses
    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\]]+))");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String key   = m.group(1);
            String value = m.group(2) != null ? m.group(2) : m.group(3).trim();
            map.put(key, value);
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

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 3600; }
    }
}