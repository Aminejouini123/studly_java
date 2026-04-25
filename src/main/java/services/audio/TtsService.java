package services.audio;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

public class TtsService {
    private String apiKey;
    private static final String API_URL = "https://api.voicerss.org/";

    public TtsService() {
        loadApiKey();
    }

    private void loadApiKey() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("openrouter.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                this.apiKey = prop.getProperty("voicerss.apiKey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Converts text to speech and returns the audio file path.
     * @param text The text to convert.
     * @param lang The language code (e.g., "en-us", "fr-fr").
     * @return Path to the temporary mp3 file.
     */
    public File speak(String text, String lang) throws Exception {
        if (!hasApiKey()) {
            throw new Exception("VoiceRSS API Key not configured in openrouter.properties");
        }

        // VoiceRSS has a limit on text length per request (usually 100KB or so, but let's be safe)
        // For a full PDF, we might need to chunk it, but for now let's handle a reasonable amount.
        String truncatedText = text.length() > 3000 ? text.substring(0, 3000) : text;

        String urlParameters = "key=" + apiKey +
                "&src=" + URLEncoder.encode(truncatedText, StandardCharsets.UTF_8.toString()) +
                "&hl=" + lang +
                "&c=MP3" +
                "&f=44khz_16bit_stereo";

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            File tempFile = File.createTempFile("studly_tts_", ".mp3");
            tempFile.deleteOnExit();
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } else {
            throw new Exception("VoiceRSS API error: " + responseCode + " " + conn.getResponseMessage());
        }
    }
}
