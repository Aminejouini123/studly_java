package services.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Stores OpenRouter configuration in the user's home directory so the user
 * doesn't need to re-declare environment variables every run.
 *
 * Location (Windows example):
 *   C:\Users\<you>\.studly\openrouter.properties
 */
public final class OpenRouterConfigStore {
    private static final String DIR_NAME = ".studly";
    private static final String FILE_NAME = "openrouter.properties";

    private static final String KEY_API_KEY = "openrouter.apiKey";
    private static final String KEY_MODEL = "openrouter.model";
    private static final String KEY_APP_URL = "openrouter.appUrl";
    private static final String KEY_APP_TITLE = "openrouter.appTitle";

    private final Path configPath;

    public OpenRouterConfigStore() {
        this(getDefaultPath());
    }

    public OpenRouterConfigStore(Path configPath) {
        this.configPath = configPath;
    }

    public String getApiKey() {
        return normalizeKey(get(KEY_API_KEY));
    }

    public void saveApiKey(String apiKey) throws IOException {
        set(KEY_API_KEY, normalizeKey(apiKey));
    }

    public String getModel() {
        return get(KEY_MODEL);
    }

    public String getAppUrl() {
        return get(KEY_APP_URL);
    }

    public String getAppTitle() {
        return get(KEY_APP_TITLE);
    }

    public void saveDefaultsIfMissing(String model, String appUrl, String appTitle) {
        try {
            Properties p = load();
            boolean changed = false;
            if (isBlank(p.getProperty(KEY_MODEL)) && !isBlank(model)) {
                p.setProperty(KEY_MODEL, model.trim());
                changed = true;
            }
            if (isBlank(p.getProperty(KEY_APP_URL)) && !isBlank(appUrl)) {
                p.setProperty(KEY_APP_URL, appUrl.trim());
                changed = true;
            }
            if (isBlank(p.getProperty(KEY_APP_TITLE)) && !isBlank(appTitle)) {
                p.setProperty(KEY_APP_TITLE, appTitle.trim());
                changed = true;
            }
            if (changed) {
                store(p);
            }
        } catch (IOException ignored) {
        }
    }

    public Path getConfigPath() {
        return configPath;
    }

    private String get(String key) {
        try {
            Properties p = load();
            String v = p.getProperty(key);
            return v == null ? "" : v.trim();
        } catch (IOException e) {
            return "";
        }
    }

    private void set(String key, String value) throws IOException {
        Properties p = load();
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            p.remove(key);
        } else {
            p.setProperty(key, v);
        }
        store(p);
    }

    private Properties load() throws IOException {
        Properties p = new Properties();
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                p.load(in);
            }
        }
        return p;
    }

    private void store(Properties p) throws IOException {
        Path dir = configPath.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        try (OutputStream out = Files.newOutputStream(configPath)) {
            p.store(out, "Studly AI - OpenRouter configuration");
        }
    }

    private static Path getDefaultPath() {
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, DIR_NAME, FILE_NAME);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        String k = raw.trim();
        if (k.toLowerCase().startsWith("bearer ")) {
            k = k.substring("bearer ".length()).trim();
        }
        return k;
    }
}

