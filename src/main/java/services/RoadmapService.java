package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Resource;
import models.Roadmap;
import models.RoadmapStep;
import services.chat.ChatService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoadmapService {

    private static final String SYSTEM_PROMPT =
            "You are a strict JSON data generator.\n" +
            "You MUST respond ONLY with a valid JSON array of objects.\n" +
            "Do NOT include any markdown formatting, code blocks, or conversational text.\n" +
            "Ensure every string is properly escaped and double-quoted, and every object is correctly closed with { and }.";

    private static final String USER_TEMPLATE =
            "Generate a practical 6-step learning roadmap for the skill: \"%s\"\n\n" +
            "Respond with ONLY this JSON array (no other text):\n" +
            "[\n" +
            "  {\n" +
            "    \"stepNumber\": 1,\n" +
            "    \"title\": \"Concise step title\",\n" +
            "    \"description\": \"2-3 sentences explaining what to learn and why it matters.\",\n" +
            "    \"resources\": [\n" +
            "      { \"label\": \"Resource Name\", \"url\": \"https://real-url.com\" },\n" +
            "      { \"label\": \"Resource Name 2\", \"url\": \"https://real-url2.com\" }\n" +
            "    ]\n" +
            "  }\n" +
            "]\n\n" +
            "Rules:\n" +
            "- Exactly 6 steps, ordered from beginner to advanced.\n" +
            "- Each step has 2-3 real, working resource URLs (docs, tutorials, or courses).\n" +
            "- Descriptions are practical, not generic.";

    private final ChatService  chatService;
    private final ObjectMapper mapper;

    public RoadmapService() {
        this.chatService = new ChatService();
        this.mapper      = new ObjectMapper();
    }

    /** Returns true when an OpenRouter API key is configured. */
    public boolean isConfigured() {
        return chatService.hasApiKeyConfigured();
    }

    /**
     * Calls the AI and returns a parsed list of roadmap steps.
     * Must be called off the JavaFX Application Thread.
     */
    public List<RoadmapStep> generateRoadmap(String skill) throws Exception {
        String prompt      = String.format(USER_TEMPLATE, skill);
        String rawResponse = chatService.chat(SYSTEM_PROMPT, prompt);
        return parseResponse(rawResponse);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Inserts the roadmap and all its steps.
     * Sets the generated DB id on each RoadmapStep so callers can activate checkboxes.
     */
    public void saveRoadmap(Roadmap roadmap) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();

        String insertRoadmap = "INSERT INTO roadmap (skill, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertRoadmap, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roadmap.getSkill());
            ps.setInt(2, roadmap.getUserId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) roadmap.setId(rs.getInt(1));
            }
        }

        String insertStep = "INSERT INTO roadmap_step (roadmap_id, step_number, title, description, resources_json, is_completed) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertStep, Statement.RETURN_GENERATED_KEYS)) {
            for (RoadmapStep step : roadmap.getSteps()) {
                String resourcesJson = serializeResources(step.getResources());
                ps.setInt(1, roadmap.getId());
                ps.setInt(2, step.getStepNumber());
                ps.setString(3, step.getTitle());
                ps.setString(4, step.getDescription());
                ps.setString(5, resourcesJson);
                ps.setBoolean(6, step.isCompleted());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) step.setId(rs.getInt(1));
                }
            }
        }
    }

    /** Returns all saved roadmaps (with steps) for the given user, newest first. */
    public List<Roadmap> getSavedRoadmaps(int userId) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        List<Roadmap> result = new ArrayList<>();

        String query = "SELECT id, skill, created_at FROM roadmap WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Roadmap rm = new Roadmap();
                    rm.setId(rs.getInt("id"));
                    rm.setSkill(rs.getString("skill"));
                    rm.setCreatedAt(rs.getTimestamp("created_at"));
                    rm.setUserId(userId);
                    rm.setSteps(loadSteps(rm.getId()));
                    result.add(rm);
                }
            }
        }
        return result;
    }

    /** Updates the is_completed flag of a single step. */
    public void toggleStepCompletion(int stepId, boolean completed) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE roadmap_step SET is_completed = ? WHERE id = ?")) {
            ps.setBoolean(1, completed);
            ps.setInt(2, stepId);
            ps.executeUpdate();
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private List<RoadmapStep> loadSteps(int roadmapId) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        List<RoadmapStep> steps = new ArrayList<>();
        String q = "SELECT id, step_number, title, description, resources_json, is_completed FROM roadmap_step WHERE roadmap_id = ? ORDER BY step_number";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, roadmapId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoadmapStep step = new RoadmapStep();
                    step.setId(rs.getInt("id"));
                    step.setStepNumber(rs.getInt("step_number"));
                    step.setTitle(rs.getString("title"));
                    step.setDescription(rs.getString("description"));
                    step.setCompleted(rs.getBoolean("is_completed"));
                    step.setResources(deserializeResources(rs.getString("resources_json")));
                    steps.add(step);
                }
            }
        }
        return steps;
    }

    private String serializeResources(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) return "[]";
        try { return mapper.writeValueAsString(resources); } catch (Exception e) { return "[]"; }
    }

    private List<Resource> deserializeResources(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return mapper.readValue(json, new TypeReference<List<Resource>>() {}); } catch (Exception e) { return new ArrayList<>(); }
    }

    private List<RoadmapStep> parseResponse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            throw new Exception("AI returned an empty response.");
        }
        // Tolerate leading/trailing prose: find the JSON array bounds
        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');
        if (start == -1 || end == -1 || end < start) {
            throw new Exception("No valid JSON array found in AI response:\n" + raw);
        }
        String json = raw.substring(start, end + 1);
        return mapper.readValue(json, new TypeReference<List<RoadmapStep>>() {});
    }
}
