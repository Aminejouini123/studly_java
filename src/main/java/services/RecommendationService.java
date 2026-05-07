package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Recommendation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RecommendationService {

    private static final String PROPS_PATH = "/recommendation.properties";
    private static final int MAX_JOBS    = 6;
    private static final int MAX_COURSES_PER_SKILL = 2;
    private static final int MAX_SKILLS_FOR_COURSES = 3;

    private final HttpClient  httpClient;
    private final ObjectMapper mapper;
    private final String adzunaAppId;
    private final String adzunaAppKey;
    private final String youtubeApiKey;

    public RecommendationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();

        Properties props = loadProperties();
        this.adzunaAppId  = props.getProperty("adzuna.app.id",   "").strip();
        this.adzunaAppKey = props.getProperty("adzuna.app.key",  "").strip();
        this.youtubeApiKey= props.getProperty("youtube.api.key", "").strip();
    }

    /** Returns true when at least one API is fully configured. */
    public boolean isConfigured() {
        return (!adzunaAppId.isEmpty() && !adzunaAppKey.isEmpty()) || !youtubeApiKey.isEmpty();
    }

    // ── Jobs ─────────────────────────────────────────────────────────────────

    public List<Recommendation> fetchJobs(List<String> skills) {
        List<Recommendation> results = new ArrayList<>();
        if (adzunaAppId.isEmpty() || adzunaAppKey.isEmpty()) return results;

        String query = URLEncoder.encode(String.join(" ", skills), StandardCharsets.UTF_8);
        String url = String.format(
                "https://api.adzuna.com/v1/api/jobs/us/search/1" +
                "?app_id=%s&app_key=%s&results_per_page=%d&what=%s&content-type=application/json",
                adzunaAppId, adzunaAppKey, MAX_JOBS, query);

        try {
            String body = get(url);
            JsonNode root = mapper.readTree(body);
            for (JsonNode job : root.path("results")) {
                String title       = job.path("title").asText("");
                String company     = job.path("company").path("display_name").asText("Unknown");
                String redirectUrl = job.path("redirect_url").asText("");
                String description = job.path("description").asText("");
                int score = calculateMatchScore(skills, title + " " + description);
                results.add(new Recommendation(title, company, redirectUrl, Recommendation.Type.JOB, score));
            }
        } catch (Exception e) {
            System.err.println("RecommendationService – jobs fetch failed: " + e.getMessage());
        }
        return results;
    }

    // ── Courses ───────────────────────────────────────────────────────────────

    public List<Recommendation> fetchCourses(List<String> skills) {
        List<Recommendation> results = new ArrayList<>();
        if (youtubeApiKey.isEmpty()) return results;

        List<String> topSkills = skills.subList(0, Math.min(MAX_SKILLS_FOR_COURSES, skills.size()));
        for (String skill : topSkills) {
            String query = URLEncoder.encode(skill + " tutorial course", StandardCharsets.UTF_8);
            String url = String.format(
                    "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet&q=%s&type=video&key=%s&maxResults=%d&videoCategoryId=27",
                    query, youtubeApiKey, MAX_COURSES_PER_SKILL);

            try {
                String body = get(url);
                JsonNode root = mapper.readTree(body);
                for (JsonNode item : root.path("items")) {
                    String videoId    = item.path("id").path("videoId").asText("");
                    String title      = item.path("snippet").path("title").asText("");
                    String channel    = item.path("snippet").path("channelTitle").asText("YouTube");
                    String description= item.path("snippet").path("description").asText("");
                    String videoUrl   = "https://www.youtube.com/watch?v=" + videoId;
                    int score = calculateMatchScore(skills, title + " " + description);
                    results.add(new Recommendation(title, channel, videoUrl, Recommendation.Type.COURSE, score));
                }
            } catch (Exception e) {
                System.err.println("RecommendationService – courses fetch failed for '" + skill + "': " + e.getMessage());
            }
        }
        return results;
    }

    // ── Match scoring ─────────────────────────────────────────────────────────

    /**
     * Returns 0–100 based on the fraction of userSkills that appear
     * (case-insensitive) anywhere in the given text.
     */
    public int calculateMatchScore(List<String> userSkills, String text) {
        if (userSkills.isEmpty() || text == null || text.isBlank()) return 0;
        String lower = text.toLowerCase();
        long hits = userSkills.stream()
                .filter(s -> !s.isBlank() && lower.contains(s.toLowerCase()))
                .count();
        return (int) Math.round((double) hits / userSkills.size() * 100);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(PROPS_PATH)) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            System.err.println("RecommendationService: could not load " + PROPS_PATH);
        }
        return props;
    }
}